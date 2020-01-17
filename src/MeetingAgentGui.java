package jadeproject;

import jade.core.AID;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class MeetingAgentGui extends JFrame {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private MeetingAgent myAgent;
	
	MeetingAgentGui(MeetingAgent a) {
		super(a.getLocalName());
		
		myAgent = a;
		
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(1, 1));

		JButton button = new JButton("Schedule Meeting");
		button.addActionListener(new ActionListener () {
			public void actionPerformed(ActionEvent ev) {
				try {
					myAgent.scheduleMeeting();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(MeetingAgentGui.this, e.getMessage());
				}
			}
		});
		
		button.setPreferredSize(new Dimension(300,30));
		p.add(button);
		
		getContentPane().add(p, BorderLayout.SOUTH);
		
		setResizable(false);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.doDelete();
			}
		});
	}
	
	public void display() {
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int) screenSize.getWidth() / 2;
		int centerY = (int) screenSize.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
		setVisible(true);
	}
}