package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.List;

public class Experiment {

	List<Game> games;
	String setId;
	int numGames;
	int numRounds;

	public Experiment() {
		games = new ArrayList<Game>();
	}

	public void addGame(Game game) {
		games.add(game);

	}

}
