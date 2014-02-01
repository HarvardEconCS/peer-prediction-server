package edu.harvard.econcs.peerprediction.analysis;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
		
		double[] paymentArray = new double[] {1.5, 0.1, 0.3, 1.2};
		
		Map<String, Double> oppStrategy = new HashMap<String, Double>();
		String myReport;

		double percentMM = 0.0;
		while (percentMM < 1.0) {

			oppStrategy.clear();
			oppStrategy.put("MM", percentMM);
			oppStrategy.put("GB", 1 - percentMM);
			myReport = game.getBestResponseT1N2(oppStrategy, paymentArray);
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
		double[] paymentArray = new double[] {1.5, 0.1, 0.1, 1.5};
		
		Map<String, Double> oppStrategy = new HashMap<String, Double>();
		String myReport;

		double percentMM = 0.0;
		while (percentMM < 1.0) {

			oppStrategy.clear();
			oppStrategy.put("MM", percentMM);
			oppStrategy.put("GB", 1 - percentMM);
			myReport = game.getBestResponseT1N2(oppStrategy, paymentArray);
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
		
		Map<String, Double> oppStrategy = new HashMap<String, Double>();
		oppStrategy.put("MM", 1.0);
		oppStrategy.put("GB", 0.0);
		String myReport = game.getBestResponseT3(oppStrategy);
		assertEquals(myReport, "GB");
		
		oppStrategy.clear();
		oppStrategy.put("MM", 0.0);
		oppStrategy.put("GB", 1.0);
		myReport = game.getBestResponseT3(oppStrategy);
		assertEquals(myReport, "MM");
		
		double percentMM = 0.1;
		while (percentMM < 0.99) {
			oppStrategy.clear();
			oppStrategy.put("MM", percentMM);
			oppStrategy.put("GB", 1 - percentMM);
			myReport = game.getBestResponseT3(oppStrategy);
			System.out.println(String.format("%.1f, %s", percentMM, myReport));
			
			if (percentMM > 0.5) 
				assertEquals("MM", myReport);
			else 
				assertEquals("GB", myReport);
			
			percentMM += 0.1;
		}
		
	}
	
	@Test
	public void testGetPaymentT1N2() {
		double[] paymentArray = new double[]{0.3, 0.4, 0.5, 0.6};

		double pay = game.getPaymentT1N2("MM", "MM", paymentArray);
		assertEquals(paymentArray[0], pay, AnalysisUtils.eps);
		
		pay = game.getPaymentT1N2("MM", "GB", paymentArray);
		assertEquals(paymentArray[1], pay, AnalysisUtils.eps);
		
		pay = game.getPaymentT1N2("GB", "MM", paymentArray);
		assertEquals(paymentArray[2], pay, AnalysisUtils.eps);
		
		pay = game.getPaymentT1N2("GB", "GB", paymentArray);
		assertEquals(paymentArray[3], pay, AnalysisUtils.eps);

	}
	
	@Test
	public void testCountRefReports() {
		List<String> refReports = new ArrayList<String>();
		refReports.add("MM");refReports.add("GB");refReports.add("GB");
		refReports.add("MM");refReports.add("MM");
		int numMM = game.getNumMMInRefReports(refReports);
		assertEquals(numMM, 3);
	}
	


}
