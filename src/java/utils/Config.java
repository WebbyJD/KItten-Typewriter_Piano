package utils;

import java.awt.Point;
import java.awt.Rectangle;

public final class Config {
    private Config() {}

    public static final int WINDOW_WIDTH = 1024;
    public static final int WINDOW_HEIGHT = 768;
    public static final int KEY_RADIUS = 30;

    // 8 keys for top row (CDEFGABC)
    public static final Point[] TOP_ROW = row(88, 294, 94, 8);

    // 7 keys for middle row (CDEFGAB)
    public static final Point[] MID_ROW = row(120, 418, 95, 7);

    // 8 keys for bottom row (#DEFGABb)
    public static final Point[] BOT_ROW = row(98, 497, 94, 8);

    // Space bar split into two logical click zones: low C and Bb
    public static final Rectangle LOW_C_HITBOX = new Rectangle(170, 617, 330, 42);
    public static final Rectangle B_FLAT_HITBOX = new Rectangle(505, 617, 330, 42);

    // Tail pedal hitboxes
    public static final Rectangle LEFT_PEDAL_HITBOX = new Rectangle(320, 645, 145, 95);
    public static final Rectangle MIDDLE_PEDAL_HITBOX = new Rectangle(470, 645, 130, 95);
    public static final Rectangle RIGHT_PEDAL_HITBOX = new Rectangle(560, 645, 155, 95);

    // Hearts in shooToClick overlay (tune these from debug clicks)
    public static final Rectangle SHOO_HEART_1 = new Rectangle(326, 571, 52, 52);
    public static final Rectangle SHOO_HEART_2 = new Rectangle(833, 118, 52, 52);
    public static final Rectangle SHOO_HEART_3 = new Rectangle(934, 466, 52, 52);

    public static final Rectangle SETTINGS_BUTTON_HITBOX = new Rectangle(18, 150, 84, 84);

    private static Point[] row(int startX, int y, int spacing, int count) {
        Point[] points = new Point[count];
        for (int i = 0; i < count; i++) {
            points[i] = new Point(startX + (i * spacing), y);
        }
        return points;
    }
}