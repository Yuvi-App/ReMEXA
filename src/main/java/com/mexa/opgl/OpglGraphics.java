package com.mexa.opgl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.microedition.lcdui.Graphics;

public class OpglGraphics {
    private static volatile OpglGraphics instance;
    public static final int GL_ACTIVE_TEXTURE = 0;
    public static final int GL_ADD = 0;
    public static final int GL_ADD_SIGNED = 0;
    public static final int GL_ALIASED_LINE_WIDTH_RANGE = 0;
    public static final int GL_ALIASED_POINT_SIZE_RANGE = 0;
    public static final int GL_ALPHA = 0;
    public static final int GL_ALPHA_BITS = 0;
    public static final int GL_ALPHA_SCALE = 0;
    public static final int GL_ALPHA_TEST = 0;
    public static final int GL_ALPHA_TEST_FUNC = 0;
    public static final int GL_ALPHA_TEST_REF = 0;
    public static final int GL_ALWAYS = 0;
    public static final int GL_AMBIENT = 0;
    public static final int GL_AMBIENT_AND_DIFFUSE = 0;
    public static final int GL_AND = 0;
    public static final int GL_AND_INVERTED = 0;
    public static final int GL_AND_REVERSE = 0;
    public static final int GL_ARRAY_BUFFER = 34962;
    public static final int GL_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_BACK = 0;
    public static final int GL_BLEND = 0;
    public static final int GL_BLEND_DST = 0;
    public static final int GL_BLEND_SRC = 0;
    public static final int GL_BLUE_BITS = 0;
    public static final int GL_BUFFER_SIZE = 0;
    public static final int GL_BUFFER_USAGE = 0;
    public static final int GL_BYTE = 5120;
    public static final int GL_CCW = 0;
    public static final int GL_CLAMP_TO_EDGE = 0;
    public static final int GL_CLEAR = 0;
    public static final int GL_CLIENT_ACTIVE_TEXTURE = 0;
    public static final int GL_CLIP_PLANE0 = 0;
    public static final int GL_CLIP_PLANE1 = 0;
    public static final int GL_CLIP_PLANE2 = 0;
    public static final int GL_CLIP_PLANE3 = 0;
    public static final int GL_CLIP_PLANE4 = 0;
    public static final int GL_CLIP_PLANE5 = 0;
    public static final int GL_COLOR_ARRAY = 32886;
    public static final int GL_COLOR_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_COLOR_ARRAY_POINTER = 0;
    public static final int GL_COLOR_ARRAY_SIZE = 0;
    public static final int GL_COLOR_ARRAY_STRIDE = 0;
    public static final int GL_COLOR_ARRAY_TYPE = 0;
    public static final int GL_COLOR_BUFFER_BIT = 16384;
    public static final int GL_COLOR_CLEAR_VALUE = 0;
    public static final int GL_COLOR_LOGIC_OP = 0;
    public static final int GL_COLOR_MATERIAL = 0;
    public static final int GL_COLOR_WRITEMASK = 0;
    public static final int GL_COMBINE = 0;
    public static final int GL_COMBINE_ALPHA = 0;
    public static final int GL_COMBINE_RGB = 0;
    public static final int GL_COMPRESSED_TEXTURE_FORMATS = 0;
    public static final int GL_CONSTANT = 0;
    public static final int GL_CONSTANT_ATTENUATION = 0;
    public static final int GL_COORD_REPLACE_OES = 0;
    public static final int GL_COPY = 0;
    public static final int GL_COPY_INVERTED = 0;
    public static final int GL_CULL_FACE = 0;
    public static final int GL_CULL_FACE_MODE = 0;
    public static final int GL_CURRENT_COLOR = 0;
    public static final int GL_CURRENT_NORMAL = 0;
    public static final int GL_CURRENT_PALETTE_MATRIX_OES = 0;
    public static final int GL_CURRENT_TEXTURE_COORDS = 0;
    public static final int GL_CW = 0;
    public static final int GL_DECAL = 0;
    public static final int GL_DECR = 0;
    public static final int GL_DEPTH_BITS = 0;
    public static final int GL_DEPTH_BUFFER_BIT = 0;
    public static final int GL_DEPTH_CLEAR_VALUE = 0;
    public static final int GL_DEPTH_FUNC = 0;
    public static final int GL_DEPTH_RANGE = 0;
    public static final int GL_DEPTH_TEST = 0;
    public static final int GL_DEPTH_WRITEMASK = 0;
    public static final int GL_DIFFUSE = 0;
    public static final int GL_DITHER = 0;
    public static final int GL_DONT_CARE = 0;
    public static final int GL_DOT3_RGB = 0;
    public static final int GL_DOT3_RGBA = 0;
    public static final int GL_DST_ALPHA = 0;
    public static final int GL_DST_COLOR = 0;
    public static final int GL_DYNAMIC_DRAW = 35048;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 34963;
    public static final int GL_ELEMENT_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_EMISSION = 0;
    public static final int GL_EQUAL = 0;
    public static final int GL_EQUIV = 0;
    public static final int GL_EXP = 0;
    public static final int GL_EXP2 = 0;
    public static final int GL_EXTENSIONS = 7939;
    public static final int GL_FALSE = 0;
    public static final int GL_FASTEST = 0;
    public static final int GL_FLAT = 0;
    public static final int GL_FLOAT = 5126;
    public static final int GL_FOG = 0;
    public static final int GL_FOG_COLOR = 0;
    public static final int GL_FOG_DENSITY = 0;
    public static final int GL_FOG_END = 0;
    public static final int GL_FOG_HINT = 0;
    public static final int GL_FOG_MODE = 0;
    public static final int GL_FOG_START = 0;
    public static final int GL_FRONT = 0;
    public static final int GL_FRONT_AND_BACK = 0;
    public static final int GL_FRONT_FACE = 0;
    public static final int GL_GENERATE_MIPMAP = 0;
    public static final int GL_GENERATE_MIPMAP_HINT = 0;
    public static final int GL_GEQUAL = 0;
    public static final int GL_GREATER = 0;
    public static final int GL_GREEN_BITS = 0;
    public static final int GL_INCR = 0;
    public static final int GL_INTERPOLATE = 0;
    public static final int GL_INVALID_ENUM = 1280;
    public static final int GL_INVALID_OPERATION = 1282;
    public static final int GL_INVALID_VALUE = 1281;
    public static final int GL_INVERT = 0;
    public static final int GL_KEEP = 0;
    public static final int GL_LEQUAL = 0;
    public static final int GL_LESS = 0;
    public static final int GL_LIGHT_MODEL_AMBIENT = 0;
    public static final int GL_LIGHT_MODEL_TWO_SIDE = 0;
    public static final int GL_LIGHT0 = 0;
    public static final int GL_LIGHT1 = 0;
    public static final int GL_LIGHT2 = 0;
    public static final int GL_LIGHT3 = 0;
    public static final int GL_LIGHT4 = 0;
    public static final int GL_LIGHT5 = 0;
    public static final int GL_LIGHT6 = 0;
    public static final int GL_LIGHT7 = 0;
    public static final int GL_LIGHTING = 0;
    public static final int GL_LINE_LOOP = 2;
    public static final int GL_LINE_SMOOTH = 0;
    public static final int GL_LINE_SMOOTH_HINT = 0;
    public static final int GL_LINE_STRIP = 3;
    public static final int GL_LINE_WIDTH = 0;
    public static final int GL_LINEAR = 0;
    public static final int GL_LINEAR_ATTENUATION = 0;
    public static final int GL_LINEAR_MIPMAP_LINEAR = 0;
    public static final int GL_LINEAR_MIPMAP_NEAREST = 0;
    public static final int GL_LINES = 1;
    public static final int GL_LOGIC_OP_MODE = 0;
    public static final int GL_LUMINANCE = 0;
    public static final int GL_LUMINANCE_ALPHA = 0;
    public static final int GL_MATRIX_INDEX_ARRAY_BUFFER_BINDING_OES = 0;
    public static final int GL_MATRIX_INDEX_ARRAY_OES = 34884;
    public static final int GL_MATRIX_INDEX_ARRAY_POINTER_OES = 0;
    public static final int GL_MATRIX_INDEX_ARRAY_SIZE_OES = 0;
    public static final int GL_MATRIX_INDEX_ARRAY_STRIDE_OES = 0;
    public static final int GL_MATRIX_INDEX_ARRAY_TYPE_OES = 0;
    public static final int GL_MATRIX_MODE = 0;
    public static final int GL_MATRIX_PALETTE_OES = 34880;
    public static final int GL_MAX_CLIP_PLANES = 0;
    public static final int GL_MAX_LIGHTS = 0;
    public static final int GL_MAX_MODELVIEW_STACK_DEPTH = 0;
    public static final int GL_MAX_PALETTE_MATRICES_OES = 0;
    public static final int GL_MAX_PROJECTION_STACK_DEPTH = 0;
    public static final int GL_MAX_TEXTURE_SIZE = 0;
    public static final int GL_MAX_TEXTURE_STACK_DEPTH = 0;
    public static final int GL_MAX_TEXTURE_UNITS = 0;
    public static final int GL_MAX_VERTEX_UNITS_OES = 0;
    public static final int GL_MAX_VIEWPORT_DIMS = 0;
    public static final int GL_MODELVIEW = 5888;
    public static final int GL_MODELVIEW_MATRIX = 0;
    public static final int GL_MODELVIEW_MATRIX_FLOAT_AS_INT_BITS_OES = 0;
    public static final int GL_MODELVIEW_STACK_DEPTH = 0;
    public static final int GL_MODULATE = 0;
    public static final int GL_MULTISAMPLE = 0;
    public static final int GL_NAND = 0;
    public static final int GL_NEAREST = 0;
    public static final int GL_NEAREST_MIPMAP_LINEAR = 0;
    public static final int GL_NEAREST_MIPMAP_NEAREST = 0;
    public static final int GL_NEVER = 0;
    public static final int GL_NICEST = 0;
    public static final int GL_NO_ERROR = 0;
    public static final int GL_NOOP = 0;
    public static final int GL_NOR = 0;
    public static final int GL_NORMAL_ARRAY = 32885;
    public static final int GL_NORMAL_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_NORMAL_ARRAY_POINTER = 0;
    public static final int GL_NORMAL_ARRAY_STRIDE = 0;
    public static final int GL_NORMAL_ARRAY_TYPE = 0;
    public static final int GL_NORMALIZE = 0;
    public static final int GL_NOTEQUAL = 0;
    public static final int GL_NUM_COMPRESSED_TEXTURE_FORMATS = 0;
    public static final int GL_ONE = 0;
    public static final int GL_ONE_MINUS_DST_ALPHA = 0;
    public static final int GL_ONE_MINUS_DST_COLOR = 0;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0;
    public static final int GL_ONE_MINUS_SRC_COLOR = 0;
    public static final int GL_OPERAND0_ALPHA = 0;
    public static final int GL_OPERAND0_RGB = 0;
    public static final int GL_OPERAND1_ALPHA = 0;
    public static final int GL_OPERAND1_RGB = 0;
    public static final int GL_OPERAND2_ALPHA = 0;
    public static final int GL_OPERAND2_RGB = 0;
    public static final int GL_OR = 0;
    public static final int GL_OR_INVERTED = 0;
    public static final int GL_OR_REVERSE = 0;
    public static final int GL_OUT_OF_MEMORY = 0;
    public static final int GL_PACK_ALIGNMENT = 0;
    public static final int GL_PALETTE4_R5_G6_B5_OES = 0;
    public static final int GL_PALETTE4_RGB5_A1_OES = 0;
    public static final int GL_PALETTE4_RGB8_OES = 0;
    public static final int GL_PALETTE4_RGBA4_OES = 0;
    public static final int GL_PALETTE4_RGBA8_OES = 0;
    public static final int GL_PALETTE8_R5_G6_B5_OES = 0;
    public static final int GL_PALETTE8_RGB5_A1_OES = 0;
    public static final int GL_PALETTE8_RGB8_OES = 0;
    public static final int GL_PALETTE8_RGBA4_OES = 0;
    public static final int GL_PALETTE8_RGBA8_OES = 0;
    public static final int GL_PERSPECTIVE_CORRECTION_HINT = 0;
    public static final int GL_POINT_DISTANCE_ATTENUATION = 0;
    public static final int GL_POINT_FADE_THRESHOLD_SIZE = 0;
    public static final int GL_POINT_SIZE = 0;
    public static final int GL_POINT_SIZE_ARRAY_BUFFER_BINDING_OES = 0;
    public static final int GL_POINT_SIZE_ARRAY_OES = 35740;
    public static final int GL_POINT_SIZE_ARRAY_POINTER_OES = 0;
    public static final int GL_POINT_SIZE_ARRAY_STRIDE_OES = 0;
    public static final int GL_POINT_SIZE_ARRAY_TYPE_OES = 0;
    public static final int GL_POINT_SIZE_MAX = 0;
    public static final int GL_POINT_SIZE_MIN = 0;
    public static final int GL_POINT_SMOOTH = 0;
    public static final int GL_POINT_SMOOTH_HINT = 0;
    public static final int GL_POINT_SPRITE_OES = 0;
    public static final int GL_POINTS = 0;
    public static final int GL_POLYGON_OFFSET_FACTOR = 0;
    public static final int GL_POLYGON_OFFSET_FILL = 0;
    public static final int GL_POLYGON_OFFSET_UNITS = 0;
    public static final int GL_POLYGON_SMOOTH_HINT = 0;
    public static final int GL_POSITION = 0;
    public static final int GL_PREVIOUS = 0;
    public static final int GL_PRIMARY_COLOR = 0;
    public static final int GL_PROJECTION = 5889;
    public static final int GL_PROJECTION_MATRIX = 0;
    public static final int GL_PROJECTION_MATRIX_FLOAT_AS_INT_BITS_OES = 0;
    public static final int GL_PROJECTION_STACK_DEPTH = 0;
    public static final int GL_QUADRATIC_ATTENUATION = 0;
    public static final int GL_RED_BITS = 0;
    public static final int GL_RENDERER = 7937;
    public static final int GL_REPEAT = 0;
    public static final int GL_REPLACE = 0;
    public static final int GL_RESCALE_NORMAL = 0;
    public static final int GL_RGB = 0;
    public static final int GL_RGB_SCALE = 0;
    public static final int GL_RGBA = 0;
    public static final int GL_SAMPLE_ALPHA_TO_COVERAGE = 0;
    public static final int GL_SAMPLE_ALPHA_TO_ONE = 0;
    public static final int GL_SAMPLE_BUFFERS = 0;
    public static final int GL_SAMPLE_COVERAGE = 0;
    public static final int GL_SAMPLE_COVERAGE_INVERT = 0;
    public static final int GL_SAMPLE_COVERAGE_VALUE = 0;
    public static final int GL_SAMPLES = 0;
    public static final int GL_SCISSOR_BOX = 0;
    public static final int GL_SCISSOR_TEST = 0;
    public static final int GL_SET = 0;
    public static final int GL_SHADE_MODEL = 0;
    public static final int GL_SHININESS = 0;
    public static final int GL_SHORT = 5122;
    public static final int GL_SMOOTH = 0;
    public static final int GL_SMOOTH_LINE_WIDTH_RANGE = 0;
    public static final int GL_SMOOTH_POINT_SIZE_RANGE = 0;
    public static final int GL_SPECULAR = 0;
    public static final int GL_SPOT_CUTOFF = 0;
    public static final int GL_SPOT_DIRECTION = 0;
    public static final int GL_SPOT_EXPONENT = 0;
    public static final int GL_SRC_ALPHA = 0;
    public static final int GL_SRC_ALPHA_SATURATE = 0;
    public static final int GL_SRC_COLOR = 0;
    public static final int GL_SRC0_ALPHA = 0;
    public static final int GL_SRC0_RGB = 0;
    public static final int GL_SRC1_ALPHA = 0;
    public static final int GL_SRC1_RGB = 0;
    public static final int GL_SRC2_ALPHA = 0;
    public static final int GL_SRC2_RGB = 0;
    public static final int GL_STACK_OVERFLOW = 0;
    public static final int GL_STACK_UNDERFLOW = 0;
    public static final int GL_STATIC_DRAW = 35044;
    public static final int GL_STENCIL_BITS = 0;
    public static final int GL_STENCIL_BUFFER_BIT = 0;
    public static final int GL_STENCIL_CLEAR_VALUE = 0;
    public static final int GL_STENCIL_FAIL = 0;
    public static final int GL_STENCIL_FUNC = 0;
    public static final int GL_STENCIL_PASS_DEPTH_FAIL = 0;
    public static final int GL_STENCIL_PASS_DEPTH_PASS = 0;
    public static final int GL_STENCIL_REF = 0;
    public static final int GL_STENCIL_TEST = 0;
    public static final int GL_STENCIL_VALUE_MASK = 0;
    public static final int GL_STENCIL_WRITEMASK = 0;
    public static final int GL_SUBPIXEL_BITS = 0;
    public static final int GL_SUBTRACT = 0;
    public static final int GL_TEXTURE = 5890;
    public static final int GL_TEXTURE_2D = 3553;
    public static final int GL_TEXTURE_BINDING_2D = 0;
    public static final int GL_TEXTURE_COORD_ARRAY = 32888;
    public static final int GL_TEXTURE_COORD_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_TEXTURE_COORD_ARRAY_POINTER = 0;
    public static final int GL_TEXTURE_COORD_ARRAY_SIZE = 0;
    public static final int GL_TEXTURE_COORD_ARRAY_STRIDE = 0;
    public static final int GL_TEXTURE_COORD_ARRAY_TYPE = 0;
    public static final int GL_TEXTURE_CROP_RECT_OES = 0;
    public static final int GL_TEXTURE_ENV = 0;
    public static final int GL_TEXTURE_ENV_COLOR = 0;
    public static final int GL_TEXTURE_ENV_MODE = 0;
    public static final int GL_TEXTURE_MAG_FILTER = 0;
    public static final int GL_TEXTURE_MATRIX = 0;
    public static final int GL_TEXTURE_MATRIX_FLOAT_AS_INT_BITS_OES = 0;
    public static final int GL_TEXTURE_MIN_FILTER = 0;
    public static final int GL_TEXTURE_STACK_DEPTH = 0;
    public static final int GL_TEXTURE_WRAP_S = 0;
    public static final int GL_TEXTURE_WRAP_T = 0;
    public static final int GL_TEXTURE0 = 33984;
    public static final int GL_TEXTURE1 = 33985;
    public static final int GL_TEXTURE10 = 33994;
    public static final int GL_TEXTURE11 = 33995;
    public static final int GL_TEXTURE12 = 33996;
    public static final int GL_TEXTURE13 = 33997;
    public static final int GL_TEXTURE14 = 33998;
    public static final int GL_TEXTURE15 = 33999;
    public static final int GL_TEXTURE16 = 34000;
    public static final int GL_TEXTURE17 = 34001;
    public static final int GL_TEXTURE18 = 34002;
    public static final int GL_TEXTURE19 = 34003;
    public static final int GL_TEXTURE2 = 33986;
    public static final int GL_TEXTURE20 = 34004;
    public static final int GL_TEXTURE21 = 34005;
    public static final int GL_TEXTURE22 = 34006;
    public static final int GL_TEXTURE23 = 34007;
    public static final int GL_TEXTURE24 = 34008;
    public static final int GL_TEXTURE25 = 34009;
    public static final int GL_TEXTURE26 = 34010;
    public static final int GL_TEXTURE27 = 34011;
    public static final int GL_TEXTURE28 = 34012;
    public static final int GL_TEXTURE29 = 34013;
    public static final int GL_TEXTURE3 = 33987;
    public static final int GL_TEXTURE30 = 34014;
    public static final int GL_TEXTURE31 = 34015;
    public static final int GL_TEXTURE4 = 33988;
    public static final int GL_TEXTURE5 = 33989;
    public static final int GL_TEXTURE6 = 33990;
    public static final int GL_TEXTURE7 = 33991;
    public static final int GL_TEXTURE8 = 33992;
    public static final int GL_TEXTURE9 = 33993;
    public static final int GL_TRIANGLE_FAN = 6;
    public static final int GL_TRIANGLE_STRIP = 5;
    public static final int GL_TRIANGLES = 4;
    public static final int GL_TRUE = 0;
    public static final int GL_UNPACK_ALIGNMENT = 0;
    public static final int GL_UNSIGNED_BYTE = 5121;
    public static final int GL_UNSIGNED_SHORT = 5123;
    public static final int GL_UNSIGNED_SHORT_4_4_4_4 = 0;
    public static final int GL_UNSIGNED_SHORT_5_5_5_1 = 0;
    public static final int GL_UNSIGNED_SHORT_5_6_5 = 0;
    public static final int GL_VENDOR = 7936;
    public static final int GL_VERSION = 7938;
    public static final int GL_VERTEX_ARRAY = 32884;
    public static final int GL_VERTEX_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_VERTEX_ARRAY_POINTER = 0;
    public static final int GL_VERTEX_ARRAY_SIZE = 0;
    public static final int GL_VERTEX_ARRAY_STRIDE = 0;
    public static final int GL_VERTEX_ARRAY_TYPE = 0;
    public static final int GL_VIEWPORT = 0;
    public static final int GL_WEIGHT_ARRAY_BUFFER_BINDING_OES = 0;
    public static final int GL_WEIGHT_ARRAY_OES = 34477;
    public static final int GL_WEIGHT_ARRAY_POINTER_OES = 0;
    public static final int GL_WEIGHT_ARRAY_SIZE_OES = 0;
    public static final int GL_WEIGHT_ARRAY_STRIDE_OES = 0;
    public static final int GL_WEIGHT_ARRAY_TYPE_OES = 0;
    public static final int GL_WRITE_ONLY = 0;
    public static final int GL_XOR = 0;
    public static final int GL_ZERO = 0;
    private static final String GL_EXTENSIONS_STRING =
            "GL_OES_point_size_array GL_OES_matrix_palette GL_OES_draw_texture "
                    + "GL_OES_compressed_paletted_texture GL_OES_point_sprite";

    private final Set<Integer> textures = new HashSet<>();
    private final Set<Integer> buffers = new HashSet<>();
    private final Set<Integer> enabledCaps = new HashSet<>();
    private final Set<Integer> enabledClientStates = new HashSet<>();
    private final Map<Integer, Integer> bufferSizes = new HashMap<>();
    private final Map<Integer, Integer> bufferUsages = new HashMap<>();
    private Object boundTarget;
    private int boundArrayBuffer;
    private int boundElementArrayBuffer;
    private int nextTextureId = 1;
    private int nextBufferId = 1;
    private int clearColorArgb = 0xFF000000;
    private int lastError = GL_NO_ERROR;
    private ClientArrayBinding colorArrayBinding;
    private ClientArrayBinding normalArrayBinding;
    private ClientArrayBinding texCoordArrayBinding;
    private ClientArrayBinding vertexArrayBinding;
    private ClientArrayBinding pointSizeArrayBinding;
    private ClientArrayBinding matrixIndexArrayBinding;
    private ClientArrayBinding weightArrayBinding;

    private static final class ClientArrayBinding {
        private final String label;
        private final boolean usesVbo;
        private final int componentCount;
        private final int type;
        private final int stride;
        private final int offset;
        private final int bufferId;
        private final Buffer pointer;

        private ClientArrayBinding(
                String label,
                boolean usesVbo,
                int componentCount,
                int type,
                int stride,
                int offset,
                int bufferId,
                Buffer pointer
        ) {
            this.label = label;
            this.usesVbo = usesVbo;
            this.componentCount = componentCount;
            this.type = type;
            this.stride = stride;
            this.offset = offset;
            this.bufferId = bufferId;
            this.pointer = pointer;
        }
    }

    private OpglGraphics() {
    }

    public static com.mexa.opgl.OpglGraphics getInstance () {
        OpglGraphics current = instance;
        if (current == null) {
            synchronized (OpglGraphics.class) {
                current = instance;
                if (current == null) {
                    current = new OpglGraphics();
                    instance = current;
                }
            }
        }
        remexa.probes.SdkStubSupport.log(
                "com.mexa.opgl.OpglGraphics",
                "getInstance",
                describeInstance(current)
        );
        return current;
    }

    public void bind (java.lang.Object target) {
        remexa.probes.SdkStubSupport.log(
                "com.mexa.opgl.OpglGraphics",
                "bind",
                describeInstance(this),
                target
        );
        if (target == null) {
            throw new NullPointerException("target");
        }
        if (!(target instanceof Graphics)) {
            throw new IllegalArgumentException("target");
        }
        if (boundTarget != null) {
            throw new IllegalStateException("target already bound");
        }
        boundTarget = target;
    }

    public void release () {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "release");
        boundTarget = null;
    }

    public void glActiveTexture (int texture) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glActiveTexture", texture);
    }

    public void glAlphaFunc (int func, float ref) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glAlphaFunc", func, ref);
    }

    public void glBindTexture (int target, int texture) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glBindTexture", target, texture);
        ensureBound();
    }

    public void glBlendFunc (int sfactor, int dfactor) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glBlendFunc", sfactor, dfactor);
    }

    public void glClear (int mask) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glClear", mask);
        ensureBound();
        if ((mask & 0x4000) != 0) {
            ((Graphics) boundTarget).clearSurface(clearColorArgb);
        }
    }

    public void glClearColor (float red, float green, float blue, float alpha) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glClearColor", red, green, blue, alpha);
        ensureBound();
        clearColorArgb = (clampColor(alpha) << 24)
                | (clampColor(red) << 16)
                | (clampColor(green) << 8)
                | clampColor(blue);
    }

    public void glClearDepthf (float depth) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glClearDepthf", depth);
    }

    public void glClearStencil (int s) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glClearStencil", s);
    }

    public void glClientActiveTexture (int texture) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glClientActiveTexture", texture);
    }

    public void glColor4f (float red, float green, float blue, float alpha) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glColor4f", red, green, blue, alpha);
    }

    public void glColorMask (boolean red, boolean green, boolean blue, boolean alpha) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glColorMask", red, green, blue, alpha);
    }

    public void glColorPointer (int size, int type, int stride, com.mexa.opgl.Buffer pointer) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glColorPointer", size, type, stride, pointer);
        ensureBound();
        requireArrayBufferDisabled();
        if (pointer == null) {
            throw new NullPointerException("pointer");
        }
        validateColorPointer(size, type, stride, pointer);
        colorArrayBinding = new ClientArrayBinding("color", false, size, type, stride, 0, 0, pointer);
    }

    public void glColorPointer (int size, int type, int stride, int offset) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glColorPointer", size, type, stride, offset);
        ensureBound();
        requireArrayBufferEnabled();
        validateColorPointer(size, type, stride, null);
        validateOffset(type, offset);
        colorArrayBinding = new ClientArrayBinding("color", true, size, type, stride, offset, boundArrayBuffer, null);
    }

    public void glCompressedTexImage2D (int target, int level, int internalformat, int width, int height, int border, com.mexa.opgl.ByteBuffer data) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glCompressedTexImage2D", target, level, internalformat, width, height, border, data);
    }

    public void glCompressedTexSubImage2D (int target, int level, int xoffset, int yoffset, int width, int height, int format, com.mexa.opgl.ByteBuffer data) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glCompressedTexSubImage2D", target, level, xoffset, yoffset, width, height, format, data);
    }

    public void glCopyTexImage2D (int target, int level, int internalformat, int x, int y, int width, int height, int border) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glCopyTexImage2D", target, level, internalformat, x, y, width, height, border);
    }

    public void glCopyTexSubImage2D (int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glCopyTexSubImage2D", target, level, xoffset, yoffset, x, y, width, height);
    }

    public void glCullFace (int mode) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glCullFace", mode);
    }

    public void glDeleteTextures (int[] textures) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDeleteTextures", textures);
        ensureBound();
        if (textures == null) {
            throw new NullPointerException("textures");
        }
        if (textures.length == 0) {
            throw new IllegalArgumentException("textures");
        }
        for (int texture : textures) {
            this.textures.remove(texture);
        }
    }

    public void glDepthFunc (int func) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDepthFunc", func);
    }

    public void glDepthMask (boolean flag) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDepthMask", flag);
    }

    public void glDepthRangef (float zNear, float zFar) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDepthRangef", zNear, zFar);
    }

    public void glDisable (int cap) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDisable", cap);
        ensureBound();
        enabledCaps.remove(cap);
    }

    public void glDisableClientState (int array) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDisableClientState", array);
        ensureBound();
        enabledClientStates.remove(array);
    }

    public void glDrawArrays (int mode, int first, int count) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDrawArrays", mode, first, count);
        ensureBound();
        if (first < 0) {
            throw new IllegalArgumentException("first");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count");
        }
        if (!isSupportedDrawMode(mode)) {
            setError(GL_INVALID_ENUM);
            return;
        }
        boolean usesVbo = requireConsistentEnabledArrays();
        validateDrawArrayRange(first, count, usesVbo);
    }

    public void glDrawElements (int mode, int type, com.mexa.opgl.Buffer indices) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDrawElements", mode, type, indices);
        ensureBound();
        if (boundElementArrayBuffer != 0) {
            throw new IllegalStateException("VBO active");
        }
        if (indices == null) {
            throw new NullPointerException("indices");
        }
        validateElementIndexBuffer(type, indices);
        if (!isSupportedDrawMode(mode)) {
            setError(GL_INVALID_ENUM);
            return;
        }
        if (requireConsistentEnabledArrays()) {
            throw new IllegalStateException("enabled arrays require VBO draw");
        }
        validateDrawElementsIndices(type, indices);
    }

    public void glDrawElements (int mode, int count, int type, int offset) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDrawElements", mode, count, type, offset);
        ensureBound();
        if (boundElementArrayBuffer == 0) {
            throw new IllegalStateException("VBO not active");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count");
        }
        validateElementIndexType(type);
        validateOffset(type, offset);
        if (!isSupportedDrawMode(mode)) {
            setError(GL_INVALID_ENUM);
            return;
        }
        if (!requireConsistentEnabledArrays()) {
            throw new IllegalStateException("enabled arrays require buffer draw");
        }
    }

    public void glEnable (int cap) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glEnable", cap);
        ensureBound();
        enabledCaps.add(cap);
    }

    public void glEnableClientState (int array) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glEnableClientState", array);
        ensureBound();
        enabledClientStates.add(array);
    }

    public void glFlush () {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glFlush");
        ensureBound();
    }

    public void glFogf (int pname, float param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glFogf", pname, param);
    }

    public void glFogfv (int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glFogfv", pname, params);
    }

    public void glFrontFace (int mode) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glFrontFace", mode);
    }

    public void glFrustumf (float left, float right, float bottom, float top, float zNear, float zFar) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glFrustumf", left, right, bottom, top, zNear, zFar);
    }

    public void glGenTextures (int[] textures) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGenTextures", textures);
        ensureBound();
        if (textures == null) {
            throw new NullPointerException("textures");
        }
        if (textures.length == 0) {
            throw new IllegalArgumentException("textures");
        }
        for (int index = 0; index < textures.length; index++) {
            int texture = nextTextureId++;
            this.textures.add(texture);
            textures[index] = texture;
        }
    }

    public int glGetError () {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetError");
        int error = lastError;
        lastError = GL_NO_ERROR;
        return error;
    }

    public void glGetIntegerv (int pname, int[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetIntegerv", pname, params);
        if (params == null) {
            throw new NullPointerException("params");
        }
    }

    public java.lang.String glGetString (int name) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetString", name);
        ensureBound();
        switch (name) {
            case GL_VENDOR:
                return "ReMEXA";
            case GL_RENDERER:
                return "ReMEXA-OPGL";
            case GL_VERSION:
                return "OpenGL ES-CM 1.1";
            case GL_EXTENSIONS:
                return GL_EXTENSIONS_STRING;
            default:
                setError(GL_INVALID_ENUM);
                return null;
        }
    }

    public void glHint (int target, int mode) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glHint", target, mode);
    }

    public void glLightModelf (int pname, float param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLightModelf", pname, param);
    }

    public void glLightModelfv (int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLightModelfv", pname, params);
    }

    public void glLightf (int light, int pname, float param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLightf", light, pname, param);
    }

    public void glLightfv (int light, int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLightfv", light, pname, params);
    }

    public void glLineWidth (float width) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLineWidth", width);
    }

    public void glLoadIdentity () {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLoadIdentity");
        ensureBound();
    }

    public void glLoadMatrixf (float[] m) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLoadMatrixf", m);
        ensureBound();
        if (m == null) {
            throw new NullPointerException("m");
        }
        if (m.length < 16) {
            throw new IllegalArgumentException("m");
        }
    }

    public void glLogicOp (int opcode) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLogicOp", opcode);
    }

    public void glMaterialf (int face, int pname, float param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glMaterialf", face, pname, param);
    }

    public void glMaterialfv (int face, int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glMaterialfv", face, pname, params);
    }

    public void glMatrixMode (int mode) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glMatrixMode", mode);
        ensureBound();
    }

    public void glMultMatrixf (float[] m) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glMultMatrixf", m);
        ensureBound();
        if (m == null) {
            throw new NullPointerException("m");
        }
        if (m.length < 16) {
            throw new IllegalArgumentException("m");
        }
    }

    public void glMultiTexCoord4f (int target, float s, float t, float r, float q) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glMultiTexCoord4f", target, s, t, r, q);
    }

    public void glNormal3f (float nx, float ny, float nz) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glNormal3f", nx, ny, nz);
    }

    public void glNormalPointer (int type, int stride, com.mexa.opgl.Buffer pointer) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glNormalPointer", type, stride, pointer);
        ensureBound();
        requireArrayBufferDisabled();
        if (pointer == null) {
            throw new NullPointerException("pointer");
        }
        validateNormalPointer(type, stride, pointer);
        normalArrayBinding = new ClientArrayBinding("normal", false, 3, type, stride, 0, 0, pointer);
    }

    public void glNormalPointer (int type, int stride, int offset) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glNormalPointer", type, stride, offset);
        ensureBound();
        requireArrayBufferEnabled();
        validateNormalPointer(type, stride, null);
        validateOffset(type, offset);
        normalArrayBinding = new ClientArrayBinding("normal", true, 3, type, stride, offset, boundArrayBuffer, null);
    }

    public void glOrthof (float left, float right, float bottom, float top, float zNear, float zFar) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glOrthof", left, right, bottom, top, zNear, zFar);
        ensureBound();
    }

    public void glPixelStorei (int pname, int param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glPixelStorei", pname, param);
        ensureBound();
    }

    public void glPointSize (float size) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glPointSize", size);
    }

    public void glPolygonOffset (float factor, float units) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glPolygonOffset", factor, units);
    }

    public void glPopMatrix () {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glPopMatrix");
    }

    public void glPushMatrix () {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glPushMatrix");
    }

    public void glRotatef (float angle, float x, float y, float z) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glRotatef", angle, x, y, z);
    }

    public void glSampleCoverage (float value, boolean invert) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glSampleCoverage", value, invert);
    }

    public void glScalef (float x, float y, float z) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glScalef", x, y, z);
    }

    public void glScissor (int x, int y, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glScissor", x, y, width, height);
    }

    public void glShadeModel (int mode) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glShadeModel", mode);
    }

    public void glStencilFunc (int func, int ref, int mask) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glStencilFunc", func, ref, mask);
    }

    public void glStencilMask (int mask) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glStencilMask", mask);
    }

    public void glStencilOp (int fail, int zfail, int zpass) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glStencilOp", fail, zfail, zpass);
    }

    public void glTexCoordPointer (int size, int type, int stride, com.mexa.opgl.Buffer pointer) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexCoordPointer", size, type, stride, pointer);
        ensureBound();
        requireArrayBufferDisabled();
        if (pointer == null) {
            throw new NullPointerException("pointer");
        }
        validateVertexLikePointer("texCoord", size, type, stride, pointer);
        texCoordArrayBinding = new ClientArrayBinding("texCoord", false, size, type, stride, 0, 0, pointer);
    }

    public void glTexCoordPointer (int size, int type, int stride, int offset) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexCoordPointer", size, type, stride, offset);
        ensureBound();
        requireArrayBufferEnabled();
        validateVertexLikePointer("texCoord", size, type, stride, null);
        validateOffset(type, offset);
        texCoordArrayBinding = new ClientArrayBinding("texCoord", true, size, type, stride, offset, boundArrayBuffer, null);
    }

    public void glTexEnvf (int target, int pname, float param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexEnvf", target, pname, param);
        ensureBound();
    }

    public void glTexEnvfv (int target, int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexEnvfv", target, pname, params);
    }

    public void glTexImage2D (int target, int level, int internalformat, int width, int height, int border, int format, int type, com.mexa.opgl.Buffer pixels) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexImage2D", target, level, internalformat, width, height, border, format, type, pixels);
        ensureBound();
    }

    public void glTexParameterf (int target, int pname, float param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexParameterf", target, pname, param);
        ensureBound();
    }

    public void glTexSubImage2D (int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, com.mexa.opgl.Buffer pixels) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexSubImage2D", target, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    public void glTranslatef (float x, float y, float z) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTranslatef", x, y, z);
        ensureBound();
    }

    public void glVertexPointer (int size, int type, int stride, com.mexa.opgl.Buffer pointer) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glVertexPointer", size, type, stride, pointer);
        ensureBound();
        requireArrayBufferDisabled();
        if (pointer == null) {
            throw new NullPointerException("pointer");
        }
        validateVertexLikePointer("vertex", size, type, stride, pointer);
        vertexArrayBinding = new ClientArrayBinding("vertex", false, size, type, stride, 0, 0, pointer);
    }

    public void glVertexPointer (int size, int type, int stride, int offset) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glVertexPointer", size, type, stride, offset);
        ensureBound();
        requireArrayBufferEnabled();
        validateVertexLikePointer("vertex", size, type, stride, null);
        validateOffset(type, offset);
        vertexArrayBinding = new ClientArrayBinding("vertex", true, size, type, stride, offset, boundArrayBuffer, null);
    }

    public void glViewport (int x, int y, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glViewport", x, y, width, height);
        ensureBound();
    }

    public void glBindBuffer (int target, int buffer) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glBindBuffer", target, buffer);
        ensureBound();
        if (target != GL_ARRAY_BUFFER && target != GL_ELEMENT_ARRAY_BUFFER) {
            setError(GL_INVALID_ENUM);
            return;
        }
        if (buffer != 0 && !buffers.contains(buffer)) {
            setError(GL_INVALID_VALUE);
            return;
        }
        if (target == GL_ARRAY_BUFFER) {
            boundArrayBuffer = buffer;
        } else {
            boundElementArrayBuffer = buffer;
        }
    }

    public void glBufferData (int target, com.mexa.opgl.Buffer data, int usage) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glBufferData", target, data, usage);
        ensureBound();
        if (data == null) {
            throw new NullPointerException("data");
        }
        if (usage != GL_STATIC_DRAW && usage != GL_DYNAMIC_DRAW) {
            setError(GL_INVALID_ENUM);
            return;
        }
        int bufferId;
        if (target == GL_ARRAY_BUFFER) {
            bufferId = boundArrayBuffer;
        } else if (target == GL_ELEMENT_ARRAY_BUFFER) {
            bufferId = boundElementArrayBuffer;
        } else {
            setError(GL_INVALID_ENUM);
            return;
        }
        if (bufferId == 0) {
            setError(GL_INVALID_OPERATION);
            return;
        }
        bufferSizes.put(bufferId, bufferLengthInBytes(data));
        bufferUsages.put(bufferId, usage);
    }

    public void glBufferSubData (int target, int offset, com.mexa.opgl.Buffer data) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glBufferSubData", target, offset, data);
    }

    public void glClipPlanef (int plane, float[] equation) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glClipPlanef", plane, equation);
    }

    public void glColor4ub (byte red, byte green, byte blue, byte alpha) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glColor4ub", red, green, blue, alpha);
        ensureBound();
    }

    public void glDeleteBuffers (int[] buffers) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDeleteBuffers", buffers);
        ensureBound();
        if (buffers == null) {
            throw new NullPointerException("buffers");
        }
        if (buffers.length == 0) {
            throw new IllegalArgumentException("buffers");
        }
        for (int buffer : buffers) {
            this.buffers.remove(buffer);
            bufferSizes.remove(buffer);
            bufferUsages.remove(buffer);
            if (boundArrayBuffer == buffer) {
                boundArrayBuffer = 0;
            }
            if (boundElementArrayBuffer == buffer) {
                boundElementArrayBuffer = 0;
            }
        }
    }

    public void glGetBooleanv (int pname, boolean[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetBooleanv", pname, params);
    }

    public void glGetBufferParameteriv (int target, int pname, int[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetBufferParameteriv", target, pname, params);
    }

    public void glGetClipPlanef (int pname, float[] equation) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetClipPlanef", pname, equation);
    }

    public void glGetFloatv (int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetFloatv", pname, params);
    }

    public void glGetLightfv (int light, int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetLightfv", light, pname, params);
    }

    public void glGetMaterialfv (int face, int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetMaterialfv", face, pname, params);
    }

    public void glGetTexEnvfv (int env, int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetTexEnvfv", env, pname, params);
    }

    public void glGetTexParameterfv (int target, int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetTexParameterfv", target, pname, params);
    }

    public void glGenBuffers (int[] buffers) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGenBuffers", buffers);
        ensureBound();
        if (buffers == null) {
            throw new NullPointerException("buffers");
        }
        if (buffers.length == 0) {
            throw new IllegalArgumentException("buffers");
        }
        for (int index = 0; index < buffers.length; index++) {
            int buffer = nextBufferId++;
            this.buffers.add(buffer);
            buffers[index] = buffer;
        }
    }

    public void glGetTexEnviv (int env, int pname, int[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetTexEnviv", env, pname, params);
    }

    public void glGetTexParameteriv (int target, int pname, int[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetTexParameteriv", target, pname, params);
    }

    public boolean glIsBuffer (int buffer) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glIsBuffer", buffer);
        ensureBound();
        return buffers.contains(buffer);
    }

    public boolean glIsEnabled (int cap) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glIsEnabled", cap);
        ensureBound();
        return enabledClientStates.contains(cap) || enabledCaps.contains(cap);
    }

    public boolean glIsTexture (int texture) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glIsTexture", texture);
        ensureBound();
        return textures.contains(texture);
    }

    public void glPointParameterf (int pname, float param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glPointParameterf", pname, param);
    }

    public void glPointParameterfv (int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glPointParameterfv", pname, params);
    }

    public void glTexEnvi (int target, int pname, int param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexEnvi", target, pname, param);
    }

    public void glTexEnviv (int target, int pname, int[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexEnviv", target, pname, params);
    }

    public void glTexParameterfv (int target, int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexParameterfv", target, pname, params);
    }

    public void glTexParameteri (int target, int pname, int param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexParameteri", target, pname, param);
    }

    public void glTexParameteriv (int target, int pname, int[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexParameteriv", target, pname, params);
    }

    public void glPointSizePointerOES (int type, int stride, com.mexa.opgl.Buffer pointer) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glPointSizePointerOES", type, stride, pointer);
        ensureBound();
        requireArrayBufferDisabled();
        if (pointer == null) {
            throw new NullPointerException("pointer");
        }
        validatePointSizePointer(type, stride, pointer);
        pointSizeArrayBinding = new ClientArrayBinding("pointSize", false, 1, type, stride, 0, 0, pointer);
    }

    public void glPointSizePointerOES (int type, int stride, int offset) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glPointSizePointerOES", type, stride, offset);
        ensureBound();
        requireArrayBufferEnabled();
        validatePointSizePointer(type, stride, null);
        validateOffset(type, offset);
        pointSizeArrayBinding = new ClientArrayBinding("pointSize", true, 1, type, stride, offset, boundArrayBuffer, null);
    }

    public void glCurrentPaletteMatrixOES (int index) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glCurrentPaletteMatrixOES", index);
        ensureBound();
    }

    public void glLoadPaletteFromModelViewMatrixOES () {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLoadPaletteFromModelViewMatrixOES");
        ensureBound();
    }

    public void glMatrixIndexPointerOES (int size, int type, int stride, com.mexa.opgl.Buffer pointer) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glMatrixIndexPointerOES", size, type, stride, pointer);
        ensureBound();
        requireArrayBufferDisabled();
        if (pointer == null) {
            throw new NullPointerException("pointer");
        }
        validateMatrixIndexPointer(type, stride, pointer);
        matrixIndexArrayBinding = new ClientArrayBinding("matrixIndex", false, size, type, stride, 0, 0, pointer);
    }

    public void glMatrixIndexPointerOES (int size, int type, int stride, int offset) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glMatrixIndexPointerOES", size, type, stride, offset);
        ensureBound();
        requireArrayBufferEnabled();
        validateMatrixIndexPointer(type, stride, null);
        if (offset < 0) {
            throw new IllegalArgumentException("offset");
        }
        matrixIndexArrayBinding = new ClientArrayBinding("matrixIndex", true, size, type, stride, offset, boundArrayBuffer, null);
    }

    public void glWeightPointerOES (int size, int type, int stride, com.mexa.opgl.Buffer pointer) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glWeightPointerOES", size, type, stride, pointer);
        ensureBound();
        requireArrayBufferDisabled();
        if (pointer == null) {
            throw new NullPointerException("pointer");
        }
        validateWeightPointer(type, stride, pointer);
        weightArrayBinding = new ClientArrayBinding("weight", false, size, type, stride, 0, 0, pointer);
    }

    public void glWeightPointerOES (int size, int type, int stride, int offset) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glWeightPointerOES", size, type, stride, offset);
        ensureBound();
        requireArrayBufferEnabled();
        validateWeightPointer(type, stride, null);
        validateOffset(type, offset);
        weightArrayBinding = new ClientArrayBinding("weight", true, size, type, stride, offset, boundArrayBuffer, null);
    }

    public void glDrawTexsOES (short x, short y, short z, short width, short height) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDrawTexsOES", x, y, z, width, height);
    }

    public void glDrawTexiOES (int x, int y, int z, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDrawTexiOES", x, y, z, width, height);
    }

    public void glDrawTexfOES (float x, float y, float z, float width, float height) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDrawTexfOES", x, y, z, width, height);
    }

    private void ensureBound() {
        if (boundTarget == null) {
            throw new IllegalStateException("bind not active");
        }
    }

    private void requireArrayBufferDisabled() {
        if (boundArrayBuffer != 0) {
            throw new IllegalStateException("VBO active");
        }
    }

    private void requireArrayBufferEnabled() {
        if (boundArrayBuffer == 0) {
            throw new IllegalStateException("VBO not active");
        }
    }

    private void validateColorPointer(int size, int type, int stride, Buffer pointer) {
        if (size != 4) {
            throw new IllegalArgumentException("size");
        }
        if (type != GL_UNSIGNED_BYTE && type != GL_FLOAT) {
            throw new IllegalArgumentException("type");
        }
        if (stride < 0) {
            throw new IllegalArgumentException("stride");
        }
        if (type == GL_FLOAT && stride % 4 != 0) {
            throw new IllegalArgumentException("stride");
        }
        if (pointer != null) {
            validatePointerType(type, pointer);
        }
    }

    private void validateNormalPointer(int type, int stride, Buffer pointer) {
        if (type != GL_BYTE && type != GL_SHORT && type != GL_FLOAT) {
            throw new IllegalArgumentException("type");
        }
        if (stride < 0) {
            throw new IllegalArgumentException("stride");
        }
        if (type == GL_SHORT && stride % 2 != 0) {
            throw new IllegalArgumentException("stride");
        }
        if (type == GL_FLOAT && stride % 4 != 0) {
            throw new IllegalArgumentException("stride");
        }
        if (pointer != null) {
            validatePointerType(type, pointer);
        }
    }

    private void validateVertexLikePointer(String label, int size, int type, int stride, Buffer pointer) {
        if (size < 2 || size > 4) {
            throw new IllegalArgumentException(label + ".size");
        }
        if (type != GL_BYTE && type != GL_SHORT && type != GL_FLOAT) {
            throw new IllegalArgumentException(label + ".type");
        }
        if (stride < 0) {
            throw new IllegalArgumentException(label + ".stride");
        }
        if (type == GL_SHORT && stride % 2 != 0) {
            throw new IllegalArgumentException(label + ".stride");
        }
        if (type == GL_FLOAT && stride % 4 != 0) {
            throw new IllegalArgumentException(label + ".stride");
        }
        if (pointer != null) {
            validatePointerType(type, pointer);
        }
    }

    private void validatePointSizePointer(int type, int stride, Buffer pointer) {
        if (type != GL_FLOAT) {
            throw new IllegalArgumentException("type");
        }
        if (stride < 0 || stride % 4 != 0) {
            throw new IllegalArgumentException("stride");
        }
        if (pointer != null && !(pointer instanceof FloatBuffer)) {
            throw new IllegalArgumentException("pointer");
        }
    }

    private void validateMatrixIndexPointer(int type, int stride, Buffer pointer) {
        if (type != GL_UNSIGNED_BYTE) {
            throw new IllegalArgumentException("type");
        }
        if (stride < 0) {
            throw new IllegalArgumentException("stride");
        }
        if (pointer != null && !(pointer instanceof ByteBuffer)) {
            throw new IllegalArgumentException("pointer");
        }
    }

    private void validateWeightPointer(int type, int stride, Buffer pointer) {
        if (type != GL_FLOAT) {
            throw new IllegalArgumentException("type");
        }
        if (stride < 0 || stride % 4 != 0) {
            throw new IllegalArgumentException("stride");
        }
        if (pointer != null && !(pointer instanceof FloatBuffer)) {
            throw new IllegalArgumentException("pointer");
        }
    }

    private void validatePointerType(int type, Buffer pointer) {
        if (type == GL_BYTE && !(pointer instanceof ByteBuffer)) {
            throw new IllegalArgumentException("pointer");
        }
        if (type == GL_UNSIGNED_BYTE && !(pointer instanceof ByteBuffer)) {
            throw new IllegalArgumentException("pointer");
        }
        if (type == GL_SHORT && !(pointer instanceof ShortBuffer)) {
            throw new IllegalArgumentException("pointer");
        }
        if (type == GL_UNSIGNED_SHORT && !(pointer instanceof ShortBuffer)) {
            throw new IllegalArgumentException("pointer");
        }
        if (type == GL_FLOAT && !(pointer instanceof FloatBuffer)) {
            throw new IllegalArgumentException("pointer");
        }
        if (!(pointer instanceof ByteBuffer)
                && !(pointer instanceof ShortBuffer)
                && !(pointer instanceof FloatBuffer)) {
            throw new IllegalArgumentException("pointer");
        }
    }

    private void validateElementIndexBuffer(int type, Buffer indices) {
        validateElementIndexType(type);
        validatePointerType(type, indices);
    }

    private void validateElementIndexType(int type) {
        if (type != GL_UNSIGNED_BYTE && type != GL_UNSIGNED_SHORT) {
            throw new IllegalArgumentException("type");
        }
    }

    private void validateOffset(int type, int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset");
        }
        if ((type == GL_SHORT || type == GL_UNSIGNED_SHORT) && offset % 2 != 0) {
            throw new IllegalArgumentException("offset");
        }
        if (type == GL_FLOAT && offset % 4 != 0) {
            throw new IllegalArgumentException("offset");
        }
    }

    private boolean requireConsistentEnabledArrays() {
        ClientArrayBinding[] bindings = enabledBindings();
        if (bindings.length == 0) {
            return false;
        }
        boolean usesVbo = bindings[0].usesVbo;
        for (ClientArrayBinding binding : bindings) {
            if (binding.usesVbo != usesVbo) {
                throw new IllegalStateException("enabled arrays mix VBO and Buffer");
            }
        }
        return usesVbo;
    }

    private ClientArrayBinding[] enabledBindings() {
        java.util.ArrayList<ClientArrayBinding> bindings = new java.util.ArrayList<>();
        addEnabledBinding(bindings, GL_VERTEX_ARRAY, vertexArrayBinding);
        addEnabledBinding(bindings, GL_COLOR_ARRAY, colorArrayBinding);
        addEnabledBinding(bindings, GL_NORMAL_ARRAY, normalArrayBinding);
        addEnabledBinding(bindings, GL_TEXTURE_COORD_ARRAY, texCoordArrayBinding);
        addEnabledBinding(bindings, GL_POINT_SIZE_ARRAY_OES, pointSizeArrayBinding);
        addEnabledBinding(bindings, GL_MATRIX_INDEX_ARRAY_OES, matrixIndexArrayBinding);
        addEnabledBinding(bindings, GL_WEIGHT_ARRAY_OES, weightArrayBinding);
        return bindings.toArray(new ClientArrayBinding[0]);
    }

    private void addEnabledBinding(
            java.util.ArrayList<ClientArrayBinding> bindings,
            int array,
            ClientArrayBinding binding
    ) {
        if (!enabledClientStates.contains(array)) {
            return;
        }
        if (binding == null) {
            throw new IllegalStateException("enabled array not configured: " + array);
        }
        bindings.add(binding);
    }

    private void validateDrawArrayRange(int first, int count, boolean usesVbo) {
        int endExclusive = first + count;
        for (ClientArrayBinding binding : enabledBindings()) {
            int available = availableVertexCount(binding);
            if (endExclusive > available) {
                throw new IllegalArgumentException(
                        binding.label + " requires " + endExclusive + " vertices, has " + available
                );
            }
            if (usesVbo && binding.bufferId == 0) {
                throw new IllegalStateException("enabled arrays require VBO data");
            }
        }
    }

    private void validateDrawElementsIndices(int type, Buffer indices) {
        int maxIndex = maxIndex(type, indices);
        for (ClientArrayBinding binding : enabledBindings()) {
            int available = availableVertexCount(binding);
            if (maxIndex >= available) {
                throw new IllegalArgumentException(
                        binding.label + " index " + maxIndex + " exceeds available vertices " + available
                );
            }
        }
    }

    private int availableVertexCount(ClientArrayBinding binding) {
        int componentSize = sizeOfType(binding.type);
        if (componentSize <= 0 || binding.componentCount <= 0) {
            return 0;
        }
        int strideBytes = binding.stride == 0 ? componentSize * binding.componentCount : binding.stride;
        if (strideBytes <= 0) {
            return 0;
        }
        int availableBytes;
        if (binding.usesVbo) {
            Integer size = bufferSizes.get(binding.bufferId);
            if (size == null || binding.offset > size) {
                return 0;
            }
            availableBytes = size - binding.offset;
        } else {
            availableBytes = bufferLengthInBytes(binding.pointer);
        }
        int firstBytes = componentSize * binding.componentCount;
        if (availableBytes < firstBytes) {
            return 0;
        }
        return 1 + ((availableBytes - firstBytes) / strideBytes);
    }

    private int maxIndex(int type, Buffer indices) {
        if (type == GL_UNSIGNED_BYTE) {
            ByteBuffer byteBuffer = (ByteBuffer) indices;
            int length = byteBuffer.boundedLength();
            byte[] values = byteBuffer.get(byteBuffer.boundedOffset(), new byte[length], 0, length);
            int max = -1;
            for (byte value : values) {
                max = Math.max(max, value & 0xFF);
            }
            return max;
        }
        ShortBuffer shortBuffer = (ShortBuffer) indices;
        int length = shortBuffer.boundedLength();
        short[] values = shortBuffer.get(shortBuffer.boundedOffset(), new short[length], 0, length);
        int max = -1;
        for (short value : values) {
            max = Math.max(max, value & 0xFFFF);
        }
        return max;
    }

    private int bufferLengthInBytes(Buffer buffer) {
        return buffer.boundedLength() * sizeOfBuffer(buffer);
    }

    private int sizeOfBuffer(Buffer buffer) {
        if (buffer instanceof ByteBuffer) {
            return 1;
        }
        if (buffer instanceof ShortBuffer) {
            return 2;
        }
        if (buffer instanceof FloatBuffer) {
            return 4;
        }
        throw new IllegalArgumentException("Unsupported buffer: " + buffer.getClass().getName());
    }

    private int sizeOfType(int type) {
        switch (type) {
            case GL_BYTE:
            case GL_UNSIGNED_BYTE:
                return 1;
            case GL_SHORT:
            case GL_UNSIGNED_SHORT:
                return 2;
            case GL_FLOAT:
                return 4;
            default:
                return -1;
        }
    }

    private boolean isSupportedDrawMode(int mode) {
        return mode == GL_POINTS
                || mode == GL_LINE_STRIP
                || mode == GL_LINE_LOOP
                || mode == GL_LINES
                || mode == GL_TRIANGLE_STRIP
                || mode == GL_TRIANGLE_FAN
                || mode == GL_TRIANGLES;
    }

    private void setError(int error) {
        if (lastError == GL_NO_ERROR) {
            lastError = error;
        }
    }

    private static String describeInstance(OpglGraphics value) {
        if (value == null) {
            return "null";
        }
        return value.getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(value));
    }

    private static int clampColor(float component) {
        if (component <= 0.0f) {
            return 0;
        }
        if (component >= 1.0f) {
            return 255;
        }
        return Math.round(component * 255.0f);
    }
}
