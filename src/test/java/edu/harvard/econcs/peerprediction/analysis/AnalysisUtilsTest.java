package edu.harvard.econcs.peerprediction.analysis;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AnalysisUtilsTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
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
