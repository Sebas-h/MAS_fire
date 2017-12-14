package fireFighters_MAS;

import repast.simphony.space.grid.GridPoint;

public class Task {
	private GridPoint location;
	private Integer reward;
	private Integer senderID;
	private Integer receiverID;

	public Task(GridPoint location, Integer reward, Integer senderID, Integer receiverID) {
		this.senderID = senderID;
		this.location = location;
		this.reward = reward;
		this.receiverID = receiverID;
	}

	public GridPoint getGridPoint() {
		return this.location;
	}

	public int getX() {
		return this.location.getX();
	}

	public int getY() {
		return this.location.getY();
	}

	public Integer getReward() {
		return this.reward;
	}

	public Integer getSenderID() {
		return this.senderID;
	}

	public Integer getReceiverID() {
		return this.receiverID;
	}
}
