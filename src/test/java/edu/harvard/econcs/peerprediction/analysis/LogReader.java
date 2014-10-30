package edu.harvard.econcs.peerprediction.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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

import net.andrewmao.models.games.SigActObservation;

public class LogReader {

	static String dbUrl = "jdbc:mysql://localhost/peerprediction";
	static String dbClass = "com.mysql.jdbc.Driver";
	static String setId = "vary-payment";
	
	 static String treatment = "prior2-basic";
//	 static String treatment = "prior2-outputagreement";
//	 static String treatment = "prior2-uniquetruthful";
//	 static String treatment = "prior2-symmlowpay";
//	 static String treatment = "prior2-constant";

	static final String rootDir = "/Users/alicexigao/Dropbox/peer-prediction/data/"
			+ treatment + "/";

	static Experiment expSet;

	public static void main(String[] args) throws Exception {

		File dir = new File(rootDir);
		dir.mkdirs();

		// Parsing game log and exit survey, and print info
		parseTextfile();
//		parseDB();
		// writePlayerCommentsToFile();
		printTreatmentInfo();

		// Statistics of raw data
//		writeRawDataToFile();
//		 graphRawData();
		 calcAvgBonus();

	}

	public static void parseDB() {
		System.out.println("Parsing mysql database");

		if (treatment.equals("prior2-constant")
				|| treatment.equals("prior2-symmlowpay"))
			MatchStrings.choseReport = MatchStrings.chosenReport1;
		else
			MatchStrings.choseReport = MatchStrings.chosenReport2;

		expSet = new Experiment();
		expSet.setId = setId;

		Connection con = null;

		// get experiment log
		Statement expStmt = null;
		ResultSet expRS = null;

		// get bonus
		Statement bonusStmt = null;
		ResultSet bonusRS = null;

		// get round log
		Statement roundStmt = null;
		ResultSet roundRS = null;

		// get exit survey
		Statement exitSurveyStmt = null;
		ResultSet exitSurveyRS = null;

		try {
			Class.forName(dbClass);
			con = DriverManager.getConnection(dbUrl, "root", "");

			// get total number of games
			String numGameQuery = String
					.format("select count(*) from experiment where setId='%s' and inputdata='%s'",
							setId, treatment);
			Statement numGameStmt = con.createStatement();
			ResultSet numGameRS = numGameStmt.executeQuery(numGameQuery);
			numGameRS.next();
			expSet.numGames = numGameRS.getInt(1);

			String expQuery = String
					.format("select * from experiment "
							+ "where setId='%s' and inputdata = '%s' "
							+ "and id not in (select distinct experimentId from round where results like '%%killed%%')",
							setId, treatment);
			expStmt = con.createStatement();
			expRS = expStmt.executeQuery(expQuery);

			while (expRS.next()) {
				String expLog = expRS.getString("results");
				Game game = parseGameLog(expLog);
				
				String gameId = expRS.getString("id");
				game.id = gameId;

				for (int i = 0; i < expSet.numRounds; i++) {

					String roundQuery = String.format("select * from round "
							+ "where experimentId = '%s' and roundnum = %d",
							gameId, i + 1);
					roundStmt = con.createStatement();
					roundRS = roundStmt.executeQuery(roundQuery);

					String roundLog = "";
					if (roundRS.next()) {
						roundLog = roundRS.getString("results");
					}
					Round round = parseRoundLog(roundLog, game);
					game.rounds.add(round);
				}

				// Save exit survey objects
				for (String hitId : game.playerHitIds) {
					String exitSurveyQuery = String.format(
							"select comment from session "
									+ "where comment is not null "
									+ "and experimentId='%s' "
									+ "and hitId='%s'", gameId, hitId);
					exitSurveyStmt = con.createStatement();
					exitSurveyRS = exitSurveyStmt.executeQuery(exitSurveyQuery);
					exitSurveyRS.next();

					String comment = exitSurveyRS.getString("comment");
					ExitSurvey exitSurvey = new ExitSurvey(comment);
					game.exitSurvey.put(hitId, exitSurvey);
				}

				String bonusQuery = String
						.format("select hitId, bonus from session where experimentId='%s'",
								gameId);
				bonusStmt = con.createStatement();
				bonusRS = bonusStmt.executeQuery(bonusQuery);
				while (bonusRS.next()) {
					String hitId = bonusRS.getString("hitId");
					double bonus = Double.parseDouble(bonusRS
							.getString("bonus"));
					game.bonus.put(hitId, bonus);
				}

				expSet.games.add(game);
			}

			// set parameters for this treatment
			expSet.nonKilledGames = expSet.games.size();

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

	public static void parseTextfile() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(rootDir
				+ "rawData.txt"));

		expSet = new Experiment();
		expSet.setId = setId;

		String str = reader.readLine();
		expSet.numPlayers = Integer.parseInt(str.split(":")[1]);

		str = reader.readLine();
		expSet.numRounds = Integer.parseInt(str.split(":")[1]);

		str = reader.readLine();
		expSet.numGames = Integer.parseInt(str.split(":")[1]);

		str = reader.readLine();
		expSet.nonKilledGames = Integer.parseInt(str.split(":")[1]);

		// prior probabilities
		str = reader.readLine();
		
		// worlds
		str = reader.readLine();
		
		str = reader.readLine();
		while (str.startsWith("Game ")) {
			Game game = new Game();
			expSet.games.add(game);

			// game id
			game.id = str.substring(5);
			
			List<String> playerStrings = new ArrayList<String>();
			for (int j = 0; j < expSet.numPlayers; j++) {
				playerStrings.add(reader.readLine());
			}

			game.playerHitIds = new String[expSet.numPlayers];
			List<String[]> playerResults = new ArrayList<String[]>();
			for (int j = 0; j < expSet.numPlayers; j++) {
				String[] splitStr = playerStrings.get(j).split(":");

				game.playerHitIds[j] = splitStr[0];

				String[] playerResult = splitStr[1].split(";");
				playerResults.add(playerResult);
			}

			game.rounds = new ArrayList<Round>();
			for (int k = 0; k < expSet.numRounds; k++) {
				Round round = new Round();
				round.roundNum = k;
				round.result = new HashMap<String, Map<String, Object>>();
				
				// parse signal, report, and refPlayer
				for (int j = 0; j < expSet.numPlayers; j++) {

					String result = playerResults.get(j)[k];
					result = result.substring(1, result.length() - 1);
					String[] resultArray = result.split(",");

					Map<String, Object> res = new HashMap<String, Object>();
					res.put("signal", resultArray[0]);
					res.put("report", resultArray[1]);
					
					if (treatment.equals("prior2-basic")
							|| treatment.equals("prior2-outputagreement")) {
						res.put("refPlayer", resultArray[2]);
					}
					
					round.result.put(game.playerHitIds[j], res);
				}
				
				// determine and save payoff
				for (int j = 0; j < expSet.numPlayers; j++) {
					String playerId = game.playerHitIds[j];
					String report = round.result.get(playerId).get("report")
							.toString();

					Object refInfo = null;
					if (treatment.equals("prior2-basic")
							|| treatment.equals("prior2-outputagreement")) {
						String refPlayer = round.result.get(playerId)
								.get("refPlayer").toString();
						String refReport = round.result.get(refPlayer)
								.get("report").toString();
						refInfo = refReport;
					} else if (treatment.equals("prior2-uniquetruthful")
							|| treatment.equals("prior2-symmlowpay")) {
						int numMM = Utils.getNumOfGivenReport(round.result,
								"MM", playerId);
						refInfo = numMM;
					}

					double reward = Utils
							.getPayment(treatment, report, refInfo);
					round.result.get(playerId).put("reward", reward);
				}
				
				game.rounds.add(round);
			}

			str = reader.readLine();
		}

		reader.close();
	}

	public static void writeRawDataToFile() throws IOException {
		System.out.println("Write raw data to file");

		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "rawData.txt"));

		writer.write(String.format(
				"number of players per game:%d\n"
				+ "number of rounds per game:%d\n"
				+ "number of games:%d\n"
				+ "number of games without disconnected player:%d\n"
				+ "prior probabilities:%s\n"
				+ "worlds:%s\n",
				expSet.numPlayers, expSet.numRounds,
				expSet.numGames,
				expSet.nonKilledGames,
				expSet.priorProbs,
				expSet.worlds
				));

		for (Game game : expSet.games) {
			writer.write(String.format("Game %s\n", game.id));
			
			List<String> playerIds = new ArrayList<String>();
			for (String hitId : game.playerHitIds) {
				playerIds.add(hitId);
			}
			for (String hitId : game.playerHitIds) {
				
				int playerIndex = playerIds.indexOf(hitId);
				writer.write(String.format("%d:", playerIndex));
				
				for (int i = 0; i < game.rounds.size(); i++) {
					
					Round r = game.rounds.get(i);
					String signal = r.getSignal(hitId);
					String report = r.getReport(hitId);

					if (treatment.equals("prior2-basic")
							|| treatment.equals("prior2-outputagreement")) {
						// Treatments 1 and 2, write reference player
						String refPlayer = (String) r.getRefPlayer(hitId);
						writer.write(String.format("(%s,%s,%d)", signal,
								report, playerIds.indexOf(refPlayer)));
					} else {
						// Other treatments, no need to write reference player
						writer.write(String.format("(%s,%s)", signal, report));
					}

					if (i == game.rounds.size() - 1) {
						writer.write("\n");
					} else {
						writer.write(";");
					}
				}
			}
		}

		if (treatment.equals("prior2-basic")) {
			writer.write("Payment rule:\n"
					+ "Each player's payoff depends on the player's report and the report of "
					+ "another player randomly chosen among all other players, as follows:\n"
					+ "(MM, MM) = 1.5, (MM, GB) = 0.1, (GB, GB) = 1.2, (GB, MM) = 0.3\n"
					+ "where (A, B) = X denotes that if a player P's report is A, P's reference report is B, "
					+ "then P's payoff is X.\n\n");
		} else if (treatment.equals("prior2-outputagreement")) {
			writer.write("Payment rule:\n"
					+ "Each player's payoff depends on the player's report and the report of "
					+ "another player randomly chosen among all other players, as follows:\n"
					+ "(MM, MM) = 1.5, (MM, GB) = 0.1, (GB, GB) = 1.5, (GB, MM) = 0.1\n"
					+ "where (A, B) = X denotes that if player P's report is A, P's reference report is B, "
					+ "then P's payoff is X.\n\n");
		} else if (treatment.equals("prior2-uniquetruthful")) {
			writer.write("Payment rule:\n"
					+ "Each player's payoff depends on the player's report and all the other reports, as follows:\n"
					+ "(MM, 3) = 0.8, (MM, 2) = 1.5, (MM, 1) = 0.1, (MM, 0) = 0.9, \n"
					+ "(GB, 3) = 0.9, (GB, 2) = 0.1, (GB, 1) = 1.5, (GB, 0) = 0.8, \n"
					+ "where (A, B) = X denotes that if player P's report is A, B of the other 3 reports are MM, "
					+ "then P's payoff is X.\n\n");
		} else if (treatment.equals("prior2-symmlowpay")) {
			writer.write("Payment rule:\n"
					+ "Each player's payoff depends on the player's report and all the other reports, as follows:\n"
					+ "(MM, 3) = 0.15, (MM, 2) = 1.50, (MM, 1) = 0.10, (MM, 0) = 0.10, \n"
					+ "(GB, 3) = 0.10, (GB, 2) = 0.15, (GB, 1) = 0.90, (GB, 0) = 0.15, \n"
					+ "where (A, B) = X denotes that if player P's report is A, B of the other 3 reports are MM, "
					+ "then P's payoff is X.\n\n");
		} else if (treatment.equals("prior2-constant")) {
			writer.write("Payment rule:\n"
					+ "Every player gets 0.90 for every round.\n\n");
		}

		writer.flush();
		writer.close();
	}

	private static Game parseGameLog(String experimentLogString) {

		Game g = new Game();

		Scanner sc = null;
		String currentLine = null;
		int lineIndex = 0;

		try {
			sc = new Scanner(experimentLogString);

			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcher = MatchStrings.experimentStart.matcher(currentLine);
			if (matcher.matches()) {
			} else
				throw new ParseException(
						"Did not find experiment start message", lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;
			matcher = MatchStrings.priorPattern.matcher(currentLine);
			if (matcher.matches()) {
				if (expSet.priorProbs == null)
					expSet.savePriorProbs(matcher.group(2));
				if (expSet.worlds == null)
					expSet.savePriorWorlds(matcher.group(3));
			} else
				throw new ParseException("Did not find prior message",
						lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;
			matcher = MatchStrings.generalInfo.matcher(currentLine);
			if (matcher.matches()) {
				if (expSet.numPlayers == -1)
					expSet.numPlayers = Integer.parseInt(matcher.group(2));
				if (expSet.numRounds == -1)
					expSet.numRounds = Integer.parseInt(matcher.group(3));
				
				g.savePlayerHitIds(matcher.group(4));
				
//				if (treatment.equals("prior2-basic") 
//						|| treatment.equals("prior2-outputagreement")) {
////					g.savePaymentRule(matcher.group(5));
//				}
			} else
				throw new ParseException(
						"Did not find general information message", lineIndex);

			for (int i = 0; i < expSet.numRounds; i++) {
				currentLine = sc.nextLine();
				lineIndex++;
				matcher = MatchStrings.experimentRoundStart
						.matcher(currentLine);
				if (!matcher.matches()) {
					throw new ParseException(
							"Did not find experiment round start message",
							lineIndex);
				}

				currentLine = sc.nextLine();
				lineIndex++;
				matcher = MatchStrings.experimentRoundFinish
						.matcher(currentLine);
				if (!matcher.matches()) {
					throw new ParseException(
							"Did not find experiment round finish message",
							lineIndex);
				}
			}

			currentLine = sc.nextLine();
			lineIndex++;
			matcher = MatchStrings.experimentFinish.matcher(currentLine);
			if (!matcher.matches()) {
				throw new ParseException(
						"Did not find experiment finish message", lineIndex);
			}

		} catch (ParseException e) {
			e.printStackTrace();
		} finally {
			if (sc != null)
				sc.close();
		}

		return g;
	}

	private static Round parseRoundLog(String roundResults, Game game) {
		Round r = new Round();

		Scanner sc = null;
		String currentLine = null;
		int lineIndex = 0;
		try {
			sc = new Scanner(roundResults);

			// parse round start
			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcher = MatchStrings.roundStart.matcher(currentLine);
			if (matcher.matches()) {
				r.roundNum = Integer.parseInt(matcher.group(2));
			} else
				throw new ParseException("Did not find round start message",
						lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcherChosenWorld = MatchStrings.chosenWorld
					.matcher(currentLine);
			if (matcherChosenWorld.matches()) {
				r.saveChosenWorld(matcherChosenWorld.group(2));
			} else
				throw new ParseException("Did not find chosen world message",
						lineIndex);

			int numPlayers = expSet.numPlayers;
			int count = 0;
			while (count < numPlayers) {
				currentLine = sc.nextLine();
				lineIndex++;

				Matcher matcherSignal = MatchStrings.gotSignal
						.matcher(currentLine);
				if (matcherSignal.matches()) {
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

				Matcher matcherReport = MatchStrings.choseReport
						.matcher(currentLine);

				if (matcherReport.matches()) {

					if (treatment.equals("prior2-constant")
							|| treatment.equals("prior2-symmlowpay")) {
						String radio = matcherReport.group(5);
						r.saveRadio(radio);
					}

				} else {
					throw new ParseException(
							"Did not find real or fake report messages.",
							lineIndex);
				}
				count++;
			}

			// parse round result
			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcherResult = MatchStrings.roundResult
					.matcher(currentLine);
			if (matcherResult.matches()) {
				r.saveResult(matcherResult.group(2));
			} else
				throw new ParseException("Did not find round result message",
						lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;

			while (true) {
				Matcher matcherBonus = MatchStrings.getBonus
						.matcher(currentLine);
				Matcher matcherNoBonus = MatchStrings.noBonus
						.matcher(currentLine);
				if (matcherBonus.matches() || matcherNoBonus.matches()) {
					currentLine = sc.nextLine();
					lineIndex++;
				} else {
					break;
				}
			}

			// parse round end
			Matcher matcherEnd = MatchStrings.roundEnd.matcher(currentLine);
			if (matcherEnd.matches()) {
				String endTimeString = matcherEnd.group(1);
				r.endTime = endTimeString;
				int min = Integer.parseInt(endTimeString.substring(0, 2));
				int sec = Integer.parseInt(endTimeString.substring(3, 5));
				int millisec = Integer.parseInt(endTimeString.substring(6, 9));
				int totalMillisec = min * 60 * 1000 + sec * 1000 + millisec;
				r.duration = totalMillisec;
			} else
				throw new ParseException("Did not find round end message",
						lineIndex);

			return r;

		} catch (ParseException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sc != null)
				sc.close();
		}
		return null;
	}

	private static void writePlayerCommentsToFile() throws IOException {
		System.out.println("Write player comments to file");

		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "playerComments.csv"));
		writer.write("gameId,hitId,actions,bonus,strategy,otherStrategy,reason,change,comments\n");

		for (Game game : expSet.games) {

			for (String hitId : game.playerHitIds) {
				
				List<SigActObservation<CandySignal, CandyReport>> signalReportPairs = 
						game.getSignalReportPairList(hitId);
				ExitSurvey survey = game.exitSurvey.get(hitId);

				if (survey == null)
					writer.write(String.format(
							"%s,%s,\"\"\"%s\"\"\",null,null,null,null,null\n",
							game.id, hitId, signalReportPairs));
				else
					writer.write(String.format("%s,%s," + "\"\"\"%s\"\"\","
							+ "%.2f," + "\"\"\"%s\"\"\"," + "\"\"\"%s\"\"\","
							+ "\"\"\"%s\"\"\"," + "\"\"\"%s\"\"\","
							+ "\"\"\"%s\"\"\"\n", game.id, hitId,
							signalReportPairs, game.bonus.get(hitId),
							survey.checkedStrategies, survey.otherStrategy,
							survey.strategyReason, survey.strategyChange,
							survey.comments));
			}
			writer.write("\n");
		}

		writer.flush();
		writer.close();

	}

	static void printTreatmentInfo() {
		System.out.printf("treatment: %s\n"
				+ "total num of games: %d\n"
				+ "non-killed games: %d\n"
//				+ "Prior probs: %s\n"
//				+ "Prior worlds: %s\n"
				+ "numPlayers per game: %d\n"
				+ "numRounds: %d\n\n", 
				treatment, 
				expSet.numGames,
				expSet.nonKilledGames, 
//				Arrays.toString(expSet.priorProbs),
//				expSet.worlds, 
				expSet.numPlayers, 
				expSet.numRounds);

	}

	public static void calcAvgBonus() {
		System.out.println("Write average bonus");

		int numPlayersPerGame = expSet.numPlayers;
		int numPlayers = numPlayersPerGame * expSet.nonKilledGames;

		double totalReward = 0;
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				for (Round round : game.rounds) {
					double reward = (double) round.getReward(hitId);
					totalReward += reward;
				}
			}
		}
		double avgReward = totalReward / numPlayers / expSet.numRounds;
		System.out.printf("Average reward: %.2f\n", avgReward);
	}

	/**
	 * For the raw data figure in EC'14 paper
	 */
	public static void graphRawData() throws IOException {
		System.out.println("Graph raw data");
	
		int numPlayersPerGame = expSet.numPlayers;
		int totalNumPlayers = numPlayersPerGame * expSet.nonKilledGames;
	
		double[] numMMSignalsMMReports = new double[expSet.numRounds];
		double[] numMMSignalsGBReports = new double[expSet.numRounds];
		double[] numGBSignalsMMReports = new double[expSet.numRounds];
		double[] numGBSignalsGBReports = new double[expSet.numRounds];
	
		for (Game game : expSet.games) {
			int index = 0;
			for (Round round : game.rounds) {
				for (String hitId : game.playerHitIds) {
					if (round.getSignal(hitId).equals("MM")) {
						if (round.getReport(hitId).equals("MM")) {
							numMMSignalsMMReports[index]++;
						} else {
							numMMSignalsGBReports[index]++;
						}
					} else {
						if (round.getReport(hitId).equals("MM")) {
							numGBSignalsMMReports[index]++;
						} else {
							numGBSignalsGBReports[index]++;
						}
					}
				}
				index++;
			}
		}
		for (int i = 0; i < expSet.numRounds; i++) {
			numMMSignalsMMReports[i] = numMMSignalsMMReports[i]
					/ totalNumPlayers;
			numMMSignalsGBReports[i] = numMMSignalsGBReports[i]
					/ totalNumPlayers;
			numGBSignalsMMReports[i] = numGBSignalsMMReports[i]
					/ totalNumPlayers;
			numGBSignalsGBReports[i] = numGBSignalsGBReports[i]
					/ totalNumPlayers;
		}
	
		// write to rawData.m
		BufferedWriter writerMatlab = new BufferedWriter(new FileWriter(rootDir
				+ "rawData.m"));
	
		writerMatlab.write("MMsignalsMMreports = "
				+ Arrays.toString(numMMSignalsMMReports) + "';\n");
		writerMatlab.write("MMsignalsGBreports = "
				+ Arrays.toString(numMMSignalsGBReports) + "';\n");
		writerMatlab.write("GBsignalsMMreports = "
				+ Arrays.toString(numGBSignalsMMReports) + "';\n");
		writerMatlab.write("GBsignalsGBreports = "
				+ Arrays.toString(numGBSignalsGBReports) + "';\n");
	
		writerMatlab
				.write(String
						.format("fH = figure;\n"
								+ "hBar = bar(0:%d, [MMsignalsGBreports MMsignalsMMreports  GBsignalsGBreports GBsignalsMMreports], "
								+ "'BarWidth', 0.7, 'BarLayout', 'stack', 'LineStyle', 'none');\n"
								+ "box off;\n", expSet.numRounds - 1));
		if (treatment.equals("prior2-constant")) {
			writerMatlab.write("set(fH, 'Position', [300, 300, 800, 400]);\n"
					+ "set(gca,'Position',[.1 .15 .88 .8]);\n");
		} else {
			writerMatlab.write("set(fH, 'Position', [300, 300, 500, 400]);\n"
					+ "set(gca,'Position',[.15 .15 .8 .8]);\n");
		}
		writerMatlab
				.write("xlh = xlabel('Round');\n"
						+ "ylh = ylabel('Percentage of players');\n"
						+ "set(xlh, 'FontSize', 26);\n"
						+ "set(ylh, 'FontSize', 26);\n"
						+ "axes = findobj(gcf,'type','axes');\n"
						+ "set(axes, 'FontSize', 20);\n"
						+ "axis([-1 20 0 1]);\n"
						+ "axes = findobj(gcf,'type','axes');\n"
						+ "set(axes, 'XTick', [0 19]);\n"
						+ "set(hBar,{'FaceColor'},{[1 0.27 0];[1 0.64 0];'b';[0.1 0.1 0.4];});\n");
		if (treatment.equals("prior2-constant")) {
			writerMatlab
					.write("AX=legend('MM signals, GB reports', 'MM signals, MM reports', 'GB signals, GB reports', 'GB signals, MM reports', "
							+ "'Location', 'BestOutside');\n"
							+ "LEG = findobj(AX,'type','text');\n"
							+ "set(LEG,'FontSize',22);\n"
							+ "set(LEG, 'FontWeight', 'bold');\n");
		}
		writerMatlab.flush();
		writerMatlab.close();
	}

}
