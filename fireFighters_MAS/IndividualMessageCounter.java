package fireFighters_MAS;

public class IndividualMessageCounter {

	// MessageCounter - Test
	private int countPos;
	private int countHW;
	private int countTask;
	private int countLU;
	private int countInfoToL;
	private int countWind;
	private int countBye;
	private int countNotRecieved;

	public void increment(MessageType messageType) {
		switch (messageType) {
		case ALL:
			incrementcountLU();
			break;
		case POSITION:
			incrementcountPos();
			break;
		case ISEENEW:
			incrementcountInfoToL();
			break;
		case ISEE:
			incrementcountInfoToL();
			break;
		case LEADER:
			incrementcountHW();
			break;
		case OLDLEADER:
			incrementcountHW();
			break;
		case BYE:
			incrementcountBye();
			break;
		case TASK:
			incrementcountTask();
			break;
		case WIND:
			incrementcountWind();
			break;
		default:
			break;
		}
	}

	public void incrementcountNotRecieved() {
		this.countNotRecieved++;
	}

	public void incrementcountPos() {
		this.countPos++;
	}

	public void incrementcountHW() {
		this.countHW++;
	}

	public void incrementcountLU() {
		this.countLU++;
	}

	public void incrementcountInfoToL() {
		this.countInfoToL++;
	}

	public void incrementcountWind() {
		this.countWind++;
	}

	public void incrementcountBye() {
		this.countBye++;
	}

	public void incrementcountTask() {
		this.countTask++;
	}

	public IndividualMessageCounter() {
		countPos = 0;
		countHW = 0;
		countTask = 0;
		countLU = 0;
		countInfoToL = 0;
		countWind = 0;
		countBye = 0;
	}

	public void printCounter() {
		System.out.println("Position " + countPos);
		System.out.println("HW " + countHW);
		System.out.println("Task " + countTask);
		System.out.println("LeaderUpdate " + countLU);
		System.out.println("ISEE " + countInfoToL);
		System.out.println("Wind " + countWind);
		System.out.println("Bye " + countBye);
		System.out.println("Not Recieved " + countNotRecieved);
	}

}
