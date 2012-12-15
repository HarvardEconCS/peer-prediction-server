package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.Maps;

import edu.harvard.econcs.turkserver.api.HITWorker;

public class PeerResult {

	Map<String, Map<String, String>> resultObject;
	Map<String, Double> chosenWorld;
	
	public PeerResult(Map<String, Double> chosenWorld) {
		
		this.chosenWorld = chosenWorld;
		this.resultObject = new HashMap<String, Map<String, String>>();
		
	}

	public static Map<String, Map<String, String>> deserialize(Object object) {		
		Map<String, Map<String, String>> returnMap = Maps.newHashMap();
		
		Map<String, Object> inputMap = (Map<String, Object>) object;
		for( Map.Entry<String, Object> firstLevelMapping : inputMap.entrySet() ) {
			
			HashMap<String, String> resultFirstLevelValue = Maps.newHashMap();
			Map<String, Object> secondLevelMappings = (Map<String, Object>) firstLevelMapping.getValue();
			for( Map.Entry<String, Object> secondLevelMapping : secondLevelMappings.entrySet() ) {
				resultFirstLevelValue.put(secondLevelMapping.getKey(), secondLevelMapping.getValue().toString());
			}
			
			String firstLevelKey = firstLevelMapping.getKey();
			returnMap.put(firstLevelKey, resultFirstLevelValue);
		}
		
		return returnMap;
	}

	public void saveSignal(HITWorker p, String selected) {
		if (resultObject.containsKey(p.getHitId())) {
			Map<String, String> playerResult = resultObject.get(p.getHitId());
			playerResult.put("signal", selected);
		} else {
			Map<String, String> playerResult = new HashMap<String, String>();
			playerResult.put("signal", selected);
			resultObject.put(p.getHitId(), playerResult);
		}

	}


	public void saveReport(HITWorker reporter, String report) {
		if (resultObject.containsKey(reporter.getHitId())) {
			Map<String, String> playerResult = resultObject.get(reporter.getHitId());
			playerResult.put("report", report);
		} else {
			Map<String, String> playerResult = new HashMap<String, String>();
			playerResult.put("report", report);
			resultObject.put(reporter.getHitId(), playerResult);
		}

	}

	public boolean containsReport(HITWorker reporter) {
		return resultObject.containsKey(reporter.getHitId()) 
				&& resultObject.get(reporter.getHitId()).containsKey("report");
	}


	public int getReportSize() {
		int numReports = 0;
		for (String key : this.resultObject.keySet()) {
			if (resultObject.get(key).containsKey("report"))
				numReports++;
		}
		return numReports;
	}

	public Map<String, Map<String, String>> getResultForPlayer(HITWorker p) {

		Map<String, Map<String, String>> currResult = new HashMap<String, Map<String, String>>();
		
		for (String playerName: resultObject.keySet()) {
			Map<String, String> playerResult = new HashMap<String, String>();
			playerResult.put("report", resultObject.get(playerName).get("report"));
			playerResult.put("refPlayer", resultObject.get(playerName).get("refPlayer"));
			currResult.put(playerName, playerResult);
		}
		return currResult;
	}
	
	public void computePayments(PaymentRule paymentRule) {
		
		Random r = new Random();
		
		List<String> playerNames = new ArrayList<String>();
		playerNames.addAll(this.resultObject.keySet());
		
		// choose reference player
		for (int i = 0; i < playerNames.size(); i++) {
			int refPlayerIdx = r.nextInt(playerNames.size() - 1);
			if (refPlayerIdx >= i)
				refPlayerIdx++;

			Map<String, String> playerResult = resultObject.get(playerNames.get(i));
			playerResult.put("refPlayer", playerNames.get(refPlayerIdx));
		}
		
		// find payment
		for (String playerName : playerNames) {
			
			String refPlayerName = resultObject.get(playerName).get("refPlayer");
			String myReport = resultObject.get(playerName).get("report");
			String otherReport = resultObject.get(refPlayerName).get("report");
			Double reward = paymentRule.getPayment(myReport, otherReport);
			
			Map<String, String> playerResult = resultObject.get(playerName);
			playerResult.put("reward", String.format("%f", reward));
		}
	}
		
	
}
