package edu.harvard.econcs.peerprediction.analysis;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.reflect.TypeToken;

public class Round {

	int roundNum = -1;

	Map<String, Double> chosenWorld;
	Map<String, Map<String, Object>> result;

	int radio = -1;

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

	public void saveRadio(String r) {
		this.radio = Integer.parseInt(r);
	}

	public String getReport(String hitId) {
		return result.get(hitId).get("report").toString();
	}

	public String getSignal(String hitId) {
		return result.get(hitId).get("signal").toString();
	}

	public double getReward(String hitId) {
		return Double.parseDouble(result.get(hitId).get("reward").toString());
	}

	public String getRefPlayer(String hitId) {
		return result.get(hitId).get("refPlayer").toString();
	}

	public double getHypoReward(String treatment, String playerId,
			String hypotheticalReport) {
		if (treatment.equals("prior2-basic")
				|| treatment.equals("prior2-outputagreement")) {

			String refPlayer = this.getRefPlayer(playerId);
			String refReport = this.getReport(refPlayer);
			return Utils.getPayment(treatment, hypotheticalReport, refReport);

		} else {

			int numMMInOtherReports = 0;
			for (String hitId : result.keySet()) {
				if (hitId.equals(playerId))
					continue;
				else {
					String report = this.getReport(hitId).toString();
					if (report.equals("MM"))
						numMMInOtherReports++;
				}
			}
			return Utils.getPayment(treatment, hypotheticalReport,
					numMMInOtherReports);
		}
	}

	public String toString() {
		return String.format("round %s, chosen world:%s\n" + "result: %s\n",
				roundNum, chosenWorld, result);

	}

}
