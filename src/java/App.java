import javax.swing.SwingUtilities;
import ui.TypeWriterFrame;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TypeWriterFrame().setVisible(true));
    }
}
