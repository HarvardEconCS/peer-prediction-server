package edu.harvard.econcs.peerprediction;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.peerprediction.PeerPrior;

public class PeerPriorTest {

	PeerPrior prior;
	
	@Before
	public void setUp() throws Exception {
		prior = PeerPrior.getTestPrior();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testProbCalc() {
		
		String sMM = "MM";
		String sGB = "GB";
		double ProbMMExpected = 0.575;
		
		double ProbMM = prior.getProbForSignal(sMM);
		assertEquals(ProbMMExpected, ProbMM, 0.001);
		
		double ProbGB = prior.getProbForSignal(sGB);
		assertEquals(1 - ProbMMExpected, ProbGB, 0.001);
		
		double ProbMMAndMMExpected = 0.40625;
		double ProbMMAndGBExpected = 0.16875;
		double ProbMMAndMM = prior.getProbForSignalPair(sMM, sMM);
		assertEquals(ProbMMAndMMExpected, ProbMMAndMM, 0.00001);
		double ProbMMAndGB = prior.getProbForSignalPair(sMM, sGB);
		assertEquals(ProbMMAndGBExpected, ProbMMAndGB, 0.00001);
		
		double ProbMMGMMExpected = 0.706521739;
		double ProbMMGMM = prior.getProbSignal1GivenSignal2(sMM, sMM);
		assertEquals(ProbMMGMMExpected, ProbMMGMM, 0.000000001);
		
		double ProbMMGGBExpected = 0.397058824;
		double ProbMMGGB = prior.getProbSignal1GivenSignal2(sMM, sGB);
		assertEquals(ProbMMGGBExpected, ProbMMGGB, 0.000000001);
		
	}
	
	@Test
	public void testChooseWorld() {
		int num = 10000000;
		int count = 0;
		int numWorld1 = 0;
		while (count < num) {
			Map<String, Double> chosenWorld = prior.chooseWorld();
			assertTrue(Double.compare(chosenWorld.get("MM"), 0.85) == 0 ||
					Double.compare(chosenWorld.get("MM"), 0.30) == 0);
			
			if (Double.compare(chosenWorld.get("MM"), 0.85) == 0) {
				numWorld1++;
			}
			count++;
		} 
		double ratioDerived = numWorld1/(double)num;
		double ratioExpected = 0.5;
		assertEquals(ratioDerived, ratioExpected, 0.001);
	}

	@Test
	public void testChooseSignal() {
		
		Map<String, Double> chosenWorld = prior.chooseWorld();
		int num = 10000000;
		int count = 0;
		int numSignal0 = 0;
		if (Double.compare(chosenWorld.get("MM"), 0.85) == 0) {
			
			while (count < num) {
				String chosenSignal = prior.chooseSignal(chosenWorld);
				if (chosenSignal.equals("MM"))
					numSignal0++;
				count++;
			}
			
			double ratioDerived = numSignal0/(double)num;
			double ratioExpected = 0.85;
			assertEquals(ratioExpected, ratioDerived, 0.001);
			
		} else if (Double.compare(chosenWorld.get("MM"), 0.3) == 0 ) {
			
			while (count < num) {
				String chosenSignal = prior.chooseSignal(chosenWorld);
				if (chosenSignal.equals("MM"))
					numSignal0++;
				count++;
			}
			
			double ratioDerived = numSignal0/(double)num;
			double ratioExpected = 0.3;
			assertEquals(ratioExpected, ratioDerived, 0.001);
			
		} else {
			fail("chosen an unknown world");
		}
		
		
	}
	
	
}
