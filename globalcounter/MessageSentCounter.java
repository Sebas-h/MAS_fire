package globalcounter;

public class MessageSentCounter implements IGlobalCounter {
	
	private int counter;
	
	public MessageSentCounter() {
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
