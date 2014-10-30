package edu.harvard.econcs.peerprediction.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class UtilsTest {

	double[] priorProbs = new double[] { 0.5, 0.5 };
	List<Map<String, Double>> prior = new ArrayList<Map<String, Double>>();

	@Before
	public void setUp() throws Exception {
		prior.add(ImmutableMap.of("MM", 0.2, "GB", 0.8));
		prior.add(ImmutableMap.of("MM", 0.7, "GB", 0.3));
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testNormalizeDist() {
		double[] dist = new double[]{1,2,3};
		Utils.normalizeDist(dist);
		assertEquals(1.0/6, dist[0], Utils.eps);
		assertEquals(2.0/6, dist[1], Utils.eps);
		assertEquals(3.0/6, dist[2], Utils.eps);
		
	}
	
	@Test
	public void testGetRandomList() {
		List<Double> list = Utils.getRandomList(3);
		double total = 0;
		for (int i = 0; i < list.size(); i++) {
			double value = list.get(i).doubleValue();
			assertTrue(value > 0);
			total += value;
		}
		assertEquals(1, total, Utils.eps);

		list = Utils.getRandomList(5);
		total = 0;
		for (int i = 0; i < list.size(); i++) {
			double value = list.get(i).doubleValue();
			assertTrue(value > 0);
			total += value;
		}
		assertEquals(1, total, Utils.eps);

	}



	@Test
	public void testGetPayment() {
		String treatment = "prior2-basic";
		assertEquals(1.5, Utils.getPayment(treatment, "MM", "MM"),
				Utils.eps);
		assertEquals(0.1, Utils.getPayment(treatment, "MM", "GB"),
				Utils.eps);
		assertEquals(0.3, Utils.getPayment(treatment, "GB", "MM"),
				Utils.eps);
		assertEquals(1.2, Utils.getPayment(treatment, "GB", "GB"),
				Utils.eps);

		treatment = "prior2-outputagreement";
		assertEquals(1.5, Utils.getPaymentTreatmentOutputAgreement("MM", "MM"),
				Utils.eps);
		assertEquals(0.1, Utils.getPaymentTreatmentOutputAgreement("MM", "GB"),
				Utils.eps);
		assertEquals(0.1, Utils.getPaymentTreatmentOutputAgreement("GB", "MM"),
				Utils.eps);
		assertEquals(1.5, Utils.getPaymentTreatmentOutputAgreement("GB", "GB"),
				Utils.eps);
		
		treatment = "prior2-uniquetruthful";
		assertEquals(0.9,
				Utils.getPaymentTreatmentUniqueTruthful("MM", 0),
				Utils.eps);
		assertEquals(0.1,
				Utils.getPaymentTreatmentUniqueTruthful("MM", 1),
				Utils.eps);
		assertEquals(1.5,
				Utils.getPaymentTreatmentUniqueTruthful("MM", 2),
				Utils.eps);
		assertEquals(0.8,
				Utils.getPaymentTreatmentUniqueTruthful("MM", 3),
				Utils.eps);

		assertEquals(0.8,
				Utils.getPaymentTreatmentUniqueTruthful("GB", 0),
				Utils.eps);
		assertEquals(1.5,
				Utils.getPaymentTreatmentUniqueTruthful("GB", 1),
				Utils.eps);
		assertEquals(0.1,
				Utils.getPaymentTreatmentUniqueTruthful("GB", 2),
				Utils.eps);
		assertEquals(0.9,
				Utils.getPaymentTreatmentUniqueTruthful("GB", 3),
				Utils.eps);
		
		treatment = "prior2-symmlowpay";
		assertEquals(0.1,
				Utils.getPaymentTreatmentSymmLowPay("MM", 0),
				Utils.eps);
		assertEquals(0.1,
				Utils.getPaymentTreatmentSymmLowPay("MM", 1),
				Utils.eps);
		assertEquals(1.5,
				Utils.getPaymentTreatmentSymmLowPay("MM", 2),
				Utils.eps);
		assertEquals(0.15,
				Utils.getPaymentTreatmentSymmLowPay("MM", 3),
				Utils.eps);

		assertEquals(0.15,
				Utils.getPaymentTreatmentSymmLowPay("GB", 0),
				Utils.eps);
		assertEquals(0.9,
				Utils.getPaymentTreatmentSymmLowPay("GB", 1),
				Utils.eps);
		assertEquals(0.15,
				Utils.getPaymentTreatmentSymmLowPay("GB", 2),
				Utils.eps);
		assertEquals(0.1,
				Utils.getPaymentTreatmentSymmLowPay("GB", 3),
				Utils.eps);

	}

	@Test
	public void testCalcMMProb() {
		double attrMM; double attrGB; double lambda; double expMMProb;
		
		attrMM = 0.2;
		double mmProb = Utils.calcMMProb(0.3, attrMM, attrMM);
		assertEquals(0.5, mmProb, Utils.eps);
		
		attrMM = 0.3;
		attrGB = 0.1;
		lambda = 1;
		mmProb = Utils.calcMMProb(lambda, attrMM, attrGB);
		expMMProb = Math.pow(Math.E, attrMM) / (Math.pow(Math.E, attrMM) + Math.pow(Math.E, attrGB));
		assertEquals(expMMProb, mmProb, Utils.eps);
		
		attrMM = 1;
		attrGB = 0.4;
		lambda = 10;
		mmProb = Utils.calcMMProb(lambda, attrMM, attrGB);
		expMMProb = 0.9975273768;
		assertEquals(expMMProb, mmProb, Utils.eps);
	}
	
	@Test
	public void testGetNumMMReports() {
		Game game = new Game();
		int numPlayers = 3;
		game.playerHitIds = new String[numPlayers];
		for (int i = 0; i < numPlayers; i++) {
			game.playerHitIds[i] = String.format("%d", i);
		}
		
		game.rounds = new ArrayList<Round>();
		int numRounds = 5;
		for (int i = 0; i < numRounds; i++) {
			Round round = new Round();
			round.result = new HashMap<String, Map<String, Object>>();
		}
		
	}

}
