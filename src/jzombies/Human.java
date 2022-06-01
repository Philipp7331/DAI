/**
 * 
 */
package jzombies;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialException;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;

/**
 * @author Philipp
 *
 */
public class Human {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int energy, startingEnergy;
	private int carriedMaterial;
	public static ArrayList<Trap> traps = new ArrayList<Trap>();
	
	public Human(ContinuousSpace<Object> space, Grid<Object> grid, int energy) {
		this.space = space;
		this.grid = grid;
		this.energy = startingEnergy = energy;
		this.carriedMaterial = 0;
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void run() {
		Context context = repast.simphony.util.ContextUtils.getContext(this);
		
		// watch for zombies in the neighborhood
		GridPoint pt = grid.getLocation(this);
		GridCellNgh<Zombie> nghCreator = new GridCellNgh<Zombie>(grid, pt, Zombie.class, 4, 4);
		List<GridCell<Zombie>> gridCells = nghCreator.getNeighborhood(true);
		SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
		
		GridPoint pointWithLeastZombies = null;
		int minCount = Integer.MAX_VALUE;
		boolean zombieInNgh = false;
		for (GridCell<Zombie> cell : gridCells) {
			if (cell.size() < minCount) {
				pointWithLeastZombies = cell.getPoint();
				minCount = cell.size();
			}
			if (cell.size() > 0) {
				zombieInNgh = true;
			}
		}
		
		// watch for material in the neighborhood
		GridCellNgh<Material> nghCreatorMaterial = new GridCellNgh<Material>(grid, pt, Material.class, 4, 4);
		List<GridCell<Material>> gridCellsMaterial = nghCreatorMaterial.getNeighborhood(true);
		SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
		
		Material closestMaterial = null;
		for (GridCell<Material> cell : gridCellsMaterial) {
			Iterable<Material> agentsAtCell = cell.items();
			if (agentsAtCell.iterator().hasNext()) {
				closestMaterial = agentsAtCell.iterator().next();
				for (Material material : agentsAtCell) {
					if (space.getDistance(space.getLocation(material), space.getLocation(this)) 
						< space.getDistance(space.getLocation(closestMaterial), space.getLocation(this))) {
						closestMaterial = material;
					}
				}
			}
		}
		GridPoint pointWithMaterial = grid.getLocation(closestMaterial);
		
		// execute next action based on the presence of zombies
		if (zombieInNgh && energy > 0) {
			moveTowards(pointWithLeastZombies);
		} else if (carriedMaterial == 3) {
			buildTrap(context);
		} else if (pointWithMaterial != null && carriedMaterial != 3 && !pointWithMaterial.equals(grid.getLocation(this)) && energy > 0) {
			moveTowards(pointWithMaterial);
		} else if (pointWithMaterial != null && pointWithMaterial.equals(grid.getLocation(this))) {
			carriedMaterial++;
			context.remove(closestMaterial);
		} else if (pointWithMaterial == null) {
			int x = RandomHelper.nextIntFromTo(0,49);
			int y = RandomHelper.nextIntFromTo(0,49);
			moveTowards(new GridPoint(x,y));
		} else {
			energy = startingEnergy;
		}	
	}
	
	
	public void buildTrap(Context<Object> context) {
		GridPoint pt = grid.getLocation(this);
		Trap trap = new Trap(space, grid);
		carriedMaterial = 0;
		context.add(trap);
		traps.add(trap);
		grid.moveTo(trap, pt.getX(), pt.getY());
	}
	
	
	public void moveTowards(GridPoint pt) {
		// only move if we are not already in this grid location
		try {
			if (!pt.equals(grid.getLocation(this))) {
				NdPoint myPoint = space.getLocation(this);
				NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
				double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
				space.moveByVector(this, 2, angle, 0);
				myPoint = space.getLocation(this);
				grid.moveTo(this, (int)myPoint.getX(), (int)myPoint.getY());
				energy--;
			}

		} catch (SpatialException e) {
			return;
		}
	}
}
