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

/**
 * @author Philipp Flügger 1361053, Patrick Mertes 1368734, Nhat Tran 1373869
 *
 */
public class Human {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	// save goals in an arraylist
	private ArrayList<GridPoint> goals = new ArrayList<GridPoint>();
	
	public Human(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
		// initialize goals (also possible with parameters)
		this.goals.add(new GridPoint(10, 15));
		this.goals.add(new GridPoint(30, 45));
		this.goals.add(new GridPoint(10, 35));
		this.goals.add(new GridPoint(5, 5));
	}
	
	// add a ScheduledMethod to the run method of the human instead of the watcher
	@ScheduledMethod(start = 1, interval = 1)
	public void run() {
		// if we have goals left we move towards them and remove if reached
		if (!goals.isEmpty()) {
			moveTowards(goals.get(0));
			if (goals.get(0).equals(grid.getLocation(this))) {
				goals.remove(0);
			}
		} else {
			System.out.println("DONE");
		}
	}
	
	public void moveTowards(GridPoint pt) {
		// only move if we are not already in this grid location
		if (!pt.equals(grid.getLocation(this))) {
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
			space.moveByVector(this, 1, angle, 0);
			myPoint = space.getLocation(this);
			// we could Math.round values to avoid "jumping over" goals:
			grid.moveTo(this, (int)myPoint.getX(), (int)myPoint.getY()); 
		}
	}
}
