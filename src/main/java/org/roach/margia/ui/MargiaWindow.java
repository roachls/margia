package org.roach.margia.ui;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.*;

import org.roach.margia.Musician;
import org.roach.margia.Transport;
import org.roach.margia.timing.TimingSource;

/**
 * The main program window
 */
public class MargiaWindow extends JFrame {
	private static final String ABOUT_MESSAGE = """
			<html>
			<h1>MARGIA</h1>

			<h3>The <u>M</u>usic <u>a</u>nd <u>R</u>hythm <u>G</u>enerating <u>I</u>ntelligent <u>A</u>gents</h3>

			<h3>By Stevie Roach</h3>
			<a href="mailto:roachls@yahoo.com">roachls@yahoo.com</a>
			""";
	private OptionPanel optionPanel;

	/**
	 * @param title     window title
	 * @param timing    the {@link TimingSource}
	 * @param transport the {@link Transport}
	 * @param musicians the musicians to display
	 * @throws HeadlessException if {@link GraphicsEnvironment#isHeadless()} returns
	 *                           true
	 */
	public MargiaWindow(String title, TimingSource timing, Transport transport, List<Musician> musicians)
			throws HeadlessException {
		super("MARGIA - " + title);
		this.getContentPane().setLayout(new BorderLayout());
		setupMenu();
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		var transportPanel = new TransportPanel(timing, transport);
		getContentPane().add(transportPanel, BorderLayout.SOUTH);
		optionPanel = new OptionPanel();
		var agentPanel = new AgentPanel(musicians, optionPanel);
		transportPanel.addTempoListener(agentPanel);
		getContentPane().add(agentPanel, BorderLayout.CENTER);
		getContentPane().add(optionPanel, BorderLayout.EAST);
		pack();
		optionPanel.setVisible(false);
		agentPanel.initMusicians();
	}

	private void setupMenu() {
		var menubar = new JMenuBar();

		var editMenu = new JMenu("Edit");
		editMenu.setMnemonic('E');
		var showOptionPaneMenuItem = new JCheckBoxMenuItem("Show Options");
		showOptionPaneMenuItem.setMnemonic(KeyEvent.VK_O);
		showOptionPaneMenuItem.addActionListener(_ -> optionPanel.setVisible(showOptionPaneMenuItem.isSelected()));
		editMenu.add(showOptionPaneMenuItem);
		menubar.add(editMenu);

		var helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');
		var aboutMenuItem = new JMenuItem("About");
		aboutMenuItem.setMnemonic(KeyEvent.VK_A);
		aboutMenuItem.addActionListener(_ -> JOptionPane.showMessageDialog(this, ABOUT_MESSAGE));
		helpMenu.add(aboutMenuItem);

		menubar.add(Box.createHorizontalGlue());
		menubar.add(helpMenu);
		this.setJMenuBar(menubar);
	}

}
