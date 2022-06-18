package jzombies;

import java.util.ArrayList;

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
import repast.simphony.space.grid.GridPoint;
import repast.simphony.space.grid.SimpleGridAdder;

/**
 * @author Philipp Flügger 1361053, Patrick Mertes 1368734, Nhat Tran 1373869
 *
 */

public class JZombiesBuilder implements ContextBuilder<Object> {
	
	private ArrayList<GridPoint> initialMessengerPositions = new ArrayList<GridPoint>();
	private ArrayList<GridPoint> customerPositions = new ArrayList<GridPoint>();
	
	private ArrayList<Messenger> messengerList = new ArrayList<Messenger>();
	private ArrayList<Customer> customerList = new ArrayList<Customer>();
	
	private ArrayList<Double> trustFactors = new ArrayList<Double>();
	private ArrayList<Double> deliveryProbability = new ArrayList<Double>();

	@Override
	public Context build(Context<Object> context) {
		context.setId("jzombies");
		
		initialMessengerPositions.add(new GridPoint(5, 5));
		initialMessengerPositions.add(new GridPoint(45, 5));
		initialMessengerPositions.add(new GridPoint(5, 25));
		initialMessengerPositions.add(new GridPoint(45, 45));
		
		customerPositions.add(new GridPoint(15, 10));
		customerPositions.add(new GridPoint(5, 35));
		customerPositions.add(new GridPoint(40, 10));
		customerPositions.add(new GridPoint(35, 45));
		customerPositions.add(new GridPoint(40, 35));
		customerPositions.add(new GridPoint(10, 35));
				
		deliveryProbability.add(0.89);
		deliveryProbability.add(0.74);
		deliveryProbability.add(0.94);
		deliveryProbability.add(0.71);
		
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
		
		Parameters params = RunEnvironment.getInstance().getParameters();
		
		int customerCount = params.getInteger("human_count");
		for (int i = 0; i < customerCount; i++) {
			Customer customer = new Customer(space, grid, i);
			context.add(customer);
			customerList.add(customer);
			grid.moveTo(customer, customerPositions.get(i).getX(), customerPositions.get(i).getY());
			space.moveTo(customer, customerPositions.get(i).getX(), customerPositions.get(i).getY());
		}

		MessageCenter mc = new MessageCenter();
		
		int messengerCount = params.getInteger("zombie_count");
		for (int i = 0; i < messengerCount; i++) {
			Messenger messenger = new Messenger(space, grid, customerList, i, mc, deliveryProbability.get(i));
			messengerList.add(messenger);
			context.add(messenger);
			grid.moveTo(messenger, initialMessengerPositions.get(i).getX(), initialMessengerPositions.get(i).getY());
			space.moveTo(messenger, initialMessengerPositions.get(i).getX(), initialMessengerPositions.get(i).getY());
		}
		
		Initiator initiator = new Initiator(space, grid, messengerList, customerList, mc, 1337);
		context.add(initiator);
				
		return context;
	}

}