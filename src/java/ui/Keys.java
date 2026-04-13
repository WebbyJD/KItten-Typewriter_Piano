package ui;

public final class Keys {
    public static final String ROW_TOP = "CDEFGABC";
    public static final String ROW_MID = "CDEFGAB";
    public static final String ROW_BOT = "#DEFGABb";

    private Keys() {}

    public static String row(int index) {
        return switch (index) {
            case 0 -> ROW_TOP;
            case 1 -> ROW_MID;
            case 2 -> ROW_BOT;
            default -> throw new IndexOutOfBoundsException("Invalid row: " + index);
        };
    }

    public static int rowLength(int index) {
        return row(index).length();
    }

    public static boolean isModifier(String label) {
        return "#".equals(label) || "b".equals(label);
    }

    public static int baseMidiFor(String label, int rowIndex, int colIndex) {
        if ("LC".equals(label)) return 48; // C3
        if ("Bb".equals(label)) return 58; // Bb3
        // Modifiers do not produce a sound by themselves.
        if (isModifier(label)) return -1;

        // Starting "C" reference note for each visual row.
        int cBase = switch (rowIndex) {
            case 0 -> 72; // C5 row
            case 1 -> 60; // C4 row
            case 2 -> 50; // D3-ish row
            default -> 60;
        };

        int semitone = switch (label) {
            case "C" -> 0;
            case "D" -> 2;
            case "E" -> 4;
            case "F" -> 5;
            case "G" -> 7;
            case "A" -> 9;
            case "B" -> 11;
            default -> 0;
        };

        int note = cBase + semitone;

        // Top row ends with another C, so push just that last key up an octave.
        if (rowIndex == 0 && "C".equals(label) && colIndex == ROW_TOP.length() - 1) {
            note += 12;
        }

        return note;
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
    

