package edu.harvard.econcs.peerprediction;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.harvard.econcs.peerprediction.TestPlayer.WrongStateException;
import edu.harvard.econcs.turkserver.api.HITWorker;

public class TurkPeerPlayer extends PeerPlayer {

	HITWorker worker;
	
	TurkPeerPlayer(HITWorker worker) {
		this.worker = worker;
	}
	
	@Override
	public void sendGeneralInfo(int nPlayers, int nRounds, 
			String[] playerNames, String yourName, double[] paymentArray) {		
		
		Map<String, Object> msg = new HashMap<String, Object>();
		msg.put("status"		, "startGame");
		msg.put("numPlayers"	, nPlayers);
		msg.put("playerNames"	, playerNames);
		msg.put("yourName"		, yourName);
		msg.put("numRounds"		, nRounds);
		msg.put("payments"		, paymentArray);
		
		worker.sendExperimentMessage(msg);
	}

	@Override
	public void sendSignal(String selected) throws WrongStateException {		
		
		worker.sendExperimentMessage(ImmutableMap.of(
				"status", "signal",
				"signal", selected
				));
	}

	@Override
	public void sendReportConfirmation(PeerPlayer reporter) {
		worker.sendExperimentMessage(ImmutableMap.of(
				"status", "confirmReport",
				"playerName", reporter.name
				));
	}

	@Override
	public void sendResults(Map<String, Map<String, String>> results) {
		worker.sendExperimentMessage(results);
	}

}
