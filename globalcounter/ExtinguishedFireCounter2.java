package globalcounter;

public class ExtinguishedFireCounter2 implements IGlobalCounter {
	
	private int counter;
	
	public ExtinguishedFireCounter2() {
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
