package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.sound.midi.*;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import utils.Config;

public class TypeWriterFrame extends JFrame {
    private static final Color SOFT_PINK_BG = new Color(245, 191, 215);
    private static final Color CREAM_WHITE = new Color(255, 246, 230);

    private final CatAssets assets = new CatAssets();
    private final Random random = new Random();

    private Synthesizer synth;
    private MidiChannel[] channels;

    private final List<KeyHotspot> keyHotspots = new ArrayList<>();
    // Tracks the exact MIDI note started by each hotspot so key release turns off the same note.
    private final Map<KeyHotspot, Integer> activeNotes = new HashMap<>();

    private final boolean[] shooHeartsClicked = new boolean[3];

    private boolean shiftHeld = false;
    private int accidentalOffset = 0;
    private int pedalOffset = 0;
    private boolean catsBlocking = false;
    private boolean angryCatsVisible = false;

    private boolean settingsOpen = false;
    private boolean secondScreenOpen = false;
    private int notesPlayed = 0;                // counts how many notes have been played
    //broken keys mechanics
    private List<Point> brokenKeyPoints = new ArrayList<>();
    private double[] fixProgress;
    private Color[] keyColors;
    private boolean mouseHeld = false;
    private boolean[] fixedKeys; 

    // 1..100 shown in settings screen and also used as note velocity.
    private int masterVolume = 90;

    private final List<FloatingNumber> floatingNumbers = new ArrayList<>();

    private static final Point[] HOLDER_POINTS = {
        // Row 1 (10)
        new Point(116, 251), new Point(203, 252), new Point(294, 251),
        new Point(384, 251), new Point(468, 254), new Point(557, 254),
        new Point(642, 256), new Point(726, 257), new Point(814, 257),
        new Point(899, 256),

        // Row 2 (11)
        new Point(89, 359),  new Point(171, 358), new Point(258, 359),
        new Point(344, 360), new Point(429, 358), new Point(512, 359),
        new Point(590, 360), new Point(674, 360), new Point(757, 362),
        new Point(837, 361), new Point(912, 362),

        // Row 3 (11)
        new Point(129, 449), new Point(208, 450), new Point(286, 448),
        new Point(369, 448), new Point(450, 449), new Point(528, 449),
        new Point(605, 449), new Point(684, 451), new Point(764, 448),
        new Point(839, 450), new Point(915, 452),

        // Row 4 (12)
        new Point(75, 526),  new Point(176, 527), new Point(255, 526),
        new Point(334, 527), new Point(414, 527), new Point(488, 525),
        new Point(565, 523), new Point(639, 526), new Point(712, 524),
        new Point(786, 526), new Point(861, 522), new Point(937, 525)
    };


    private void generateBrokenKeys() {
        brokenKeyPoints.clear();
        int count = 15 + random.nextInt(7);

        fixedKeys = new boolean[count];
        fixProgress = new double[count];
        keyColors = new Color[count];

        Set<Integer> used = new HashSet<>();

        while (brokenKeyPoints.size() < count) {
            int index = random.nextInt(HOLDER_POINTS.length);
            if (used.add(index)) {
                brokenKeyPoints.add(HOLDER_POINTS[index]);
            }
        }
        for (int i = 0; i < count; i++) {
            fixProgress[i] = 0;
            keyColors[i] = Color.RED;
        }
    }




    private final KeyboardPanel keyboardPanel = new KeyboardPanel();
    private Timer catTimer;
    private Timer floatingAnimationTimer;

    public TypeWriterFrame() {
        super("Kitten Typewriter Piano");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        setResizable(false);
        setLocationRelativeTo(null);

        initMidi();
        buildKeyHotspots();
        initFloatingNumbers();

        setContentPane(keyboardPanel);
        MouseInput mouseInput = new MouseInput(this);
        keyboardPanel.addMouseListener(mouseInput);

        keyboardPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (secondScreenOpen) {
                 handleBrokenKeyMotion(e.getPoint());
                }
            }
        });


        installShiftBinding();
        startFloatingAnimationTimer();
        startCatTimer();
    }

    public void onMousePressed(Point point) {
        // block piano clicks while settings page is open
        if (settingsOpen) {
            return;
        }
        // in repair screen we only care about hold + drag
        if (secondScreenOpen) {
            mouseHeld = true;
            return;
        }


        Point modelPoint = toModelPoint(point);
        // while cats are blocking, ignore key input
        if (catsBlocking) return;

        // Pedal tails apply a temporary pitch offset while held.
        if (Config.LEFT_PEDAL_HITBOX.contains(modelPoint)) {
            pedalOffset = -12;
            angryCatsVisible = true;
            keyboardPanel.repaint();
            return;
        }
        if (Config.RIGHT_PEDAL_HITBOX.contains(modelPoint)) {
            pedalOffset = 12;
            angryCatsVisible = true;
            keyboardPanel.repaint();
            return;
        }
        if (Config.MIDDLE_PEDAL_HITBOX.contains(modelPoint)) {
            pedalOffset = 0;
            angryCatsVisible = true;
            keyboardPanel.repaint();
            return;
        }

        KeyHotspot hotspot = findHotspot(modelPoint);
        if (hotspot == null) return;

        if (Keys.isModifier(hotspot.label)) {
            accidentalOffset = "#".equals(hotspot.label) ? 1 : -1;
            keyboardPanel.repaint();
            return;
        }

        int note = Keys.baseMidiFor(hotspot.label, hotspot.row, hotspot.col);
        if (note < 0 || channels == null || channels[0] == null) return;

        note += accidentalOffset;
        note += shiftHeld ? 12 : 0;
        note += pedalOffset;
        note = Math.max(0, Math.min(127, note));
        channels[0].noteOn(note, masterVolume);
        System.out.println(shiftHeld);
        activeNotes.put(hotspot, note);

        // Count how many notes have been played.
        notesPlayed++;

        if (notesPlayed >= 15 && !secondScreenOpen) {
            secondScreenOpen = true;
            notesPlayed = 0;
            stopAllNotes();          // stop any currently playing notes
            generateBrokenKeys();
            keyboardPanel.repaint(); // redraw UI to show the new screen
        }
    }

    public void onMouseReleased(Point point) {
        // no piano release actions while settings is open
        if (settingsOpen) {
            return;
        }
        if (secondScreenOpen) {
            mouseHeld = false;
            return;
        }

        Point modelPoint = toModelPoint(point);
        // releasing input hides angry eyebrow overlay
        angryCatsVisible = false;
        if (Config.LEFT_PEDAL_HITBOX.contains(modelPoint) || Config.RIGHT_PEDAL_HITBOX.contains(modelPoint)) {
            pedalOffset = 0;
        }

        KeyHotspot hotspot = findHotspot(modelPoint);
        if (hotspot == null) return;

        if (Keys.isModifier(hotspot.label)) {
            accidentalOffset = 0;
            keyboardPanel.repaint();
            return;
        }

        Integer note = activeNotes.remove(hotspot);
        if (note != null && channels != null && channels[0] != null) {
            channels[0].noteOff(note);
        }
    }

    public void onMouseClicked(Point point) {
        // clicks are disabled in repair scene (drag is used instead)
        if (secondScreenOpen) {
            // no click actions here, repair uses drag movement
            return;
        }

        if (settingsOpen) {
            handleSettingsClick(point);
            return;
        }

        Point modelPoint = toModelPoint(point);

        if (catsBlocking) {
            if (Config.SHOO_HEART_1.contains(modelPoint)) shooHeartsClicked[0] = true;
            if (Config.SHOO_HEART_2.contains(modelPoint)) shooHeartsClicked[1] = true;
            if (Config.SHOO_HEART_3.contains(modelPoint)) shooHeartsClicked[2] = true;

            if (shooHeartsClicked[0] && shooHeartsClicked[1] && shooHeartsClicked[2]) {
                catsBlocking = false;
                Arrays.fill(shooHeartsClicked, false);
            }
            keyboardPanel.repaint();
            return;
        }

        if (Config.SETTINGS_BUTTON_HITBOX.contains(modelPoint)) {
            settingsOpen = true;
            keyboardPanel.repaint();
        }
    }

    private void handleSettingsClick(Point point) {
        Point modelPoint = toModelPoint(point);
        // same paw button acts as back from settings
        if (Config.SETTINGS_BUTTON_HITBOX.contains(modelPoint)) {
            settingsOpen = false;
            keyboardPanel.repaint();
            return;
        }

        for (FloatingNumber floatingNumber : floatingNumbers) {
            if (floatingNumber.hitBox != null && floatingNumber.hitBox.contains(point)) {
                setMasterVolume(floatingNumber.value);
                keyboardPanel.repaint();
                return;
            }
        }
    }

    private void setMasterVolume(int value) {
        masterVolume = Math.max(1, Math.min(100, value));

        // Also updates channel volume controller, so sustained notes feel consistent.
        if (channels != null && channels[0] != null) {
            int midiControllerVolume = Math.max(0, Math.min(127, (int) Math.round(masterVolume * 1.27)));
            channels[0].controlChange(7, midiControllerVolume);
        }
    }

    private Point toModelPoint(Point screenPoint) {
        // Play scene art is drawn with UI_OFFSET_Y translation; convert clicks into that model space.
        return new Point(screenPoint.x, screenPoint.y - Config.UI_OFFSET_Y);
    }

    private void initMidi() {
        try {
            // open synth and set channels before loading meow instruments
            synth = MidiSystem.getSynthesizer();
            synth.open();
            channels = synth.getChannels();
            File meowFile = new File("assets/audio/meow.sf2");
            Soundbank msb = MidiSystem.getSoundbank(meowFile);
            if (msb == null) {
                throw new IllegalStateException("meow.sf2 could not be loaded");
            }
            Instrument[] meows = msb.getInstruments();
            if (meows.length < 3) {
                throw new IllegalStateException("meow.sf2 needs at least 3 instruments");
            }

            synth.loadInstrument(meows[0]);
            synth.loadInstrument(meows[1]);
            synth.loadInstrument(meows[2]);
            channels[0].programChange(meows[0].getPatch().getBank(), meows[0].getPatch().getProgram());
            channels[1].programChange(meows[1].getPatch().getBank(), meows[1].getPatch().getProgram());
            channels[2].programChange(meows[2].getPatch().getBank(), meows[2].getPatch().getProgram());
            setMasterVolume(masterVolume);
        } catch (Exception e) {
            channels = null;
        }
    }

    private void buildKeyHotspots() {
        // One PNG for keys: manual circular hitboxes. Order: row1 → row2 → row3 → row4 → space bar.
        addRow(Keys.ROW_1, Config.ROW1, 0);
        addRow(Keys.ROW_2, Config.ROW2, 1);
        addRow(Keys.ROW_3, Config.ROW3, 2);
        addRow(Keys.ROW_4, Config.ROW4, 3);

        keyHotspots.add(new KeyHotspot("SP", 4, 0,
                new Point(Config.SPACEBAR_HITBOX.x + (Config.SPACEBAR_HITBOX.width / 2),
                        Config.SPACEBAR_HITBOX.y + (Config.SPACEBAR_HITBOX.height / 2)),
                Config.SPACEBAR_HITBOX));
    }

    private void addRow(String rowText, Point[] centers, int rowIndex) {
        int count = Math.min(rowText.length(), centers.length);
        for (int i = 0; i < count; i++) {
            Point c = centers[i];
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
        // cats interrupt every 25s if they are not already on the keyboard
        catTimer = new Timer(25000, e -> {
            if (!catsBlocking) {
                catsBlocking = true;
                stopAllNotes();
                Arrays.fill(shooHeartsClicked, false);
            }
            keyboardPanel.repaint();
        });
        catTimer.start();
    }

    private void initFloatingNumbers() {
        floatingNumbers.clear();
        for (int value = 1; value <= 100; value++) {
            float x = 110 + random.nextInt(Math.max(1, Config.WINDOW_WIDTH - 220));
            float y = 110 + random.nextInt(Math.max(1, Config.WINDOW_HEIGHT - 200));
            float vx = randomSpeed();
            float vy = randomSpeed();
            floatingNumbers.add(new FloatingNumber(value, x, y, vx, vy));
        }
    }

    private float randomSpeed() {
        float magnitude = 0.6f + (random.nextFloat() * 1.0f);
        return random.nextBoolean() ? magnitude : -magnitude;
    }

    private void startFloatingAnimationTimer() {
        // 33ms tick ~= 30fps for smooth floating numbers
        floatingAnimationTimer = new Timer(33, e -> {
            if (settingsOpen) {
                updateFloatingNumbers();
            }
            keyboardPanel.repaint();
        });
        floatingAnimationTimer.start();
    }

    private void updateFloatingNumbers() {
        int despawnMargin = 140;

        for (FloatingNumber floatingNumber : floatingNumbers) {
            if (floatingNumber.respawnTicks > 0) {
                floatingNumber.respawnTicks--;
                if (floatingNumber.respawnTicks == 0) {
                    respawnFloatingNumber(floatingNumber);
                } else {
                    floatingNumber.hitBox = null;
                }
                continue;
            }

            floatingNumber.x += floatingNumber.vx;
            floatingNumber.y += floatingNumber.vy;

            // Let numbers travel off-screen, then wait before re-entering.
            if (floatingNumber.x < -despawnMargin
                    || floatingNumber.x > Config.WINDOW_WIDTH + despawnMargin
                    || floatingNumber.y < -despawnMargin
                    || floatingNumber.y > Config.WINDOW_HEIGHT + despawnMargin) {
                floatingNumber.respawnTicks = 120 + random.nextInt(220); // about 4s to 11s
                floatingNumber.hitBox = null;
            }
        }
    }

    private void respawnFloatingNumber(FloatingNumber floatingNumber) {
        int side = random.nextInt(4);
        float drift = randomSpeed() * 0.55f;

        if (side == 0) { // left -> right
            floatingNumber.x = -70;
            floatingNumber.y = 70 + random.nextInt(Math.max(1, Config.WINDOW_HEIGHT - 140));
            floatingNumber.vx = Math.abs(randomSpeed());
            floatingNumber.vy = drift;
        } else if (side == 1) { // right -> left
            floatingNumber.x = Config.WINDOW_WIDTH + 70;
            floatingNumber.y = 70 + random.nextInt(Math.max(1, Config.WINDOW_HEIGHT - 140));
            floatingNumber.vx = -Math.abs(randomSpeed());
            floatingNumber.vy = drift;
        } else if (side == 2) { // top -> down
            floatingNumber.x = 70 + random.nextInt(Math.max(1, Config.WINDOW_WIDTH - 140));
            floatingNumber.y = -60;
            floatingNumber.vx = drift;
            floatingNumber.vy = Math.abs(randomSpeed());
        } else { // bottom -> up
            floatingNumber.x = 70 + random.nextInt(Math.max(1, Config.WINDOW_WIDTH - 140));
            floatingNumber.y = Config.WINDOW_HEIGHT + 60;
            floatingNumber.vx = drift;
            floatingNumber.vy = -Math.abs(randomSpeed());
        }
        floatingNumber.hitBox = null;
    }

    private void stopAllNotes() {
        // safety stop so no notes get stuck when scene/state changes
        if (channels != null && channels[0] != null) {
            for (Integer note : activeNotes.values()) {
                channels[0].noteOff(note);
            }
        }
        activeNotes.clear();
        accidentalOffset = 0;
        pedalOffset = 0;
    }

    private void handleBrokenKeyMotion(Point p) {
        // only progress repairs while mouse button is held down
        if (!mouseHeld) return;

        for (int i = 0; i < brokenKeyPoints.size(); i++) {
            if (fixedKeys[i]) continue;

            Point target = brokenKeyPoints.get(i);

            double dx = p.x - target.x;
            double dy = p.y - target.y;
            double dist = Math.sqrt(dx*dx + dy*dy);

                if (dist < 40 && dist > 20) {
                    // Add progress based on movement amount
                    fixProgress[i] += 2.5; // tune speed here

                    if (fixProgress[i] >= 100) {
                        fixedKeys[i] = true;
                        // Assign random color (not red)
                        keyColors[i] = new Color(
                        50 + random.nextInt(205),
                        50 + random.nextInt(205),
                        50 + random.nextInt(205)
                    );
                }
            }
        }
        
        boolean allFixed = true;
        for (boolean f : fixedKeys) {
            if (!f) { allFixed = false; break; }
        }

        if (allFixed) {
            secondScreenOpen = false;
            mouseHeld = false;
        }

        keyboardPanel.repaint();
    }


    private class KeyboardPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // scene priority: repair -> settings -> main play scene
            if (secondScreenOpen) {
                drawSecondScene(g2);
            } else if (settingsOpen) {
                drawSettingsScene(g2);
            } else {
                Graphics2D playG2 = (Graphics2D) g2.create();
                playG2.translate(0, Config.UI_OFFSET_Y);
                drawPlayScene(playG2);
                playG2.dispose();
            }


            drawVolumeDisplay(g2);
            g2.dispose();
        }

        private void drawPlayScene(Graphics2D g2) {
            // normal gameplay scene layers
            drawScaled(g2, assets.backOfKeyboard);
            drawScaled(g2, assets.keysBackground);
            drawKeyLabels(g2);
            drawScaled(g2, assets.catHeadsAndTailPeadles);
            if (angryCatsVisible) {
                drawScaled(g2, assets.angryCats);
            }

            if (catsBlocking) {
                drawScaled(g2, assets.catsOnTopOfKeyboard);
                drawScaled(g2, assets.shooToClick);
                drawOutlinedText(g2, "click the hearts to shoo", 230, 170, 35);
            }

            drawScaled(g2, assets.settingsButtonUnder);
            drawScaled(g2, assets.settingsButtonOutline);
            drawSettingsButtonText(g2, "Settings");
            drawScaled(g2, assets.border);
        }

        private void drawSecondScene(Graphics2D g2) {
            // Fill the whole window with a soft background color.
            drawScaled(g2, assets.secondScreenBack);
            drawScaled(g2, assets.strings);
            drawScaled(g2, assets.Holders);
            drawScaled(g2, assets.Handels);

            for (int i = 0; i < brokenKeyPoints.size(); i++) {
                g2.setColor(keyColors[i]);
                Point p = brokenKeyPoints.get(i);
                g2.fillOval(p.x - 10, p.y - 10, 20, 20);
            }

            g2.setFont(new Font("SansSerif", Font.BOLD, 35));
            
            String text = "Oh no! Looks like some keys are broken!";
            FontMetrics metrics = g2.getFontMetrics();
            int textWidth = metrics.stringWidth(text);
            int x = (Config.WINDOW_WIDTH - textWidth) / 2;
            int y = 60;

            // Draw black outline (4 directions)
            g2.setColor(Color.BLACK);
            g2.drawString(text, x - 2, y);
            g2.drawString(text, x + 2, y);
            g2.drawString(text, x, y - 2);
            g2.drawString(text, x, y + 2);

            // Draw main text on top
            g2.setColor(CREAM_WHITE);
            g2.drawString(text, x, y);

            g2.setFont(new Font("SansSerif", Font.BOLD, 25));
            String sub1 = "Let’s quickly fix them!";
            String sub2 = "Click and move your cursor around each broken key.";

            // Line 1
            FontMetrics fm = g2.getFontMetrics();
            int w1 = fm.stringWidth(sub1);
            int x1 = (Config.WINDOW_WIDTH - w1) / 2;
            int y1 = 110;

            // Outline
            g2.setColor(Color.BLACK);
            g2.drawString(sub1, x1 - 2, y1);
            g2.drawString(sub1, x1 + 2, y1);
            g2.drawString(sub1, x1, y1 - 2);
            g2.drawString(sub1, x1, y1 + 2);

            // Main text
            g2.setColor(CREAM_WHITE);
            g2.drawString(sub1, x1, y1);


            // Line 2
            int w2 = fm.stringWidth(sub2);
            int x2 = (Config.WINDOW_WIDTH - w2) / 2 - 70;
            int y2 = y1 + 45; // spacing below line 1

            // Outline
            g2.setColor(Color.BLACK);
            g2.drawString(sub2, x2 - 2, y2);
            g2.drawString(sub2, x2 + 2, y2);
            g2.drawString(sub2, x2, y2 - 2);
            g2.drawString(sub2, x2, y2 + 2);

            // Main text
            g2.setColor(CREAM_WHITE);
            g2.drawString(sub2, x2, y2);

        }



        private void drawSettingsScene(Graphics2D g2) {
            g2.setColor(SOFT_PINK_BG);
            g2.fillRect(0, 0, Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);

            drawFloatingNumbers(g2);

            // Reuse the exact same visual position as play scene (includes UI vertical offset).
            Graphics2D buttonLayer = (Graphics2D) g2.create();
            buttonLayer.translate(0, Config.UI_OFFSET_Y);
            drawScaled(buttonLayer, assets.settingsButtonUnder);
            drawScaled(buttonLayer, assets.settingsButtonOutline);
            drawSettingsButtonText(buttonLayer, "Back");
            buttonLayer.dispose();
        }

        private void drawFloatingNumbers(Graphics2D g2) {
            g2.setColor(CREAM_WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 26));
            FontMetrics metrics = g2.getFontMetrics();

            for (FloatingNumber floatingNumber : floatingNumbers) {
                if (floatingNumber.respawnTicks > 0) {
                    floatingNumber.hitBox = null;
                    continue;
                }

                String text = Integer.toString(floatingNumber.value);
                int textX = Math.round(floatingNumber.x);
                int textY = Math.round(floatingNumber.y);

                int width = metrics.stringWidth(text);
                int height = metrics.getHeight();
                int hitY = textY - metrics.getAscent();
                floatingNumber.hitBox = new Rectangle(textX - 4, hitY - 2, width + 8, height + 4);

                // Slight highlight for currently selected volume.
                if (floatingNumber.value == masterVolume) {
                    g2.setColor(new Color(255, 255, 255, 70));
                    g2.fillRoundRect(floatingNumber.hitBox.x, floatingNumber.hitBox.y,
                            floatingNumber.hitBox.width, floatingNumber.hitBox.height, 12, 12);
                    g2.setColor(CREAM_WHITE);
                }

                g2.drawString(text, textX, textY);
            }
        }

        private void drawSettingsButtonText(Graphics2D g2, String text) {
            Rectangle button = Config.SETTINGS_BUTTON_HITBOX;
            g2.setColor(CREAM_WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, text.length() > 1 ? 18 : 28));
            FontMetrics metrics = g2.getFontMetrics();
            int x = button.x + (button.width - metrics.stringWidth(text)) / 2 + 4;
            int y = button.y + (button.height + metrics.getAscent()) / 2 + 10;
            g2.drawString(text, x, y);
        }

        private void drawVolumeDisplay(Graphics2D g2) {
            String text = "Vol: " + masterVolume;
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            FontMetrics metrics = g2.getFontMetrics();
            int padding = 10;
            int boxWidth = metrics.stringWidth(text) + (padding * 2);
            int boxHeight = metrics.getHeight() + 6;
            int x = Config.WINDOW_WIDTH - boxWidth - 16;
            int y = 16;

            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillRoundRect(x, y, boxWidth, boxHeight, 14, 14);
            g2.setColor(CREAM_WHITE);
            g2.drawString(text, x + padding, y + metrics.getAscent() + 2);
        }

        private void drawOutlinedText(Graphics2D g2, String text, int x, int y, int fontSize) {
            g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            g2.setColor(Color.BLACK);
            g2.drawString(text, x - 2, y);
            g2.drawString(text, x + 2, y);
            g2.drawString(text, x, y - 2);
            g2.drawString(text, x, y + 2);
            g2.setColor(CREAM_WHITE);
            g2.drawString(text, x, y);
        }

        private void drawScaled(Graphics2D g2, java.awt.image.BufferedImage image) {
            if (image == null) return;
            g2.drawImage(image, 0, 0, Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT, null);
        }

        private void drawKeyLabels(Graphics2D g2) {
            g2.setColor(CREAM_WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 21));

            for (KeyHotspot hotspot : keyHotspots) {
                String label = "SP".equals(hotspot.label) ? "Space" : hotspot.label;
                int x = hotspot.center.x - (label.length() > 2 ? 24 : (label.length() > 1 ? 14 : 7));
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

    private static class FloatingNumber {
        final int value;
        float x;
        float y;
        float vx;
        float vy;
        int respawnTicks;
        Rectangle hitBox;

        FloatingNumber(int value, float x, float y, float vx, float vy) {
            this.value = value;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }
    }
}
