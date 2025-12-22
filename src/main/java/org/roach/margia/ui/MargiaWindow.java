package org.roach.margia.ui;

import java.awt.*;
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

	/**
	 * @param timing    the {@link TimingSource}
	 * @param transport the {@link Transport}
	 * @param musicians the musicians to display
	 * @throws HeadlessException if {@link GraphicsEnvironment#isHeadless()} returns
	 *                           true
	 */
	public MargiaWindow(TimingSource timing, Transport transport, List<Musician> musicians) throws HeadlessException {
		super("MARGIA");
		this.getContentPane().setLayout(new BorderLayout());
//		this.setLocationRelativeTo(null);
		setupMenu();
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().add(new TransportPanel(timing, transport), BorderLayout.SOUTH);
		getContentPane().add(new AgentPanel(musicians, transport.getTickLength()), BorderLayout.CENTER);
		pack();
	}
	
	private void setupMenu() {
		var menubar = new JMenuBar();
		var helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');
		var aboutMenuItem = new JMenuItem("About");
		aboutMenuItem.addActionListener(_ -> JOptionPane.showMessageDialog(this, ABOUT_MESSAGE));
		helpMenu.add(aboutMenuItem);

		menubar.add(helpMenu);
		this.setJMenuBar(menubar);
	}

}
