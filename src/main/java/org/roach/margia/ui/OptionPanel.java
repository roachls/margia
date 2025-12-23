package org.roach.margia.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.*;
import javax.swing.event.ChangeListener;

/**
 * GUI and musical options
 */
public class OptionPanel extends JPanel {
	private JSpinner radius;
	private JSpinner gravity;
	private JSpinner edgeLength;

	/**
	 * constructor
	 */
	public OptionPanel() {
		this.setLayout(new BorderLayout());
		this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		var innerPanel = new JPanel();
		innerPanel.setLayout(new GridLayout(0, 2, 3, 5));
		radius = addSpinner(innerPanel, MusicianComponent.RADIUS_PROPERTY, (double) MusicianComponent.DEFAULT_RADIUS, 1d, 50d, 1d, Integer.class);
		gravity = addSpinner(innerPanel, AgentPanel.GRAVITY_PROPERTY, AgentPanel.DEFAULT_GRAVITATIONAL_CONSTANT, 0.0, 20.0, 0.1,
				Double.class);
		edgeLength = addSpinner(innerPanel, AgentPanel.EDGE_LENGTH_PROPERTY, (double) AgentPanel.DEFAULT_EDGE_LENGTH, 20d, 150d, 1d,
				Integer.class);

		this.add(innerPanel, BorderLayout.NORTH);
	}

	private static JSpinner addSpinner(JPanel innerPanel, String propertyName, Double defValue, Double min, Double max,
			Double step, Class<? extends Number> type) {

		SpinnerNumberModel model;
		if (type.equals(Integer.class))
			model = new SpinnerNumberModel(defValue.intValue(), min.intValue(), max.intValue(), step.intValue());
		else
			model = new SpinnerNumberModel(defValue.doubleValue(), min.doubleValue(), max.doubleValue(),
					step.doubleValue());
		var spinner = new JSpinner(model);
		spinner.setName(propertyName);
		var spinnerLabel = new JLabel(propertyName);
		spinnerLabel.setLabelFor(spinner);
		innerPanel.add(spinnerLabel);
		innerPanel.add(spinner);
		return spinner;
	}

	/**
	 * @param listener listener for radius changes
	 */
	public void addRadiusListener(ChangeListener listener) {
		this.radius.addChangeListener(listener);
	}

	/**
	 * @param listener listener for gravity changes
	 */
	public void addGravityListener(ChangeListener listener) {
		this.gravity.addChangeListener(listener);
	}
	
	/**
	 * @param listener listener for edge length changes
	 */
	public void addEdgeLengthListener(ChangeListener listener) {
		this.edgeLength.addChangeListener(listener);
	}
}
