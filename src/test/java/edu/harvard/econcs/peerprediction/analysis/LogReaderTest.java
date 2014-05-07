package edu.harvard.econcs.peerprediction.analysis;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

public class LogReaderTest {

	Random rand = new Random();
	
	@Test
	public void testSelectByDist() {
		
		int limit = 10000;
		double firstProb = 0.6;
		int[] count = new int[]{0,0};
		for (int i = 0; i < limit; i++) {
			int chosen = LogReader.selectByDist(firstProb);
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
		chosen = LogReader.chooseRefPlayer(currPlayer);
		assertNotSame(currPlayer, chosen);
		assertTrue(chosen == 2 || chosen == 1);
		
		int countOne = 0;
		for (int i = 0; i < count; i++) {
			chosen = LogReader.chooseRefPlayer(currPlayer);
			if (chosen == 1)
				countOne++;
		}
		assertEquals(count / 2, countOne, 50);
		
		currPlayer = 1;
		chosen = LogReader.chooseRefPlayer(currPlayer);
		assertNotSame(currPlayer, chosen);
		assertTrue(chosen == 2 || chosen == 0);
		int countZero = 0;
		for (int i = 0; i < count; i++) {
			chosen = LogReader.chooseRefPlayer(currPlayer);
			if (chosen == 0)
				countZero++;
		}
		assertEquals(count / 2, countZero, 50);

		currPlayer = 2;
		chosen = LogReader.chooseRefPlayer(currPlayer);
		assertNotSame(currPlayer, chosen);
		assertTrue(chosen == 1 || chosen == 0);
		countOne = 0;
		for (int i = 0; i < count; i++) {
			chosen = LogReader.chooseRefPlayer(currPlayer);
			if (chosen == 0)
				countOne++;
		}
		assertEquals(count / 2, countOne, 50);


	}

	
	@Test
	public void testNormalizeDist() {
		double[] dist = new double[]{1,2,3};
		LogReader.normalizeDist(dist);
		assertEquals(1.0/6, dist[0], AnalysisUtils.eps);
		assertEquals(2.0/6, dist[1], AnalysisUtils.eps);
		assertEquals(3.0/6, dist[2], AnalysisUtils.eps);
		
	}

	
	@Test
	public void testGetExpectedPayoff() {
		
		LogReader.treatment = "prior2-basic";
		
		double pay = LogReader.getExpectedPayoffT12("MM", 2);
		assertEquals(1.5, pay, AnalysisUtils.eps);
		
		pay = LogReader.getExpectedPayoffT12("MM", 0);
		assertEquals(0.1, pay, AnalysisUtils.eps);
		
		pay = LogReader.getExpectedPayoffT12("MM", 1);
		assertEquals(0.8, pay, AnalysisUtils.eps);
		
		pay = LogReader.getExpectedPayoffT12("GB", 2);
		assertEquals(0.3, pay, AnalysisUtils.eps);
		
		pay = LogReader.getExpectedPayoffT12("GB", 0);
		assertEquals(1.2, pay, AnalysisUtils.eps);
		
		pay = LogReader.getExpectedPayoffT12("GB", 1);
		assertEquals(0.75, pay, AnalysisUtils.eps);
	}
	
	@Test
	public void testGetNumMMReports() {
		String[] playerIds = new String[]{"0", "1", "2"};
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
		int numMM = LogReader.getNumGivenReports("MM", playerIds, playerIds[excludeIndex], result);
		assertEquals(expectedNumMM, numMM);
	} 
	
	@Test
	public void testGetMMProb() {
		double mmProb = LogReader.getMMProb(10, 1, 1);
		assertEquals(0.5, mmProb, AnalysisUtils.eps);
		
		mmProb = LogReader.getMMProb(4, 20, 20);
		assertEquals(0.5, mmProb, AnalysisUtils.eps);
	}
	
}
