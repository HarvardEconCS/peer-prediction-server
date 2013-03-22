package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.List;

import net.andrewmao.misc.Pair;

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

	public List<String> getSignalsForRound(int i) {
		List<String> list = new ArrayList<String> ();
		for (Game game : games) {
			for (String hitId : game.playerHitIds) {
				list.add(game.rounds.get(i - 1).getSignal(hitId));
			}
		}
		return list;
	}

	public List<String> getReportsForRound(int roundNum) {
		List<String> list = new ArrayList<String> ();
		for (Game game : games) {
			for (String hitId : game.playerHitIds) {
				list.add(game.rounds.get(roundNum - 1).getReport(hitId));
			}
		}
		return list;
	}



	public List<Pair<String, String>> getSignalReportPairsForRound(int roundNum) {
		List<Pair<String, String>> list = new ArrayList<Pair<String, String>>();
		for (Game game : games) {
			for (int i = 0; i < game.playerHitIds.length; i++) {
				String hitId = game.playerHitIds[i];
				String signal = game.rounds.get(roundNum).getSignal(hitId);
				String report = game.rounds.get(roundNum).getReport(hitId);
				list.add(new Pair<String, String>(signal, report));
			}
		}
		return list;
	}

	public List<List<Pair<String, String>>> getSignalReportPairsForRoundGroupByGame(
			int roundNum) {
		List<List<Pair<String, String>>> listOfLists = new ArrayList<List<Pair<String, String>>>();

		for (Game game : games) {
			List<Pair<String, String>> listForGame = new ArrayList<Pair<String, String>>();
			for (int i = 0; i < game.playerHitIds.length; i++) {
				String hitId = game.playerHitIds[i];
				String signal = game.rounds.get(roundNum).getSignal(hitId);
				String report = game.rounds.get(roundNum).getReport(hitId);
				listForGame.add(new Pair<String, String>(signal, report));
			}
			listOfLists.add(listForGame);
		}
		return listOfLists;
	}

	public List<Pair<String, String>> getSignalReportPairsForRoundRange(int startRoundNum,
			int endRoundNum) {
		List<Pair<String, String>> list = new ArrayList<Pair<String, String>>();
		for (Game game : games) {
			for (int i = 0; i < game.playerHitIds.length; i++) {
				for (int j = startRoundNum; j <= endRoundNum; j++) {
					String hitId = game.playerHitIds[i];
					String signal = game.rounds.get(j).getSignal(hitId);
					String report = game.rounds.get(j).getReport(hitId);
					list.add(new Pair<String, String>(signal, report));
				}
			}
		}
		return list;
	}
	
}
