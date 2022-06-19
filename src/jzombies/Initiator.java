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
	
	public Initiator(ContinuousSpace<Object> space, Grid<Object> grid, ArrayList<Messenger> messengerList, 
			ArrayList<Customer> customerList, MessageCenter mc, int id) {
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
	}
	
	// TODO change trust discount & second round doesn't start (at same time) & multiple days
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		System.out.println("MESSAGE LIST: ");
		mc.messageListToString();

						
		if (nextGoalAllNull() && !cfpSent && !customerList.isEmpty()) {
			cfpSent = true;
			System.out.println("-------------------------SENT CFP--------------------------");
			// send CFP if goals all null
			System.out.println("ml size: " + messengerList.size());
			System.out.println("cl size: " + customerList.size());
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
		
		// accept proposal of closest messenger
		ArrayList<ArrayList<FIPA_Message>> allProposals = getAllProposals();
		int i = 0;
		/*
		int numberOfProposals = 0;
		for (ArrayList<FIPA_Message> customerProposals : allProposals) {
			numberOfProposals += customerProposals.size();
		}
		System.out.println("Number of proposals: " + numberOfProposals);
		*/
		System.out.println("SIZE allProposals: " + allProposals.size());
		for (ArrayList<FIPA_Message> customerProposals : allProposals) {
			//System.out.println("SIZE customerProposals: " + customerProposals.size());
			FIPA_Message msg = findCheapestProposal(customerProposals);
			//System.out.println("Cheapest messenger: " + msg.getSender());
			//System.out.println("Cheapest Proposal: " + msg.getContent());
			for (Messenger messenger : messengerList) {
				//System.out.println(messenger.id);
				if (msg.getSender() == messenger.id) {
					mc.send(this.id, msg.getSender(), i, FIPA_Performative.ACCEPT_PROPOSAL, "");
					System.out.println("ACCEPT PROPOSAL");
				} else {
					mc.send(this.id, messenger.id, i, FIPA_Performative.REJECT_PROPOSAL, "");
					//System.out.println("REJECT PROPOSAL");
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
					jobsFinished++;
					sender.succesfulDeliveries++;
					customerDelivered.set(msg.getSubject(), true);
					//customerList.remove(getCustomerById(msg.getSubject()));
					removeList.add(msg);
				}
				if (msg.getPerformative().equals(FIPA_Performative.FAILURE.toString())) {
					jobsFinished++;
					sender.unsuccesfulDeliveries++;
					customerDelivered.set(msg.getSubject(), true);
					//customerList.remove(getCustomerById(msg.getSubject()));
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
	
	public ArrayList<ArrayList<FIPA_Message>> getAllProposals() {
		ArrayList<ArrayList<FIPA_Message>> allProposals = new ArrayList<>();
		for (int i = 0; i < customerList.size(); i++) {
			ArrayList<FIPA_Message> customerProposals = new ArrayList<>();
			for (FIPA_Message msg : mc.messageList) {
				if (msg.getPerformative().equals(FIPA_Performative.PROPOSE.toString()) 
						&& msg.getReceiver() == this.id && msg.getSubject() == i) {
					customerProposals.add(msg);
					System.out.println("GOT PROPOSAL!");
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
		//System.out.println("PROPOSAL SIZE: " + customerProposals.size());
		for (FIPA_Message msg : customerProposals) {
			Double trustAdjustedPrice;
			if (getTrustFactor(messengerList.get(msg.getSender())) == 1) {
				trustAdjustedPrice = Double.parseDouble(msg.getContent()) * (1 - 0.99);
			} else {
				trustAdjustedPrice = Double.parseDouble(msg.getContent()) * (1 - getTrustFactor(messengerList.get(msg.getSender())));
			}
			System.out.println("Trust adjusted price: " + trustAdjustedPrice);
			//TODO 2nd round doesn't start with trustAdjustedPrice (only with normal price)
			//trustAdjustedPrice = Double.parseDouble(msg.getContent());
			//
			System.out.println("PROPOSED PRICE: " + trustAdjustedPrice);
			if (trustAdjustedPrice < Double.parseDouble(cheapestProposal.getContent())) {
				cheapestProposal = msg;
			}
		}
		System.out.println("---------------- cheapestProposal: ---------------------");
		System.out.println(cheapestProposal.toString());
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