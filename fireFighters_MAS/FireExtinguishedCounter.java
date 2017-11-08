package fireFighters_MAS;

public class FireExtinguishedCounter {
	private int counter;
	
	public FireExtinguishedCounter() {
		this.counter = 0;
	}
	
	public int getCounter() {
		return counter;
	}
	
	// Adds 1 to current value of counter;
	public void incrementCounter() {
		this.counter += 1;
	}
}
