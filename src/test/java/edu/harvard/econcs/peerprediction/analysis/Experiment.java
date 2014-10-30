package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;

public class Experiment {

	String setId;
	
	int numGames = -1;
	int nonKilledGames = -1; // non-killed games
	
	int numPlayers = -1;
	int numRounds = -1;
	List<Double> priorProbs = null;
	List<Map<String, Double>> worlds = null;

	List<Game> games;
	
	public Experiment() {
		games = new ArrayList<Game>();
	}

	/**
	 * Save prior probabilities
	 * @param probString
	 */
	public void savePriorProbs(String probString) {
		double[] priorProbArray = Utils.gson.fromJson(probString, double[].class);
		List<Double> priorProbList = new ArrayList<Double>();
		for (double prob : priorProbArray) {
			priorProbList.add(prob);
		}
		priorProbs = priorProbList;
	}
	
	/**
	 * Save prior worlds
	 * @param worldsString
	 */
	public void savePriorWorlds(String worldsString) {
		worlds = new ArrayList<Map<String, Double>>();
		Object[] worldsArray = Utils.gson.fromJson(worldsString, Object[].class);
		for (int i = 0; i < worldsArray.length; i++) {
			Map<String, Double> worldMap = Utils.gson.fromJson(
					worldsArray[i].toString(),
					new TypeToken<Map<String, Double>>() {
					}.getType());
			worlds.add(worldMap);
		}
	}
	
}
