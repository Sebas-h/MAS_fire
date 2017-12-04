package fireFighters_MAS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import globalcounter.FireKnowledgeUpdateCounter;
import globalcounter.ForestKnowledgeUpdateCounter;
import globalcounter.IGlobalCounter;
import globalcounter.MessageSentCounter;
import globalcounter.TaskAcceptedCounter;
import globalcounter.TaskReceiveCounter;
import repast.simphony.context.Context;
import repast.simphony.space.grid.GridPoint;

/**
 * A class describing the firefighter's knowledge of the world It contains the
 * relevant methods for knowledge manipulation
 * 
 * @author Kirill Tumanov, 2015-2017
 */
public class Knowledge {
	// Local variables declaration
	private LinkedHashMap<GridPoint, Integer> fireKnowledge; // A hash with locations, and corresponding flags of fire
																// presence in the knowledge
	private LinkedHashMap<GridPoint, Boolean> forestKnowledge; // A hash with locations, and corresponding flags of
																// forest presence in the knowledge
	private LinkedHashMap<Integer, GridPoint> firefighterKnowledge; // A hash with locations, and corresponding flags of
																	// firefighter presence in the knowledge
	private LinkedHashMap<GridPoint, Boolean> rainKnowledge; // A hash with locations, and corresponding flags of rain
																// presence in the knowledge
	private LinkedHashMap<Integer, GridPoint> givenTasks;
	private ArrayList<Integer> deadFirefighterKnowledge;
	// private ArrayList<Integer> firefighterID; //Storage for all firefighter ID's
	// of my friends
	private Velocity windVelocity; // A knowledge about the wind velocity
	private Context<Object> context;
	private GridPoint currentTask;
	private int radioDistance;
	private GridPoint radioDistPosition;
	private int newBounty;

	/** Custom constructor */
	public Knowledge(Context<Object> context) {
		// Initialize local variables
		this.fireKnowledge = new LinkedHashMap<GridPoint, Integer>();
		this.forestKnowledge = new LinkedHashMap<GridPoint, Boolean>();
		this.firefighterKnowledge = new LinkedHashMap<Integer, GridPoint>();
		this.rainKnowledge = new LinkedHashMap<GridPoint, Boolean>();
		this.givenTasks = new LinkedHashMap<Integer, GridPoint>();
		this.windVelocity = null;
		this.context = context;
		this.newBounty = 0;
	}

	public GridPoint getCurrentTask() {
		return this.currentTask;
	}

	public void setCurrentTask(GridPoint currentTask) {
		this.currentTask = currentTask;
	}

	public int getRadioDistance() {
		return radioDistance;
	}

	public void setRadioDistance(int radioDistance) {
		this.radioDistance = radioDistance;
	}

	public GridPoint getRadioDistPosition() {
		return radioDistPosition;
	}

	public void setRadioDistPosition(GridPoint radioDistPosition) {
		this.radioDistPosition = radioDistPosition;
	}

	/**
	 * Get positions of all the fire objects from the current knowledge
	 * 
	 * @return a set of positions of all the fire objects
	 */
	public ArrayList<GridPoint> getAllFire() {
		ArrayList<GridPoint> returnArray = new ArrayList<>();

		if (fireKnowledge != null) {
			for (GridPoint p : fireKnowledge.keySet()) {
				if (fireKnowledge.get(p) != null || fireKnowledge.get(p) != 0) {
					returnArray.add(p);
				}
			}
		}

		return returnArray;
	}

	/**
	 * Get all the forest objects from the current knowledge
	 * 
	 * @return a set of positions of all the forest objects
	 */
	public ArrayList<GridPoint> getAllForest() {
		ArrayList<GridPoint> returnArray = new ArrayList<>();

		if (forestKnowledge != null) {
			for (GridPoint p : forestKnowledge.keySet()) {
				if (forestKnowledge.get(p) != null) {
					returnArray.add(p);
				}
			}
		}

		return returnArray;
	}

	/**
	 * Get all the firefighter objects from the current knowledge
	 * 
	 * @return a set of positions of all the firefighter objects
	 */
	public HashMap<Integer, GridPoint> getAllFirefighters() {
		HashMap<Integer, GridPoint> returnHashMap = new HashMap<Integer, GridPoint>();

		if (firefighterKnowledge != null) {
			for (Integer id : firefighterKnowledge.keySet()) {
				if (firefighterKnowledge.get(id) != null) {
					returnHashMap.put(id, firefighterKnowledge.get(id));
				}
			}
		}

		return returnHashMap;
	}

	/**
	 * Get all the rain objects from the current knowledge
	 * 
	 * @return a set of positions of all the rain objects
	 */
	public ArrayList<GridPoint> getAllRain() {
		ArrayList<GridPoint> returnArray = new ArrayList<>();

		if (rainKnowledge != null) {
			for (GridPoint p : rainKnowledge.keySet()) {
				if (rainKnowledge.get(p) != null) {
					returnArray.add(p);
				}
			}
		}

		return returnArray;
	}

	// overloading this method may not be neccesarry
	public boolean addFire(GridPoint pos) {
		return addFire(pos, 0);
	}

	/**
	 * Add a position of a fire to the current knowledge
	 * 
	 * @param pos
	 *            - position to put a fire to
	 * @return 0 - if this fire is already known, 1 - if the fire was unknown and
	 *         was added to the knowledge
	 */
	public boolean addFire(GridPoint pos, int dur) {
		for (GridPoint p : fireKnowledge.keySet()) {
			// allready knows fire
			if (pos.equals(p)) {
				return false;
			}
		}

		fireKnowledge.put(pos, 1);

		// Increment successful knowledge update about fire:
		((IGlobalCounter) context.getObjects(FireKnowledgeUpdateCounter.class).get(0)).incrementCounter();

		return true;
	}

	/**
	 * Add a forest object to a given position in a current knowledge
	 * 
	 * @param pos
	 *            - position at which the forest object should be added
	 * @return 0 - if this forest is already known, 1 - if the forest was unknown
	 *         and was added to the knowledge
	 */
	public boolean addForest(GridPoint pos) {
		for (GridPoint p : forestKnowledge.keySet()) {
			if (pos.equals(p)) {
				return false;
			}
		}

		forestKnowledge.put(pos, true);

		// Increment successful knowledge update about forest:
		((IGlobalCounter) context.getObjects(ForestKnowledgeUpdateCounter.class).get(0)).incrementCounter();

		return true;
	}

	/**
	 * Add the information that there is no more forest on a gridpoint
	 * 
	 * @param pos
	 *            -position of the empty gridpoint
	 * @return true if burned forest is new, false if it is already in the knowledge
	 */
	public boolean addBurnedForest(GridPoint pos) {
		for (GridPoint p : forestKnowledge.keySet()) {
			if (pos.equals(p)) {
				return false;
			}
		}
		forestKnowledge.put(pos, false);
		return true;
	}

	/**
	 * Looks if burnedForest information is in knowledge
	 * 
	 * @param Gridpoint
	 *            of the information
	 * @return true if there is no forest false if it is not in the knowledge or the
	 *         tree lives in the knowledge
	 */
	public boolean getBurnedForest(GridPoint pos) {
		for (GridPoint p : forestKnowledge.keySet()) {
			if (p.equals(pos) && !forestKnowledge.get(p))
				return true;
		}
		return false;
	}

	/**
	 * Looks through all entries in forestknowledge to get already burned down
	 * forest points
	 * 
	 * @return all gridpoint in own forest knowledge, that have value false
	 */
	public ArrayList<GridPoint> getAllBurnedForest() {
		ArrayList<GridPoint> returnArray = new ArrayList<>();

		if (forestKnowledge != null) {
			for (GridPoint p : forestKnowledge.keySet()) {
				if (!forestKnowledge.get(p)) {
					returnArray.add(p);
				}
			}
		}

		return returnArray;
	}

	/**
	 * Add a firefighter object to a given position in a current knowledge
	 * 
	 * @param pos
	 *            - position at which the firefighter object should be added
	 * @return 0 - if this firefighter is already known, 1 - if the firefighter was
	 *         unknown and was added to the knowledge
	 */
	public boolean addFirefighter(GridPoint pos, Integer ID) {
		for (Integer id : firefighterKnowledge.keySet()) {
			// Update position in case he moved without noticing
			if (id.equals(ID)) {
				firefighterKnowledge.put(ID, pos);
				return false;
			}
		}
		firefighterKnowledge.put(ID, pos);
		return true;
	}

	public boolean addTask(Integer ID, GridPoint pos) {
		for (Integer id : givenTasks.keySet()) {
			if (id.equals(ID)) {
				givenTasks.put(ID, pos);
				return false;
			}
		}
		givenTasks.put(ID, pos);
		return true;
	}

	public GridPoint getTask(Integer ID) {
		for (Integer id : givenTasks.keySet()) {
			if (id.equals(ID)) {
				return givenTasks.get(id);
			}
		}
		return null;
	}

	/**
	 * Add the ID of a firefighter to the list of known IDs
	 * 
	 * @param id
	 *            - id of the firefighter
	 * @return 0 - if this firefighter is already known, 1 - if the firefighter was
	 *         unknown and was added to the knowledge
	 */
	// public boolean addID(GridPoint pos, int id)
	// {
	// for (GridPoint p : IDKnowledge.keySet())
	// {
	// if (pos.equals(p)) {
	// if (IDKnowledge.get(p)==id) { return false; }
	// else if (IDKnowledge.containsValue(id)) {
	//
	// }
	// return false;
	// }
	//
	// }
	//
	// firefighterKnowledge.put(pos, true);
	// return true;
	//
	// //if (D.contains(id)) return false;
	// //else firefighterID.add(id);
	// //return true;
	// }
	/**
	 * Add a rain object to a given position in a current knowledge
	 * 
	 * @param pos
	 *            - position at which the rain object should be added
	 * @return 0 - if this rain is already known, 1 - if the rain was unknown and
	 *         was added to the knowledge
	 */
	public boolean addRain(GridPoint pos) {
		for (GridPoint p : rainKnowledge.keySet()) {
			if (pos.equals(p)) {
				return false;
			}
		}

		rainKnowledge.put(pos, true);
		return true;
	}

	/**
	 * Remove fire at a given position from a current knowledge
	 * 
	 * @param pos
	 *            - position from which the fire object should be removed
	 */
	public void removeFire(GridPoint pos) {
		for (GridPoint p : fireKnowledge.keySet()) {
			if (pos.equals(p)) {
				fireKnowledge.put(p, 0);
				return;
			}
		}
	}

	/**
	 * Remove forest at a given position from a current knowledge
	 * 
	 * @param pos
	 *            - position from which the forest object should be removed
	 */
	public void removeForest(GridPoint pos) {
		for (GridPoint p : forestKnowledge.keySet()) {
			if (pos.equals(p)) {
				forestKnowledge.put(p, null);
				return;
			}
		}
	}

	/**
	 * Remove firefighter at a given position from a current knowledge
	 * 
	 * @param pos
	 *            - position from which the firefighter object should be removed
	 */
	public void removeFirefighter(Integer ID) {
		for (Integer id : firefighterKnowledge.keySet()) {
			if (ID.equals(id)) {
				firefighterKnowledge.remove(id);
				return;
			}
		}
	}

	public void increaseFireScore() {
		for (GridPoint p : fireKnowledge.keySet()) {
			if (fireKnowledge.get(p) != 0) {
				fireKnowledge.replace(p, fireKnowledge.get(p) + 1);
			}
		}
	}

	/**
	 * Remove rain at a given position from a current knowledge
	 * 
	 * @param pos
	 *            - position from which the rain object should be removed
	 */
	public void removeRain(GridPoint pos) {
		for (GridPoint p : rainKnowledge.keySet()) {
			if (pos.equals(p)) {
				rainKnowledge.put(p, null);
				return;
			}
		}
	}

	public void removeAllRain() {
		for (GridPoint p : rainKnowledge.keySet()) {
			removeRain(p);
		}
	}

	/**
	 * Encode the knowledge to a string
	 * 
	 * @return the string representation of the knowledge
	 */
	public String convert2String() {
		// TODO Form a string out of the knowledge at hand - Add own code
		String str = "";

		for (GridPoint pos : getAllFire()) {
			str += "F " + pos.getX() + " " + pos.getY() + ";";
		}
		for (Map.Entry<Integer, GridPoint> entry : firefighterKnowledge.entrySet()) {
			str += "FF " + entry.getValue().getX() + " " + entry.getValue().getY() + " " + entry.getKey() + ";";
		}
		return str;
	}

	/**
	 * Encode the knowledge about my sight range cells to a string
	 * 
	 * @return the string representation of what I can see
	 */
	public String WhatIsee2String(GridPoint myPos, int sightRange) {
		String str = "";
		for (GridPoint pos : getAllFire()) {
			if (canBeSeen(myPos, pos, sightRange)) {
				str += " " + pos.getX() + " " + pos.getY() + ";";
			}
		}
		// for (Map.Entry<Integer, GridPoint> entry : firefighterKnowledge.entrySet()) {
		// GridPoint pos = entry.getValue();
		// if (canBeSeen(myPos, pos, sightRange)) {
		// str += "Firefighters " + pos.getX() + " " + pos.getY() + " " + entry.getKey()
		// + ";";
		// }
		// }
		for (GridPoint pos : getAllBurnedForest()) {
			if (canBeSeen(myPos, pos, sightRange)) {
				str += "NF " + pos.getX() + " " + pos.getY() + ";";
			}
		}
		return str;
	}

	/**
	 * Checks if the GridPoint pos can be seen from position myPos
	 * 
	 * @return true if GridPoint pos can be seen, false if not
	 */
	public boolean canBeSeen(GridPoint myPos, GridPoint pos, int sightRange) {
		if (java.lang.Math.abs(pos.getX() - myPos.getX()) < sightRange
				&& java.lang.Math.abs(pos.getY() - myPos.getY()) < sightRange)
			return true;
		return false;
	}

	/**
	 * Decode knowledge from the string message
	 * 
	 * @param str
	 *            - a string representation of knowledge
	 */
	public void convertFromString(String str) {
		String[] arr1 = str.split(";");

		for (int i = 0; i < arr1.length; i++) {
			String[] arr2 = arr1[i].split(" ");
			if (arr2[0].equals("F")) {
				addFire(new GridPoint(Integer.parseInt(arr2[1]), Integer.parseInt(arr2[2])));
			}
			if (arr2[0].equals("B")) {
				newBounty = newBounty + Integer.parseInt(arr2[1]);
			}
			if (arr2[0].equals("NF")) {
				removeFire(new GridPoint(Integer.parseInt(arr2[1]), Integer.parseInt(arr2[2])));
				addBurnedForest(new GridPoint(Integer.parseInt(arr2[1]), Integer.parseInt(arr2[2])));
			}
			if (arr2[0].equals("FF")) {
				addFirefighter(new GridPoint(Integer.parseInt(arr2[1]), Integer.parseInt(arr2[2])),
						Integer.parseInt(arr2[3]));
			}
			// if (arr2[0].equals("ID")) {addID(Integer.parseInt(arr2[1]));}
			if (arr2[0].equals("HW")) {
				addFirefighter(new GridPoint(Integer.parseInt(arr2[1]), Integer.parseInt(arr2[2])),
						Integer.parseInt(arr2[3]));
			}
			if (arr2[0].equals("DF")) {
				removeFirefighter(Integer.parseInt(arr2[1]));
				deadFirefighterKnowledge.add(Integer.parseInt(arr2[1]));
			}

			if (arr2[0].equals("T")) {
				setCurrentTask(new GridPoint(Integer.parseInt(arr2[1]), Integer.parseInt(arr2[2])));
				// increment the counter for the received and accepted tasks:
				((IGlobalCounter) context.getObjects(TaskReceiveCounter.class).get(0)).incrementCounter();
				((IGlobalCounter) context.getObjects(TaskAcceptedCounter.class).get(0)).incrementCounter();
			}
			if (arr2[0].equals("W")) {
				windVelocity = new Velocity(Double.parseDouble(arr2[1]), Double.parseDouble(arr2[1]));
			}
		}
	}

	/**
	 * Update the knowledge of the firefighter by taking info from a given knowledge
	 * 
	 * @param k
	 *            - knowledge to update from
	 */
	public void updateFromKnowledge(Knowledge k) {
		for (GridPoint pos : k.getAllFire()) {
			addFire(pos, k.getFire(pos));
			// Increment successful knowledge update about fire:
			// ((IGlobalCounter)
			// context.getObjects(FireKnowledgeUpdateCounter.class).get(0)).incrementCounter();
		}
		for (GridPoint pos : k.getAllForest()) {
			if (k.getForest(pos))
				addForest(pos);
			else
				addBurnedForest(pos);
			// Increment successful knowledge update about forest:
			((IGlobalCounter) context.getObjects(ForestKnowledgeUpdateCounter.class).get(0)).incrementCounter();
		}
		for (Map.Entry<Integer, GridPoint> entry : k.getAllFirefighters().entrySet()) {
			addFirefighter(entry.getValue(), entry.getKey());
		}
		for (GridPoint pos : k.getAllRain()) {
			addRain(pos);
		}
		if (k.getCurrentTask() != null) {
			setCurrentTask(k.getCurrentTask());
		}
		this.windVelocity = k.windVelocity;

		newBounty = newBounty + k.getNewBounty();

		for (Integer id : k.deadFirefighterKnowledge) {
			removeFirefighter(id);
		}
	}

	// Local getters
	/**
	 * gets the duration the fire is already known ==0 if no fire
	 * 
	 * @param p
	 *            - a cell to check
	 * @return 1 if the object exists in the knowledge, 0 - if not
	 */
	public int getFire(GridPoint p) {
		if (fireKnowledge.get(p) == null) {
			return 0;
		}

		return fireKnowledge.get(p);
	}

	/**
	 * Get knowledge about a presence of forest in a given cell
	 * 
	 * @param p
	 *            - a cell to check
	 * @return 1 if the object exists in the knowledge, 0 - if not
	 */
	public boolean getForest(GridPoint p) {
		if (forestKnowledge.get(p) == null) {
			return false;
		}

		return forestKnowledge.get(p);
	}

	/**
	 * Get knowledge about a presence of firefighter in a given cell
	 * 
	 * @param p
	 *            - a cell to check
	 * @return 1 if the object exists in the knowledge, 0 - if not
	 */
	/*
	 * public boolean getFirefighter(int id) { if (firefighterKnowledge.get(id) ==
	 * null) { return false; } return true; // return
	 * firefighterKnowledge.values(id); }
	 */

	public GridPoint getFirefighter(int id) {
		return firefighterKnowledge.get(id);
	}

	/**
	 * Get knowledge about a presence of rain in a given cell
	 * 
	 * @param p
	 *            - a cell to check
	 * @return 1 if the object exists in the knowledge, 0 - if not
	 */
	public boolean getRain(GridPoint p) {
		if (rainKnowledge.get(p) == null) {
			return false;
		}

		return rainKnowledge.get(p);
	}

	/**
	 * Get wind velocity
	 * 
	 * @return - the velocity
	 */
	public Velocity getWindVelocity() {
		return windVelocity;
	}

	// Local setters
	/**
	 * Set wind velocity
	 * 
	 * @param vel
	 *            - the velocity
	 */
	public void setWindVelocity(Velocity vel) {
		windVelocity = vel;
	}

	public int getNewBounty() {
		return newBounty;
	}

	public void setNewBounty(int newBounty) {
		this.newBounty = newBounty;
	}
}
