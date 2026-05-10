package com.mascotcapsule.micro3d.v3;

import javax.microedition.lcdui.Graphics;

public class Graphics3D {
    public static final int COMMAND_LIST_VERSION_1_0 = com.jblend.graphics.j3d.Graphics3D.COMMAND_LIST_VERSION_1_0;
    public static final int COMMAND_END = com.jblend.graphics.j3d.Graphics3D.COMMAND_END;
    public static final int COMMAND_NOP = com.jblend.graphics.j3d.Graphics3D.COMMAND_NOP;
    public static final int COMMAND_FLUSH = com.jblend.graphics.j3d.Graphics3D.COMMAND_FLUSH;
    public static final int COMMAND_ATTRIBUTE = com.jblend.graphics.j3d.Graphics3D.COMMAND_ATTRIBUTE;
    public static final int COMMAND_CLIP = com.jblend.graphics.j3d.Graphics3D.COMMAND_CLIP;
    public static final int COMMAND_CENTER = com.jblend.graphics.j3d.Graphics3D.COMMAND_CENTER;
    public static final int COMMAND_TEXTURE_INDEX = com.jblend.graphics.j3d.Graphics3D.COMMAND_TEXTURE_INDEX;
    public static final int COMMAND_AFFINE_INDEX = com.jblend.graphics.j3d.Graphics3D.COMMAND_AFFINE_INDEX;
    public static final int COMMAND_PARALLEL_SCALE = com.jblend.graphics.j3d.Graphics3D.COMMAND_PARALLEL_SCALE;
    public static final int COMMAND_PARALLEL_SIZE = com.jblend.graphics.j3d.Graphics3D.COMMAND_PARALLEL_SIZE;
    public static final int COMMAND_PERSPECTIVE_FOV = com.jblend.graphics.j3d.Graphics3D.COMMAND_PERSPECTIVE_FOV;
    public static final int COMMAND_PERSPECTIVE_WH = com.jblend.graphics.j3d.Graphics3D.COMMAND_PERSPECTIVE_WH;
    public static final int COMMAND_AMBIENT_LIGHT = com.jblend.graphics.j3d.Graphics3D.COMMAND_AMBIENT_LIGHT;
    public static final int COMMAND_DIRECTION_LIGHT = com.jblend.graphics.j3d.Graphics3D.COMMAND_DIRECTION_LIGHT;
    public static final int COMMAND_THRESHOLD = com.jblend.graphics.j3d.Graphics3D.COMMAND_THRESHOLD;
    public static final int PRIMITIVE_POINTS = com.jblend.graphics.j3d.Graphics3D.PRIMITIVE_POINTS;
    public static final int PRIMITIVE_LINES = com.jblend.graphics.j3d.Graphics3D.PRIMITIVE_LINES;
    public static final int PRIMITIVE_TRIANGLES = com.jblend.graphics.j3d.Graphics3D.PRIMITIVE_TRIANGLES;
    public static final int PRIMITIVE_QUADS = com.jblend.graphics.j3d.Graphics3D.PRIMITIVE_QUADS;
    public static final int PRIMITIVE_POINT_SPRITES = com.jblend.graphics.j3d.Graphics3D.PRIMITIVE_POINT_SPRITES;
    public static final int PRIMITVE_POINTS = PRIMITIVE_POINTS;
    public static final int PRIMITVE_LINES = PRIMITIVE_LINES;
    public static final int PRIMITVE_TRIANGLES = PRIMITIVE_TRIANGLES;
    public static final int PRIMITVE_QUADS = PRIMITIVE_QUADS;
    public static final int PRIMITVE_POINT_SPRITES = PRIMITIVE_POINT_SPRITES;
    public static final int POINT_SPRITE_LOCAL_SIZE = com.jblend.graphics.j3d.Graphics3D.POINT_SPRITE_LOCAL_SIZE;
    public static final int POINT_SPRITE_PIXEL_SIZE = com.jblend.graphics.j3d.Graphics3D.POINT_SPRITE_PIXEL_SIZE;
    public static final int POINT_SPRITE_PERSPECTIVE = com.jblend.graphics.j3d.Graphics3D.POINT_SPRITE_PERSPECTIVE;
    public static final int POINT_SPRITE_NO_PERS = com.jblend.graphics.j3d.Graphics3D.POINT_SPRITE_NO_PERS;
    public static final int ENV_ATTR_LIGHTING = com.jblend.graphics.j3d.Graphics3D.ENV_ATTR_LIGHTING;
    public static final int ENV_ATTR_SPHERE_MAP = com.jblend.graphics.j3d.Graphics3D.ENV_ATTR_SPHERE_MAP;
    public static final int ENV_ATTR_TOON_SHADING = com.jblend.graphics.j3d.Graphics3D.ENV_ATTR_TOON_SHADING;
    public static final int ENV_ATTR_SEMI_TRANSPARENT = com.jblend.graphics.j3d.Graphics3D.ENV_ATTR_SEMI_TRANSPARENT;
    public static final int PATTR_LIGHTING = com.jblend.graphics.j3d.Graphics3D.PATTR_LIGHTING;
    public static final int PATTR_SPHERE_MAP = com.jblend.graphics.j3d.Graphics3D.PATTR_SPHERE_MAP;
    public static final int PATTR_COLORKEY = com.jblend.graphics.j3d.Graphics3D.PATTR_COLORKEY;
    public static final int PATTR_BLEND_NORMAL = com.jblend.graphics.j3d.Graphics3D.PATTR_BLEND_NORMAL;
    public static final int PATTR_BLEND_HALF = com.jblend.graphics.j3d.Graphics3D.PATTR_BLEND_HALF;
    public static final int PATTR_BLEND_ADD = com.jblend.graphics.j3d.Graphics3D.PATTR_BLEND_ADD;
    public static final int PATTR_BLEND_SUB = com.jblend.graphics.j3d.Graphics3D.PATTR_BLEND_SUB;
    public static final int PDATA_NORMAL_NONE = com.jblend.graphics.j3d.Graphics3D.PDATA_NORMAL_NONE;
    public static final int PDATA_NORMAL_PER_FACE = com.jblend.graphics.j3d.Graphics3D.PDATA_NORMAL_PER_FACE;
    public static final int PDATA_NORMAL_PER_VERTEX = com.jblend.graphics.j3d.Graphics3D.PDATA_NORMAL_PER_VERTEX;
    public static final int PDATA_COLOR_NONE = com.jblend.graphics.j3d.Graphics3D.PDATA_COLOR_NONE;
    public static final int PDATA_COLOR_PER_COMMAND = com.jblend.graphics.j3d.Graphics3D.PDATA_COLOR_PER_COMMAND;
    public static final int PDATA_COLOR_PER_FACE = com.jblend.graphics.j3d.Graphics3D.PDATA_COLOR_PER_FACE;
    public static final int PDATA_TEXURE_COORD_NONE = com.jblend.graphics.j3d.Graphics3D.PDATA_TEXURE_COORD_NONE;
    public static final int PDATA_POINT_SPRITE_PARAMS_PER_CMD = com.jblend.graphics.j3d.Graphics3D.PDATA_POINT_SPRITE_PARAMS_PER_CMD;
    public static final int PDATA_POINT_SPRITE_PARAMS_PER_FACE = com.jblend.graphics.j3d.Graphics3D.PDATA_POINT_SPRITE_PARAMS_PER_FACE;
    public static final int PDATA_POINT_SPRITE_PARAMS_PER_VERTEX = com.jblend.graphics.j3d.Graphics3D.PDATA_POINT_SPRITE_PARAMS_PER_VERTEX;
    public static final int PDATA_TEXURE_COORD = com.jblend.graphics.j3d.Graphics3D.PDATA_TEXURE_COORD;

    private Graphics boundGraphics;
    private com.jblend.graphics.j3d.Graphics3D delegate;

    public Graphics3D() {
    }

    public synchronized void bind(Graphics graphics) {
        bind(graphics, true);
    }

    public synchronized void bind(Graphics graphics, boolean doClip) {
        if (graphics == null) {
            throw new NullPointerException("Argument 'Graphics' is NULL");
        }
        if (delegate != null) {
            throw new IllegalStateException("Target already bound");
        }
        if (!(graphics instanceof com.jblend.graphics.j3d.Graphics3D graphics3D)) {
            throw new IllegalArgumentException("Graphics target does not support MascotCapsule rendering");
        }
        boundGraphics = graphics;
        delegate = graphics3D;
    }

    public synchronized void release(Graphics graphics) {
        if (graphics == null) {
            throw new NullPointerException("Argument 'Graphics' is NULL");
        }
        if (graphics == boundGraphics && delegate != null) {
            delegate.flush();
        }
        boundGraphics = null;
        delegate = null;
    }

    public void dispose() {
        boundGraphics = null;
        delegate = null;
    }

    public void drawCommandList(Texture[] textures, int x, int y, FigureLayout layout, Effect3D effect, int[] commandList) {
        target().drawCommandList(textures, x, y, layout, effect, commandList);
    }

    public void drawCommandList(Texture texture, int x, int y, FigureLayout layout, Effect3D effect, int[] commandList) {
        target().drawCommandList(texture, x, y, layout, effect, commandList);
    }

    public void drawFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect) {
        target().drawFigure(figure, x, y, layout, effect);
    }

    public void renderFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect) {
        target().renderFigure(figure, x, y, layout, effect);
    }

    public void renderPrimitives(
            Texture texture,
            int x,
            int y,
            FigureLayout layout,
            Effect3D effect,
            int command,
            int numPrimitives,
            int[] vertexCoords,
            int[] normals,
            int[] textureCoords,
            int[] colors
    ) {
        target().renderPrimitives(texture, x, y, layout, effect, command,
                numPrimitives, vertexCoords, normals, textureCoords, colors);
    }

    public void flush() {
        target().flush();
    }

    private com.jblend.graphics.j3d.Graphics3D target() {
        if (delegate == null) {
            throw new IllegalStateException("No target is bound");
        }
        return delegate;
    }
}
