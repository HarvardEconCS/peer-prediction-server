package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Experiment {

	String setId;
	int numGames;
	
	int numPlayers = -1;
	int numRounds = -1;
	double[] priorProbs = null;
	List<Map<String, Double>> worlds = null;

	List<Game> games;
	
	public Experiment() {
		games = new ArrayList<Game>();
	}

	public void addGame(Game game) {
		games.add(game);
	}

}
