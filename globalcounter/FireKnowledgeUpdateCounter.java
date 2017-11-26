package globalcounter;

public class FireKnowledgeUpdateCounter implements IGlobalCounter{
	
	private int counter;
	
	public FireKnowledgeUpdateCounter() {
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
