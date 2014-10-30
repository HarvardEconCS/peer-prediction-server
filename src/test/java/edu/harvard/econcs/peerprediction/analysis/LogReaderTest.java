package edu.harvard.econcs.peerprediction.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.andrewmao.math.RandomSelection;
import net.andrewmao.misc.Pair;

import org.junit.Test;

public class LogReaderTest {

	Random rand = new Random();
	static String[] signalList = new String[] { "MM", "GB" };

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

		double pay = LearningModelsExisting.getExpectedPayoff("MM", 2);
		assertEquals(1.5, pay, Utils.eps);

		pay = LearningModelsExisting.getExpectedPayoff("MM", 0);
		assertEquals(0.1, pay, Utils.eps);

		pay = LearningModelsExisting.getExpectedPayoff("MM", 1);
		assertEquals(0.8, pay, Utils.eps);

		pay = LearningModelsExisting.getExpectedPayoff("GB", 2);
		assertEquals(0.3, pay, Utils.eps);

		pay = LearningModelsExisting.getExpectedPayoff("GB", 0);
		assertEquals(1.2, pay, Utils.eps);

		pay = LearningModelsExisting.getExpectedPayoff("GB", 1);
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

		// treatment 1
		LogReader.treatment = "prior2-basic";
		int[] refPlayerIndices = new int[reports.length];
		double[] payoffs = new double[reports.length];
		determinePayoff(reports, refPlayerIndices, payoffs);

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
		payoffs = LogReaderTest.determinePayoff(reports, refPlayerIndices, null);
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
		LogReader.parseDB();
	
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
	
		Map<String, Double> strategy = LearningModelsExisting.getStrategy(attraction,
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
		strategy = LearningModelsExisting.getStrategy(attraction, playerId,
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
				LearningModelsExisting.initAttraction(playerHitIds);
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
		LearningModelsExisting.updateAttractionsRL(attractions, playerId, phi, signalPrevRound, 
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
		LearningModelsExisting.updateAttractionsRL(attractions, playerId, phi, signalPrevRound, 
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
	public void testS1() {
		int numGames = 400;
		LogReader.treatment = "prior2-basic";
		LogReader.parseDB();
		
		int numTests = 3;
		int numStrategies = 5;
		String model = "s1";

		for (int i = 0; i < numTests; i++) {

			System.out.println();
			System.out.printf("Test %s (%d)\n", model, i);
			
			double[] vec = Utils.getRandomVec(numStrategies);
			double expProbTruthful = vec[0];
			double expProbMM = vec[1];
			double expProbGB = vec[2];
			double expProbOP = vec[3];
			double expEps = 0.1 * rand.nextDouble();
			double[] point = new double[]{expProbTruthful, expProbMM, expProbGB, expProbOP, expEps};
			
			Map<String, Object> params = LearningModelsCustom.pointToMap(model, point);
			
			testS1Helper(numGames, params, model);
		}

	}

	@Test
	public void testS3Abs() {
		int numGames = 300;
		LogReader.treatment = "prior2-basic";
		LogReader.parseDB();
		
		int numTests = 3;
		int numStrategies = 5;
		String model = "s3-abs";
				
		for (int i = 0; i < numTests; i++) {
			
			System.out.println();
			System.out.printf("Testing %s (%d)\n", model, i);

			double[] vec = Utils.getRandomVec(numStrategies);
			double expProbTR = vec[0];
			double expProbMM = vec[1];
			double expProbGB = vec[2];
			double expProbOP = vec[3];
			double expEps = 0.1 * rand.nextDouble();
//			double expDelta = rand.nextDouble()	* LearningModels.getUBCobyla(model, "delta");
			double expDelta = 5;
			double[] point = new double[] { expProbTR, expProbMM, expProbGB, expProbOP, expEps, expDelta };

			Map<String, Object> params = LearningModelsCustom.pointToMap(model, point);

			testS3Helper(numGames, params, model);
		}
		
	}

	@Test
	public void testS3Rel() {
		int numGames = 300;
		LogReader.treatment = "prior2-basic";
		LogReader.parseDB();
		
		int numTests = 3;
		int numStrategies = 5;
		String model = "s3-rel";
		
		for (int i = 0; i < numTests; i++) {
			
			System.out.println();
			System.out.printf("Model %s - test %d\n", model, i);
	
			double[] vec = Utils.getRandomVec(numStrategies);
			double expProbTR = vec[0];
			double expProbMM = vec[1];
			double expProbGB = vec[2];
			double expProbOP = vec[3];
			double expEps = 0.1 * rand.nextDouble();
			double expDelta = 5;
			double[] point = new double[] { expProbTR, expProbMM, expProbGB, expProbOP, expEps, expDelta };
	
			Map<String, Object> params = LearningModelsCustom.pointToMap(model, point);
			
			testS3Helper(numGames, params, model);
		}
		
	}


	@Test
	public void testRL() {
		int numGames = 300;
		LogReader.treatment = "prior2-uniquetruthful";
		LogReader.parseDB();

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
		testRLHelper(numGames, params);
	
		// Test 2
		System.out.println();
		System.out.println("Test RL 2");
		expConsiderSignal = false;
		expPhi = 0.8;
		expLambda = 10;
		params.put("considerSignal", expConsiderSignal);
		params.put("phi", expPhi);
		params.put("lambda", expLambda);
		testRLHelper(numGames, params);
	}

	@Test
	public void testSFP() {
		int numGames = 200;
		LogReader.treatment = "prior2-basic";
		LogReader.parseDB();
	
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
		testSFPHelper(numGames, params);
	
		// test 2
		System.out.println();
		System.out.println("Test SFP 2");
		expConsiderSignal = false;
		expRho = 0.8;
		expLambda = 10;
		params.put("considerSignal", expConsiderSignal);
		params.put("rho", expRho);
		params.put("lambda", expLambda);
		testSFPHelper(numGames, params);
	}

	@Test
	public void testEWA() {
		int numGames = 400;
		LogReader.treatment = "prior2-basic";
		LogReader.parseDB();
	
		// test 1
		System.out.println();
		System.out.println("Test EWA 1");
		boolean expConsiderSignal = true;
		double expPhi = rand.nextDouble();
		double expDelta = 0.8;
		double expRho = 0.5;
		double expLambda = 5;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("considerSignal", expConsiderSignal);
		params.put("rho", expRho);
		params.put("lambda", expLambda);
		params.put("phi", expPhi);
		params.put("delta", expDelta);
		testEWAHelper(numGames, params);
	
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
		testEWAHelper(numGames, params);
	}

	void testS3Helper(int numGames, Map<String, Object> params, String model) {
		
		// expected parameters
		System.out.printf("Expected parameters: ");
		Utils.printParams(params);
		
		// simulate a set of games
		List<Game> games = new ArrayList<Game>();
		for (int i = 0; i < numGames; i++) {
			Game game = LogReaderTest.simulateOneGameS3(params);
			games.add(game);
		}
		
		// expected loglk
		double loglkExp = LearningModelsCustom.computeLogLkS3(params, games);
		System.out.printf("Expected loglk = %.2f\n", loglkExp);
		
		// estimate model
		double[] point = LearningModelsCustom.setRandomStartPoint(model);
		point[5] = (double) params.get("delta");
		point = LearningModelsCustom.estimateUsingCobyla(model, games);

		System.out.printf("Actual parameters: ");
		Utils.printParams(LearningModelsCustom.pointToMap(model, point));
				
		assertEquals((double) params.get("probTR"), point[0], 0.1);
		assertEquals((double) params.get("probMM"), point[1], 0.1);
		assertEquals((double) params.get("probGB"), point[2], 0.1);	
		assertEquals((double) params.get("probOP"), point[3], 0.1);	
		assertEquals((double) params.get("eps"), 	point[4], 0.1);
		assertEquals((double) params.get("delta"), 	point[5], 0.1);
	}

	void testS1Helper(int numGames, Map<String, Object> expectedParams, String model) {
		// expected parameters
		System.out.printf("Expected parameters: ");
		Utils.printParams(expectedParams);
		
		// simulate a set of games
		List<Game> games = new ArrayList<Game>();
		for (int i = 0; i < numGames; i++) {
			Game game = simulateOneGameS1(expectedParams);
			games.add(game);
		}
		
		// estimate model
		double[] point = LearningModelsCustom.estimateUsingCobyla(model, games);
		Map<String, Object> actualParams = LearningModelsCustom.pointToMap(model, point);
		System.out.printf("Actual parameters: ");
		Utils.printParams(actualParams);
		
		// compare parameters
		assertEquals((double) expectedParams.get("probTR"), (double) actualParams.get("probTR"), 0.1);
		assertEquals((double) expectedParams.get("probMM"), (double) actualParams.get("probMM"), 0.1);
		assertEquals((double) expectedParams.get("probGB"), (double) actualParams.get("probGB"), 0.1);
		assertEquals((double) expectedParams.get("probOP"), (double) actualParams.get("probOP"), 0.1);
		assertEquals((double) expectedParams.get("eps"), 	(double) actualParams.get("eps"), 0.05);
	}

	void testRLHelper(int numGames, Map<String, Object> params) {
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
			point = LearningModelsExisting.estimateUsingApacheOptimizer(games, "RLS");
		else 
			point = LearningModelsExisting.estimateUsingApacheOptimizer(games, "RLNS");
		System.out.printf("Actual parameters: phi=%.2f lambda=%.2f, \n", point[0], point[1]);
		assertEquals((double) params.get("phi"), point[0], 0.1);
		assertEquals((double) params.get("lambda"), point[1], 1);
	}

	void testSFPHelper(int numGames, Map<String, Object> params) {
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
			point = LearningModelsExisting.estimateUsingApacheOptimizer(games, "SFPS");
		else 
			point = LearningModelsExisting.estimateUsingApacheOptimizer(games, "SFPNS");
		System.out.printf("Actual parameters: rho=%.2f lambda=%.2f, \n", point[0], point[1]);
		assertEquals((double) params.get("rho"), point[0], 0.1);
		assertEquals((double) params.get("lambda"), point[1], 0.5);
	}

	void testEWAHelper(int numGames, Map<String, Object> params) {
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
			point = LearningModelsExisting.estimateUsingApacheOptimizer(games, "EWAS");
		else
			point = LearningModelsExisting.estimateUsingApacheOptimizer(games, "EWANS");
		System.out.printf("Actual parameters: rho=%.2f, phi=%.2f, delta=%.2f, lambda=%.2f\n", 
				point[0], point[1], point[2], point[3]);
		assertEquals((double) params.get("rho"), point[0], 0.1);
		assertEquals((double) params.get("phi"), point[1], 0.1);
		assertEquals((double) params.get("delta"), point[2], 0.1);
		assertEquals((double) params.get("lambda"), point[3], 1);		
	}

	Game simulateOneGameS1(Map<String, Object> params) {
	
		int numStrategies = 5;
		List<Round> rounds = new ArrayList<Round>();
		Game game = new Game();
		
		String[] playerHitIds = new String[LogReader.expSet.numPlayers];
		for (int index = 0; index < LogReader.expSet.numPlayers; index++) {
			playerHitIds[index] = String.format("%d", index);
		}
		game.playerHitIds = playerHitIds;
	
		int[] strategyIndices = new int[LogReader.expSet.numPlayers];
		for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
			
			double[] probDist = new double[numStrategies];
			probDist[0] = (double) params.get("probTR");
			probDist[1] = (double) params.get("probMM");
			probDist[2] = (double) params.get("probGB");
			probDist[3] = (double) params.get("probOP");
			probDist[4] = 1 - probDist[0] - probDist[1] - probDist[2] - probDist[3];
			strategyIndices[j] = RandomSelection.selectRandomWeighted(
					probDist, Utils.rand);	
		}
		
		double eps = (double) params.get("eps");
		for (int i = 0; i < LogReader.expSet.numRounds; i++) {
	
			Round round = new Round();
			round.roundNum = i;
			int worldIndex = Utils.selectByBinaryDist(LogReader.expSet.priorProbs.get(0));
			round.chosenWorld = LogReader.expSet.worlds.get(worldIndex);
	
			round.result = new HashMap<String, Map<String, Object>>();
	
			String[] signals = new String[LogReader.expSet.numPlayers];
			String[] reports = new String[LogReader.expSet.numPlayers];
	
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
	
				getSignal(round, signals, j);
				chooseReport(strategyIndices, signals, reports, eps, j);
			}

			// determine payoffs
			int[] refPlayerIndices = new int[LogReader.expSet.numPlayers];
			double[] payoffs = new double[LogReader.expSet.numPlayers];
			determinePayoff(reports, refPlayerIndices, payoffs);
			saveResult(round, signals, reports, refPlayerIndices, payoffs);
	
			rounds.add(round);
		}
		game.rounds = rounds;
		return game;
	}

	static Game simulateOneGameS3(Map<String, Object> params) {
	
		boolean isAbs = (boolean) params.get("isAbs");
		double eps = (double) params.get("eps");
		double delta = (double) params.get("delta");
		int numStrategies = 5;
		
		List<Round> rounds = new ArrayList<Round>();
		Game game = new Game();
		game.rounds = rounds;
	
		String[] playerHitIds = new String[LogReader.expSet.numPlayers];
		for (int index = 0; index < LogReader.expSet.numPlayers; index++) {
			playerHitIds[index] = String.format("%d", index);
		}
		game.playerHitIds = playerHitIds;
	
		// initial probabilities for strategies
		double[] probDist = new double[numStrategies];
		probDist[0] = (double) params.get("probTR");
		probDist[1] = (double) params.get("probMM");
		probDist[2] = (double) params.get("probGB");
		probDist[3] = (double) params.get("probOP");
		probDist[4] = 1 - probDist[0] - probDist[1] - probDist[2] - probDist[3];
		
		// select initial strategies
		int[] strategyIndices = new int[LogReader.expSet.numPlayers];
		for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
			strategyIndices[j] = RandomSelection.selectRandomWeighted(probDist, Utils.rand);	
		}
		
		// initialize actual and hypothetical payoffs
		double[] actualPayoffs = new double[LogReader.expSet.numPlayers];
		Map<String, List<Double>> hypoPayoffMap = new HashMap<String, List<Double>>();
		for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
			String playerId = String.format("%d", j);
			actualPayoffs[j] = 0.0;
			List<Double> list = new ArrayList<Double>();
			for (int x = 0; x < numStrategies; x++) {
				list.add(0.0);
			}
			hypoPayoffMap.put(playerId, list);
		}
	
		for (int i = 0; i < LogReader.expSet.numRounds; i++) {
	
			Round r = new Round();
			r.roundNum = i;
			int worldIndex = Utils.selectByBinaryDist(LogReader.expSet.priorProbs.get(0));
			r.chosenWorld = LogReader.expSet.worlds.get(worldIndex);
			r.result = new HashMap<String, Map<String, Object>>();
	
			// generate signals, reports, payoffs, and save them
			String[] signals = new String[LogReader.expSet.numPlayers];
			String[] reports = new String[LogReader.expSet.numPlayers];

	
			boolean[] switched = new boolean[LogReader.expSet.numPlayers];
			// switch strategy according to criteria
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
				
				if (switched[j]) continue;
				
				String playerId = String.format("%d", j);
				List<Double> hypoPayoffs = hypoPayoffMap.get(playerId);
				Double bestAltPayoff = Collections.max(hypoPayoffs);
				
				if ((isAbs && LearningModelsCustom.shouldSwitchAbsS3(bestAltPayoff, actualPayoffs[j], delta)) || 
						(!isAbs && LearningModelsCustom.shouldSwitchRelS3(bestAltPayoff, actualPayoffs[j], delta))) {
					
					strategyIndices[j] = hypoPayoffs.indexOf(bestAltPayoff);
					switched[j] = true;
				}
			}
			
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
				getSignal(r, signals, j);
				chooseReport(strategyIndices, signals, reports, eps, j);
			}
			
			// determine payoffs
			int[] refPlayerIndices = new int[LogReader.expSet.numPlayers];
			double[] payoffs = new double[LogReader.expSet.numPlayers];
			determinePayoff(reports, refPlayerIndices, payoffs);
			saveResult(r, signals, reports, refPlayerIndices, payoffs);
	
			// update actual and hypothetical payoffs
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
				String playerId = String.format("%d", j);
	
				actualPayoffs[j] = actualPayoffs[j] + payoffs[j];
				List<Double> hypoPayoffs = hypoPayoffMap.get(playerId);
				LearningModelsCustom.updateHypoPayoffsS3(hypoPayoffs, playerId,
						signals[j], r, LogReader.treatment);
	
			}
			
			rounds.add(r);
		}
		
		game.rounds = rounds;
		return game;
	}

	static List<Round> simulateOneGameRL(Map<String, Object> params) {
	
		double firstRoundMMProb = 0.5;
		List<Round> rounds = new ArrayList<Round>();
	
		String[] playerHitIds = new String[LogReader.expSet.numPlayers];
		for (int index = 0; index < LogReader.expSet.numPlayers; index++) {
			playerHitIds[index] = String.format("%d", index);
		}
	
		Map<String, Map<Pair<String, String>, Double>> attraction = 
				LearningModelsExisting.initAttraction(playerHitIds);
	
		for (int i = 0; i < LogReader.expSet.numRounds; i++) {
	
			Round r = new Round();
			r.roundNum = i;
			int worldIndex = Utils.selectByBinaryDist(LogReader.expSet.priorProbs.get(0));
			r.chosenWorld = LogReader.expSet.worlds.get(worldIndex);
	
			r.result = new HashMap<String, Map<String, Object>>();
	
			String[] signals = new String[LogReader.expSet.numPlayers];
			String[] reports = new String[LogReader.expSet.numPlayers];
			double[] mmProbs = new double[LogReader.expSet.numPlayers];
	
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
	
				String playerId = String.format("%d", j);
	
				getSignal(r, signals, j);
	
				if (i == 0) {
	
					int reportIndex = Utils
							.selectByBinaryDist(firstRoundMMProb);
					reports[j] = LogReaderTest.signalList[reportIndex];
	
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
	
					LearningModelsExisting.updateAttractionsRL(attraction, playerId, phi,
							signalPrevRound, reportPrevRound, rewardPrevRound);
					Map<String, Double> strategy = LearningModelsExisting.getStrategy(attraction,
							playerId, considerSignal, lambda, signals[j],
							signalPrevRound);
	
					mmProbs[j] = strategy.get("MM");
					int reportIndex = Utils.selectByBinaryDist(strategy
							.get("MM"));
					reports[j] = LogReaderTest.signalList[reportIndex];
	
				}
	
			}
	
			// determine payoffs
			int[] refPlayerIndices = new int[LogReader.expSet.numPlayers];
			double[] payoffs = new double[LogReader.expSet.numPlayers];
			determinePayoff(reports, refPlayerIndices, payoffs);
			saveResult(r, signals, reports, refPlayerIndices, payoffs);
	
			rounds.add(r);
		}
		return rounds;
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
		Map<String, Map<Pair<String, String>, Double>> attractions = LearningModelsExisting.initAttraction(playerHitIds);
	
		for (int i = 0; i < LogReader.expSet.numRounds; i++) {
	
			Round r = new Round();
			r.roundNum = i;
			int worldIndex = Utils.selectByBinaryDist(LogReader.expSet.priorProbs.get(0));
			r.chosenWorld = LogReader.expSet.worlds.get(worldIndex);
	
			r.result = new HashMap<String, Map<String, Object>>();
	
			String[] signals = new String[LogReader.expSet.numPlayers];
			String[] reports = new String[LogReader.expSet.numPlayers];
			double[] mmProbs = new double[LogReader.expSet.numPlayers];
	
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
	
				String playerId = String.format("%d", j);
	
				getSignal(r, signals, j);
	
				// choose report
				if (i == 0) {
	
					// first round, choose reports randomly
					int reportIndex = Utils
							.selectByBinaryDist(firstRoundMMProb);
					reports[j] = LogReaderTest.signalList[reportIndex];
	
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
					LearningModelsExisting.updateAttractionsSFP(attractions, experiences, playerId,
							rho, reportPrev, rewardPrev, numOtherMMReportsPrev);
	
					// update experience
					experiences = LearningModelsExisting.updateExperience(experiences, rho);
	
					// get strategy
					Map<String, Double> strategy = LearningModelsExisting.getStrategy(attractions,
							playerId, considerSignal, lambda, signals[j],
							signalPrev);
	
					// get report
					mmProbs[j] = strategy.get("MM").doubleValue();
					int reportIndex = Utils.selectByBinaryDist(mmProbs[j]);
					reports[j] = LogReaderTest.signalList[reportIndex];
				}
	
			}
	
			// determine payoffs
			int[] refPlayerIndices = new int[LogReader.expSet.numPlayers];
			double[] payoffs = new double[LogReader.expSet.numPlayers];
			determinePayoff(reports, refPlayerIndices, payoffs);
			saveResult(r, signals, reports, refPlayerIndices, payoffs);
	
			rounds.add(r);
		}
	
		return rounds;
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
		Map<String, Map<Pair<String, String>, Double>> attractions = LearningModelsExisting.initAttraction(playerHitIds);
	
		for (int i = 0; i < LogReader.expSet.numRounds; i++) {
	
			Round r = new Round();
			r.roundNum = i;
			int worldIndex = Utils.selectByBinaryDist(LogReader.expSet.priorProbs.get(0));
			r.chosenWorld = LogReader.expSet.worlds.get(worldIndex);
	
			r.result = new HashMap<String, Map<String, Object>>();
	
			String[] signals = new String[LogReader.expSet.numPlayers];
			String[] reports = new String[LogReader.expSet.numPlayers];
			double[] mmProbs = new double[LogReader.expSet.numPlayers];
	
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
	
				String playerId = String.format("%d", j);
	
				getSignal(r, signals, j);
	
				// choose report
				if (i == 0) {
	
					// first round, choose reports randomly
					int reportIndex = Utils
							.selectByBinaryDist(firstRoundMMProb);
					reports[j] = LogReaderTest.signalList[reportIndex];
	
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
					LearningModelsExisting.updateAttractionsEWA(attractions, experiences, playerId,
							rho, delta, phi, signalPrev, reportPrev,
							rewardPrev, signals[j], numMMPrev);
	
					// update experience
					experiences = LearningModelsExisting.updateExperience(experiences, rho);
	
					// get strategy
					Map<String, Double> strategy = LearningModelsExisting.getStrategy(attractions,
							playerId, considerSignal, lambda, signals[j],
							signalPrev);
	
					// get report
					mmProbs[j] = strategy.get("MM").doubleValue();
					int reportIndex = Utils.selectByBinaryDist(mmProbs[j]);
					reports[j] = LogReaderTest.signalList[reportIndex];
				}
	
			}
	
			// determine payoffs
			int[] refPlayerIndices = new int[LogReader.expSet.numPlayers];
			double[] payoffs = new double[LogReader.expSet.numPlayers];
			determinePayoff(reports, refPlayerIndices, payoffs);
			saveResult(r, signals, reports, refPlayerIndices, payoffs);
	
			rounds.add(r);
		}
	
		return rounds;
	}

	static void getSignal(Round r, String[] signals, int j) {
		int signalIndex = Utils.selectByBinaryDist(r.chosenWorld.get("MM"));
		signals[j] = LogReaderTest.signalList[signalIndex];
	}

	static void chooseReport(int[] strategyIndices, String[] signals,
			String[] reports, double eps, int j) {
		
		if (strategyIndices[j] == 0) {
			// truthful strategy
			int index = Utils.selectByBinaryDist(1 - eps);
			if (index == 0)
				reports[j] = signals[j];
			else
				reports[j] = Utils.getOtherReport(signals[j]);
			
		} else if (strategyIndices[j] == 1) {
			// MM strategy
			int index = Utils.selectByBinaryDist(1 - eps);
			if (index == 0)
				reports[j] = "MM";
			else
				reports[j] = "GB";
			
		} else if (strategyIndices[j] == 2) {
			// GB strategy
			int index = Utils.selectByBinaryDist(1 - eps);
			if (index == 0)
				reports[j] = "GB";
			else
				reports[j] = "MM";
			
		} else if (strategyIndices[j] == 3) {
			// Opposite strategy
			int index = Utils.selectByBinaryDist(1 - eps);
			if (index == 0)
				reports[j] = Utils.getOtherReport(signals[j]);
			else
				reports[j] = signals[j];
			
		} else if (strategyIndices[j] == 4) {
			// random strategy
			int reportIndex = Utils.selectByBinaryDist(0.5);
			reports[j] = LogReaderTest.signalList[reportIndex];
		}
		
	}

	static double[] determinePayoff(String[] reports,
			int[] refPlayerIndices, double[] payoffs) {
	
		if (LogReader.treatment.equals("prior2-basic")
				|| LogReader.treatment.equals("prior2-outputagreement")) {
	
			for (int j = 0; j < reports.length; j++) {
	
				String myReport = reports[j];
				refPlayerIndices[j] = Utils.chooseRefPlayer(j);
				String refReport = reports[refPlayerIndices[j]];
				payoffs[j] = Utils.getPayment(LogReader.treatment, myReport, refReport);
			}
	
		} else if (LogReader.treatment.equals("prior2-uniquetruthful")
				|| LogReader.treatment.equals("prior2-symmlowpay")) {
	
			int totalNumMM = 0;
			for (int j = 0; j < reports.length; j++) {
				if (reports[j].equals("MM"))
					totalNumMM++;
			}
	
			for (int j = 0; j < reports.length; j++) {
				String myReport = reports[j];
				int numOtherMMReports = totalNumMM;
				if (myReport.equals("MM"))
					numOtherMMReports = totalNumMM - 1;
				payoffs[j] = Utils.getPayment(LogReader.treatment, myReport,
						numOtherMMReports);
			}
	
		}
	
		return payoffs;
	}

	static void saveResult(Round r, String[] signals, String[] reports,
			int[] refPlayerIndices, double[] payoffs) {
		for (int j = 0; j < LogReader.expSet.numPlayers; j++) {

			Map<String, Object> playerResult = new HashMap<String, Object>();

			playerResult.put("signal", signals[j]);
			playerResult.put("report", reports[j]);

			if (LogReader.treatment.equals("prior2-basic")
					|| LogReader.treatment.equals("prior2-outputagreement")) {

				String refPlayerId = String.format("%d", refPlayerIndices[j]);
				playerResult.put("refPlayer", refPlayerId);

			}

			playerResult.put("reward", payoffs[j]);

			String playerId = String.format("%d", j);
			r.result.put(playerId, playerResult);
		}
	}

	@Test
	public void testGetLkStrategy() {
		LogReader.treatment = "prior2-basic";
		LogReader.parseDB();
		
		testGetLkStrategyHelper("TR");
		
		testGetLkStrategyHelper("MM");
		
		testGetLkStrategyHelper("GB");

	}

	private void testGetLkStrategyHelper(String strategy) {
		Game g = simulateGameWithPureStrategy(strategy);
		int roundStart = 4;
		int roundEnd = 7;
		double strError = Utils.eps;
		String playerId = "0";
		double lk = LearningModelsCustom.helperGetLkStrategy(g, playerId, roundStart, roundEnd, strategy, strError, null);
		assertEquals(Math.pow(1 - Utils.eps, roundEnd - roundStart), lk, Utils.eps);
		
		double lkRandom = LearningModelsCustom.helperGetLkStrategy(g, playerId, roundStart, roundEnd, "RA", strError, null);
		assertEquals(Math.pow(0.5, roundEnd - roundStart), lkRandom, Utils.eps);
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
				int worldIndex = Utils.selectByBinaryDist(LogReader.expSet.priorProbs.get(0));
				r.chosenWorld = LogReader.expSet.worlds.get(worldIndex);
		
				r.result = new HashMap<String, Map<String, Object>>();
		
				String[] signals = new String[LogReader.expSet.numPlayers];
				String[] reports = new String[LogReader.expSet.numPlayers];
		
				for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
		
					getSignal(r, signals, j);
					
					if (strategy.equals("TR"))
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
	
	@Test
	public void testStrategyIndexToString() {
		assertEquals("TR", LearningModelsCustom.strategyIndexToStringS3(0));
		assertEquals("MM", LearningModelsCustom.strategyIndexToStringS3(1));
		assertEquals("GB", LearningModelsCustom.strategyIndexToStringS3(2));
		assertEquals("OP", LearningModelsCustom.strategyIndexToStringS3(3));
		assertEquals("RA", LearningModelsCustom.strategyIndexToStringS3(4));
	}
}
