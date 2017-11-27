package globalcounter;

public class ForestKnowledgeUpdateCounter implements IGlobalCounter {
	
	private int counter;
	
	public ForestKnowledgeUpdateCounter() {
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
