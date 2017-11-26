package globalcounter;

public class AvgMessageLength {
	private int length;
	private int counter;

	public AvgMessageLength() {
		this.counter = 0;
		this.length = 0;
	}

	public int getAverage() {
		if (counter != 0)
			return length / counter;
		return 0;
	}

	public void addMessage(String s) {
		counter++;
		length += s.length();
	}

}
