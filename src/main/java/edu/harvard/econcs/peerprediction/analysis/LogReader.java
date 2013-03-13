package edu.harvard.econcs.peerprediction.analysis;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	static final Pattern choseReport = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) chose report ([A-Z]{2})");
	static final Pattern fakeReport = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) killed, put in fake report ([A-Z]{2})");
	static final Pattern roundResult = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round result is (.*)");
	static final Pattern roundEnd = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round ([0-9]+) finished");
	static final Pattern getBonus = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) gets bonus ([0-9.]+)");
	static final Pattern chosenWorld = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Chosen world is (.*)");;

	static GameInfo gameInfo;
	private static Pattern killed = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) killed, because disconnected for ([0-9]+) milliseconds");
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		parseInfo();
		
		analyzeInfo();
	}

	private static void analyzeInfo() {
		String[] playerNames = gameInfo.getPlayerNames();
		Map<String, Map<String, Double>> strategy = gameInfo.getStrategy(playerNames[0], roundStart, roundEnd);
	}

	private static void parseInfo() {
		String dbUrl = "jdbc:mysql://localhost/turkserver";
		String dbClass = "com.mysql.jdbc.Driver";
		String setId = "test set";

		Connection con = null;
		Statement expStmt = null;
		ResultSet expRS = null;
		Statement roundStmt = null;
		ResultSet roundRS = null;

		try {
			Class.forName(dbClass);
			con = DriverManager.getConnection(dbUrl, "root", "");
			expStmt = con.createStatement();
			String expQuery = String.format("select * from experiment where setId='%s'", setId);
			expRS = expStmt.executeQuery(expQuery);

			while (expRS.next()) {
				String expId = expRS.getString("id");
				String expLog = expRS.getString("results");

				gameInfo = parseGameInfo(expLog);
				
				for (int i = 1; i <= gameInfo.getNumRounds(); i++) {

					String roundQuery = String.format(
							"select * from round where"
									+ " experimentId = '%s' and roundnum = %d",
							expId, i);
					roundStmt = con.createStatement();
					roundRS = roundStmt.executeQuery(roundQuery);
					
					String roundLog = "";
					if (roundRS.next()) {
						roundLog = roundRS.getString("results");
						System.out.printf("round %d log: %s", i, roundLog);
					}
					RoundInfo roundInfo = parseRoundInfo(roundLog);
					gameInfo.addRoundInfo(roundInfo);
				}
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

	private static GameInfo parseGameInfo(String experimentLogString) {
		GameInfo gameInfo = new GameInfo();

		Scanner sc = null;
		String currentLine = null;
		int lineIndex = 0;

		try {
			sc = new Scanner(experimentLogString);

			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcher = experimentStart.matcher(currentLine);
			if (matcher.matches()) {
				System.out.println("Experiment start message found");
				System.out.printf("%s, %s, %s\n\n", matcher.group(1),
						matcher.group(2), matcher.group(3));
			} else
				throw new ParseException(
						"Did not find experiment start message", lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;
			matcher = prior.matcher(currentLine);
			if (matcher.matches()) {
				System.out.println("Prior message found");
				System.out.printf("%s, %s, %s\n\n", matcher.group(1),
						matcher.group(2), matcher.group(3));
				gameInfo.setPriorProb(matcher.group(2));
				gameInfo.setPriorWorlds(matcher.group(3));
			} else
				throw new ParseException("Did not find prior message",
						lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;
			matcher = generalInfo.matcher(currentLine);
			if (matcher.matches()) {
				System.out.println("General information message found");
				System.out.printf("%s, %s, %s, %s, %s, %s\n\n",
						matcher.group(1), matcher.group(2), matcher.group(3),
						matcher.group(4), matcher.group(5), matcher.group(6));
				gameInfo.setNumPlayers(matcher.group(2));
				gameInfo.setNumRounds(matcher.group(3));
				gameInfo.setPlayerNames(matcher.group(4));
				gameInfo.setPaymentRule(matcher.group(5));
				gameInfo.setSignalList(matcher.group(6));
			} else
				throw new ParseException(
						"Did not find general information message", lineIndex);

			int numRounds = gameInfo.getNumRounds();

			for (int i = 1; i <= numRounds; i++) {
				currentLine = sc.nextLine();
				lineIndex++;
				matcher = experimentRoundStart.matcher(currentLine);
				if (matcher.matches()) {
					System.out.println("Experiment round start message found");
					System.out.printf("%s, %s\n\n", matcher.group(1),
							matcher.group(2));
				} else
					throw new ParseException(
							"Did not find experiment round start message",
							lineIndex);

				currentLine = sc.nextLine();
				lineIndex++;
				matcher = experimentRoundFinish.matcher(currentLine);
				if (matcher.matches()) {
					System.out.println("Experiment round finish message found");
					System.out.printf("%s, %s\n\n", matcher.group(1),
							matcher.group(2));
				} else
					throw new ParseException(
							"Did not find experiment round finish message",
							lineIndex);
			}

			currentLine = sc.nextLine();
			lineIndex++;
			matcher = experimentFinish.matcher(currentLine);
			if (matcher.matches()) {
				System.out.println("Experiment finish message found");
				System.out.printf("%s, %s, %s\n\n", matcher.group(1),
						matcher.group(2), matcher.group(3));
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

	private static RoundInfo parseRoundInfo(String roundResults) {
		RoundInfo roundInfo = new RoundInfo();

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
				System.out.println("Round start message found");
				System.out.printf("%s, %s\n\n", matcher.group(1),
						matcher.group(2));
				roundInfo.setRoundNum(Integer.parseInt(matcher.group(2)));
			} else
				throw new ParseException("Did not find round start message",
						lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcherChosenWorld = chosenWorld.matcher(currentLine);
			if (matcherChosenWorld.matches()) {
				System.out.println("Chosen world message found");
				System.out.printf("%s, %s\n\n", matcherChosenWorld.group(1),
						matcherChosenWorld.group(2));
				roundInfo.setChosenWorld(matcherChosenWorld.group(2));
			} else
				throw new ParseException("Did not find chosen world message",
						lineIndex);

			int numPlayers = gameInfo.getNumPlayers();
			int count = 0;
			while (count < numPlayers) {
				currentLine = sc.nextLine();
				lineIndex++;

				Matcher matcherSignal = gotSignal.matcher(currentLine);		
				if (matcherSignal.matches()) {
					System.out.println("Signal message found");
					System.out.printf("%s, %s, %s, %s\n\n",
							matcherSignal.group(1), matcherSignal.group(2),
							matcherSignal.group(3), matcherSignal.group(4));
					roundInfo.setSignal(matcherSignal.group(2),
							matcherSignal.group(3), matcherSignal.group(4),
							matcherSignal.group(1));
				} else {
					throw new ParseException("Expected a signal message but did not find one", lineIndex);
				}
				count++;
			}
			
			// parse get signal and choose report messages
			int limit = numPlayers;
			count = 0;
			while (count < limit) {
				currentLine = sc.nextLine();
				lineIndex++;
				System.out.println("line is " + currentLine);

				Matcher matcherKilledMsg = killed .matcher(currentLine);
				Matcher matcherReport = choseReport.matcher(currentLine);
				Matcher matcherFakeReport = fakeReport.matcher(currentLine);

				if (matcherKilledMsg.matches()) {
					limit++;
					// write into game, round num, worker id, disconnected time
					
				} else if (matcherReport.matches()) {

					System.out.println("Real report message found");
					System.out.printf("%s, %s, %s, %s\n\n",
							matcherReport.group(1), matcherReport.group(2),
							matcherReport.group(3), matcherReport.group(4));
					roundInfo.setReport(matcherReport.group(2),
							matcherReport.group(3), matcherReport.group(4),
							matcherReport.group(1));
				} else if (matcherFakeReport.matches()) {

					System.out.println("Fake report message found");
					System.out.printf("%s, %s, %s, %s\n\n",
							matcherFakeReport.group(1),
							matcherFakeReport.group(2),
							matcherFakeReport.group(3),
							matcherFakeReport.group(4));
					// roundInfo.setReport(matcherFakeReport.group(2),
					// matcherFakeReport.group(3), matcherFakeReport.group(4),
					// matcherFakeReport.group(1));
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
			Matcher matcherResult = roundResult.matcher(currentLine);
			if (matcherResult.matches()) {
				System.out.println("Round result message found");
				System.out.printf("%s, %s\n\n", matcherResult.group(1),
						matcherResult.group(2));
				roundInfo.setResult(matcherResult.group(2));
			} else
				throw new ParseException("Did not find round result message",
						lineIndex);
			currentLine = sc.nextLine();
			lineIndex++;

			// parse get bonus
			while (true) {
				Matcher matcherBonus = getBonus.matcher(currentLine);
				if (matcherBonus.matches()) {
					System.out.println("Get bonus message found");
					System.out.printf("%s, %s, %s, %s\n\n",
							matcherBonus.group(1), matcherBonus.group(2),
							matcherBonus.group(3), matcherBonus.group(4));

					currentLine = sc.nextLine();
					lineIndex++;
				} else {
					break;
				}
			}

			// parse round end
			Matcher matcherEnd = roundEnd.matcher(currentLine);
			if (matcherEnd.matches()) {
				System.out.println("Round end message found");
				System.out.printf("%s, %s\n\n", matcherEnd.group(1),
						matcherEnd.group(2));
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

}
