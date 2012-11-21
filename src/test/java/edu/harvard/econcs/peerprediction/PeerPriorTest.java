package edu.harvard.econcs.peerprediction;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
	public void testChooseWorld() {
		int num = 10000000;
		int count = 0;
		int numWorld1 = 0;
		while (count < num) {
			Map<String, Double> chosenWorld = prior.chooseWorld();
			assertTrue(Double.compare(chosenWorld.get("MM"), 0.4) == 0 ||
					Double.compare(chosenWorld.get("MM"), 0.8) == 0);
			
			if (Double.compare(chosenWorld.get("MM"), 0.8) == 0) {
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
		if (Double.compare(chosenWorld.get("MM"), 0.4) == 0) {
			
			while (count < num) {
				String chosenSignal = prior.chooseSignal(chosenWorld);
				if (chosenSignal.equals("MM"))
					numSignal0++;
				count++;
			}
			
			double ratioDerived = numSignal0/(double)num;
			double ratioExpected = 0.4;
			assertEquals(ratioExpected, ratioDerived, 0.001);
			
		} else if (Double.compare(chosenWorld.get("MM"), 0.8) == 0 ) {
			
			while (count < num) {
				String chosenSignal = prior.chooseSignal(chosenWorld);
				if (chosenSignal.equals("MM"))
					numSignal0++;
				count++;
			}
			
			double ratioDerived = numSignal0/(double)num;
			double ratioExpected = 0.8;
			assertEquals(ratioExpected, ratioDerived, 0.001);
			
		} else {
			fail("chosen an unknown world");
		}
		
		
	}
	
	
}
