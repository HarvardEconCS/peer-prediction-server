package edu.harvard.econcs.peerprediction.analysis;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GameTest {

	Game game;
	
	@Before
	public void setUp() throws Exception {
		game = new Game();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testT1() {
		System.out.println("Testing T1");
		
		String treatment = "prior2-basic";
		
		Map<String, Double> oppStrategy = new HashMap<String, Double>();
		String myReport;

		double percentMM = 0.0;
		while (percentMM < 1.0) {

			oppStrategy.clear();
			oppStrategy.put("MM", percentMM);
			oppStrategy.put("GB", 1 - percentMM);
			myReport = Utils.getBestResponse(treatment, oppStrategy);
			System.out.println(String.format("%.1f, %s", percentMM, myReport));
			
			if (percentMM > 0.4)
				assertEquals("MM", myReport);
			else 
				assertEquals("GB", myReport);
			
			percentMM += 0.1;
		}	
		System.out.println();
	}
	
	@Test
	public void testT2() {
		System.out.println("Testing T2");
		String treatment = "prior2-outputagreement";
		
		Map<String, Double> oppStrategy = new HashMap<String, Double>();
		String myReport;

		double percentMM = 0.0;
		while (percentMM < 1.0) {

			oppStrategy.clear();
			oppStrategy.put("MM", percentMM);
			oppStrategy.put("GB", 1 - percentMM);
			myReport = Utils.getBestResponse(treatment, oppStrategy);
			System.out.println(String.format("%.1f, %s", percentMM, myReport));
			
			if (percentMM > 0.5)
				assertEquals("MM", myReport);
			else 
				assertEquals("GB", myReport);
			
			percentMM += 0.1;
		}	
		System.out.println();
	}
	
	@Test
	public void testT3() {
		System.out.println("Testing T3");
		String treatment = "prior2-uniquetruthful";
		Map<String, Double> oppStrategy = new HashMap<String, Double>();
		oppStrategy.put("MM", 1.0);
		oppStrategy.put("GB", 0.0);
		String myReport = Utils.getBestResponse(treatment, oppStrategy);
		assertEquals(myReport, "GB");
		
		oppStrategy.clear();
		oppStrategy.put("MM", 0.0);
		oppStrategy.put("GB", 1.0);
		myReport = Utils.getBestResponse(treatment, oppStrategy);
		assertEquals(myReport, "MM");
		
		double percentMM = 0.1;
		while (percentMM < 0.99) {
			oppStrategy.clear();
			oppStrategy.put("MM", percentMM);
			oppStrategy.put("GB", 1 - percentMM);
			myReport = Utils.getBestResponse(treatment, oppStrategy);
			System.out.println(String.format("%.1f, %s", percentMM, myReport));
			
			if (percentMM > 0.5) 
				assertEquals("MM", myReport);
			else 
				assertEquals("GB", myReport);
			
			percentMM += 0.1;
		}
		
	}
	
	



}
