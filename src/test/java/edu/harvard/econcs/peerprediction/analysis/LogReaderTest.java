package edu.harvard.econcs.peerprediction.analysis;

import static org.junit.Assert.*;

import org.junit.Test;

public class LogReaderTest {

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
	public void testGetBestResponse() {
		LogReader.treatment = "prior2-basic";
		String bestResponse = LogReader.getBestResponse(1, 1);
		assertEquals("MM", bestResponse);
		
		bestResponse = LogReader.getBestResponse(2, 0);
		assertEquals("MM", bestResponse);

		bestResponse = LogReader.getBestResponse(0, 2);
		assertEquals("GB", bestResponse);

		LogReader.treatment = "prior2-outputagreement";
		bestResponse = LogReader.getBestResponse(1, 1);
		assertEquals("Mixed", bestResponse);
		
		bestResponse = LogReader.getBestResponse(2, 0);
		assertEquals("MM", bestResponse);

		bestResponse = LogReader.getBestResponse(0, 2);
		assertEquals("GB", bestResponse);

	}
}
