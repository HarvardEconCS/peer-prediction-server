package edu.harvard.econcs.peerprediction.analysis;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.andrewmao.misc.Pair;

import org.junit.Test;

public class LogReaderTest {

	Random rand = new Random();
	
	@Test
	public void testSelectByDist() {
		
		int limit = 10000;
		double firstProb = 0.6;
		int[] count = new int[]{0,0};
		for (int i = 0; i < limit; i++) {
			int chosen = Utils.selectByBinaryDist(firstProb);
			count[chosen]++;
		}
		double expected = limit * firstProb;
		assertEquals(expected, count[0], 100);

	}
	
	@Test
	public void testChooseRefPlayer() {
		
		int currPlayer;
		int chosen;
		
		int count = 10000;
		
		currPlayer = 0;
		chosen = Utils.chooseRefPlayer(currPlayer);
		assertNotSame(currPlayer, chosen);
		assertTrue(chosen == 2 || chosen == 1);
		
		int countOne = 0;
		for (int i = 0; i < count; i++) {
			chosen = Utils.chooseRefPlayer(currPlayer);
			if (chosen == 1)
				countOne++;
		}
		assertEquals(count / 2, countOne, 120);
		
		currPlayer = 1;
		chosen = Utils.chooseRefPlayer(currPlayer);
		assertNotSame(currPlayer, chosen);
		assertTrue(chosen == 2 || chosen == 0);
		int countZero = 0;
		for (int i = 0; i < count; i++) {
			chosen = Utils.chooseRefPlayer(currPlayer);
			if (chosen == 0)
				countZero++;
		}
		assertEquals(count / 2, countZero, 100);

		currPlayer = 2;
		chosen = Utils.chooseRefPlayer(currPlayer);
		assertNotSame(currPlayer, chosen);
		assertTrue(chosen == 1 || chosen == 0);
		countOne = 0;
		for (int i = 0; i < count; i++) {
			chosen = Utils.chooseRefPlayer(currPlayer);
			if (chosen == 0)
				countOne++;
		}
		assertEquals(count / 2, countOne, 120);
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
		
		Map<String, Map<String, Object>> result = 
				new HashMap<String, Map<String, Object>>();
		
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
		int numMM = Utils.getNumOfGivenReport(
				"MM", playerIds, playerIds[excludeIndex], result);
		assertEquals(expectedNumMM, numMM);
	} 
	
	@Test
	public void testGetMMProb() {
		double mmProb = Utils.calcMMProb(10, 1, 1);
		assertEquals(0.5, mmProb, Utils.eps);
		
		mmProb = Utils.calcMMProb(4, 20, 20);
		assertEquals(0.5, mmProb, Utils.eps);
	}
	
	@Test
	public void testEstimateRL() {

		LogReader.parseLog();
		
		// test 1
		System.out.println();
		System.out.println("Test 1");
		boolean considerSignal = true;
		double discount = 0.5;
		double lambda = 5;
		System.out.printf(
				"Expected parameters for RL; l=%.2f, d=%.2f, s=%b\n",
				lambda, discount, considerSignal);
		
		List<Game> games = new ArrayList<Game>();
		for (int i = 0; i < 100; i++) {
			List<Round> rounds = LogReader.simulateRLOneGame(considerSignal, discount, lambda);
			Game game = new Game();
			game.rounds = rounds;
			game.playerHitIds = new String[LogReader.expSet.numPlayers];
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
				game.playerHitIds[j] = String.format("%d", j);
			}		
			games.add(game);
		}
		
		Map<String, Object> bestParam = LogReader.estimateRL(games);
		assertEquals(considerSignal, (boolean) bestParam.get("considerSignal"));
		assertEquals(discount, (double) bestParam.get("discount"), 0.05);
		assertEquals(lambda, (double) bestParam.get("lambda"), 1);
		

		// test 2
		System.out.println();
		System.out.println("Test 2");
		considerSignal = false;
		discount = 0.8;
		lambda = 10;
		System.out.printf(
				"Expected parameters for RL; l=%.2f, d=%.2f, s=%b\n",
				lambda, discount, considerSignal);
		
		games = new ArrayList<Game>();
		for (int i = 0; i < 100; i++) {
			List<Round> rounds = LogReader.simulateRLOneGame(considerSignal, discount, lambda);
			Game game = new Game();
			game.rounds = rounds;
			game.playerHitIds = new String[LogReader.expSet.numPlayers];
			for (int j = 0; j < LogReader.expSet.numPlayers; j++) {
				game.playerHitIds[j] = String.format("%d", j);
			}		
			games.add(game);
		}
		
		bestParam = new HashMap<String, Object>();
		bestParam = LogReader.estimateRL(games);
		assertEquals(considerSignal, (boolean) bestParam.get("considerSignal"));
		assertEquals(discount, (double) bestParam.get("discount"), 0.05);
		assertEquals(lambda, (double) bestParam.get("lambda"), 1);

	}
	
	@Test
	public void testDiscountAll() {
		int numPlayers = 3;
		String[] playerHitIds = new String[numPlayers];
		for (int i = 0; i < numPlayers; i++) {
			playerHitIds[i] = String.format("%d", i);
		}
		
		Map<String, Map<Pair<String, String>, Double>> map = 
				new HashMap<String, Map<Pair<String, String>, Double>>();
		for (String player : playerHitIds) {
			Map<Pair<String, String>, Double> payoffs = new HashMap<Pair<String, String>, Double>();
			payoffs.put(new Pair<String, String>("MM", "MM"), 2.0);
			payoffs.put(new Pair<String, String>("MM", "GB"), 4.0);
			payoffs.put(new Pair<String, String>("GB", "MM"), 1.0);
			payoffs.put(new Pair<String, String>("GB", "GB"), 5.0);
			map.put(player, payoffs);
		}
		
		LogReader.discountAll(0.3, map);
		
		for (String player: playerHitIds) {
			assertEquals(0.3 * 2.0, map.get(player).get(new Pair<String, String>("MM", "MM")), Utils.eps);
			assertEquals(0.3 * 4.0, map.get(player).get(new Pair<String, String>("MM", "GB")), Utils.eps);
			assertEquals(0.3 * 1.0, map.get(player).get(new Pair<String, String>("GB", "MM")), Utils.eps);
			assertEquals(0.3 * 5.0, map.get(player).get(new Pair<String, String>("GB", "GB")), Utils.eps);
		}
	}
	
}
