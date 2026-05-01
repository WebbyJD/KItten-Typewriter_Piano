import javax.swing.SwingUtilities;
import ui.TypeWriterFrame;

public class App {
    public static void main(String[] args) {
        // start the kitten typewriter window
        SwingUtilities.invokeLater(() -> new TypeWriterFrame().setVisible(true));
    }
}
