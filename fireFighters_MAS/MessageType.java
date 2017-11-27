package fireFighters_MAS;

public enum MessageType {
	//ME to send own location and ID
	//ALL to send everything in my knowledge
	//ISEE to send everything in my sightrange
	//BYE to send everyone in knowledge that you died
	LEADER,OLDLEADER, ALL, ISEE, BYE,TASK,WIND
}
