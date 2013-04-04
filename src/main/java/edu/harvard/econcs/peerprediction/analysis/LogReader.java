package edu.harvard.econcs.peerprediction.analysis;

import java.io.BufferedWriter;
import java.io.File;
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
import java.util.Arrays;
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

	static String setId = "mar-13-fixed";
	static String treatment = "ten-cent-base";
	static final String rootDir = "/Users/alicexigao/Dropbox/peer_prediction/data/" + treatment + "/";

	static Experiment expSet;
	static Map<String, ExitSurvey> exitComments;

	static String dbUrl = "jdbc:mysql://localhost/turkserver";
	static String dbClass = "com.mysql.jdbc.Driver";
	
	public static void main(String[] args) throws Exception {

		File dir = new File(rootDir);
		dir.mkdirs();
		
		parseLog();
		parseExitSurvey();
		
		System.out.printf("%d non-killed games\n\n", expSet.games.size());
		
		equilibriumPerGame();
		strategyPerWorker();
		strategyPerRound();

	}

	private static void equilibriumPerGame() throws IOException {
		System.out.println("Categorize worker behavior per game");
		
		int mmCount = 0;
		int gbCount = 0;
		int honestCount = 0;
		int otherCount = 0;
		
		List<Game> otherGames = new ArrayList<Game>();
		
		for (Game game : expSet.games) {
			
			int mmStart = 0;
			int gbStart = 0;
			int honestStart = 0;
			for (int i = 0; i < game.playerHitIds.length; i++) {
				String hitId = game.playerHitIds[i];
				
				int playerMMStart = game.getMMStart(hitId);
				mmStart = Math.max(mmStart, playerMMStart);
				
				int playerHonestStart = game.getHonestStart(hitId);
				honestStart = Math.max(honestStart, playerHonestStart);
				
				int playerGBStart = game.getGBStart(hitId);
				gbStart = Math.max(gbStart, playerGBStart);
			}
			
			System.out.printf("game %s, (%d, %d, %d) ", game.id, mmStart, honestStart, gbStart);
			if (mmStart > 5 && honestStart > 5 && gbStart > 5) {
				System.out.printf("other");
				otherGames.add(game);
				otherCount++;
			} else {

				int min1 = Math.min(mmStart, honestStart);
				int min = Math.min(min1, gbStart);
				// if there are ties, both category will show

				if (min == mmStart) {
					System.out.printf("mm ");
					mmCount++;
				}
				if (honestStart == min) {
					System.out.printf("honest ");
					honestCount++;
				}
				if (gbStart == min) {
					System.out.printf("gb ");
					gbCount++;
				}
			}
			System.out.println();
		}
		System.out.printf("mm %d, honest %d, gb %d, other %d\n\n", mmCount, honestCount, gbCount, otherCount);
		
		// categorize other games
		int otherMMHonest = 0;
		int otherMM = 0;
		int otherHonest = 0;
		int otherGB = 0;
		int otherOther = 0;
		for (Game game : otherGames) {
			int numMM = Integer.MAX_VALUE;
			int numGB = Integer.MAX_VALUE;
			int numHonest = Integer.MAX_VALUE;
			
			for (int i = 0; i < game.playerHitIds.length; i++) {
				String hitId = game.playerHitIds[i];
				
				int playerNumMM = game.getNumMM(hitId); 
				numMM = Math.min(numMM, playerNumMM);
				
				int playerNumGB = game.getNumGB(hitId);
				numGB = Math.min(numGB, playerNumGB);
				
				int playerNumHonest = game.getNumHonest(hitId);
				numHonest = Math.min(numHonest, playerNumHonest);
			}
			
			System.out.printf("game %s, (%d, %d, %d) ", game.id, numMM, numGB, numHonest);
			
			int max1 = Math.max(numMM, numGB);
			int max = Math.max(max1, numHonest);
			
			if (max < 5) {
				System.out.printf("other ");
				otherOther++;
			} else {
				if (numMM == max && numHonest == max) {
					System.out.printf("mm or honest ");
					otherMMHonest++;
				} else if (numMM == max) {
					System.out.printf("mm ");
					otherMM++;
				} else if (numHonest == max) {
					System.out.printf("honest ");
					otherHonest++;
				} else if (numGB == max) {
					System.out.printf("gb ");
					otherGB++;
				}
			}
			System.out.println();
		}
		
		System.out.printf("mm or honest %d, mm %d, honest %d, gb, %d, other %d\n", 
				otherMMHonest, otherMM, otherHonest, otherGB, otherOther);
		System.out.println();
		System.out.println();
		
	}
	
	private static void strategyPerWorker() throws IOException {
		System.out.println("Categorize worker strategies");
		
		BufferedWriter writer = null;
		
		BufferedWriter writerHonest = new BufferedWriter(
				new FileWriter(rootDir + "workers-honest.csv"));
		writerHonest.write("gameId, hitId, actions, inferredStrategy, " +
				"claimedStrategies, " +
				"strategyComment, learnComment\n");
		
		BufferedWriter writerMM = new BufferedWriter(
				new FileWriter(rootDir + "workers-alwaysmm.csv"));
		writerMM.write("gameId, hitId, actions, inferredStrategy, " +
				"claimedStrategies, " +
				"strategyComment, learnComment\n");
		
		BufferedWriter writerGB = new BufferedWriter(
				new FileWriter(rootDir + "workers-alwaysgb.csv"));
		writerGB.write("gameId, hitId, actions, inferredStrategy, " +
				"claimedStrategies, " +
				"strategyComment, learnComment\n");
		
		BufferedWriter writerOpposite = new BufferedWriter(
				new FileWriter(rootDir + "workers-opposite.csv"));
		writerOpposite.write("gameId, hitId, actions, inferredStrategy, " +
				"claimedStrategies, " +
				"strategyComment, learnComment\n");

		BufferedWriter writerOther = new BufferedWriter(
				new FileWriter(rootDir + "workers-other.csv"));
		writerOther.write("gameId, hitId, actions, inferredStrategy, " +
				"claimedStrategies, " +
				"strategyComment, learnComment\n");
		
		for (Game game : expSet.games) {
	
			for (int i = 0; i < game.playerHitIds.length; i++) {
				
				// hitId
				String hitId = game.playerHitIds[i];
				
				// signal report pairs
				List<Pair<String, String>> signalReportPairs = 
						game.getSignalReportPairsForPlayer(hitId);

				// exit survey 
				ExitSurvey comments = exitComments.get(hitId);

				// strategy
				Strategy strategy = AnalysisUtils.getStrategyForPlayer(
						game, hitId);

				// assign correct writer
				if (strategy.isHonest()) {
					writer = writerHonest;
				} else if (strategy.isMM()) {
					writer = writerMM;
				} else if (strategy.isGB()) {
					writer = writerGB;
				} else if (strategy.isOpposite()) {
					writer = writerOpposite;
				} else {
					if (strategy.isCloseToMM(0.7) || 
							AnalysisUtils.fractionMMReports(signalReportPairs) >= 0.7) {
						writer = writerMM;
					} else if (strategy.isCloseToHonest(0.7)) {
						writer = writerHonest;
					} else if (strategy.isCloseToGB(0.7) || 
							AnalysisUtils.fractionMMReports(signalReportPairs) <= 0.3) {
						writer = writerGB;
					} else {
						writer = writerOther;
					}
				}

				writer.write(String.format("%s, %s,\"%s\",", 
						game.id, hitId, signalReportPairs.toString()));

				if (strategy.isHonest()) {
					writer.write(String.format("honest,"));
				} else if (strategy.isMM()) {
					writer.write(String.format("mm,"));
				} else if (strategy.isGB()) {
					writer.write(String.format("gb,"));
				} else if (strategy.isOpposite()) {
					writer.write(String.format("opposite,"));
				} else {
					if (strategy.isCloseToMM(0.7)) {
						writer.write(String.format("%s,", strategy.label));
					} else {
						writer.write(String.format("other,"));
					}
				}
				
				if (comments != null && comments.checkedStrategies != null) {
					writer.write(String.format("\"\"\"%s\"\"\",", comments.checkedStrategies));
				} else {
					writer.write("\"null\",");
				}
	
				if (comments != null && comments.strategyComment != null) {
					writer.write(String.format("\"\"\"%s\"\"\",",
							comments.strategyComment));
				} else {
					writer.write("\"null\",");
				}
				
				if (comments != null && comments.learnComment != null) {
					writer.write(String.format("\"\"\"%s\"\"\",",
							comments.learnComment));
				} else {
					writer.write("\"null\",");
				}
	
				writer.write("\n");
	
			}
		}
		
//		System.out.printf("# honest players %d\n", numHonest);
//		System.out.printf("# MM players %d\n", numMM);
//		System.out.printf("# other players %d\n", expSet.games.size() * 3);
		System.out.println();
		System.out.println();
		
		writerHonest.write("\n");
		writerHonest.flush();
		writerHonest.close();
		
		writerMM.write("\n");
		writerMM.flush();
		writerMM.close();
		
		writerGB.write("\n");
		writerGB.flush();
		writerGB.close();

		writerOpposite.write("\n");
		writerOpposite.flush();
		writerOpposite.close();
		
		writerOther.write("\n");
		writerOther.flush();
		writerOther.close();
		
	}

	private static void strategyPerRound() throws IOException {
		strategyPerNRoundEMStr(1);
		strategyPerNRoundEMStr(2);
		strategyPerNRoundEMStr(5);
		
		strategyPerRoundPureStr();
		
		strategyDistributionPerRound();

	}

	/**
	 * Start with almost pure strategies
	 * 
	 * @throws IOException
	 */
	private static void strategyPerRoundPureStr() throws IOException {

		System.out.println("Round Strategies - Starting with Pure Strategies");
		
		Strategy[] strategies = new Strategy[] {
				AnalysisUtils.getHonestStrategy(),
				AnalysisUtils.getMMStrategy(), 
				AnalysisUtils.getGBStrategy(),
				AnalysisUtils.getOppositeStrategy() };
		System.out.printf("%s\n", Arrays.toString(strategies));
		
		double[] prob = new double[strategies.length];
		for (int i = 0; i < prob.length; i++) {
			prob[i] = 1.0 / prob.length;
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "strategyPerRound-pureStrategies.csv"));
		
		for (int i = 0; i < strategies.length; i++) {
			writer.write("," + strategies[i].label);
		}
		writer.write("\n");

		int numRounds = expSet.games.get(0).numRounds;
		for (int roundNum = 0; roundNum < numRounds; roundNum++) {
			System.out.printf("%d,", roundNum);
			writer.write(String.format("%d,", roundNum));

			double[] pi_new = AnalysisUtils.calcPosteriorProb(
					prob, strategies,
					expSet.getSignalReportPairsForRoundGroupByGame(roundNum));

			for (int i = 0; i < pi_new.length; i++) {
				System.out.printf("%.4f,", pi_new[i]);
				writer.write(String.format("%.4f,", pi_new[i]));
			}
			writer.write("\n");
			System.out.println();
		}

		System.out.println();
		writer.flush();
		writer.close();

	}

	private static void strategyPerNRoundEMStr(
			List<String> workersToExclude, int n) throws IOException {

		System.out.println("Round Strategies - Starting With EM Strategies");
		List<List<Pair<String, String>>> signalReportPairs = 
				expSet.getSignalReportPairsGroupByGameExcludeWorkers(workersToExclude);
		
		AnalysisUtils.em_K = 2; // number of strategies
		int numEM = 10;
		int count = 0;
		double likelihood = Double.NEGATIVE_INFINITY;
		Strategy[] strategies = null;
		double[] prob = null;
		while (count < numEM) {
			
			AnalysisUtils.runEMAlgorithm(signalReportPairs);
			
			if (AnalysisUtils.em_likelihood > likelihood) {
				strategies = AnalysisUtils.em_strategies;
				prob = AnalysisUtils.em_pi;
				likelihood = AnalysisUtils.em_likelihood;
			}
			count++;
		}
		System.out.printf("strategies: %s\n", Arrays.toString(strategies));
		System.out.printf("probs: %s\n", Arrays.toString(prob));
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(
				rootDir + "strategyPer" + n + "Round-emStrategies-excludeWorkers.csv"));

		writer.write(",");
		for (int i = 0; i < strategies.length; i++) {
			writer.write(String.format("%s,", strategies[i].label));
		}
		writer.write("\n");

		int totalNumRounds = expSet.games.get(0).numRounds;
		for (int i = 0; i < totalNumRounds; i += n) {
			System.out.printf("%d,", i);
			writer.write(String.format("%d,", i));

			signalReportPairs = expSet
					.getSignalReportPairsForRoundRangeGroupByGame(i, i + n - 1);

			double[] pi_new = AnalysisUtils.calcPosteriorProb(
					prob, strategies,
					signalReportPairs);

			for (int j = 0; j < pi_new.length; j++) {
				System.out.printf("%.4f,", pi_new[j]);
				writer.write(String.format("%.4f,", pi_new[j]));
			}
			writer.write("\n");
			System.out.println();
		}
		System.out.println();	
		System.out.println();
		writer.flush();
		writer.close();
	}
	
	private static void strategyPerNRoundEMStr(int n) throws IOException {

		System.out.println("Round Strategies - Starting With EM Strategies");
		List<List<Pair<String, String>>> signalReportPairs = expSet
				.getSignalReportPairsGroupByGame();
		
		AnalysisUtils.em_K = 4; // number of strategies
		int numEM = 100;
		int count = 0;
		double likelihood = Double.NEGATIVE_INFINITY;
		Strategy[] strategies = null;
		double[] prob = null;
		while (count < numEM) {
			
			AnalysisUtils.runEMAlgorithm(signalReportPairs);
//			AnalysisUtils.runEMAlgorithmAnna(signalReportPairs);
			
			if (AnalysisUtils.em_likelihood > likelihood) {
				strategies = AnalysisUtils.em_strategies;
				prob = AnalysisUtils.em_pi;
				likelihood = AnalysisUtils.em_likelihood;
			}
			count++;
		}
		System.out.printf("strategies: %s\n", Arrays.toString(strategies));
		System.out.printf("probs: %s\n", Arrays.toString(prob));
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(
				rootDir + "strategyPer" + n + "Round-emStrategies.csv"));

		writer.write(",");
		for (int i = 0; i < strategies.length; i++) {
			writer.write(String.format("%s,", strategies[i].label));
		}
		writer.write("\n");

		int totalNumRounds = expSet.games.get(0).numRounds;
		for (int i = 0; i < totalNumRounds; i += n) {
			System.out.printf("%d,", i);
			writer.write(String.format("%d,", i));

			signalReportPairs = expSet
					.getSignalReportPairsForRoundRangeGroupByGame(i, i + n - 1);

			double[] pi_new = AnalysisUtils.calcPosteriorProb(
					prob, strategies,
					signalReportPairs);

			for (int j = 0; j < pi_new.length; j++) {
				System.out.printf("%.4f,", pi_new[j]);
				writer.write(String.format("%.4f,", pi_new[j]));
			}
			writer.write("\n");
			System.out.println();
		}
		System.out.println();	
		System.out.println();
		writer.flush();
		writer.close();
	}

	private static void strategyDistributionPerRound() throws IOException {
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "pureStrategyDistributionPerRound.csv"));
	
		System.out.println("Strategy distribution per round");
		writer.write(",honest,MM,GB,opposite,\n");
		int numRounds = expSet.games.get(0).numRounds;
		for (int roundNum = 0; roundNum < numRounds; roundNum++) {
			double[] dist = new double[]{0.0,0.0,0.0,0.0};
			
			List<List<Pair<String, String>>> signalReportPairs = 
					expSet.getSignalReportPairsForRoundGroupByGame(roundNum);
			
			for (List<Pair<String, String>> dataPoint : signalReportPairs) {
				
				double[] pureStrategyDistribution = 
						AnalysisUtils.getBestPureStrategies(dataPoint);
				for (int i = 0; i < dist.length; i++) {
					dist[i] = dist[i] + pureStrategyDistribution[i];
				}
				
			}
			
			double total = 0.0;
			for (int i = 0; i < dist.length; i++) {
				total = total + dist[i];
			}
			for (int i = 0; i < dist.length; i++) {
				dist[i] = dist[i] / total;
			}
			
			writer.write(String.format("%d,", roundNum));
			for (int i = 0; i < dist.length; i++) {
				writer.write(String.format("%.4f,", dist[i]));
			}
			writer.write("\n");
			System.out.printf("%d, %s\n", roundNum, Arrays.toString(dist));
		}
		writer.flush();
		writer.close();
		
	}

	private static void parseLog() {

		expSet = new Experiment();
		expSet.setId = setId;

		Connection con = null;

		// get experiment log
		Statement expStmt = null;
		ResultSet expRS = null;

		// get round log
		Statement roundStmt = null;
		ResultSet roundRS = null;

		// get exit survey
		Statement exitSurveyStmt = null;
		ResultSet exitSurveyRS = null;

		try {
			Class.forName(dbClass);
			con = DriverManager.getConnection(dbUrl, "root", "");

			String expQuery = "select * from experiment " +
					"where setId='" + setId + "' and inputdata = '" + treatment + "' "
					+ "and id not in (select distinct experimentId from round where results like '%killed%')";
			expStmt = con.createStatement();
			expRS = expStmt.executeQuery(expQuery);

			while (expRS.next()) {
				String gameId = expRS.getString("id");
				String expLog = expRS.getString("results");

				Game game = parseGameLog(expLog);
				game.id = gameId;

				for (int i = 0; i < game.numRounds; i++) {

					String roundQuery = String.format(
							"select * from round " +
							"where experimentId = '%s' and roundnum = %d",
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
							"select comment from session " +
							"where experimentId='%s' and hitId='%s'",
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
				// gameInfo.saveSignalList(matcher.group(6));
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
			// boolean killedFound = false;
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

	private static void parseExitSurvey() {
	
		Connection con = null;
		exitComments = new HashMap<String, ExitSurvey>();
	
		try {
			Class.forName(dbClass);
			con = DriverManager.getConnection(dbUrl, "root", "");
	
			String query = String.format("select hitId, comment from session "
					+ "where comment is not null and setId='%s'", setId);
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				String hitId = rs.getString("hitId");
				String comment = rs.getString("comment");
	
				ExitSurvey survey = new ExitSurvey(comment);
				exitComments.put(hitId, survey);
			}
			rs.close();
			stmt.close();
			con.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	
	}

	
/*
	private static void mixedStrategyPerWorker() throws IOException {

		BufferedWriter writerHO = new BufferedWriter(new FileWriter(rootDir
				+ "worker-honest.csv"));

		BufferedWriter writerMM = new BufferedWriter(new FileWriter(rootDir
				+ "worker-alwaysmm.csv"));

		BufferedWriter writerGB = new BufferedWriter(new FileWriter(rootDir
				+ "worker-alwaysgb.csv"));

		BufferedWriter writerOP = new BufferedWriter(new FileWriter(rootDir
				+ "worker-opposite.csv"));

		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "mixedStrategyPerWorker.csv"));

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

	private static void mixedStrategyPerRound() throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "mixedStrategyPerRound.csv"));
		writer.write(",MM->MM,GB->MM\n");
		int numRounds = expSet.games.get(0).numRounds;

		for (int i = 0; i < numRounds; i++) {
			Strategy strategy = AnalysisUtils.getStrategyForRound(expSet, i);

			writer.write(String.format("%d, %.2f, %.2f\n", i,
					strategy.getPercent("MM", "MM"),
					strategy.getPercent("GB", "MM")));
		}
		writer.flush();
		writer.close();
	}

	private static void mixedStrategyPerRound(List<String> workersToExclude)
			throws IOException {

		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "mixedStrategyPerRound_excludeWorkers.csv"));
		writer.write(",MM->MM,GB->MM\n");
		int numRounds = expSet.games.get(0).numRounds;

		for (int i = 0; i < numRounds; i++) {
			Strategy strategy = AnalysisUtils
					.getStrategyForRoundExcludeWorkers(expSet, i,
							workersToExclude);

			writer.write(String.format("%d, %.2f, %.2f\n", i,
					strategy.getPercent("MM", "MM"),
					strategy.getPercent("GB", "MM")));
		}
		writer.flush();
		writer.close();
	}

	private static void mixedStrategyPerNRounds(int numRounds)
			throws IOException {
		String filename = String.format(rootDir
				+ "mixedStrategyPer%dRounds.csv", numRounds);
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		writer.write(",MM->MM,GB->MM\n");

		int totalNumRounds = expSet.games.get(0).numRounds;
		for (int i = 0; i < totalNumRounds; i += numRounds) {
			List<Pair<String, String>> signalReportPairs = expSet
					.getSignalReportPairsForRoundRange(i, i + numRounds - 1);
			Strategy strategy = AnalysisUtils.getMixedStrategy(
					signalReportPairs, AnalysisUtils.signalList);

			writer.write(String.format("%d, %.2f, %.2f\n", i,
					strategy.getPercent("MM", "MM"),
					strategy.getPercent("GB", "MM")));
		}
		writer.flush();
		writer.close();

	}

	private static void mixedStrategyPerNRounds(List<String> hitIds,
			int numRounds) throws IOException {
		String filename = String.format(rootDir
				+ "mixedStrategyPer%dRounds_excludeWorkers.csv", numRounds);
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		writer.write(",MM->MM,GB->MM\n");

		int totalNumRounds = expSet.games.get(0).numRounds;

		for (int i = 0; i < totalNumRounds; i += numRounds) {
			List<Pair<String, String>> signalReportPairs = expSet
					.getSignalReportPairsForRoundRangeExcludeWorkers(i, i
							+ numRounds - 1, hitIds);
			Strategy strategy = AnalysisUtils.getMixedStrategy(
					signalReportPairs, AnalysisUtils.signalList);

			writer.write(String.format("%d, %.2f, %.2f\n", i,
					strategy.getPercent("MM", "MM"),
					strategy.getPercent("GB", "MM")));
		}
		writer.flush();
		writer.close();
	}
*/
}
