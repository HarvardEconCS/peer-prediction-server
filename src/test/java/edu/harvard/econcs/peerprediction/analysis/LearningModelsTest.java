package edu.harvard.econcs.peerprediction.analysis;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class LearningModelsTest {

	@Before
	public void setUp() {
		LogReader.treatment = "prior2-basic";
		LogReader.parseDB();
	}
	
	@Test
	public void tesBoundsCobyla() {
	
		helperBounds("s1", true, "eps", 0.5);
		helperBounds("s1", false, "eps", 0.0);
		
		helperBounds("s3-abs", true, "delta", (1.5 - 0.1) * LogReader.expSet.numRounds);
		helperBounds("s3-abs", false, "delta", 0.0);
		helperBounds("s3-rel", true, "delta", (1.5 / 0.1) * LogReader.expSet.numRounds);
		helperBounds("s3-rel", false, "delta", 1.0);
		
	}

	
	void helperBounds(String model, boolean isUpper, String paramName, double expected) {
		double bound = 0.0;
		if (isUpper)
			bound = LearningModelsCustom.getUBCobyla(model, paramName);
		else
			bound = LearningModelsCustom.getLBCobyla(model, paramName);
		assertEquals(expected, bound, Utils.eps);
		
	}
	
}
