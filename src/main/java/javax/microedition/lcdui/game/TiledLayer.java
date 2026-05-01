package javax.microedition.lcdui.game;

import java.util.ArrayList;
import java.util.List;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class TiledLayer extends Layer {
    private final int columns;
    private final int rows;
    private final int[][] cells;
    private final List<Integer> animatedTiles = new ArrayList<>();
    private Image image;
    private int tileWidth;
    private int tileHeight;
    private int imageColumns;
    private int staticTileCount;

    public TiledLayer(int columns, int rows, Image image, int tileWidth, int tileHeight) {
        super(checkedLayerSize(columns, tileWidth), checkedLayerSize(rows, tileHeight));
        if (columns < 1 || rows < 1) {
            throw new IllegalArgumentException("TiledLayer dimensions must be positive");
        }
        this.columns = columns;
        this.rows = rows;
        this.cells = new int[rows][columns];
        setStaticTileSet(image, tileWidth, tileHeight);
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public int getCellWidth() {
        return tileWidth;
    }

    public int getCellHeight() {
        return tileHeight;
    }

    public int getCell(int col, int row) {
        checkCell(col, row);
        return cells[row][col];
    }

    public void setCell(int col, int row, int tileIndex) {
        checkCell(col, row);
        checkTileIndex(tileIndex);
        cells[row][col] = tileIndex;
    }

    public void fillCells(int col, int row, int numCols, int numRows, int tileIndex) {
        checkRegion(col, row, numCols, numRows);
        checkTileIndex(tileIndex);
        for (int y = row; y < row + numRows; y++) {
            for (int x = col; x < col + numCols; x++) {
                cells[y][x] = tileIndex;
            }
        }
    }

    public int createAnimatedTile(int staticTileIndex) {
        checkStaticTileIndex(staticTileIndex);
        animatedTiles.add(staticTileIndex);
        return -animatedTiles.size();
    }

    public void setAnimatedTile(int animatedTileIndex, int staticTileIndex) {
        int offset = animatedOffset(animatedTileIndex);
        checkStaticTileIndex(staticTileIndex);
        animatedTiles.set(offset, staticTileIndex);
    }

    public int getAnimatedTile(int animatedTileIndex) {
        return animatedTiles.get(animatedOffset(animatedTileIndex));
    }

    public void setStaticTileSet(Image image, int tileWidth, int tileHeight) {
        requireImage(image);
        validateTileSize(image, tileWidth, tileHeight);
        this.image = image;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.imageColumns = image.getWidth() / tileWidth;
        this.staticTileCount = imageColumns * (image.getHeight() / tileHeight);
        clearInvalidCells();
        clampInvalidAnimatedTiles();
    }

    @Override
    public final void paint(Graphics graphics) {
        if (graphics == null) {
            throw new NullPointerException("graphics");
        }
        if (!isVisible()) {
            return;
        }
        int baseX = getX();
        int baseY = getY();
        for (int row = 0; row < rows; row++) {
            int destY = baseY + row * tileHeight;
            for (int col = 0; col < columns; col++) {
                int staticTile = resolveTile(cells[row][col]);
                if (staticTile == 0) {
                    continue;
                }
                int tile = staticTile - 1;
                int srcX = tile % imageColumns * tileWidth;
                int srcY = tile / imageColumns * tileHeight;
                graphics.drawRegion(
                        image,
                        srcX,
                        srcY,
                        tileWidth,
                        tileHeight,
                        Sprite.TRANS_NONE,
                        baseX + col * tileWidth,
                        destY,
                        Graphics.LEFT | Graphics.TOP
                );
            }
        }
    }

    private int resolveTile(int tileIndex) {
        if (tileIndex >= 0) {
            return tileIndex;
        }
        return animatedTiles.get(animatedOffset(tileIndex));
    }

    private void clearInvalidCells() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int tile = cells[row][col];
                if (tile > staticTileCount || tile < -animatedTiles.size()) {
                    cells[row][col] = 0;
                }
            }
        }
    }

    private void clampInvalidAnimatedTiles() {
        for (int i = 0; i < animatedTiles.size(); i++) {
            int staticTile = animatedTiles.get(i);
            if (staticTile > staticTileCount) {
                animatedTiles.set(i, 0);
            }
        }
    }

    private void checkTileIndex(int tileIndex) {
        if (tileIndex >= 0) {
            checkStaticTileIndex(tileIndex);
            return;
        }
        animatedOffset(tileIndex);
    }

    private void checkStaticTileIndex(int tileIndex) {
        if (tileIndex < 0 || tileIndex > staticTileCount) {
            throw new IndexOutOfBoundsException("Tile index out of range: " + tileIndex);
        }
    }

    private int animatedOffset(int animatedTileIndex) {
        int offset = -animatedTileIndex - 1;
        if (animatedTileIndex >= 0 || offset < 0 || offset >= animatedTiles.size()) {
            throw new IndexOutOfBoundsException("Animated tile index out of range: " + animatedTileIndex);
        }
        return offset;
    }

    private void checkCell(int col, int row) {
        if (col < 0 || col >= columns || row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException("Cell out of range: " + col + "," + row);
        }
    }

    private void checkRegion(int col, int row, int numCols, int numRows) {
        if (numCols < 0 || numRows < 0 || col < 0 || row < 0 || col + numCols > columns || row + numRows > rows) {
            throw new IndexOutOfBoundsException("Cell region out of range");
        }
    }

    private static int checkedLayerSize(int cells, int cellSize) {
        if (cells < 1 || cellSize < 1) {
            return 0;
        }
        long size = (long) cells * cellSize;
        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("TiledLayer size is too large");
        }
        return (int) size;
    }

    private static void validateTileSize(Image image, int tileWidth, int tileHeight) {
        if (tileWidth <= 0 || tileHeight <= 0) {
            throw new IllegalArgumentException("Tile size must be positive");
        }
        if (image.getWidth() < tileWidth || image.getHeight() < tileHeight) {
            throw new IllegalArgumentException("Tile size exceeds image size");
        }
        if (image.getWidth() % tileWidth != 0 || image.getHeight() % tileHeight != 0) {
            throw new IllegalArgumentException("Image dimensions must be divisible by tile size");
        }
    }

    private static void requireImage(Image image) {
        if (image == null) {
            throw new NullPointerException("image");
        }
    }
}
