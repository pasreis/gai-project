package jadeproject;

import jade.core.AID;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class MeetingAgentGui extends JFrame {
	private MeetingAgent myAgent;
	
	private JTextField coco; // TODO
	
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
		
		p.add(button);
		
		getContentPane().add(p, BorderLayout.SOUTH);
		
	}
	
	public void display() {
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int) screenSize.getWidth() / 2;
		int centerY = (int) screenSize.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerX - getHeight() / 2);
		setVisible(true);
	}
}