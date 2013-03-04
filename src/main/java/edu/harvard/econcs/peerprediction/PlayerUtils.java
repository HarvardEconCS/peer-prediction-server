package edu.harvard.econcs.peerprediction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.harvard.econcs.peerprediction.TestPlayer.WrongStateException;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.server.MessageException;

public class PlayerUtils {

	public static void sendGeneralInfo(HITWorker worker, int nPlayers, int nRounds, 
			String[] playerNames, String yourName, double[] paymentArray, String[] signalList) {		
		
		Map<String, Object> msg = new HashMap<String, Object>();
		msg.put("status"		, "generalInfo");
		msg.put("numPlayers"	, nPlayers);
		msg.put("playerNames"	, playerNames);
		msg.put("yourName"		, yourName);
		msg.put("signalList"    , signalList);	
		msg.put("numRounds"		, nRounds);
		msg.put("payments"		, paymentArray);
		
		try {
			worker.deliverExperimentService(msg);
		} catch (MessageException e) {			
			e.printStackTrace(); 
		}
	}

	public static void sendSignal(HITWorker worker, String selected) throws WrongStateException {				
		try {
			worker.deliverExperimentService(ImmutableMap.of(
					"status", "signal",
					"signal", (Object) selected
					));
		} catch (MessageException e) {			
			e.printStackTrace();
		}
	}

	public static void sendReportConfirmation(HITWorker worker, String reporter) {
		try {
			worker.deliverExperimentService(ImmutableMap.of(
					"status", "confirmReport",
					"playerName", (Object) reporter
					));
		} catch (MessageException e) {			
			e.printStackTrace();
		}
	}
	
	public static void sendResults(HITWorker worker, Map<String, Map<String, String>> results) {
		try {
			worker.deliverExperimentService(ImmutableMap.of(
					"status", "results",
					"result", results));
		} catch (MessageException e) {			
			e.printStackTrace();
		}
	}

	public static void sendDisconnectedErrorMessage(HITWorker worker) {
		try {
			worker.deliverExperimentService(ImmutableMap.of(
					"status", (Object) "killed"));
		} catch (MessageException e) {			
			e.printStackTrace();
		}
		
		
	}

	public static void resentState(HITWorker worker, int groupSize,
			int nRounds, String[] playerNames, String hitId,
			double[] paymentArray, String[] signalArray,
			List<Map<String, Map<String, String>>> existingResults,
			String currPlayerSignal, String currPlayerReport, 
			List<String> workersConfirmed) {
		Map<String, Object> msg = new HashMap<String, Object>();
		msg.put("status"		, "resendState");
		msg.put("numPlayers"	, groupSize);
		msg.put("playerNames"	, playerNames);
		msg.put("yourName"		, hitId);
		msg.put("signalList"    , signalArray);	
		msg.put("numRounds"		, nRounds);
		msg.put("payments"		, paymentArray);
		msg.put("existingResults"	, existingResults);
		msg.put("currPlayerSignal"	, currPlayerSignal);
		msg.put("currPlayerReport"	, currPlayerReport);
		msg.put("workersConfirmed"	, workersConfirmed);

		try {
			worker.deliverExperimentService(msg);
		} catch (MessageException e) {			
			e.printStackTrace();
		}
		
	}
	
}
