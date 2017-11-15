package globalcounter;

public class RadioMsgCounter implements IGlobalCounter {

	private int counter;

	public RadioMsgCounter() {
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