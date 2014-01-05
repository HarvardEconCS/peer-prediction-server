package edu.harvard.econcs.peerprediction.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Round {

	int roundNum;
	Map<String, Double> chosenWorld;
	Map<String, Map<String, Object>> roundPlay;
	Map<String, Map<String, Object>> roundResult;
	Gson gson = new Gson();
	int durationInMS;
	String endTimeString;

	public Round() {
		chosenWorld = new HashMap<String, Double>();
		roundPlay = new HashMap<String, Map<String, Object>>();
		roundResult = new HashMap<String, Map<String, Object>>();
	}

	public void saveChosenWorld(String chosenWorldString) {
		chosenWorld = gson.fromJson(chosenWorldString,
				new TypeToken<Map<String, Double>>() {
				}.getType());
	}

	public void saveSignal(String workerId, String hitId, String signal, String signalTimestamp) {
		if (roundPlay.containsKey(hitId)) {
			roundPlay.get(hitId).put("signal", signal);
			roundPlay.get(hitId).put("signalTimestamp", signalTimestamp);
			if (!roundPlay.get(hitId).containsKey("workerId"))
				roundPlay.get(hitId).put("workerId", workerId);
		} else {
			Map<String, Object> workerObj = new HashMap<String, Object>();
			workerObj.put("signal", signal);
			workerObj.put("signalTimestamp", signalTimestamp);
			workerObj.put("workerId", workerId);
			roundPlay.put(hitId, workerObj);
		}
	}

	public void saveReport(String workerId, String hitId, String report, String reportTimestamp) {
		if (roundPlay.containsKey(hitId)) {
			roundPlay.get(hitId).put("report", report);
			roundPlay.get(hitId).put("reportTimestamp", reportTimestamp);
			if (!roundPlay.get(hitId).containsKey("workerId"))
				roundPlay.get(hitId).put("workerId", workerId);
		} else {
			Map<String, Object> workerObj = new HashMap<String, Object>();
			workerObj.put("report", report);
			workerObj.put("reportTimestamp", reportTimestamp);
			workerObj.put("workerId", workerId);
			roundPlay.put(hitId, workerObj);
		}
	}

	public void saveResult(String resultString) throws Exception {
		// parse result string
		Map<String, Object> parsedMap = gson.fromJson(resultString,
				new TypeToken<Map<String, Object>>() {
				}.getType());
		for (String hitId : parsedMap.keySet()) {
			String valueString = parsedMap.get(hitId).toString();
			Map<String, Object> parsedValue = gson.fromJson(valueString,
					new TypeToken<Map<String, Object>>() {
					}.getType());
			roundResult.put(hitId, parsedValue);
		}

		// check whether log is consistent
		Set<String> hitIds = roundPlay.keySet();
		for (String hitId : hitIds) {
			if (!parsedMap.keySet().contains(hitId))
				throw new LogInconsistencyException(String.format(
						"hitId %s not found in result received %s", hitId,
						resultString));

			Object expectedSignal = roundPlay.get(hitId).get("signal");
			Object actualSignal = roundResult.get(hitId).get("signal");
			if (actualSignal == null) {
				throw new LogInconsistencyException(String.format(
						"hitId %s: expected signal %s but got signal null",
						hitId, expectedSignal.toString()));
			} else if (!expectedSignal.toString().equals(
					actualSignal.toString())) {
				throw new LogInconsistencyException(String.format(
						"hitId %s: expected signal %s but got signal %s",
						hitId, expectedSignal.toString(),
						actualSignal.toString()));
			}

			Object expectedReport = roundPlay.get(hitId).get("report");
			Object actualReport = roundResult.get(hitId).get("report");
			if (actualReport == null) {
				throw new LogInconsistencyException(String.format(
						"hitId %s: expected report %s but got report null",
						hitId, expectedReport.toString()));
			} else if (!expectedReport.toString().equals(
					actualReport.toString())) {
				throw new LogInconsistencyException(String.format(
						"hitId %s: expected report % but got report %s", hitId,
						expectedReport.toString(), actualReport.toString()));
			}

		}

	}

	public String getReport(String hitId) {
		return roundResult.get(hitId).get("report").toString();
	}

	public String getSignal(String hitId) {
		return roundResult.get(hitId).get("signal").toString();
	}

}
