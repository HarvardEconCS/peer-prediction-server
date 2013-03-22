package edu.harvard.econcs.peerprediction.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.andrewmao.misc.Pair;

public class LogReader {

	static final Pattern experimentStart = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) (.*) (.*) started");
	static final Pattern prior = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Prior is prob=(.*), worlds=(.*)");
	static final Pattern generalInfo = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) General information sent: numPlayers=([0-9]+), "
					+ "numRounds=([0-9]+), playerNames=(.*), paymentRule=(.*), signalList=(.*)");
	static final Pattern experimentRoundStart = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round ([0-9]+) started");
	static final Pattern experimentRoundFinish = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round ([0-9]+) finished");
	static final Pattern experimentFinish = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) (.*) (.*) finished");

	static final Pattern roundStart = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round (\\d+) started");
	static final Pattern gotSignal = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) got signal ([A-Z]{2})");
	// private static Pattern killed = Pattern
	// .compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) killed, because disconnected for ([0-9]+) milliseconds");
	static final Pattern choseReport = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) chose report ([A-Z]{2})");
	// static final Pattern fakeReport = Pattern
	// .compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) killed, put in fake report ([A-Z]{2})");
	static final Pattern roundResult = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round result is (.*)");
	static final Pattern roundEnd = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round ([0-9]+) finished");
	static final Pattern getBonus = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) gets bonus ([0-9.]+)");
	static final Pattern noBonus = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) killed, no bonus");
	static final Pattern chosenWorld = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Chosen world is (.*)");;

	static final Pattern timeStamp = Pattern
			.compile("([0-9]{2}):([0-9]{2}).([0-9]{3})");

	static final String rootDir = "/Users/alicexigao/Dropbox/peer_prediction/data/";
	
	static Experiment expSet;
	
	static List<ExitSurvey> exitComments;

	public static void main(String[] args) throws Exception {
		parseLog();
		
		analyzeInfo();
		
		saveExitSurvey();
		
		displayExitSurvey();
	}

	private static void analyzeInfo() throws Exception {

		System.out.printf("%d non-killed games\n\n", expSet.games.size());

		clusterStrategies();
		
		pureStrategyPerWorker();

		mixedStrategyPerWorker();

		pureStrategyPerRound();

		mixedStrategyPerRound();
		
		mixedStrategyPerRoundPerGame();
		
		int n = 2;
		mixedStrategyPerNRounds(n);
	}

	private static void clusterStrategies() {
		int numRounds = expSet.games.get(0).numRounds;
		
		for (int roundNum = 0; roundNum < numRounds; roundNum++) {
			System.out.printf("round %s\n", roundNum);
			List<List<Pair<String, String>>> signalReportPairs = 
				expSet.getSignalReportPairsForRoundGroupByGame(roundNum);
			AnalysisUtils.runEMAlgorithm(
				signalReportPairs, new String[]{"MM","GB"});
		}
	}

	private static void pureStrategyPerWorker() throws IOException {
		BufferedWriter writer1 = new BufferedWriter(new FileWriter(
				rootDir + "pureStrategyPerWorker.csv"));

		Strategy[] strategies = AnalysisUtils.getPureStrategyArray();
		double[] likelihoodsPerPlayer = new double[strategies.length];

		int workerCount = 0;
		// System.out.printf(",HO,MM,GB,OP\n");
		writer1.write(",HO,MM,GB,OP\n");
		double[] totalProb = new double[strategies.length];
		for (Game game : expSet.games) {

			// System.out.printf("%s, ", game.id);
			for (int i = 0; i < game.playerHitIds.length; i++) {
				for (int j = 0; j < strategies.length; j++) {
					String hitId = game.playerHitIds[i];
					List<Pair<String, String>> signalReportPairs = game
							.getSignalReportPairsForPlayer(hitId);

					likelihoodsPerPlayer[j] = strategies[j]
							.getLogLikelihood(signalReportPairs);
				}
				List<Strategy> bestPureStrategies = AnalysisUtils
						.getBestPureStrategies(strategies, likelihoodsPerPlayer);

				// System.out.printf("%d, ", workerCount);
				writer1.write(String.format("%d, ", workerCount));

				double[] prob = new double[strategies.length];
				for (Strategy str : bestPureStrategies) {
					if (str.toString().equals("(0.99,0.01)"))
						prob[0] = 1.0 / bestPureStrategies.size();
					else if (str.toString().equals("(0.99,0.99)"))
						prob[1] = 1.0 / bestPureStrategies.size();
					else if (str.toString().equals("(0.01,0.01)"))
						prob[2] = 1.0 / bestPureStrategies.size();
					else if (str.toString().equals("(0.01,0.99)"))
						prob[3] = 1.0 / bestPureStrategies.size();
					else
						System.out.printf(
								"Unrecognized best pure strategy, %s\n",
								str.toString());
				}
				for (int k = 0; k < prob.length; k++) {
					totalProb[k] = totalProb[k] + prob[k];
					// System.out.printf("%.4f,", prob[k]);
					writer1.write(String.format("%.4f,", prob[k]));
				}

				// for (Strategy str : bestPureStrategies) {
				// System.out.printf("%s ", str.toString());
				// writer1.write(String.format("%s ", str.toString()));
				// }
				writer1.write("\n");
				workerCount++;
				// System.out.println();
			}
		}
		// System.out.printf(",HO,MM,GB,OP\n");
		writer1.write(",HO,MM,GB,OP\n");
		writer1.write(",");
		for (int k = 0; k < totalProb.length; k++) {
			writer1.write(String.format("%.4f,", totalProb[k] / workerCount));
		}
		writer1.write("\n");
		writer1.flush();
		writer1.close();
		// System.out.println();
		// System.out.println();
	}

	private static void mixedStrategyPerWorker() throws IOException {

		BufferedWriter writerHO = new BufferedWriter(new FileWriter(
				rootDir + "worker-honest.csv"));

		BufferedWriter writerMM = new BufferedWriter(new FileWriter(
				rootDir + "worker-alwaysmm.csv"));
		
		BufferedWriter writerGB = new BufferedWriter(new FileWriter(
				rootDir + "worker-alwaysgb.csv"));

		BufferedWriter writerOP = new BufferedWriter(new FileWriter(
				rootDir + "worker-opposite.csv"));

		BufferedWriter writer = new BufferedWriter(new FileWriter(
				rootDir + "mixedStrategyPerWorker.csv"));

		writer.write(",MM->MM,GB->MM\n");
		int count = 0;
		for (Game game : expSet.games) {
			for (int i = 0; i < game.playerHitIds.length; i++) {

				String hitId = game.playerHitIds[i];	
				Strategy strategy = AnalysisUtils.getStrategyForPlayer(game,
						hitId);
				
				if (strategy.isHonest()) {
					writerHO.write(String.format("%s\n", hitId));
				} else if (strategy.isMM()) {
					writerMM.write(String.format("%s\n", hitId));
				} else if (strategy.isGB()) {
					writerGB.write(String.format("%s\n", hitId));
				} else if (strategy.isOpposite()) {
					writerOP.write(String.format("%s\n", hitId));
				}
				
				// System.out.printf("%d, %.2f, %.2f\n", count,
				// strategy.getPercent("MM", "MM"),
				// strategy.getPercent("GB", "GB"));
				writer.write(String.format("%d, %.2f, %.2f\n", count,
						strategy.getPercent("MM", "MM"),
						strategy.getPercent("GB", "MM")));
				count++;
			}
		}
		
		writerOP.flush();
		writerOP.close();
		
		writerGB.flush();
		writerGB.close();
		
		writerMM.flush();
		writerMM.close();
		
		writerHO.flush();
		writerHO.close();

		writer.flush();
		writer.close();

	}

	private static double[] pureStrategyPerRound() throws IOException {
		Strategy[] strategies = AnalysisUtils.getPureStrategyArray();

		BufferedWriter writer = new BufferedWriter(new FileWriter(
				rootDir + "pureStrategyPerRound.csv"));
		int numRounds = expSet.games.get(0).numRounds;
		double[] likelihoodsPerRound = new double[strategies.length];
		// System.out.printf(",HO,MM,GB,OP\n");
		writer.write(",HO,MM,GB,OP\n");
		for (int i = 0; i < numRounds; i++) {
			for (int j = 0; j < strategies.length; j++) {
				List<Pair<String, String>> signalReportPairs = expSet
						.getSignalReportPairsForRound(i);
				likelihoodsPerRound[j] = strategies[j]
						.getLogLikelihood(signalReportPairs);
			}
			List<Strategy> bestPureStrategies = AnalysisUtils
					.getBestPureStrategies(strategies, likelihoodsPerRound);
			// System.out.printf("%d,", i);
			writer.write(String.format("%d,", i));

			int[] count = new int[strategies.length];
			for (Strategy str : bestPureStrategies) {
				if (str.toString().equals("(0.99,0.01)"))
					count[0] = 1;
				else if (str.toString().equals("(0.99,0.99)"))
					count[1] = 1;
				else if (str.toString().equals("(0.01,0.01)"))
					count[2] = 1;
				else if (str.toString().equals("(0.01,0.99)"))
					count[3] = 1;
				else
					System.out.printf("Unrecognized best pure strategy, %s",
							str.toString());
			}
			for (int k = 0; k < count.length; k++) {
				// System.out.printf("%d,", count[k]);
				writer.write(String.format("%d,", count[k]));
			}

			// System.out.println();
			writer.write("\n");
		}
		writer.flush();
		writer.close();
		return likelihoodsPerRound;
	}

	private static void mixedStrategyPerRound() throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(
				rootDir + "mixedStrategyPerRound.csv"));
		writer.write(",MM->MM,GB->MM\n");
		int numRounds = expSet.games.get(0).numRounds;

		for (int i = 0; i < numRounds; i++) {
			Strategy strategy = AnalysisUtils.getStrategyForRound(expSet, i);

			// System.out.printf("%d, %.2f, %.2f\n", i,
			// strategy.getPercent("MM", "MM"),
			// strategy.getPercent("GB", "GB"));
			
			writer.write(String.format("%d, %.2f, %.2f\n", i,
					strategy.getPercent("MM", "MM"),
					strategy.getPercent("GB", "MM")));
		}
		writer.flush();
		writer.close();
	}

	private static void mixedStrategyPerNRounds(int numRounds) throws IOException {
		String filename = String.format(rootDir + "mixedStrategyPer%dRounds.csv", numRounds);
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		writer.write(",MM->MM,GB->MM\n");
		
		int totalNumRounds = expSet.games.get(0).numRounds;
		
		for (int i = 0; i < totalNumRounds; i += numRounds) {
			List<Pair<String, String>> signalReportPairs = 
					expSet.getSignalReportPairsForRoundRange(i, i + numRounds - 1);
			Strategy strategy = AnalysisUtils.getMixedStrategy(
					signalReportPairs, expSet.games.get(0).signalList);
			
			writer.write(String.format("%d, %.2f, %.2f\n", 
					i,
					strategy.getPercent("MM", "MM"),
					strategy.getPercent("GB", "MM")));
		}
		writer.flush();
		writer.close();
		
	}

	private static void mixedStrategyPerRoundPerGame() throws IOException {
		BufferedWriter writer = new BufferedWriter(
				new FileWriter(rootDir + "mixedStrategyPerRoundPerGame.csv"));

		for (Game game : expSet.games) {
			writer.write(",MM->MM,GB->MM\n");
			int count = 0;
			for (Round round : game.rounds) {
				List<Pair<String, String>> signalReportPairs = 
						game.getSignalReportPairsForGameRound(game, round);
				Strategy strategy = 
						AnalysisUtils.getMixedStrategy(signalReportPairs, game.signalList);
				
				writer.write(String.format("%d, %.2f, %.2f\n", 
						count,
						strategy.getPercent("MM", "MM"),
						strategy.getPercent("GB", "MM")));
				count++;
			}
			writer.write("\n\n");
		}
		writer.flush();
		writer.close();
		
	}
	
	private static void displayExitSurvey() throws IOException {
		BufferedWriter writer = new BufferedWriter(
				new FileWriter(rootDir + "exitsurvey.txt"));
		
		int count = 0;
		for (ExitSurvey comment : exitComments) {
			writer.write(String.format("%d, strategy %s, %s\n", 
					count, comment.checkedStrategies.toString(), comment.strategyComment));
			writer.write(String.format("learn: %s\n\n", comment.learnComment));
			count++;
		}
		
		writer.flush();
		writer.close();
	}

	private static void saveExitSurvey() {
		String dbUrl = "jdbc:mysql://localhost/turkserver";
		String dbClass = "com.mysql.jdbc.Driver";
		String setId = "mar-13-fixed";
		
		Connection con = null;
		exitComments = new ArrayList<ExitSurvey>();
		
		try {
			Class.forName(dbClass);
			con = DriverManager.getConnection(dbUrl, "root", "");

			String query = String.format("select hitId, comment from session " +
					"where comment is not null and setId='%s'", setId);
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				String hitId = rs.getString("hitId");
				String comment = rs.getString("comment");
				
				ExitSurvey survey = new ExitSurvey(comment);
				exitComments.add(survey);
			}
			rs.close();
			stmt.close();
			con.close();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		
	}

	private static void parseLog() {
		String dbUrl = "jdbc:mysql://localhost/turkserver";
		String dbClass = "com.mysql.jdbc.Driver";
		String setId = "mar-13-fixed";

		expSet = new Experiment();
		expSet.setId = setId;

		Connection con = null;

		// get experiment log
		Statement expStmt = null;
		ResultSet expRS = null;

		Statement killedStmt = null;
		ResultSet killedRS = null;

		// get round log
		Statement roundStmt = null;
		ResultSet roundRS = null;

		// get exit survey
		Statement exitSurveyStmt = null;
		ResultSet exitSurveyRS = null;

		try {
			Class.forName(dbClass);
			con = DriverManager.getConnection(dbUrl, "root", "");

			// Select killed games
			String killedGames = "select distinct experimentId from round where results like '%killed%'";
			killedStmt = con.createStatement();
			killedRS = killedStmt.executeQuery(killedGames);
			List<String> killedGameIds = new ArrayList<String>();
			while (killedRS.next()) {
				killedGameIds.add(killedRS.getString("experimentId"));
			}
			// System.out.println(killedGameIds.toString());

			expSet.numGames = getNumGames(expSet, con);

			String expQuery = String.format(
					"select * from experiment where setId='%s'", setId);
			expStmt = con.createStatement();
			expRS = expStmt.executeQuery(expQuery);

			while (expRS.next()) {
				String gameId = expRS.getString("id");
				if (killedGameIds.contains(gameId)) {
					// System.out.printf("Ignoring game %s with at least one killed player\n",
					// gameId);
					continue;
				}
				String expLog = expRS.getString("results");

				Game game = parseGameLog(expLog);
				game.id = gameId;

				for (int i = 0; i < game.numRounds; i++) {

					String roundQuery = String.format(
							"select * from round where"
									+ " experimentId = '%s' and roundnum = %d",
							gameId, i + 1);
					roundStmt = con.createStatement();
					roundRS = roundStmt.executeQuery(roundQuery);

					String roundLog = "";
					if (roundRS.next()) {
						roundLog = roundRS.getString("results");
					}
					Round roundInfo = parseRoundLog(roundLog, game);
					game.addRoundInfo(roundInfo);
				}

				// Save exit survey strings
				Map<String, String> exitSurveys = new HashMap<String, String>();
				for (int i = 0; i < game.playerHitIds.length; i++) {
					String hitId = game.playerHitIds[i];
					String exitSurveyQuery = String.format(
							"select comment from session where "
									+ "experimentId='%s' and hitId='%s'",
							gameId, hitId);
					exitSurveyStmt = con.createStatement();
					exitSurveyRS = exitSurveyStmt.executeQuery(exitSurveyQuery);
					exitSurveyRS.next();

					String exitComments = exitSurveyRS.getString("comment");
					// System.out.printf("%s\n", exitComments);
					exitSurveys.put(hitId, exitComments);
				}
				game.exitSurvey = exitSurveys;

				expSet.addGame(game);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (expRS != null)
					expRS.close();
				if (roundRS != null)
					roundRS.close();
				if (expStmt != null)
					expStmt.close();
				if (con != null)
					con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static Game parseGameLog(String experimentLogString) {
		Game gameInfo = new Game();

		Scanner sc = null;
		String currentLine = null;
		int lineIndex = 0;

		try {
			sc = new Scanner(experimentLogString);

			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcher = experimentStart.matcher(currentLine);
			if (matcher.matches()) {
				// System.out.println("Experiment start message found");
				// System.out.printf("%s, %s, %s\n\n", matcher.group(1),
				// matcher.group(2), matcher.group(3));
			} else
				throw new ParseException(
						"Did not find experiment start message", lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;
			matcher = prior.matcher(currentLine);
			if (matcher.matches()) {
				// System.out.println("Prior message found");
				// System.out.printf("%s, %s, %s\n\n", matcher.group(1),
				// matcher.group(2), matcher.group(3));
				gameInfo.savePriorProb(matcher.group(2));
				gameInfo.savePriorWorlds(matcher.group(3));
			} else
				throw new ParseException("Did not find prior message",
						lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;
			matcher = generalInfo.matcher(currentLine);
			if (matcher.matches()) {
				// System.out.println("General information message found");
				// System.out.printf("%s, %s, %s, %s, %s, %s\n\n",
				// matcher.group(1), matcher.group(2), matcher.group(3),
				// matcher.group(4), matcher.group(5), matcher.group(6));
				gameInfo.numPlayers = Integer.parseInt(matcher.group(2));
				gameInfo.numRounds = Integer.parseInt(matcher.group(3));
				gameInfo.savePlayerHitIds(matcher.group(4));
				gameInfo.savePaymentRule(matcher.group(5));
				gameInfo.saveSignalList(matcher.group(6));
			} else
				throw new ParseException(
						"Did not find general information message", lineIndex);

			int numRounds = gameInfo.numRounds;

			for (int i = 0; i < numRounds; i++) {
				currentLine = sc.nextLine();
				lineIndex++;
				matcher = experimentRoundStart.matcher(currentLine);
				if (matcher.matches()) {
					// System.out.println("Experiment round start message found");
					// System.out.printf("%s, %s\n\n", matcher.group(1),
					// matcher.group(2));
				} else
					throw new ParseException(
							"Did not find experiment round start message",
							lineIndex);

				currentLine = sc.nextLine();
				lineIndex++;
				matcher = experimentRoundFinish.matcher(currentLine);
				if (matcher.matches()) {
					// System.out.println("Experiment round finish message found");
					// System.out.printf("%s, %s\n\n", matcher.group(1),
					// matcher.group(2));
				} else
					throw new ParseException(
							"Did not find experiment round finish message",
							lineIndex);
			}

			currentLine = sc.nextLine();
			lineIndex++;
			matcher = experimentFinish.matcher(currentLine);
			if (matcher.matches()) {
				// System.out.println("Experiment finish message found");
				// System.out.printf("%s, %s, %s\n\n", matcher.group(1),
				// matcher.group(2), matcher.group(3));
			} else
				throw new ParseException(
						"Did not find experiment finish message", lineIndex);

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (sc != null)
				sc.close();
		}

		return gameInfo;
	}

	private static Round parseRoundLog(String roundResults, Game game) {
		Round roundInfo = new Round();

		Scanner sc = null;
		String currentLine = null;
		int lineIndex = 0;
		try {
			sc = new Scanner(roundResults);

			// parse round start
			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcher = roundStart.matcher(currentLine);
			if (matcher.matches()) {
				// System.out.println("Round start message found");
				// System.out.printf("%s, %s\n\n", matcher.group(1),
				// matcher.group(2));
				roundInfo.roundNum = Integer.parseInt(matcher.group(2));
			} else
				throw new ParseException("Did not find round start message",
						lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcherChosenWorld = chosenWorld.matcher(currentLine);
			if (matcherChosenWorld.matches()) {
				// System.out.println("Chosen world message found");
				// System.out.printf("%s, %s\n\n", matcherChosenWorld.group(1),
				// matcherChosenWorld.group(2));
				roundInfo.saveChosenWorld(matcherChosenWorld.group(2));
			} else
				throw new ParseException("Did not find chosen world message",
						lineIndex);

			int numPlayers = game.numPlayers;
			int count = 0;
			while (count < numPlayers) {
				currentLine = sc.nextLine();
				lineIndex++;

				Matcher matcherSignal = gotSignal.matcher(currentLine);
				if (matcherSignal.matches()) {
					// System.out.println("Signal message found");
					// System.out.printf("%s, %s, %s, %s\n\n",
					// matcherSignal.group(1), matcherSignal.group(2),
					// matcherSignal.group(3), matcherSignal.group(4));
					roundInfo.saveSignal(matcherSignal.group(2),
							matcherSignal.group(3), matcherSignal.group(4),
							matcherSignal.group(1));
				} else {
					throw new ParseException(
							"Expected a signal message but did not find one",
							lineIndex);
				}
				count++;
			}

			// parse get signal and choose report messages
			int limit = numPlayers;
			count = 0;
//			boolean killedFound = false;
			while (count < limit) {
				currentLine = sc.nextLine();
				lineIndex++;

				// Matcher matcherKilledMsg = killed .matcher(currentLine);
				Matcher matcherReport = choseReport.matcher(currentLine);
				// Matcher matcherFakeReport = fakeReport.matcher(currentLine);

				// if (matcherKilledMsg.matches()) {
				// // killed message found, there are 3 messages in this section
				// if (killedFound == false) {
				// killedFound = true;
				// limit++;
				// }
				// // write into game, round num, worker id, disconnected time
				//
				// } else
				if (matcherReport.matches()) {

					// System.out.println("Real report message found");
					// System.out.printf("%s, %s, %s, %s\n\n",
					// matcherReport.group(1), matcherReport.group(2),
					// matcherReport.group(3), matcherReport.group(4));
					roundInfo.saveReport(matcherReport.group(2),
							matcherReport.group(3), matcherReport.group(4),
							matcherReport.group(1));

				}
				// else if (matcherFakeReport.matches()) {
				// // fake report found, there are 3 messages in this section
				//
				// // System.out.println("Fake report message found");
				// // System.out.printf("%s, %s, %s, %s\n\n",
				// // matcherFakeReport.group(1),
				// // matcherFakeReport.group(2),
				// // matcherFakeReport.group(3),
				// // matcherFakeReport.group(4));
				//
				// roundInfo.saveReport(matcherFakeReport.group(2),
				// matcherFakeReport.group(3), matcherFakeReport.group(4),
				// matcherFakeReport.group(1));
				// }
				else {
					throw new ParseException(
							"Did not find real or fake report messages.",
							lineIndex);
				}
				count++;
			}

			// parse round result
			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcherResult = roundResult.matcher(currentLine);
			if (matcherResult.matches()) {
				// System.out.println("Round result message found");
				// System.out.printf("%s, %s\n\n", matcherResult.group(1),
				// matcherResult.group(2));
				roundInfo.saveResult(matcherResult.group(2));
			} else
				throw new ParseException("Did not find round result message",
						lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;

			while (true) {
				Matcher matcherBonus = getBonus.matcher(currentLine);
				Matcher matcherNoBonus = noBonus.matcher(currentLine);
				if (matcherBonus.matches() || matcherNoBonus.matches()) {
					// System.out.println("Get bonus message found");
					// System.out.printf("%s, %s, %s, %s\n\n",
					// matcherBonus.group(1), matcherBonus.group(2),
					// matcherBonus.group(3), matcherBonus.group(4));

					currentLine = sc.nextLine();
					lineIndex++;
				} else {
					break;
				}
			}

			// parse round end
			Matcher matcherEnd = roundEnd.matcher(currentLine);
			if (matcherEnd.matches()) {
				// System.out.println("Round end message found");
				// System.out.printf("%s, %s\n\n", matcherEnd.group(1),
				// matcherEnd.group(2));
				String endTimeString = matcherEnd.group(1);
				roundInfo.endTimeString = endTimeString;
				int min = Integer.parseInt(endTimeString.substring(0, 2));
				int sec = Integer.parseInt(endTimeString.substring(3, 5));
				int millisec = Integer.parseInt(endTimeString.substring(6, 9));
				int totalMillisec = min * 60 * 1000 + sec * 1000 + millisec;
				roundInfo.durationInMS = totalMillisec;
			} else
				throw new ParseException("Did not find round end message",
						lineIndex);

			return roundInfo;

		} catch (ParseException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (sc != null)
				sc.close();
		}
		return null;
	}

	private static int getNumGames(Experiment exp, Connection con)
			throws SQLException {
		String expCountQuery = String.format(
				"select count(*) as numgames from experiment where setId='%s'",
				exp.setId);
		Statement expCountStmt = con.createStatement();
		ResultSet expCountRS = expCountStmt.executeQuery(expCountQuery);
		expCountRS.next();
		int numGames = expCountRS.getInt("numgames");
		System.out.printf("set: %s, %d games\n\n", exp.setId, numGames);

		expCountStmt.close();
		expCountRS.close();
		return numGames;
	}

}
