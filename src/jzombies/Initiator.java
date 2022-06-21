/**
 * 
 */
package jzombies;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;
import zmq.Msg;

/**
 * @author Philipp Flügger 1361053, Patrick Mertes 1368734, Nhat Tran 1373869
 *
 */
public class Initiator {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private ArrayList<Messenger> messengerList;
	private ArrayList<Customer> customerList;
	private ArrayList<Boolean> customerDelivered;
	private MessageCenter mc;
	private int id;
	private boolean cfpSent = false;
	public int jobsFinished = 0;
	public int daysToSimulate;
	public int simulatedDays;
	
	public Initiator(ContinuousSpace<Object> space, Grid<Object> grid, ArrayList<Messenger> messengerList, 
			ArrayList<Customer> customerList, MessageCenter mc, int id, int daysToSimulate) {
		this.space = space;
		this.grid = grid;
		this.messengerList = messengerList;
		this.customerList = customerList;
		this.customerDelivered = new ArrayList<>();
		for (int i = 0; i < customerList.size(); i++) {
			customerDelivered.add(false);
		}
		this.mc = mc;
		this.id = id;
		this.daysToSimulate = daysToSimulate;
		this.simulatedDays = 0;
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		
		// if all customers received a delivery reset messengers (and simulate more days)
		if (!customerDelivered.contains(false)) {
			simulatedDays++;
			System.out.println("Day " + simulatedDays + " finished!");
			if (simulatedDays == daysToSimulate) {
				System.exit(0);
			}
			for (Messenger messenger : messengerList) {
				messenger.resetPosition();
			}
			for (int i = 0; i < customerDelivered.size(); i++) {
				customerDelivered.set(i, false);
			}
		}

		if (Messenger.ongoingJobs == 0 && !cfpSent) {			
			cfpSent = true;
			for (Messenger messenger : messengerList) {
				for (Customer customer : customerList) {
					if (!customerDelivered.get(customer.getId())) {
						mc.send(this.id, 
								messenger.id, 
								customer.getId(),
								FIPA_Performative.CFP,
								Integer.toString(customer.getId()));
					}
				}
			}
		}
		
		if (!nextGoalAllNull() && cfpSent) {
			cfpSent = false;
		}
		
		// accept proposal of closest messenger (reject others)
		ArrayList<ArrayList<FIPA_Message>> allProposals = getAllProposals();
		int i = 0;
		for (ArrayList<FIPA_Message> customerProposals : allProposals) {
			FIPA_Message msg = findCheapestProposal(customerProposals);
			for (Messenger messenger : messengerList) {
				if (msg.getSender() == messenger.id) {
					mc.send(this.id, msg.getSender(), i, FIPA_Performative.ACCEPT_PROPOSAL, "");
				} else {
					mc.send(this.id, messenger.id, i, FIPA_Performative.REJECT_PROPOSAL, "");
				}
			}
			i++;
		}
		
		// adjust trust factor for failure/done
		ArrayList<FIPA_Message> removeList = new ArrayList<>();
		for (FIPA_Message msg : mc.messageList) {
			if (msg.getReceiver() == this.id) {
				Messenger sender = messengerList.get(msg.getSender());
				if (msg.getPerformative().equals(FIPA_Performative.INFORM_DONE.toString())) {
					Messenger.ongoingJobs--;
					jobsFinished++;
					sender.succesfulDeliveries++;
					customerDelivered.set(msg.getSubject(), true);
					removeList.add(msg);
				}
				if (msg.getPerformative().equals(FIPA_Performative.FAILURE.toString())) {
					Messenger.ongoingJobs--;
					jobsFinished++;
					sender.unsuccesfulDeliveries++;
					customerDelivered.set(msg.getSubject(), true);
					removeList.add(msg);
				}
			}
		}
		
		for (FIPA_Message msg : removeList) {
			mc.messageList.remove(msg);
		}
		
	}
	
	public boolean nextGoalAllNull() {
		for (Messenger messenger : messengerList) {
			if (!messenger.nextGoalNull()) {
				return false;
			} 
		}
		return true;
	}
	
	// gets all proposals from the message center
	public ArrayList<ArrayList<FIPA_Message>> getAllProposals() {
		ArrayList<ArrayList<FIPA_Message>> allProposals = new ArrayList<>();
		for (int i = 0; i < customerList.size(); i++) {
			ArrayList<FIPA_Message> customerProposals = new ArrayList<>();
			for (FIPA_Message msg : mc.messageList) {
				if (msg.getPerformative().equals(FIPA_Performative.PROPOSE.toString()) 
						&& msg.getReceiver() == this.id && msg.getSubject() == i) {
					customerProposals.add(msg);
				}
			}
			allProposals.add(customerProposals);
			for (FIPA_Message msg : customerProposals) {
				mc.messageList.remove(msg);
			}
		}
		return allProposals;
	}
	
	
	public FIPA_Message findCheapestProposal(ArrayList<FIPA_Message> customerProposals) {
		FIPA_Message cheapestProposal = new FIPA_Message(999, 999, 999, FIPA_Performative.PROPOSE, String.valueOf(Integer.MAX_VALUE));
		for (FIPA_Message msg : customerProposals) {
			Double trustAdjustedPrice;
			if (getTrustFactor(messengerList.get(msg.getSender())) == 1) {
				trustAdjustedPrice = Double.parseDouble(msg.getContent()) * (1 - 0.99);
			} else {
				trustAdjustedPrice = Double.parseDouble(msg.getContent()) * (1 - getTrustFactor(messengerList.get(msg.getSender())));
			}
			if (trustAdjustedPrice < Double.parseDouble(cheapestProposal.getContent())) {
				cheapestProposal = msg;
			}
		}
		return cheapestProposal;
	}
	
	
	public Double getTrustFactor(Messenger messenger) {
		if (messenger.succesfulDeliveries != 0 || messenger.unsuccesfulDeliveries != 0) {
			return ((messenger.succesfulDeliveries - messenger.unsuccesfulDeliveries) / (messenger.succesfulDeliveries + messenger.unsuccesfulDeliveries));
		} else {
			return 0.0;
		}
	}
	
	public Customer getCustomerById(int id) {
		for (Customer customer : customerList) {
			if (customer.getId() == id)
				return customer;
		}
		return null;
	}
	
}