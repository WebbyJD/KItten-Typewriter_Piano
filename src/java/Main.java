import java.awt.*;
import javax.swing.*;

public class Main extends JFrame {

    public Main() {
        setTitle("Java Piano");
        setSize(700, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 7));
        add(panel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main piano = new Main();
            piano.setVisible(true);
        });
    }
}
