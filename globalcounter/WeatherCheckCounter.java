package globalcounter;

public class WeatherCheckCounter implements IGlobalCounter {
	
	private int counter;
	
	public WeatherCheckCounter() {
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
