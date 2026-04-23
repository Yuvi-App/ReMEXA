package remexa.tools.sdkstub;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class JavadocStubGenerator {
    private static final Charset SHIFT_JIS = Charset.forName("Shift_JIS");
    private static final Set<String> SKIP_FILE_NAMES = Set.of(
            "package-summary.html",
            "package-tree.html",
            "package-frame.html",
            "overview-summary.html",
            "overview-frame.html",
            "overview-tree.html",
            "allclasses-frame.html",
            "allclasses-noframe.html",
            "index.html",
            "index-all.html",
            "deprecated-list.html",
            "help-doc.html",
            "serialized-form.html",
            "constant-values.html"
    );
    private static final Pattern TAG_PATTERN = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern PRE_PATTERN = Pattern.compile("(?is)<PRE>(.*?)</PRE>");
    private static final Pattern DECLARATION_PREFIX = Pattern.compile("^(public|protected|private)\\b");
    private static final Pattern LINK_PATTERN = Pattern.compile("(?is)<A HREF=\"([^\"]+)\"[^>]*>(.*?)</A>");

    private JavadocStubGenerator() {
    }

    public static void main(String[] args) throws IOException {
        var options = parseArguments(args);
        var docsRoot = Path.of(requiredOption(options, "--docs-root"));
        var outputRoot = Path.of(requiredOption(options, "--output-root"));

        Files.createDirectories(outputRoot);
        clearGeneratedTree(outputRoot);

        var sources = List.of(
                docsRoot.resolve("javadoc").resolve("mexa"),
                docsRoot.resolve("vodafone_docs")
        );

        var types = new LinkedHashMap<String, TypeSpec>();
        for (var source : sources) {
            if (!Files.isDirectory(source)) {
                continue;
            }
            try (var stream = Files.walk(source)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".html"))
                        .filter(path -> !SKIP_FILE_NAMES.contains(path.getFileName().toString()))
                        .forEach(path -> {
                            try {
                                var parsed = parseType(source, path);
                                if (parsed != null) {
                                    types.merge(parsed.qualifiedName(), parsed, TypeSpec::merge);
                                }
                            } catch (IOException exception) {
                                throw new RuntimeException("Failed to parse " + path, exception);
                            }
                        });
            }
        }

        for (var type : types.values()) {
            writeType(outputRoot, type);
        }

        System.out.println("Generated " + types.size() + " SDK stub types into " + outputRoot);
    }

    private static Map<String, String> parseArguments(String[] args) {
        var options = new LinkedHashMap<String, String>();
        for (int index = 0; index < args.length - 1; index += 2) {
            options.put(args[index], args[index + 1]);
        }
        return options;
    }

    private static String requiredOption(Map<String, String> options, String key) {
        var value = options.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required argument: " + key);
        }
        return value;
    }

    private static void clearGeneratedTree(Path outputRoot) throws IOException {
        if (!Files.exists(outputRoot)) {
            return;
        }
        try (var stream = Files.walk(outputRoot)) {
            stream.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .filter(path -> !path.equals(outputRoot))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    });
        }
        Files.createDirectories(outputRoot);
    }

    private static TypeSpec parseType(Path sourceRoot, Path htmlFile) throws IOException {
        var relativeParent = sourceRoot.relativize(htmlFile.getParent()).toString();
        if (relativeParent.isBlank()) {
            return null;
        }

        var packageName = relativeParent.replace('\\', '.').replace('/', '.');
        if (!packageName.startsWith("com.")) {
            return null;
        }

        var simpleName = stripSuffix(htmlFile.getFileName().toString(), ".html");
        var content = Files.readString(htmlFile, SHIFT_JIS);

        var declaration = extractDeclaration(sourceRoot, htmlFile, content, simpleName);
        if (declaration == null) {
            return null;
        }

        var fields = new LinkedHashSet<>(extractMemberSignatures(sourceRoot, htmlFile, content, "field_detail"));
        var constructors = new LinkedHashSet<>(extractMemberSignatures(sourceRoot, htmlFile, content, "constructor_detail"));
        var methods = new LinkedHashSet<>(extractMemberSignatures(sourceRoot, htmlFile, content, "method_detail"));

        return new TypeSpec(packageName, simpleName, declaration, fields, constructors, methods);
    }

    private static String extractDeclaration(Path sourceRoot, Path htmlFile, String content, String simpleName) {
        var marker = "<B>" + simpleName + "</B>";
        var markerIndex = content.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }

        var start = content.lastIndexOf("<DT>", markerIndex);
        var end = content.indexOf("</DL>", markerIndex);
        if (start < 0 || end < 0 || end <= start) {
            return null;
        }

        var declaration = cleanSignature(sourceRoot, htmlFile, content.substring(start, end));
        if (!declaration.contains(" class ") && !declaration.contains(" interface ")) {
            return null;
        }
        declaration = declaration.replace("extends java.lang.Object", "").trim();
        return declaration;
    }

    private static List<String> extractMemberSignatures(Path sourceRoot, Path htmlFile, String content, String anchorName) {
        var section = extractSection(content, anchorName);
        if (section == null) {
            return List.of();
        }

        var signatures = new ArrayList<String>();
        var matcher = PRE_PATTERN.matcher(section);
        while (matcher.find()) {
            var signature = cleanSignature(sourceRoot, htmlFile, matcher.group(1));
            if (signature.isBlank()) {
                continue;
            }
            if (signature.contains(" class ") || signature.contains(" interface ")) {
                continue;
            }
            if (!DECLARATION_PREFIX.matcher(signature).find()) {
                continue;
            }
            signatures.add(signature);
        }
        return signatures;
    }

    private static String extractSection(String content, String anchorName) {
        var startMarker = "<A NAME=\"" + anchorName + "\">";
        var start = content.indexOf(startMarker);
        if (start < 0) {
            return null;
        }

        var end = content.length();
        for (var nextAnchor : List.of("field_detail", "constructor_detail", "method_detail", "end_of_class")) {
            if (nextAnchor.equals(anchorName)) {
                continue;
            }
            var nextIndex = content.indexOf("<A NAME=\"" + nextAnchor + "\">", start + startMarker.length());
            if (nextIndex > start && nextIndex < end) {
                end = nextIndex;
            }
        }

        var bodyEnd = content.indexOf("</BODY>", start);
        if (bodyEnd > start && bodyEnd < end) {
            end = bodyEnd;
        }
        return content.substring(start, end);
    }

    private static void writeType(Path outputRoot, TypeSpec type) throws IOException {
        var file = outputRoot.resolve(type.packageName().replace('.', '\\')).resolve(type.simpleName() + ".java");
        Files.createDirectories(file.getParent());

        var builder = new StringBuilder();
        builder.append("package ").append(type.packageName()).append(";\n\n");
        builder.append(type.declaration()).append(" {\n");

        for (var field : type.fields()) {
            builder.append("    ").append(renderField(field, type.interfaceType())).append('\n');
        }
        if (!type.fields().isEmpty()) {
            builder.append('\n');
        }

        if (!type.interfaceType() && type.needsSyntheticNoArgConstructor()) {
            builder.append("    protected ").append(type.simpleName()).append("() {\n");
            builder.append("        remexa.probes.SdkStubSupport.log(\"")
                    .append(type.qualifiedName())
                    .append("\", \"")
                    .append(type.simpleName())
                    .append("\");\n");
            builder.append("    }\n\n");
        }

        for (var constructor : type.constructors()) {
            builder.append(renderExecutable(type, constructor, true)).append('\n');
        }
        if (!type.constructors().isEmpty() && !type.methods().isEmpty()) {
            builder.append('\n');
        }

        for (int index = 0; index < type.methods().size(); index++) {
            builder.append(renderExecutable(type, type.methods().get(index), false));
            if (index < type.methods().size() - 1) {
                builder.append('\n');
            }
        }

        builder.append("}\n");

        Files.writeString(
                file,
                builder.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private static String renderField(String signature, boolean interfaceType) {
        var cleaned = signature.replace(" transient ", " ").replace(" volatile ", " ");
        var lastSpace = cleaned.lastIndexOf(' ');
        if (lastSpace < 0) {
            return cleaned + ";";
        }

        var fieldName = cleaned.substring(lastSpace + 1).trim();
        var typePart = cleaned.substring(0, lastSpace).trim();
        var rawType = fieldBaseType(typePart);
        var initializer = defaultLiteral(rawType);
        if (interfaceType && !typePart.contains(" static ")) {
            typePart = "public static final " + rawType;
        }
        return typePart + " " + fieldName + " = " + initializer + ";";
    }

    private static String renderExecutable(TypeSpec owner, String signature, boolean constructor) {
        var declaration = signature.replace(" native ", " ").replace(" abstract ", " ").trim();
        var parameters = extractParameters(signature);
        var methodName = constructor ? owner.simpleName() : extractExecutableName(signature);
        var callArguments = parameters.isEmpty() ? "" : String.join(", ", parameters);

        if (owner.interfaceType() || (!constructor && signature.contains(" abstract "))) {
            return "    " + signature.trim() + ";";
        }

        var builder = new StringBuilder();
        builder.append("    ").append(declaration).append(" {\n");
        builder.append("        remexa.probes.SdkStubSupport.log(\"")
                .append(owner.qualifiedName())
                .append("\", \"")
                .append(methodName)
                .append("\"");
        if (!callArguments.isBlank()) {
            builder.append(", ").append(callArguments);
        }
        builder.append(");\n");

        if (!constructor) {
            var returnType = extractReturnType(signature, methodName);
            var defaultValue = defaultLiteral(returnType);
            if (!"void".equals(returnType)) {
                builder.append("        return ").append(defaultValue).append(";\n");
            }
        }

        builder.append("    }\n");
        return builder.toString();
    }

    private static String extractExecutableName(String signature) {
        var openParen = signature.indexOf('(');
        var prefix = signature.substring(0, openParen).trim();
        var pieces = prefix.split("\\s+");
        return pieces[pieces.length - 1];
    }

    private static List<String> extractParameters(String signature) {
        var openParen = signature.indexOf('(');
        var closeParen = signature.indexOf(')', openParen);
        if (openParen < 0 || closeParen < 0) {
            return List.of();
        }

        var parameterBlock = signature.substring(openParen + 1, closeParen).trim();
        if (parameterBlock.isBlank()) {
            return List.of();
        }

        var parameters = new ArrayList<String>();
        for (var parameter : parameterBlock.split(",")) {
            var trimmed = parameter.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            var pieces = trimmed.split("\\s+");
            parameters.add(pieces[pieces.length - 1]);
        }
        return parameters;
    }

    private static String extractReturnType(String signature, String executableName) {
        var openParen = signature.indexOf('(');
        var prefix = signature.substring(0, openParen).trim();
        var index = prefix.lastIndexOf(executableName);
        var beforeName = prefix.substring(0, index).trim();
        var tokens = new ArrayList<>(Arrays.asList(beforeName.split("\\s+")));
        tokens.removeIf(JavadocStubGenerator::isModifier);
        if (tokens.isEmpty()) {
            return "void";
        }
        return String.join(" ", tokens);
    }

    private static String fieldBaseType(String signatureWithoutName) {
        var tokens = new ArrayList<>(Arrays.asList(signatureWithoutName.split("\\s+")));
        tokens.removeIf(JavadocStubGenerator::isModifier);
        return tokens.isEmpty() ? "java.lang.Object" : String.join(" ", tokens);
    }

    private static boolean isModifier(String token) {
        return Set.of(
                "public",
                "protected",
                "private",
                "static",
                "final",
                "abstract",
                "native",
                "synchronized",
                "strictfp",
                "transient",
                "volatile"
        ).contains(token);
    }

    private static String defaultLiteral(String typeName) {
        return switch (typeName) {
            case "boolean" -> "false";
            case "byte" -> "(byte) 0";
            case "short" -> "(short) 0";
            case "int" -> "0";
            case "long" -> "0L";
            case "float" -> "0.0f";
            case "double" -> "0.0d";
            case "char" -> "'\\0'";
            case "java.lang.String", "String" -> "\"\"";
            case "void" -> "";
            default -> "null";
        };
    }

    private static String stripSuffix(String value, String suffix) {
        return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
    }

    private static String cleanSignature(Path sourceRoot, Path currentFile, String html) {
        var cleaned = qualifyLinks(sourceRoot, currentFile, html)
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&#40;", "(")
                .replace("&#41;", ")")
                .replace("<BR>", " ")
                .replace("<br>", " ")
                .replace("<br/>", " ")
                .replace("<br />", " ");
        cleaned = TAG_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replace('\n', ' ').replace('\r', ' ');
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        cleaned = cleaned.replace(" ,", ",");
        cleaned = cleaned.replace("( ", "(").replace(" )", ")");
        cleaned = cleaned.replace(" []", "[]");
        cleaned = cleaned.replace(" >", ">").replace("< ", "<");
        return cleaned;
    }

    private static String qualifyLinks(Path sourceRoot, Path currentFile, String html) {
        var matcher = LINK_PATTERN.matcher(html);
        var builder = new StringBuilder();
        while (matcher.find()) {
            var href = matcher.group(1);
            var text = TAG_PATTERN.matcher(matcher.group(2)).replaceAll("").trim();
            var replacement = resolveHrefType(sourceRoot, currentFile, href, text);
            matcher.appendReplacement(builder, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private static String resolveHrefType(Path sourceRoot, Path currentFile, String href, String text) {
        var hashIndex = href.indexOf('#');
        var pathPart = hashIndex >= 0 ? href.substring(0, hashIndex) : href;
        if (pathPart.isBlank() || !pathPart.endsWith(".html")) {
            return text;
        }
        try {
            var resolved = currentFile.getParent().resolve(pathPart).normalize();
            var relative = sourceRoot.relativize(resolved).toString().replace('\\', '/');
            if (!relative.startsWith("com/")) {
                return text;
            }
            return stripSuffix(relative, ".html").replace('/', '.');
        } catch (IllegalArgumentException exception) {
            return text;
        }
    }

    private record TypeSpec(
            String packageName,
            String simpleName,
            String declaration,
            Set<String> fieldSet,
            Set<String> constructorSet,
            Set<String> methodSet
    ) {
        String qualifiedName() {
            return packageName + "." + simpleName;
        }

        boolean interfaceType() {
            return declaration.contains(" interface ");
        }

        List<String> fields() {
            return List.copyOf(fieldSet);
        }

        List<String> constructors() {
            return deduplicateExecutables(constructorSet);
        }

        List<String> methods() {
            return deduplicateExecutables(methodSet);
        }

        boolean needsSyntheticNoArgConstructor() {
            return !constructorSet.isEmpty() && constructorSet.stream().noneMatch(signature -> executableKey(signature).endsWith("()"));
        }

        TypeSpec merge(TypeSpec other) {
            var mergedFields = new LinkedHashSet<>(fieldSet);
            mergedFields.addAll(other.fieldSet);

            var mergedConstructors = new LinkedHashSet<>(constructorSet);
            mergedConstructors.addAll(other.constructorSet);

            var mergedMethods = new LinkedHashSet<>(methodSet);
            mergedMethods.addAll(other.methodSet);

            var chosenDeclaration = declaration.length() >= other.declaration.length() ? declaration : other.declaration;
            return new TypeSpec(packageName, simpleName, chosenDeclaration, mergedFields, mergedConstructors, mergedMethods);
        }

        TypeSpec {
            Objects.requireNonNull(packageName);
            Objects.requireNonNull(simpleName);
            Objects.requireNonNull(declaration);
            Objects.requireNonNull(fieldSet);
            Objects.requireNonNull(constructorSet);
            Objects.requireNonNull(methodSet);
        }
    }

    private static List<String> deduplicateExecutables(Collection<String> signatures) {
        var result = new LinkedHashMap<String, String>();
        for (var signature : signatures) {
            result.putIfAbsent(executableKey(signature), signature);
        }
        return List.copyOf(result.values());
    }

    private static String executableKey(String signature) {
        var openParen = signature.indexOf('(');
        var closeParen = signature.indexOf(')', openParen);
        if (openParen < 0 || closeParen < 0) {
            return signature;
        }

        var name = extractExecutableName(signature);
        var parameterBlock = signature.substring(openParen + 1, closeParen).trim();
        if (parameterBlock.isBlank()) {
            return name + "()";
        }

        var types = new ArrayList<String>();
        for (var parameter : parameterBlock.split(",")) {
            var trimmed = parameter.trim();
            var pieces = trimmed.split("\\s+");
            if (pieces.length <= 1) {
                types.add(trimmed);
                continue;
            }
            types.add(String.join(" ", Arrays.copyOf(pieces, pieces.length - 1)));
        }
        return name + "(" + String.join(",", types) + ")";
    }
}
