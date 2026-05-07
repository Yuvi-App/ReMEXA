package com.mexa.opgl;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.microedition.lcdui.Graphics;
import remexa.host.jblend.CanvasGraphics3D;

public class OpglGraphics {
    private static volatile OpglGraphics instance;
    private static final int MAX_PALETTE_MATRICES = 32;
    private static final int MAX_TEXTURE_UNITS_IMPLEMENTED = 32;
    public static final int GL_ACTIVE_TEXTURE = 34016;
    public static final int GL_ADD = 260;
    public static final int GL_ADD_SIGNED = 34164;
    public static final int GL_ALIASED_LINE_WIDTH_RANGE = 33902;
    public static final int GL_ALIASED_POINT_SIZE_RANGE = 33901;
    public static final int GL_ALPHA = 6406;
    public static final int GL_ALPHA_BITS = 3413;
    public static final int GL_ALPHA_SCALE = 3356;
    public static final int GL_ALPHA_TEST = 3008;
    public static final int GL_ALPHA_TEST_FUNC = 3009;
    public static final int GL_ALPHA_TEST_REF = 3010;
    public static final int GL_ALWAYS = 519;
    public static final int GL_AMBIENT = 4608;
    public static final int GL_AMBIENT_AND_DIFFUSE = 5634;
    public static final int GL_AND = 5377;
    public static final int GL_AND_INVERTED = 5380;
    public static final int GL_AND_REVERSE = 5378;
    public static final int GL_ARRAY_BUFFER = 34962;
    public static final int GL_ARRAY_BUFFER_BINDING = 34964;
    public static final int GL_BACK = 1029;
    public static final int GL_BLEND = 3042;
    public static final int GL_BLEND_DST = 3040;
    public static final int GL_BLEND_SRC = 3041;
    public static final int GL_BLUE_BITS = 3412;
    public static final int GL_BUFFER_SIZE = 34660;
    public static final int GL_BUFFER_USAGE = 34661;
    public static final int GL_BYTE = 5120;
    public static final int GL_CCW = 2305;
    public static final int GL_CLAMP_TO_EDGE = 33071;
    public static final int GL_CLEAR = 5376;
    public static final int GL_CLIENT_ACTIVE_TEXTURE = 34017;
    public static final int GL_CLIP_PLANE0 = 12288;
    public static final int GL_CLIP_PLANE1 = 12289;
    public static final int GL_CLIP_PLANE2 = 12290;
    public static final int GL_CLIP_PLANE3 = 12291;
    public static final int GL_CLIP_PLANE4 = 12292;
    public static final int GL_CLIP_PLANE5 = 12293;
    public static final int GL_COLOR_ARRAY = 32886;
    public static final int GL_COLOR_ARRAY_BUFFER_BINDING = 34968;
    public static final int GL_COLOR_ARRAY_POINTER = 32912;
    public static final int GL_COLOR_ARRAY_SIZE = 32897;
    public static final int GL_COLOR_ARRAY_STRIDE = 32899;
    public static final int GL_COLOR_ARRAY_TYPE = 32898;
    public static final int GL_COLOR_BUFFER_BIT = 16384;
    public static final int GL_COLOR_CLEAR_VALUE = 3106;
    public static final int GL_COLOR_LOGIC_OP = 3058;
    public static final int GL_COLOR_MATERIAL = 2903;
    public static final int GL_COLOR_WRITEMASK = 3107;
    public static final int GL_COMBINE = 34160;
    public static final int GL_COMBINE_ALPHA = 34162;
    public static final int GL_COMBINE_RGB = 34161;
    public static final int GL_COMPRESSED_TEXTURE_FORMATS = 34467;
    public static final int GL_CONSTANT = 34166;
    public static final int GL_CONSTANT_ATTENUATION = 4615;
    public static final int GL_COORD_REPLACE_OES = 34914;
    public static final int GL_COPY = 5379;
    public static final int GL_COPY_INVERTED = 5388;
    public static final int GL_CULL_FACE = 2884;
    public static final int GL_CULL_FACE_MODE = 2885;
    public static final int GL_CURRENT_COLOR = 2816;
    public static final int GL_CURRENT_NORMAL = 2818;
    public static final int GL_CURRENT_PALETTE_MATRIX_OES = 34883;
    public static final int GL_CURRENT_TEXTURE_COORDS = 2819;
    public static final int GL_CW = 2304;
    public static final int GL_DECAL = 8449;
    public static final int GL_DECR = 7683;
    public static final int GL_DEPTH_BITS = 3414;
    public static final int GL_DEPTH_BUFFER_BIT = 256;
    public static final int GL_DEPTH_CLEAR_VALUE = 2931;
    public static final int GL_DEPTH_FUNC = 2932;
    public static final int GL_DEPTH_RANGE = 2928;
    public static final int GL_DEPTH_TEST = 2929;
    public static final int GL_DEPTH_WRITEMASK = 2930;
    public static final int GL_DIFFUSE = 4609;
    public static final int GL_DITHER = 3024;
    public static final int GL_DONT_CARE = 4352;
    public static final int GL_DOT3_RGB = 34478;
    public static final int GL_DOT3_RGBA = 34479;
    public static final int GL_DST_ALPHA = 772;
    public static final int GL_DST_COLOR = 774;
    public static final int GL_DYNAMIC_DRAW = 35048;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 34963;
    public static final int GL_ELEMENT_ARRAY_BUFFER_BINDING = 34965;
    public static final int GL_EMISSION = 5632;
    public static final int GL_EQUAL = 514;
    public static final int GL_EQUIV = 5385;
    public static final int GL_EXP = 2048;
    public static final int GL_EXP2 = 2049;
    public static final int GL_EXTENSIONS = 7939;
    public static final int GL_FALSE = 0;
    public static final int GL_FASTEST = 4353;
    public static final int GL_FLAT = 7424;
    public static final int GL_FLOAT = 5126;
    public static final int GL_FOG = 2912;
    public static final int GL_FOG_COLOR = 2918;
    public static final int GL_FOG_DENSITY = 2914;
    public static final int GL_FOG_END = 2916;
    public static final int GL_FOG_HINT = 3156;
    public static final int GL_FOG_MODE = 2917;
    public static final int GL_FOG_START = 2915;
    public static final int GL_FRONT = 1028;
    public static final int GL_FRONT_AND_BACK = 1032;
    public static final int GL_FRONT_FACE = 2886;
    public static final int GL_GENERATE_MIPMAP = 33169;
    public static final int GL_GENERATE_MIPMAP_HINT = 33170;
    public static final int GL_GEQUAL = 518;
    public static final int GL_GREATER = 516;
    public static final int GL_GREEN_BITS = 3411;
    public static final int GL_INCR = 7682;
    public static final int GL_INTERPOLATE = 34165;
    public static final int GL_INVALID_ENUM = 1280;
    public static final int GL_INVALID_OPERATION = 1282;
    public static final int GL_INVALID_VALUE = 1281;
    public static final int GL_INVERT = 5386;
    public static final int GL_KEEP = 7680;
    public static final int GL_LEQUAL = 515;
    public static final int GL_LESS = 513;
    public static final int GL_LIGHT_MODEL_AMBIENT = 2899;
    public static final int GL_LIGHT_MODEL_TWO_SIDE = 2898;
    public static final int GL_LIGHT0 = 16384;
    public static final int GL_LIGHT1 = 16385;
    public static final int GL_LIGHT2 = 16386;
    public static final int GL_LIGHT3 = 16387;
    public static final int GL_LIGHT4 = 16388;
    public static final int GL_LIGHT5 = 16389;
    public static final int GL_LIGHT6 = 16390;
    public static final int GL_LIGHT7 = 16391;
    public static final int GL_LIGHTING = 2896;
    public static final int GL_LINE_LOOP = 2;
    public static final int GL_LINE_SMOOTH = 2848;
    public static final int GL_LINE_SMOOTH_HINT = 3154;
    public static final int GL_LINE_STRIP = 3;
    public static final int GL_LINE_WIDTH = 2849;
    public static final int GL_LINEAR = 9729;
    public static final int GL_LINEAR_ATTENUATION = 4616;
    public static final int GL_LINEAR_MIPMAP_LINEAR = 9987;
    public static final int GL_LINEAR_MIPMAP_NEAREST = 9985;
    public static final int GL_LINES = 1;
    public static final int GL_LOGIC_OP_MODE = 3056;
    public static final int GL_LUMINANCE = 6409;
    public static final int GL_LUMINANCE_ALPHA = 6410;
    public static final int GL_MATRIX_INDEX_ARRAY_BUFFER_BINDING_OES = 35742;
    public static final int GL_MATRIX_INDEX_ARRAY_OES = 34884;
    public static final int GL_MATRIX_INDEX_ARRAY_POINTER_OES = 34889;
    public static final int GL_MATRIX_INDEX_ARRAY_SIZE_OES = 34886;
    public static final int GL_MATRIX_INDEX_ARRAY_STRIDE_OES = 34888;
    public static final int GL_MATRIX_INDEX_ARRAY_TYPE_OES = 34887;
    public static final int GL_MATRIX_MODE = 2976;
    public static final int GL_MATRIX_PALETTE_OES = 34880;
    public static final int GL_MAX_CLIP_PLANES = 3378;
    public static final int GL_MAX_LIGHTS = 3377;
    public static final int GL_MAX_MODELVIEW_STACK_DEPTH = 3382;
    public static final int GL_MAX_PALETTE_MATRICES_OES = 34882;
    public static final int GL_MAX_PROJECTION_STACK_DEPTH = 3384;
    public static final int GL_MAX_TEXTURE_SIZE = 3379;
    public static final int GL_MAX_TEXTURE_STACK_DEPTH = 3385;
    public static final int GL_MAX_TEXTURE_UNITS = 34018;
    public static final int GL_MAX_VERTEX_UNITS_OES = 34468;
    public static final int GL_MAX_VIEWPORT_DIMS = 3386;
    public static final int GL_MODELVIEW = 5888;
    public static final int GL_MODELVIEW_MATRIX = 2982;
    public static final int GL_MODELVIEW_MATRIX_FLOAT_AS_INT_BITS_OES = 35213;
    public static final int GL_MODELVIEW_STACK_DEPTH = 2979;
    public static final int GL_MODULATE = 8448;
    public static final int GL_MULTISAMPLE = 32925;
    public static final int GL_NAND = 5390;
    public static final int GL_NEAREST = 9728;
    public static final int GL_NEAREST_MIPMAP_LINEAR = 9986;
    public static final int GL_NEAREST_MIPMAP_NEAREST = 9984;
    public static final int GL_NEVER = 512;
    public static final int GL_NICEST = 4354;
    public static final int GL_NO_ERROR = 0;
    public static final int GL_NOOP = 5381;
    public static final int GL_NOR = 5384;
    public static final int GL_NORMAL_ARRAY = 32885;
    public static final int GL_NORMAL_ARRAY_BUFFER_BINDING = 34967;
    public static final int GL_NORMAL_ARRAY_POINTER = 32911;
    public static final int GL_NORMAL_ARRAY_STRIDE = 32895;
    public static final int GL_NORMAL_ARRAY_TYPE = 32894;
    public static final int GL_NORMALIZE = 2977;
    public static final int GL_NOTEQUAL = 517;
    public static final int GL_NUM_COMPRESSED_TEXTURE_FORMATS = 34466;
    public static final int GL_ONE = 1;
    public static final int GL_ONE_MINUS_DST_ALPHA = 773;
    public static final int GL_ONE_MINUS_DST_COLOR = 775;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 771;
    public static final int GL_ONE_MINUS_SRC_COLOR = 769;
    public static final int GL_OPERAND0_ALPHA = 34200;
    public static final int GL_OPERAND0_RGB = 34192;
    public static final int GL_OPERAND1_ALPHA = 34201;
    public static final int GL_OPERAND1_RGB = 34193;
    public static final int GL_OPERAND2_ALPHA = 34202;
    public static final int GL_OPERAND2_RGB = 34194;
    public static final int GL_OR = 5383;
    public static final int GL_OR_INVERTED = 5389;
    public static final int GL_OR_REVERSE = 5387;
    public static final int GL_OUT_OF_MEMORY = 1285;
    public static final int GL_PACK_ALIGNMENT = 3333;
    public static final int GL_PALETTE4_R5_G6_B5_OES = 35730;
    public static final int GL_PALETTE4_RGB5_A1_OES = 35732;
    public static final int GL_PALETTE4_RGB8_OES = 35728;
    public static final int GL_PALETTE4_RGBA4_OES = 35731;
    public static final int GL_PALETTE4_RGBA8_OES = 35729;
    public static final int GL_PALETTE8_R5_G6_B5_OES = 35735;
    public static final int GL_PALETTE8_RGB5_A1_OES = 35737;
    public static final int GL_PALETTE8_RGB8_OES = 35733;
    public static final int GL_PALETTE8_RGBA4_OES = 35736;
    public static final int GL_PALETTE8_RGBA8_OES = 35734;
    public static final int GL_PERSPECTIVE_CORRECTION_HINT = 3152;
    public static final int GL_POINT_DISTANCE_ATTENUATION = 33065;
    public static final int GL_POINT_FADE_THRESHOLD_SIZE = 33064;
    public static final int GL_POINT_SIZE = 2833;
    public static final int GL_POINT_SIZE_ARRAY_BUFFER_BINDING_OES = 35743;
    public static final int GL_POINT_SIZE_ARRAY_OES = 35740;
    public static final int GL_POINT_SIZE_ARRAY_POINTER_OES = 35212;
    public static final int GL_POINT_SIZE_ARRAY_STRIDE_OES = 35211;
    public static final int GL_POINT_SIZE_ARRAY_TYPE_OES = 35210;
    public static final int GL_POINT_SIZE_MAX = 33063;
    public static final int GL_POINT_SIZE_MIN = 33062;
    public static final int GL_POINT_SMOOTH = 2832;
    public static final int GL_POINT_SMOOTH_HINT = 3153;
    public static final int GL_POINT_SPRITE_OES = 34913;
    public static final int GL_POINTS = 0;
    public static final int GL_POLYGON_OFFSET_FACTOR = 32824;
    public static final int GL_POLYGON_OFFSET_FILL = 32823;
    public static final int GL_POLYGON_OFFSET_UNITS = 10752;
    public static final int GL_POLYGON_SMOOTH_HINT = 3155;
    public static final int GL_POSITION = 4611;
    public static final int GL_PREVIOUS = 34168;
    public static final int GL_PRIMARY_COLOR = 34167;
    public static final int GL_PROJECTION = 5889;
    public static final int GL_PROJECTION_MATRIX = 2983;
    public static final int GL_PROJECTION_MATRIX_FLOAT_AS_INT_BITS_OES = 35214;
    public static final int GL_PROJECTION_STACK_DEPTH = 2980;
    public static final int GL_QUADRATIC_ATTENUATION = 4617;
    public static final int GL_RED_BITS = 3410;
    public static final int GL_RENDERER = 7937;
    public static final int GL_REPEAT = 10497;
    public static final int GL_REPLACE = 7681;
    public static final int GL_RESCALE_NORMAL = 32826;
    public static final int GL_RGB = 6407;
    public static final int GL_RGB_SCALE = 34163;
    public static final int GL_RGBA = 6408;
    public static final int GL_SAMPLE_ALPHA_TO_COVERAGE = 32926;
    public static final int GL_SAMPLE_ALPHA_TO_ONE = 32927;
    public static final int GL_SAMPLE_BUFFERS = 32936;
    public static final int GL_SAMPLE_COVERAGE = 32928;
    public static final int GL_SAMPLE_COVERAGE_INVERT = 32939;
    public static final int GL_SAMPLE_COVERAGE_VALUE = 32938;
    public static final int GL_SAMPLES = 32937;
    public static final int GL_SCISSOR_BOX = 3088;
    public static final int GL_SCISSOR_TEST = 3089;
    public static final int GL_SET = 5391;
    public static final int GL_SHADE_MODEL = 2900;
    public static final int GL_SHININESS = 5633;
    public static final int GL_SHORT = 5122;
    public static final int GL_SMOOTH = 7425;
    public static final int GL_SMOOTH_LINE_WIDTH_RANGE = 2850;
    public static final int GL_SMOOTH_POINT_SIZE_RANGE = 2834;
    public static final int GL_SPECULAR = 4610;
    public static final int GL_SPOT_CUTOFF = 4614;
    public static final int GL_SPOT_DIRECTION = 4612;
    public static final int GL_SPOT_EXPONENT = 4613;
    public static final int GL_SRC_ALPHA = 770;
    public static final int GL_SRC_ALPHA_SATURATE = 776;
    public static final int GL_SRC_COLOR = 768;
    public static final int GL_SRC0_ALPHA = 34184;
    public static final int GL_SRC0_RGB = 34176;
    public static final int GL_SRC1_ALPHA = 34185;
    public static final int GL_SRC1_RGB = 34177;
    public static final int GL_SRC2_ALPHA = 34186;
    public static final int GL_SRC2_RGB = 34178;
    public static final int GL_STACK_OVERFLOW = 1283;
    public static final int GL_STACK_UNDERFLOW = 1284;
    public static final int GL_STATIC_DRAW = 35044;
    public static final int GL_STENCIL_BITS = 3415;
    public static final int GL_STENCIL_BUFFER_BIT = 1024;
    public static final int GL_STENCIL_CLEAR_VALUE = 2961;
    public static final int GL_STENCIL_FAIL = 2964;
    public static final int GL_STENCIL_FUNC = 2962;
    public static final int GL_STENCIL_PASS_DEPTH_FAIL = 2965;
    public static final int GL_STENCIL_PASS_DEPTH_PASS = 2966;
    public static final int GL_STENCIL_REF = 2967;
    public static final int GL_STENCIL_TEST = 2960;
    public static final int GL_STENCIL_VALUE_MASK = 2963;
    public static final int GL_STENCIL_WRITEMASK = 2968;
    public static final int GL_SUBPIXEL_BITS = 3408;
    public static final int GL_SUBTRACT = 34023;
    public static final int GL_TEXTURE = 5890;
    public static final int GL_TEXTURE_ENV = 8960;
    public static final int GL_TEXTURE_ENV_MODE = 8704;
    public static final int GL_TEXTURE_MATRIX = 2984;
    public static final int GL_TEXTURE_2D = 3553;
    public static final int GL_TEXTURE_BINDING_2D = 32873;
    public static final int GL_TEXTURE_COORD_ARRAY = 32888;
    public static final int GL_TEXTURE_COORD_ARRAY_BUFFER_BINDING = 34970;
    public static final int GL_TEXTURE_COORD_ARRAY_POINTER = 32914;
    public static final int GL_TEXTURE_COORD_ARRAY_SIZE = 32904;
    public static final int GL_TEXTURE_COORD_ARRAY_STRIDE = 32906;
    public static final int GL_TEXTURE_COORD_ARRAY_TYPE = 32905;
    public static final int GL_TEXTURE_CROP_RECT_OES = 35741;
    public static final int GL_TEXTURE_ENV_COLOR = 8705;
    public static final int GL_TEXTURE_MAG_FILTER = 10240;
    public static final int GL_TEXTURE_MATRIX_FLOAT_AS_INT_BITS_OES = 35215;
    public static final int GL_TEXTURE_MIN_FILTER = 10241;
    public static final int GL_TEXTURE_STACK_DEPTH = 2981;
    public static final int GL_TEXTURE_WRAP_S = 10242;
    public static final int GL_TEXTURE_WRAP_T = 10243;
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
    public static final int GL_TRUE = 1;
    public static final int GL_UNPACK_ALIGNMENT = 3317;
    public static final int GL_UNSIGNED_BYTE = 5121;
    public static final int GL_UNSIGNED_SHORT = 5123;
    public static final int GL_UNSIGNED_SHORT_4_4_4_4 = 32819;
    public static final int GL_UNSIGNED_SHORT_5_5_5_1 = 32820;
    public static final int GL_UNSIGNED_SHORT_5_6_5 = 33635;
    public static final int GL_VENDOR = 7936;
    public static final int GL_VERSION = 7938;
    public static final int GL_VERTEX_ARRAY = 32884;
    public static final int GL_VERTEX_ARRAY_BUFFER_BINDING = 34966;
    public static final int GL_VERTEX_ARRAY_POINTER = 32910;
    public static final int GL_VERTEX_ARRAY_SIZE = 32890;
    public static final int GL_VERTEX_ARRAY_STRIDE = 32892;
    public static final int GL_VERTEX_ARRAY_TYPE = 32891;
    public static final int GL_VIEWPORT = 2978;
    public static final int GL_WEIGHT_ARRAY_BUFFER_BINDING_OES = 34974;
    public static final int GL_WEIGHT_ARRAY_OES = 34477;
    public static final int GL_WEIGHT_ARRAY_POINTER_OES = 34476;
    public static final int GL_WEIGHT_ARRAY_SIZE_OES = 34475;
    public static final int GL_WEIGHT_ARRAY_STRIDE_OES = 34474;
    public static final int GL_WEIGHT_ARRAY_TYPE_OES = 34473;
    public static final int GL_WRITE_ONLY = 35001;
    public static final int GL_XOR = 5382;
    public static final int GL_ZERO = 0;
    private static final String GL_EXTENSIONS_STRING =
            "GL_OES_point_size_array GL_OES_matrix_palette GL_OES_draw_texture "
                    + "GL_OES_compressed_paletted_texture GL_OES_point_sprite";
    private static final int[] COMPRESSED_TEXTURE_FORMATS = {
            GL_PALETTE4_RGB8_OES,
            GL_PALETTE4_RGBA8_OES,
            GL_PALETTE4_R5_G6_B5_OES,
            GL_PALETTE4_RGBA4_OES,
            GL_PALETTE4_RGB5_A1_OES,
            GL_PALETTE8_RGB8_OES,
            GL_PALETTE8_RGBA8_OES,
            GL_PALETTE8_R5_G6_B5_OES,
            GL_PALETTE8_RGBA4_OES,
            GL_PALETTE8_RGB5_A1_OES
    };

    private final Set<Integer> textures = new HashSet<>();
    private final Set<Integer> buffers = new HashSet<>();
    private final Set<Integer> enabledCaps = new HashSet<>();
    private final Set<Integer> enabledClientStates = new HashSet<>();
    private final Map<Integer, Buffer> bufferData = new HashMap<>();
    private final Map<Integer, Integer> bufferSizes = new HashMap<>();
    private final Map<Integer, Integer> bufferUsages = new HashMap<>();
    private final Map<Integer, TextureState> textureStates = new HashMap<>();
    private final Map<Integer, LightState> lightStates = new HashMap<>();
    private Object boundTarget;
    private BufferedImage boundBackingImage;
    private int boundArrayBuffer;
    private int boundElementArrayBuffer;
    private int nextTextureId = 1;
    private int nextBufferId = 1;
    private int clearColorArgb = 0xFF000000;
    private float clearDepthValue = 1.0f;
    private int lastError = GL_NO_ERROR;
    private int surfaceWidth;
    private int surfaceHeight;
    private int[] surfacePixels = new int[0];
    private float[] surfaceDepth = new float[0];
    private int[] surfaceStencil = new int[0];
    private int currentMatrixMode = GL_MODELVIEW;
    private final float[] modelViewMatrix = identityMatrix();
    private final float[] projectionMatrix = identityMatrix();
    private final float[] textureMatrix = identityMatrix();
    private final ArrayDeque<float[]> modelViewStack = new ArrayDeque<>();
    private final ArrayDeque<float[]> projectionStack = new ArrayDeque<>();
    private final ArrayDeque<float[]> textureStack = new ArrayDeque<>();
    private final ArrayDeque<float[]> paletteStack = new ArrayDeque<>();
    private int viewportX;
    private int viewportY;
    private int viewportWidth;
    private int viewportHeight;
    private int scissorX;
    private int scissorY;
    private int scissorWidth;
    private int scissorHeight;
    private final int[] boundTexture2dByUnit = new int[MAX_TEXTURE_UNITS_IMPLEMENTED];
    private int activeTextureUnit;
    private int clientActiveTextureUnit;
    private int boundTexture2d;
    private int currentColorArgb = 0xFFFFFFFF;
    private float currentColorR = 1.0f;
    private float currentColorG = 1.0f;
    private float currentColorB = 1.0f;
    private float currentColorA = 1.0f;
    private final float[] currentNormal = new float[] {0.0f, 0.0f, 1.0f};
    private final float[] currentTextureCoords = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
    private int blendSrcFactor = GL_ONE;
    private int blendDstFactor = GL_ZERO;
    private int logicOpMode = GL_COPY;
    private int packAlignment = 4;
    private int unpackAlignment = 4;
    private int alphaFunc = GL_NOTEQUAL;
    private float alphaRef = 0.0f;
    private boolean depthWriteEnabled = true;
    private float fogStart = 0.0f;
    private float fogEnd = 1.0f;
    private float fogDensity = 1.0f;
    private int fogMode = GL_EXP;
    private int fogColorArgb = 0xFF000000;
    private float pointSize = 1.0f;
    private float pointSizeMin = 0.0f;
    private float pointSizeMax = Float.MAX_VALUE;
    private float pointFadeThresholdSize = 1.0f;
    private final float[] pointDistanceAttenuation = new float[] {1.0f, 0.0f, 0.0f};
    private float lineWidth = 1.0f;
    private int depthFunc = GL_LESS;
    private float depthRangeNear = 0.0f;
    private float depthRangeFar = 1.0f;
    private int shadeModel = GL_SMOOTH;
    private int textureEnvMode = GL_MODULATE;
    private final float[] textureEnvColor = new float[] {0.0f, 0.0f, 0.0f, 0.0f};
    private boolean colorMaskRed = true;
    private boolean colorMaskGreen = true;
    private boolean colorMaskBlue = true;
    private boolean colorMaskAlpha = true;
    private int currentPaletteMatrixIndex;
    private int frontFaceMode = GL_CCW;
    private int cullFaceMode = GL_BACK;
    private int clearStencilValue;
    private int stencilFunc = GL_ALWAYS;
    private int stencilRef;
    private int stencilValueMask = 0xFFFFFFFF;
    private int stencilWriteMask = 0xFFFFFFFF;
    private int stencilFailOp = GL_KEEP;
    private int stencilDepthFailOp = GL_KEEP;
    private int stencilDepthPassOp = GL_KEEP;
    private float polygonOffsetFactor;
    private float polygonOffsetUnits;
    private float sampleCoverageValue = 1.0f;
    private boolean sampleCoverageInvert;
    private final float[][] clipPlanes = new float[6][4];
    private final float[][] paletteMatrices = new float[MAX_PALETTE_MATRICES][16];
    private final MaterialState frontMaterial = new MaterialState();
    private final MaterialState backMaterial = new MaterialState();
    private final float[] lightModelAmbient = new float[] {0.2f, 0.2f, 0.2f, 1.0f};
    private boolean lightModelTwoSide;
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

    private static final class TextureState {
        private int width;
        private int height;
        private int[] pixels;
        private int wrapS = GL_REPEAT;
        private int wrapT = GL_REPEAT;
        private int minFilter = GL_NEAREST;
        private int magFilter = GL_NEAREST;
        private final float[] cropRect = new float[] {0.0f, 0.0f, 0.0f, 0.0f};
        private boolean hasCropRect;
    }

    private static final class LightState {
        private final float[] ambient = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
        private final float[] diffuse = new float[] {1.0f, 1.0f, 1.0f, 1.0f};
        private final float[] specular = new float[] {1.0f, 1.0f, 1.0f, 1.0f};
        private final float[] position = new float[] {0.0f, 0.0f, 1.0f, 0.0f};
        private final float[] spotDirection = new float[] {0.0f, 0.0f, -1.0f};
        private float spotExponent;
        private float spotCutoff = 180.0f;
        private float constantAttenuation = 1.0f;
        private float linearAttenuation;
        private float quadraticAttenuation;
    }

    private static final class MaterialState {
        private final float[] ambient = new float[] {0.2f, 0.2f, 0.2f, 1.0f};
        private final float[] diffuse = new float[] {0.8f, 0.8f, 0.8f, 1.0f};
        private final float[] emission = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
        private final float[] specular = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
        private float shininess;
    }

    private record ClipVertex(
            float clipX,
            float clipY,
            float clipZ,
            float clipW,
            float u,
            float v,
            float eyeDepth,
            float modelX,
            float modelY,
            float modelZ,
            float pointSize,
            int color
    ) {
    }

    private record Vertex(float x, float y, float z, float w, float u, float v, float eyeDepth, int color) {
    }

    private OpglGraphics() {
        for (var paletteMatrix : paletteMatrices) {
            loadIdentity(paletteMatrix);
        }
        textureStates.put(0, new TextureState());
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
        if (remexa.probes.SdkStubSupport.SDK_TRACE_ENABLED) {
            remexa.probes.SdkStubSupport.log(
                    "com.mexa.opgl.OpglGraphics",
                    "getInstance",
                    describeInstance(current)
            );
        }
        return current;
    }

    public void bind (java.lang.Object target) {
        if (remexa.probes.SdkStubSupport.SDK_TRACE_ENABLED) {
            remexa.probes.SdkStubSupport.log(
                    "com.mexa.opgl.OpglGraphics",
                    "bind",
                    describeInstance(this),
                    target
            );
        }
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
        boundBackingImage = null;
        if (target instanceof CanvasGraphics3D canvasGraphics3D) {
            boundBackingImage = canvasGraphics3D.backingImage();
        }
        if (boundBackingImage != null) {
            surfaceWidth = boundBackingImage.getWidth();
            surfaceHeight = boundBackingImage.getHeight();
            ensureSurfaceBuffers();
            boundBackingImage.getRGB(0, 0, surfaceWidth, surfaceHeight, surfacePixels, 0, surfaceWidth);
        } else {
            surfaceWidth = Math.max(1, ((Graphics) target).getClipWidth());
            surfaceHeight = Math.max(1, ((Graphics) target).getClipHeight());
            ensureSurfaceBuffers();
            Arrays.fill(surfacePixels, 0);
        }
        Arrays.fill(surfaceDepth, 1.0f);
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            viewportX = 0;
            viewportY = 0;
            viewportWidth = surfaceWidth;
            viewportHeight = surfaceHeight;
        }
        if (scissorWidth <= 0 || scissorHeight <= 0) {
            scissorX = 0;
            scissorY = 0;
            scissorWidth = surfaceWidth;
            scissorHeight = surfaceHeight;
        }
    }

    public void release () {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "release");
        if (boundTarget instanceof Graphics graphics) {
            forceSurfaceOpaque();
            if (boundBackingImage != null) {
                boundBackingImage.setRGB(0, 0, surfaceWidth, surfaceHeight, surfacePixels, 0, surfaceWidth);
            } else {
                graphics.drawRGB(surfacePixels, 0, surfaceWidth, 0, 0, surfaceWidth, surfaceHeight, false);
            }
        }
        boundBackingImage = null;
        boundTarget = null;
    }

    public void glActiveTexture (int texture) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glActiveTexture", texture);
        ensureBound();
        int unit = texture - GL_TEXTURE0;
        if (unit < 0 || unit >= MAX_TEXTURE_UNITS_IMPLEMENTED) {
            setError(GL_INVALID_ENUM);
            return;
        }
        activeTextureUnit = unit;
        boundTexture2d = boundTexture2dByUnit[unit];
    }

    public void glAlphaFunc (int func, float ref) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glAlphaFunc", func, ref);
        ensureBound();
        alphaFunc = func;
        alphaRef = clampUnit(ref);
    }

    public void glBindTexture (int target, int texture) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glBindTexture", target, texture);
        ensureBound();
        if (target != GL_TEXTURE_2D) {
            setError(GL_INVALID_ENUM);
            return;
        }
        if (texture != 0 && !textures.contains(texture)) {
            setError(GL_INVALID_VALUE);
            return;
        }
        boundTexture2dByUnit[activeTextureUnit] = texture;
        boundTexture2d = texture;
    }

    public void glBlendFunc (int sfactor, int dfactor) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glBlendFunc", sfactor, dfactor);
        ensureBound();
        blendSrcFactor = sfactor;
        blendDstFactor = dfactor;
    }

    public void glClear (int mask) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glClear", mask);
        ensureBound();
        if ((mask & GL_COLOR_BUFFER_BIT) != 0) {
            Arrays.fill(surfacePixels, clearColorArgb);
        }
        if ((mask & GL_DEPTH_BUFFER_BIT) != 0) {
            Arrays.fill(surfaceDepth, clearDepthValue);
        }
        if ((mask & GL_STENCIL_BUFFER_BIT) != 0) {
            clearStencilBuffer();
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
        ensureBound();
        clearDepthValue = clampUnit(depth);
    }

    public void glClearStencil (int s) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glClearStencil", s);
        ensureBound();
        clearStencilValue = s & 0xFF;
    }

    public void glClientActiveTexture (int texture) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glClientActiveTexture", texture);
        ensureBound();
        int unit = texture - GL_TEXTURE0;
        if (unit < 0 || unit >= MAX_TEXTURE_UNITS_IMPLEMENTED) {
            setError(GL_INVALID_ENUM);
            return;
        }
        clientActiveTextureUnit = unit;
    }

    public void glColor4f (float red, float green, float blue, float alpha) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glColor4f", red, green, blue, alpha);
        ensureBound();
        currentColorR = clampUnit(red);
        currentColorG = clampUnit(green);
        currentColorB = clampUnit(blue);
        currentColorA = clampUnit(alpha);
        currentColorArgb = (clampColor(currentColorA) << 24)
                | (clampColor(currentColorR) << 16)
                | (clampColor(currentColorG) << 8)
                | clampColor(currentColorB);
        applyColorMaterial();
    }

    public void glColorMask (boolean red, boolean green, boolean blue, boolean alpha) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glColorMask", red, green, blue, alpha);
        ensureBound();
        colorMaskRed = red;
        colorMaskGreen = green;
        colorMaskBlue = blue;
        colorMaskAlpha = alpha;
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
        ensureBound();
        if (target != GL_TEXTURE_2D || data == null) {
            return;
        }
        TextureState textureState = textureStates.computeIfAbsent(boundTexture2d, ignored -> new TextureState());
        textureState.width = width;
        textureState.height = height;
        textureState.pixels = decodePaletteTexture(data, width, height, internalformat);
    }

    public void glCompressedTexSubImage2D (int target, int level, int xoffset, int yoffset, int width, int height, int format, com.mexa.opgl.ByteBuffer data) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glCompressedTexSubImage2D", target, level, xoffset, yoffset, width, height, format, data);
        ensureBound();
        if (target != GL_TEXTURE_2D || data == null || width <= 0 || height <= 0) {
            return;
        }
        TextureState textureState = textureStates.get(boundTexture2d);
        if (textureState == null || textureState.pixels == null) {
            setError(GL_INVALID_OPERATION);
            return;
        }
        copyTextureRegion(
                decodePaletteTexture(data, width, height, format),
                width,
                height,
                textureState,
                xoffset,
                yoffset
        );
    }

    public void glCopyTexImage2D (int target, int level, int internalformat, int x, int y, int width, int height, int border) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glCopyTexImage2D", target, level, internalformat, x, y, width, height, border);
        ensureBound();
        if (target != GL_TEXTURE_2D || width <= 0 || height <= 0) {
            return;
        }
        TextureState textureState = textureStates.computeIfAbsent(boundTexture2d, ignored -> new TextureState());
        textureState.width = width;
        textureState.height = height;
        textureState.pixels = copySurfacePixels(x, y, width, height);
    }

    public void glCopyTexSubImage2D (int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glCopyTexSubImage2D", target, level, xoffset, yoffset, x, y, width, height);
        ensureBound();
        if (target != GL_TEXTURE_2D || width <= 0 || height <= 0) {
            return;
        }
        TextureState textureState = textureStates.get(boundTexture2d);
        if (textureState == null || textureState.pixels == null) {
            setError(GL_INVALID_OPERATION);
            return;
        }
        copyTextureRegion(copySurfacePixels(x, y, width, height), width, height, textureState, xoffset, yoffset);
    }

    public void glCullFace (int mode) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glCullFace", mode);
        ensureBound();
        cullFaceMode = mode;
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
            textureStates.remove(texture);
            for (int unit = 0; unit < boundTexture2dByUnit.length; unit++) {
                if (boundTexture2dByUnit[unit] == texture) {
                    boundTexture2dByUnit[unit] = 0;
                }
            }
            if (boundTexture2d == texture) {
                boundTexture2d = 0;
            }
        }
    }

    public void glDepthFunc (int func) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDepthFunc", func);
        ensureBound();
        depthFunc = func;
    }

    public void glDepthMask (boolean flag) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDepthMask", flag);
        ensureBound();
        depthWriteEnabled = flag;
    }

    public void glDepthRangef (float zNear, float zFar) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDepthRangef", zNear, zFar);
        ensureBound();
        depthRangeNear = clampUnit(zNear);
        depthRangeFar = clampUnit(zFar);
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
        renderDrawArrays(mode, first, count);
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
        int[] decodedIndices = readIndices(indices, type, indices.boundedOffset() * sizeOfBuffer(indices), indices.boundedLength());
        validateDrawElementsIndices(decodedIndices);
        renderDrawElements(mode, decodedIndices);
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
        Buffer elementBuffer = bufferData.get(boundElementArrayBuffer);
        if (elementBuffer == null) {
            setError(GL_INVALID_OPERATION);
            return;
        }
        int[] decodedIndices = readIndices(elementBuffer, type, offset, count);
        validateDrawElementsIndices(decodedIndices);
        renderDrawElements(mode, decodedIndices);
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
        ensureBound();
        if (pname == GL_FOG_START) {
            fogStart = param;
        } else if (pname == GL_FOG_END) {
            fogEnd = param;
        } else if (pname == GL_FOG_DENSITY) {
            fogDensity = Math.max(0.0f, param);
        } else if (pname == GL_FOG_MODE) {
            fogMode = Math.round(param);
        }
    }

    public void glFogfv (int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glFogfv", pname, params);
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        if (pname == GL_FOG_COLOR && params.length >= 4) {
            fogColorArgb = (clampColor(params[3]) << 24)
                    | (clampColor(params[0]) << 16)
                    | (clampColor(params[1]) << 8)
                    | clampColor(params[2]);
        } else if (params.length > 0) {
            glFogf(pname, params[0]);
        }
    }

    public void glFrontFace (int mode) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glFrontFace", mode);
        ensureBound();
        frontFaceMode = mode;
    }

    public void glFrustumf (float left, float right, float bottom, float top, float zNear, float zFar) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glFrustumf", left, right, bottom, top, zNear, zFar);
        ensureBound();
        multiplyCurrentMatrix(frustumMatrix(left, right, bottom, top, zNear, zFar));
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
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        switch (pname) {
            case GL_MAX_LIGHTS -> params[0] = 8;
            case GL_MAX_CLIP_PLANES -> params[0] = 6;
            case GL_MAX_MODELVIEW_STACK_DEPTH -> params[0] = 16;
            case GL_MAX_PROJECTION_STACK_DEPTH -> params[0] = 2;
            case GL_MAX_TEXTURE_STACK_DEPTH -> params[0] = 2;
            case GL_MAX_TEXTURE_SIZE -> params[0] = 1024;
            case GL_MAX_TEXTURE_UNITS -> params[0] = 2;
            case GL_MAX_PALETTE_MATRICES_OES -> params[0] = MAX_PALETTE_MATRICES;
            case GL_MAX_VERTEX_UNITS_OES -> params[0] = 4;
            case GL_NUM_COMPRESSED_TEXTURE_FORMATS -> params[0] = COMPRESSED_TEXTURE_FORMATS.length;
            case GL_COMPRESSED_TEXTURE_FORMATS -> {
                int count = Math.min(params.length, COMPRESSED_TEXTURE_FORMATS.length);
                System.arraycopy(COMPRESSED_TEXTURE_FORMATS, 0, params, 0, count);
            }
            case GL_TEXTURE_BINDING_2D -> params[0] = boundTexture2d;
            case GL_ARRAY_BUFFER_BINDING -> params[0] = boundArrayBuffer;
            case GL_ELEMENT_ARRAY_BUFFER_BINDING -> params[0] = boundElementArrayBuffer;
            case GL_VERTEX_ARRAY_BUFFER_BINDING -> params[0] = vertexArrayBinding != null ? vertexArrayBinding.bufferId : 0;
            case GL_NORMAL_ARRAY_BUFFER_BINDING -> params[0] = normalArrayBinding != null ? normalArrayBinding.bufferId : 0;
            case GL_COLOR_ARRAY_BUFFER_BINDING -> params[0] = colorArrayBinding != null ? colorArrayBinding.bufferId : 0;
            case GL_TEXTURE_COORD_ARRAY_BUFFER_BINDING -> params[0] = texCoordArrayBinding != null ? texCoordArrayBinding.bufferId : 0;
            case GL_POINT_SIZE_ARRAY_BUFFER_BINDING_OES -> params[0] = pointSizeArrayBinding != null ? pointSizeArrayBinding.bufferId : 0;
            case GL_MATRIX_INDEX_ARRAY_BUFFER_BINDING_OES -> params[0] = matrixIndexArrayBinding != null ? matrixIndexArrayBinding.bufferId : 0;
            case GL_WEIGHT_ARRAY_BUFFER_BINDING_OES -> params[0] = weightArrayBinding != null ? weightArrayBinding.bufferId : 0;
            case GL_PACK_ALIGNMENT -> params[0] = packAlignment;
            case GL_UNPACK_ALIGNMENT -> params[0] = unpackAlignment;
            case GL_ACTIVE_TEXTURE -> params[0] = GL_TEXTURE0 + activeTextureUnit;
            case GL_CLIENT_ACTIVE_TEXTURE -> params[0] = GL_TEXTURE0 + clientActiveTextureUnit;
            case GL_MATRIX_MODE -> params[0] = currentMatrixMode;
            case GL_CURRENT_PALETTE_MATRIX_OES -> params[0] = currentPaletteMatrixIndex;
            case GL_BLEND_SRC -> params[0] = blendSrcFactor;
            case GL_BLEND_DST -> params[0] = blendDstFactor;
            case GL_ALPHA_TEST_FUNC -> params[0] = alphaFunc;
            case GL_ALPHA_TEST_REF -> params[0] = clampColor(alphaRef);
            case GL_DEPTH_FUNC -> params[0] = depthFunc;
            case GL_DEPTH_WRITEMASK -> params[0] = depthWriteEnabled ? GL_TRUE : GL_FALSE;
            case GL_SHADE_MODEL -> params[0] = shadeModel;
            case GL_FOG_MODE -> params[0] = fogMode;
            case GL_CULL_FACE_MODE -> params[0] = cullFaceMode;
            case GL_FRONT_FACE -> params[0] = frontFaceMode;
            case GL_LIGHT_MODEL_TWO_SIDE -> params[0] = lightModelTwoSide ? GL_TRUE : GL_FALSE;
            case GL_STENCIL_CLEAR_VALUE -> params[0] = clearStencilValue;
            case GL_STENCIL_FUNC -> params[0] = stencilFunc;
            case GL_STENCIL_REF -> params[0] = stencilRef;
            case GL_STENCIL_VALUE_MASK -> params[0] = stencilValueMask;
            case GL_STENCIL_WRITEMASK -> params[0] = stencilWriteMask;
            case GL_STENCIL_FAIL -> params[0] = stencilFailOp;
            case GL_STENCIL_PASS_DEPTH_FAIL -> params[0] = stencilDepthFailOp;
            case GL_STENCIL_PASS_DEPTH_PASS -> params[0] = stencilDepthPassOp;
            case GL_LOGIC_OP_MODE -> params[0] = logicOpMode;
            case GL_MODELVIEW_STACK_DEPTH -> params[0] = modelViewStack.size() + 1;
            case GL_PROJECTION_STACK_DEPTH -> params[0] = projectionStack.size() + 1;
            case GL_TEXTURE_STACK_DEPTH -> params[0] = textureStack.size() + 1;
            case GL_MAX_VIEWPORT_DIMS -> {
                if (params.length > 0) {
                    params[0] = surfaceWidth;
                }
                if (params.length > 1) {
                    params[1] = surfaceHeight;
                }
            }
            case GL_VIEWPORT -> {
                if (params.length > 0) {
                    params[0] = viewportX;
                }
                if (params.length > 1) {
                    params[1] = viewportY;
                }
                if (params.length > 2) {
                    params[2] = viewportWidth;
                }
                if (params.length > 3) {
                    params[3] = viewportHeight;
                }
            }
            case GL_SCISSOR_BOX -> {
                if (params.length > 0) {
                    params[0] = scissorX;
                }
                if (params.length > 1) {
                    params[1] = scissorY;
                }
                if (params.length > 2) {
                    params[2] = scissorWidth;
                }
                if (params.length > 3) {
                    params[3] = scissorHeight;
                }
            }
            default -> {
            }
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
        ensureBound();
    }

    public void glLightModelf (int pname, float param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLightModelf", pname, param);
        ensureBound();
        if (pname == GL_LIGHT_MODEL_TWO_SIDE) {
            lightModelTwoSide = param != 0.0f;
        }
    }

    public void glLightModelfv (int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLightModelfv", pname, params);
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        if (pname == GL_LIGHT_MODEL_AMBIENT) {
            copyFloats(params, lightModelAmbient, 4);
        } else if (pname == GL_LIGHT_MODEL_TWO_SIDE && params.length > 0) {
            lightModelTwoSide = params[0] != 0.0f;
        }
    }

    public void glLightf (int light, int pname, float param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLightf", light, pname, param);
        ensureBound();
        applyLightScalar(lightStates.computeIfAbsent(light, ignored -> new LightState()), pname, param);
    }

    public void glLightfv (int light, int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLightfv", light, pname, params);
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        var lightState = lightStates.computeIfAbsent(light, ignored -> new LightState());
        switch (pname) {
            case GL_AMBIENT -> copyFloats(params, lightState.ambient, 4);
            case GL_DIFFUSE -> copyFloats(params, lightState.diffuse, 4);
            case GL_SPECULAR -> copyFloats(params, lightState.specular, 4);
            case GL_POSITION -> {
                float w = params.length > 3 ? params[3] : 1.0f;
                float[] transformed = multiplyVec4(modelViewMatrix, params[0], params.length > 1 ? params[1] : 0.0f,
                        params.length > 2 ? params[2] : 0.0f, w);
                copyFloats(transformed, lightState.position, 4);
            }
            case GL_SPOT_DIRECTION -> {
                float[] transformed = multiplyVec4(modelViewMatrix, params[0], params.length > 1 ? params[1] : 0.0f,
                        params.length > 2 ? params[2] : -1.0f, 0.0f);
                copyFloats(transformed, lightState.spotDirection, 3);
                normalize3(lightState.spotDirection);
            }
            case GL_SPOT_EXPONENT, GL_SPOT_CUTOFF, GL_CONSTANT_ATTENUATION,
                    GL_LINEAR_ATTENUATION, GL_QUADRATIC_ATTENUATION -> {
                if (params.length > 0) {
                    applyLightScalar(lightState, pname, params[0]);
                }
            }
            default -> {
            }
        }
    }

    public void glLineWidth (float width) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLineWidth", width);
        ensureBound();
        lineWidth = Math.max(1.0f, width);
    }

    public void glLoadIdentity () {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLoadIdentity");
        ensureBound();
        loadIdentity(currentMatrix());
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
        System.arraycopy(m, 0, currentMatrix(), 0, 16);
    }

    public void glLogicOp (int opcode) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLogicOp", opcode);
        ensureBound();
        logicOpMode = opcode;
    }

    public void glMaterialf (int face, int pname, float param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glMaterialf", face, pname, param);
        ensureBound();
        applyMaterialScalar(face, pname, param);
    }

    public void glMaterialfv (int face, int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glMaterialfv", face, pname, params);
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        applyMaterial(face, pname, params);
    }

    public void glMatrixMode (int mode) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glMatrixMode", mode);
        ensureBound();
        currentMatrixMode = mode;
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
        multiplyCurrentMatrix(m);
    }

    public void glMultiTexCoord4f (int target, float s, float t, float r, float q) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glMultiTexCoord4f", target, s, t, r, q);
        ensureBound();
        if (target == GL_TEXTURE0) {
            currentTextureCoords[0] = s;
            currentTextureCoords[1] = t;
            currentTextureCoords[2] = r;
            currentTextureCoords[3] = q;
        }
    }

    public void glNormal3f (float nx, float ny, float nz) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glNormal3f", nx, ny, nz);
        ensureBound();
        currentNormal[0] = nx;
        currentNormal[1] = ny;
        currentNormal[2] = nz;
        normalize3(currentNormal);
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
        multiplyCurrentMatrix(orthoMatrix(left, right, bottom, top, zNear, zFar));
    }

    public void glPixelStorei (int pname, int param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glPixelStorei", pname, param);
        ensureBound();
        if (pname != GL_PACK_ALIGNMENT && pname != GL_UNPACK_ALIGNMENT) {
            setError(GL_INVALID_ENUM);
            return;
        }
        if (param != 1 && param != 2 && param != 4 && param != 8) {
            setError(GL_INVALID_VALUE);
            return;
        }
        if (pname == GL_PACK_ALIGNMENT) {
            packAlignment = param;
        } else {
            unpackAlignment = param;
        }
    }

    public void glPointSize (float size) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glPointSize", size);
        ensureBound();
        pointSize = Math.max(1.0f, size);
    }

    public void glPolygonOffset (float factor, float units) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glPolygonOffset", factor, units);
        ensureBound();
        polygonOffsetFactor = factor;
        polygonOffsetUnits = units;
    }

    public void glPopMatrix () {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glPopMatrix");
        ensureBound();
        popMatrix();
    }

    public void glPushMatrix () {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glPushMatrix");
        ensureBound();
        pushMatrix();
    }

    public void glRotatef (float angle, float x, float y, float z) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glRotatef", angle, x, y, z);
        ensureBound();
        multiplyCurrentMatrix(rotationMatrix(angle, x, y, z));
    }

    public void glSampleCoverage (float value, boolean invert) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glSampleCoverage", value, invert);
        ensureBound();
        sampleCoverageValue = clampUnit(value);
        sampleCoverageInvert = invert;
    }

    public void glScalef (float x, float y, float z) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glScalef", x, y, z);
        ensureBound();
        multiplyCurrentMatrix(scaleMatrix(x, y, z));
    }

    public void glScissor (int x, int y, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glScissor", x, y, width, height);
        ensureBound();
        scissorX = x;
        scissorY = y;
        scissorWidth = Math.max(0, width);
        scissorHeight = Math.max(0, height);
    }

    public void glShadeModel (int mode) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glShadeModel", mode);
        ensureBound();
        shadeModel = mode;
    }

    public void glStencilFunc (int func, int ref, int mask) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glStencilFunc", func, ref, mask);
        ensureBound();
        stencilFunc = func;
        stencilRef = ref;
        stencilValueMask = mask;
    }

    public void glStencilMask (int mask) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glStencilMask", mask);
        ensureBound();
        stencilWriteMask = mask;
    }

    public void glStencilOp (int fail, int zfail, int zpass) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glStencilOp", fail, zfail, zpass);
        ensureBound();
        stencilFailOp = fail;
        stencilDepthFailOp = zfail;
        stencilDepthPassOp = zpass;
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
        if (target == GL_TEXTURE_ENV && pname == GL_TEXTURE_ENV_MODE) {
            textureEnvMode = Math.round(param);
        }
    }

    public void glTexEnvfv (int target, int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexEnvfv", target, pname, params);
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        if (target == GL_TEXTURE_ENV && pname == GL_TEXTURE_ENV_MODE && params.length > 0) {
            textureEnvMode = Math.round(params[0]);
        } else if (target == GL_TEXTURE_ENV && pname == GL_TEXTURE_ENV_COLOR && params.length >= 4) {
            System.arraycopy(params, 0, textureEnvColor, 0, 4);
        }
    }

    public void glTexImage2D (int target, int level, int internalformat, int width, int height, int border, int format, int type, com.mexa.opgl.Buffer pixels) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexImage2D", target, level, internalformat, width, height, border, format, type, pixels);
        ensureBound();
        if (target != GL_TEXTURE_2D || pixels == null) {
            return;
        }
        TextureState textureState = textureStates.computeIfAbsent(boundTexture2d, ignored -> new TextureState());
        textureState.width = width;
        textureState.height = height;
        textureState.pixels = decodeTexturePixels(pixels, width, height, format, type);
    }

    public void glTexParameterf (int target, int pname, float param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexParameterf", target, pname, param);
        ensureBound();
        applyTextureParameter(target, pname, Math.round(param));
    }

    public void glTexSubImage2D (int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, com.mexa.opgl.Buffer pixels) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexSubImage2D", target, level, xoffset, yoffset, width, height, format, type, pixels);
        ensureBound();
        if (target != GL_TEXTURE_2D || pixels == null || width <= 0 || height <= 0) {
            return;
        }
        TextureState textureState = textureStates.get(boundTexture2d);
        if (textureState == null || textureState.pixels == null) {
            setError(GL_INVALID_OPERATION);
            return;
        }
        copyTextureRegion(decodeTexturePixels(pixels, width, height, format, type), width, height, textureState, xoffset, yoffset);
    }

    public void glTranslatef (float x, float y, float z) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTranslatef", x, y, z);
        ensureBound();
        multiplyCurrentMatrix(translationMatrix(x, y, z));
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
        viewportX = x;
        viewportY = y;
        viewportWidth = Math.max(1, width);
        viewportHeight = Math.max(1, height);
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
        bufferData.put(bufferId, cloneBuffer(data));
    }

    public void glBufferSubData (int target, int offset, com.mexa.opgl.Buffer data) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glBufferSubData", target, offset, data);
        ensureBound();
        if (data == null) {
            throw new NullPointerException("data");
        }
        int bufferId = target == GL_ARRAY_BUFFER ? boundArrayBuffer
                : target == GL_ELEMENT_ARRAY_BUFFER ? boundElementArrayBuffer : 0;
        if (bufferId == 0) {
            setError(GL_INVALID_OPERATION);
            return;
        }
        Buffer destination = bufferData.get(bufferId);
        if (destination == null) {
            setError(GL_INVALID_OPERATION);
            return;
        }
        writeBufferData(destination, offset, data);
    }

    public void glClipPlanef (int plane, float[] equation) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glClipPlanef", plane, equation);
        ensureBound();
        if (equation == null) {
            throw new NullPointerException("equation");
        }
        int index = clipPlaneIndex(plane);
        if (index < 0 || equation.length < 4) {
            setError(GL_INVALID_ENUM);
            return;
        }
        System.arraycopy(equation, 0, clipPlanes[index], 0, 4);
    }

    public void glColor4ub (byte red, byte green, byte blue, byte alpha) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glColor4ub", red, green, blue, alpha);
        ensureBound();
        currentColorR = (red & 0xFF) / 255.0f;
        currentColorG = (green & 0xFF) / 255.0f;
        currentColorB = (blue & 0xFF) / 255.0f;
        currentColorA = (alpha & 0xFF) / 255.0f;
        currentColorArgb = ((alpha & 0xFF) << 24)
                | ((red & 0xFF) << 16)
                | ((green & 0xFF) << 8)
                | (blue & 0xFF);
        applyColorMaterial();
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
            bufferData.remove(buffer);
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
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        if (params.length > 0) {
            if (pname == GL_COLOR_WRITEMASK) {
                params[0] = colorMaskRed;
                if (params.length > 1) {
                    params[1] = colorMaskGreen;
                }
                if (params.length > 2) {
                    params[2] = colorMaskBlue;
                }
                if (params.length > 3) {
                    params[3] = colorMaskAlpha;
                }
            } else {
                params[0] = switch (pname) {
                    case GL_DEPTH_WRITEMASK -> depthWriteEnabled;
                    case GL_LIGHT_MODEL_TWO_SIDE -> lightModelTwoSide;
                    case GL_SAMPLE_COVERAGE_INVERT -> sampleCoverageInvert;
                    default -> enabledCaps.contains(pname) || enabledClientStates.contains(pname);
                };
            }
        }
    }

    public void glGetBufferParameteriv (int target, int pname, int[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetBufferParameteriv", target, pname, params);
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        int bufferId = target == GL_ARRAY_BUFFER ? boundArrayBuffer
                : target == GL_ELEMENT_ARRAY_BUFFER ? boundElementArrayBuffer : 0;
        if (bufferId == 0) {
            setError(GL_INVALID_OPERATION);
            return;
        }
        if (pname == GL_BUFFER_SIZE && params.length > 0) {
            params[0] = bufferSizes.getOrDefault(bufferId, 0);
        } else if (pname == GL_BUFFER_USAGE && params.length > 0) {
            params[0] = bufferUsages.getOrDefault(bufferId, GL_STATIC_DRAW);
        }
    }

    public void glGetClipPlanef (int pname, float[] equation) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetClipPlanef", pname, equation);
        ensureBound();
        if (equation == null) {
            throw new NullPointerException("equation");
        }
        int index = clipPlaneIndex(pname);
        if (index < 0 || equation.length < 4) {
            setError(GL_INVALID_ENUM);
            return;
        }
        System.arraycopy(clipPlanes[index], 0, equation, 0, 4);
    }

    public void glGetFloatv (int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetFloatv", pname, params);
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        switch (pname) {
            case GL_MODELVIEW_MATRIX -> copyFloats(modelViewMatrix, params, Math.min(16, params.length));
            case GL_PROJECTION_MATRIX -> copyFloats(projectionMatrix, params, Math.min(16, params.length));
            case GL_TEXTURE_MATRIX -> copyFloats(textureMatrix, params, Math.min(16, params.length));
            case GL_CURRENT_COLOR -> writeFloatValues(params, currentColorR, currentColorG, currentColorB, currentColorA);
            case GL_CURRENT_NORMAL -> writeFloatValues(params, currentNormal[0], currentNormal[1], currentNormal[2]);
            case GL_CURRENT_TEXTURE_COORDS -> writeFloatValues(params, currentTextureCoords);
            case GL_COLOR_CLEAR_VALUE -> writeColorFloats(params, clearColorArgb);
            case GL_DEPTH_CLEAR_VALUE -> writeFloatValues(params, clearDepthValue);
            case GL_DEPTH_RANGE -> writeFloatValues(params, depthRangeNear, depthRangeFar);
            case GL_LINE_WIDTH -> writeFloatValues(params, lineWidth);
            case GL_POINT_SIZE -> writeFloatValues(params, pointSize);
            case GL_POINT_SIZE_MIN -> writeFloatValues(params, pointSizeMin);
            case GL_POINT_SIZE_MAX -> writeFloatValues(params, pointSizeMax);
            case GL_POINT_FADE_THRESHOLD_SIZE -> writeFloatValues(params, pointFadeThresholdSize);
            case GL_POINT_DISTANCE_ATTENUATION -> writeFloatValues(params, pointDistanceAttenuation);
            case GL_POLYGON_OFFSET_FACTOR -> writeFloatValues(params, polygonOffsetFactor);
            case GL_POLYGON_OFFSET_UNITS -> writeFloatValues(params, polygonOffsetUnits);
            case GL_SAMPLE_COVERAGE_VALUE -> writeFloatValues(params, sampleCoverageValue);
            case GL_FOG_START -> writeFloatValues(params, fogStart);
            case GL_FOG_END -> writeFloatValues(params, fogEnd);
            case GL_FOG_DENSITY -> writeFloatValues(params, fogDensity);
            case GL_FOG_COLOR -> writeColorFloats(params, fogColorArgb);
            case GL_LIGHT_MODEL_AMBIENT -> copyFloats(lightModelAmbient, params, Math.min(4, params.length));
            default -> {
            }
        }
    }

    public void glGetLightfv (int light, int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetLightfv", light, pname, params);
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        var lightState = lightStates.get(light);
        if (lightState == null) {
            return;
        }
        switch (pname) {
            case GL_AMBIENT -> copyFloats(lightState.ambient, params, Math.min(4, params.length));
            case GL_DIFFUSE -> copyFloats(lightState.diffuse, params, Math.min(4, params.length));
            case GL_SPECULAR -> copyFloats(lightState.specular, params, Math.min(4, params.length));
            case GL_POSITION -> copyFloats(lightState.position, params, Math.min(4, params.length));
            case GL_SPOT_DIRECTION -> copyFloats(lightState.spotDirection, params, Math.min(3, params.length));
            case GL_SPOT_EXPONENT -> writeFloatValues(params, lightState.spotExponent);
            case GL_SPOT_CUTOFF -> writeFloatValues(params, lightState.spotCutoff);
            case GL_CONSTANT_ATTENUATION -> writeFloatValues(params, lightState.constantAttenuation);
            case GL_LINEAR_ATTENUATION -> writeFloatValues(params, lightState.linearAttenuation);
            case GL_QUADRATIC_ATTENUATION -> writeFloatValues(params, lightState.quadraticAttenuation);
            default -> {
            }
        }
    }

    public void glGetMaterialfv (int face, int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetMaterialfv", face, pname, params);
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        var material = materialForFace(face);
        switch (pname) {
            case GL_AMBIENT -> copyFloats(material.ambient, params, Math.min(4, params.length));
            case GL_DIFFUSE -> copyFloats(material.diffuse, params, Math.min(4, params.length));
            case GL_EMISSION -> copyFloats(material.emission, params, Math.min(4, params.length));
            case GL_SPECULAR -> copyFloats(material.specular, params, Math.min(4, params.length));
            case GL_AMBIENT_AND_DIFFUSE -> copyFloats(material.diffuse, params, Math.min(4, params.length));
            case GL_SHININESS -> writeFloatValues(params, material.shininess);
            default -> {
            }
        }
    }

    public void glGetTexEnvfv (int env, int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetTexEnvfv", env, pname, params);
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        if (env == GL_TEXTURE_ENV && pname == GL_TEXTURE_ENV_MODE && params.length > 0) {
            params[0] = textureEnvMode;
        } else if (env == GL_TEXTURE_ENV && pname == GL_TEXTURE_ENV_COLOR && params.length >= 4) {
            System.arraycopy(textureEnvColor, 0, params, 0, 4);
        }
    }

    public void glGetTexParameterfv (int target, int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetTexParameterfv", target, pname, params);
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        TextureState textureState = textureStates.get(boundTexture2d);
        if (target != GL_TEXTURE_2D || textureState == null) {
            setError(GL_INVALID_ENUM);
            return;
        }
        if (pname == GL_TEXTURE_CROP_RECT_OES) {
            if (params.length >= 4) {
                System.arraycopy(textureState.cropRect, 0, params, 0, 4);
            }
        } else if (params.length > 0) {
            params[0] = textureParameter(textureState, pname);
        }
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
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        if (env == GL_TEXTURE_ENV && pname == GL_TEXTURE_ENV_MODE && params.length > 0) {
            params[0] = textureEnvMode;
        } else if (env == GL_TEXTURE_ENV && pname == GL_TEXTURE_ENV_COLOR && params.length >= 4) {
            for (int i = 0; i < 4; i++) {
                params[i] = clampColor(textureEnvColor[i]);
            }
        }
    }

    public void glGetTexParameteriv (int target, int pname, int[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glGetTexParameteriv", target, pname, params);
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        TextureState textureState = textureStates.get(boundTexture2d);
        if (target != GL_TEXTURE_2D || textureState == null) {
            setError(GL_INVALID_ENUM);
            return;
        }
        if (pname == GL_TEXTURE_CROP_RECT_OES) {
            int count = Math.min(params.length, 4);
            for (int i = 0; i < count; i++) {
                params[i] = Math.round(textureState.cropRect[i]);
            }
        } else if (params.length > 0) {
            params[0] = textureParameter(textureState, pname);
        }
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
        ensureBound();
        switch (pname) {
            case GL_POINT_SIZE_MIN -> pointSizeMin = Math.max(0.0f, param);
            case GL_POINT_SIZE_MAX -> pointSizeMax = Math.max(pointSizeMin, param);
            case GL_POINT_FADE_THRESHOLD_SIZE -> pointFadeThresholdSize = Math.max(0.0f, param);
            default -> {
            }
        }
    }

    public void glPointParameterfv (int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glPointParameterfv", pname, params);
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        if (pname == GL_POINT_DISTANCE_ATTENUATION && params.length >= 3) {
            System.arraycopy(params, 0, pointDistanceAttenuation, 0, 3);
        } else if (params.length > 0) {
            glPointParameterf(pname, params[0]);
        }
    }

    public void glTexEnvi (int target, int pname, int param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexEnvi", target, pname, param);
        ensureBound();
        if (target == GL_TEXTURE_ENV && pname == GL_TEXTURE_ENV_MODE) {
            textureEnvMode = param;
        }
    }

    public void glTexEnviv (int target, int pname, int[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexEnviv", target, pname, params);
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        if (target == GL_TEXTURE_ENV && pname == GL_TEXTURE_ENV_MODE && params.length > 0) {
            textureEnvMode = params[0];
        } else if (target == GL_TEXTURE_ENV && pname == GL_TEXTURE_ENV_COLOR && params.length >= 4) {
            for (int i = 0; i < 4; i++) {
                textureEnvColor[i] = clampUnit(params[i] / 255.0f);
            }
        }
    }

    public void glTexParameterfv (int target, int pname, float[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexParameterfv", target, pname, params);
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        if (target != GL_TEXTURE_2D) {
            setError(GL_INVALID_ENUM);
            return;
        }
        if (pname == GL_TEXTURE_CROP_RECT_OES) {
            if (params.length < 4) {
                setError(GL_INVALID_VALUE);
                return;
            }
            TextureState textureState = textureStates.computeIfAbsent(boundTexture2d, ignored -> new TextureState());
            System.arraycopy(params, 0, textureState.cropRect, 0, 4);
            textureState.hasCropRect = true;
        } else if (params.length > 0) {
            applyTextureParameter(target, pname, Math.round(params[0]));
        }
    }

    public void glTexParameteri (int target, int pname, int param) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexParameteri", target, pname, param);
        ensureBound();
        applyTextureParameter(target, pname, param);
    }

    public void glTexParameteriv (int target, int pname, int[] params) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glTexParameteriv", target, pname, params);
        ensureBound();
        if (params == null) {
            throw new NullPointerException("params");
        }
        if (target != GL_TEXTURE_2D) {
            setError(GL_INVALID_ENUM);
            return;
        }
        if (pname == GL_TEXTURE_CROP_RECT_OES) {
            if (params.length < 4) {
                setError(GL_INVALID_VALUE);
                return;
            }
            TextureState textureState = textureStates.computeIfAbsent(boundTexture2d, ignored -> new TextureState());
            for (int i = 0; i < 4; i++) {
                textureState.cropRect[i] = params[i];
            }
            textureState.hasCropRect = true;
        } else if (params.length > 0) {
            applyTextureParameter(target, pname, params[0]);
        }
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
        if (index < 0 || index >= MAX_PALETTE_MATRICES) {
            setError(GL_INVALID_VALUE);
            return;
        }
        currentPaletteMatrixIndex = index;
    }

    public void glLoadPaletteFromModelViewMatrixOES () {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glLoadPaletteFromModelViewMatrixOES");
        ensureBound();
        System.arraycopy(modelViewMatrix, 0, paletteMatrices[currentPaletteMatrixIndex], 0, 16);
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
        drawTexOes(x, y, z, width, height);
    }

    public void glDrawTexiOES (int x, int y, int z, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDrawTexiOES", x, y, z, width, height);
        drawTexOes(x, y, z, width, height);
    }

    public void glDrawTexfOES (float x, float y, float z, float width, float height) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.OpglGraphics", "glDrawTexfOES", x, y, z, width, height);
        drawTexOes(x, y, z, width, height);
    }

    private void ensureSurfaceBuffers() {
        int size = Math.max(1, surfaceWidth * surfaceHeight);
        if (surfacePixels.length != size) {
            surfacePixels = new int[size];
            surfaceDepth = new float[size];
            surfaceStencil = new int[size];
        }
    }

    private void forceSurfaceOpaque() {
        for (int i = 0; i < surfacePixels.length; i++) {
            surfacePixels[i] |= 0xFF000000;
        }
    }

    private void applyTextureParameter(int target, int pname, int param) {
        if (target != GL_TEXTURE_2D) {
            return;
        }
        TextureState textureState = textureStates.computeIfAbsent(boundTexture2d, ignored -> new TextureState());
        if (pname == GL_TEXTURE_WRAP_S) {
            textureState.wrapS = param;
        } else if (pname == GL_TEXTURE_WRAP_T) {
            textureState.wrapT = param;
        } else if (pname == GL_TEXTURE_MIN_FILTER) {
            textureState.minFilter = param;
        } else if (pname == GL_TEXTURE_MAG_FILTER) {
            textureState.magFilter = param;
        }
    }

    private static int textureParameter(TextureState textureState, int pname) {
        return switch (pname) {
            case GL_TEXTURE_WRAP_S -> textureState.wrapS;
            case GL_TEXTURE_WRAP_T -> textureState.wrapT;
            case GL_TEXTURE_MIN_FILTER -> textureState.minFilter;
            case GL_TEXTURE_MAG_FILTER -> textureState.magFilter;
            default -> 0;
        };
    }

    private void drawTexOes(float x, float y, float z, float width, float height) {
        ensureBound();
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }
        if (!enabledCaps.contains(GL_TEXTURE_2D)) {
            fillDrawTextureRect(x, y, z, width, height, currentColorArgb);
            return;
        }
        TextureState textureState = textureStates.get(boundTexture2d);
        if (textureState == null
                || textureState.pixels == null
                || textureState.width <= 0
                || textureState.height <= 0) {
            return;
        }

        int dstLeft = Math.round(x);
        int dstTop = Math.round(surfaceHeight - y - height);
        int dstRight = Math.round(x + width);
        int dstBottom = Math.round(dstTop + height);
        int clippedLeft = Math.max(0, Math.min(surfaceWidth, dstLeft));
        int clippedTop = Math.max(0, Math.min(surfaceHeight, dstTop));
        int clippedRight = Math.max(0, Math.min(surfaceWidth, dstRight));
        int clippedBottom = Math.max(0, Math.min(surfaceHeight, dstBottom));
        if (clippedLeft >= clippedRight || clippedTop >= clippedBottom) {
            return;
        }

        float cropX = textureState.hasCropRect ? textureState.cropRect[0] : 0.0f;
        float cropY = textureState.hasCropRect ? textureState.cropRect[1] : 0.0f;
        float cropWidth = textureState.hasCropRect ? textureState.cropRect[2] : textureState.width;
        float cropHeight = textureState.hasCropRect ? textureState.cropRect[3] : textureState.height;
        if (cropWidth == 0.0f || cropHeight == 0.0f) {
            return;
        }

        float depth = clampUnit(z);
        for (int py = clippedTop; py < clippedBottom; py++) {
            float v = (py - dstTop + 0.5f) / height;
            float texY = mapDrawTextureCoordinate(cropY, cropHeight, textureState.height, v, true);
            for (int px = clippedLeft; px < clippedRight; px++) {
                float u = (px - dstLeft + 0.5f) / width;
                float texX = mapDrawTextureCoordinate(cropX, cropWidth, textureState.width, u, false);
                int texel = sampleTextureTexel(textureState, texX, texY);
                int color = applyTextureEnv(currentColorArgb, texel);
                if (enabledCaps.contains(GL_ALPHA_TEST) && !passesAlphaTest((color >>> 24) & 0xFF)) {
                    continue;
                }
                writePixel(px, py, depth, color);
            }
        }
    }

    private void fillDrawTextureRect(float x, float y, float z, float width, float height, int color) {
        int dstLeft = Math.round(x);
        int dstTop = Math.round(surfaceHeight - y - height);
        int dstRight = Math.round(x + width);
        int dstBottom = Math.round(dstTop + height);
        int clippedLeft = Math.max(0, Math.min(surfaceWidth, dstLeft));
        int clippedTop = Math.max(0, Math.min(surfaceHeight, dstTop));
        int clippedRight = Math.max(0, Math.min(surfaceWidth, dstRight));
        int clippedBottom = Math.max(0, Math.min(surfaceHeight, dstBottom));
        if (clippedLeft >= clippedRight || clippedTop >= clippedBottom) {
            return;
        }
        if (enabledCaps.contains(GL_ALPHA_TEST) && !passesAlphaTest((color >>> 24) & 0xFF)) {
            return;
        }
        float depth = clampUnit(z);
        for (int py = clippedTop; py < clippedBottom; py++) {
            for (int px = clippedLeft; px < clippedRight; px++) {
                writePixel(px, py, depth, color);
            }
        }
    }

    private static float mapDrawTextureCoordinate(float cropStart, float cropSize, int textureExtent, float fraction, boolean vertical) {
        float start = cropStart;
        float span = cropSize;
        if (span < 0.0f) {
            if (vertical && start <= 0.0f) {
                start = textureExtent + start + span;
            } else {
                start += span;
            }
            span = -span;
        }
        return start + fraction * span;
    }

    private void copyTextureRegion(int[] source, int sourceWidth, int sourceHeight, TextureState textureState, int xoffset, int yoffset) {
        if (source == null || sourceWidth <= 0 || sourceHeight <= 0) {
            return;
        }
        for (int row = 0; row < sourceHeight; row++) {
            int dstY = yoffset + row;
            if (dstY < 0 || dstY >= textureState.height) {
                continue;
            }
            int srcIndex = row * sourceWidth;
            int dstIndex = dstY * textureState.width + Math.max(0, xoffset);
            int copyX = Math.max(0, -xoffset);
            int copyWidth = Math.min(sourceWidth - copyX, textureState.width - Math.max(0, xoffset));
            if (copyWidth <= 0) {
                continue;
            }
            System.arraycopy(source, srcIndex + copyX, textureState.pixels, dstIndex, copyWidth);
        }
    }

    private int[] copySurfacePixels(int x, int y, int width, int height) {
        int[] copy = new int[Math.max(0, width * height)];
        if (width <= 0 || height <= 0 || surfacePixels.length == 0) {
            return copy;
        }
        for (int row = 0; row < height; row++) {
            int srcY = surfaceHeight - y - height + row;
            if (srcY < 0 || srcY >= surfaceHeight) {
                continue;
            }
            for (int col = 0; col < width; col++) {
                int srcX = x + col;
                if (srcX < 0 || srcX >= surfaceWidth) {
                    continue;
                }
                copy[row * width + col] = surfacePixels[srcY * surfaceWidth + srcX];
            }
        }
        return copy;
    }

    private void applyMaterial(int face, int pname, float[] params) {
        if (face == GL_FRONT_AND_BACK) {
            applyMaterial(GL_FRONT, pname, params);
            applyMaterial(GL_BACK, pname, params);
            return;
        }
        var material = materialForFace(face);
        switch (pname) {
            case GL_AMBIENT -> copyFloats(params, material.ambient, 4);
            case GL_DIFFUSE -> copyFloats(params, material.diffuse, 4);
            case GL_EMISSION -> copyFloats(params, material.emission, 4);
            case GL_SPECULAR -> copyFloats(params, material.specular, 4);
            case GL_AMBIENT_AND_DIFFUSE -> {
                copyFloats(params, material.ambient, 4);
                copyFloats(params, material.diffuse, 4);
            }
            case GL_SHININESS -> {
                if (params.length > 0) {
                    material.shininess = Math.max(0.0f, Math.min(128.0f, params[0]));
                }
            }
            default -> {
            }
        }
    }

    private void applyMaterialScalar(int face, int pname, float param) {
        if (face == GL_FRONT_AND_BACK) {
            applyMaterialScalar(GL_FRONT, pname, param);
            applyMaterialScalar(GL_BACK, pname, param);
            return;
        }
        var material = materialForFace(face);
        if (pname == GL_SHININESS) {
            material.shininess = Math.max(0.0f, Math.min(128.0f, param));
        }
    }

    private void applyLightScalar(LightState lightState, int pname, float param) {
        switch (pname) {
            case GL_SPOT_EXPONENT -> lightState.spotExponent = Math.max(0.0f, Math.min(128.0f, param));
            case GL_SPOT_CUTOFF -> lightState.spotCutoff = param == 180.0f ? 180.0f : Math.max(0.0f, Math.min(90.0f, param));
            case GL_CONSTANT_ATTENUATION -> lightState.constantAttenuation = Math.max(0.0f, param);
            case GL_LINEAR_ATTENUATION -> lightState.linearAttenuation = Math.max(0.0f, param);
            case GL_QUADRATIC_ATTENUATION -> lightState.quadraticAttenuation = Math.max(0.0f, param);
            default -> {
            }
        }
    }

    private void applyColorMaterial() {
        if (!enabledCaps.contains(GL_COLOR_MATERIAL)) {
            return;
        }
        float[] color = new float[] {currentColorR, currentColorG, currentColorB, currentColorA};
        copyFloats(color, frontMaterial.ambient, 4);
        copyFloats(color, frontMaterial.diffuse, 4);
        if (lightModelTwoSide) {
            copyFloats(color, backMaterial.ambient, 4);
            copyFloats(color, backMaterial.diffuse, 4);
        }
    }

    private MaterialState materialForFace(int face) {
        return face == GL_BACK ? backMaterial : frontMaterial;
    }

    private static void copyFloats(float[] source, float[] destination, int count) {
        System.arraycopy(source, 0, destination, 0, Math.min(Math.min(source.length, destination.length), count));
    }

    private static void writeFloatValues(float[] destination, float... values) {
        System.arraycopy(values, 0, destination, 0, Math.min(destination.length, values.length));
    }

    private static void writeColorFloats(float[] destination, int argb) {
        writeFloatValues(
                destination,
                ((argb >>> 16) & 0xFF) / 255.0f,
                ((argb >>> 8) & 0xFF) / 255.0f,
                (argb & 0xFF) / 255.0f,
                ((argb >>> 24) & 0xFF) / 255.0f
        );
    }

    private void renderDrawArrays(int mode, int first, int count) {
        switch (mode) {
            case GL_TRIANGLES -> {
                for (int i = 0; i + 2 < count; i += 3) {
                    drawTriangle(fetchVertex(first + i), fetchVertex(first + i + 1), fetchVertex(first + i + 2));
                }
            }
            case GL_TRIANGLE_STRIP -> {
                for (int i = 0; i + 2 < count; i++) {
                    ClipVertex v0 = fetchVertex(first + i);
                    ClipVertex v1 = fetchVertex(first + i + 1);
                    ClipVertex v2 = fetchVertex(first + i + 2);
                    if ((i & 1) == 0) {
                        drawTriangle(v0, v1, v2);
                    } else {
                        drawTriangle(v1, v0, v2);
                    }
                }
            }
            case GL_TRIANGLE_FAN -> {
                ClipVertex center = fetchVertex(first);
                for (int i = 1; i + 1 < count; i++) {
                    drawTriangle(center, fetchVertex(first + i), fetchVertex(first + i + 1));
                }
            }
            case GL_POINTS -> {
                for (int i = 0; i < count; i++) {
                    drawPoint(fetchVertex(first + i));
                }
            }
            case GL_LINES -> {
                for (int i = 0; i + 1 < count; i += 2) {
                    drawLine(fetchVertex(first + i), fetchVertex(first + i + 1));
                }
            }
            case GL_LINE_STRIP -> {
                for (int i = 0; i + 1 < count; i++) {
                    drawLine(fetchVertex(first + i), fetchVertex(first + i + 1));
                }
            }
            case GL_LINE_LOOP -> {
                for (int i = 0; i + 1 < count; i++) {
                    drawLine(fetchVertex(first + i), fetchVertex(first + i + 1));
                }
                if (count > 1) {
                    drawLine(fetchVertex(first + count - 1), fetchVertex(first));
                }
            }
            default -> {
            }
        }
    }

    private void renderDrawElements(int mode, int[] indices) {
        switch (mode) {
            case GL_TRIANGLES -> {
                for (int i = 0; i + 2 < indices.length; i += 3) {
                    drawTriangle(fetchVertex(indices[i]), fetchVertex(indices[i + 1]), fetchVertex(indices[i + 2]));
                }
            }
            case GL_TRIANGLE_STRIP -> {
                for (int i = 0; i + 2 < indices.length; i++) {
                    ClipVertex v0 = fetchVertex(indices[i]);
                    ClipVertex v1 = fetchVertex(indices[i + 1]);
                    ClipVertex v2 = fetchVertex(indices[i + 2]);
                    if ((i & 1) == 0) {
                        drawTriangle(v0, v1, v2);
                    } else {
                        drawTriangle(v1, v0, v2);
                    }
                }
            }
            case GL_TRIANGLE_FAN -> {
                if (indices.length == 0) {
                    return;
                }
                ClipVertex center = fetchVertex(indices[0]);
                for (int i = 1; i + 1 < indices.length; i++) {
                    drawTriangle(center, fetchVertex(indices[i]), fetchVertex(indices[i + 1]));
                }
            }
            case GL_POINTS -> {
                for (int index : indices) {
                    drawPoint(fetchVertex(index));
                }
            }
            case GL_LINES -> {
                for (int i = 0; i + 1 < indices.length; i += 2) {
                    drawLine(fetchVertex(indices[i]), fetchVertex(indices[i + 1]));
                }
            }
            case GL_LINE_STRIP -> {
                for (int i = 0; i + 1 < indices.length; i++) {
                    drawLine(fetchVertex(indices[i]), fetchVertex(indices[i + 1]));
                }
            }
            case GL_LINE_LOOP -> {
                for (int i = 0; i + 1 < indices.length; i++) {
                    drawLine(fetchVertex(indices[i]), fetchVertex(indices[i + 1]));
                }
                if (indices.length > 1) {
                    drawLine(fetchVertex(indices[indices.length - 1]), fetchVertex(indices[0]));
                }
            }
            default -> {
            }
        }
    }

    private ClipVertex fetchVertex(int vertexIndex) {
        float[] pos = readVertexComponents(vertexArrayBinding, vertexIndex, 4, 1.0f);
        float[] tex = texCoordArrayBinding == null || !enabledClientStates.contains(GL_TEXTURE_COORD_ARRAY)
                ? new float[] {currentTextureCoords[0], currentTextureCoords[1], currentTextureCoords[2], currentTextureCoords[3]}
                : readVertexComponents(texCoordArrayBinding, vertexIndex, 4, 1.0f);
        float[] model = transformPosition(vertexIndex, pos);
        float[] clip = multiplyVec4(projectionMatrix, model[0], model[1], model[2], model[3]);
        float[] texCoord = multiplyVec4(textureMatrix, tex[0], tex[1], tex[2], tex[3]);
        int vertexColor = resolveVertexColor(vertexIndex, model);
        float resolvedPointSize = pointSizeArrayBinding != null && enabledClientStates.contains(GL_POINT_SIZE_ARRAY_OES)
                ? readVertexComponents(pointSizeArrayBinding, vertexIndex, 1, 1.0f)[0]
                : pointSize;
        resolvedPointSize = resolvePointSize(resolvedPointSize, -model[2]);
        return new ClipVertex(
                clip[0],
                clip[1],
                clip[2],
                clip[3],
                texCoord[0],
                texCoord[1],
                -model[2],
                model[0],
                model[1],
                model[2],
                resolvedPointSize,
                vertexColor
        );
    }

    private float[] transformPosition(int vertexIndex, float[] position) {
        if (!usesMatrixPalette()) {
            return multiplyVec4(modelViewMatrix, position[0], position[1], position[2], position[3]);
        }
        float[] indices = readVertexComponents(matrixIndexArrayBinding, vertexIndex, matrixIndexArrayBinding.componentCount, 0.0f);
        float[] weights = readVertexComponents(weightArrayBinding, vertexIndex, weightArrayBinding.componentCount, 0.0f);
        float[] accum = new float[4];
        float totalWeight = 0.0f;
        int limit = Math.min(indices.length, weights.length);
        for (int i = 0; i < limit; i++) {
            float weight = weights[i];
            if (weight == 0.0f) {
                continue;
            }
            totalWeight += weight;
            float[] matrix = paletteMatrix((int) indices[i]);
            float[] transformed = multiplyVec4(matrix, position[0], position[1], position[2], position[3]);
            accum[0] += transformed[0] * weight;
            accum[1] += transformed[1] * weight;
            accum[2] += transformed[2] * weight;
            accum[3] += transformed[3] * weight;
        }
        if (totalWeight <= 0.000001f) {
            return multiplyVec4(modelViewMatrix, position[0], position[1], position[2], position[3]);
        }
        return accum;
    }

    private float[] transformNormal(int vertexIndex) {
        float[] normal = normalArrayBinding == null || !enabledClientStates.contains(GL_NORMAL_ARRAY)
                ? new float[] {currentNormal[0], currentNormal[1], currentNormal[2], 0.0f}
                : readVertexComponents(normalArrayBinding, vertexIndex, 3, 0.0f);
        float nx = normal[0];
        float ny = normal[1];
        float nz = normal[2];
        float[] transformed;
        if (usesMatrixPalette()) {
            float[] indices = readVertexComponents(matrixIndexArrayBinding, vertexIndex, matrixIndexArrayBinding.componentCount, 0.0f);
            float[] weights = readVertexComponents(weightArrayBinding, vertexIndex, weightArrayBinding.componentCount, 0.0f);
            transformed = new float[4];
            float totalWeight = 0.0f;
            int limit = Math.min(indices.length, weights.length);
            for (int i = 0; i < limit; i++) {
                float weight = weights[i];
                if (weight == 0.0f) {
                    continue;
                }
                totalWeight += weight;
                float[] matrix = paletteMatrix((int) indices[i]);
                float[] weighted = multiplyVec4(matrix, nx, ny, nz, 0.0f);
                transformed[0] += weighted[0] * weight;
                transformed[1] += weighted[1] * weight;
                transformed[2] += weighted[2] * weight;
            }
            if (totalWeight <= 0.0f) {
                transformed = multiplyVec4(modelViewMatrix, nx, ny, nz, 0.0f);
            }
        } else {
            transformed = multiplyVec4(modelViewMatrix, nx, ny, nz, 0.0f);
        }
        normalize3(transformed);
        return transformed;
    }

    private int resolveVertexColor(int vertexIndex, float[] modelPosition) {
        int baseColor = currentColorArgb;
        if (colorArrayBinding != null && enabledClientStates.contains(GL_COLOR_ARRAY)) {
            baseColor = readColor(vertexIndex);
        }
        if (!enabledCaps.contains(GL_LIGHTING)) {
            return baseColor;
        }
        float[] normal = transformNormal(vertexIndex);
        if (normal == null) {
            return baseColor;
        }
        return applyLighting(normal, modelPosition);
    }

    private int readColor(int vertexIndex) {
        if (colorArrayBinding == null) {
            return currentColorArgb;
        }
        float[] components = readVertexComponents(colorArrayBinding, vertexIndex, 4, 1.0f);
        if (colorArrayBinding.type == GL_FLOAT) {
            return (clampColor(components[3]) << 24)
                    | (clampColor(components[0]) << 16)
                    | (clampColor(components[1]) << 8)
                    | clampColor(components[2]);
        }
        return ((int) components[3] << 24)
                | ((int) components[0] << 16)
                | ((int) components[1] << 8)
                | (int) components[2];
    }

    private int applyLighting(float[] normal, float[] modelPosition) {
        float[] color = new float[] {
                frontMaterial.emission[0] + lightModelAmbient[0] * frontMaterial.ambient[0],
                frontMaterial.emission[1] + lightModelAmbient[1] * frontMaterial.ambient[1],
                frontMaterial.emission[2] + lightModelAmbient[2] * frontMaterial.ambient[2],
                frontMaterial.diffuse[3]
        };
        for (var entry : lightStates.entrySet()) {
            if (!enabledCaps.contains(entry.getKey())) {
                continue;
            }
            var light = entry.getValue();
            color[0] += light.ambient[0] * frontMaterial.ambient[0];
            color[1] += light.ambient[1] * frontMaterial.ambient[1];
            color[2] += light.ambient[2] * frontMaterial.ambient[2];
            float[] direction = lightDirection(light.position, modelPosition);
            float attenuation = lightAttenuation(light, modelPosition);
            float spot = spotEffect(light, direction);
            float diffuse = Math.max(0.0f, dot3(normal, direction));
            float[] eye = new float[] {-modelPosition[0], -modelPosition[1], -modelPosition[2]};
            normalize3(eye);
            float[] half = new float[] {direction[0] + eye[0], direction[1] + eye[1], direction[2] + eye[2]};
            normalize3(half);
            float specular = frontMaterial.shininess <= 0.0f
                    ? 0.0f
                    : (float) Math.pow(Math.max(0.0f, dot3(normal, half)), frontMaterial.shininess);
            float scale = attenuation * spot;
            color[0] += scale * (light.diffuse[0] * frontMaterial.diffuse[0] * diffuse
                    + light.specular[0] * frontMaterial.specular[0] * specular);
            color[1] += scale * (light.diffuse[1] * frontMaterial.diffuse[1] * diffuse
                    + light.specular[1] * frontMaterial.specular[1] * specular);
            color[2] += scale * (light.diffuse[2] * frontMaterial.diffuse[2] * diffuse
                    + light.specular[2] * frontMaterial.specular[2] * specular);
        }
        return (clampColor(frontMaterial.diffuse[3]) << 24)
                | (clampColor(color[0]) << 16)
                | (clampColor(color[1]) << 8)
                | clampColor(color[2]);
    }

    private float resolvePointSize(float baseSize, float eyeDepth) {
        float size = Math.max(0.0f, baseSize);
        if (enabledCaps.contains(GL_POINT_SPRITE_OES)) {
            float distance = Math.max(0.0f, eyeDepth);
            float attenuation = pointDistanceAttenuation[0]
                    + pointDistanceAttenuation[1] * distance
                    + pointDistanceAttenuation[2] * distance * distance;
            if (attenuation > 0.000001f) {
                size /= (float) Math.sqrt(attenuation);
            }
        }
        if (pointFadeThresholdSize > 0.0f) {
            size = Math.max(size, Math.min(pointFadeThresholdSize, pointSizeMax));
        }
        return Math.max(1.0f, Math.min(pointSizeMax, Math.max(pointSizeMin, size)));
    }

    private boolean usesMatrixPalette() {
        return enabledCaps.contains(GL_MATRIX_PALETTE_OES)
                && matrixIndexArrayBinding != null
                && weightArrayBinding != null
                && enabledClientStates.contains(GL_MATRIX_INDEX_ARRAY_OES)
                && enabledClientStates.contains(GL_WEIGHT_ARRAY_OES);
    }

    private float[] paletteMatrix(int index) {
        if (index < 0 || index >= paletteMatrices.length) {
            return modelViewMatrix;
        }
        return paletteMatrices[index];
    }

    private Vertex projectVertex(ClipVertex vertex) {
        if (Math.abs(vertex.clipW()) < 0.000001f) {
            return null;
        }
        float invW = 1.0f / vertex.clipW();
        float ndcX = vertex.clipX() * invW;
        float ndcY = vertex.clipY() * invW;
        float ndcZ = vertex.clipZ() * invW;
        float screenX = viewportX + ((ndcX + 1.0f) * 0.5f * viewportWidth);
        float screenY = viewportY + ((1.0f - (ndcY + 1.0f) * 0.5f) * viewportHeight);
        float screenZ = depthRangeNear + ((ndcZ + 1.0f) * 0.5f * (depthRangeFar - depthRangeNear));
        screenZ = Math.max(0.0f, Math.min(1.0f, screenZ));
        return new Vertex(
                screenX,
                screenY,
                screenZ,
                vertex.clipW(),
                vertex.u(),
                vertex.v(),
                vertex.eyeDepth(),
                vertex.color()
        );
    }

    private void drawPoint(ClipVertex clipVertex) {
        if (!isInsideClipVolume(clipVertex)) {
            return;
        }
        Vertex vertex = projectVertex(clipVertex);
        if (vertex == null) {
            return;
        }
        if (vertex.w() <= 0.0f) {
            return;
        }
        float pointExtent = Math.max(1.0f, clipVertex.pointSize());
        float half = pointExtent * 0.5f;
        int minX = Math.max(0, (int) Math.floor(vertex.x() - half));
        int maxX = Math.min(surfaceWidth - 1, (int) Math.ceil(vertex.x() + half));
        int minY = Math.max(0, (int) Math.floor(vertex.y() - half));
        int maxY = Math.min(surfaceHeight - 1, (int) Math.ceil(vertex.y() + half));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float u = pointExtent <= 1.0f ? 0.5f : (x + 0.5f - (vertex.x() - half)) / pointExtent;
                float v = pointExtent <= 1.0f ? 0.5f : (y + 0.5f - (vertex.y() - half)) / pointExtent;
                int color = shadeFragment(u, v, vertex.eyeDepth(), vertex.color());
                writePixel(x, y, vertex.z(), color);
            }
        }
    }

    private void drawLine(ClipVertex startClip, ClipVertex endClip) {
        if (!isInsideClipVolume(startClip) || !isInsideClipVolume(endClip)) {
            return;
        }
        Vertex start = projectVertex(startClip);
        Vertex end = projectVertex(endClip);
        if (start == null || end == null || start.w() <= 0.0f || end.w() <= 0.0f) {
            return;
        }
        float dx = end.x() - start.x();
        float dy = end.y() - start.y();
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy))));
        int radius = Math.max(0, Math.round(lineWidth * 0.5f) - 1);
        for (int step = 0; step <= steps; step++) {
            float t = step / (float) steps;
            int x = Math.round(lerp(start.x(), end.x(), t));
            int y = Math.round(lerp(start.y(), end.y(), t));
            float depth = lerp(start.z(), end.z(), t);
            float eyeDepth = lerp(start.eyeDepth(), end.eyeDepth(), t);
            int color = shadeFragment(
                    lerp(start.u(), end.u(), t),
                    lerp(start.v(), end.v(), t),
                    eyeDepth,
                    lerpColor(start.color(), end.color(), t)
            );
            for (int oy = -radius; oy <= radius; oy++) {
                for (int ox = -radius; ox <= radius; ox++) {
                    int px = x + ox;
                    int py = y + oy;
                    if (px >= 0 && px < surfaceWidth && py >= 0 && py < surfaceHeight) {
                        writePixel(px, py, depth, color);
                    }
                }
            }
        }
    }

    private void drawTriangle(ClipVertex v0, ClipVertex v1, ClipVertex v2) {
        List<ClipVertex> polygon = clipTriangleToFrustum(v0, v1, v2);
        if (polygon.size() < 3) {
            return;
        }
        Vertex first = projectVertex(polygon.get(0));
        if (first == null) {
            return;
        }
        for (int i = 1; i + 1 < polygon.size(); i++) {
            Vertex second = projectVertex(polygon.get(i));
            Vertex third = projectVertex(polygon.get(i + 1));
            if (second == null || third == null) {
                continue;
            }
            rasterizeTriangle(first, second, third);
        }
    }

    private void rasterizeTriangle(Vertex v0, Vertex v1, Vertex v2) {
        if (v0.w() <= 0.0f || v1.w() <= 0.0f || v2.w() <= 0.0f) {
            return;
        }
        float area = edge(v0.x(), v0.y(), v1.x(), v1.y(), v2.x(), v2.y());
        if (Math.abs(area) < 0.00001f) {
            return;
        }
        if (enabledCaps.contains(GL_CULL_FACE)) {
            boolean frontFacing = frontFacing(area);
            if ((cullFaceMode == GL_BACK && !frontFacing) || (cullFaceMode == GL_FRONT && frontFacing)) {
                return;
            }
        }
        int minX = Math.max(0, (int) Math.floor(Math.min(v0.x(), Math.min(v1.x(), v2.x()))));
        int maxX = Math.min(surfaceWidth - 1, (int) Math.ceil(Math.max(v0.x(), Math.max(v1.x(), v2.x()))));
        int minY = Math.max(0, (int) Math.floor(Math.min(v0.y(), Math.min(v1.y(), v2.y()))));
        int maxY = Math.min(surfaceHeight - 1, (int) Math.ceil(Math.max(v0.y(), Math.max(v1.y(), v2.y()))));
        float invW0 = 1.0f / v0.w();
        float invW1 = 1.0f / v1.w();
        float invW2 = 1.0f / v2.w();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5f;
                float py = y + 0.5f;
                float w0 = edge(v1.x(), v1.y(), v2.x(), v2.y(), px, py) / area;
                float w1 = edge(v2.x(), v2.y(), v0.x(), v0.y(), px, py) / area;
                float w2 = edge(v0.x(), v0.y(), v1.x(), v1.y(), px, py) / area;
                if (w0 < -0.0001f || w1 < -0.0001f || w2 < -0.0001f) {
                    continue;
                }
                float reciprocal = w0 * invW0 + w1 * invW1 + w2 * invW2;
                if (reciprocal == 0.0f) {
                    continue;
                }
                float u = (w0 * v0.u() * invW0 + w1 * v1.u() * invW1 + w2 * v2.u() * invW2) / reciprocal;
                float v = (w0 * v0.v() * invW0 + w1 * v1.v() * invW1 + w2 * v2.v() * invW2) / reciprocal;
                float depth = w0 * v0.z() + w1 * v1.z() + w2 * v2.z();
                if (enabledCaps.contains(GL_POLYGON_OFFSET_FILL)) {
                    depth += polygonOffsetFactor * 0.00001f + polygonOffsetUnits * 0.000001f;
                }
                depth = clampUnit(depth);
                float eyeDepth = w0 * v0.eyeDepth() + w1 * v1.eyeDepth() + w2 * v2.eyeDepth();
                int baseColor = shadeModel == GL_FLAT
                        ? v0.color()
                        : interpolateColor(v0.color(), v1.color(), v2.color(), w0, w1, w2);
                int color = shadeFragment(u, v, eyeDepth, baseColor);
                writePixel(x, y, depth, color);
            }
        }
    }

    private List<ClipVertex> clipTriangleToFrustum(ClipVertex v0, ClipVertex v1, ClipVertex v2) {
        List<ClipVertex> polygon = new ArrayList<>(3);
        polygon.add(v0);
        polygon.add(v1);
        polygon.add(v2);
        for (int plane = 0; plane < 6 && !polygon.isEmpty(); plane++) {
            polygon = clipPolygonToPlane(polygon, plane);
        }
        for (int plane = 0; plane < 6 && !polygon.isEmpty(); plane++) {
            if (enabledCaps.contains(GL_CLIP_PLANE0 + plane)) {
                polygon = clipPolygonToPlane(polygon, 6 + plane);
            }
        }
        return polygon;
    }

    private List<ClipVertex> clipPolygonToPlane(List<ClipVertex> polygon, int plane) {
        if (polygon.isEmpty()) {
            return List.of();
        }
        List<ClipVertex> output = new ArrayList<>(polygon.size() + 2);
        ClipVertex previous = polygon.get(polygon.size() - 1);
        float previousDistance = clipDistance(previous, plane);
        boolean previousInside = previousDistance >= 0.0f;
        for (ClipVertex current : polygon) {
            float currentDistance = clipDistance(current, plane);
            boolean currentInside = currentDistance >= 0.0f;
            if (currentInside != previousInside) {
                output.add(interpolateClipVertex(previous, current, previousDistance, currentDistance));
            }
            if (currentInside) {
                output.add(current);
            }
            previous = current;
            previousDistance = currentDistance;
            previousInside = currentInside;
        }
        return output;
    }

    private boolean isInsideClipVolume(ClipVertex vertex) {
        if (vertex.clipW() <= 0.0f) {
            return false;
        }
        for (int plane = 0; plane < 6; plane++) {
            if (clipDistance(vertex, plane) < 0.0f) {
                return false;
            }
        }
        for (int plane = 0; plane < 6; plane++) {
            if (enabledCaps.contains(GL_CLIP_PLANE0 + plane) && clipDistance(vertex, 6 + plane) < 0.0f) {
                return false;
            }
        }
        return true;
    }

    private float clipDistance(ClipVertex vertex, int plane) {
        if (plane >= 6) {
            float[] equation = clipPlanes[plane - 6];
            return equation[0] * vertex.modelX()
                    + equation[1] * vertex.modelY()
                    + equation[2] * vertex.modelZ()
                    + equation[3];
        }
        return switch (plane) {
            case 0 -> vertex.clipW() + vertex.clipX();
            case 1 -> vertex.clipW() - vertex.clipX();
            case 2 -> vertex.clipW() + vertex.clipY();
            case 3 -> vertex.clipW() - vertex.clipY();
            case 4 -> vertex.clipW() + vertex.clipZ();
            case 5 -> vertex.clipW() - vertex.clipZ();
            default -> throw new IllegalArgumentException("Unsupported clip plane: " + plane);
        };
    }

    private static ClipVertex interpolateClipVertex(ClipVertex start, ClipVertex end, float startDistance, float endDistance) {
        float denominator = startDistance - endDistance;
        float t = Math.abs(denominator) < 0.000001f ? 0.0f : startDistance / denominator;
        t = Math.max(0.0f, Math.min(1.0f, t));
        return new ClipVertex(
                lerp(start.clipX(), end.clipX(), t),
                lerp(start.clipY(), end.clipY(), t),
                lerp(start.clipZ(), end.clipZ(), t),
                lerp(start.clipW(), end.clipW(), t),
                lerp(start.u(), end.u(), t),
                lerp(start.v(), end.v(), t),
                lerp(start.eyeDepth(), end.eyeDepth(), t),
                lerp(start.modelX(), end.modelX(), t),
                lerp(start.modelY(), end.modelY(), t),
                lerp(start.modelZ(), end.modelZ(), t),
                lerp(start.pointSize(), end.pointSize(), t),
                lerpColor(start.color(), end.color(), t)
        );
    }

    private int shadeFragment(float u, float v, float eyeDepth, int baseColor) {
        int color = baseColor;
        if (enabledCaps.contains(GL_TEXTURE_2D)) {
            TextureState textureState = textureStates.get(boundTexture2d);
            if (textureState != null && textureState.pixels != null) {
                int texel = sampleTexture(textureState, u, v);
                color = applyTextureEnv(color, texel);
            }
        }
        if (enabledCaps.contains(GL_ALPHA_TEST)) {
            int alpha = (color >>> 24) & 0xFF;
            if (!passesAlphaTest(alpha)) {
                return 0;
            }
        }
        if (enabledCaps.contains(GL_FOG)) {
            float fogFactor = fogFactor(eyeDepth);
            color = lerpColor(fogColorArgb, color, fogFactor);
        }
        return color;
    }

    private float fogFactor(float eyeDepth) {
        float distance = Math.max(0.0f, eyeDepth);
        float factor;
        if (fogMode == GL_EXP) {
            factor = (float) Math.exp(-fogDensity * distance);
        } else if (fogMode == GL_EXP2) {
            float densityDistance = fogDensity * distance;
            factor = (float) Math.exp(-(densityDistance * densityDistance));
        } else if (fogEnd <= fogStart) {
            factor = 1.0f;
        } else {
            factor = (fogEnd - distance) / (fogEnd - fogStart);
        }
        return Math.max(0.0f, Math.min(1.0f, factor));
    }

    private void writePixel(int x, int y, float depth, int color) {
        if (color == 0) {
            return;
        }
        if (enabledCaps.contains(GL_SCISSOR_TEST) && !insideScissor(x, y)) {
            return;
        }
        int index = y * surfaceWidth + x;
        boolean stencilEnabled = enabledCaps.contains(GL_STENCIL_TEST);
        if (stencilEnabled && !passesStencilTest(surfaceStencil[index])) {
            applyStencilOp(index, stencilFailOp);
            return;
        }
        boolean depthPass = !enabledCaps.contains(GL_DEPTH_TEST) || passesDepthTest(depth, surfaceDepth[index]);
        if (!depthPass) {
            if (stencilEnabled) {
                applyStencilOp(index, stencilDepthFailOp);
            }
            return;
        }
        int existing = surfacePixels[index];
        int written = enabledCaps.contains(GL_COLOR_LOGIC_OP)
                ? logicOp(color, existing)
                : enabledCaps.contains(GL_BLEND) ? blend(color, existing) : color;
        if (!colorMaskRed || !colorMaskGreen || !colorMaskBlue || !colorMaskAlpha) {
            written = (colorMaskAlpha ? written & 0xFF000000 : existing & 0xFF000000)
                    | (colorMaskRed ? written & 0x00FF0000 : existing & 0x00FF0000)
                    | (colorMaskGreen ? written & 0x0000FF00 : existing & 0x0000FF00)
                    | (colorMaskBlue ? written & 0x000000FF : existing & 0x000000FF);
        }
        surfacePixels[index] = written;
        if (stencilEnabled) {
            applyStencilOp(index, stencilDepthPassOp);
        }
        if (depthWriteEnabled || !enabledCaps.contains(GL_DEPTH_TEST)) {
            surfaceDepth[index] = depth;
        }
    }

    private boolean insideScissor(int x, int y) {
        if (scissorWidth <= 0 || scissorHeight <= 0) {
            return false;
        }
        int minX = scissorX;
        int maxX = scissorX + scissorWidth;
        int minY = surfaceHeight - (scissorY + scissorHeight);
        int maxY = surfaceHeight - scissorY;
        return x >= minX && x < maxX && y >= minY && y < maxY;
    }

    private void clearStencilBuffer() {
        int mask = stencilWriteMask & 0xFF;
        int value = clearStencilValue & mask;
        for (int i = 0; i < surfaceStencil.length; i++) {
            surfaceStencil[i] = (surfaceStencil[i] & ~mask) | value;
        }
    }

    private boolean passesStencilTest(int stencilValue) {
        int left = stencilRef & stencilValueMask;
        int right = stencilValue & stencilValueMask;
        return compareInt(stencilFunc, right, left);
    }

    private void applyStencilOp(int pixelIndex, int operation) {
        int current = surfaceStencil[pixelIndex] & 0xFF;
        int updated = switch (operation) {
            case GL_ZERO -> 0;
            case GL_REPLACE -> stencilRef & 0xFF;
            case GL_INCR -> Math.min(0xFF, current + 1);
            case GL_DECR -> Math.max(0, current - 1);
            case GL_INVERT -> (~current) & 0xFF;
            default -> current;
        };
        int mask = stencilWriteMask & 0xFF;
        surfaceStencil[pixelIndex] = (current & ~mask) | (updated & mask);
    }

    private int logicOp(int src, int dst) {
        return switch (logicOpMode) {
            case GL_CLEAR -> 0;
            case GL_SET -> 0xFFFFFFFF;
            case GL_COPY -> src;
            case GL_COPY_INVERTED -> ~src;
            case GL_NOOP -> dst;
            case GL_INVERT -> ~dst;
            case GL_AND -> src & dst;
            case GL_NAND -> ~(src & dst);
            case GL_OR -> src | dst;
            case GL_NOR -> ~(src | dst);
            case GL_XOR -> src ^ dst;
            case GL_EQUIV -> ~(src ^ dst);
            case GL_AND_REVERSE -> src & ~dst;
            case GL_AND_INVERTED -> ~src & dst;
            case GL_OR_REVERSE -> src | ~dst;
            case GL_OR_INVERTED -> ~src | dst;
            default -> src;
        };
    }

    private int blend(int src, int dst) {
        float srcA = ((src >>> 24) & 0xFF) / 255.0f;
        float dstA = ((dst >>> 24) & 0xFF) / 255.0f;
        int sr = (src >>> 16) & 0xFF;
        int sg = (src >>> 8) & 0xFF;
        int sb = src & 0xFF;
        int sa = (src >>> 24) & 0xFF;
        int dr = (dst >>> 16) & 0xFF;
        int dg = (dst >>> 8) & 0xFF;
        int db = dst & 0xFF;
        int da = (dst >>> 24) & 0xFF;
        float[] srcFactor = factor(blendSrcFactor, srcA, dstA, sr, sg, sb, dr, dg, db);
        float[] dstFactor = factor(blendDstFactor, srcA, dstA, sr, sg, sb, dr, dg, db);
        int r = clampByte(sr * srcFactor[0] + dr * dstFactor[0]);
        int g = clampByte(sg * srcFactor[1] + dg * dstFactor[1]);
        int b = clampByte(sb * srcFactor[2] + db * dstFactor[2]);
        int a = clampByte(sa * srcFactor[3] + da * dstFactor[3]);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private float[] factor(int factor, float srcAlpha, float dstAlpha, int sr, int sg, int sb, int dr, int dg, int db) {
        if (factor == GL_ZERO) {
            return new float[] {0.0f, 0.0f, 0.0f, 0.0f};
        }
        if (factor == GL_ONE) {
            return new float[] {1.0f, 1.0f, 1.0f, 1.0f};
        }
        if (factor == GL_SRC_COLOR) {
            return new float[] {sr / 255.0f, sg / 255.0f, sb / 255.0f, srcAlpha};
        }
        if (factor == GL_ONE_MINUS_SRC_COLOR) {
            return new float[] {1.0f - sr / 255.0f, 1.0f - sg / 255.0f, 1.0f - sb / 255.0f, 1.0f - srcAlpha};
        }
        if (factor == GL_DST_COLOR) {
            return new float[] {dr / 255.0f, dg / 255.0f, db / 255.0f, dstAlpha};
        }
        if (factor == GL_ONE_MINUS_DST_COLOR) {
            return new float[] {1.0f - dr / 255.0f, 1.0f - dg / 255.0f, 1.0f - db / 255.0f, 1.0f - dstAlpha};
        }
        if (factor == GL_SRC_ALPHA) {
            return new float[] {srcAlpha, srcAlpha, srcAlpha, srcAlpha};
        }
        if (factor == GL_ONE_MINUS_SRC_ALPHA) {
            float value = 1.0f - srcAlpha;
            return new float[] {value, value, value, value};
        }
        if (factor == GL_DST_ALPHA) {
            return new float[] {dstAlpha, dstAlpha, dstAlpha, dstAlpha};
        }
        if (factor == GL_ONE_MINUS_DST_ALPHA) {
            float value = 1.0f - dstAlpha;
            return new float[] {value, value, value, value};
        }
        if (factor == GL_SRC_ALPHA_SATURATE) {
            float value = Math.min(srcAlpha, 1.0f - dstAlpha);
            return new float[] {value, value, value, 1.0f};
        }
        return new float[] {1.0f, 1.0f, 1.0f, 1.0f};
    }

    private int[] decodeTexturePixels(Buffer buffer, int width, int height, int format, int type) {
        if (buffer instanceof ByteBuffer byteBuffer) {
            return decodeRgbaTexture(byteBuffer, width, height, format, type);
        }
        if (buffer instanceof ShortBuffer shortBuffer) {
            return decodeShortTexture(shortBuffer, width, height, format, type);
        }
        return new int[width * height];
    }

    private int[] decodeRgbaTexture(ByteBuffer buffer, int width, int height, int format, int type) {
        byte[] data = buffer.rawData();
        int[] pixels = new int[width * height];
        int offset = buffer.boundedOffset();
        if (type == GL_UNSIGNED_SHORT_5_6_5
                || type == GL_UNSIGNED_SHORT_4_4_4_4
                || type == GL_UNSIGNED_SHORT_5_5_5_1
                || type == GL_UNSIGNED_SHORT) {
            int rowStride = alignedRowStride(width * 2, unpackAlignment);
            if (buffer.boundedLength() < requiredByteLength(rowStride, width * 2, height)) {
                setError(GL_INVALID_VALUE);
                return pixels;
            }
            for (int row = 0; row < height; row++) {
                int rowOffset = offset + row * rowStride;
                int pixelOffset = row * width;
                for (int col = 0; col < width; col++) {
                    int value = readUnsignedShort(data, rowOffset + col * 2);
                    pixels[pixelOffset + col] = switch (type) {
                        case GL_UNSIGNED_SHORT_5_6_5 -> decodeRgb565(value);
                        case GL_UNSIGNED_SHORT_4_4_4_4 -> decodeRgba4444(value);
                        case GL_UNSIGNED_SHORT_5_5_5_1 -> decodeRgba5551(value);
                        case GL_UNSIGNED_SHORT -> (format == GL_RGB || format == GL_RGBA)
                                ? decodeRgb565(value)
                                : 0;
                        default -> 0;
                    };
                }
            }
            return pixels;
        }
        if (type != GL_UNSIGNED_BYTE) {
            return pixels;
        }
        if (format == GL_RGB) {
            int rowStride = alignedRowStride(width * 3, unpackAlignment);
            if (buffer.boundedLength() < requiredByteLength(rowStride, width * 3, height)) {
                setError(GL_INVALID_VALUE);
                return pixels;
            }
            for (int row = 0; row < height; row++) {
                int rowOffset = offset + row * rowStride;
                int pixelOffset = row * width;
                for (int col = 0; col < width; col++) {
                    int base = rowOffset + col * 3;
                    int r = data[base] & 0xFF;
                    int g = data[base + 1] & 0xFF;
                    int b = data[base + 2] & 0xFF;
                    pixels[pixelOffset + col] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }
            return pixels;
        }
        if (format == GL_ALPHA || format == GL_LUMINANCE) {
            int rowStride = alignedRowStride(width, unpackAlignment);
            if (buffer.boundedLength() < requiredByteLength(rowStride, width, height)) {
                setError(GL_INVALID_VALUE);
                return pixels;
            }
            for (int row = 0; row < height; row++) {
                int rowOffset = offset + row * rowStride;
                int pixelOffset = row * width;
                for (int col = 0; col < width; col++) {
                    int value = data[rowOffset + col] & 0xFF;
                    pixels[pixelOffset + col] = format == GL_ALPHA
                            ? (value << 24) | 0x00FFFFFF
                            : 0xFF000000 | (value << 16) | (value << 8) | value;
                }
            }
            return pixels;
        }
        if (format == GL_LUMINANCE_ALPHA) {
            int rowStride = alignedRowStride(width * 2, unpackAlignment);
            if (buffer.boundedLength() < requiredByteLength(rowStride, width * 2, height)) {
                setError(GL_INVALID_VALUE);
                return pixels;
            }
            for (int row = 0; row < height; row++) {
                int rowOffset = offset + row * rowStride;
                int pixelOffset = row * width;
                for (int col = 0; col < width; col++) {
                    int base = rowOffset + col * 2;
                    int lum = data[base] & 0xFF;
                    int a = data[base + 1] & 0xFF;
                    pixels[pixelOffset + col] = (a << 24) | (lum << 16) | (lum << 8) | lum;
                }
            }
            return pixels;
        }
        int rowStride = alignedRowStride(width * 4, unpackAlignment);
        if (buffer.boundedLength() < requiredByteLength(rowStride, width * 4, height)) {
            setError(GL_INVALID_VALUE);
            return pixels;
        }
        for (int row = 0; row < height; row++) {
            int rowOffset = offset + row * rowStride;
            int pixelOffset = row * width;
            for (int col = 0; col < width; col++) {
                int base = rowOffset + col * 4;
                int r = data[base] & 0xFF;
                int g = data[base + 1] & 0xFF;
                int b = data[base + 2] & 0xFF;
                int a = data[base + 3] & 0xFF;
                pixels[pixelOffset + col] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return pixels;
    }

    private int[] decodeShortTexture(ShortBuffer buffer, int width, int height, int format, int type) {
        short[] data = buffer.rawData();
        int offset = buffer.boundedOffset();
        int[] pixels = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            int value = data[offset + i] & 0xFFFF;
            pixels[i] = switch (type) {
                case GL_UNSIGNED_SHORT_5_6_5 -> decodeRgb565(value);
                case GL_UNSIGNED_SHORT_4_4_4_4 -> decodeRgba4444(value);
                case GL_UNSIGNED_SHORT_5_5_5_1 -> decodeRgba5551(value);
                case GL_UNSIGNED_SHORT -> (format == GL_RGB || format == GL_RGBA)
                        ? decodeRgb565(value)
                        : 0;
                default -> 0;
            };
        }
        return pixels;
    }

    private static int decodeRgb565(int value) {
        int r = expand5((value >>> 11) & 0x1F);
        int g = expand6((value >>> 5) & 0x3F);
        int b = expand5(value & 0x1F);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int decodeRgba4444(int value) {
        int r = expand4((value >>> 12) & 0x0F);
        int g = expand4((value >>> 8) & 0x0F);
        int b = expand4((value >>> 4) & 0x0F);
        int a = expand4(value & 0x0F);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int decodeRgba5551(int value) {
        int r = expand5((value >>> 11) & 0x1F);
        int g = expand5((value >>> 6) & 0x1F);
        int b = expand5((value >>> 1) & 0x1F);
        int a = (value & 0x1) != 0 ? 0xFF : 0x00;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int expand4(int value) {
        return (value << 4) | value;
    }

    private static int expand5(int value) {
        return (value << 3) | (value >>> 2);
    }

    private static int expand6(int value) {
        return (value << 2) | (value >>> 4);
    }

    private int[] decodePaletteTexture(ByteBuffer buffer, int width, int height, int internalFormat) {
        int[] pixels = new int[Math.max(0, width * height)];
        if (width <= 0 || height <= 0) {
            setError(GL_INVALID_VALUE);
            return pixels;
        }
        byte[] data = buffer.rawData();
        int offset = buffer.boundedOffset();
        int paletteEntries = paletteEntryCount(internalFormat);
        int paletteEntryBytes = paletteEntryBytes(internalFormat);
        int indexBits = paletteIndexBits(internalFormat);
        if (paletteEntries == 0 || paletteEntryBytes == 0 || indexBits == 0) {
            setError(GL_INVALID_ENUM);
            return pixels;
        }
        int paletteBytes = paletteEntries * paletteEntryBytes;
        int indexBytes = indexBits == 4 ? (pixels.length + 1) / 2 : pixels.length;
        if (buffer.boundedLength() < paletteBytes + indexBytes) {
            setError(GL_INVALID_VALUE);
            return pixels;
        }

        int[] palette = new int[paletteEntries];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = decodePaletteEntry(data, offset + i * paletteEntryBytes, internalFormat);
        }
        int indexOffset = offset + paletteBytes;
        for (int i = 0; i < pixels.length; i++) {
            int paletteIndex;
            if (indexBits == 4) {
                int packed = data[indexOffset + i / 2] & 0xFF;
                paletteIndex = (i & 1) == 0 ? (packed >>> 4) : (packed & 0x0F);
            } else {
                paletteIndex = data[indexOffset + i] & 0xFF;
            }
            pixels[i] = palette[paletteIndex];
        }
        return pixels;
    }

    private static int paletteEntryCount(int internalFormat) {
        return switch (internalFormat) {
            case GL_PALETTE4_RGB8_OES,
                    GL_PALETTE4_RGBA8_OES,
                    GL_PALETTE4_R5_G6_B5_OES,
                    GL_PALETTE4_RGBA4_OES,
                    GL_PALETTE4_RGB5_A1_OES -> 16;
            case GL_PALETTE8_RGB8_OES,
                    GL_PALETTE8_RGBA8_OES,
                    GL_PALETTE8_R5_G6_B5_OES,
                    GL_PALETTE8_RGBA4_OES,
                    GL_PALETTE8_RGB5_A1_OES -> 256;
            default -> 0;
        };
    }

    private static int paletteEntryBytes(int internalFormat) {
        return switch (internalFormat) {
            case GL_PALETTE4_RGB8_OES, GL_PALETTE8_RGB8_OES -> 3;
            case GL_PALETTE4_RGBA8_OES, GL_PALETTE8_RGBA8_OES -> 4;
            case GL_PALETTE4_R5_G6_B5_OES,
                    GL_PALETTE4_RGBA4_OES,
                    GL_PALETTE4_RGB5_A1_OES,
                    GL_PALETTE8_R5_G6_B5_OES,
                    GL_PALETTE8_RGBA4_OES,
                    GL_PALETTE8_RGB5_A1_OES -> 2;
            default -> 0;
        };
    }

    private static int paletteIndexBits(int internalFormat) {
        int entries = paletteEntryCount(internalFormat);
        return entries == 16 ? 4 : entries == 256 ? 8 : 0;
    }

    private static int decodePaletteEntry(byte[] data, int base, int internalFormat) {
        return switch (internalFormat) {
            case GL_PALETTE4_RGB8_OES, GL_PALETTE8_RGB8_OES -> 0xFF000000
                    | ((data[base] & 0xFF) << 16)
                    | ((data[base + 1] & 0xFF) << 8)
                    | (data[base + 2] & 0xFF);
            case GL_PALETTE4_RGBA8_OES, GL_PALETTE8_RGBA8_OES -> ((data[base + 3] & 0xFF) << 24)
                    | ((data[base] & 0xFF) << 16)
                    | ((data[base + 1] & 0xFF) << 8)
                    | (data[base + 2] & 0xFF);
            case GL_PALETTE4_R5_G6_B5_OES, GL_PALETTE8_R5_G6_B5_OES ->
                    decodeRgb565(readUnsignedShort(data, base));
            case GL_PALETTE4_RGBA4_OES, GL_PALETTE8_RGBA4_OES ->
                    decodeRgba4444(readUnsignedShort(data, base));
            case GL_PALETTE4_RGB5_A1_OES, GL_PALETTE8_RGB5_A1_OES ->
                    decodeRgba5551(readUnsignedShort(data, base));
            default -> 0;
        };
    }

    private static int readUnsignedShort(byte[] data, int offset) {
        if (offset < 0 || offset + 1 >= data.length) {
            return 0;
        }
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    private static byte readSignedByte(byte[] data, int offset) {
        return offset < 0 || offset >= data.length ? 0 : data[offset];
    }

    private static int readUnsignedByte(byte[] data, int offset) {
        return offset < 0 || offset >= data.length ? 0 : data[offset] & 0xFF;
    }

    private static int readInt(byte[] data, int offset) {
        if (offset < 0 || offset + 3 >= data.length) {
            return 0;
        }
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }

    private static int alignedRowStride(int rowBytes, int alignment) {
        int safeAlignment = alignment == 1 || alignment == 2 || alignment == 4 || alignment == 8 ? alignment : 4;
        int remainder = rowBytes % safeAlignment;
        return remainder == 0 ? rowBytes : rowBytes + safeAlignment - remainder;
    }

    private static int requiredByteLength(int rowStride, int finalRowBytes, int height) {
        return height <= 0 ? 0 : rowStride * (height - 1) + finalRowBytes;
    }

    private int sampleTexture(TextureState textureState, float u, float v) {
        if (textureState.width <= 0 || textureState.height <= 0 || textureState.pixels == null) {
            return currentColorArgb;
        }
        float wrappedU = wrapCoordinate(u, textureState.wrapS);
        float wrappedV = wrapCoordinate(v, textureState.wrapT);
        int x = Math.max(0, Math.min(textureState.width - 1, (int) (wrappedU * (textureState.width - 1))));
        int y = Math.max(0, Math.min(textureState.height - 1, (int) (wrappedV * (textureState.height - 1))));
        return textureState.pixels[y * textureState.width + x];
    }

    private int sampleTextureTexel(TextureState textureState, float texX, float texY) {
        if (textureState.magFilter == GL_LINEAR || textureState.minFilter == GL_LINEAR) {
            int x0 = texelCoordinate((int) Math.floor(texX), textureState.width, textureState.wrapS);
            int y0 = texelCoordinate((int) Math.floor(texY), textureState.height, textureState.wrapT);
            int x1 = texelCoordinate((int) Math.floor(texX) + 1, textureState.width, textureState.wrapS);
            int y1 = texelCoordinate((int) Math.floor(texY) + 1, textureState.height, textureState.wrapT);
            float fx = texX - (float) Math.floor(texX);
            float fy = texY - (float) Math.floor(texY);
            int c00 = textureState.pixels[y0 * textureState.width + x0];
            int c10 = textureState.pixels[y0 * textureState.width + x1];
            int c01 = textureState.pixels[y1 * textureState.width + x0];
            int c11 = textureState.pixels[y1 * textureState.width + x1];
            return lerpColor(lerpColor(c00, c10, fx), lerpColor(c01, c11, fx), fy);
        }
        int x = texelCoordinate((int) Math.floor(texX), textureState.width, textureState.wrapS);
        int y = texelCoordinate((int) Math.floor(texY), textureState.height, textureState.wrapT);
        return textureState.pixels[y * textureState.width + x];
    }

    private static int texelCoordinate(int coordinate, int extent, int wrapMode) {
        if (extent <= 1) {
            return 0;
        }
        if (wrapMode == GL_CLAMP_TO_EDGE) {
            return Math.max(0, Math.min(extent - 1, coordinate));
        }
        int wrapped = coordinate % extent;
        return wrapped < 0 ? wrapped + extent : wrapped;
    }

    private float wrapCoordinate(float value, int wrapMode) {
        if (wrapMode == GL_CLAMP_TO_EDGE) {
            return Math.max(0.0f, Math.min(1.0f, value));
        }
        return value - (float) Math.floor(value);
    }

    private int[] readIndices(Buffer buffer, int type, int byteOffset, int count) {
        int[] indices = new int[count];
        if (buffer instanceof ByteBuffer byteBuffer) {
            byte[] data = byteBuffer.rawData();
            if (type == GL_UNSIGNED_BYTE) {
                for (int i = 0; i < count; i++) {
                    indices[i] = readUnsignedByte(data, byteOffset + i);
                }
            } else {
                for (int i = 0; i < count; i++) {
                    indices[i] = readUnsignedShort(data, byteOffset + i * 2);
                }
            }
            return indices;
        }
        if (buffer instanceof ShortBuffer shortBuffer) {
            short[] data = shortBuffer.rawData();
            int start = byteOffset / 2;
            for (int i = 0; i < count; i++) {
                indices[i] = data[start + i] & 0xFFFF;
            }
        }
        return indices;
    }

    private boolean passesAlphaTest(int alpha) {
        int ref = clampColor(alphaRef);
        return compareInt(alphaFunc, alpha, ref);
    }

    private boolean passesDepthTest(float incomingDepth, float existingDepth) {
        return switch (depthFunc) {
            case GL_NEVER -> false;
            case GL_EQUAL -> Math.abs(incomingDepth - existingDepth) <= 0.000001f;
            case GL_LEQUAL -> incomingDepth <= existingDepth + 0.000001f;
            case GL_LESS -> incomingDepth < existingDepth - 0.000001f;
            case GL_GREATER -> incomingDepth > existingDepth + 0.000001f;
            case GL_GEQUAL -> incomingDepth + 0.000001f >= existingDepth;
            case GL_NOTEQUAL -> Math.abs(incomingDepth - existingDepth) > 0.000001f;
            case GL_ALWAYS -> true;
            default -> incomingDepth < existingDepth - 0.000001f;
        };
    }

    private static boolean compareInt(int func, int left, int right) {
        return switch (func) {
            case GL_NEVER -> false;
            case GL_EQUAL -> left == right;
            case GL_LEQUAL -> left <= right;
            case GL_LESS -> left < right;
            case GL_GREATER -> left > right;
            case GL_GEQUAL -> left >= right;
            case GL_NOTEQUAL -> left != right;
            case GL_ALWAYS -> true;
            default -> left != right;
        };
    }

    private boolean frontFacing(float signedArea) {
        return frontFaceMode == GL_CW ? signedArea < 0.0f : signedArea > 0.0f;
    }

    private static int interpolateColor(int c0, int c1, int c2, float w0, float w1, float w2) {
        float a = ((c0 >>> 24) & 0xFF) * w0 + ((c1 >>> 24) & 0xFF) * w1 + ((c2 >>> 24) & 0xFF) * w2;
        float r = ((c0 >>> 16) & 0xFF) * w0 + ((c1 >>> 16) & 0xFF) * w1 + ((c2 >>> 16) & 0xFF) * w2;
        float g = ((c0 >>> 8) & 0xFF) * w0 + ((c1 >>> 8) & 0xFF) * w1 + ((c2 >>> 8) & 0xFF) * w2;
        float b = (c0 & 0xFF) * w0 + (c1 & 0xFF) * w1 + (c2 & 0xFF) * w2;
        return (clampByte(a) << 24) | (clampByte(r) << 16) | (clampByte(g) << 8) | clampByte(b);
    }

    private static void normalize3(float[] vector) {
        float length = (float) Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);
        if (length <= 0.000001f) {
            vector[0] = 0.0f;
            vector[1] = 0.0f;
            vector[2] = 1.0f;
            return;
        }
        vector[0] /= length;
        vector[1] /= length;
        vector[2] /= length;
    }

    private static float dot3(float[] left, float[] right) {
        return left[0] * right[0] + left[1] * right[1] + left[2] * right[2];
    }

    private static float[] lightDirection(float[] position, float[] modelPosition) {
        float[] direction;
        if (position[3] == 0.0f) {
            direction = new float[] {position[0], position[1], position[2]};
        } else {
            direction = new float[] {
                    position[0] - modelPosition[0],
                    position[1] - modelPosition[1],
                    position[2] - modelPosition[2]
            };
        }
        normalize3(direction);
        return direction;
    }

    private static float lightAttenuation(LightState light, float[] modelPosition) {
        if (light.position[3] == 0.0f) {
            return 1.0f;
        }
        float dx = light.position[0] - modelPosition[0];
        float dy = light.position[1] - modelPosition[1];
        float dz = light.position[2] - modelPosition[2];
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float denominator = light.constantAttenuation
                + light.linearAttenuation * distance
                + light.quadraticAttenuation * distance * distance;
        return denominator <= 0.000001f ? 1.0f : 1.0f / denominator;
    }

    private static float spotEffect(LightState light, float[] directionToLight) {
        if (light.spotCutoff >= 180.0f) {
            return 1.0f;
        }
        float[] lightToVertex = new float[] {-directionToLight[0], -directionToLight[1], -directionToLight[2]};
        normalize3(lightToVertex);
        float cosine = dot3(lightToVertex, light.spotDirection);
        float cutoff = (float) Math.cos(Math.toRadians(light.spotCutoff));
        if (cosine < cutoff) {
            return 0.0f;
        }
        return light.spotExponent <= 0.0f ? 1.0f : (float) Math.pow(cosine, light.spotExponent);
    }

    private float[] readVertexComponents(ClientArrayBinding binding, int vertexIndex, int components, float defaultW) {
        float[] values = new float[] {0.0f, 0.0f, 0.0f, defaultW};
        if (binding == null) {
            return values;
        }
        int componentSize = sizeOfType(binding.type);
        int strideBytes = binding.stride == 0 ? binding.componentCount * componentSize : binding.stride;
        int byteOffset = binding.offset + vertexIndex * strideBytes;
        Buffer source = binding.usesVbo ? bufferData.get(binding.bufferId) : binding.pointer;
        if (source == null) {
            return values;
        }
        for (int component = 0; component < Math.min(binding.componentCount, components); component++) {
            int componentOffset = byteOffset + component * componentSize;
            values[component] = readComponent(source, binding.type, componentOffset);
        }
        return values;
    }

    private float readComponent(Buffer buffer, int type, int byteOffset) {
        if (buffer instanceof ByteBuffer byteBuffer) {
            byte[] data = byteBuffer.rawData();
            return switch (type) {
                case GL_BYTE -> readSignedByte(data, byteOffset);
                case GL_UNSIGNED_BYTE -> readUnsignedByte(data, byteOffset);
                case GL_SHORT -> (short) readUnsignedShort(data, byteOffset);
                case GL_UNSIGNED_SHORT -> readUnsignedShort(data, byteOffset);
                case GL_FLOAT -> Float.intBitsToFloat(readInt(data, byteOffset));
                default -> 0.0f;
            };
        }
        return switch (type) {
            case GL_BYTE -> ((ByteBuffer) buffer).rawData()[byteOffset];
            case GL_UNSIGNED_BYTE -> ((ByteBuffer) buffer).rawData()[byteOffset] & 0xFF;
            case GL_SHORT -> ((ShortBuffer) buffer).rawData()[byteOffset / 2];
            case GL_UNSIGNED_SHORT -> ((ShortBuffer) buffer).rawData()[byteOffset / 2] & 0xFFFF;
            case GL_FLOAT -> ((FloatBuffer) buffer).rawData()[byteOffset / 4];
            default -> 0.0f;
        };
    }

    private Buffer cloneBuffer(Buffer source) {
        if (source instanceof ByteBuffer byteBuffer) {
            return ByteBuffer.allocateDirect(byteBuffer);
        }
        if (source instanceof ShortBuffer shortBuffer) {
            return ShortBuffer.allocateDirect(shortBuffer);
        }
        if (source instanceof FloatBuffer floatBuffer) {
            return FloatBuffer.allocateDirect(floatBuffer);
        }
        if (source instanceof IntBuffer intBuffer) {
            return IntBuffer.allocateDirect(intBuffer);
        }
        throw new IllegalArgumentException("Unsupported buffer: " + source.getClass().getName());
    }

    private void writeBufferData(Buffer destination, int offsetBytes, Buffer source) {
        if (destination instanceof ByteBuffer dst && source instanceof ByteBuffer src) {
            System.arraycopy(src.rawData(), src.boundedOffset(), dst.rawData(), offsetBytes, src.boundedLength());
            return;
        }
        if (destination instanceof ShortBuffer dst && source instanceof ShortBuffer src) {
            System.arraycopy(src.rawData(), src.boundedOffset(), dst.rawData(), offsetBytes / 2, src.boundedLength());
            return;
        }
        if (destination instanceof FloatBuffer dst && source instanceof FloatBuffer src) {
            System.arraycopy(src.rawData(), src.boundedOffset(), dst.rawData(), offsetBytes / 4, src.boundedLength());
        }
    }

    private float[] currentMatrix() {
        return switch (currentMatrixMode) {
            case GL_PROJECTION -> projectionMatrix;
            case GL_TEXTURE -> textureMatrix;
            case GL_MATRIX_PALETTE_OES -> paletteMatrices[currentPaletteMatrixIndex];
            default -> modelViewMatrix;
        };
    }

    private ArrayDeque<float[]> currentStack() {
        return switch (currentMatrixMode) {
            case GL_PROJECTION -> projectionStack;
            case GL_TEXTURE -> textureStack;
            case GL_MATRIX_PALETTE_OES -> paletteStack;
            default -> modelViewStack;
        };
    }

    private void pushMatrix() {
        currentStack().push(Arrays.copyOf(currentMatrix(), 16));
    }

    private void popMatrix() {
        ArrayDeque<float[]> stack = currentStack();
        if (!stack.isEmpty()) {
            System.arraycopy(stack.pop(), 0, currentMatrix(), 0, 16);
        }
    }

    private void multiplyCurrentMatrix(float[] rhs) {
        float[] result = multiply(currentMatrix(), rhs);
        System.arraycopy(result, 0, currentMatrix(), 0, 16);
    }

    private static float[] identityMatrix() {
        float[] matrix = new float[16];
        loadIdentity(matrix);
        return matrix;
    }

    private static void loadIdentity(float[] matrix) {
        Arrays.fill(matrix, 0.0f);
        matrix[0] = 1.0f;
        matrix[5] = 1.0f;
        matrix[10] = 1.0f;
        matrix[15] = 1.0f;
    }

    private static float[] multiply(float[] lhs, float[] rhs) {
        float[] out = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                out[col * 4 + row] = lhs[row] * rhs[col * 4]
                        + lhs[4 + row] * rhs[col * 4 + 1]
                        + lhs[8 + row] * rhs[col * 4 + 2]
                        + lhs[12 + row] * rhs[col * 4 + 3];
            }
        }
        return out;
    }

    private static float[] multiplyVec4(float[] matrix, float x, float y, float z, float w) {
        return new float[] {
                matrix[0] * x + matrix[4] * y + matrix[8] * z + matrix[12] * w,
                matrix[1] * x + matrix[5] * y + matrix[9] * z + matrix[13] * w,
                matrix[2] * x + matrix[6] * y + matrix[10] * z + matrix[14] * w,
                matrix[3] * x + matrix[7] * y + matrix[11] * z + matrix[15] * w
        };
    }

    private static float[] translationMatrix(float x, float y, float z) {
        float[] matrix = identityMatrix();
        matrix[12] = x;
        matrix[13] = y;
        matrix[14] = z;
        return matrix;
    }

    private static float[] scaleMatrix(float x, float y, float z) {
        float[] matrix = identityMatrix();
        matrix[0] = x;
        matrix[5] = y;
        matrix[10] = z;
        return matrix;
    }

    private static float[] rotationMatrix(float angleDegrees, float x, float y, float z) {
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (length == 0.0f) {
            return identityMatrix();
        }
        x /= length;
        y /= length;
        z /= length;
        float radians = (float) Math.toRadians(angleDegrees);
        float c = (float) Math.cos(radians);
        float s = (float) Math.sin(radians);
        float oneMinusC = 1.0f - c;
        return new float[] {
                x * x * oneMinusC + c, y * x * oneMinusC + z * s, z * x * oneMinusC - y * s, 0.0f,
                x * y * oneMinusC - z * s, y * y * oneMinusC + c, z * y * oneMinusC + x * s, 0.0f,
                x * z * oneMinusC + y * s, y * z * oneMinusC - x * s, z * z * oneMinusC + c, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        };
    }

    private static float[] orthoMatrix(float left, float right, float bottom, float top, float zNear, float zFar) {
        float[] matrix = identityMatrix();
        matrix[0] = 2.0f / (right - left);
        matrix[5] = 2.0f / (top - bottom);
        matrix[10] = -2.0f / (zFar - zNear);
        matrix[12] = -(right + left) / (right - left);
        matrix[13] = -(top + bottom) / (top - bottom);
        matrix[14] = -(zFar + zNear) / (zFar - zNear);
        return matrix;
    }

    private static float[] frustumMatrix(float left, float right, float bottom, float top, float zNear, float zFar) {
        float[] matrix = new float[16];
        matrix[0] = (2.0f * zNear) / (right - left);
        matrix[5] = (2.0f * zNear) / (top - bottom);
        matrix[8] = (right + left) / (right - left);
        matrix[9] = (top + bottom) / (top - bottom);
        matrix[10] = -(zFar + zNear) / (zFar - zNear);
        matrix[11] = -1.0f;
        matrix[14] = -(2.0f * zFar * zNear) / (zFar - zNear);
        return matrix;
    }

    private static float edge(float ax, float ay, float bx, float by, float px, float py) {
        return (px - ax) * (by - ay) - (py - ay) * (bx - ax);
    }

    private static int modulateColor(int baseColor, int textureColor) {
        int a = (((baseColor >>> 24) & 0xFF) * ((textureColor >>> 24) & 0xFF)) / 255;
        int r = (((baseColor >>> 16) & 0xFF) * ((textureColor >>> 16) & 0xFF)) / 255;
        int g = (((baseColor >>> 8) & 0xFF) * ((textureColor >>> 8) & 0xFF)) / 255;
        int b = ((baseColor & 0xFF) * (textureColor & 0xFF)) / 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int applyTextureEnv(int baseColor, int textureColor) {
        return switch (textureEnvMode) {
            case GL_REPLACE -> textureColor;
            case GL_DECAL -> decalColor(baseColor, textureColor);
            case GL_ADD -> addColor(baseColor, textureColor);
            case GL_BLEND -> blendTextureEnvColor(baseColor, textureColor);
            case GL_COMBINE -> modulateColor(baseColor, textureColor);
            default -> modulateColor(baseColor, textureColor);
        };
    }

    private static int decalColor(int baseColor, int textureColor) {
        int texAlpha = (textureColor >>> 24) & 0xFF;
        int invAlpha = 255 - texAlpha;
        int a = (baseColor >>> 24) & 0xFF;
        int r = (((baseColor >>> 16) & 0xFF) * invAlpha + ((textureColor >>> 16) & 0xFF) * texAlpha) / 255;
        int g = (((baseColor >>> 8) & 0xFF) * invAlpha + ((textureColor >>> 8) & 0xFF) * texAlpha) / 255;
        int b = ((baseColor & 0xFF) * invAlpha + (textureColor & 0xFF) * texAlpha) / 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int addColor(int baseColor, int textureColor) {
        int a = Math.min(255, ((baseColor >>> 24) & 0xFF) + ((textureColor >>> 24) & 0xFF));
        int r = Math.min(255, ((baseColor >>> 16) & 0xFF) + ((textureColor >>> 16) & 0xFF));
        int g = Math.min(255, ((baseColor >>> 8) & 0xFF) + ((textureColor >>> 8) & 0xFF));
        int b = Math.min(255, (baseColor & 0xFF) + (textureColor & 0xFF));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int blendTextureEnvColor(int baseColor, int textureColor) {
        int env = (clampColor(textureEnvColor[3]) << 24)
                | (clampColor(textureEnvColor[0]) << 16)
                | (clampColor(textureEnvColor[1]) << 8)
                | clampColor(textureEnvColor[2]);
        int texAlpha = (textureColor >>> 24) & 0xFF;
        return decalColor(baseColor, (texAlpha << 24) | (env & 0x00FFFFFF));
    }

    private static int lerpColor(int fogColor, int color, float factor) {
        int fa = (fogColor >>> 24) & 0xFF;
        int fr = (fogColor >>> 16) & 0xFF;
        int fg = (fogColor >>> 8) & 0xFF;
        int fb = fogColor & 0xFF;
        int ca = (color >>> 24) & 0xFF;
        int cr = (color >>> 16) & 0xFF;
        int cg = (color >>> 8) & 0xFF;
        int cb = color & 0xFF;
        int a = clampByte(fa + (ca - fa) * factor);
        int r = clampByte(fr + (cr - fr) * factor);
        int g = clampByte(fg + (cg - fg) * factor);
        int b = clampByte(fb + (cb - fb) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float lerp(float start, float end, float factor) {
        return start + (end - start) * factor;
    }

    private static int clampByte(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
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
        validateDrawElementsIndices(maxIndex);
    }

    private void validateDrawElementsIndices(int[] indices) {
        int maxIndex = -1;
        for (int index : indices) {
            maxIndex = Math.max(maxIndex, index);
        }
        validateDrawElementsIndices(maxIndex);
    }

    private void validateDrawElementsIndices(int maxIndex) {
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

    private static float clampUnit(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static int clipPlaneIndex(int plane) {
        int index = plane - GL_CLIP_PLANE0;
        return index >= 0 && index < 6 ? index : -1;
    }
}
