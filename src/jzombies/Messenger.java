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
	public GridPoint nextGoal;
	private Customer nextCustomer;
	public static ArrayList<Messenger> messengerList = new ArrayList<Messenger>();
	public static ArrayList<Customer> customerList;
	public MessageCenter mc;
	public int id;
	public Double succesfulDeliveries;
	public Double unsuccesfulDeliveries;
	public Double deliveryProbability;

	public Messenger(ContinuousSpace<Object> space, Grid<Object> grid, ArrayList<Customer> customerList, 
			int id, MessageCenter mc, Double deliveryProbability) {
		this.space = space;
		this.grid = grid;
		this.nextGoal = null;
		this.nextCustomer = null;
		Messenger.messengerList.add(this);
		Messenger.customerList = customerList;
		this.id = id;
		this.mc = mc;
		this.succesfulDeliveries = 0.0;
		this.unsuccesfulDeliveries = 0.0;
		this.deliveryProbability = deliveryProbability;
	}

	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		System.out.println(nextGoal);
		
		// check messages for CFP or ACCEPT_PROPOSAL
		ArrayList<Customer> potentialNextGoals = new ArrayList<>();
		while (mc.messagesAvailable(this.id)) {
			FIPA_Message msg = mc.getMessage(this.id);
			
			// if we get a CFP we propose with distance to customer
			if (msg.getPerformative().equals(FIPA_Performative.CFP.toString())) {
				Double distanceToCustomer = space.getDistance(space.getLocation(this), 
						space.getLocation(getCustomerById(msg.getSubject())));
				mc.send(this.id, 
						msg.getSender(), 
						msg.getSubject(),
						FIPA_Performative.PROPOSE,
						String.valueOf(distanceToCustomer));
				System.out.println("I PROPOSED!");
			}
			
			// if initiator accepts one or more proposals set the goal for closest one
			else if (msg.getPerformative().equals(FIPA_Performative.ACCEPT_PROPOSAL.toString())) {
				potentialNextGoals.add(getCustomerById(msg.getSubject()));
				if (this.id == 1) {
					System.out.println("Goals of 1: " + potentialNextGoals.size());
				}
			}
		}
		
		if (nextGoal == null && nextCustomer == null 
				&& potentialNextGoals != null) {
			if (potentialNextGoals.size() == 1) {
				this.nextGoal = grid.getLocation(potentialNextGoals.get(0));
				this.nextCustomer = potentialNextGoals.get(0);
				potentialNextGoals = null;
			} else {
				Customer closestCustomer = getClosestCustomer(potentialNextGoals);
				this.nextGoal = grid.getLocation(closestCustomer);
				this.nextCustomer = closestCustomer;
				potentialNextGoals = null;
			}
		}
						
		// move towards the next customer
		if (nextGoal != null) {
			moveTowards(nextGoal, nextCustomer);
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
			// either inform-done or failure
			System.out.println("AT GOAL");
			if (Math.random() < this.deliveryProbability) {
				mc.send(this.id, 1337, nextCustomer.getId(), FIPA_Performative.INFORM_DONE, "");
			} else {
				mc.send(this.id, 1337, nextCustomer.getId(), FIPA_Performative.FAILURE, "");
			}
			if (!customerList.isEmpty()) customerList.remove(customer);
			
			this.nextGoal = null;
			this.nextCustomer = null;
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
	
	public boolean nextGoalNull() {
		return nextGoal == null;
	}
	
	public Customer getClosestCustomer(ArrayList<Customer> potentialNextGoals) {
		if (!potentialNextGoals.isEmpty()) {
			/*System.out.println("METHOD CALLED BY: " + this.id);
			System.out.println("potentialNextGoals size: " + potentialNextGoals.size());
			System.out.println(potentialNextGoals);
			System.out.println("first elem: " + potentialNextGoals.get(0));*/
			Customer closestCustomer = potentialNextGoals.get(0);
			for (Customer customer : potentialNextGoals) {
				/*System.out.println("Customer: " + customer);
				System.out.println("Closest Customer: " + closestCustomer);*/
				if (space.getDistance(space.getLocation(this), space.getLocation(customer))
						< space.getDistance(space.getLocation(this), space.getLocation(closestCustomer))) {
					closestCustomer = customer;
				}
			}
			return closestCustomer;
		}
		return null;
	}

	
}