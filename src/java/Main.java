
import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class CatPiano extends JFrame {

    private Synthesizer synth;
    private MidiChannel channel;
    private Map<Character, Integer> noteMap = new HashMap<>();

    public CatPiano() {
        super("Cat Piano");

        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            channel = synth.getChannels()[0];
           
            noteMap.put('A', 60); // C4
            noteMap.put('B', 62); // D4
            noteMap.put('C', 64); // E4
            noteMap.put('D', 65); // F4
            noteMap.put('E', 67); // G4
            noteMap.put('F', 69); // A4
            noteMap.put('G', 71); // B4
        } catch (Exception e) {
            e.printStackTrace();
        }

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        // --- Main panel ---
        JPanel main = new JPanel();
        main.setBackground(new Color(250, 240, 220));
        main.setLayout(new BorderLayout(10, 10));
        add(main);

        
        JPanel catsPanel = new JPanel();
        catsPanel.setOpaque(false);
        catsPanel.add(new JLabel("Kitten Keyboard")); 
        main.add(catsPanel, BorderLayout.NORTH);

        
        JPanel grid = new JPanel();
        grid.setOpaque(false);
        grid.setLayout(new GridLayout(4, 1, 5, 5));
        main.add(grid, BorderLayout.CENTER);

       
        String[] row1 = {"A","E","B","C","G","D","A","E","A"};
        String[] row2 = {"B","F","C","G","D","A","E","B","E"};
        String[] row3 = {"C","G","D","A","E","B","F","C"};
        String[] row4 = {"D","A","E","B","C","E","D","A","D","G","A"};

        addRow(grid, row1, new Color(255, 204, 204)); // soft pink
        addRow(grid, row2, new Color(204, 229, 255)); // soft blue
        addRow(grid, row3, new Color(204, 255, 204)); // soft green
        addRow(grid, row4, new Color(255, 229, 204)); // soft orange
    }

    private void addRow(JPanel parent, String[] letters, Color color) {
        JPanel rowPanel = new JPanel();
        rowPanel.setOpaque(false);
        rowPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 5));

        for (String s : letters) {
            JButton key = createKeyButton(s, color);
            rowPanel.add(key);
        }

        parent.add(rowPanel);
    }

    private JButton createKeyButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 18));
        btn.setForeground(Color.DARK_GRAY);
        btn.setBackground(color);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setPreferredSize(new Dimension(45, 45));

       
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                playNoteForChar(text.charAt(0));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                stopNoteForChar(text.charAt(0));
            }
        });

        return btn;
    }

    private void playNoteForChar(char c) {
        c = Character.toUpperCase(c);
        if (channel != null && noteMap.containsKey(c)) {
            int note = noteMap.get(c);
            channel.noteOn(note, 90);
        }
    }

    private void stopNoteForChar(char c) {
        c = Character.toUpperCase(c);
        if (channel != null && noteMap.containsKey(c)) {
            int note = noteMap.get(c);
            channel.noteOff(note);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CatPiano piano = new CatPiano();
            piano.setVisible(true);
        });
    }
}
