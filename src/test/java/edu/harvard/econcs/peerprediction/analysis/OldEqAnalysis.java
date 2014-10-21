package edu.harvard.econcs.peerprediction.analysis;

import java.io.IOException;

public class OldEqAnalysis {

	public static void performAnalysis() throws IOException {
		eqConvergenceSimpleMethod();
	}
	
	public static void eqConvergenceSimpleMethod() throws IOException {
		System.out.println("Equilibrium convergence by simple method");
	
		if (LogReader.treatment.equals("prior2-basic")
				|| LogReader.treatment.equals("prior2-outputagreement")
				|| LogReader.treatment.equals("prior2-symmlowpay")) {
			OldEqAnalysis.gameSymmetricConvergenceType();
			// gameSymmetricConvergenceTypeRelaxed(3);
		} else if (LogReader.treatment.equals("prior2-uniquetruthful")) {
			OldEqAnalysis.gameConvergenceTypeT3();
			// gameAsymmetricConvergenceTypeRelaxed(3);
		}
	}

	static void gameSymmetricConvergenceType() throws IOException {
	
		int numHO = 0;
		int numMM = 0;
		int numGB = 0;
		int numUnclassified = 0;
		int numTotal = LogReader.expSet.numGames;
	
		for (Game game : LogReader.expSet.games) {
	
			OldEqAnalysis.fillConvergenceType(game);
	
			if (game.convergenceType.equals("MM"))
				numMM++;
			else if (game.convergenceType.equals("GB"))
				numGB++;
			else if (game.convergenceType.equals("HO"))
				numHO++;
			else
				numUnclassified++;
		}
	
		System.out
				.println(String
						.format("GB: %d (%d%%), MM: %d (%d%%), HO: %d (%d%%), Unclassified: %d(%d%%), Total: %d",
								numGB, Math.round(numGB * 100.0 / numTotal),
								numMM, Math.round(numMM * 100.0 / numTotal),
								numHO, Math.round(numHO * 100.0 / numTotal),
								numUnclassified,
								Math.round(numUnclassified * 100.0 / numTotal),
								numTotal));
	}

	static void gameConvergenceTypeT3() {
	
		int numHO = 0;
		int num3MM1GB = 0;
		int num1MM3GB = 0;
		int numUnclassified = 0;
	
		for (Game game : LogReader.expSet.games) {
	
			OldEqAnalysis.fillAsymmetricConvergenceType(game);
	
			if (game.convergenceType.equals("3MM"))
				num3MM1GB++;
			else if (game.convergenceType.equals("3GB"))
				num1MM3GB++;
			else if (game.convergenceType.equals("HO"))
				numHO++;
			else
				numUnclassified++;
		}
	
		System.out.println(String.format(
				"3GB: %d, 3MM: %d, HO: %d, Unclassified: %d, Total: %d",
				num1MM3GB, num3MM1GB, numHO, numUnclassified, LogReader.expSet.numGames));
	
	}

	private static void gameSymmetricConvergenceTypeRelaxed(int i)
			throws IOException {
	
		int numHO = 0;
		int numMM = 0;
		int numGB = 0;
		int numUnclassified = 0;
	
		for (Game game : LogReader.expSet.games) {
			OldEqAnalysis.fillConvergenceTypeRelaxed(game, i);
	
			if (game.convergenceTypeRelaxed.startsWith("MM"))
				numMM++;
			else if (game.convergenceTypeRelaxed.startsWith("GB"))
				numGB++;
			else if (game.convergenceTypeRelaxed.startsWith("HO"))
				numHO++;
			else
				numUnclassified++;
		}
	
		System.out.println(String.format(
				"i=%d, GB: %d, MM: %d, HO: %d, Unclassified: %d", i, numGB,
				numMM, numHO, numUnclassified));
	
	}

	private static void gameAsymmetricConvergenceTypeRelaxed(int i)
			throws IOException {
	
		int numHO = 0;
		int num3MM = 0;
		int num3GB = 0;
		int numUnclassified = 0;
	
		for (Game game : LogReader.expSet.games) {
	
			OldEqAnalysis.fillAsymmetricConvergenceTypeRelaxed(game, i);
	
			if (game.convergenceTypeRelaxed.startsWith("3MM"))
				num3MM++;
			else if (game.convergenceTypeRelaxed.startsWith("3GB"))
				num3GB++;
			else if (game.convergenceTypeRelaxed.startsWith("HO"))
				numHO++;
			else
				numUnclassified++;
		}
	
		System.out.println(String.format(
				"i=%d, 3GB: %d, 3MM: %d, HO: %d, Unclassified: %d", i, num3GB,
				num3MM, numHO, numUnclassified));
	}

	/**
	 * Check if players are playing the strategy of always clicking left/right
	 * choice.
	 */
	private static void lazyStrategy() {
		int total = 0;
		int countLeft = 0;
		int numAlwaysLeft = 0;
		int numAlwaysRight = 0;
		for (Game game : LogReader.expSet.games) {
			for (String hitId : game.playerHitIds) {
	
				int numLeft = getNumLeftChosen(game, hitId);
				countLeft += numLeft;
				total += LogReader.expSet.numRounds;
				if (numLeft == LogReader.expSet.numRounds)
					numAlwaysLeft++;
				if (numLeft == 0)
					numAlwaysRight++;
			}
		}
		System.out.println("Number of player always left: " + numAlwaysLeft);
		System.out.println("Number of player always right: " + numAlwaysRight);
		System.out.println("Number of left choices: " + countLeft + ", total: "
				+ total);
		System.out.println("Percent of left choices: " + 100.0 * countLeft
				/ total);
	}

	public static int getHonestStart(String hitId, Game g) {
		for (int i = g.rounds.size() - 1; i >= 0; i--) {
			String signal = g.rounds.get(i).getSignal(hitId);
			String report = g.rounds.get(i).getReport(hitId);
			if (signal.equals(report))
				continue;
			else 
				return i + 1;
		}
		return 1;
	}

	public static int getCandyStart(String hitId, String candy, Game game) {
		for (int i = game.rounds.size() - 1; i >= 0; i--) {
			if (game.rounds.get(i).getReport(hitId).equals(candy))
				continue;
			else 
				return i + 1;
		}
		return 1;
	}

	static int getCandyStartRelaxed(String hitId, String candy, int num, Game game) {
		int countRelaxed = 0;
		for (int i = game.rounds.size() - 1; i >= 0; i--) {
			if (game.rounds.get(i).getReport(hitId).equals(candy))
				continue;
			else {
				if (countRelaxed < num) {
					countRelaxed++;
				} else {
					return i + 1;
				}
			}
		}
		return 1;
	}

	static int getHonestStartRelaxed(String hitId, int num, Game game) {
		int countRelaxed = 0;
		for (int i = game.rounds.size() - 1; i >= 0; i--) {
			String signal = game.rounds.get(i).getSignal(hitId);
			String report = game.rounds.get(i).getReport(hitId);
			if (signal.equals(report))
				continue;
			else {
				if (countRelaxed < num) {
					countRelaxed++;
				} else {
					return i + 1;
				}
			}
		}
		return 1;
	}

	public static void fillConvergenceType(Game game) {
		
		int gameMMStart = 0;
		int gameGBStart = 0;
		int gameHOStart = 0; 
		
		for (String hitId : game.playerHitIds) {
			
			int playerMMStart = getCandyStart(hitId, "MM", game);
			gameMMStart = Math.max(gameMMStart, playerMMStart);
			
			int playerGBStart = getCandyStart(hitId, "GB", game);
			gameGBStart = Math.max(gameGBStart, playerGBStart);
			
			int playerHOStart = getHonestStart(hitId, game);
			gameHOStart = Math.max(gameHOStart, playerHOStart);
		}
		
		int min = Math.min(Math.min(gameMMStart, gameGBStart), gameHOStart);
		game.roundConverged = min;
		
		String gameType = "";
		if (min > (LogReader.expSet.numRounds - 5)) {
			gameType = "undecided";
		} else {
			if (gameMMStart == min) {
				gameType = "MM";
			}
			if (gameGBStart == min) {
				gameType = "GB";
			}
			if (gameHOStart == min) {
				gameType = "HO";
			}
		}
		game.convergenceType = gameType;
	}

	public static void fillConvergenceTypeRelaxed(Game game, int i) {
		String gameType = "";
		
		int gameMMStart = 0;
		int gameGBStart = 0;
		int gameHOStart = 0; 
		
		for (String hitId : game.playerHitIds) {
			
			int playerMMScore = getCandyStartRelaxed(hitId, "MM", i, game);
			gameMMStart = Math.max(gameMMStart, playerMMScore);
			
			int playerGBScore = getCandyStartRelaxed(hitId, "GB", i, game);
			gameGBStart = Math.max(gameGBStart, playerGBScore);
			
			int playerHOScore = getHonestStartRelaxed(hitId, i, game);
			gameHOStart = Math.max(gameHOStart, playerHOScore);
		}
		
		int min = Math.min(Math.min(gameMMStart, gameGBStart), gameHOStart);
		game.roundConvergedRelaxed = min;
		
		if (min > ((LogReader.expSet.numRounds - 5) - i)) {
			gameType = "undecided";
		} else {
			if (gameMMStart == min) {
				gameType = "MM";
			}
			if (gameGBStart == min) {
				gameType = "GB";
			}
			if (gameHOStart == min) {
				gameType = "HO";
			}
		}
		game.convergenceTypeRelaxed = gameType;
	}

	public static void fillAsymmetricConvergenceType(Game game) {
		// 3 MM, 1 GB
		int best3MMStart = Integer.MAX_VALUE;
		
		
		for (String hitId1 : game.playerHitIds) {
			
			int threeMMStart = 0;
			
			for (String hitId2 : game.playerHitIds) {
				
				if (hitId1.equals(hitId2)) {
					int playerGBScore = getCandyStart(hitId2, "GB", game);
					threeMMStart = Math.max(threeMMStart, playerGBScore);
				} else {
					int playerMMScore = getCandyStart(hitId2, "MM", game);
					threeMMStart = Math.max(threeMMStart, playerMMScore);
				}
			}
			
			if (threeMMStart < best3MMStart)
				best3MMStart = threeMMStart;
			
		}
		
		// 3 GB, 1 MM
		int best3GBStart = Integer.MAX_VALUE;
		for (String hitId1 : game.playerHitIds) {
			
			int threeGBStart = 0;
			
			for (String hitId2 : game.playerHitIds) {
				if (hitId1.equals(hitId2)) {
					int playerMMScore = getCandyStart(hitId2, "MM", game);
					threeGBStart = Math.max(threeGBStart, playerMMScore);
				} else {
					int playerGBScore = getCandyStart(hitId2, "GB", game);
					threeGBStart = Math.max(threeGBStart, playerGBScore);
				}
			}
			
			if (threeGBStart < best3GBStart)
				best3GBStart = threeGBStart;
			
		}
	
		int gameHOStart = 0; 
		for (String hitId : game.playerHitIds) {
			
			int playerHOStart = getHonestStart(hitId, game);
			gameHOStart = Math.max(gameHOStart, playerHOStart);
		}
		
		int min = Math.min(Math.min(best3MMStart, best3GBStart), gameHOStart);
		game.roundConverged = min;
		
		String gameType = "";
		if (min > (LogReader.expSet.numRounds - 5)) {
			gameType = "undecided";
		} else {
			if (min == best3MMStart) {
				gameType = "3MM";					
			} else if (min == best3GBStart) {
				gameType = "3GB"; 
			} else if (min == gameHOStart) {
				gameType = "HO";
			}
		}
		game.convergenceType = gameType;
	}

	public static void fillAsymmetricConvergenceTypeRelaxed(Game game, int i) {
	
		// 3 MM, 1 GB
		int bestThreeMMOneGBStart = Integer.MAX_VALUE;
		for (String hitId1 : game.playerHitIds) {
			
			int threeMMOneGBStart = 0;
			
			for (String hitId2 : game.playerHitIds) {
				if (hitId1.equals(hitId2)) {
					int playerGBScore = getCandyStartRelaxed(hitId2, "GB", i, game);
					threeMMOneGBStart = Math.max(threeMMOneGBStart, playerGBScore);
				} else {
					int playerMMScore = getCandyStartRelaxed(hitId2, "MM", i, game);
					threeMMOneGBStart = Math.max(threeMMOneGBStart, playerMMScore);
				}
			}
			
			if (threeMMOneGBStart < bestThreeMMOneGBStart)
				bestThreeMMOneGBStart = threeMMOneGBStart;
			
		}
		
		// 3 GB, 1 MM
		int bestThreeGBOneMMStart = Integer.MAX_VALUE;
		for (String hitId1 : game.playerHitIds) {
	
			int threeGBOneMMStart = 0;
			
			for (String hitId2 : game.playerHitIds) {
				if (hitId1.equals(hitId2)) {
					int playerMMScore = getCandyStartRelaxed(hitId2, "MM", i, game);
					threeGBOneMMStart = Math.max(threeGBOneMMStart, playerMMScore);
				} else {
					int playerGBScore = getCandyStartRelaxed(hitId2, "GB", i, game);
					threeGBOneMMStart = Math.max(threeGBOneMMStart, playerGBScore);
				}
			}
			
			if (threeGBOneMMStart < bestThreeGBOneMMStart)
				bestThreeGBOneMMStart = threeGBOneMMStart;
			
		}
		
		int gameHOStart = 0; 
		for (String hitId : game.playerHitIds) {
			
			int playerHOStart = getHonestStartRelaxed(hitId, i, game);
			gameHOStart = Math.max(gameHOStart, playerHOStart);
		}
	
		int min = Math.min(Math.min(bestThreeMMOneGBStart, bestThreeGBOneMMStart), gameHOStart);
		game.roundConvergedRelaxed = min;
	
		String gameType = "";
		if (min > ((LogReader.expSet.numRounds - 5) - i)) {
			gameType = "undecided";
		} else {
			if (min == bestThreeMMOneGBStart) {
				gameType = "3MM";
			} else if (min == bestThreeGBOneMMStart) {
				gameType = "3GB";
			} else if (min == gameHOStart) {
				gameType = "HO";
			}
		}
		game.convergenceTypeRelaxed = gameType;
	}

	/**
	 * Given a player HIT ID, give the number of times the player made the left choice 
	 * @param hitId
	 * @return
	 */
	public static int getNumLeftChosen(Game game, String hitId) {
		int count = 0;
		for (Round round : game.rounds) {
			String report = round.getReport(hitId);
			int radio = round.radio;
			if ((report.equals("MM") && radio == 0) || (report.equals("GB") && radio == 1))
				count++;
		}
		return count;
	}

}
