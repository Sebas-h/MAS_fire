package globalcounter;

public class TaskReceiveCounter implements IGlobalCounter {
	private int counter;

	public TaskReceiveCounter() {
		counter = 0;
	}

	@Override
	public int getCounter() {
		return counter;
	}

	@Override
	public void incrementCounter() {
		counter++;
	}

}
