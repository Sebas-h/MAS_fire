package globalcounter;

public class ExtinguishedFireCounter1 implements IGlobalCounter {
	
	private int counter;
	
	public ExtinguishedFireCounter1() {
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
