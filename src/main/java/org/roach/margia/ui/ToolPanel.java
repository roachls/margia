package org.roach.margia.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import org.roach.margia.ui.AgentPanel.EditMode;

/**
 * {@link JPanel} with toolset for editing musicians
 */
@SuppressWarnings({ "java:S1948" })
public class ToolPanel extends JPanel {
    private final AgentPanel agentPanel;

    /**
     * @param agentPanel the agent panel
     */
    public ToolPanel(AgentPanel agentPanel) {
        super(new BorderLayout());
        this.agentPanel = agentPanel;
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        initGui();
    }

    private void initGui() {
        var innerPanel = new JPanel(new GridLayout(0, 1, 3, 5));
        
        var buttonFont = new Font("SansSerif", Font.PLAIN, 18);

        var selectButton = new JToggleButton("⛭");
        selectButton.setToolTipText("View/Edit musical properties of the selected musician");
        selectButton.setFont(buttonFont);
        selectButton.setName(EditMode.SELECT.name());
        var addButton = new JToggleButton("+");
        addButton.setToolTipText("Add musician at cursor location");
        addButton.setFont(buttonFont);
        addButton.setName(EditMode.ADD.name());
        var deleteButton = new JToggleButton("-");
        deleteButton.setToolTipText("Delete musician at cursor location");
        deleteButton.setFont(buttonFont);
        deleteButton.setName(EditMode.DELETE.name());
        var muteButton = new JToggleButton("🔇");
        muteButton.setFont(buttonFont);
        muteButton.setToolTipText("Toggle muting of musician");
        muteButton.setName(EditMode.MUTE.name());
        var muteAllButton = new JButton("🔇 all");
        muteAllButton.setToolTipText("Mute all musicians");
        muteAllButton.setFont(buttonFont);
        muteAllButton.addActionListener(_ -> agentPanel.muteAll());
        var unmuteAllButton = new JButton("🔊 all");
        unmuteAllButton.setFont(buttonFont);
        unmuteAllButton.setToolTipText("Unmute all musicians");
        unmuteAllButton.addActionListener(_ -> agentPanel.unmuteAll());
        var lockButton = new JToggleButton("🔒");
        lockButton.setFont(buttonFont);
        lockButton.setToolTipText("Toggle whether a musician's screen location is locked");
        lockButton.setName(EditMode.LOCK.name());
        var lockAllButton = new JButton("🔒 all");
        lockAllButton.setToolTipText("Lock the screen locations of all musicians");
        lockAllButton.setFont(buttonFont);
        lockAllButton.addActionListener(_ -> agentPanel.lockAll());
        var unlockAllButton = new JButton("🔓 all");
        unlockAllButton.setFont(buttonFont);
        unlockAllButton.setToolTipText("Unlock the screen locations of all musicians");
        unlockAllButton.addActionListener(_ -> agentPanel.unlockAll());
        var connectButton = new JToggleButton("🔗");
        connectButton.setFont(buttonFont);
        connectButton.setToolTipText("Connect or disconnect musicians");
        connectButton.setName(EditMode.CONNECT.name());
        var moveIconUrl = getClass().getResource("/icons/move.png");
        var moveIcon = new ImageIcon(moveIconUrl);
        moveIcon = new ImageIcon(moveIcon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));
        var moveButton = new JToggleButton(moveIcon);
        moveButton.setFont(buttonFont);
        moveButton.setToolTipText("Move and lock a musician onscreen");
        moveButton.setName(EditMode.MOVE.name());

        var group = new ButtonGroup();
        group.add(addButton);
        group.add(deleteButton);
        group.add(lockButton);
        group.add(muteButton);
        group.add(selectButton);
        group.add(connectButton);
        group.add(moveButton);
        selectButton.setSelected(true);

        innerPanel.add(selectButton);
        innerPanel.add(addButton);
        innerPanel.add(deleteButton);
        innerPanel.add(muteButton);
        innerPanel.add(lockButton);
        innerPanel.add(connectButton);
        innerPanel.add(moveButton);

        innerPanel.add(muteAllButton);
        innerPanel.add(unmuteAllButton);
        innerPanel.add(lockAllButton);
        innerPanel.add(unlockAllButton);

        var modeActionListener = new ModeActionListener();
        selectButton.addActionListener(modeActionListener);
        addButton.addActionListener(modeActionListener);
        deleteButton.addActionListener(modeActionListener);
        muteButton.addActionListener(modeActionListener);
        lockButton.addActionListener(modeActionListener);
        connectButton.addActionListener(modeActionListener);
        moveButton.addActionListener(modeActionListener);

        add(innerPanel, BorderLayout.NORTH);
    }

    private class ModeActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof JToggleButton jtb) {
                var name = jtb.getName();
                var mode = EditMode.valueOf(name);
                agentPanel.setEditMode(mode);
            }
        }
    }
}
