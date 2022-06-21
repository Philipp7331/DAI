/**
 * 
 */
package jzombies;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
	public static int ongoingJobs = 0;
	public int initialX;
	public int initialY;

	public Messenger(ContinuousSpace<Object> space, Grid<Object> grid, ArrayList<Customer> customerList, 
			int id, MessageCenter mc, Double deliveryProbability, int initialX, int initialY) {
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
		this.initialX = initialX;
		this.initialY = initialY;
	}

	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		
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
			}
			
			// if initiator accepts one or more proposals set the goal for closest one
			else if (msg.getPerformative().equals(FIPA_Performative.ACCEPT_PROPOSAL.toString())) {
				potentialNextGoals.add(getCustomerById(msg.getSubject()));
			}
		}
		
		if (nextGoal == null && nextCustomer == null 
				&& potentialNextGoals != null) {
			if (potentialNextGoals.size() == 1) {
				this.nextGoal = grid.getLocation(potentialNextGoals.get(0));
				this.nextCustomer = potentialNextGoals.get(0);
				potentialNextGoals = null;
				ongoingJobs++;
			} else if (potentialNextGoals.size() > 1){
				Customer closestCustomer = getClosestCustomer(potentialNextGoals);
				this.nextGoal = grid.getLocation(closestCustomer);
				this.nextCustomer = closestCustomer;
				potentialNextGoals = null;
				ongoingJobs++;
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
			Random generator = new Random(1337); 
			double random = generator.nextDouble();
			if (random < this.deliveryProbability) {
				mc.send(this.id, 1337, nextCustomer.getId(), FIPA_Performative.INFORM_DONE, "");
			} else {
				mc.send(this.id, 1337, nextCustomer.getId(), FIPA_Performative.FAILURE, "");
			}
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
	
	public boolean nextGoalNull() {
		return nextGoal == null;
	}
	
	public Customer getClosestCustomer(ArrayList<Customer> potentialNextGoals) {
		if (!potentialNextGoals.isEmpty()) {
			Customer closestCustomer = potentialNextGoals.get(0);
			for (Customer customer : potentialNextGoals) {
				if (space.getDistance(space.getLocation(this), space.getLocation(customer))
						< space.getDistance(space.getLocation(this), space.getLocation(closestCustomer))) {
					closestCustomer = customer;
				}
			}
			return closestCustomer;
		}
		return null;
	}
	
	public void resetPosition() {
		grid.moveTo(this, this.initialX, this.initialY);
		space.moveTo(this, this.initialX, this.initialY);
	}

	
}