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
	
	String strategyComment;
	String learnComment;
	
	public ExitSurvey(String comment) {
		Map<String, Map<String, String>> simpleQuestions = new HashMap<String, Map<String, String>>();
		
		Gson gson = new Gson();
		Map<String, Object> exitSurveyMap = gson.fromJson(comment,
				new TypeToken<Map<String, Object>>() {
				}.getType());

		Object bugString = exitSurveyMap.get("bug");
		Map<String, String> bugMap = (Map<String, String>) bugString;
		simpleQuestions.put("bug", bugMap);

		Object interfaceString = exitSurveyMap.get("interface");
		Map<String, String> interfaceMap = (Map<String, String>) interfaceString;
		simpleQuestions.put("interface", interfaceMap);

		Object learnString = exitSurveyMap.get("learn");
		Map<String, String> learnMap = (Map<String, String>) learnString;
		simpleQuestions.put("learn", learnMap);
		learnComment = learnMap.get("comments");
		
		Object strategyString = exitSurveyMap.get("strategy");
		Map<String, Object> strategyMap = (Map<String, Object>) strategyString;
		strategyComment = strategyMap.get("comments").toString();
		
		checkedStrategies = new ArrayList<String>();
		for (int i = 1; i <= 5; i++) {
			Map<String, Object> strategyMapChild = (Map<String, Object>) strategyMap
					.get("strategy" + i);
			if (strategyMapChild.get("checked").toString().equals("true")) {
				checkedStrategies.add(strategyMapChild.get("value").toString());
			}
		}
		
		// 		"strategy":{"strategy1":{"value":"honest","checked":true},"strategy2":{"value":"opposite","checked":false},"strategy3":{"value":"alwaysmm","checked":false},"strategy4":{"value":"alwaysgb","checked":false},"strategy5":{"value":"other","checked":false},"comments":"I was just honest and reported the type of candy I got."}}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
