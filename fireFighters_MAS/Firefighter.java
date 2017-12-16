package fireFighters_MAS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import antlr.build.Tool;
import globalcounter.IGlobalCounter;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ActionQueue;
import repast.simphony.engine.schedule.ISchedulableAction;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.collections.IndexedIterable;
import globalcounter.*;

// Enumerator listing the available message transmission methods
enum TransmissionMethod {
	Radio, // Send a message with the radio (local)
	Satellite // Send a message with a satellite (global)
}

/**
 * A class describing the Firefighter agent and its behavior This is a "stupid"
 * implementation of the firefighter you should focus on improving it by
 * utilizing the Multi-Agent Systems concepts learned in class
 * 
 * @author Kirill Tumanov, 2015-2017
 */
public class Firefighter {
	// Local variables definition
	private Context<Object> context; // Context in which the firefighter is placed
	private Grid<Object> grid; // Grid in which the firefighter is projected
	private GridPoint myPos;
	private int lifePoints; // Amount of damage the firefighter can still take from the fire, before
							// extinction
	private int strength; // Amount of damage the firefighter can deal to the fire
	private Velocity velocity; // Vector describing the firefighter movement's heading and speed
	private int sightRange; // Number of cells defining how far the firefighter can see around
	private int bounty; // Bounty units the firefighter has
	private int tasksSent;
	private int bountySpent;
	private int bountyToBeSent;
	private GridPoint bountyReceiver;
	private int bountyTransferred;
	private int lastBountyOffer;
	// Local variables initialization
	private boolean newInfo; // Flag if the firefighter has a new info to communicate to peers
	private Knowledge knowledge; // A knowledge the firefighter has
	private Knowledge newknowledge;
	ISchedulableAction stepSchedule; // Action scheduled for the step method
	ISchedulableAction removeSchedule; // Action scheduled for the remove method
	int id; // An ID of the firefighter
	private ArrayList<GridPoint> sightedFirefighters;
	private ArrayList<GridPoint> sightedFirefightersLastStep;
	Role role; // The character of the firefighter defining its behavior //TODO This should be
				// implemented
	boolean iWasLeader;
	// int numberOfGroups;
	// int myGroupNumber;
	// int peopleInMyGroup;
	// GridPoint groupLocation;
	// boolean atGroupLocation;
	// boolean foundGroup;
	int radioDist;
	Task TaskToGive;
	boolean windUpdate = false;
	boolean moved = false;
	boolean myFirstStep;
	boolean mySecondStep;
	int waitSteps;
	int stepsSinceLastUpdate = 0;
	int numleaders;
	int numteams;
	Integer leader;
	Integer oldleader;
	private boolean decentralizedCooperation = false;
	private boolean centralizedCooperation = false;
	public int groupNumber;
	private int randomStepCount;
	private double mainDirection;

	/**
	 * Custom constructor
	 * 
	 * @param context
	 *            - context to which the firefighter is added
	 * @param grid
	 *            - grid to which the firefighter is added
	 * @param id
	 *            - an id of the firefighter
	 */
	public Firefighter(Context<Object> context, Grid<Object> grid, int id) {
		Parameters params = RunEnvironment.getInstance().getParameters();
		// Initialize local variables
		this.context = context;
		// waitSteps=60;
		this.grid = grid;
		this.id = id;
		this.role = Role.Alone;
		tasksSent = 0;
		bountySpent = 0;
		bountyToBeSent = 0;
		bountyTransferred = 0;
		myFirstStep = true;
		mySecondStep = false;
		// numberOfGroups = 7;
		// myGroupNumber = -1;
		// peopleInMyGroup = 0;
		randomStepCount = 0;
		// atGroupLocation = false;
		// foundGroup = false;
		// myGroupNumber = id % numberOfGroups;
		iWasLeader = false;
		oldleader = id;
		leader = id;
		lastBountyOffer = 0;
		lifePoints = params.getInteger("firefighter_life");
		strength = params.getInteger("firefighter_strength");
		sightRange = params.getInteger("firefighter_sight_range");
		radioDist = params.getInteger("firefighter_radio_range");
		bounty = params.getInteger("firefighter_initial_bounty");
		numleaders = params.getInteger("firefighter_num_leaders");
		numteams = params.getInteger("firefighter_num_teams");
		if (numteams == 1)
			groupNumber = 0;
		else
			groupNumber = (this.id % numteams);
		double initialSpeed = params.getDouble("firefighter_initial_speed");
		double initialSpeedDeviation = params.getDouble("firefighter_initial_speed_deviation");
		velocity = new Velocity(RandomHelper.nextDoubleFromTo(initialSpeed - initialSpeedDeviation,
				initialSpeed + initialSpeedDeviation), RandomHelper.nextDoubleFromTo(0, 360));
		knowledge = new Knowledge(this.context); // No knowledge yet
		newknowledge = new Knowledge(this.context);
		newInfo = false; // No new info yet
		// Schedule methods
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		double current_tick = schedule.getTickCount();
		ScheduleParameters sch_params = ScheduleParameters.createRepeating(current_tick + 1,
				Math.round(1 / velocity.speed));
		stepSchedule = schedule.schedule(sch_params, this, "step");
	}

	/** A step method of the firefighter */
	@ScheduledMethod(shuffle = false) // Prevent call order shuffling
	public void step() {
		// if you want to try decentralized coop, uncomment line below
		numleaders = 0;

		if (!Tools.isAtTick(stepSchedule.getNextTime())) {
			return;
		} // Execute only at the specified ticks
		if (!context.contains(this)) {
			return;
		} // Safety
		myPos = grid.getLocation(this);

		// See if you received new bounty
		if (knowledge.getNewBounty() > 0) {
			bounty = bounty + knowledge.getNewBounty();
			knowledge.setNewBounty(0);
		}
		
		switch (numleaders) {
		case 0:
			switch (numteams) {
			case 1: // Decentralized cooperateon
				stepDecentralizedCooperation();
				break;
			case 2: // Competing decentralized teams
				stepDecentralizedCooperation();
				break;
			default:
				break;
			}
			break;
		case 1:
			switch (numteams) {
			case 1: // Centralized cooperation
				stepCentralizedCooperation();
				break;

			case 2: // Semi-centralized cooperating teams
				if (groupNumber == 1)
					stepCentralizedCooperation();
				else
					stepDecentralizedCooperation();
				break;
			default:
				break;
			}
			break;
		case 2:
			switch (numteams) {
			case 2:// Competing centralized teams
				stepCentralizedCooperation();
				break;
			default:
				break;
			}
			break;
		default:
			break;
		}

		// System.out.println("Extinguished: "
		// + ((IGlobalCounter)
		// context.getObjects(ExtinguishedFireCounter.class).get(0)).getCounter());
	}

	/**
	 * in this situation we have (at least) 1 leader
	 */
	private void stepCentralizedCooperation() {
		this.centralizedCooperation = true;
		moved = false;
		// Declare role in the beginning
		if (!myFirstStep) {
			assignRole();
		}
		// Info acquisition part (takes no time)
		checkEnvironment(sightRange);
		// increases a score for each known fire by 1
		knowledge.increaseFireScore();

		if (checkSurroundedByFire()) // If caught by fire, die
		{
			// Tell people that I am dead
			sendMessage(TransmissionMethod.Satellite, new ArrayList<GridPoint>(knowledge.getAllFirefighters().values()),
					MessageType.BYE);
			decreaseLifePoints(lifePoints);
			return;
		}

		// Action part (takes one step)

		// No follower checks weather
		// Leader checks weather if he is safe from fire
		boolean checkWeather = false;
		if (role == Role.Leader) {
			double[] firedirection = findDirection2NearestFire();
			if (firedirection[1] >= 0 && firedirection[1] < sightRange) {
				tryToMove(firedirection[0] + 180);
				// System.out.println("away from fire");
			} else {
				checkWeather = true;
			}
		}
		if (knowledge.getFire(myPos) > 0) {
			runOutOfFire(); // If firefighter knows that he is standing in the fire
		} else if (checkWeather) {
			Velocity oldWindVelocity = knowledge.getWindVelocity();
			checkWeather();
			Velocity windVelocity = knowledge.getWindVelocity();
			if (oldWindVelocity == null
					|| ((int) oldWindVelocity.direction * 100) != ((int) windVelocity.direction * 100)
					|| (int) oldWindVelocity.speed != (int) windVelocity.speed) {
				windUpdate = true;
			}
		} else {
			moveOrExtinguish();
			// Update own location (also in knowledge)
			myPos = grid.getLocation(this);
			knowledge.addFirefighter(myPos, id);
		}
		// send leader half of the bounty in the beginning
		if (role == Role.Follower && mySecondStep) {
			sendBounty(bounty / 2, knowledge.getFirefighter(leader),
					getTransmissionMethode(knowledge.getFirefighter(leader)));
			mySecondStep = false;
		}
		// Send each other messages to find leader
		if (myFirstStep) {
			// Send everybody your location and ID in case it is the first step
			sendMessage(TransmissionMethod.Satellite, new ArrayList<GridPoint>(), MessageType.POSITION);
			myFirstStep = false;
			mySecondStep = true;
		} else {
			// Communicating to Follower and Leader
			if (role == Role.Follower) {
				ArrayList<GridPoint> leaderloc = new ArrayList<GridPoint>(
						Arrays.asList(knowledge.getFirefighter(leader)));
				// Choose between Radio and Satellite
				TransmissionMethod leaderMethod = TransmissionMethod.Satellite;
				if (Tools.getDistance(myPos, knowledge.getFirefighter(leader)) <= this.radioDist)
					leaderMethod = TransmissionMethod.Radio;
				// Send leader what I can see if I see new things
				if (newInfo)
					sendMessage(leaderMethod, leaderloc, MessageType.ISEENEW);
				newInfo = false;
				stepsSinceLastUpdate++;
				if (moved && stepsSinceLastUpdate > 5)
					sendMessage(leaderMethod, leaderloc, MessageType.POSITION);
				stepsSinceLastUpdate = 0;

			} else if (role == Role.Leader) {
				// System.out.println(bounty);
				// Give followers your position in case you ran away from fire
				if (moved)
					sendMessage(TransmissionMethod.Satellite, new ArrayList<GridPoint>(), MessageType.POSITION);

				// Get location of followers
				HashMap<Integer, GridPoint> followers = knowledge.getAllFirefighters();
				followers.remove(id);
				// Send each follower specific task
				for (int followerID : followers.keySet()) {
					GridPoint destination = followers.get(followerID);
					ArrayList<GridPoint> destinationList = new ArrayList<GridPoint>(Arrays.asList(destination));
					TaskToGive = evaluate(followerID);
					if (knowledge.getTask(followerID) == null || !(knowledge.getTask(followerID).equals(TaskToGive))) {
						if (TaskToGive.getGridPoint() != null) {
							sendMessage(getTransmissionMethode(destination), destinationList, MessageType.TASK);
							knowledge.addTask(TaskToGive.getReceiverID(), TaskToGive.getGridPoint());
							tasksSent++;
						}
					}
				}
				// Send wind update
				if (windUpdate && !knowledge.getAllFire().isEmpty()) {
					// Send Global Message to everyone
					sendMessage(TransmissionMethod.Satellite, new ArrayList<GridPoint>(), MessageType.WIND);
					windUpdate = false;
				}
			}
		}

		// Set newknowledge to zero
		newknowledge = new Knowledge(this.context);
		// Update sighterFirefightersLastStep
		sightedFirefightersLastStep = sightedFirefighters;

	}

	private void stepDecentralizedCooperation() {
		this.decentralizedCooperation = true;

		// Info acquisition part (takes no time)
		checkEnvironment(sightRange);

		// Action part (takes one step) :

		///////////////////////////
		// Move or check weather //
		///////////////////////////

		if (knowledge.getFire(myPos) > 0) {
			runOutOfFire(); // If firefighter knows that he is standing in the fire
		} else {
			moveOrExtinguish();
		}

		/////////////////
		// Communicate //
		/////////////////
		
		// new fire is spotted in sight range and I have no task currently:
		if (knowledge.getCurrentTask() == null && newknowledge.getAllFire().size() > 0) {
			GridPoint fireGridPoint = newknowledge.getAllFire().get(0);
			
			// send global msg with task to help extinguish the fire
			Message msg = new Message();
			msg.setContent("Z " + fireGridPoint.getX() + " " + fireGridPoint.getY());
			sendGlobalMessage(msg);
			
			// set the fire as my current task
			knowledge.setCurrentTask(fireGridPoint);
		}
		else if (knowledge.getReceivedTask() != null) {
			// if I want to do this task then set it as current task
			if (evaluateTask(knowledge.getReceivedTask()))
				knowledge.setCurrentTask(knowledge.getReceivedTask());

			// either way set received task back to null, to indicate you have read it:
			knowledge.setReceivedTask(null);
		}

		// Set newknowledge to zero
		newknowledge = new Knowledge(this.context);

//		Parameters params = RunEnvironment.getInstance().getParameters();
//		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
//		if (schedule.getTickCount() == params.getInteger("end_tick")) {
//			System.out.println("Extinguished: " + 
//					((IGlobalCounter) context.getObjects(ExtinguishedFireCounter.class).get(0)).getCounter());
//		}
	}

	private boolean evaluateTask(GridPoint p) {	
		// I already have a task and therefore will not accept another one
		if(knowledge.getCurrentTask() != null)
			return false;
		
		return true;
	}

	private boolean groupInRadioDist() {
		for (int groupID : knowledge.getMyGroup().keySet()) {
			GridPoint destination = knowledge.getMyGroup().get(groupID);
			if (getTransmissionMethode(destination) == TransmissionMethod.Satellite) {
				return false;
			}
		}
		return true;
	}

	private boolean fireInKnowledge() {
		for (GridPoint f : knowledge.getAllFire()) {
			if (knowledge.getFire(f) != 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Executes task; either moving closer to task destination or extinguishing fire
	 * at task destination
	 * 
	 * @param taskDestination
	 *            gridpoint of task destination
	 */
	private boolean executeTask(GridPoint taskDestination) {
		if (taskDestination == null) {
			return false;
		}
		GridPoint myPos = grid.getLocation(this);
		double angleToTask = Tools.getAngle(myPos, taskDestination);
		double distanceToTask = Tools.getDistance(myPos, taskDestination);

		if (distanceToTask <= sightRange) {
			if (knowledge.getFire(taskDestination) == 0) { // && foundGroup) {
				// no more fire.
				knowledge.setCurrentTask(null);
				return false;
			}
		}
		// One grid cell away from task destination (i.e. the fire to be extinguished)
		if (distanceToTask == 1) {
			GridPoint sightPos = Tools.dirToCoord(velocity.direction, myPos);
			// Extinguish the fire in the direction of heading:
			if (taskDestination.equals(sightPos)) {
				extinguishFire(angleToTask);
			}
			// Turn to fire:
			else {
				velocity.direction = angleToTask;
			}
		} else if (distanceToTask == 2 && nextSquareOccupied(myPos, angleToTask)) { // avoid blocking each other
			// try +45 degrees and -45 degrees which ever one gets me to within 1 distance
			// of the task
			// is the angle I will choose to move with
			double angleToMove = angleToTask;
			if (Tools.getDistance(taskDestination, Tools.dirToCoord(angleToMove + 45.0, myPos)) == 1)
				angleToMove += 45.0;
			else if (Tools.getDistance(taskDestination, Tools.dirToCoord(angleToMove - 45.0, myPos)) == 1)
				angleToMove -= 45.0;
			return tryToMove(angleToMove);
		}
		// Move toward the task destination:
		else {
			return tryToMove(angleToTask);
		}
		return false;
	}

	/**
	 * Checks if the is a firefighter on the next square given a grid point and
	 * direction to go in.
	 * 
	 * @param point
	 *            Current grid point
	 * @param direction
	 *            Direction to head in
	 * @return Boolean
	 */
	private boolean nextSquareOccupied(GridPoint point, double direction) {
		GridPoint nextPoint = Tools.dirToCoord(direction, point);
		for (GridPoint p : knowledge.getAllFirefighters().values()) {
			if (p.equals(nextPoint))
				return true;
		}
		return false;
	}

	/** Movement routine of a firefighter */
	private void moveOrExtinguish() {
		// TODO do not attack the fire from the front/against the wind direction
		double result[] = findDirection2NearestFire();
		double directionToFire = result[0];
		double distance = result[1];

		if (knowledge.getCurrentTask() != null) {
			executeTask(knowledge.getCurrentTask());
			// } else if (distance > 1) { // If fire is more than extinguishingDistance away
			// tryToMove(directionToFire);
		} else if (distance == 1) {
			extinguishFire(directionToFire);
		} else { // Otherwise explore randomly
			// Firefighter sets a new main direction every 15 steps
			// He walks in the main direction with higher probability (0.7 in this case)
			setMainDirection(15);
			velocity.direction = getRandomDirection(0.7, 20);
			// velocity.direction = RandomHelper.nextDoubleFromTo(0, 360);
			tryToMove(velocity.direction);
		}
	}

	/**
	 * Resets main direction after 'threshold' steps
	 * 
	 * @param threshold
	 */
	private void setMainDirection(int threshold) {
		if (randomStepCount >= threshold) {
			randomStepCount = 0;
			this.mainDirection = RandomHelper.nextDoubleFromTo(0, 360);
		} else
			randomStepCount++;
	}

	/**
	 * Decides where to go based on main direction. With a certain it goes in a
	 * certain degree window of the main direction
	 * 
	 * @param double
	 *            prob: probability to go in the main direction double angle: size
	 *            of the angle where to go
	 * @return double direction to go to
	 */
	private double getRandomDirection(double prob, double angle) {
		double directionToGo = 0;
		if (RandomHelper.nextDoubleFromTo(0, 1) < prob)
			directionToGo = RandomHelper.nextDoubleFromTo(this.mainDirection - (angle / 2),
					this.mainDirection + (angle / 2));
		else {
			if (RandomHelper.nextDoubleFromTo(0, 1) < 0.5)
				directionToGo = RandomHelper.nextDoubleFromTo(0, this.mainDirection - 45);
			else
				directionToGo = RandomHelper.nextDoubleFromTo(this.mainDirection + 45, 360);
		}
		return directionToGo;
	}

	public void runAwayFromFire() {
		double result[] = findDirection2NearestFire();
		double direction = result[0];
		tryToMove(direction + 180);
	}

	/**
	 * Given a possible movement direction, generate a set of others, and try to
	 * move in one of them
	 * 
	 * @param pDir
	 *            - direction to try to move to
	 * @return 0 - movement failed, 1 - movement succeeded
	 */
	private boolean tryToMove(double pDir) {
		GridPoint myPos = grid.getLocation(this);
		for (int i = 0; i < 8; i++) {
			GridPoint newPos = Tools.dirToCoord(pDir + (i % 2 == 0 ? -i * 45 : i * 45), myPos);

			if (move(newPos) == 0) {
				return true;
			}
		}

		return false;
	}

	/** Firefighter's reaction on being in the fire */
	private void runOutOfFire() {
		if (lifePoints <= 1)
			sendMessage(TransmissionMethod.Satellite, new ArrayList<GridPoint>(knowledge.getAllFirefighters().values()),
					MessageType.BYE);
		if (!decreaseLifePoints(1)) {
			return;
		} // Burn, and see if still moving

		Velocity knownWindVelocity = knowledge.getWindVelocity();
		// set a random movement direction for the cae no knowledge is available about
		// the wind
		double directionUpwind = RandomHelper.nextDoubleFromTo(0, 360);

		if (knownWindVelocity != null) {
			// run in the direction the fire is not going to
			directionUpwind = knownWindVelocity.direction + 180;
		}

		tryToMove(directionUpwind); // Try to move in an upwind direction to escape from fire
	}

	/**
	 * Decrease the lifePoints of the firefighter by a given amount
	 * 
	 * @param amount
	 *            - an amount to decrease by
	 * @return 1 if still active, 0 - otherwise
	 */
	private boolean decreaseLifePoints(int amount) {
		lifePoints -= amount;

		if (lifePoints <= 0) {
			ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
			double current_tick = schedule.getTickCount();
			ScheduleParameters removeParams = ScheduleParameters.createOneTime(current_tick + 0.000001);
			removeSchedule = schedule.schedule(removeParams, this, "remove");
			// Schedule method calls to the future
			stepSchedule.reschedule(new ActionQueue());
			// Remove the future method calls
			schedule.removeAction(stepSchedule);
			return false;
		}

		return true;
	}

	/**
	 * Check a NxN area around the firefighter for fires (N = 2*sightRange + 1)
	 * 
	 * @param sightRange
	 *            - number of cells from the given position to search in
	 */
	private void checkEnvironment(int sightRange) {
		GridPoint myPos = grid.getLocation(this);
		sightedFirefighters = new ArrayList<>();
		for (int i = -sightRange; i <= sightRange; i++) {
			for (int j = -sightRange; j <= sightRange; j++) {
				if (i != 0 || j != 0)
					checkCell(new GridPoint(myPos.getX() + i, myPos.getY() + j));
			}
		}
	}

	public void selectMyGroup() {

	}

	/**
	 * Computes group location by setting it to the average GridPoint of all group
	 * members
	 * 
	 * @return Group location as gridpoint
	 */
	// public GridPoint findGroupLocation() {
	// double avgXCoord = 0;
	// double avgYCoord = 0;
	// int numberGroupMembers = 0;
	// for (int ID : knowledge.getAllFirefighters().keySet()) {
	// if (ID % numberOfGroups == myGroupNumber) {
	// GridPoint groupMemberPos = knowledge.getAllFirefighters().get(ID);
	// avgXCoord += groupMemberPos.getX();
	// avgYCoord += groupMemberPos.getY();
	// numberGroupMembers++;
	// }
	// }
	// avgXCoord = avgXCoord / numberGroupMembers;
	// avgYCoord = avgYCoord / numberGroupMembers;
	// groupLocation = new GridPoint((int) avgXCoord, (int) avgYCoord);
	//
	// peopleInMyGroup = numberGroupMembers;
	// return groupLocation;
	//
	// }

	// private void formGroup() {
	// GridPoint analysePos = new GridPoint();
	// boolean hasFirefighter = false;
	//
	// // Check the grid points surrounding the group location
	// for (int i = -sightRange; i <= sightRange; i++) {
	// for (int j = -sightRange; j <= sightRange; j++) {
	// // don't check the group location
	// if (i != 0 || j != 0) {
	// analysePos = new GridPoint(groupLocation.getX() + i, groupLocation.getY() +
	// j);
	// hasFirefighter = (Tools.getObjectOfTypeAt(grid, Firefighter.class,
	// analysePos) != null);
	// // If there is a firefighter and its not me and I don't already have him in
	// my
	// // group send him a message and/or add him to the group
	// if (hasFirefighter && !analysePos.equals(grid.getLocation(this)) &&
	// (knowledge.getMyGroup() == null
	// || !knowledge.getMyGroup().values().contains(analysePos))) {
	//
	// HashMap<Integer, GridPoint> friends = knowledge.getAllFirefighters();
	// // Check my knowledge if there is a firefighter with the same location
	// for (int ID : friends.keySet()) {
	// // Check if I know a firefighter on this location that belongs to my group
	// if (myGroupNumber == (ID % numberOfGroups) &&
	// friends.get(ID).equals(analysePos)) {
	// // add him to my group
	// // System.out.println("Group "+myGroupNumber+" Firefighter "+id+" added
	// // firefighter "+ID+" to his group"+" GroupLocation"+groupLocation+" my
	// location
	// // "+ grid.getLocation(this));
	// knowledge.addToMyGroup(analysePos, ID);
	// }
	// }
	// // Since this guy was not in my group yet, send a message
	// sendMessage(TransmissionMethod.Radio, new
	// ArrayList<GridPoint>(Arrays.asList(analysePos)),
	// MessageType.POSITION);
	// }
	//
	// }
	//
	// }
	// }
	// }

	/**
	 * Check if a firefighter is surrounded by fire Note: this method assumes that
	 * the firefighter knowledge about the surrounding was updated already, so
	 * checkCell(...) is not called
	 * 
	 * @return 1 if surrounded, 0 if not
	 */
	private boolean checkSurroundedByFire() {
		GridPoint myPos = grid.getLocation(this);

		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if (!(i == 0 && j == 0)) // Do not check the point at which standing
				{
					GridPoint pos = new GridPoint(myPos.getX() + i, myPos.getY() + j);

					if (knowledge.getFire(pos) == 0) {
						return false;
					}
				}
			}
		}

		return true;
	}

	/**
	 * Extinguish fire in a given direction
	 * 
	 * @param directionToFire
	 *            - a direction to extinguish fire in
	 */
	private void extinguishFire(double directionToFire) {
		GridPoint myPos = grid.getLocation(this);
		GridPoint firePos = Tools.dirToCoord(directionToFire, myPos);
		Fire fire = (Fire) Tools.getObjectOfTypeAt(grid, Fire.class, firePos);

		if (fire != null) {

			if (!fire.decreaseLifetime(strength)) // If the fire was extinguished
			{
				Parameters params = RunEnvironment.getInstance().getParameters();
				bounty += params.getInteger("firefighter_fire_reward_bounty");

				// Get fire extinguished counter and increment:
				((IGlobalCounter) context.getObjects(ExtinguishedFireCounter.class).get(0)).incrementCounter();
			}
		}
	}

	/**
	 * Move to a given position
	 * 
	 * @param newPos
	 *            - a position to move to
	 * @return -1 - if move was unsuccessful, 0 - if move was successful, 1 - if
	 *         couldn't move because another firefighter already took this place
	 */
	private int move(GridPoint newPos) {
		if (Tools.isWithinBorders(newPos, grid)) {
			checkCell(newPos);

			boolean hasFire = knowledge.getFire(newPos) > 0;
			boolean hasFirefighter = hasFirefighter(newPos);

			if (!hasFire && !hasFirefighter) // Make sure that the cell is not on fire, and is not occupied by another
												// firefighter
			{
				grid.moveTo(this, newPos.toIntArray(null));
				this.moved = true;
				return 0;
			} else if (hasFirefighter) {
				return 1;
			}
		}

		return -1;
	}

	private boolean hasFirefighter(GridPoint pos) {
		for (GridPoint p : sightedFirefighters) {
			if (p.equals(pos))
				return true;
		}
		return false;
	}

	/**
	 * Method used to find the direction and distance to the nearest fire
	 * 
	 * @return a tuple (direction, distance) to the nearest fire
	 */
	private double[] findDirection2NearestFire() {
		GridPoint myPos = grid.getLocation(this);
		int minDist = Integer.MAX_VALUE;
		double direction = -1;

		for (GridPoint p : knowledge.getAllFire()) // For all the fires in the firefighter's knowledge
		{
			if (knowledge.getFire(p) != 0) {
				int dist = Tools.getDistance(myPos, p);
				// Determine if the fire is closest. If so, update distance and direction
				// accordingly
				if (dist < minDist) {
					minDist = dist;
					direction = Tools.getAngle(myPos, p);
				}
			}
		}

		double[] result = { direction, (minDist == Integer.MAX_VALUE ? -1 : minDist) };

		return result;
	}

	/**
	 * Check if a given position contains a fire object, update own knowledge
	 * 
	 * @param pos
	 *            - position to check
	 */
	public void checkCell(GridPoint pos) {
		if (!Tools.isWithinBorders(pos, grid))
			return;
		boolean hasFirefighter = (Tools.getObjectOfTypeAt(grid, Firefighter.class, pos) != null);
		boolean hasFire = (Tools.getObjectOfTypeAt(grid, Fire.class, pos) != null);
		boolean hasForest = (Tools.getObjectOfTypeAt(grid, Forest.class, pos) != null);

		GridPoint task = knowledge.getCurrentTask();

		// delete task if one step away?
		// if (task != null && task.equals(pos) && Tools.getDistance(task,
		// grid.getLocation(this)) == 1) {
		// knowledge.setCurrentTask(null);
		// atGroupLocation = true;
		// }
		if (hasFire) {
			if (knowledge.addFire(pos)) {
				// If the fire is not yet in the knowledge, update leader about it
				newknowledge.addFire(pos);
				newInfo = true;
			}
		} else {
			if (knowledge.getCurrentTask() != null && pos.equals(knowledge.getCurrentTask())) {
				((IGlobalCounter) context.getObjects(TaskCompleteCounter.class).get(0)).incrementCounter();
				if (decentralizedCooperation) {
					knowledge.setCurrentTask(null);
					// Send global message for other firefighters to drop this task
					Message msg = new Message();
					msg.setContent("Y "+pos.getX()+" "+pos.getY()); // arbitrarily chosen letter
					sendGlobalMessage(msg);
				}
			}
			knowledge.removeFire(pos);
		}

		if (hasFirefighter) {
			// only send the new firefighter ID and location of your leader
			sightedFirefighters.add(pos);
		} else {
		}
		if (!hasForest) {
			if (knowledge.addBurnedForest(pos)) {
				// if burned forest is not yet in the knowledge, update leader about it
				newknowledge.addBurnedForest(pos);
				newInfo = true;
			}
		}
	}

	/**
	 * Send a message to a given set of recipients using a given communication
	 * method
	 * 
	 * @param transmissionMethod
	 *            - a method to used for message sending
	 * @param recipientLocations
	 *            - a list of locations of firefighters to send the message to (only
	 *            important if radio)
	 */
	public void sendMessage(TransmissionMethod transmissionMethod, ArrayList<GridPoint> recipientLocations,
			MessageType messageType) {
		Parameters params = RunEnvironment.getInstance().getParameters();
		GridPoint myPos = grid.getLocation(this);
		Message message = new Message();
		// Get leader informations
		GridPoint leaderLocation = knowledge.getAllFirefighters().get(leader);
		GridPoint oldleaderLocation = knowledge.getAllFirefighters().get(oldleader);
		switch (messageType) {
		case ALL:
			message.setContent(this.knowledge.convert2String());
			break;
		case POSITION:
			GridPoint position = this.grid.getLocation(this);
			message.setContent("FF " + position.getX() + " " + position.getY() + " " + this.id);
			break;
		case ISEENEW:
			// Update leader only with stuff that is new!
			message.setContent(this.newknowledge.convert2String());
			break;
		case ISEE:
			// Update leader only with stuff that is new!
			message.setContent(this.knowledge.WhatIsee2String(myPos, sightRange));
			break;
		case LEADER:
			message.setContent("HW " + leaderLocation.getX() + " " + leaderLocation.getY() + " " + this.leader);
			break;
		case OLDLEADER:
			message.setContent("HW " + oldleaderLocation.getX() + " " + oldleaderLocation.getY() + " " + this.leader);
			break;
		case BYE:
			message.setContent("DF " + id);
			break;
		case TASK:
			message.setContent("T " + TaskToGive.getX() + " " + TaskToGive.getY() + " " + TaskToGive.getReceiverID()
					+ " " + TaskToGive.getSenderID());
			break;
		case WIND:
			if (knowledge.getWindVelocity() == null) {
				message.setContent(""); // because of nullpointer
			} else {
				message.setContent(
						"W " + knowledge.getWindVelocity().speed + " " + knowledge.getWindVelocity().direction);
			}
			break;
		case BOUNTY:
			message.setContent("B " + bountyToBeSent + " " + bountyReceiver.getX() + " " + bountyReceiver.getY());
			break;
		case GROUPDIRECTION:
			message.setContent("GD " + knowledge.getGroupDirection());
		default:
			break;
		}
		IndividualMessageCounter indMesCount = ((IndividualMessageCounter) context
				.getObjects(IndividualMessageCounter.class).get(0));

		// System.out.println("Message -" + message.getContent() + " -from " + id);
		if (transmissionMethod == TransmissionMethod.Radio)
			sendLocalMessage(recipientLocations, message);
		else
			sendGlobalMessage(message);
	}

	/**
	 * Sends local method to the firefighters
	 * 
	 * @param recipientLocations
	 *            firefighter location that the message should be sent to
	 * @param message
	 *            message that should be sent
	 */
	public void sendLocalMessage(ArrayList<GridPoint> recipientLocations, Message message) {
		Parameters params = RunEnvironment.getInstance().getParameters();
		GridPoint myPos = grid.getLocation(this);
		int messageCost = message.getCost();
		int radioRange = params.getInteger("firefighter_radio_range");

		// counts the general message sending actions
		((IGlobalCounter) context.getObjects(MsgMethodCounter.class).get(0)).incrementCounter();

		for (GridPoint recipientLocation : recipientLocations) {

			Firefighter recipient = (Firefighter) Tools.getObjectOfTypeAt(grid, Firefighter.class, recipientLocation);
			if (recipient != null && recipient.groupNumber == this.groupNumber) // First of all, if the recipient is
																				// there at all
			{
				if (Tools.getDistance(myPos, recipientLocation) <= radioRange) // If using the radio, and the
																				// recipient is within range
				{
					if (getBounty() >= messageCost) {
						recipient.receiveMessage(message); // Deliver message
						bounty -= messageCost; // Pay for the message
						// System.out.println("Firefighter "+id+" spend "+messageCost+" bounty on a
						// local message");
						bountySpent += messageCost;
						((IGlobalCounter) context.getObjects(MessageSentCounter.class).get(0)).incrementCounter();
						((IGlobalCounter) context.getObjects(RadioMsgCounter.class).get(0)).incrementCounter();
						((AvgMessageLength) context.getObjects(AvgMessageLength.class).get(0))
								.addMessage(message.getContent());

					}
				}
			}
		}
	}

	/**
	 * Sends a global message to every firefighter on the map using the Satellite
	 * transmission method
	 * 
	 * @param message
	 *            the message to be send to every firefighter
	 */
	public void sendGlobalMessage(Message message) {
		Parameters params = RunEnvironment.getInstance().getParameters();
		int messageCost = message.getCost();
		int satelliteCostMultiplier = params.getInteger("firefighter_satellite_cost_multiplier");
		double globalMessageCost = messageCost * satelliteCostMultiplier; // A cost to send a message
		if (bounty < globalMessageCost) {
			return;
		}
		bounty -= globalMessageCost; // Pay for the message
		bountySpent += globalMessageCost;

		// Get recipient locations of all firefighter on the map:
		IndexedIterable<Object> firefightersFromContext = context.getObjects(Firefighter.class);
		List<Firefighter> allFirefighters = new ArrayList<>();
		for (Object firefighterFromContext : firefightersFromContext) {
			// Casting to type Firefighter:
			Firefighter firefighter = (Firefighter) firefighterFromContext;
			// Do not send message to myself:
			if (firefighter.equals(this))
				continue;
			else
				allFirefighters.add(firefighter);
		}
		// Increment counters:
		((IGlobalCounter) context.getObjects(MsgMethodCounter.class).get(0)).incrementCounter();
		IndividualMessageCounter indMesCount = ((IndividualMessageCounter) context
				.getObjects(IndividualMessageCounter.class).get(0));
		// System.out.println("Firefighter "+id+" spend "+messageCost+" bounty on a
		// global message");
		// Increment counters:
		((IGlobalCounter) context.getObjects(MessageSentCounter.class).get(0)).incrementCounter();
		((AvgMessageLength) context.getObjects(AvgMessageLength.class).get(0)).addMessage(message.getContent());

		// If bounty is send via global message, only send it to specific firefighter
		// and not all!
		if (message.getContent().charAt(0) == 'B') {
			String[] content = message.getContent().split(" ");
			GridPoint position = new GridPoint(Integer.parseInt(content[2]), Integer.parseInt(content[3]));
			for (Firefighter recipient : allFirefighters) {
				if (recipient.grid.getLocation(recipient).equals(position))
					recipient.receiveMessage(message);
			}

		} else {
			for (Firefighter recipient : allFirefighters) {
				if (recipient != null && recipient.groupNumber == this.groupNumber) {
					recipient.receiveMessage(message); // Deliver message
				}
			}
			newInfo = false; // All the new information was sent, over now
		}
	}

	/**
	 * Receive message
	 * 
	 * @param message
	 *            - a message
	 */
	public void receiveMessage(Message message) {
		if (message.getContent().charAt(0) == 'T') {
			// incoming task: ("T" + " " + highscore.getX() + "" + highscore.getY() + " " +
			// reward + " " + this.id);
			String[] content = message.getContent().split(" ");
			GridPoint position = new GridPoint(Integer.parseInt(content[1]), Integer.parseInt(content[2]));
			Integer receiverID = Integer.parseInt(content[3]);
			// int reward = Integer.parseInt(content[3]);
			int sender = Integer.parseInt(content[4]);
			boolean accepted = true;
			// TODO decide when to accept
			if (knowledge.getCurrentTask() != null) {
				accepted = false;
			}
			// Only accept task if yu want to and you are the receiver
			if (accepted && this.id == receiverID) {
				// System.out.println("receive&accepted");
				knowledge.setCurrentTask(position);
				ArrayList<GridPoint> receiver = new ArrayList<>();
				receiver.add(knowledge.getAllFirefighters().get(sender));
				Message m = new Message();
				m.setContent("A " + this.id);
				// sendLocalMessage(receiver, m);
			}
		} else if (message.getContent().charAt(0) == 'A') {
			String[] content = message.getContent().split(" ");
			sendBounty(lastBountyOffer, knowledge.getAllFirefighters().get(Integer.parseInt(content[1])),
					TransmissionMethod.Radio);
		} else if (message.getContent().charAt(0) == 'Z') { // arbitrarily chosen letter, used for Decentralized Coop
			// Unpack
			String[] content = message.getContent().split(" ");
			GridPoint position = new GridPoint(Integer.parseInt(content[1]), Integer.parseInt(content[2]));
			// Set received task in knowledge
			knowledge.setReceivedTask(position);
		} else if (message.getContent().charAt(0) == 'Y') { // arbitrarily chosen letter
			// Unpack
			String[] content = message.getContent().split(" ");
			GridPoint position = new GridPoint(Integer.parseInt(content[1]), Integer.parseInt(content[2]));
			// if received pos equals current task position then quit current task because fire is gone there
			if (position.equals(knowledge.getCurrentTask())) 
				knowledge.setReceivedTask(null);
		}
		else{
			Knowledge receivedKnowledge = new Knowledge(this.context);
			receivedKnowledge.convertFromString(message.getContent());
			knowledge.updateFromKnowledge(receivedKnowledge);
		}
	}

	/**
	 * Chooses if message should be send over Radio or Satellite
	 * 
	 * @param destination
	 *            - location of the firefighter that has to be reached
	 * @return TransmissionMethod
	 */
	private TransmissionMethod getTransmissionMethode(GridPoint destination) {
		if (Tools.getDistance(grid.getLocation(this), destination) <= radioDist) {
			return TransmissionMethod.Radio;
		} else {
			return TransmissionMethod.Satellite;
		}

	}

	/**
	 * This methods returns the Gridpoint which should be reached next by the
	 * Firefighter (The next task) this is only going to be executed by the leader
	 * 
	 * 
	 * @param f
	 *            current firefighter
	 * @return next Task
	 */
	private Task evaluate(int id) {

		// TODO add a border if the fire is known for more than x it does not make sense
		// to move there
		// TODO take positions of the other firefighters into account so they spread
		// around the grid
		// TODO if a firefighter of the same team is in radio range of this gridpoint
		// this point is preferred
		// TODO only update exploration task if there is a new fire

		// Get access to the user accessible parameters
		Parameters params = RunEnvironment.getInstance().getParameters();
		int gridXsize = params.getInteger("gridWidth");
		int gridYsize = params.getInteger("gridHeight");
		sightRange = params.getInteger("firefighter_sight_range");
		int maxvalue = Integer.MIN_VALUE;
		GridPoint highscore = null;
		GridPoint Pos = knowledge.getFirefighter(id);
		GridPoint currenttask = knowledge.getCurrentTask();

		/*
		 * Go through all fire gridpoints check lifetime check distance if lifetime +
		 * distance > 10????? not reachable continue Else Gridpoint is goal If no goal
		 * found search for a point where i have space (no other firefighters) (Take
		 * tasks of the other firefightes into account If other firefightes have no task
		 * take their current position into account)
		 * 
		 */

		for (GridPoint p : knowledge.getAllFire()) {
			int count = 0;
			for (int otherid : knowledge.getAllFirefighters().keySet()) {
				GridPoint s = knowledge.getTask(otherid);
				if (s == p)
					count = count + 1;
			}
			if (count > 2)
				continue;
			int tempvalue = 0;
			int dist = Tools.getDistance(Pos, p);
			int fire = knowledge.getFire(p);
			if (fire == 0)
				continue;
			// Check if surrounded by fire is not necessary because in this case there is
			// allways a fire that is nearer
			tempvalue = tempvalue - dist - fire + 10;
			// Wind should only have influence if two fires got the same value
			if (tempvalue > 0) {
				if (tempvalue > maxvalue) {
					maxvalue = tempvalue;
					highscore = p;
				}
				if (tempvalue == maxvalue) { // take wind into account
					Velocity wind = knowledge.getWindVelocity();
					if (wind != null) {
						double direction = wind.direction;
						double directionfirea = Tools.getAngle(p, Pos);
						double directionfireb = Tools.getAngle(highscore, Pos);
						// decide which fire is better because the direction is not the wind direction
						if (Math.abs(direction - directionfirea) > Math.abs(direction - directionfireb)) {
							highscore = p;
						}
					}
				}
			}
		}
		// If no fire was found firefighter should explore
		// Firefighters should spread out such that they cover as many fields as
		// possible for observation and have as less fields as possible shared

		/*
		 * if (highscore == null) { for (int x = 1; x <= gridXsize; x++) { for (int y =
		 * 1; y <= gridYsize; y++) { GridPoint p = new GridPoint(x, y); boolean valid =
		 * true; // search for all grid points that have a distance of 2 times of how
		 * far they // can see to the other firefighters or their tasks // for every
		 * firefighter // if has task // check if girdpoint is 2times sightrange away if
		 * so mark boolean as true if // not continue // else // check if gridpoint is 2
		 * times sightrange away from current position if not // continue (biger for
		 * loop)
		 * 
		 * for (int tmpID : knowledge.getAllFirefighters().keySet()) { // first check
		 * for the current task if (knowledge.getTask(tmpID) != null) { GridPoint q =
		 * knowledge.getTask(tmpID); int dist = Tools.getDistance(p, q); if (dist <= 2 *
		 * sightRange) { valid = false; } } else { // check for the current position
		 * GridPoint q = knowledge.getFirefighter(tmpID); int dist =
		 * Tools.getDistance(p, q); if (dist <= 2 * sightRange) valid = false; } }
		 * 
		 * if (valid == true) { int dist = Tools.getDistance(Pos, p); int tempvalue =
		 * -dist; if (tempvalue > maxvalue) { maxvalue = tempvalue; highscore = p; } }
		 * 
		 * } } }
		 */
		Task TaskToGive = new Task(highscore, lastBountyOffer, this.id, id);
		if (maxvalue > 5) {
			// System.out.println(bounty);

			if (bounty > 140) {
				lastBountyOffer = (int) (bounty * 0.08 / (knowledge.getAllFirefighters().size() / numteams));
				// sendTask(TaskToGive);
			}

		}
		return TaskToGive;
	}

	public void sendTask(Task task) {
		ArrayList<GridPoint> recipientList = new ArrayList<>();
		for (int groupID : knowledge.getMyGroup().keySet()) {
			GridPoint destination = knowledge.getMyGroup().get(groupID);
			if (getTransmissionMethode(destination) == TransmissionMethod.Radio) {
				recipientList.add(destination);
			}
		}
		if (recipientList.size() != 0) {
			Message m = new Message();
			m.setContent("T" + " " + task.getX() + " " + task.getY() + " " + task.getReward() + " " + this.id);
			sendLocalMessage(recipientList, m);
		}
	}

	/** Define the firefighter character */
	private void assignRole() {
		// If he doesn't know any one -> Role Alone
		if (knowledge.getAllFirefighters().isEmpty())
			role = Role.Alone;
		else {
			HashMap<Integer, GridPoint> friends = knowledge.getAllFirefighters();
			int leaderID = getLowestID(friends.keySet());
			if (leaderID == this.id) {
				// If the only one he knows is himself -> Role Alone
				if (friends.keySet().size() == 1)
					this.role = Role.Alone;
				// If he knows at least one other firefighter and has the lowest ID -> Role
				// Leader
				else
					this.role = Role.Leader;
			}
			// If he has a firefighter in his knowledge with a lower ID -> Role.Follower
			else {
				this.role = Role.Follower;
				leader = leaderID;
			}
		}
	}

	private int getLowestID(Set<Integer> IDs) {
		int min = this.id;
		for (int ID : IDs) {
			if (ID < min)
				min = ID;
		}
		return min;
	}

	/** Check current weather conditions */
	private void checkWeather() {
		if (context != null) {
			// Increment weather check counter
			((IGlobalCounter) context.getObjects(WeatherCheckCounter.class).get(0)).incrementCounter();

			if (context.getObjects(Wind.class).size() > 0) {
				knowledge.setWindVelocity(((Wind) context.getObjects(Wind.class).get(0)).getWindDirection());
			}

			IndexedIterable<Object> rains = context.getObjects(Rain.class);

			// Removes all rain objects from knowledge
			knowledge.removeAllRain();
			// Add accurate rain knowledge
			for (Object o : rains) {
				knowledge.addRain(grid.getLocation(o));
			}
		}
	}

	/**
	 * Calculates who of the firefighters was probably the same as seen the round
	 * before If a seen firefighter has distance lower than two to the seen
	 * firefighters of last round, he is not new
	 * 
	 * @return Gridpoint list of who of the seen firefighters is new
	 */
	private ArrayList<GridPoint> computeNewSeenFirefighters() {
		if (sightedFirefighters == null || sightedFirefightersLastStep == null || sightedFirefightersLastStep.isEmpty()
				|| sightedFirefighters.isEmpty())
			return sightedFirefighters;
		@SuppressWarnings("unchecked")
		ArrayList<GridPoint> newSeenFirefighters = (ArrayList<GridPoint>) sightedFirefighters.clone();
		for (GridPoint newff : sightedFirefighters) {
			for (GridPoint oldff : sightedFirefightersLastStep) {
				if (Tools.getDistance(newff, oldff) < 2)
					newSeenFirefighters.remove(newff);
			}
		}
		return newSeenFirefighters;
	}

	/** The method for removal of the object */
	@ScheduledMethod(shuffle = false) // Prevent call order shuffling
	public void remove() {
		if (!Tools.isLastTick()) {
			context.remove(this);
		} // Do not include this in the executeEndActions()
	}

	public boolean sendBounty(int sendbounty, GridPoint Firefighter, TransmissionMethod transmissionmethod) {
		if (bounty - sendbounty <= 0)
			return false;
		this.bountyReceiver = Firefighter;
		this.bountyToBeSent = sendbounty;
		bounty = bounty - sendbounty;
		bountyTransferred += sendbounty;
		sendMessage(transmissionmethod, new ArrayList<GridPoint>(Arrays.asList(Firefighter)), MessageType.BOUNTY);
		this.bountyToBeSent = 0;
		return true;
	}

	/// Local getters & setters
	/**
	 * Get current amount of lifePoints the firefighter has
	 * 
	 * @return the amount of lifePoints
	 */
	public int getLifePoints() {
		return lifePoints;
	}

	/**
	 * Get current amount of strength the firefighter has
	 * 
	 * @return the amount of strength
	 */
	public int getStrength() {
		return strength;
	}

	/**
	 * Get current amount of bounty the firefighter has
	 * 
	 * @return the amount of bounty
	 */
	public int getBounty() {
		return bounty;
	}

	/**
	 * Get the currently sent tasks
	 * 
	 * @return the number of tasks
	 */
	public int getTasksSent() {
		return tasksSent;
	}

	public int getBountySpent() {
		return bountySpent;
	}

	public int getBountyTransferred() {
		return bountyTransferred;
	}

	/**
	 * Get speed of the firefighter
	 * 
	 * @return - the speed
	 */
	public double getSpeed() {
		return velocity.speed;
	}

	/**
	 * Get heading of the firefighter
	 * 
	 * @return - the direction
	 */
	public double getHeading() {
		return velocity.direction;
	}

	/**
	 * Get ID of the firefighter
	 * 
	 * @return - the ID
	 */
	public double getId() {
		return id;
	}

	/**
	 * Get position of the firefighter
	 * 
	 * @return - the position
	 */
	public String getPos() {
		return grid.getLocation(this).toString();
	}
}
