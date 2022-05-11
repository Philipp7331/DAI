/**
 * 
 */
package jzombies;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

/**
 * @author Philipp Flügger 1361053, Patrick Mertes 1368734, Nhat Tran 1373869
 *
 */
public class Messenger {

	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public boolean initiator;
	public GridPoint nextGoal;
	private Customer nextCustomer;
	public static ArrayList<Messenger> messengerList = new ArrayList<Messenger>();
	public static ArrayList<Customer> customerList;
	public static MessageCenter mc = new MessageCenter();
	private int id;

	public Messenger(ContinuousSpace<Object> space, Grid<Object> grid, boolean initiator, ArrayList<Customer> customerList, int id) {
		this.space = space;
		this.grid = grid;
		this.initiator = initiator;
		this.nextGoal = null;
		this.nextCustomer = null;
		Messenger.messengerList.add(this);
		Messenger.customerList = customerList;
		this.id = id;
	}

	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		
		if (this.initiator) {
			handleInitiatorTasks();
		}

		// check messages for CFP or ACCEPT_PROPOSAL
		while (mc.messagesAvailable(this.id)) {
			FIPA_Message msg = mc.getMessage(this.id);
			// if we get a CFP we propose with our current location
			if (msg.getPerformative().equals(FIPA_Performative.CFP.toString())) {
				mc.addMessage(new FIPA_Message(this.id, 
											   msg.getSender(), 
											   FIPA_Performative.PROPOSE,
											   grid.getLocation(this).toString()));
			}
			// if initiator accepts the proposal set the goal
			else if (msg.getPerformative().equals(FIPA_Performative.ACCEPT_PROPOSAL.toString())) {
				this.nextGoal = grid.getLocation(getCustomerById(Integer.parseInt(msg.getContent())));
				this.nextCustomer = getCustomerById(Integer.parseInt(msg.getContent()));
			}
		}
		
		// move towards the next customer
		if (nextGoal != null) {
			moveTowards(nextGoal, nextCustomer);
		}
	}

	public void handleInitiatorTasks() {
		// CFP if nobody has a goal left
		if (nextGoalAllNull(messengerList)) {
			for (Messenger messenger : messengerList) {
				mc.addMessage(new FIPA_Message(this.id, 
											   messenger.id, 
											   FIPA_Performative.CFP,
											   "Send me the coordinates of your current location!"));
			}
		}

		// accept proposal of messenger closest to the customer
		for (Messenger messenger : messengerList) {
			//Messenger closestMessenger = findClosestMessenger(customer);
			Customer closestCustomer = findClosestCustomer(messenger);
			if (closestCustomer != null) {
				if (findClosestMessenger(closestCustomer).equals(messenger)) {
					mc.addMessage(new FIPA_Message(this.id, 
							   messenger.id, 
							   FIPA_Performative.ACCEPT_PROPOSAL,
							   String.valueOf(closestCustomer.getId())));
				} else {
					mc.addMessage(new FIPA_Message(this.id, 
							   					   messenger.id, 
							   					   FIPA_Performative.REJECT_PROPOSAL,
							   					   String.valueOf(closestCustomer.getId())));
				}
			}
		}
	}

	// checks if messengers all are idle (no nextGoal)
	public boolean nextGoalAllNull(ArrayList<Messenger> messengerList) {
		for (Messenger messenger : messengerList) {
			if (messenger.nextGoal != null)
				return false;
		}
		return true;
	}

	public void moveTowards(GridPoint pt, Customer customer) {
		// only move if we are not already in this grid location
		if (!pt.equals(grid.getLocation(this))) {
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
			space.moveByVector(this, 1, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this, (int) Math.round(myPoint.getX()), (int) Math.round(myPoint.getY()));
		} else {
			mc.addMessage(new FIPA_Message(this.id, 
										   messengerList.get(0).id, 
										   FIPA_Performative.INFORM_DONE, 
										   "Finished!"));
			if (!customerList.isEmpty()) customerList.remove(customer);
			
			this.nextGoal = null;
		}
	}

	public Customer getCustomerById(int id) {
		for (Customer customer : customerList) {
			if (customer.getId() == id)
				return customer;
		}
		return null;
	}

	// returns the closest messenger using the getDistance method from current space
	public Messenger findClosestMessenger(Customer customer) {
		if (customer != null) {
			Messenger closestMessenger = messengerList.get(0);
			for (Messenger messenger : messengerList) {
				if (space.getDistance(space.getLocation(messenger), space.getLocation(customer)) < space
						.getDistance(space.getLocation(closestMessenger), space.getLocation(customer))) {
					closestMessenger = messenger;
				}
			}
			return closestMessenger;
		}
		return null;
	}
	
	public Customer findClosestCustomer(Messenger messenger) {
		if (!customerList.isEmpty()) {
			Customer closestCustomer = customerList.get(0);
			for (Customer customer : customerList) {
				if (space.getDistance(space.getLocation(customer), space.getLocation(messenger)) < space
						.getDistance(space.getLocation(closestCustomer), space.getLocation(messenger))) {
					closestCustomer = customer;
				}
			}
			return closestCustomer;
		}
		return null;
	}

	
}