package ui;

import utils.Config;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TypeWriterFrame extends JFrame {
    private final CatAssets assets = new CatAssets();
    private final Random random = new Random();

    private Synthesizer synth;
    private MidiChannel channel;

    private final List<KeyHotspot> keyHotspots = new ArrayList<>();
    // Tracks the exact MIDI note started by each hotspot so key release turns off the same note.
    private final Map<KeyHotspot, Integer> activeNotes = new HashMap<>();

    // Each entry matches one heart in Config.SHOO_HEART_1..3.
    private final boolean[] shooHeartsClicked = new boolean[3];

    private boolean shiftHeld = false;
    private int accidentalOffset = 0;
    private int pedalOffset = 0;
    private boolean catsBlocking = false;

    private final KeyboardPanel keyboardPanel = new KeyboardPanel();
    private Timer catTimer;

    public TypeWriterFrame() {
        super("Kitten Typewriter Piano");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        setResizable(false);
        setLocationRelativeTo(null);

        initMidi();
        buildKeyHotspots();

        setContentPane(keyboardPanel);
        MouseInput mouseInput = new MouseInput(this);
        keyboardPanel.addMouseListener(mouseInput);

        installShiftBinding();
        startCatTimer();
    }

    public void onMousePressed(Point point) {
        // While cats are on the keyboard, musical input is intentionally locked.
        if (catsBlocking) return;

        // Pedal tails apply a temporary pitch offset while held.
        if (Config.LEFT_PEDAL_HITBOX.contains(point)) {
            pedalOffset = -12;
            return;
        }
        if (Config.RIGHT_PEDAL_HITBOX.contains(point)) {
            pedalOffset = 12;
            return;
        }
        if (Config.MIDDLE_PEDAL_HITBOX.contains(point)) {
            pedalOffset = 0;
            return;
        }

        KeyHotspot hotspot = findHotspot(point);
        if (hotspot == null) return;

        if (Keys.isModifier(hotspot.label)) {
            // "#" raises and "b" lowers the next played note by one semitone.
            accidentalOffset = "#".equals(hotspot.label) ? 1 : -1;
            keyboardPanel.repaint();
            return;
        }

        int note = Keys.baseMidiFor(hotspot.label, hotspot.row, hotspot.col);
        if (note < 0 || channel == null) return;

        // Final note is base pitch + active modifiers.
        note += accidentalOffset;
        note += shiftHeld ? 12 : 0;
        note += pedalOffset;
        // MIDI notes must stay in 0..127.
        note = Math.max(0, Math.min(127, note));

        channel.noteOn(note, 90);
        activeNotes.put(hotspot, note);
    }

    public void onMouseReleased(Point point) {
        // Releasing side pedals returns to neutral pitch offset.
        if (Config.LEFT_PEDAL_HITBOX.contains(point) || Config.RIGHT_PEDAL_HITBOX.contains(point)) {
            pedalOffset = 0;
        }

        KeyHotspot hotspot = findHotspot(point);
        if (hotspot == null) return;

        if (Keys.isModifier(hotspot.label)) {
            // Modifier applies only while it is held.
            accidentalOffset = 0;
            keyboardPanel.repaint();
            return;
        }

        Integer note = activeNotes.remove(hotspot);
        if (note != null && channel != null) {
            channel.noteOff(note);
        }
    }

    public void onMouseClicked(Point point) {
        if (catsBlocking) {
            // Cat minigame: all three hearts must be clicked to unlock keys again.
            if (Config.SHOO_HEART_1.contains(point)) shooHeartsClicked[0] = true;
            if (Config.SHOO_HEART_2.contains(point)) shooHeartsClicked[1] = true;
            if (Config.SHOO_HEART_3.contains(point)) shooHeartsClicked[2] = true;

            if (shooHeartsClicked[0] && shooHeartsClicked[1] && shooHeartsClicked[2]) {
                catsBlocking = false;
                Arrays.fill(shooHeartsClicked, false);
            }
            keyboardPanel.repaint();
            return;
        }

        if (Config.SETTINGS_BUTTON_HITBOX.contains(point)) {
            JOptionPane.showMessageDialog(this,
                    "Settings placeholder.\nLater: add volume slider here.",
                    "Settings",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void initMidi() {
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            channel = synth.getChannels()[0];
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "MIDI init failed: " + e.getMessage(),
                    "Audio Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buildKeyHotspots() {
        // The key image is one big PNG, so we define manual click zones (hotspots).
        addRow(Keys.ROW_TOP, Config.TOP_ROW, 0);
        addRow(Keys.ROW_MID, Config.MID_ROW, 1);
        addRow(Keys.ROW_BOT, Config.BOT_ROW, 2);

        keyHotspots.add(new KeyHotspot("LC", 3, 0,
                new Point(Config.LOW_C_HITBOX.x + (Config.LOW_C_HITBOX.width / 2),
                        Config.LOW_C_HITBOX.y + (Config.LOW_C_HITBOX.height / 2)),
                Config.LOW_C_HITBOX));

        keyHotspots.add(new KeyHotspot("Bb", 3, 1,
                new Point(Config.B_FLAT_HITBOX.x + (Config.B_FLAT_HITBOX.width / 2),
                        Config.B_FLAT_HITBOX.y + (Config.B_FLAT_HITBOX.height / 2)),
                Config.B_FLAT_HITBOX));
    }

    private void addRow(String rowText, Point[] centers, int rowIndex) {
        int count = Math.min(rowText.length(), centers.length);
        for (int i = 0; i < count; i++) {
            Point c = centers[i];
            // Build circular hitboxes to match each painted key circle.
            Shape shape = new Ellipse2D.Double(
                    c.x - Config.KEY_RADIUS,
                    c.y - Config.KEY_RADIUS,
                    Config.KEY_RADIUS * 2.0,
                    Config.KEY_RADIUS * 2.0
            );
            keyHotspots.add(new KeyHotspot(String.valueOf(rowText.charAt(i)), rowIndex, i, c, shape));
        }
    }

    private KeyHotspot findHotspot(Point point) {
        for (KeyHotspot hotspot : keyHotspots) {
            if (hotspot.hitShape.contains(point)) {
                return hotspot;
            }
        }
        return null;
    }

    private void installShiftBinding() {
        // Uses key bindings (not KeyListener) so SHIFT works while panel has focus.
        InputMap inputMap = keyboardPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = keyboardPanel.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("pressed SHIFT"), "shiftPressed");
        inputMap.put(KeyStroke.getKeyStroke("released SHIFT"), "shiftReleased");

        actionMap.put("shiftPressed", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                shiftHeld = true;
            }
        });

        actionMap.put("shiftReleased", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                shiftHeld = false;
            }
        });
    }

    private void startCatTimer() {
        catTimer = new Timer(2000, e -> {
            // Every 2s, roll chance for cats to appear and block the keyboard.
            if (!catsBlocking && random.nextDouble() < 0.25) {
                catsBlocking = true;
                stopAllNotes();
                Arrays.fill(shooHeartsClicked, false);
            }
            keyboardPanel.repaint();
        });
        catTimer.start();
    }

    private void stopAllNotes() {
        // Safety stop when game state changes (e.g., cats appear mid-note).
        if (channel != null) {
            for (Integer note : activeNotes.values()) {
                channel.noteOff(note);
            }
        }
        activeNotes.clear();
        accidentalOffset = 0;
        pedalOffset = 0;
    }

    private class KeyboardPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // Draw from back to front so transparent PNG layers stack correctly.
            drawScaled(g2, assets.backOfKeyboard);
            drawScaled(g2, assets.keysBackground);
            drawKeyLabels(g2);
            drawScaled(g2, assets.catHeadsAndTailPeadles);

            if (catsBlocking) {
                drawScaled(g2, assets.catsOnTopOfKeyboard);
                drawScaled(g2, assets.shooToClick);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 22));
                g2.drawString("Cats blocked the keyboard! Click all hearts to shoo them.", 170, 60);
            }

            drawScaled(g2, assets.settingsButtonUnder);
            drawScaled(g2, assets.settingsButtonOutline);
            drawScaled(g2, assets.border);

            g2.dispose();
        }

        private void drawScaled(Graphics2D g2, java.awt.image.BufferedImage image) {
            if (image == null) return;
            g2.drawImage(image, 0, 0, Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT, null);
        }

        private void drawKeyLabels(Graphics2D g2) {
            g2.setColor(new Color(45, 45, 45));
            g2.setFont(new Font("SansSerif", Font.BOLD, 21));

            for (KeyHotspot hotspot : keyHotspots) {
                String label = hotspot.label;
                int x = hotspot.center.x - (label.length() > 1 ? 14 : 7);
                int y = hotspot.center.y + 7;
                g2.drawString(label, x, y);
            }
        }
    }

    private static class KeyHotspot {
        final String label;
        final int row;
        final int col;
        final Point center;
        final Shape hitShape;

        KeyHotspot(String label, int row, int col, Point center, Shape hitShape) {
            this.label = label;
            this.row = row;
            this.col = col;
            this.center = center;
            this.hitShape = hitShape;
        }
    }
}