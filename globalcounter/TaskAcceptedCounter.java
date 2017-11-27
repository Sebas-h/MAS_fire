package globalcounter;

public class TaskAcceptedCounter implements IGlobalCounter {
	private int counter;
	public TaskAcceptedCounter() {
		counter=0;
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
