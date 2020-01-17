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

import java.util.ArrayList;
import java.util.Random;

public class MeetingAgent extends Agent {
	private MeetingAgentGui myGui;
	private boolean schedulingMeeting = false;
	private AID[] meetingAgents;
	private final String agentType = "meeting-agent";
	private WeekCalendar weekCalendar = new WeekCalendar();
	ArrayList<int[]> availableSlots= weekCalendar.listAllAvailableSlots();;
	private Random random = new Random();

	@Override
	protected void setup() {
		myGui = new MeetingAgentGui(this);
		myGui.display();

		System.out.println(getAID().getLocalName() + ": is now ready!");

		final int interval = 5000;

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

		

		System.out.println("done!");
		schedulingMeeting = true;
	}

	private class BookMeetingBehavior extends Behaviour {
		private AID[] attendees;
		private int[] slot;
		private int step = 0;
		private int numberOfAttendees = 0;
		private long begin, end;
		private MessageTemplate template;
		

		@Override
		public void action() {
			switch (step) {
				case 0:
					// Select Attendees and meeting slot
					System.out.println(getAID().getLocalName() + ": is deciding the attendees list for the meeting...");
					
					int maximumAttendees = meetingAgents.length - 1;
					numberOfAttendees = random.nextInt(maximumAttendees) + 1;

					System.out.println(getAID().getLocalName() + ": decided to invite " + numberOfAttendees + " attendees to the meeting!");

					if (numberOfAttendees != 0) {
						attendees = new AID[numberOfAttendees];

						System.out.println(getAID().getLocalName() + ": Attendee list:");
						for (int i = 0; i < numberOfAttendees; ++i) {
							attendees[i] = meetingAgents[random.nextInt(meetingAgents.length - 1)];
							System.out.println(getAID().getLocalName() + ": Attendee" + (i + 1) + ": " + attendees[i].getLocalName());
						}

						System.out.println(getAID().getLocalName() + ": is choosing a meeting time...");
						
						slot = availableSlots.get(random.nextInt(availableSlots.size() - 1));

						if (slot == null) {
							// No available time found
							System.out.println(getAID().getLocalName() + ": does not have any free time to held the meeting");
							step = 10;
						} else {
							int day = slot[0], hour = slot[1];
							String weekDay = WeekCalendar.getWeekDayName(day);

							System.out.println(getAID().getLocalName() + ": Proposed meeting date: " + weekDay + " at " + hour + "H00");

							step = 1;
						}
					} else {
						step = 10; // TODO
					}

					break;
				
				case 1:
					// Send INVITATIONS to the choosen attendees
					begin = System.currentTimeMillis();

					System.out.println(getAID().getLocalName() + ": is sending invitations...");
					ACLMessage message = new ACLMessage(ACLMessage.CFP);

					for (int i = 0; i < numberOfAttendees; ++i) {
						message.addReceiver(attendees[i]);
					}

					message.setContent(Integer.toString(slot[0]) + "," + Integer.toString(slot[1]));
					message.setConversationId("schedule-meeting");
					message.setReplyWith("cfp" + System.currentTimeMillis());
					
					// block this slot, so it can be offered for another meeting!
					weekCalendar.scheduleMeeting(slot[0], slot[1]);
					availableSlots = weekCalendar.listAllAvailableSlots();

					myAgent.send(message);
					System.out.println(getAID().getLocalName() + ": Invitations sent! Waiting for answers...");

					template = MessageTemplate.and(MessageTemplate.MatchConversationId("schedule-meeting"),
						MessageTemplate.MatchInReplyTo(message.getReplyWith()));

					step = 2;
				default:
					System.out.println(getAID().getLocalName() + ": Error when deciding the attendees list!");

			}
		}

		@Override
		public boolean done() {
			return true;
		}
	}
		
}