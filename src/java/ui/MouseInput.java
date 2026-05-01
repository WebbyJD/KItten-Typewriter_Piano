package ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MouseInput extends MouseAdapter {
    private final TypeWriterFrame frame;

    public MouseInput(TypeWriterFrame frame) {
        this.frame = frame;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // press handles notes/pedals
        frame.onMousePressed(e.getPoint());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // release turns notes/pedals off
        frame.onMouseReleased(e.getPoint());
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Keep input rules centralized in TypeWriterFrame.
        frame.onMouseClicked(e.getPoint());
        // Debug helper for tuning hitbox coordinates in Config.
        System.out.println("Mouse clicked at: " + e.getX() + ", " + e.getY());
    }
}
