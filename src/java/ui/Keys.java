package java.ui;

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
                default -> throw new IndexOutOfBoundsException();
            };
        }
    
    public static int rowLength(int index){
        return row(index).length();
    }
}
    

