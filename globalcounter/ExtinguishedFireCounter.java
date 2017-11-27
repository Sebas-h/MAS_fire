package globalcounter;

public class ExtinguishedFireCounter implements IGlobalCounter {
	
	private int counter;
	
	public ExtinguishedFireCounter() {
		this.counter = 0;
	}
	
	@Override
	public int getCounter() {
		return this.counter;
	}

	@Override
	public void incrementCounter() {
		this.counter++;
	}

}
