package org.roach.margia.ui;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;

import org.roach.margia.storage.Persistence;

class MouseDragAdapter extends MouseAdapter {
    private final JDialog dialog;
    private final String name;
    private Point pressLocation;

    /**
     * @param dialog dialog being dragged
     */
    public MouseDragAdapter(JDialog dialog) {
        this.dialog = dialog;
        this.name = dialog.getName();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // Record the initial click location relative to the screen
        pressLocation = e.getLocationOnScreen();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // Calculate the difference in cursor position
        Point currentLocation = e.getLocationOnScreen();
        int deltaX = currentLocation.x - pressLocation.x;
        int deltaY = currentLocation.y - pressLocation.y;

        // Get the current dialog location
        Point dialogLocation = dialog.getLocation();

        // Set the new dialog location
        dialog.setLocation(dialogLocation.x + deltaX, dialogLocation.y + deltaY);

        // Update the press location for the next drag event
        pressLocation = currentLocation;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
        var comp = (JComponent) e.getSource();
        var loc = comp.getLocationOnScreen();
        Persistence.getInstance().saveProperty(name + "_x", Integer.toString(loc.x));
        Persistence.getInstance().saveProperty(name + "_y", Integer.toString(loc.y));
    }
}