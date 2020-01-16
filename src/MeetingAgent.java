package jadeproject;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.UnreadableException;

public class MeetingAgent extends Agent {
	private MeetingAgentGui myGui;
	
	protected void setup() {
		myGui = new MeetingAgentGui(this);
		myGui.display();
	}
	
	public void scheduleMeeting() {
		System.out.println("cenas"); // DEBUG PRINT
	}
		
}