package globalcounter;

public class MsgMethodCounter implements IGlobalCounter {

	private int counter;

	public MsgMethodCounter() {
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