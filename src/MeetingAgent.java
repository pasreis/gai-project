package jadeproject;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.UnreadableException;

public class MeetingAgent extends Agent {
	private MeetingAgentGui myGui;
	private boolean schedulingMeeting = false;
	private AID[] meetingAgents;
	private final String agentType = "meeting-agent";
	private WeekCalendar weekCalendar = new WeekCalendar(); // TODO

	@Override
	protected void setup() {
		myGui = new MeetingAgentGui(this);
		myGui.display();

		System.out.println(getAID().getLocalName() + ": is now ready!");

		final int interval = 20000;

		addBehaviour(new TickerBehaviour(this, interval) {
			@Override
			protected void onTick() {
				if (!schedulingMeeting) {
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription serviceDescription = new ServiceDescription();

					serviceDescription.setType(agentType);

					template.addServices(serviceDescription);

					try {
						DFAgentDescription[] result = DFService.search(myAgent, template);
						System.out.println(getAID().getLocalName() + ": The following agents have been found:");
						meetingAgents = new AID[result.length];

						for (int i = 0; i < result.length - 1; ++i) {
							meetingAgents[i] = result[i].getName();

							if (!meetingAgents[i].equals(getAID().getLocalName())) {
								System.out.println(getAID().getLocalName() + ": " + meetingAgents[i].getLocalName());
							}
						}
					} catch (FIPAException ex) {
						ex.printStackTrace();
					}

					myAgent.addBehaviour(new BookMeetingBehavior());
				}
			}
		});

		
		DFAgentDescription dfAgentDescription = new DFAgentDescription();
		dfAgentDescription.setName(getAID());

		ServiceDescription serviceDescription = new ServiceDescription();
		serviceDescription.setType(agentType);
		serviceDescription.setName(agentType);

		dfAgentDescription.addServices(serviceDescription);

		try {
			DFService.register(this, dfAgentDescription);
		} catch (FIPAException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	protected void takeDown() {
		myGui.dispose();

		try {
			DFService.deregister(this);
		} catch (FIPAException ex) {
			ex.printStackTrace();
		}

		System.out.println(getAID().getLocalName() + ": terminated!");
	}
	
	public void scheduleMeeting() {
		System.out.println(weekCalendar); // DEBUG PRINT
		schedulingMeeting = true;
	}

	private class BookMeetingBehavior extends Behaviour {
		@Override
		public void action() {
			// TODO: schedule meetings
		}

		@Override
		public boolean done() {
			return true;
		}
	}
		
}