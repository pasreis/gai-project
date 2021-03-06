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
import javafx.beans.value.WritableLongValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import javafx.util.Pair;

public class MeetingAgent extends Agent {
	private MeetingAgentGui myGui;
	private boolean schedulingMeeting = false;
	private final String agentType = "meeting-agent";
	private WeekCalendar weekCalendar = new WeekCalendar();
	ArrayList<int[]> availableSlots= weekCalendar.listAllAvailableSlots();;
	private Random random = new Random();
	private final long TIMEOUT = 10000;

	@Override
	protected void setup() {
		myGui = new MeetingAgentGui(this);
		myGui.display();

		System.out.println(getAID().getLocalName() + ": is now ready!");

		DFAgentDescription dfAgentDescription = new DFAgentDescription();
		dfAgentDescription.setName(getAID());

		ServiceDescription serviceDescription = new ServiceDescription();
		serviceDescription.setType(agentType);
		serviceDescription.setName(getAID().getLocalName());

		dfAgentDescription.addServices(serviceDescription);

		try {
			DFService.register(this, dfAgentDescription);
		} catch (FIPAException ex) {
			ex.printStackTrace();
		}

		addBehaviour(new ReceiveMeetingInvitation());
		addBehaviour(new ReceiveMeetingCancelation());
		addBehaviour(new ReceiveMeetingConfirmation());
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
		//System.out.println(weekCalendar); // DEBUG PRINT

		

		System.out.println(getAID().getLocalName() + ": is going to schedule a meeting!");
		schedulingMeeting = true;
		addBehaviour(new BookMeetingBehavior());
	}

	private class BookMeetingBehavior extends Behaviour {
		private AID[] attendees;
		private AID[] meetingAgents;
		private int[] slot;
		private int step = 0;
		private int numberOfAttendees = 0;
		private int repliesCount = 0;
		private long begin, end;
		private MessageTemplate template;
		private HashSet<int[]> proposedMeetingTimes = new HashSet<int[]>();
		

		@Override
		public void action() {
			if (schedulingMeeting) {
				switch (step) {
					case 0:
						// Select Attendees and meeting slot
						System.out.println(myAgent.getLocalName() + ": is looking of other agents...");

						DFAgentDescription dfTemplate = new DFAgentDescription();
						ServiceDescription serviceDescription = new ServiceDescription();

						serviceDescription.setType(agentType);

						dfTemplate.addServices(serviceDescription);

						try {
							DFAgentDescription[] result = DFService.search(myAgent, dfTemplate);
							
							System.out.println(getAID().getLocalName() + ": The following agents have been found:");
							meetingAgents = new AID[result.length - 1];

							int meetingAgentsIndex = 0;
							
							for (int i = 0; i < result.length; ++i) {
								if (!result[i].getName().equals(getAID().getName())) {
									meetingAgents[meetingAgentsIndex] = result[i].getName();
									System.out.println(myAgent.getLocalName() + ": found " + meetingAgents[meetingAgentsIndex++].getLocalName());
								}								
							}
						} catch (FIPAException ex) {
							ex.printStackTrace();
						}

						System.out.println(myAgent.getLocalName() + ": is deciding the attendees list for the meeting...");
						
						int maximumAttendees = meetingAgents.length;
						System.out.println(maximumAttendees);
						if (maximumAttendees > 0) numberOfAttendees = random.nextInt(maximumAttendees) + 1;

						System.out.println(getAID().getLocalName() + ": decided to invite " + numberOfAttendees + " attendees to the meeting!");

						if (numberOfAttendees != 0) {
							attendees = new AID[numberOfAttendees];

							System.out.println(getAID().getLocalName() + ": Attendee list:");
							for (int i = 0; i < numberOfAttendees; ++i) {
								attendees[i] = meetingAgents[random.nextInt(meetingAgents.length)];
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

								proposedMeetingTimes.add(slot);

								step = 1;
							}
						} else {
							step = 10; 
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

						break;
					case 2:
						// Check the reply status: if ok then proceed with booking, if not do something TODO
						ACLMessage reply = myAgent.receive(template);
						
						end = System.currentTimeMillis();
						double timeElapsed = end - begin;

						if (timeElapsed > TIMEOUT) {
							sendCancelMessage(Integer.toString(slot[0]) + "," + Integer.toString(slot[1]), null);
							step = 10;
						} else {
							if (reply != null) {
								if (reply.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
									// TODO
									System.out.println(getAID().getLocalName() + ": " + reply.getSender().getLocalName() + " rejected the meeting!");

									AID rejectingAgent = reply.getSender();

									int day = slot[0], hour = slot[1];
									weekCalendar.cancelMeeting(day, hour);

									availableSlots = weekCalendar.listAllAvailableSlots();
									
									for (int[] proposedMeetingTime : proposedMeetingTimes) {
										availableSlots.remove(proposedMeetingTime);
									}

									if (availableSlots.size() > 1) slot = availableSlots.get(random.nextInt(availableSlots.size() - 1));
									else if (availableSlots.size() == 1) slot = availableSlots.get(0); // only one slot is missing
									else slot = null; // there are no more available slots
									
									if (slot == null) {
										// No more slots found
										System.out.println(getAID().getLocalName() + ": does not have any free time to held the meeting");
										step = 10;
									} else {
										day = slot[0]; hour = slot[1];
										String weekDay = WeekCalendar.getWeekDayName(day);

										System.out.println(getAID().getLocalName() + ": found new time to held the meeting: " + weekDay + " at " + hour + "H00");

										step = 1;
									}
									
									System.out.println(getAID().getLocalName() + ": is notifying all the attendees that meeting schedule will be changed");
									
									sendCancelMessage(Integer.toString(slot[0]) + "," + Integer.toString(slot[1]), rejectingAgent);

								} else {
									System.out.println(getAID().getLocalName() + ": " + reply.getSender().getLocalName() + " aceepted the meeting!");
									repliesCount++;
									if (repliesCount >= attendees.length) {
										step = 3;
									}
								}
							} else {
								block(TIMEOUT);
							}
						}
						break;
					case 3:
						// Send meeting confirmation
						System.out.println(getAID().getLocalName() + ": is sending confimation messages...");

						begin = System.currentTimeMillis();

						ACLMessage confirmationMessage = new ACLMessage(ACLMessage.CONFIRM);
						for (int i = 0; i < numberOfAttendees; ++i) {
							confirmationMessage.addReceiver(attendees[i]);
						}

						confirmationMessage.setContent(Integer.toString(slot[0]) + "," + Integer.toString(slot[1]));
						confirmationMessage.setConversationId("schedule-meeting");
						confirmationMessage.setReplyWith("confirm" + System.currentTimeMillis());
	
						myAgent.send(confirmationMessage);

						System.out.println(getAID().getLocalName() + ": has sent all confimation messages");

						template = MessageTemplate.and(MessageTemplate.MatchConversationId("schedule-meeting"),
							MessageTemplate.MatchInReplyTo(confirmationMessage.getReplyWith()));
						step = 4;

					case 4:
						// Receive acknowledgements
						ACLMessage acknowledgeMessage = myAgent.receive(template);
						
						end = System.currentTimeMillis();
						timeElapsed = end - begin;

						if (timeElapsed > TIMEOUT) {
							sendCancelMessage(Integer.toString(slot[0]) + "," + Integer.toString(slot[1]), null);
							step = 10;
						} else {
							if (acknowledgeMessage != null) {
								if (acknowledgeMessage.getPerformative() == ACLMessage.AGREE) {
									AID agent = acknowledgeMessage.getSender();

									System.out.println(getAID().getLocalName() + ": received acknowledgment from " + agent.getLocalName());

									repliesCount++;
									if (repliesCount >= attendees.length) {
										step = 5;
									}
								}
							} else {
								block(TIMEOUT);
							}
						}
						break;
					case 5:
						// Finish!
						int day = slot[0], hour = slot[1];
						String weekDay = WeekCalendar.getWeekDayName(day);

						System.out.println(getAID().getLocalName() + ": Meeting scheduled on " + weekDay + " at " + hour + "H00");
					default:
						System.out.println(getAID().getLocalName() + ": Error when scheduling the meeting!");
						step = 10;
						break;

				}
			}
		}

		@Override
		public boolean done() {
			if (step == 10 || step == 5) {
				schedulingMeeting = false;
				return true;
			} else {
				return false;
			}
		}

		private void sendCancelMessage(String content, AID rejectingAgent) {
			System.out.println(getAID().getLocalName() + ": is sending cancelation messages...");
			ACLMessage cancelationMessage = new ACLMessage(ACLMessage.CANCEL);

			for (int i = 0; i < numberOfAttendees; ++i) {
				if (rejectingAgent != null) {
					if (!rejectingAgent.equals(attendees[i])) cancelationMessage.addReceiver(attendees[i]);
				}
			}

			cancelationMessage.setContent(content);
			cancelationMessage.setConversationId("schedule-meeting");
			cancelationMessage.setReplyWith("cancel" + System.currentTimeMillis());

			myAgent.send(cancelationMessage);

			System.out.println(getAID().getLocalName() + ": has sent all cancelation messages!");
		}
	}

	private class ReceiveMeetingInvitation extends CyclicBehaviour  {
		@Override
		public void action() {
			MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage message = myAgent.receive(template);
			
			if (message != null) {
				AID invitingAgent = message.getSender();

				System.out.println(getAID().getLocalName() + ": received an invitation from " + invitingAgent.getLocalName() + ". Reading...");
				
				String messageContent = message.getContent();
				String[] splitedStrings = messageContent.split(",");

				int day, hour;
				day = Integer.parseInt(splitedStrings[0]);
				hour = Integer.parseInt(splitedStrings[1]);

				String weekDay = WeekCalendar.getWeekDayName(day);

				System.out.println(getAID().getLocalName() + ": Invitation for a meeting on " + weekDay + " at " +  hour + "H00. Checking availability...");

				if (weekCalendar.isAvailable(day, hour)) {
					System.out.println(getAID().getLocalName() + ": is available for the meeting! Waiting for confirmation...");

					weekCalendar.scheduleMeeting(day, hour);

					ACLMessage acceptMessage = message.createReply();

					acceptMessage.setContent(messageContent);
					acceptMessage.setPerformative(ACLMessage.INFORM);

					myAgent.send(acceptMessage);

					System.out.println(getAID().getLocalName() + ": Aceptance message sent! Waiting for confirmation...");

				} else {
					System.out.println(getAID().getLocalName() + ": is not available for the meeting! Rejecting the invitation...");

					ACLMessage rejectMessage = message.createReply();
					
					rejectMessage.setContent(messageContent);
					rejectMessage.setPerformative(ACLMessage.REJECT_PROPOSAL);

					myAgent.send(rejectMessage);

					System.out.println(getAID().getLocalName() + ": Rejection message sent! Wainting for new proposals...");

				}
			} else {
				block();
			}
		}
	}

	private class ReceiveMeetingCancelation extends CyclicBehaviour {
		@Override
		public void action() {
			MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
			ACLMessage message = myAgent.receive(template);

			if (message != null) {
				AID cancelingAgent = message.getSender();

				String messageContent = message.getContent();
				String[] splitedStrings = messageContent.split(",");

				int day, hour;
				day = Integer.parseInt(splitedStrings[0]);
				hour = Integer.parseInt(splitedStrings[1]);
				int[] meetingTime = {day, hour};

				String weekDay = WeekCalendar.getWeekDayName(day);

				System.out.println(getAID().getLocalName() + ": received a cancelation from " + cancelingAgent.getLocalName()  + " for a meeting on " + weekDay + "at " + hour + "H00");

				weekCalendar.cancelMeeting(day, hour);

				System.out.println(getAID().getLocalName() + ": meeting on " + weekDay + " at " + hour + "H00 has been cancelled");
			} else {
				block(TIMEOUT);
			}
		}
	}

	private class ReceiveMeetingConfirmation extends CyclicBehaviour {
		@Override
		public void action() {
			MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
			ACLMessage message = myAgent.receive(template);

			if (message != null) {
				ACLMessage acknowledgeMessage = message.createReply();
				String messageContent = message.getContent();
				String[] splitedStrings = messageContent.split(",");

				int day, hour;
				day = Integer.parseInt(splitedStrings[0]);
				hour = Integer.parseInt(splitedStrings[1]);

				System.out.println(getAID().getLocalName() + ": received the confirmation for the meeting on " + WeekCalendar.getWeekDayName(day) + " at " +  hour + "H00. Sending Acknowledge...");

				acknowledgeMessage.setContent(messageContent);
				acknowledgeMessage.setPerformative(ACLMessage.AGREE);
				
				myAgent.send(acknowledgeMessage);

				System.out.println(getAID().getLocalName() + ": acknowledge sent!");
			} else {
				block(TIMEOUT);
			}
		}
	}
		
}