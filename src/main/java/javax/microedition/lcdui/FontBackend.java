package javax.microedition.lcdui;

import java.awt.Graphics2D;

interface FontBackend {
    int getHeight();

    int stringWidth(String value);

    int getAscent();

    int getDescent();

    java.awt.Font awtFont();

    void drawString(Graphics2D graphics, String text, int x, int baselineY, int argbColor);
}
