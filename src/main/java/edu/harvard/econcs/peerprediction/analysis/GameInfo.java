package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class GameInfo {

	List<Map<String, Double>> worlds;
	double[] priorProbs;

	int numRounds;
	int numPlayers;
	String[] playerNames;
	double[] paymentArray;
	String[] signalList;
	
	List<RoundInfo> rounds;
	
	Gson gson;

	public GameInfo() {
		worlds = new ArrayList<Map<String, Double>>();
		rounds = new ArrayList<RoundInfo>();
		gson = new Gson();
	}
	
	public void setNumRounds(String numRoundsString) {
		this.numRounds = Integer.parseInt(numRoundsString);
	}

	public void setNumPlayers(String numPlayers) {	
		this.numPlayers = Integer.parseInt(numPlayers);
	}

	public int getNumRounds() {
		return this.numRounds;
	}

	public int getNumPlayers() {
		return numPlayers;
	}

	public RoundInfo getRoundInfo(int i) {
		return rounds.get(i);
	}

	public void addRoundInfo(RoundInfo roundInfo) {
		rounds.add(roundInfo);
	}

	public void setPrior(String priorString) {
//		Map<String, String> topMap = gson.fromJson(priorString, new TypeToken<Map<String, String>>() {}.getType());
//		Object[] worldsArray = gson.fromJson(topMap.get("worlds"), Object[].class);
//		priorProbs = gson.fromJson(topMap.get("prob"), double[].class);
	}

	public void setPlayerNames(String playerNamesString) {
		playerNames = gson.fromJson(playerNamesString, String[].class);
		System.out.printf("player names %s\n", Arrays.toString(playerNames));
	}

	public void setPaymentRule(String paymentRuleString) {
		paymentArray = gson.fromJson(paymentRuleString, double[].class);
	}

	public void setSignalList(String signalListString) {
		signalList = gson.fromJson(signalListString, String[].class);
	}

	public String[] getPlayerNames() {
		return playerNames;
	}

	public Map<String, Map<String, Double>> getStrategy(
			String string, Pattern roundstart, Pattern roundend) {
		return null;
	}

	public void setPriorProb(String probString) {
		priorProbs = gson.fromJson(probString, double[].class);
	}

	public void setPriorWorlds(String worldsString) {
		Object[] worldsArray = gson.fromJson(worldsString, Object[].class);
		for (int i = 0; i < worldsArray.length; i++) {
			Map<String, Double> worldMap = gson.fromJson(worldsArray[i].toString(), new TypeToken<Map<String, Double>>(){}.getType());
			worlds.add(worldMap);
		}
	}


	
}
