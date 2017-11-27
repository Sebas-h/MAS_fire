package globalcounter;

public class TaskCompleteCounter implements IGlobalCounter {
	private int counter;

	public TaskCompleteCounter() {
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
