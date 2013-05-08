package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ExitSurvey {

	Map<String, Map<String, String>> simpleQuestions;
	
	List<String> checkedStrategies;
	
	String otherStrategy;
	String strategyReason;
	String strategyChange;
	String comments;
	
	public ExitSurvey(String comment) {
		Map<String, Map<String, String>> simpleQuestions = new HashMap<String, Map<String, String>>();
		
		Gson gson = new Gson();
		Map<String, Object> exitSurveyMap = gson.fromJson(comment,
				new TypeToken<Map<String, Object>>() {
				}.getType());

		comments = exitSurveyMap.get("comments").toString();
		strategyChange = exitSurveyMap.get("strategyChange").toString();
		strategyReason = exitSurveyMap.get("strategyReason").toString();
		otherStrategy = exitSurveyMap.get("otherStrategy").toString();
		
		Map<String, Object> strategyMap = (Map<String, Object>) exitSurveyMap.get("strategy");
		checkedStrategies = new ArrayList<String>();
		for (int i = 1; i <= 5; i++) {
			Map<String, Object> strategyMapChild = (Map<String, Object>) strategyMap
					.get("strategy" + i);
			if (strategyMapChild.get("checked").toString().equals("true")) {
				checkedStrategies.add(strategyMapChild.get("value").toString());
			}
		}
		
//		{"strategy":{"strategy1":{"value":"honest","checked":false},"strategy2":{"value":"opposite","checked":false},"strategy3":{"value":"alwaysmm","checked":true},"strategy4":{"value":"alwaysgb","checked":false},"strategy5":{"value":"other","checked":false}},"otherStrategy":"","strategyReason":"I felt it the best way to maximize my bonus.","strategyChange":"Early on I didn't do this but quickly realized it was the best way to go as long as the other 2 people caught on as well.","comments":"A little buggy at first it wouldn't advance off the 1st screen and am not sure if I was waiting on someone to choose a candy or if the game was froze but it finally started to go again."}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
