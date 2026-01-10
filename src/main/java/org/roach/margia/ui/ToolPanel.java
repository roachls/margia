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

        var addButton = new JToggleButton("+");
        addButton.setToolTipText("Add musician at cursor location");
        addButton.setFont(buttonFont);
        addButton.setName(EditMode.ADD.name());
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
        group.add(connectButton);
        group.add(moveButton);

        innerPanel.add(addButton);
        innerPanel.add(connectButton);
        innerPanel.add(moveButton);

        var modeActionListener = new ModeActionListener();
        addButton.addActionListener(modeActionListener);
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
