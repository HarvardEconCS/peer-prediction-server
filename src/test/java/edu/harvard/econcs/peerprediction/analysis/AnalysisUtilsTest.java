package edu.harvard.econcs.peerprediction.analysis;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class AnalysisUtilsTest {

	double[] priorProbs = new double[]{0.5,0.5};
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
	public void testProbSignalsGivenSignal() {
		
		double prob0MMGivenMM = AnalysisUtils.getProbSignalsGivenSignal(0, 3,
				"MM", priorProbs, prior);
		assertEquals(1213.0/9000, prob0MMGivenMM, AnalysisUtils.eps);

		double prob1MMGivenMM = AnalysisUtils.getProbSignalsGivenSignal(1, 2,
				"MM", priorProbs, prior);
		assertEquals(697.0/3000, prob1MMGivenMM, AnalysisUtils.eps);

		double prob2MMGivenMM = AnalysisUtils.getProbSignalsGivenSignal(2, 1,
				"MM", priorProbs, prior);
		assertEquals(1093.0/3000, prob2MMGivenMM, AnalysisUtils.eps);
		
		double prob3MMGivenMM = AnalysisUtils.getProbSignalsGivenSignal(3, 0,
				"MM", priorProbs, prior);
		assertEquals(2417.0/9000, prob3MMGivenMM, AnalysisUtils.eps);
		
		
		double prob0MMGivenGB = AnalysisUtils.getProbSignalsGivenSignal(0, 3,
				"GB", priorProbs, prior);
		assertEquals(4177.0/11000, prob0MMGivenGB, AnalysisUtils.eps);
		
		double prob1MMGivenGB = AnalysisUtils.getProbSignalsGivenSignal(1, 2,
				"GB", priorProbs, prior);
		assertEquals(3639.0/11000, prob1MMGivenGB, AnalysisUtils.eps);
		
		double prob2MMGivenGB = AnalysisUtils.getProbSignalsGivenSignal(2, 1,
				"GB", priorProbs, prior);
		assertEquals(2091.0/11000, prob2MMGivenGB, AnalysisUtils.eps);
		
		double prob3MMGivenGB = AnalysisUtils.getProbSignalsGivenSignal(3, 0,
				"GB", priorProbs, prior);
		assertEquals(1093.0/11000, prob3MMGivenGB, AnalysisUtils.eps);
	}
	
	@Test
	public void testProbSignalGivenSignal() {

		double probMMGivenMM = AnalysisUtils.getProbSignalGivenSignal("MM", "MM", priorProbs, prior);
		assertEquals(53.0/90, probMMGivenMM, AnalysisUtils.eps);
		
		double probMMGivenGB = AnalysisUtils.getProbSignalGivenSignal("MM", "GB", priorProbs, prior);
		assertEquals(37.0/110, probMMGivenGB, AnalysisUtils.eps);
		
	}

	@Test 
	public void testProbMM() {
		double probMM = AnalysisUtils.getProbMM(priorProbs, prior);
		assertEquals(9.0/20, probMM, AnalysisUtils.eps);
		
	}
	
	@Test
	public void testMixedPayoff() {
		
		String rule = "T5";		
		double alwaysMMPayoffT5 = AnalysisUtils.getMixedPayoff(rule, priorProbs, prior, 1.0, 1.0);
		assertEquals(0.15, alwaysMMPayoffT5, AnalysisUtils.eps);
		
		double alwaysGBPayoffT5 = AnalysisUtils.getMixedPayoff(rule, priorProbs, prior, 0.0, 0.0);
		assertEquals(0.15, alwaysGBPayoffT5, AnalysisUtils.eps);

		rule = "T3";
		double alwaysMMPayoffT3 = AnalysisUtils.getMixedPayoff(rule, priorProbs, prior, 1.0, 1.0);
		assertEquals(0.8, alwaysMMPayoffT3, AnalysisUtils.eps);
		
		double alwaysGBPayoffT3 = AnalysisUtils.getMixedPayoff(rule, priorProbs, prior, 0.0, 0.0);
		assertEquals(0.8, alwaysGBPayoffT3, AnalysisUtils.eps);

		
	}
	
	@Test
	public void testGetOppPopStr() {
		List<String> reports = new ArrayList<String>();
		reports.add("MM");reports.add("MM");reports.add("MM");
		Map<String, Double> strategy = AnalysisUtils.getOppPopStr(reports);
		assertEquals(strategy.get("MM"), 1, 0.00001);
		assertEquals(strategy.get("GB"), 0, 0.00001);
		
		reports.clear();
		reports.add("MM");reports.add("MM");reports.add("GB");
		strategy = AnalysisUtils.getOppPopStr(reports);
		assertEquals(0.66666667, strategy.get("MM"), 0.00000001);
		assertEquals(0.33333333, strategy.get("GB"), 0.00000001);
		
		reports.clear();
		reports.add("MM");reports.add("GB");reports.add("GB");
		strategy = AnalysisUtils.getOppPopStr(reports);
		assertEquals(0.33333333, strategy.get("MM"), 0.00000001);
		assertEquals(0.66666667, strategy.get("GB"), 0.00000001);
		
		reports.clear();
		reports.add("GB");reports.add("GB");reports.add("GB");
		strategy = AnalysisUtils.getOppPopStr(reports);
		assertEquals(0, strategy.get("MM"), 0.00000001);
		assertEquals(1, strategy.get("GB"), 0.00000001);

		reports.clear();
		reports.add("GB");reports.add("GB");
		strategy = AnalysisUtils.getOppPopStr(reports);
		assertEquals(0, strategy.get("MM"), 0.00000001);
		assertEquals(1, strategy.get("GB"), 0.00000001);

		reports.clear();
		reports.add("MM");reports.add("GB");
		strategy = AnalysisUtils.getOppPopStr(reports);
		assertEquals(0.5, strategy.get("MM"), 0.00000001);
		assertEquals(0.5, strategy.get("GB"), 0.00000001);

		reports.clear();
		reports.add("MM");reports.add("MM");
		strategy = AnalysisUtils.getOppPopStr(reports);
		assertEquals(1, strategy.get("MM"), 0.00000001);
		assertEquals(0, strategy.get("GB"), 0.00000001);

		
	}

}
