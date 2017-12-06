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

	private double countPosCost;
	private double countHWCost;
	private double countTaskCost;
	private double countLUCost;
	private double countInfoToLCost;
	private double countWindCost;
	private double countByeCost;
	private double countNotRecievedCost;

	
	public void increment(MessageType messageType,int cost) {
		switch (messageType) {
		case ALL:
			incrementcountLU();
			countLUCost += cost;
			break;
		case POSITION:
			incrementcountPos();
			countPosCost += cost;
			break;
		case ISEENEW:
			incrementcountInfoToL();
			countInfoToLCost += cost;
			break;
		case ISEE:
			incrementcountInfoToL();
			countInfoToLCost += cost;
			break;
		case LEADER:
			incrementcountHW();
			countHWCost += cost;
			break;
		case OLDLEADER:
			incrementcountHW();
			countHWCost += cost;
			break;
		case BYE:
			incrementcountBye();
			countByeCost += cost;
			break;
		case TASK:
			incrementcountTask();
			countTaskCost += cost;
			break;
		case WIND:
			incrementcountWind();
			countWindCost += cost;
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
		System.out.println("Position " + countPos + "Cost "+countPosCost);
		System.out.println("HW " + countHW + "Cost "+countHWCost);
		System.out.println("Task " + countTask + "Cost "+countTaskCost);
		System.out.println("LeaderUpdate " + countLU + "Cost "+countLUCost);
		System.out.println("ISEE " + countInfoToL + "Cost "+countInfoToLCost);
		System.out.println("Wind " + countWind + "Cost "+countWindCost);
		System.out.println("Bye " + countBye + "Cost "+countByeCost);
		System.out.println("Not Recieved " + countNotRecieved);
	}

}
