package edu.harvard.econcs.peerprediction.analysis;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.reflect.TypeToken;

public class Round {

	int roundNum;
	
	Map<String, Double> chosenWorld;
	Map<String, Map<String, Object>> result;
	
	int duration; // in milliseconds
	String endTime;
	
	public Round() {
		chosenWorld = new HashMap<String, Double>();
		result = new HashMap<String, Map<String, Object>>();
	}

	public void saveChosenWorld(String chosenWorldString) {
		chosenWorld = Utils.gson.fromJson(chosenWorldString,
				new TypeToken<Map<String, Double>>() {
				}.getType());
	}

	public void saveResult(String resultString) throws Exception {
		Map<String, Object> parsedMap = Utils.gson.fromJson(resultString,
				new TypeToken<Map<String, Object>>() {
				}.getType());
		for (String hitId : parsedMap.keySet()) {
			String valueString = parsedMap.get(hitId).toString();
			Map<String, Object> parsedValue = Utils.gson.fromJson(valueString,
					new TypeToken<Map<String, Object>>() {
					}.getType());
			result.put(hitId, parsedValue);
		}

	}

	public String getReport(String hitId) {
		return result.get(hitId).get("report").toString();
	}

	public String getSignal(String hitId) {
		return result.get(hitId).get("signal").toString();
	}
	
	public String toString() {
		return String.format("round %s, chosen world:%s\n"
				+ "result: %s\n", roundNum, chosenWorld, result);
		
	}

}
