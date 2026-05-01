package javax.microedition.lcdui;

import java.util.ArrayList;
import remexa.host.runtime.MidletRuntime;

public class Screen extends Displayable {
    private static final int BACKGROUND_COLOR = 0xF7F7F1;
    private static final int TITLE_BACKGROUND_COLOR = 0x273143;
    private static final int TITLE_TEXT_COLOR = 0xFFFFFF;
    private static final int TEXT_COLOR = 0x202020;
    private static final int SCROLL_TRACK_COLOR = 0xD6D2C8;
    private static final int SCROLL_THUMB_COLOR = 0x686052;
    private static final int MARGIN = 4;

    private int scrollY;
    private int maxScrollY;

    void repaintHost() {
        MidletRuntime.renderScreen(this, graphics -> {
            try {
                paintScreen(graphics);
            } finally {
                graphics.dispose();
            }
        });
    }

    void fireScreenKeyPressed(int keyCode) {
        var lineHeight = Font.getDefaultFont().getHeight() + 1;
        var pageHeight = Math.max(lineHeight, getHeight() - titleHeight() - MARGIN * 2);
        switch (keyCode) {
            case Canvas.KEYCODE_UP, Canvas.UP, '2' -> scrollBy(-lineHeight);
            case Canvas.KEYCODE_DOWN, Canvas.DOWN, '8' -> scrollBy(lineHeight);
            case Canvas.KEYCODE_LEFT, Canvas.LEFT, '4' -> scrollBy(-pageHeight);
            case Canvas.KEYCODE_RIGHT, Canvas.RIGHT, '6' -> scrollBy(pageHeight);
            default -> {
            }
        }
    }

    void fireScreenKeyRepeated(int keyCode) {
        fireScreenKeyPressed(keyCode);
    }

    protected void paintScreen(Graphics graphics) {
        var bodyTop = paintChrome(graphics);
        var font = Font.getDefaultFont();
        var lines = new ArrayList<String>();
        appendWrappedLines(getTitle(), font, Math.max(1, getWidth() - MARGIN * 2), lines);
        paintLines(graphics, lines, bodyTop);
    }

    protected int paintChrome(Graphics graphics) {
        var width = getWidth();
        var height = getHeight();
        graphics.setColor(BACKGROUND_COLOR);
        graphics.fillRect(0, 0, width, height);

        var title = getTitle();
        if (title == null || title.isEmpty()) {
            return 0;
        }

        var titleHeight = titleHeight();
        graphics.setColor(TITLE_BACKGROUND_COLOR);
        graphics.fillRect(0, 0, width, titleHeight);
        graphics.setColor(TITLE_TEXT_COLOR);
        graphics.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        graphics.drawString(title, width / 2, (titleHeight - graphics.getFont().getHeight()) / 2, Graphics.HCENTER | Graphics.TOP);
        graphics.setFont(Font.getDefaultFont());
        return titleHeight;
    }

    protected void paintLines(Graphics graphics, java.util.List<String> lines, int bodyTop) {
        var font = Font.getDefaultFont();
        graphics.setFont(font);
        var lineHeight = font.getHeight() + 1;
        var bodyHeight = Math.max(0, getHeight() - bodyTop);
        var contentHeight = MARGIN * 2 + Math.max(0, lines.size()) * lineHeight;
        setScrollBounds(contentHeight, bodyHeight);

        graphics.setClip(0, bodyTop, getWidth(), bodyHeight);
        graphics.setColor(TEXT_COLOR);
        var y = bodyTop + MARGIN - scrollY;
        for (var line : lines) {
            if (y + lineHeight >= bodyTop && y <= bodyTop + bodyHeight) {
                graphics.drawString(line, MARGIN, y, Graphics.LEFT | Graphics.TOP);
            }
            y += lineHeight;
        }
        graphics.setClip(0, 0, getWidth(), getHeight());
        paintScrollBar(graphics, bodyTop, bodyHeight, contentHeight);
    }

    protected void appendWrappedLines(String value, Font font, int maxWidth, java.util.List<String> lines) {
        if (value == null || value.isEmpty()) {
            return;
        }
        var normalized = Font.normalizeText(value);
        var line = new StringBuilder();
        for (var index = 0; index < normalized.length(); index++) {
            var ch = normalized.charAt(index);
            if (ch == '\n') {
                lines.add(line.toString());
                line.setLength(0);
                continue;
            }
            line.append(ch);
            if (font.stringWidth(line.toString()) > maxWidth && line.length() > 1) {
                var overflow = line.charAt(line.length() - 1);
                line.setLength(line.length() - 1);
                lines.add(line.toString());
                line.setLength(0);
                line.append(overflow);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
    }

    protected int bodyTextWidth() {
        return Math.max(1, getWidth() - MARGIN * 2 - 4);
    }

    protected int titleHeight() {
        var title = getTitle();
        if (title == null || title.isEmpty()) {
            return 0;
        }
        return Math.max(18, Font.getDefaultFont().getHeight() + 6);
    }

    private void scrollBy(int amount) {
        if (amount == 0 || maxScrollY <= 0) {
            return;
        }
        var next = Math.max(0, Math.min(maxScrollY, scrollY + amount));
        if (next == scrollY) {
            return;
        }
        scrollY = next;
        repaintHost();
    }

    private void setScrollBounds(int contentHeight, int viewportHeight) {
        maxScrollY = Math.max(0, contentHeight - Math.max(0, viewportHeight));
        if (scrollY > maxScrollY) {
            scrollY = maxScrollY;
        }
    }

    private void paintScrollBar(Graphics graphics, int bodyTop, int bodyHeight, int contentHeight) {
        if (maxScrollY <= 0 || bodyHeight <= 0 || contentHeight <= 0) {
            return;
        }
        var x = getWidth() - 3;
        graphics.setColor(SCROLL_TRACK_COLOR);
        graphics.fillRect(x, bodyTop, 2, bodyHeight);
        var thumbHeight = Math.max(6, bodyHeight * bodyHeight / contentHeight);
        var thumbTravel = Math.max(1, bodyHeight - thumbHeight);
        var thumbY = bodyTop + scrollY * thumbTravel / maxScrollY;
        graphics.setColor(SCROLL_THUMB_COLOR);
        graphics.fillRect(x, thumbY, 2, thumbHeight);
    }
}
