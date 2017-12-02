package fireFighters_MAS;

import globalcounter.ExtinguishedFireCounter;
import globalcounter.FireKnowledgeUpdateCounter;
import globalcounter.ForestKnowledgeUpdateCounter;
import globalcounter.IGlobalCounter;
import globalcounter.MessageSentCounter;
import globalcounter.MsgMethodCounter;
import globalcounter.RadioMsgCounter;
import globalcounter.TaskAcceptedCounter;
import globalcounter.TaskCompleteCounter;
import globalcounter.TaskReceiveCounter;

import com.jgoodies.binding.adapter.RadioButtonAdapter;

import globalcounter.AvgMessageLength;
import globalcounter.WeatherCheckCounter;
import repast.simphony.context.Context;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedulableAction;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.grid.BouncyBorders;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.space.grid.SimpleGridAdder;

/**
 * A class used to build the simulation environment
 * 
 * @author Kirill Tumanov, 2015-2017
 */
public class WildFireBuilder implements ContextBuilder<Object> {
	ISchedulableAction addRainSchedule; // Action scheduled for the addRain method

	Context<Object> context;
	Grid<Object> grid;

	@Override
	public Context<Object> build(Context<Object> context) // Build a context of the simulation
	{
		context.setId("fireFighters_MAS");
		context = initRandom(context);
		this.context = context;
		context.add(this);
		return context;
	}

	/**
	 * Initialize simulation with random positioning of firefighters
	 * 
	 * @param context
	 *            - a context to build
	 * @return - a built context
	 */
	public Context<Object> initRandom(Context<Object> context) {
		// Get access to the user accessible parameters
		Parameters params = RunEnvironment.getInstance().getParameters();
		// Create a grid for the simulation
		int gridXsize = params.getInteger("gridWidth");
		int gridYsize = params.getInteger("gridHeight");
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);

		grid = gridFactory.createGrid("grid", context, new GridBuilderParameters<Object>(new BouncyBorders(),
				new SimpleGridAdder<Object>(), true, gridXsize, gridYsize));

		// Create firefighter instances, and add them to the context and to the grid in
		// random locations

		int firefighterCount = params.getInteger("firefighter_amount");

		for (int i = 0; i < firefighterCount; i++) {
			Firefighter f = new Firefighter(context, grid, i);
			context.add(f);
			grid.moveTo(f, Tools.getRandomPosWithinBounds(grid).toIntArray(null));
		}
		// Create forest instances, and add them to the context and to the grid
		double forestProb = 1; // Probability to plant a forest on a grid cell

		for (int i = 0; i < gridXsize; i++) {
			for (int j = 0; j < gridYsize; j++) {
				if (RandomHelper.nextDouble() < forestProb) {
					Forest f = new Forest(context, grid);
					context.add(f);
					grid.moveTo(f, i, j);
				}
			}
		}

		// Add wind to the simulation
		context.add(new Wind());

		// Schedule methods

		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		double rainProb = params.getDouble("rain_generation_speed");
		ScheduleParameters sch_params = ScheduleParameters.createRepeating(1, 1 / rainProb);
		addRainSchedule = schedule.schedule(sch_params, this, "addRain", context, grid);

		// Set the simulation termination tick
		int endTick = params.getInteger("end_tick");
		RunEnvironment.getInstance().endAt(endTick);

		// Add global counters to context
		context.add(new ExtinguishedFireCounter());
		context.add(new WeatherCheckCounter());
		context.add(new FireKnowledgeUpdateCounter());
		context.add(new ForestKnowledgeUpdateCounter());
		context.add(new MessageSentCounter());
		context.add(new AvgMessageLength());
		context.add(new RadioMsgCounter());
		context.add(new MsgMethodCounter());
		context.add(new TaskReceiveCounter());
		context.add(new TaskAcceptedCounter());
		context.add(new TaskCompleteCounter());

		return context;
	}

	/**
	 * Method called regularly to add a new rain to a random location
	 * 
	 * @param context
	 *            - context to add the rain to
	 * @param grid
	 *            - grid to add the rain to
	 */

	public void addRain(Context<Object> context, Grid<Object> grid) {
		new Rain(context, grid, null);
	}

	public int getVisiblePercentage() {
		Parameters params = RunEnvironment.getInstance().getParameters();
		int gridXsize = params.getInteger("gridWidth");
		int gridYsize = params.getInteger("gridHeight");
		boolean[][] board = new boolean[gridXsize][gridYsize];
		int sightRange = params.getInteger("firefighter_sight_range");
		for (Object o : context.getObjects(Firefighter.class)) {
			GridPoint p = grid.getLocation(o);
			for (int i = -sightRange; i <= sightRange; i++) {
				for (int j = -sightRange; j <= sightRange; j++) {
					if (p.getX() + i >= 0 && p.getX() + i < gridXsize && p.getY() + j >= 0
							&& p.getY() + j < gridYsize) {
						board[p.getX() + i][p.getY() + j] = true;
					} else {
					}

				}
			}
		}
		int counterVisible = 0;
		for (int i = 0; i < gridXsize; i++) {
			for (int j = 0; j < gridYsize; j++) {
				if (board[i][j]) {
					counterVisible++;
				}
			}
		}
		return (int) (((double) counterVisible / (gridXsize * gridYsize)) * 100);
	}

	/**
	 * gets the percentag of succesfully sent messages via the radio method in view
	 * of totally sucessfully sent messages
	 * 
	 * @return
	 */
	public int getRadioSentPercentage() {
		return (int) Math.round((((IGlobalCounter) context.getObjects(RadioMsgCounter.class).get(0)).getCounter()
				/ (double) ((IGlobalCounter) context.getObjects(MessageSentCounter.class).get(0)).getCounter()) * 100);
	}

	/**
	 * returns the average amount of the messages sucessfully sent per performing a
	 * broadcast/message sending.
	 * 
	 * @return
	 */
	public int avgContactsSentTo() {
		return (int) Math.round((((IGlobalCounter) context.getObjects(MessageSentCounter.class).get(0)).getCounter()
				/ (double) ((IGlobalCounter) context.getObjects(MsgMethodCounter.class).get(0)).getCounter()));

	}

	public int getTaskCompletitionPercent() {
		double taskaccepted = ((IGlobalCounter) context.getObjects(TaskAcceptedCounter.class).get(0)).getCounter();
		int taskcompleted = ((IGlobalCounter) context.getObjects(TaskCompleteCounter.class).get(0)).getCounter();
		return (int) Math.round((taskcompleted / taskaccepted)*100);
	}

	public int getTaskAcceptancePercent() {
		double tasknum = ((IGlobalCounter) context.getObjects(TaskReceiveCounter.class).get(0)).getCounter();
		int taskaccepted = ((IGlobalCounter) context.getObjects(TaskAcceptedCounter.class).get(0)).getCounter();
		return (int) Math.round((taskaccepted / tasknum)*100);
	}
	
	public int getTotalbountyEarned() {
		Parameters params = RunEnvironment.getInstance().getParameters();
		int bounty = params.getInteger("firefighter_fire_reward_bounty");
		return bounty*((IGlobalCounter) context.getObjects(ExtinguishedFireCounter.class).get(0)).getCounter();
	}
}
