package utils;

import java.awt.Point;
import java.awt.Rectangle;

public final class Config {
    private Config() {}

    public static final int WINDOW_WIDTH = 1024;
    public static final int WINDOW_HEIGHT = 768;
    // Negative value shifts the full UI upward to remove top empty gap.
    public static final int UI_OFFSET_Y = -95;
    public static final int KEY_RADIUS = 30;

    /*
     * Key centers from your latest click map, left -> right by row.
     * Counts required by UI art: 10, 11, 11, 12.
     * Two missing clicks (row1 col1 and row3 col3) were inferred from spacing to keep row counts correct.
     */
    public static final Point[] ROW1 = {
            new Point(126, 333), // inferred: missing first key in row 1
            new Point(214, 333), new Point(302, 333), new Point(383, 333), new Point(475, 333),
            new Point(562, 333), new Point(653, 333), new Point(726, 333), new Point(819, 333), new Point(904, 333),
    };
    public static final Point[] ROW2 = {
            new Point(90, 423), new Point(173, 423), new Point(263, 423), new Point(346, 423), new Point(434, 423),
            new Point(513, 423), new Point(597, 423), new Point(681, 423), new Point(763, 423), new Point(841, 423),
            new Point(918, 423),
    };
    /** Row 3 has 11 keys (one inferred center added so counts match). */
    public static final Point[] ROW3 = {
            new Point(138, 497), new Point(217, 497), new Point(296, 497), // inferred: missing key between x=217 and x=375
            new Point(375, 497), new Point(454, 497), new Point(537, 497), new Point(607, 497),
            new Point(687, 497), new Point(767, 497), new Point(846, 497), new Point(918, 497),
    };
    public static final Point[] ROW4 = {
            new Point(78, 562), new Point(182, 562), new Point(257, 562), new Point(345, 562),
            new Point(417, 562), new Point(494, 562), new Point(570, 562), new Point(646, 562),
            new Point(713, 562), new Point(789, 562), new Point(870, 562), new Point(941, 562),
    };

    /** Single space bar zone from click (502,628). */
    public static final Rectangle SPACEBAR_HITBOX = new Rectangle(360, 604, 300, 52);

    // Pedals from clicks: (415,696), (507,698), (595,711)
    public static final Rectangle LEFT_PEDAL_HITBOX = new Rectangle(365, 650, 100, 92);
    public static final Rectangle MIDDLE_PEDAL_HITBOX = new Rectangle(457, 650, 100, 92);
    public static final Rectangle RIGHT_PEDAL_HITBOX = new Rectangle(545, 663, 100, 92);

    public static final Rectangle SHOO_HEART_1 = new Rectangle(326, 571, 52, 52);
    public static final Rectangle SHOO_HEART_2 = new Rectangle(833, 118, 52, 52);
    public static final Rectangle SHOO_HEART_3 = new Rectangle(934, 466, 52, 52);

    public static final Rectangle SETTINGS_BUTTON_HITBOX = new Rectangle(18, 150, 84, 84);
}
