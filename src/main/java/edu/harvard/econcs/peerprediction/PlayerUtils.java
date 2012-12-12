package edu.harvard.econcs.peerprediction;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.harvard.econcs.peerprediction.TestPlayer.WrongStateException;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.server.MessageException;

public class PlayerUtils {

	public static void sendGeneralInfo(HITWorker worker, int nPlayers, int nRounds, 
			String[] playerNames, String yourName, double[] paymentArray) {		
		
		Map<String, Object> msg = new HashMap<String, Object>();
		msg.put("status"		, "startRound");
		msg.put("numPlayers"	, nPlayers);
		msg.put("playerNames"	, playerNames);
		msg.put("yourName"		, yourName);
		msg.put("numRounds"		, nRounds);
		msg.put("payments"		, paymentArray);
		
		try {
			worker.sendExperimentPrivate(msg);
		} catch (MessageException e) {			
			e.printStackTrace(); 
		}
	}

	public static void sendSignal(HITWorker worker, String selected) throws WrongStateException {				
		try {
			worker.sendExperimentPrivate(ImmutableMap.of(
					"status", "signal",
					"signal", selected
					));
		} catch (MessageException e) {			
			e.printStackTrace();
		}
	}

	public static void sendReportConfirmation(HITWorker worker, String reporter) {
		try {
			worker.sendExperimentPrivate(ImmutableMap.of(
					"status", "confirmReport",
					"playerName", reporter
					));
		} catch (MessageException e) {			
			e.printStackTrace();
		}
	}
	
	public static void sendResults(HITWorker worker, Map<String, Map<String, String>> results) {
		try {
			worker.sendExperimentPrivate(ImmutableMap.of(
					"status", "results",
					"result", results));
		} catch (MessageException e) {			
			e.printStackTrace();
		}
	}
	
}
