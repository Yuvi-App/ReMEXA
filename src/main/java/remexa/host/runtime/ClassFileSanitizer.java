package remexa.host.runtime;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.Label;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.LabelTarget;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

final class ClassFileSanitizer {
    private static final ClassDesc SPIN_SUPPORT = ClassDesc.of("remexa.host.runtime.LegacyRuntimeSupport");
    private static final MethodTypeDesc SPIN_HINT_DESCRIPTOR = MethodTypeDesc.ofDescriptor("()V");

    private ClassFileSanitizer() {
    }

    static SanitizeResult zeroSwitchPadding(byte[] classBytes) {
        byte[] sanitized = classBytes.clone();
        Map<Integer, String> utf8Entries = new HashMap<>();
        int offset = 0;

        if (readU4(sanitized, offset) != 0xCAFEBABE) {
            return new SanitizeResult(classBytes, 0);
        }
        offset += 4; // magic
        offset += 2; // minor
        offset += 2; // major

        int constantPoolCount = readU2(sanitized, offset);
        offset += 2;
        for (int index = 1; index < constantPoolCount; index++) {
            int tag = readU1(sanitized, offset++);
            switch (tag) {
                case 1 -> {
                    int length = readU2(sanitized, offset);
                    offset += 2;
                    utf8Entries.put(index, new String(sanitized, offset, length, java.nio.charset.StandardCharsets.UTF_8));
                    offset += length;
                }
                case 3, 4, 9, 10, 11, 12, 17, 18 -> offset += 4;
                case 5, 6 -> {
                    offset += 8;
                    index++;
                }
                case 7, 8, 16, 19, 20 -> offset += 2;
                case 15 -> offset += 3;
                default -> throw new IllegalArgumentException("Unsupported constant pool tag: " + tag);
            }
        }

        offset += 2; // access_flags
        offset += 2; // this_class
        offset += 2; // super_class

        int interfacesCount = readU2(sanitized, offset);
        offset += 2 + interfacesCount * 2;

        int fieldsCount = readU2(sanitized, offset);
        offset += 2;
        for (int index = 0; index < fieldsCount; index++) {
            offset = skipMember(sanitized, offset, utf8Entries, false);
        }

        int methodsCount = readU2(sanitized, offset);
        offset += 2;
        int changes = 0;
        for (int index = 0; index < methodsCount; index++) {
            offset = skipMember(sanitized, offset, utf8Entries, true);
            changes += LAST_MEMBER_CHANGE_COUNT;
        }

        return changes == 0 ? new SanitizeResult(classBytes, 0) : new SanitizeResult(sanitized, changes);
    }

    static SanitizeResult injectSpinLoopHints(byte[] classBytes) {
        try {
            var classFile = ClassFile.of();
            var classModel = classFile.parse(classBytes);
            var changes = new AtomicInteger();
            byte[] transformed = classFile.transformClass(
                    classModel,
                    ClassTransform.transformingMethodBodies(
                            CodeTransform.ofStateful(() -> new SpinLoopCodeTransform(changes))
                    )
            );
            return changes.get() == 0 ? new SanitizeResult(classBytes, 0) : new SanitizeResult(transformed, changes.get());
        } catch (RuntimeException ignored) {
            return new SanitizeResult(classBytes, 0);
        }
    }

    private static int LAST_MEMBER_CHANGE_COUNT;

    private static int skipMember(byte[] classBytes, int offset, Map<Integer, String> utf8Entries, boolean inspectCode) {
        LAST_MEMBER_CHANGE_COUNT = 0;
        offset += 2; // access_flags
        offset += 2; // name_index
        offset += 2; // descriptor_index
        int attributesCount = readU2(classBytes, offset);
        offset += 2;
        for (int index = 0; index < attributesCount; index++) {
            int nameIndex = readU2(classBytes, offset);
            offset += 2;
            int attributeLength = readU4(classBytes, offset);
            offset += 4;
            String attributeName = utf8Entries.get(nameIndex);
            if (inspectCode && "Code".equals(attributeName)) {
                LAST_MEMBER_CHANGE_COUNT += sanitizeCodeAttribute(classBytes, offset);
            }
            offset += attributeLength;
        }
        return offset;
    }

    private static int sanitizeCodeAttribute(byte[] classBytes, int offset) {
        int changes = 0;
        int codeAttributeOffset = offset;
        offset += 2; // max_stack
        offset += 2; // max_locals
        int codeLength = readU4(classBytes, offset);
        offset += 4;
        changes += sanitizeBytecode(classBytes, offset, codeLength);
        offset += codeLength;
        int exceptionTableLength = readU2(classBytes, offset);
        offset += 2 + exceptionTableLength * 8;
        int attributesCount = readU2(classBytes, offset);
        offset += 2;
        for (int index = 0; index < attributesCount; index++) {
            int attributeLength = readU4(classBytes, offset + 2);
            offset += 6 + attributeLength;
        }
        return changes;
    }

    private static int sanitizeBytecode(byte[] classBytes, int codeOffset, int codeLength) {
        int changes = 0;
        int pc = 0;
        while (pc < codeLength) {
            int opcode = readU1(classBytes, codeOffset + pc);
            switch (opcode) {
                case 0xAA -> {
                    int padding = (4 - ((pc + 1) & 3)) & 3;
                    for (int index = 0; index < padding; index++) {
                        int position = codeOffset + pc + 1 + index;
                        if (classBytes[position] != 0) {
                            classBytes[position] = 0;
                            changes++;
                        }
                    }
                    int high = readS4(classBytes, codeOffset + pc + 1 + padding + 8);
                    int low = readS4(classBytes, codeOffset + pc + 1 + padding + 4);
                    pc += 1 + padding + 12 + Math.max(0, high - low + 1) * 4;
                }
                case 0xAB -> {
                    int padding = (4 - ((pc + 1) & 3)) & 3;
                    for (int index = 0; index < padding; index++) {
                        int position = codeOffset + pc + 1 + index;
                        if (classBytes[position] != 0) {
                            classBytes[position] = 0;
                            changes++;
                        }
                    }
                    int pairs = readS4(classBytes, codeOffset + pc + 1 + padding + 4);
                    pc += 1 + padding + 8 + Math.max(0, pairs) * 8;
                }
                case 0xC4 -> {
                    int widenedOpcode = readU1(classBytes, codeOffset + pc + 1);
                    pc += widenedOpcode == 0x84 ? 6 : 4;
                }
                default -> pc += opcodeLength(opcode);
            }
        }
        return changes;
    }

    private static int opcodeLength(int opcode) {
        return switch (opcode) {
            case 0x10, 0x12, 0x15, 0x16, 0x17, 0x18, 0x19, 0x36, 0x37, 0x38, 0x39, 0x3A, 0xA9, 0xBC -> 2;
            case 0x11, 0x13, 0x14, 0x84, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9E, 0x9F, 0xA0, 0xA1, 0xA2,
                    0xA3, 0xA4, 0xA5, 0xA6, 0xA7, 0xA8, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7, 0xB8,
                    0xBB, 0xBD, 0xC0, 0xC1, 0xC6, 0xC7 -> 3;
            case 0xB9, 0xBA, 0xC8, 0xC9 -> 5;
            case 0xC5 -> 4;
            default -> 1;
        };
    }

    private static int readU1(byte[] input, int offset) {
        return input[offset] & 0xFF;
    }

    private static int readU2(byte[] input, int offset) {
        return (readU1(input, offset) << 8) | readU1(input, offset + 1);
    }

    private static int readU4(byte[] input, int offset) {
        return (readU1(input, offset) << 24)
                | (readU1(input, offset + 1) << 16)
                | (readU1(input, offset + 2) << 8)
                | readU1(input, offset + 3);
    }

    private static int readS4(byte[] input, int offset) {
        return readU4(input, offset);
    }

    record SanitizeResult(byte[] classBytes, int changes) {
    }

    private static final class SpinLoopCodeTransform implements CodeTransform {
        private final AtomicInteger changeCount;
        private final Set<Label> seenLabels = Collections.newSetFromMap(new IdentityHashMap<>());

        private SpinLoopCodeTransform(AtomicInteger changeCount) {
            this.changeCount = changeCount;
        }

        @Override
        public void accept(CodeBuilder builder, CodeElement element) {
            if (element instanceof LabelTarget labelTarget) {
                seenLabels.add(labelTarget.label());
                builder.with(element);
                return;
            }
            if (element instanceof BranchInstruction branch && seenLabels.contains(branch.target())) {
                builder.invokestatic(SPIN_SUPPORT, "spinLoopHint", SPIN_HINT_DESCRIPTOR);
                changeCount.incrementAndGet();
            }
            builder.with(element);
        }
    }
}
