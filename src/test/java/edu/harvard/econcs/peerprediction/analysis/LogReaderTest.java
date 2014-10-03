package edu.harvard.econcs.peerprediction.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.andrewmao.math.RandomSelection;
import net.andrewmao.misc.Pair;

import org.junit.Test;

public class LogReaderTest {

	Random rand = new Random();

	@Test
	public void testSelectByDist() {

		int limit = 10000;
		for (double j = 0; j <= 1; j += 0.1) {
			double firstProb = j * 0.1;
			int[] count = new int[] { 0, 0 };
			for (int i = 0; i < limit; i++) {
				int chosen = Utils.selectByBinaryDist(firstProb);
				count[chosen]++;
			}
			double expected = limit * firstProb;
			assertEquals(expected, count[0], 120);
		}
	}

	@Test
	public void testChooseRefPlayer() {

		int currPlayer;
		int chosen;

		int limit = 10000;

		currPlayer = 0;
		chosen = Utils.chooseRefPlayer(currPlayer);
		assertNotSame(currPlayer, chosen);
		assertTrue(chosen == 2 || chosen == 1);

		int countOne = 0;
		for (int i = 0; i < limit; i++) {
			chosen = Utils.chooseRefPlayer(currPlayer);
			if (chosen == 1)
				countOne++;
		}
		assertEquals(limit / 2, countOne, 120);

		currPlayer = 1;
		chosen = Utils.chooseRefPlayer(currPlayer);
		assertNotSame(currPlayer, chosen);
		assertTrue(chosen == 2 || chosen == 0);

		int countZero = 0;
		for (int i = 0; i < limit; i++) {
			chosen = Utils.chooseRefPlayer(currPlayer);
			if (chosen == 0)
				countZero++;
		}
		assertEquals(limit / 2, countZero, 100);

		currPlayer = 2;
		chosen = Utils.chooseRefPlayer(currPlayer);
		assertNotSame(currPlayer, chosen);
		assertTrue(chosen == 1 || chosen == 0);
		
		countOne = 0;
		for (int i = 0; i < limit; i++) {
			chosen = Utils.chooseRefPlayer(currPlayer);
			if (chosen == 0)
				countOne++;
		}
		assertEquals(limit / 2, countOne, 120);
	}

	@Test
	public void testGetExpectedPayoff() {

		LogReader.treatment = "prior2-basic";

		double pay = LogReader.getExpectedPayoff("MM", 2);
		assertEquals(1.5, pay, Utils.eps);

		pay = LogReader.getExpectedPayoff("MM", 0);
		assertEquals(0.1, pay, Utils.eps);

		pay = LogReader.getExpectedPayoff("MM", 1);
		assertEquals(0.8, pay, Utils.eps);

		pay = LogReader.getExpectedPayoff("GB", 2);
		assertEquals(0.3, pay, Utils.eps);

		pay = LogReader.getExpectedPayoff("GB", 0);
		assertEquals(1.2, pay, Utils.eps);

		pay = LogReader.getExpectedPayoff("GB", 1);
		assertEquals(0.75, pay, Utils.eps);
	}

	@Test
	public void testGetNumOfGivenReport() {
	
		int numPlayers = 3;
		String[] playerIds = new String[numPlayers];
		for (int i = 0; i < numPlayers; i++) {
			playerIds[i] = String.format("%d", i);
		}
	
		Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();

		int expectedNumMM = 0;
		int excludeIndex = rand.nextInt(playerIds.length);
		String excludeId = playerIds[excludeIndex];

		for (String id : playerIds) {
			Map<String, Object> r = new HashMap<String, Object>();
			if (rand.nextBoolean() == true) {
				r.put("report", "MM");
				if (!id.equals(excludeId))
					expectedNumMM++;
			} else {
				r.put("report", "GB");
			}
			result.put(id, r);
		}
		int numMM = Utils.getNumOfGivenReport(result, "MM",
				playerIds[excludeIndex]);
		assertEquals(expectedNumMM, numMM);
		assertTrue(numMM < numPlayers);
		
		
		result = new HashMap<String, Map<String, Object>>();
		for (String id : playerIds) {
			Map<String, Object> r = new HashMap<String, Object>();
			r.put("report", "MM");
			result.put(id, r);
		}
		excludeId = "1";
		numMM = Utils.getNumOfGivenReport(result, "MM", excludeId);
		assertEquals(numPlayers - 1, numMM);

		excludeId = "0";
		numMM = Utils.getNumOfGivenReport(result, "MM", excludeId);
		assertEquals(numPlayers - 1, numMM);
		
		excludeId = "2";
		numMM = Utils.getNumOfGivenReport(result, "MM", excludeId);
		assertEquals(numPlayers - 1, numMM);


		result = new HashMap<String, Map<String, Object>>();
		for (String id : playerIds) {
			Map<String, Object> r = new HashMap<String, Object>();
			r.put("report", "GB");
			result.put(id, r);
		}
		excludeId = "0";
		numMM = Utils.getNumOfGivenReport(result, "MM", excludeId);
		assertEquals(0, numMM);

		excludeId = "1";
		numMM = Utils.getNumOfGivenReport(result, "MM", excludeId);
		assertEquals(0, numMM);

		excludeId = "2";
		numMM = Utils.getNumOfGivenReport(result, "MM", excludeId);
		assertEquals(0, numMM);

	}

	@Test
	public void testGetMMProb() {
		double mmProb = Utils.calcMMProb(10, 1, 1);
		assertEquals(0.5, mmProb, Utils.eps);

		mmProb = Utils.calcMMProb(4, 20, 20);
		assertEquals(0.5, mmProb, Utils.eps);
	}


	@Test
	public void testDeterminePayoff() {

		String[] reports = new String[] { "MM", "GB", "GB" };
		int[] refPlayerIndices = new int[reports.length];

		// treatment 1
		LogReader.treatment = "prior2-basic";
		double[] payoffs = LogReader.determinePayoff(reports, refPlayerIndices);

		double[] expectedPayoffs = new double[reports.length];
		for (int i = 0; i < expectedPayoffs.length; i++) {
			expectedPayoffs[i] = Utils.getPayment(LogReader.treatment,
					reports[i], reports[refPlayerIndices[i]]);
		}
		for (int i = 0; i < payoffs.length; i++) {
			assertEquals(expectedPayoffs[i], payoffs[i], Utils.eps);
		}

		// treatment 2
		LogReader.treatment = "prior2-outputagreement";
		payoffs = LogReader.determinePayoff(reports, refPlayerIndices);
		expectedPayoffs = new double[reports.length];
		for (int i = 0; i < expectedPayoffs.length; i++) {
			expectedPayoffs[i] = Utils.getPayment(LogReader.treatment,
					reports[i], reports[refPlayerIndices[i]]);
		}
		for (int i = 0; i < payoffs.length; i++) {
			assertEquals(expectedPayoffs[i], payoffs[i], Utils.eps);
		}

	}

	@Test
	public void testGetStrategyRL() {
	
		LogReader.treatment = "prior2-basic";
		LogReader.parseLog();
	
		String[] playerHitIds = new String[LogReader.expSet.numPlayers];
		for (int j = 0; j < playerHitIds.length; j++) {
			playerHitIds[j] = String.format("%d", j);
		}
		String playerId = "2";
		
		// Test 1
		boolean considerSignal = true;
		double lambda = 5;
		Map<String, Map<Pair<String, String>, Double>> attraction 
			= new HashMap<String, Map<Pair<String, String>, Double>>();
		for (String player : playerHitIds) {
			Map<Pair<String, String>, Double> payoffs = 
					new HashMap<Pair<String, String>, Double>();
			payoffs.put(new Pair<String, String>("MM", "MM"), 0.5);
			payoffs.put(new Pair<String, String>("MM", "GB"), 0.5);
			payoffs.put(new Pair<String, String>("GB", "MM"), 1.0);
			payoffs.put(new Pair<String, String>("GB", "GB"), 0.1);
			attraction.put(player, payoffs);
		}
		String signalCurrRound = "MM";
		String signalPrevRound = "MM";
	
		Map<String, Double> strategy = LogReader.getStrategy(attraction,
				playerId, considerSignal, lambda, signalCurrRound, signalPrevRound);
		double expectedMMProb = 0.5;
		assertEquals(expectedMMProb, strategy.get("MM"), Utils.eps);
	
		// Test 2
		considerSignal = false;
		for (String player : playerHitIds) {
			Map<Pair<String, String>, Double> payoffs = 
					new HashMap<Pair<String, String>, Double>();
			payoffs.put(new Pair<String, String>("MM", "MM"), 0.5);
			payoffs.put(new Pair<String, String>("MM", "GB"), 0.2);
			payoffs.put(new Pair<String, String>("GB", "MM"), 1.0);
			payoffs.put(new Pair<String, String>("GB", "GB"), 0.1);
			attraction.put(player, payoffs);
		}
		strategy = LogReader.getStrategy(attraction, playerId,
				considerSignal, lambda, signalCurrRound,
				signalPrevRound);
		expectedMMProb = Math.pow(Math.E, lambda * 1.5)
				/ (Math.pow(Math.E, lambda * 1.5) + Math.pow(Math.E, lambda * 0.3));
		assertEquals(expectedMMProb, strategy.get("MM"), Utils.eps);
	
	}
	
	@Test
	public void testUpdateAttractionsRL() {
		String[] playerHitIds = new String[]{"0", "1", "2"};
		Map<String, Map<Pair<String, String>, Double>> attractions = 
				LogReader.initAttraction(playerHitIds);
		for (String hitId: playerHitIds) {
			Map<Pair<String, String>, Double> playerAttraction = attractions.get(hitId);
			assertEquals(0.0, playerAttraction.get(new Pair<String, String>("MM", "MM")), Utils.eps);
		}

		for (String player : playerHitIds) {
			Map<Pair<String, String>, Double> payoffs = 
					new HashMap<Pair<String, String>, Double>();
			payoffs.put(new Pair<String, String>("MM", "MM"), 0.5);
			payoffs.put(new Pair<String, String>("MM", "GB"), 0.2);
			payoffs.put(new Pair<String, String>("GB", "MM"), 1.0);
			payoffs.put(new Pair<String, String>("GB", "GB"), 0.1);
			attractions.put(player, payoffs);
		}
		
		double phi; String playerId; 
		String signalPrevRound; String reportPrevRound; double rewardPrevRound;
		
		phi = 0.2;
		signalPrevRound = "MM";
		reportPrevRound = "GB";
		rewardPrevRound = 1.5;
		playerId = "2";
		LogReader.updateAttractionsRL(attractions, playerId, phi, signalPrevRound, 
				reportPrevRound, rewardPrevRound);
		Map<Pair<String, String>, Double> playerAttraction = attractions.get(playerId);
		double actualAttrMMMM = playerAttraction.get(new Pair<String, String>("MM", "MM"));
		double actualAttrMMGB = playerAttraction.get(new Pair<String, String>("MM", "GB"));
		double actualAttrGBMM = playerAttraction.get(new Pair<String, String>("GB", "MM"));
		double actualAttrGBGB = playerAttraction.get(new Pair<String, String>("GB", "GB"));
		double expAttrMMMM = 0.5 * phi;
		double expAttrMMGB = 0.2 * phi + rewardPrevRound;
		double expAttrGBMM = 1.0 * phi;
		double expAttrGBGB = 0.1 * phi + rewardPrevRound;
		assertEquals(expAttrMMMM, actualAttrMMMM, Utils.eps);
		assertEquals(expAttrMMGB, actualAttrMMGB, Utils.eps);
		assertEquals(expAttrGBMM, actualAttrGBMM, Utils.eps);
		assertEquals(expAttrGBGB, actualAttrGBGB, Utils.eps);
		
		rewardPrevRound = 0.1;
		LogReader.updateAttractionsRL(attractions, playerId, phi, signalPrevRound, 
				reportPrevRound, rewardPrevRound);
		playerAttraction = attractions.get(playerId);
		actualAttrMMMM = playerAttraction.get(new Pair<String, String>("MM", "MM"));
		actualAttrMMGB = playerAttraction.get(new Pair<String, String>("MM", "GB"));
		actualAttrGBMM = playerAttraction.get(new Pair<String, String>("GB", "MM"));
		actualAttrGBGB = playerAttraction.get(new Pair<String, String>("GB", "GB"));
		expAttrMMMM = expAttrMMMM * phi;
		expAttrMMGB = expAttrMMGB * phi + rewardPrevRound;
		expAttrGBMM = expAttrGBMM * phi;
		expAttrGBGB = expAttrGBGB * phi + rewardPrevRound;
		assertEquals(expAttrMMMM, actualAttrMMMM, Utils.eps);
		assertEquals(expAttrMMGB, actualAttrMMGB, Utils.eps);
		assertEquals(expAttrGBMM, actualAttrGBMM, Utils.eps);
		assertEquals(expAttrGBGB, actualAttrGBGB, Utils.eps);		
	}
		
	@Test
	public void testEstimateRL() {
		LogReader.treatment = "prior2-uniquetruthful";
		LogReader.parseLog();
		
		int numGames = 300;
	
		// Test 1
		System.out.println();
		System.out.println("Test RL 1");
		boolean expConsiderSignal = true;
		double expPhi = 0.2;
		double expLambda = 5;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("considerSignal", expConsiderSignal);
		params.put("phi", expPhi);
		params.put("lambda", expLambda);
		testRL(numGames, params);
	
		// Test 2
		System.out.println();
		System.out.println("Test RL 2");
		expConsiderSignal = false;
		expPhi = 0.8;
		expLambda = 10;
		params.put("considerSignal", expConsiderSignal);
		params.put("phi", expPhi);
		params.put("lambda", expLambda);
		testRL(numGames, params);
	}

	private void testRL(int numGames, Map<String, Object> params) {
		System.out.printf("Expected parameters: %s\n", params);
	
		List<Game> games = new ArrayList<Game>();
		for (int i = 0; i < numGames; i++) {
			List<Round> rounds = LogReaderTest.simulateOneGameRL(params);
			Game game = new Game();
			game.rounds = rounds;
			game.playerHitIds = new String[LogReader.expSet.numPlayers];
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
				game.playerHitIds[j] = String.format("%d", j);
			}
			games.add(game);
		}
	
		boolean considerSignal = (boolean) params.get("considerSignal");
		Map<String, Object> bounds = new HashMap<String, Object>();
		bounds.put("lb", new double[]{0, 1});
		bounds.put("ub", new double[]{1, 10});
		
		double[] point = null;
		if (considerSignal)
			point = LogReader.estimateUsingApacheOptimizer(games, bounds, "RLS");
		else 
			point = LogReader.estimateUsingApacheOptimizer(games, bounds, "RLNS");
		System.out.printf("Actual parameters: phi=%.2f lambda=%.2f, \n", point[0], point[1]);
		assertEquals((double) params.get("phi"), point[0], 0.1);
		assertEquals((double) params.get("lambda"), point[1], 1);
	}

	static List<Round> simulateOneGameRL(Map<String, Object> params) {
	
		double firstRoundMMProb = 0.5;
		List<Round> rounds = new ArrayList<Round>();
	
		String[] playerHitIds = new String[LogReader.expSet.numPlayers];
		for (int index = 0; index < LogReader.expSet.numPlayers; index++) {
			playerHitIds[index] = String.format("%d", index);
		}
	
		Map<String, Map<Pair<String, String>, Double>> attraction = LogReader.initAttraction(playerHitIds);
	
		for (int i = 0; i < LogReader.expSet.numRounds; i++) {
	
			Round r = new Round();
			r.roundNum = i;
			int worldIndex = Utils.selectByBinaryDist(LogReader.expSet.priorProbs[0]);
			r.chosenWorld = LogReader.expSet.worlds.get(worldIndex);
	
			r.result = new HashMap<String, Map<String, Object>>();
	
			String[] signals = new String[LogReader.expSet.numPlayers];
			String[] reports = new String[LogReader.expSet.numPlayers];
			double[] mmProbs = new double[LogReader.expSet.numPlayers];
	
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
	
				String playerId = String.format("%d", j);
	
				int signalIndex = Utils.selectByBinaryDist(r.chosenWorld
						.get("MM"));
				signals[j] = LogReader.signalList[signalIndex];
	
				if (i == 0) {
	
					int reportIndex = Utils
							.selectByBinaryDist(firstRoundMMProb);
					reports[j] = LogReader.signalList[reportIndex];
	
				} else {
	
					Map<String, Map<String, Object>> resultPrevRound = rounds
							.get(i - 1).result;
					String signalPrevRound = (String) resultPrevRound.get(
							playerId).get("signal");
					String reportPrevRound = (String) resultPrevRound.get(
							playerId).get("report");
					double rewardPrevRound = (double) resultPrevRound.get(
							playerId).get("reward");
	
					boolean considerSignal = (boolean) params
							.get("considerSignal");
					double phi = (double) params.get("phi");
					double lambda = (double) params.get("lambda");
	
					LogReader.updateAttractionsRL(attraction, playerId, phi,
							signalPrevRound, reportPrevRound, rewardPrevRound);
					Map<String, Double> strategy = LogReader.getStrategy(attraction,
							playerId, considerSignal, lambda, signals[j],
							signalPrevRound);
	
					mmProbs[j] = strategy.get("MM");
					int reportIndex = Utils.selectByBinaryDist(strategy
							.get("MM"));
					reports[j] = LogReader.signalList[reportIndex];
	
				}
	
			}
	
			// determine payoffs
			int[] refPlayerIndices = new int[LogReader.expSet.numPlayers];
			double[] payoffs = LogReader.determinePayoff(reports, refPlayerIndices);
	
			// save result
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
	
				Map<String, Object> playerResult = new HashMap<String, Object>();
	
				playerResult.put("signal", signals[j]);
				playerResult.put("report", reports[j]);
	
				if (LogReader.treatment.equals("prior2-basic")
						|| LogReader.treatment.equals("prior2-outputagreement")) {
	
					String refPlayerId = String.format("%d",
							refPlayerIndices[j]);
					playerResult.put("refPlayer", refPlayerId);
	
				}
	
				playerResult.put("reward", payoffs[j]);
				playerResult.put("mmProb", mmProbs[j]);
	
				String playerId = String.format("%d", j);
				r.result.put(playerId, playerResult);
			}
	
			rounds.add(r);
		}
		return rounds;
	}

	@Test
	public void testEstimateSFP() {
		int numGames = 200;
		LogReader.treatment = "prior2-basic";
		LogReader.parseLog();
	
		// test 1
		System.out.println();
		System.out.println("Test SFP 1");
		boolean expConsiderSignal = true;
		double expRho = 0.5;
		double expLambda = 5;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("considerSignal", expConsiderSignal);
		params.put("rho", expRho);
		params.put("lambda", expLambda);
		testSFP(numGames, params);
	
		// test 2
		System.out.println();
		System.out.println("Test SFP 2");
		expConsiderSignal = false;
		expRho = 0.8;
		expLambda = 10;
		params.put("considerSignal", expConsiderSignal);
		params.put("rho", expRho);
		params.put("lambda", expLambda);
		testSFP(numGames, params);
	}

	private void testSFP(int numGames, Map<String, Object> params) {
		System.out.printf("Expected parameters: %s\n", params.toString());
	
		List<Game> games = new ArrayList<Game>();
		for (int i = 0; i < numGames; i++) {
			List<Round> rounds = LogReaderTest.simulateOneGameSFP(params);
			Game game = new Game();
			game.rounds = rounds;
			game.playerHitIds = new String[LogReader.expSet.numPlayers];
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
				game.playerHitIds[j] = String.format("%d", j);
			}
			games.add(game);
		}
	
		boolean considerSignal = (boolean) params.get("considerSignal");
		Map<String, Object> bounds = new HashMap<String, Object>();
		bounds.put("lb", new double[]{0, 1});
		bounds.put("ub", new double[]{1, 10});
		
		double[] point = null;
		if (considerSignal)
			point = LogReader.estimateUsingApacheOptimizer(games, bounds, "SFPS");
		else 
			point = LogReader.estimateUsingApacheOptimizer(games, bounds, "SFPNS");
		System.out.printf("Actual parameters: rho=%.2f lambda=%.2f, \n", point[0], point[1]);
		assertEquals((double) params.get("rho"), point[0], 0.1);
		assertEquals((double) params.get("lambda"), point[1], 0.5);
	}

	

	static List<Round> simulateOneGameSFP(Map<String, Object> params) {
		double firstRoundMMProb = 0.5;
		List<Round> rounds = new ArrayList<Round>();
	
		String[] playerHitIds = new String[LogReader.expSet.numPlayers];
		for (int index = 0; index < LogReader.expSet.numPlayers; index++) {
			playerHitIds[index] = String.format("%d", index);
		}
	
		// initialize experience and attraction
		double experiences = Utils.eps;
		Map<String, Map<Pair<String, String>, Double>> attractions = LogReader.initAttraction(playerHitIds);
	
		for (int i = 0; i < LogReader.expSet.numRounds; i++) {
	
			Round r = new Round();
			r.roundNum = i;
			int worldIndex = Utils.selectByBinaryDist(LogReader.expSet.priorProbs[0]);
			r.chosenWorld = LogReader.expSet.worlds.get(worldIndex);
	
			r.result = new HashMap<String, Map<String, Object>>();
	
			String[] signals = new String[LogReader.expSet.numPlayers];
			String[] reports = new String[LogReader.expSet.numPlayers];
			double[] mmProbs = new double[LogReader.expSet.numPlayers];
	
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
	
				String playerId = String.format("%d", j);
	
				// get signal
				int signalIndex = Utils.selectByBinaryDist(r.chosenWorld
						.get("MM"));
				signals[j] = LogReader.signalList[signalIndex];
	
				// choose report
				if (i == 0) {
	
					// first round, choose reports randomly
					int reportIndex = Utils
							.selectByBinaryDist(firstRoundMMProb);
					reports[j] = LogReader.signalList[reportIndex];
	
				} else {
	
					Map<String, Map<String, Object>> resultPrevRound = rounds
							.get(i - 1).result;
					String signalPrev = (String) resultPrevRound.get(playerId)
							.get("signal");
					String reportPrev = (String) resultPrevRound.get(playerId)
							.get("report");
					double rewardPrev = (double) resultPrevRound.get(playerId)
							.get("reward");
					int numOtherMMReportsPrev = Utils.getNumOfGivenReport(
							resultPrevRound, "MM", playerId);
	
					boolean considerSignal = (boolean) params
							.get("considerSignal");
					double rho = (double) params.get("rho");
					double lambda = (double) params.get("lambda");
	
					// update attractions
					LogReader.updateAttractionsSFP(attractions, experiences, playerId,
							rho, reportPrev, rewardPrev, numOtherMMReportsPrev);
	
					// update experience
					experiences = LogReader.updateExperience(experiences, rho);
	
					// get strategy
					Map<String, Double> strategy = LogReader.getStrategy(attractions,
							playerId, considerSignal, lambda, signals[j],
							signalPrev);
	
					// get report
					mmProbs[j] = strategy.get("MM").doubleValue();
					int reportIndex = Utils.selectByBinaryDist(mmProbs[j]);
					reports[j] = LogReader.signalList[reportIndex];
				}
	
			}
	
			// determine payoffs
			int[] refPlayerIndices = new int[LogReader.expSet.numPlayers];
			double[] payoffs = LogReader.determinePayoff(reports, refPlayerIndices);
	
			// save result
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
	
				Map<String, Object> playerResult = new HashMap<String, Object>();
	
				playerResult.put("signal", signals[j]);
				playerResult.put("report", reports[j]);
	
				if (LogReader.treatment.equals("prior2-basic")
						|| LogReader.treatment.equals("prior2-outputagreement")) {
					String refPlayerId = String.format("%d",
							refPlayerIndices[j]);
					playerResult.put("refPlayer", refPlayerId);
				}
	
				playerResult.put("reward", payoffs[j]);
				playerResult.put("mmProb", mmProbs[j]);
	
				String id = String.format("%d", j);
				r.result.put(id, playerResult);
			}
	
			rounds.add(r);
		}
	
		return rounds;
	}

	@Test
	public void testEstimateEWA() {
		int numGames = 400;
		LogReader.treatment = "prior2-basic";
		LogReader.parseLog();
	
		// test 1
		System.out.println();
		System.out.println("Test EWA 1");
		boolean expConsiderSignal = true;
		double expPhi = 0.3;
		double expDelta = 0.8;
		double expRho = 0.5;
		double expLambda = 5;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("considerSignal", expConsiderSignal);
		params.put("rho", expRho);
		params.put("lambda", expLambda);
		params.put("phi", expPhi);
		params.put("delta", expDelta);
		testEWA(numGames, params);
	
		// test 2
		System.out.println();
		System.out.println("Test EWA 2");
		expConsiderSignal = false;
		expPhi = 0.3;
		expDelta = 0.8;
		expRho = 0.5;
		expLambda = 5;
		params = new HashMap<String, Object>();
		params.put("considerSignal", expConsiderSignal);
		params.put("rho", expRho);
		params.put("lambda", expLambda);
		params.put("phi", expPhi);
		params.put("delta", expDelta);
		testEWA(numGames, params);
	}

	private void testEWA(int numGames, Map<String, Object> params) {
		System.out.printf("Expected parameters: %s\n", params.toString());
		
		List<Game> games = new ArrayList<Game>();
		for (int i = 0; i < numGames; i++) {
			List<Round> rounds = LogReaderTest.simulateOneGameEWA(params);
			Game game = new Game();
			game.rounds = rounds;
			game.playerHitIds = new String[LogReader.expSet.numPlayers];
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
				game.playerHitIds[j] = String.format("%d", j);
			}
			games.add(game);
		}
	

		Map<String, Object> bounds = new HashMap<String, Object>();
		bounds.put("lb", new double[]{0, 0, 0, 1});
		bounds.put("ub", new double[]{1, 1, 1, 10});
		
		boolean considerSignal = (boolean) params.get("considerSignal");
		double[] point = null;
		if (considerSignal) 
			point = LogReader.estimateUsingApacheOptimizer(games, bounds, "EWAS");
		else
			point = LogReader.estimateUsingApacheOptimizer(games, bounds, "EWANS");
		System.out.printf("Actual parameters: rho=%.2f, phi=%.2f, delta=%.2f, lambda=%.2f\n", 
				point[0], point[1], point[2], point[3]);
		assertEquals((double) params.get("rho"), point[0], 0.1);
		assertEquals((double) params.get("phi"), point[1], 0.1);
		assertEquals((double) params.get("delta"), point[2], 0.1);
		assertEquals((double) params.get("lambda"), point[3], 1);		
	}
	
	static List<Round> simulateOneGameEWA(Map<String, Object> params) {
	
		double firstRoundMMProb = 0.5;
		List<Round> rounds = new ArrayList<Round>();
	
		String[] playerHitIds = new String[LogReader.expSet.numPlayers];
		for (int index = 0; index < LogReader.expSet.numPlayers; index++) {
			playerHitIds[index] = String.format("%d", index);
		}
	
		// initialize experience and attraction
		double experiences = Utils.eps;
		Map<String, Map<Pair<String, String>, Double>> attractions = LogReader.initAttraction(playerHitIds);
	
		for (int i = 0; i < LogReader.expSet.numRounds; i++) {
	
			Round r = new Round();
			r.roundNum = i;
			int worldIndex = Utils.selectByBinaryDist(LogReader.expSet.priorProbs[0]);
			r.chosenWorld = LogReader.expSet.worlds.get(worldIndex);
	
			r.result = new HashMap<String, Map<String, Object>>();
	
			String[] signals = new String[LogReader.expSet.numPlayers];
			String[] reports = new String[LogReader.expSet.numPlayers];
			double[] mmProbs = new double[LogReader.expSet.numPlayers];
	
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
	
				String playerId = String.format("%d", j);
	
				// get signal
				int signalIndex = Utils.selectByBinaryDist(r.chosenWorld
						.get("MM"));
				signals[j] = LogReader.signalList[signalIndex];
	
				// choose report
				if (i == 0) {
	
					// first round, choose reports randomly
					int reportIndex = Utils
							.selectByBinaryDist(firstRoundMMProb);
					reports[j] = LogReader.signalList[reportIndex];
	
				} else {
	
					Map<String, Map<String, Object>> resultPrevRound = rounds
							.get(i - 1).result;
					String signalPrev = (String) resultPrevRound.get(playerId)
							.get("signal");
					String reportPrev = (String) resultPrevRound.get(playerId)
							.get("report");
					double rewardPrev = (double) resultPrevRound.get(playerId)
							.get("reward");
					int numMMPrev = Utils.getNumOfGivenReport(resultPrevRound,
							"MM", playerId);
	
					boolean considerSignal = (boolean) params
							.get("considerSignal");
					double rho = (double) params.get("rho");
					double delta = (double) params.get("delta");
					double phi = (double) params.get("phi");
					double lambda = (double) params.get("lambda");
	
					// update attractions
					LogReader.updateAttractionsEWA(attractions, experiences, playerId,
							rho, delta, phi, signalPrev, reportPrev,
							rewardPrev, signals[j], numMMPrev);
	
					// update experience
					experiences = LogReader.updateExperience(experiences, rho);
	
					// get strategy
					Map<String, Double> strategy = LogReader.getStrategy(attractions,
							playerId, considerSignal, lambda, signals[j],
							signalPrev);
	
					// get report
					mmProbs[j] = strategy.get("MM").doubleValue();
					int reportIndex = Utils.selectByBinaryDist(mmProbs[j]);
					reports[j] = LogReader.signalList[reportIndex];
				}
	
			}
	
			// determine payoffs
			int[] refPlayerIndices = new int[LogReader.expSet.numPlayers];
			double[] payoffs = LogReader.determinePayoff(reports, refPlayerIndices);
	
			// save result
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
	
				Map<String, Object> playerResult = new HashMap<String, Object>();
	
				playerResult.put("signal", signals[j]);
				playerResult.put("report", reports[j]);
	
				if (LogReader.treatment.equals("prior2-basic")
						|| LogReader.treatment.equals("prior2-outputagreement")) {
					String refPlayerId = String.format("%d",
							refPlayerIndices[j]);
					playerResult.put("refPlayer", refPlayerId);
				}
	
				playerResult.put("reward", payoffs[j]);
				playerResult.put("mmProb", mmProbs[j]);
	
				String id = String.format("%d", j);
				r.result.put(id, playerResult);
			}
	
			rounds.add(r);
		}
	
		return rounds;
	}

	@Test
	public void testEstimateS2() {
		int numGames = 400;
		LogReader.treatment = "prior2-basic";
		LogReader.parseLog();
	
		// test 1
		System.out.println();
		System.out.println("Test S2 1");
		double expProbTruthful = 0.3;
		double expProbMM = 0.4;
		double expProbGB = 0.1;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("probTruthful", expProbTruthful);
		params.put("probMM", expProbMM);
		params.put("probGB", expProbGB);
		testS2(numGames, params);
	
		// test 2
		System.out.println();
		System.out.println("Test S2 2");
		expProbTruthful = 0.5;
		expProbMM = 0.1;
		expProbGB = 0.3;
		params = new HashMap<String, Object>();
		params.put("probTruthful", expProbTruthful);
		params.put("probMM", expProbMM);
		params.put("probGB", expProbGB);
		testS2(numGames, params);
	}

	private void testS2(int numGames, Map<String, Object> params) {
		System.out.printf("Expected parameters: %s\n", params.toString());
		
		// simulate a set of games
		List<Game> games = new ArrayList<Game>();
		for (int i = 0; i < numGames; i++) {
			Game game = LogReaderTest.simulateOneGameS2(params);
			games.add(game);
		}
		
		double[] point = LogReader.estimateUsingCobyla(games, "S2");
		System.out.printf("Actual parameters: probTruthful=%.2f, probMM=%.2f, probGB=%.2f\n", 
				point[0], point[1], point[2]);
		assertEquals((double) params.get("probTruthful"), point[0], 0.1);
		assertEquals((double) params.get("probMM"), point[1], 0.1);
		assertEquals((double) params.get("probGB"), point[2], 0.1);	
		
	}
	
	static Game simulateOneGameS2(Map<String, Object> params) {
	
		List<Round> rounds = new ArrayList<Round>();
		Game game = new Game();
		
		String[] playerHitIds = new String[LogReader.expSet.numPlayers];
		for (int index = 0; index < LogReader.expSet.numPlayers; index++) {
			playerHitIds[index] = String.format("%d", index);
		}
		game.playerHitIds = playerHitIds;
	
		double[] strategyIndices = new double[LogReader.expSet.numPlayers];
		for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
			
			double[] probDist = new double[4];
			probDist[0] = (double) params.get("probTruthful");
			probDist[1] = (double) params.get("probMM");
			probDist[2] = (double) params.get("probGB");
			probDist[3] = 1 - probDist[0] - probDist[1] - probDist[2];
			strategyIndices[j] = RandomSelection.selectRandomWeighted(probDist, LogReader.rand);	
		}
		
		
		for (int i = 0; i < LogReader.expSet.numRounds; i++) {
	
			Round r = new Round();
			r.roundNum = i;
			int worldIndex = Utils.selectByBinaryDist(LogReader.expSet.priorProbs[0]);
			r.chosenWorld = LogReader.expSet.worlds.get(worldIndex);
	
			r.result = new HashMap<String, Map<String, Object>>();
	
			String[] signals = new String[LogReader.expSet.numPlayers];
			String[] reports = new String[LogReader.expSet.numPlayers];
			double[] mmProbs = new double[LogReader.expSet.numPlayers];
	
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
	
				int signalIndex = Utils.selectByBinaryDist(r.chosenWorld.get("MM"));
				signals[j] = LogReader.signalList[signalIndex];
	
				if (strategyIndices[j] == 0) {
					// truthful strategy
					reports[j] = signals[j];
				} else if (strategyIndices[j] == 1) {
					// MM strategy
					reports[j] = "MM";
				} else if (strategyIndices[j] == 2) {
					// GB strategy
					reports[j] = "GB";
				} else if (strategyIndices[j] == 3) {
					// random strategy
					int reportIndex = Utils.selectByBinaryDist(0.5);
					reports[j] = LogReader.signalList[reportIndex];
				}
				
			}
	
			// determine payoffs
			int[] refPlayerIndices = new int[LogReader.expSet.numPlayers];
			double[] payoffs = LogReader.determinePayoff(reports, refPlayerIndices);
	
			// save result
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
	
				Map<String, Object> playerResult = new HashMap<String, Object>();
	
				playerResult.put("signal", signals[j]);
				playerResult.put("report", reports[j]);
	
				if (LogReader.treatment.equals("prior2-basic")
						|| LogReader.treatment.equals("prior2-outputagreement")) {
	
					String refPlayerId = String.format("%d",
							refPlayerIndices[j]);
					playerResult.put("refPlayer", refPlayerId);
	
				}
	
				playerResult.put("reward", payoffs[j]);
				playerResult.put("mmProb", mmProbs[j]);
	
				String playerId = String.format("%d", j);
				r.result.put(playerId, playerResult);
			}
	
			rounds.add(r);
		}
		game.rounds = rounds;
		return game;
	}
	
	@Test
	public void testEstimateS1() {
		int numGames = 400;
		LogReader.treatment = "prior2-basic";
		LogReader.parseLog();
	
		// test 1
		System.out.println();
		System.out.println("Test S1 1");
		int switchRound = 10;
		double expDiffThreshold = 0.8;
		double expProbTruthful = 0.3;
		double expProbMM = 0.4;
		double expProbGB = 0.1;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("switchRound", switchRound);
		params.put("diffThreshold", expDiffThreshold);
		params.put("probTruthful", expProbTruthful);
		params.put("probMM", expProbMM);
		params.put("probGB", expProbGB);
		testS1(numGames, params);
	
		// test 2
		System.out.println();
		System.out.println("Test S1 2");
		switchRound = 15;
		expDiffThreshold = 0.4;
		expProbTruthful = 0.5;
		expProbMM = 0.1;
		expProbGB = 0.3;
		params = new HashMap<String, Object>();
		params.put("switchRound", switchRound);
		params.put("diffThreshold", expDiffThreshold);
		params.put("probTruthful", expProbTruthful);
		params.put("probMM", expProbMM);
		params.put("probGB", expProbGB);
		testS1(numGames, params);
	}

	private void testS1(int numGames, Map<String, Object> params) {
		System.out.printf("Expected parameters: %s\n", params.toString());
		
		// simulate a set of games
		List<Game> games = new ArrayList<Game>();
		for (int i = 0; i < numGames; i++) {
			Game game = LogReaderTest.simulateOneGameS1(params);
			games.add(game);
		}
		
		int switchRound = (int) params.get("switchRound");
		String modelName = String.format("S1-%d", switchRound);
		
		double[] point = LogReader.estimateUsingCobyla(games, modelName);
		System.out.printf("Actual parameters: diffThreshold=%.2f, probTruthful=%.2f, probMM=%.2f, probGB=%.2f\n",
						point[0], point[1], point[2], point[3]);
		assertEquals((double) params.get("diffThreshold"), 	point[0], 0.1);
		assertEquals((double) params.get("probTruthful"), 	point[1], 0.1);
		assertEquals((double) params.get("probMM"), 		point[2], 0.1);
		assertEquals((double) params.get("probGB"), 		point[3], 0.1);	
	}

	static Game simulateOneGameS1(Map<String, Object> params) {
	
		List<Round> rounds = new ArrayList<Round>();
		Game game = new Game();
		game.rounds = rounds;
	
		String[] playerHitIds = new String[LogReader.expSet.numPlayers];
		for (int index = 0; index < LogReader.expSet.numPlayers; index++) {
			playerHitIds[index] = String.format("%d", index);
		}
		game.playerHitIds = playerHitIds;
	
		double[] probDist = new double[4];
		probDist[0] = (double) params.get("probTruthful");
		probDist[1] = (double) params.get("probMM");
		probDist[2] = (double) params.get("probGB");
		probDist[3] = 1 - probDist[0] - probDist[1] - probDist[2];
		
		double[] strategyIndices = new double[LogReader.expSet.numPlayers];
		for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
			strategyIndices[j] = RandomSelection.selectRandomWeighted(probDist, LogReader.rand);	
		}
		
		int switchRound = (int) params.get("switchRound");
		for (int i = 0; i < switchRound; i++) {
	
			Round r = new Round();
			r.roundNum = i;
			int worldIndex = Utils.selectByBinaryDist(LogReader.expSet.priorProbs[0]);
			r.chosenWorld = LogReader.expSet.worlds.get(worldIndex);
	
			r.result = new HashMap<String, Map<String, Object>>();
	
			String[] signals = new String[LogReader.expSet.numPlayers];
			String[] reports = new String[LogReader.expSet.numPlayers];
			double[] mmProbs = new double[LogReader.expSet.numPlayers];
	
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
	
				int signalIndex = Utils.selectByBinaryDist(r.chosenWorld.get("MM"));
				signals[j] = LogReader.signalList[signalIndex];
	
				if (strategyIndices[j] == 0) {
					// truthful strategy
					reports[j] = signals[j];
				} else if (strategyIndices[j] == 1) {
					// MM strategy
					reports[j] = "MM";
				} else if (strategyIndices[j] == 2) {
					// GB strategy
					reports[j] = "GB";
				} else if (strategyIndices[j] == 3) {
					// random strategy
					int reportIndex = Utils.selectByBinaryDist(0.5);
					reports[j] = LogReader.signalList[reportIndex];
				}
				
			}
	
			// determine payoffs
			int[] refPlayerIndices = new int[LogReader.expSet.numPlayers];
			double[] payoffs = LogReader.determinePayoff(reports, refPlayerIndices);
	
			// save result
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
	
				Map<String, Object> playerResult = new HashMap<String, Object>();
	
				playerResult.put("signal", signals[j]);
				playerResult.put("report", reports[j]);
	
				if (LogReader.treatment.equals("prior2-basic")
						|| LogReader.treatment.equals("prior2-outputagreement")) {
	
					String refPlayerId = String.format("%d",
							refPlayerIndices[j]);
					playerResult.put("refPlayer", refPlayerId);
	
				}
	
				playerResult.put("reward", payoffs[j]);
				playerResult.put("mmProb", mmProbs[j]);
	
				String playerId = String.format("%d", j);
				r.result.put(playerId, playerResult);
			}
	
			rounds.add(r);
		}
		
		double diffThreshold = (double) params.get("diffThreshold");
		for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
			
			String playerId = String.format("%d", j);
			
			double countTotal = switchRound * LogReader.expSet.numPlayers;
			double countMM = Utils.getNumMMReports(0, switchRound - 1, game, playerId);
			double percentMM = countMM / countTotal;
			double percentGB = 1 - percentMM;
			
			if (percentMM - percentGB >= diffThreshold) {
				strategyIndices[j] = 1;
			} else if (percentGB - percentMM >= diffThreshold) {
				strategyIndices[j] = 2;
			}
		}
		
		for (int i = switchRound; i < LogReader.expSet.numRounds; i++) {
	
			Round r = new Round();
			r.roundNum = i;
			int worldIndex = Utils.selectByBinaryDist(LogReader.expSet.priorProbs[0]);
			r.chosenWorld = LogReader.expSet.worlds.get(worldIndex);
	
			r.result = new HashMap<String, Map<String, Object>>();
	
			String[] signals = new String[LogReader.expSet.numPlayers];
			String[] reports = new String[LogReader.expSet.numPlayers];
			double[] mmProbs = new double[LogReader.expSet.numPlayers];
	
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
	
				int signalIndex = Utils.selectByBinaryDist(r.chosenWorld.get("MM"));
				signals[j] = LogReader.signalList[signalIndex];
	
				if (strategyIndices[j] == 0) {
					// truthful strategy
					reports[j] = signals[j];
				} else if (strategyIndices[j] == 1) {
					// MM strategy
					reports[j] = "MM";
				} else if (strategyIndices[j] == 2) {
					// GB strategy
					reports[j] = "GB";
				} else if (strategyIndices[j] == 3) {
					// random strategy
					int reportIndex = Utils.selectByBinaryDist(0.5);
					reports[j] = LogReader.signalList[reportIndex];
				}
				
			}
	
			// determine payoffs
			int[] refPlayerIndices = new int[LogReader.expSet.numPlayers];
			double[] payoffs = LogReader.determinePayoff(reports, refPlayerIndices);
	
			// save result
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
	
				Map<String, Object> playerResult = new HashMap<String, Object>();
	
				playerResult.put("signal", signals[j]);
				playerResult.put("report", reports[j]);
	
				if (LogReader.treatment.equals("prior2-basic")
						|| LogReader.treatment.equals("prior2-outputagreement")) {
	
					String refPlayerId = String.format("%d",
							refPlayerIndices[j]);
					playerResult.put("refPlayer", refPlayerId);
	
				}
	
				playerResult.put("reward", payoffs[j]);
				playerResult.put("mmProb", mmProbs[j]);
	
				String playerId = String.format("%d", j);
				r.result.put(playerId, playerResult);
			}
	
			rounds.add(r);
		}
		
		game.rounds = rounds;
		return game;
	}

	@Test
	public void testGetLkTruthful() {
		LogReader.treatment = "prior2-basic";
		LogReader.parseLog();
		
		Game g = simulateGameWithPureStrategy("Truthful");
		int roundStart = 4;
		int roundEnd = 7;
		double lk = LogReader.getLkTruthful(roundStart, roundEnd, g, "0");
		assertEquals(Math.pow(1 - Utils.eps, roundEnd - roundStart), lk, Utils.eps);
	}
	
	@Test
	public void testGetLkReport() {
		LogReader.treatment = "prior2-basic";
		LogReader.parseLog();
		
		Game g = simulateGameWithPureStrategy("MM");
		int roundStart = 4;
		int roundEnd = 7;
		double lk = LogReader.getLkReport(roundStart, roundEnd, g, "0", "MM");
		assertEquals(Math.pow(1 - Utils.eps, roundEnd - roundStart), lk, Utils.eps);
		
		lk = LogReader.getLkReport(roundStart, roundEnd, g, "0", "GB");
		assertEquals(Math.pow(Utils.eps, roundEnd - roundStart), lk, Utils.eps);
	}

	static Game simulateGameWithPureStrategy(String strategy) {
			List<Round> rounds = new ArrayList<Round>();
			Game game = new Game();
			
			String[] playerHitIds = new String[LogReader.expSet.numPlayers];
			for (int index = 0; index < LogReader.expSet.numPlayers; index++) {
				playerHitIds[index] = String.format("%d", index);
			}
			game.playerHitIds = playerHitIds;
			
			for (int i = 0; i < LogReader.expSet.numRounds; i++) {
		
				Round r = new Round();
				r.roundNum = i;
				int worldIndex = Utils.selectByBinaryDist(LogReader.expSet.priorProbs[0]);
				r.chosenWorld = LogReader.expSet.worlds.get(worldIndex);
		
				r.result = new HashMap<String, Map<String, Object>>();
		
				String[] signals = new String[LogReader.expSet.numPlayers];
				String[] reports = new String[LogReader.expSet.numPlayers];
	//			double[] mmProbs = new double[LogReader.expSet.numPlayers];
		
				for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
		
					int signalIndex = Utils.selectByBinaryDist(r.chosenWorld.get("MM"));
					signals[j] = LogReader.signalList[signalIndex];
					
					if (strategy.equals("Truthful"))
						reports[j] = signals[j];
					else if (strategy.equals("MM"))
						reports[j] = "MM";
					else if (strategy.equals("GB"))
						reports[j] = "GB";
					
				}
		
				// determine payoffs
	//			int[] refPlayerIndices = new int[LogReader.expSet.numPlayers];
	//			double[] payoffs = LogReader.determinePayoff(reports, refPlayerIndices);
		
				// save result
				for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
		
					Map<String, Object> playerResult = new HashMap<String, Object>();
		
					playerResult.put("signal", signals[j]);
					playerResult.put("report", reports[j]);
		
	//				if (LogReader.treatment.equals("prior2-basic")
	//						|| LogReader.treatment.equals("prior2-outputagreement")) {
	//	
	//					String refPlayerId = String.format("%d",
	//							refPlayerIndices[j]);
	//					playerResult.put("refPlayer", refPlayerId);
	//	
	//				}
		
	//				playerResult.put("reward", payoffs[j]);
	//				playerResult.put("mmProb", mmProbs[j]);
		
					String playerId = String.format("%d", j);
					r.result.put(playerId, playerResult);
				}
		
				rounds.add(r);
			}
			game.rounds = rounds;
			return game;
		}
}
