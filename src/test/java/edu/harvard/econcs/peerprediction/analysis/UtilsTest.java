package edu.harvard.econcs.peerprediction.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
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
	public void testProbRefReportGivenSignalAndStrategy() {
		double prob1 = Utils.getProbRefReportGivenSignalAndStrategy(
				"MM", "MM", 1.0, 1.0, priorProbs, prior);
		assertEquals(1, prob1, Utils.eps);

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
	public void testProbRefReportsGivenSignalAndStrategy() {
		double prob1 = Utils.getProbRefReportsGivenSignalAndStrategy(3,
				0, "MM", 1.0, 1.0, priorProbs, prior);
		assertEquals(1, prob1, Utils.eps);

		double prob2 = Utils.getProbRefReportsGivenSignalAndStrategy(2,
				1, "MM", 1.0, 1.0, priorProbs, prior);
		assertEquals(0, prob2, Utils.eps);

		double prob3 = Utils.getProbRefReportsGivenSignalAndStrategy(1,
				2, "MM", 1.0, 1.0, priorProbs, prior);
		assertEquals(0, prob3, Utils.eps);

		double prob4 = Utils.getProbRefReportsGivenSignalAndStrategy(0,
				3, "MM", 1.0, 1.0, priorProbs, prior);
		assertEquals(0, prob4, Utils.eps);

		double prob5 = Utils.getProbRefReportsGivenSignalAndStrategy(0,
				3, "MM", 0.5, 0.5, priorProbs, prior);
		assertEquals(0.125, prob5, Utils.eps);

		double prob6 = Utils.getProbRefReportsGivenSignalAndStrategy(0,
				3, "GB", 0.5, 0.5, priorProbs, prior);
		assertEquals(0.125, prob6, Utils.eps);

		double prob7 = Utils.getProbRefReportsGivenSignalAndStrategy(1,
				2, "GB", 0.5, 0.5, priorProbs, prior);
		assertEquals(0.375, prob7, Utils.eps);

	}

	@Test
	public void testProbSignalsGivenSignal() {

		double prob0MMGivenMM = Utils.getProbSignalsGivenSignal(0, 3,
				"MM", priorProbs, prior);
		assertEquals(1213.0 / 9000, prob0MMGivenMM, Utils.eps);

		double prob1MMGivenMM = Utils.getProbSignalsGivenSignal(1, 2,
				"MM", priorProbs, prior);
		assertEquals(697.0 / 3000, prob1MMGivenMM, Utils.eps);

		double prob2MMGivenMM = Utils.getProbSignalsGivenSignal(2, 1,
				"MM", priorProbs, prior);
		assertEquals(1093.0 / 3000, prob2MMGivenMM, Utils.eps);

		double prob3MMGivenMM = Utils.getProbSignalsGivenSignal(3, 0,
				"MM", priorProbs, prior);
		assertEquals(2417.0 / 9000, prob3MMGivenMM, Utils.eps);

		double prob0MMGivenGB = Utils.getProbSignalsGivenSignal(0, 3,
				"GB", priorProbs, prior);
		assertEquals(4177.0 / 11000, prob0MMGivenGB, Utils.eps);

		double prob1MMGivenGB = Utils.getProbSignalsGivenSignal(1, 2,
				"GB", priorProbs, prior);
		assertEquals(3639.0 / 11000, prob1MMGivenGB, Utils.eps);

		double prob2MMGivenGB = Utils.getProbSignalsGivenSignal(2, 1,
				"GB", priorProbs, prior);
		assertEquals(2091.0 / 11000, prob2MMGivenGB, Utils.eps);

		double prob3MMGivenGB = Utils.getProbSignalsGivenSignal(3, 0,
				"GB", priorProbs, prior);
		assertEquals(1093.0 / 11000, prob3MMGivenGB, Utils.eps);
	}

	@Test
	public void testProbSignalGivenSignal() {

		double probMMGivenMM = Utils.getProbSignalGivenSignal("MM",
				"MM", priorProbs, prior);
		assertEquals(53.0 / 90, probMMGivenMM, Utils.eps);

		double probMMGivenGB = Utils.getProbSignalGivenSignal("MM",
				"GB", priorProbs, prior);
		assertEquals(37.0 / 110, probMMGivenGB, Utils.eps);

	}

	@Test
	public void testProbMM() {
		double probMM = Utils.getProbMM(priorProbs, prior);
		assertEquals(9.0 / 20, probMM, Utils.eps);

	}

	@Test
	public void testMixedPayoff() {

		String treatment = "prior2-symmlowpay";
		double alwaysMMPayoffT5 = Utils.getMixedPayoff(treatment,
				priorProbs, prior, 1.0, 1.0);
		assertEquals(0.15, alwaysMMPayoffT5, Utils.eps);

		double alwaysGBPayoffT5 = Utils.getMixedPayoff(treatment,
				priorProbs, prior, 0.0, 0.0);
		assertEquals(0.15, alwaysGBPayoffT5, Utils.eps);

		treatment = "prior2-uniquetruthful";
		double alwaysMMPayoffT3 = Utils.getMixedPayoff(treatment,
				priorProbs, prior, 1.0, 1.0);
		assertEquals(0.8, alwaysMMPayoffT3, Utils.eps);

		double alwaysGBPayoffT3 = Utils.getMixedPayoff(treatment,
				priorProbs, prior, 0.0, 0.0);
		assertEquals(0.8, alwaysGBPayoffT3, Utils.eps);

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
	public void testGetOppPopStr() {
		List<String> reports = new ArrayList<String>();
		reports.add("MM");
		reports.add("MM");
		reports.add("MM");
		Map<String, Double> strategy = Utils.getOppPopStr(reports);
		assertEquals(strategy.get("MM"), 1, 0.00001);
		assertEquals(strategy.get("GB"), 0, 0.00001);

		reports.clear();
		reports.add("MM");
		reports.add("MM");
		reports.add("GB");
		strategy = Utils.getOppPopStr(reports);
		assertEquals(0.66666667, strategy.get("MM"), 0.00000001);
		assertEquals(0.33333333, strategy.get("GB"), 0.00000001);

		reports.clear();
		reports.add("MM");
		reports.add("GB");
		reports.add("GB");
		strategy = Utils.getOppPopStr(reports);
		assertEquals(0.33333333, strategy.get("MM"), 0.00000001);
		assertEquals(0.66666667, strategy.get("GB"), 0.00000001);

		reports.clear();
		reports.add("GB");
		reports.add("GB");
		reports.add("GB");
		strategy = Utils.getOppPopStr(reports);
		assertEquals(0, strategy.get("MM"), 0.00000001);
		assertEquals(1, strategy.get("GB"), 0.00000001);

		reports.clear();
		reports.add("GB");
		reports.add("GB");
		strategy = Utils.getOppPopStr(reports);
		assertEquals(0, strategy.get("MM"), 0.00000001);
		assertEquals(1, strategy.get("GB"), 0.00000001);

		reports.clear();
		reports.add("MM");
		reports.add("GB");
		strategy = Utils.getOppPopStr(reports);
		assertEquals(0.5, strategy.get("MM"), 0.00000001);
		assertEquals(0.5, strategy.get("GB"), 0.00000001);

		reports.clear();
		reports.add("MM");
		reports.add("MM");
		strategy = Utils.getOppPopStr(reports);
		assertEquals(1, strategy.get("MM"), 0.00000001);
		assertEquals(0, strategy.get("GB"), 0.00000001);

	}
	
	@Test
	public void testCountRefReports() {
		List<String> refReports = new ArrayList<String>();
		refReports.add("MM");refReports.add("GB");refReports.add("GB");
		refReports.add("MM");refReports.add("MM");
		int numMM = Utils.getNumMMInRefReports(refReports);
		assertEquals(numMM, 3);
	}
	

}
