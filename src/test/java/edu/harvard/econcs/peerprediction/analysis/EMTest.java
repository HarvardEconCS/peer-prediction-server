package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import net.andrewmao.misc.Pair;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EMTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		
		Random rnd = new Random();

		double[] signalPriors = new double[] {0.5, 0.5};
		
		Strategy[] strategies = new Strategy[2];
		strategies[0] = new Strategy(0.3, 0.7);
		strategies[1] = new Strategy(0.6, 0.6);
		System.out.printf("strategies are %s\n", Arrays.toString(strategies));
		
		double[] probs = new double [] {0.1, 0.9};
		System.out.printf("probs are %s\n\n", Arrays.toString(probs));
		List<List<Pair<String, String>>> signalReportPairs = 
				new ArrayList<List<Pair<String, String>>>();
		
		int count = 0;
		int total = 30000;
		while (count < total) {

			// Choose a strategy
			double strategyProb = rnd.nextDouble();
			Strategy chosenStrategy = null;
			if (strategyProb < probs[0])
				chosenStrategy = strategies[0];
			else
				chosenStrategy = strategies[1];

			// Get a data point consisting of 3 signal report pairs
			List<Pair<String, String>> dataPoint = new ArrayList<Pair<String, String>>();
			int num = 10;
			int index = 0;			
			while (index < num) {

				double signalProb = rnd.nextDouble();
				String chosenSignal = "";
				if (signalProb < signalPriors[0])
					chosenSignal = "MM";
				else
					chosenSignal = "GB";

				double reportProb = rnd.nextDouble();
				String chosenReport = "";
				if (reportProb < chosenStrategy.getPercent(chosenSignal, "MM"))
					chosenReport = "MM";
				else
					chosenReport = "GB";

				Pair<String, String> pair = new Pair<String, String>(
						chosenSignal, chosenReport);
				dataPoint.add(pair);
				index++;
			}
			
			signalReportPairs.add(dataPoint);
			count++;
		}
		
		AnalysisUtils.em_K = strategies.length;
		AnalysisUtils.runEMAlgorithm(signalReportPairs);
		System.out.printf("strategies: %s\n", Arrays.toString(AnalysisUtils.em_strategies));
		System.out.printf("probs: %s\n", Arrays.toString(AnalysisUtils.em_pi));
		
	}

}
