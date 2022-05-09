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
	public static ArrayList<Zombie> zombieList;
	public static ArrayList<Human> humanList;
	public static MessageCenter mc;
	private static int idCounter = 0;
	private int id;
	
	public Zombie(ContinuousSpace<Object> space, Grid<Object> grid, boolean initiator, 
			ArrayList<Human> humanList, MessageCenter mc) {
		this.space = space;
		this.grid = grid;
		this.initiator = initiator;
		Zombie.zombieList.add(this);
		Zombie.humanList = humanList;
		this.id = Zombie.idCounter++;
		this.nextGoal = null;
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		// we reuse the zombie logic to make the "robot" follow the mail carrier
		// get the grid location of this Zombie
		if (this.initiator) {
			// call for proposal
			// distribute tasks
			if (nextGoalAllNull(zombieList)) {
				for (Zombie zombie : zombieList) {
					mc.addMessage(new FIPA_Message(this.id, zombie.id, FIPA_Performative.CFP, "cfp"));
				}
			}
			// TODO either only the closest agent proposes or all propose with their distance to goal
		}
		
		for (FIPA_Message msg : mc.messageList) {
			if (msg.getPerformative() == FIPA_Performative.CFP.toString()) {
				// TODO check if agent is closest to goal --> positive proposal else refuse
			}
		}
		
		// TODO move towards goal
		/*
		if (nextGoal != null) {
			moveTowards(nextGoal);
			//nextGoal = null;
		}
		*/
		
	}
	
	public boolean nextGoalAllNull(ArrayList<Zombie> zombieList) {
		for (Zombie zombie : zombieList) {
			if (zombie.nextGoal != null) return false;
		}
		return true;
	}
	
	public void moveTowards(GridPoint pt) {
		// only move if we are not already in this grid location
		if (!pt.equals(grid.getLocation(this))) {
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
			space.moveByVector(this, 1, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this, (int)myPoint.getX(), (int)myPoint.getY());
		}
	}
}

/*
GridPoint pt = grid.getLocation(this);

// use the GridCellNgh class to create GridCells for surrounding neighborhood
// extend the search radius to the size of the grid
GridCellNgh<Human> nghCreator = new GridCellNgh<Human>(grid, pt, Human.class, 50, 50);
List<GridCell<Human>> gridCells = nghCreator.getNeighborhood(true);
SimUtilities.shuffle(gridCells, RandomHelper.getUniform());

// we don't change this logic because it works perfectly fine for one carrier and one robot
GridPoint pointWithMostHumans = null;
int maxCount = -1;
for (GridCell<Human> cell : gridCells) {
	if (cell.size() > maxCount) {
		pointWithMostHumans = cell.getPoint();
		maxCount = cell.size();
	}
}
moveTowards(pointWithMostHumans);
*/

