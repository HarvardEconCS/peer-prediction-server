package edu.harvard.econcs.peerprediction.analysis;

import java.util.regex.Pattern;

public class MatchStrings {

	static final Pattern experimentStart = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) (.*) (.*) started");
	static final Pattern priorPattern = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Prior is prob=(.*), worlds=(.*)");
	static final Pattern generalInfo = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) General information sent: numPlayers=([0-9]+), "
					+ "numRounds=([0-9]+), playerNames=(.*), paymentRule=(.*), signalList=(.*)");
	static final Pattern experimentRoundStart = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round ([0-9]+) started");
	static final Pattern experimentRoundFinish = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round ([0-9]+) finished");
	static final Pattern experimentFinish = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) (.*) (.*) finished");

	static final Pattern roundStart = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round (\\d+) started");
	static final Pattern gotSignal = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) got signal ([A-Z]{2})");
	static Pattern choseReport = null;
	
	static final Pattern chosenReport1 = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) chose report ([A-Z]{2}) \\(radio: [0-9]\\)");
	static final Pattern chosenReport2 = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) chose report ([A-Z]{2})");
	
	
	static final Pattern roundResult = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round result is (.*)");
	static final Pattern roundEnd = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round ([0-9]+) finished");
	static final Pattern getBonus = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) gets bonus ([0-9.]+)");
	static final Pattern noBonus = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) killed, no bonus");
	static final Pattern chosenWorld = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Chosen world is (.*)");;
	//	private static Pattern killed = Pattern
	// .compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) killed, because disconnected for ([0-9]+) milliseconds");
	// static final Pattern fakeReport = Pattern
	// .compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) killed, put in fake report ([A-Z]{2})");

	static final Pattern timeStamp = Pattern
			.compile("([0-9]{2}):([0-9]{2}).([0-9]{3})");

	
}
