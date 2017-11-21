package fireFighters_MAS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
	private int lifePoints; // Amount of damage the firefighter can still take from the fire, before
							// extinction
	private int strength; // Amount of damage the firefighter can deal to the fire
	private Velocity velocity; // Vector describing the firefighter movement's heading and speed
	private int sightRange; // Number of cells defining how far the firefighter can see around
	private int bounty; // Bounty units the firefighter has
	// Local variables initialization
	private boolean newInfo; // Flag if the firefighter has a new info to communicate to peers
	private Knowledge knowledge; // A knowledge the firefighter has
	ISchedulableAction stepSchedule; // Action scheduled for the step method
	ISchedulableAction removeSchedule; // Action scheduled for the remove method
	int id; // An ID of the firefighter
	private ArrayList<GridPoint> sightedFirefighters;
	Role role; // The character of the firefighter defining its behavior //TODO This should be
				// implemented
	boolean iWasLeader;

	Integer leader;
	Integer oldleader;

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
		this.grid = grid;
		this.id = id;
		this.role = Role.Alone;
		iWasLeader = false;
		oldleader = id;
		leader = id;
		lifePoints = params.getInteger("firefighter_life");
		strength = params.getInteger("firefighter_strength");
		sightRange = params.getInteger("firefighter_sight_range");
		bounty = params.getInteger("firefighter_initial_bounty");
		double initialSpeed = params.getDouble("firefighter_initial_speed");
		double initialSpeedDeviation = params.getDouble("firefighter_initial_speed_deviation");
		velocity = new Velocity(RandomHelper.nextDoubleFromTo(initialSpeed - initialSpeedDeviation,
				initialSpeed + initialSpeedDeviation), RandomHelper.nextDoubleFromTo(0, 360));
		knowledge = new Knowledge(this.context); // No knowledge yet
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
		if (!Tools.isAtTick(stepSchedule.getNextTime())) {
			return;
		} // Execute only at the specified ticks

		if (!context.contains(this)) {
			return;
		} // Safety
		GridPoint myPos = grid.getLocation(this);
		// Info acquisition part (takes no time)
		checkEnvironment(sightRange);

		if (checkSurroundedByFire()) // If caught by fire, die
		{
			// Tell people that I am dead
			sendMessage(TransmissionMethod.Satellite, new ArrayList<GridPoint>(knowledge.getAllFirefighters().values()),
					MessageType.BYE);
			decreaseLifePoints(lifePoints);
			return;
		}

		if (role == Role.Leader)
			iWasLeader = true;
		// Safe the old leader to know if leader changed
		oldleader = leader;
		// Check if firefighter is leader, follower or alone
		assignRole();

		System.out.println(" ");
		System.out.print("Firefighter " + id);
		System.out.print(" is " + role + ", ");
		if (role == Role.Leader) {
			System.out.print("followers are ");
			HashMap<Integer, GridPoint> friends = knowledge.getAllFirefighters();
			for (int friendId : friends.keySet()) {
				System.out.print(friendId + ", ");
			}
			System.out.print("Connection failed to: ");
			for (int friendID : friends.keySet()) {
				Firefighter friend = (Firefighter) Tools.getObjectOfTypeAt(grid, Firefighter.class,
						friends.get(friendID));
				if (friend == null)
					System.out.print(friendID + " ");
			}

		}
		if (role == Role.Follower) {
			System.out.print("leader is " + leader);
			Firefighter myleader = (Firefighter) Tools.getObjectOfTypeAt(grid, Firefighter.class,
					knowledge.getAllFirefighters().get(leader));
			if (myleader == null)
				System.out.print(" - Follower has no connection!");

		}

		// Action part (takes one step)
		boolean checkWeather = false;
		boolean leaderMoved = false;
		if (role == Role.Leader) {
			// checkWeather = RandomHelper.nextDouble() < 0.5;
			checkWeather = true;
		}

		if (knowledge.getFire(myPos)) {
			runOutOfFire(); // If firefighter knows that he is standing in the fire
			leaderMoved = true;
		} else if (checkWeather) {
			checkWeather();
		} else {
			moveOrExtinguish();
		}
		myPos = grid.getLocation(this);
		knowledge.addFirefighter(myPos, id);
		// Communication part (takes no time)

		int radioDist = knowledge.getRadioDistance();
		if (knowledge.getRadioDistPosition() != null) {
			radioDist = Tools.getDistance(grid.getLocation(this), knowledge.getRadioDistPosition());
			knowledge.setRadioDistance(radioDist);
			sendMessage(TransmissionMethod.Radio,
					new ArrayList<GridPoint>(Arrays.asList(knowledge.getFirefighter(leader))), MessageType.RADIOPING);
		}
		// split the follower in follower within the radio distance and the other ones.
		ArrayList<GridPoint> radioFollower = new ArrayList<>();
		ArrayList<GridPoint> satFollower = new ArrayList<>();
		HashMap<Integer, GridPoint> followers = knowledge.getAllFirefighters();
		// dont send message to himself
		followers.remove(id);
		for (GridPoint gridPoint : knowledge.getAllFirefighters().values()) {
			if (Tools.getDistance(grid.getLocation(this), gridPoint) <= radioDist) {
				radioFollower.add(gridPoint);
			} else {
				satFollower.add(gridPoint);
			}
		}
		//TODO: Everyone thinks forest is evrwhere and only send dead forest
		//TODO: Maybe leave group if I have no more bounty?

		// Handwaving to everyone in my sight
		//TODO: Don't send him this message if I already informed my leader about him
		sendMessage(TransmissionMethod.Radio, sightedFirefighters, MessageType.LEADER);

		// Distinguish between people in my radio sight and people who are not
		TransmissionMethod leaderMethod = (Tools.getDistance(grid.getLocation(this),
				knowledge.getFirefighter(leader)) <= radioDist) ? TransmissionMethod.Radio
						: TransmissionMethod.Satellite;
		TransmissionMethod oldleaderMethod = (Tools.getDistance(grid.getLocation(this),
				knowledge.getFirefighter(oldleader)) <= radioDist) ? TransmissionMethod.Radio
						: TransmissionMethod.Satellite;

		// Communicating to Follower and Leader
		if (role == Role.Follower) {
		//TODO: Only send things, if anything has changed
			// If there is a leader change:
			if (oldleader != leader) {
				// Update all the firefighters in my knowledge, that there is a new leader
				sendMessage(TransmissionMethod.Satellite,
						new ArrayList<GridPoint>(knowledge.getAllFirefighters().values()), MessageType.LEADER);
				// If I had a different old leader
				if (oldleader != this.id) {
					// Update old leader what I can see in case he informs the actual leader after
					// me which can lead to wrong knowledge in the new leader
					sendMessage(oldleaderMethod,
							new ArrayList<GridPoint>(Arrays.asList(knowledge.getFirefighter(oldleader))),
							MessageType.ISEE);
					// Update new leader to inform old leader
					sendMessage(leaderMethod, new ArrayList<GridPoint>(Arrays.asList(knowledge.getFirefighter(leader))),
							MessageType.OLDLEADER);
					// Send leader what I can see
					//TODO: Only send stuff that changed
					sendMessage(leaderMethod, new ArrayList<GridPoint>(Arrays.asList(knowledge.getFirefighter(leader))),
							MessageType.ISEE);
				} else if (oldleader == this.id) {
					// in case the fire fighter was degraded from leader to follower, he has to send
					// the new leader all the information
					if (iWasLeader)
						sendMessage(leaderMethod,
								new ArrayList<GridPoint>(Arrays.asList(knowledge.getFirefighter(leader))),
								MessageType.ALL);
					// in case I was alone before, just send leader what I see, to not feed him with
					// false information
					else {
						sendMessage(leaderMethod,
								new ArrayList<GridPoint>(Arrays.asList(knowledge.getFirefighter(leader))),
								MessageType.ISEE);
					}
				}
				// Send leader what I can see if he hasn't changed
			} else
				//TODO: Only send stuff that changed
				sendMessage(leaderMethod, new ArrayList<GridPoint>(Arrays.asList(knowledge.getFirefighter(leader))),
						MessageType.ISEE);

		} else if (role == Role.Leader) {
			// Don't send message to myself
			//TODO Update followers only with additional knowledge leader gained in the last tick but send new followers all knowledge
			//TODO Send followers wind information in case of change
			sendMessage(TransmissionMethod.Satellite, satFollower, MessageType.ALL);
			sendMessage(TransmissionMethod.Radio, radioFollower, MessageType.ALL);
			// try to find the max radio distance. will only cost something if higher
			// distance was found
			sendMessage(TransmissionMethod.Radio, satFollower, MessageType.RADIOPING);
		}
	}

	/** Movement routine of a firefighter */
	private void moveOrExtinguish() {
		// TODO do not attack the fire from the front/against the wind direction
		double result[] = findDirection2NearestFire();
		double directionToFire = result[0];
		double distance = result[1];

		if (distance == 1) // If fire is exactly at the extinguishingDistance
		{
			GridPoint myPos = grid.getLocation(this);
			GridPoint firePos = Tools.dirToCoord(directionToFire, myPos);
			GridPoint sightPos = Tools.dirToCoord(velocity.direction, myPos);
			// System.out.println("x:" + firePos.getX() + " y:" + firePos.getY());
			// System.out.println("x:" + sightPos.getX() + " y:" + sightPos.getY());
			if (firePos.equals(sightPos)) {
				extinguishFire(directionToFire);
			} // Extinguish the fire in the direction of heading
			else {
				velocity.direction = directionToFire;
			} // Turn to fire
		} else if (distance > 1) {
			tryToMove(directionToFire);
		} // If fire is more than extinguishingDistance away
		else // Otherwise explore randomly
		{
			velocity.direction = RandomHelper.nextDoubleFromTo(0, 360);
			tryToMove(velocity.direction);
		}
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
				if (i != 0 && j != 0)
					checkCell(new GridPoint(myPos.getX() + i, myPos.getY() + j));
			}
		}
	}

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

					if (!knowledge.getFire(pos)) {
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

			boolean hasFire = knowledge.getFire(newPos);
			boolean hasFirefighter = hasFirefighter(newPos);

			if (!hasFire && !hasFirefighter) // Make sure that the cell is not on fire, and is not occupied by another
												// firefighter
			{
				grid.moveTo(this, newPos.toIntArray(null));
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
			int dist = Tools.getDistance(myPos, p);
			// Determine if the fire is closest. If so, update distance and direction
			// accordingly
			if (dist < minDist) {
				minDist = dist;
				direction = Tools.getAngle(myPos, p);
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
		boolean hasFirefighter = (Tools.getObjectOfTypeAt(grid, Firefighter.class, pos) != null);
		boolean hasFire = (Tools.getObjectOfTypeAt(grid, Fire.class, pos) != null);
		boolean hasForest = (Tools.getObjectOfTypeAt(grid, Forest.class, pos) != null);

		if (hasFire) {
			knowledge.addFire(pos);
		} else {
			knowledge.removeFire(pos);
		}

		if (hasFirefighter) {
			// only send the new firefighter your ID and location
			// Don't add him into your knowledge -> He will put his stuff into your
			// knowledge anyway
			sightedFirefighters.add(pos);
			// knowledge.addFirefighter(pos);
		} else {
			// knowledge.removeFirefighter(pos);
		}

		if (hasForest) {
			knowledge.addForest(pos);
		} else {
			knowledge.removeForest(pos);
		}
		newInfo = true;
	}

	/**
	 * Send a message to a given set of recipients using a given communication
	 * method
	 * 
	 * @param transmissionMethod
	 *            - a method to used for message sending
	 * @param recipientLocations
	 *            - a list of locations of firefighters to send the message to
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
		case ISEE:
			message.setContent(this.knowledge.WhatIsee2String(myPos, sightRange));
			break;
		case LEADER:
			message.setContent("HW " + leaderLocation.getX() + " " + leaderLocation.getY() + " " + this.leader + ";");
			break;
		case OLDLEADER:
			message.setContent(
					"HW " + oldleaderLocation.getX() + " " + oldleaderLocation.getY() + " " + this.leader + ";");
		case BYE:
			message.setContent("Dead Firefighter " + id);
		case RADIOPING:
			// only leader will send the ping
			message.setContent("P" + myPos.getX() + " " + myPos.getY());
			break;
		default:
			break;
		}
		int messageCost = message.getCost();
		int radioRange = params.getInteger("firefighter_radio_range");
		int satelliteCostMultiplier = params.getInteger("firefighter_satellite_cost_multiplier");
		// counts the general message sending actions
		((IGlobalCounter) context.getObjects(MsgMethodCounter.class).get(0)).incrementCounter();

		for (GridPoint recipientLocation : recipientLocations) {
			Firefighter recipient = (Firefighter) Tools.getObjectOfTypeAt(grid, Firefighter.class, recipientLocation);

			if (recipient != null) // First of all, if the recipient is there at all
			{
				if (transmissionMethod == TransmissionMethod.Radio
						&& Tools.getDistance(myPos, recipientLocation) <= radioRange) // If using the radio, and the
																						// recipient is within range
				{
					if (getBounty() >= messageCost) {
						recipient.recieveMessage(message); // Deliver message
						// bounty -= messageCost; // Pay for the message
						((IGlobalCounter) context.getObjects(MessageSentCounter.class).get(0)).incrementCounter();
						((IGlobalCounter) context.getObjects(RadioMsgCounter.class).get(0)).incrementCounter();
						((AvgMessageLength) context.getObjects(AvgMessageLength.class).get(0))
								.addMessage(message.getContent());
					}
				} else if (transmissionMethod == TransmissionMethod.Satellite) {
					double globalMessageCost = messageCost * satelliteCostMultiplier; // A cost to send a message
																						// through the satellite

					if (getBounty() >= globalMessageCost) {
						recipient.recieveMessage(message); // Deliver message

						// bounty -= globalMessageCost; // Pay for the message
						((IGlobalCounter) context.getObjects(MessageSentCounter.class).get(0)).incrementCounter();
						((AvgMessageLength) context.getObjects(AvgMessageLength.class).get(0))
								.addMessage(message.getContent());
					}
				}
			}
		}

		newInfo = false; // All the new information was sent, over now
	}

	/**
	 * Receive message
	 * 
	 * @param message
	 *            - a message
	 */
	public void recieveMessage(Message message) {
		Knowledge receivedKnowledge = new Knowledge(this.context);
		receivedKnowledge.convertFromString(message.getContent());
		knowledge.updateFromKnowledge(receivedKnowledge);
	}
	
	/**
	 * This methods returns the Gridpoint which should be reached next by the Firefighter
	 * (The next task)
	 * @param f current firefighter
	 * @return next Task
	 */
	private GridPoint evaluate(int id) {
		GridPoint Pos = knowledge.getFirefighter(id);
		// GridPoint myPos = grid.getLocation(this); grid.getLocation (Firefighter f)
		int minDist = Integer.MAX_VALUE;
		GridPoint temp = new GridPoint(null);
		for (GridPoint p : knowledge.getAllFire()) // For all the fires in the firefighter's knowledge
		{
			int dist = Tools.getDistance(Pos, p);
			// Determine if the fire is closest. If so, update distance and direction
			// accordingly
			if (dist < minDist) {
				temp = p;
				minDist = dist;
			}
		}	
		
		return temp;
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

			for (Object o : rains) {
				knowledge.addRain(grid.getLocation(o));
			}
		}
	}

	/** The method for removal of the object */
	@ScheduledMethod(shuffle = false) // Prevent call order shuffling
	public void remove() {
		if (!Tools.isLastTick()) {
			context.remove(this);
		} // Do not include this in the executeEndActions()
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
