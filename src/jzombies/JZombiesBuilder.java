package jzombies;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.SimpleCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;

/**
 * @author Philipp Flügger 1361053, Patrick Mertes 1368734, Nhat Tran 1373869
 *
 */

public class JZombiesBuilder implements ContextBuilder<Object> {

	@Override
	public Context build(Context<Object> context) {
		context.setId("jzombies");
		
		// replace RandomCartesianAdder with SimpleCartesianAdder
		// replace WrapAroundBorders with StrictBorders
		ContinuousSpaceFactory spaceFactory = 
				ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = 
				spaceFactory.createContinuousSpace("space", context, 
						new SimpleCartesianAdder<Object>(),
						new repast.simphony.space.continuous.StrictBorders(),
						50, 50);
		
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid("grid", context, 
				new GridBuilderParameters<Object>(new repast.simphony.space.grid.StrictBorders(),
						new SimpleGridAdder<Object>(),
						true, 50, 50));
		
		// add zombie/robot to context and initialize position
		Parameters params = RunEnvironment.getInstance().getParameters();
		int zombieCount = params.getInteger("zombie_count");
		for (int i = 0; i < zombieCount; i++) {
			Zombie zombie = new Zombie(space, grid);
			context.add(zombie);
			grid.moveTo(zombie, 4, 4);
			space.moveTo(zombie, 4, 4);
		}
		
		// add human/carrier to context and initialize position 
		int humanCount = params.getInteger("human_count");
		for (int i = 0; i < humanCount; i++) {
			Human human = new Human(space, grid);
			context.add(human);
			grid.moveTo(human, 5, 5);
			space.moveTo(human, 5, 5);
		}
		
		return context;
	}

}
