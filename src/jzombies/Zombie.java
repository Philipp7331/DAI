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
public class Zombie {

	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public boolean initiator;
	public GridPoint nextGoal;
	public static ArrayList<Zombie> zombieList = new ArrayList<Zombie>();
	public static ArrayList<Human> humanList;
	public static MessageCenter mc = new MessageCenter();
	private int id;
	private Human nextCustomer;

	public Zombie(ContinuousSpace<Object> space, Grid<Object> grid, boolean initiator, ArrayList<Human> humanList,
			int id) {
		this.space = space;
		this.grid = grid;
		this.initiator = initiator;
		Zombie.zombieList.add(this);
		Zombie.humanList = humanList;
		this.id = id;
		this.nextGoal = null;
		this.nextCustomer = null;
	}

	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		// if this is the initiator and nobody has a goal left: CFP for all customers
		if (this.initiator) {
			callForProposal();
		}

		while (mc.messagesAvailable(this.id)) {
			FIPA_Message msg = mc.getMessage(this.id);

			// if we get a CFP we propose for all customers
			if (msg.getPerformative().equals(FIPA_Performative.CFP.toString())) {
				mc.addMessage(new FIPA_Message(this.id, msg.getSender(), FIPA_Performative.PROPOSE,
						grid.getLocation(this).toString()));
			}
			
			// if initiator accepts the proposal set the goal and remove customer
			else if (msg.getPerformative().equals(FIPA_Performative.ACCEPT_PROPOSAL.toString())) {
				this.nextGoal = grid.getLocation(getHumanById(Integer.parseInt(msg.getContent())));
				this.nextCustomer = getHumanById(Integer.parseInt(msg.getContent()));
			}

		}

		// TODO move towards goal if reached inform-done
		if (nextGoal != null) {
			moveTowards(nextGoal, nextCustomer); // nextGoal = null;
		}
		
		System.out.println("start of tick");
		for(Zombie zombie: zombieList) {
			System.out.println(zombie.nextGoal);
			System.out.println(zombie.id);
		}
		System.out.println("end of tick");
		
		System.out.println(" ");

	}

	public void callForProposal() {
		// CFP if nobody has a goal left
		if (nextGoalAllNull(zombieList)) {
			for (Zombie zombie : zombieList) {
				mc.addMessage(new FIPA_Message(this.id, zombie.id, FIPA_Performative.CFP,
						"send me the coordinates of your current location"));
			}
		}

		// System.out.println(humanList.size());
		// accept proposal of messenger closest to the customer
		for (Human human : humanList) {
			Zombie closestZombie = findClosestZombie(human);
			// System.out.println(" ID: " + human.getId());
			mc.addMessage(new FIPA_Message(this.id, closestZombie.id, FIPA_Performative.ACCEPT_PROPOSAL,
					String.valueOf(human.getId())));

			for (Zombie zombie : zombieList) {
				if (zombie != closestZombie) {
					mc.addMessage(new FIPA_Message(this.id, zombie.id, FIPA_Performative.REJECT_PROPOSAL,
							String.valueOf(human.getId())));
				}
			}
		}

	}

	public boolean nextGoalAllNull(ArrayList<Zombie> zombieList) {
		for (Zombie zombie : zombieList) {
			if (zombie.nextGoal != null)
				return false;
		}
		return true;
	}

	public void moveTowards(GridPoint pt, Human customer) {
		// only move if we are not already in this grid location
		if (!pt.equals(grid.getLocation(this))) {
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
			space.moveByVector(this, 1, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this, (int) Math.round(myPoint.getX()), (int) Math.round(myPoint.getY()));
		} else {
			mc.addMessage(new FIPA_Message(this.id, zombieList.get(0).id, FIPA_Performative.INFORM_DONE, "Finished"));
			if (!humanList.isEmpty()) {
				humanList.remove(customer);
			}
			this.nextGoal = null;
		}
	}

	public Human getHumanById(int id) {
		for (Human human : humanList) {
			if (human.getId() == id)
				return human;
		}
		return null;
	}

	public Zombie findClosestZombie(Human human) {
		Zombie closestZombie = zombieList.get(0);
		for (Zombie zombie : zombieList) {
			if (space.getDistance(space.getLocation(zombie), space.getLocation(human)) < space
					.getDistance(space.getLocation(closestZombie), space.getLocation(human))) {
				closestZombie = zombie;
			}
		}
		return closestZombie;
	}
}