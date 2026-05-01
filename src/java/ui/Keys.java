package ui;

/**
 * Key labels left -> right; each string length matches Config row point counts (10, 11, 11, 12).
 */
public final class Keys {
    public static final String ROW_1 = "CDEFGABCDE";   // 10
    public static final String ROW_2 = "CDEFGABCDEF";  // 11
    public static final String ROW_3 = "#DEFGABCDEb";  // 11 (modifier + naturals + flat modifier)
    public static final String ROW_4 = "CDEFGABCDEFG"; // 12

    private Keys() {}

    public static String row(int index) {
        return switch (index) {
            case 0 -> ROW_1;
            case 1 -> ROW_2;
            case 2 -> ROW_3;
            case 3 -> ROW_4;
            default -> throw new IndexOutOfBoundsException("Invalid row: " + index);
        };
    }

    public static int rowLength(int index) {
        return row(index).length();
    }

    public static boolean isModifier(String label) {
        // sharp/flat keys are modifiers only
        return "#".equals(label) || "b".equals(label);
    }

    public static int baseMidiFor(String label, int rowIndex, int colIndex) {
        if ("SP".equals(label)) {
            return 48;
        }
        if (isModifier(label)) {
            return -1;
        }

        return switch (rowIndex) {
            case 0 -> row0Midi(colIndex);
            case 1 -> row1Midi(colIndex);
            case 2 -> row2Midi(colIndex);
            case 3 -> row3Midi(colIndex);
            default -> 60;
        };
    }

    private static int row0Midi(int col) {
        int[] m = {72, 74, 76, 77, 79, 81, 83, 84, 86, 88};
        return m[Math.min(col, m.length - 1)];
    }

    private static int row1Midi(int col) {
        // middle row pitch map
        int[] m = {60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77};
        return m[Math.min(col, m.length - 1)];
    }

    /** Row 3: '#DEFGABCDEb' — col 0 and col 10 are modifiers. */
    private static int row2Midi(int col) {
        int[] m = {50, 52, 53, 55, 57, 59, 60, 62, 64};
        if (col >= 1 && col <= 9) {
            return m[col - 1];
        }
        return 60;
    }

    private static int row3Midi(int col) {
        int[] m = {48, 50, 52, 53, 55, 57, 59, 60, 62, 64, 65, 67};
        return m[Math.min(col, m.length - 1)];
    }

    public static final class PianoKey {
        public final int row;
        public final int col;
        public final String label;
        public boolean broken;
        public boolean blockedByCat;

        public PianoKey(int row, int col, String label) {
            this.row = row;
            this.col = col;
            this.label = label;
        }
    }
}
