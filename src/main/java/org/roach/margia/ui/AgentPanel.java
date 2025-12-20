package org.roach.margia.ui;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.JPanel;

import org.roach.margia.Musician;

/**
 * GUI element that displays multiple {@link Musician Musicians} in a
 * {@link JPanel}
 */
public class AgentPanel extends JPanel {

	/**
	 * @param musicians        {@link Musician musicians} to display
	 * @param tickLengthMillis length of a tick in milliseconds
	 */
	public AgentPanel(List<Musician> musicians, int tickLengthMillis) {
		setLayout(new GridLayout(4, 4));
		setBackground(Color.black);
		// last musician is random
		for (var i = 0; i < 16; i++) {
			var musician = musicians.get(i);
			var agentComponent = new AgentComponent(musician, tickLengthMillis);
			add(agentComponent);
		}
	}
}
