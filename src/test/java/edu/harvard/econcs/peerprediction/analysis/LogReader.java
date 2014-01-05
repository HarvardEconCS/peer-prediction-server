package edu.harvard.econcs.peerprediction.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.andrewmao.misc.Pair;
import net.andrewmao.models.games.BWToleranceLearner;
import net.andrewmao.models.games.OpdfStrategy;
import net.andrewmao.models.games.SigActObservation;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.Opdf;
import be.ac.ulg.montefiore.run.jahmm.ViterbiCalculator;

public class LogReader {

	static final Pattern experimentStart = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) (.*) (.*) started");
	static final Pattern prior = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Prior is prob=(.*), worlds=(.*)");
	static final Pattern generalInfo = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) General information sent: numPlayers=([0-9]+), "
					+ "numRounds=([0-9]+), playerNames=(.*), paymentRule=(.*), signalList=(.*)");
	static final Pattern experimentRoundStart = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round ([0-9]+) started");
	static final Pattern experimentRoundFinish = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round ([0-9]+) finished");
	static final Pattern experimentFinish = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) (.*) (.*) finished");

	static final Pattern roundStart = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round (\\d+) started");
	static final Pattern gotSignal = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) got signal ([A-Z]{2})");
	static Pattern choseReport = null;
	static final Pattern roundResult = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round result is (.*)");
	static final Pattern roundEnd = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Round ([0-9]+) finished");
	static final Pattern getBonus = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) gets bonus ([0-9.]+)");
	static final Pattern noBonus = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) killed, no bonus");
	static final Pattern chosenWorld = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) Chosen world is (.*)");;
	//	private static Pattern killed = Pattern
	// .compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) killed, because disconnected for ([0-9]+) milliseconds");
	// static final Pattern fakeReport = Pattern
	// .compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) killed, put in fake report ([A-Z]{2})");

	static final Pattern timeStamp = Pattern
			.compile("([0-9]{2}):([0-9]{2}).([0-9]{3})");

	static String dbUrl = "jdbc:mysql://localhost/turkserver";
	static String dbClass = "com.mysql.jdbc.Driver";

//	static String setId = "mar-13-fixed";
//	static String treatment = "ten-cent-base";
	static String setId = "vary-payment";
	static String treatment = "prior2-basic";
//	static String treatment = "prior2-outputagreement";
//	static String treatment = "prior2-uniquetruthful";
//	static String treatment = "prior2-constant";

	static final String rootDir = "/Users/alicexigao/Dropbox/peer_prediction/data/"
			+ treatment + "/";

	static Experiment expSet;
	static Map<String, ExitSurvey> exitComments;
	
	static int numStrategies = -1;
	static int numPlayers = -1;
	static String[] strategyNames = null;

	static int numRounds = 20;
	static int numRestarts = 100;
	
	static double tol = 0.02;
	
	static Hmm<SigActObservation<CandySignal, CandyReport>> origHmm = null;
	static Hmm<SigActObservation<CandySignal, CandyReport>> learntHmm = null;
	
	static int mmState = -1;
	static int gbState = -1;
	static int truthfulState = -1;
	static int mixedState = -1;
	
	static double eps = 0.000000001;
	
	public static void main(String[] args) throws Exception {

		if (treatment.equals("prior2-constant"))
			choseReport = Pattern.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) chose report ([A-Z]{2}) \\(radio: [0-9]\\)");
		else 
			choseReport = Pattern.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) chose report ([A-Z]{2})");
		
		File dir = new File(rootDir);
		dir.mkdirs();

		parseLog();
		parseExitSurvey();

		System.out.println("treatment: " + treatment);
		System.out.printf("non-killed games: %d\n\n", expSet.games.size());

		// Learning analysis
		bestResponse("BR");
		bestResponse("FP");
		
		System.exit(0);
		
		playerComments();

		if (treatment.equals("prior2-basic") || treatment.equals("prior2-outputagreement")) {	
			numPlayers = 3;
			numStrategies = 3;
		} else if (treatment.equals("prior2-uniquetruthful")) {
			numPlayers = 4;
			numStrategies = 4;
		} else if (treatment.equals("prior2-constant")) {
			numPlayers = 1;
			numStrategies = 2;
		}
		strategyNames = new String[numStrategies];

		// HMM analysis
		learnHMM();
		fillStrategyNames();
		calculateMostLikelyStateSeq();
		determineHMMType();
		genStateSeqDistMatlabCode();
		genPredictedStrategyChangeCode();
		
		// Simple method
		if (treatment.equals("prior2-basic") 
				|| treatment.equals("prior2-outputagreement")
				|| treatment.equals("prior2-constant")) {
			gameSymmetricConvergenceType();
//			gameSymmetricConvergenceTypeRelaxed(3);

		} else if (treatment.equals("prior2-uniquetruthful")) {
			gameAsymmetricConvergenceType();
//			gameAsymmetricConvergenceTypeRelaxed(3);
		}
	}

	private static void bestResponse(String type) throws IOException {
		if (type.equals("BR"))
			System.out.println("Best response analysis");
		else if (type.equals("FP"))
			System.out.println("Fictitious play analysis");
		
		if (treatment.equals("prior2-constant")) {
			System.out.println("Skipping best response analysis for constant payment treatment");
			return;
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir 
				+ type + ".csv"));
		writer.write("hitId, actual payoff, simulated payoff, improvement\n");
		
		double diffTotal = 0;
		int count = 0;
		for (Game game : expSet.games) {

//			Map<String, List<String>> reportList = game.reportList;
//			for (String hitId: game.playerHitIds) {
//				List<String> list = reportList.get(hitId);
//				System.out.print(String.format("%s: %s, %.2f\n", 
//						hitId, list.toString(), game.actualPayoff.get(hitId)));
//			}
//			System.out.println();
			
			for (String hitId : game.playerHitIds) {

				double[] myPayoffs = new double[numRounds];
				String[] myReports = new String[numRounds];
				
				for (int i = 0; i < numRounds; i++) {
					
					double[] paymentArray = game.paymentArrayT1N2;
					
					String myReport = null;
					if (i == 0) {
						
						myReport = game.rounds.get(i).getReport(hitId);
						
					} else {
						
						Map<String, Double> oppPopStrategy = null;;
						if (type.equals("BR")){
							oppPopStrategy = game.getOppPopStrPrevRound(i, hitId);		
						} else if (type.equals("FP")) {
							oppPopStrategy = game.getOppPopStrFull(i, hitId);		
						}
						
						if (treatment.equals("prior2-uniquetruthful")) {
							myReport = game.getBestResponseT3(oppPopStrategy);							
						} else {
							myReport = game.getBestResponseT1N2(oppPopStrategy, paymentArray);							
						}
					}
					
					myReports[i] = myReport;
					
					if (treatment.equals("prior2-uniquetruthful")) {
						List<String> refReports = game.getRefReports(hitId, i);
						int numMM = game.getNumMMInRefReports(refReports);						
						myPayoffs[i] = game.getPaymentT3(myReport, numMM);
					} else {
						String refReport = game.getRefReport(hitId, i);
						myPayoffs[i] = game.getPaymentT1N2(myReport, refReport, paymentArray);
					}
				}
				
				double totalPayoff = 0;
				for (int i = 0; i < numRounds; i++) {
					totalPayoff += myPayoffs[i];
				}
				double simulatedAvgPayoff = totalPayoff / numRounds;
				game.simulatedFPPayoff.put(hitId, simulatedAvgPayoff);
				
//				System.out.print(String.format("%s: %s, %.2f\n\n", 
//						hitId, Arrays.toString(myReports), simulatedAvgPayoff));
				
				double actualAvgPayoff = game.actualPayoff.get(hitId);
				
				double diff = simulatedAvgPayoff - actualAvgPayoff;
				diffTotal += diff;
				count++;
				
				// Print out information
				writer.write(String.format("%s, %.2f, %.2f, %.2f\n", 
						hitId, actualAvgPayoff, simulatedAvgPayoff, diff));
//				System.out.print(String.format("%s: ", hitId));
//				System.out.print(String.format("%.2f, %.2f\n", 
//						actualAvgPayoff, simulatedAvgPayoff));
			}
		}

		writer.flush();
		writer.close();

		System.out.println("diff average " + diffTotal * 1.0 / count);
		
		BufferedWriter writerMatlab = new BufferedWriter(new FileWriter(rootDir
				+ type + "PairedTTest.m"));
		writerMatlab.write("actual = [");
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				writerMatlab.write(String.format("%.2f ", game.actualPayoff.get(hitId)));
			}
		}
		writerMatlab.write("];\n");
		
		writerMatlab.write("simulated = [");
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				writerMatlab.write(String.format("%.2f ", game.simulatedFPPayoff.get(hitId)));
			}
		}
		writerMatlab.write("];\n");
		writerMatlab.write("[h,p] = ttest(actual,simulated)\n");
		writerMatlab.write("diff = simulated - actual;\n");
		writerMatlab.write("m = mean(diff);\n" +
				"v = std(diff);\n" +
				"diff2 = (diff - m)/v;\n" +
				"h2=kstest(diff2)\n" +
				"[f,x_values] = ecdf(diff2);\n" + 
				"F = plot(x_values,f);\n" + 
				"set(F,'LineWidth',2);\n" + 
				"hold on;\n" +
				"G = plot(x_values,normcdf(x_values,0,1),'r-');\n" + 
				"set(G,'LineWidth',2);\n");
		writerMatlab.flush();
		writerMatlab.close();
		
	}


	
	private static void fictitiousPlay() throws IOException {
		
		System.out.println("Fictitious play analysis");
		
		if (treatment.equals("prior2-constant")) {
			System.out.println("Skipping fictitious play analysis for constant payment treatment");
			return;
		}
		
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir 
				+ "fictitiousPlay.csv"));
		writer.write("hitId, actual payoff, simulated payoff, improvement\n");
		
		double diffTotal = 0;
		int count = 0;
		for (Game game : expSet.games) {

//			Map<String, List<String>> reportList = game.reportList;
//			for (String hitId: game.playerHitIds) {
//				List<String> list = reportList.get(hitId);
//				System.out.print(String.format("%s: %s, %.2f\n", 
//						hitId, list.toString(), game.bonus.get(hitId)));
//			}
//			System.out.println();
			
			for (String hitId : game.playerHitIds) {

				// Array of other players' HIT ids
				ArrayList<String> otherHitIds = new ArrayList<String>();
				for (String innerHitId: game.playerHitIds) {
					if (innerHitId.equals(hitId))
						continue;
					else
						otherHitIds.add(innerHitId);
				}
				String[] otherHitIdArray = otherHitIds.toArray(new String[]{});
				
				double[] myPayoffs = new double[numRounds];
				String[] myReports = new String[numRounds];
				
				for (int i = 0; i < numRounds; i++) {
					
					Round currRound = game.rounds.get(i);
					double[] paymentArray = game.paymentArrayT1N2;
					
					String myReport;
					if (i == 0) {
						myReport = currRound.getReport(hitId);
						
					} else {

						Map<String, Double> oppPopStrategy = game.getOppPopStrFull(i, hitId);
						
						if (treatment.equals("prior2-uniquetruthful")) {
							myReport = game.getBestResponseT3(oppPopStrategy);							
						} else {
							myReport = game.getBestResponseT1N2(oppPopStrategy, paymentArray);							
						}
					}
					
					myReports[i] = myReport;
					
					if (!treatment.equals("prior2-uniquetruthful")) {
						
						Map<String, Map<String, Object>> roundResult = currRound.roundResult;
						Map<String, Object> myResult = roundResult.get(hitId);
						String refPlayerHitId = (String) myResult.get("refPlayer");
						Map<String, Object> refPlayerResult = roundResult.get(refPlayerHitId);
						String refReport = (String) refPlayerResult.get("report");
						
						myPayoffs[i] = game.getPaymentT1N2(myReport, refReport, paymentArray);
						
					} else {
						
						String[] refReports = new String[otherHitIdArray.length];
						for (int k = 0; k < otherHitIdArray.length; k++) {
							refReports[k] = game.rounds.get(i).getReport(otherHitIdArray[k]);
						}
						int numMM = 0;
						for (int k = 0; k < refReports.length; k++) {
							if (refReports[k].equals("MM"))
								numMM++;
						}
						myPayoffs[i] = game.getPaymentT3(myReport, numMM);
					}
				}
				
				double totalPayoff = 0;
				for (int i = 0; i < numRounds; i++) {
					totalPayoff += myPayoffs[i];
				}
				double simulatedAvgPayoff = totalPayoff / numRounds;
				game.simulatedFPPayoff.put(hitId, simulatedAvgPayoff);
				
//				System.out.print(String.format("%s: %s, %.2f\n\n", hitId, Arrays.toString(playerReports), avgPayoff));
				
				double actualAvgPayoff = game.actualPayoff.get(hitId);
				
				double diff = simulatedAvgPayoff - actualAvgPayoff;
				diffTotal += diff;
				count++;
				
				// Print out information
				writer.write(String.format("%s, %.2f, %.2f\n", 
						hitId, actualAvgPayoff, simulatedAvgPayoff));
//				System.out.print(String.format("%s: ", hitId));
//				System.out.print(String.format("%.2f, %.2f\n", 
//						actualAvgPayoff, simulatedAvgPayoff));
			}
		}

		writer.flush();
		writer.close();

		System.out.println("diff average " + diffTotal * 1.0 / count);
		
		BufferedWriter writerMatlab = new BufferedWriter(new FileWriter(rootDir
				+ "ficPlayPairedTTest.m"));
		writerMatlab.write("actual = [");
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				writerMatlab.write(String.format("%.2f ", game.actualPayoff.get(hitId)));
			}
		}
		writerMatlab.write("];\n");
		
		writerMatlab.write("simulated = [");
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				writerMatlab.write(String.format("%.2f ", game.simulatedFPPayoff.get(hitId)));
			}
		}
		writerMatlab.write("];\n");
		writerMatlab.write("[h,p] = ttest(actual,simulated)\n");
		writerMatlab.write("diff = simulated - actual;\n");
		writerMatlab.write("m = mean(diff);\n" +
				"v = std(diff);\n" +
				"diff2 = (diff - m)/v;\n" +
				"h2=kstest(diff2)\n" +
				"[f,x_values] = ecdf(diff2);\n" + 
				"F = plot(x_values,f);\n" + 
				"set(F,'LineWidth',2);\n" + 
				"hold on;\n" +
				"G = plot(x_values,normcdf(x_values,0,1),'r-');\n" + 
				"set(G,'LineWidth',2);\n");
		writerMatlab.flush();
		writerMatlab.close();
	}

	private static void learnHMM() throws IOException {

		System.out.println("Learning HMM");
		
		// produce list of signal, report pairs
		List<List<SigActObservation<CandySignal, CandyReport>>> seq = 
				new ArrayList<List<SigActObservation<CandySignal, CandyReport>>>();
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				List<SigActObservation<CandySignal, CandyReport>> list = 
						game.signalReportObjList.get(hitId);
				seq.add(list);
			}
		}
		
		BWToleranceLearner bwl = new BWToleranceLearner();
		
		double loglk = Double.NEGATIVE_INFINITY;

		for (int i = 0; i < numRestarts; i++) {

			Hmm<SigActObservation<CandySignal, CandyReport>> origHmmTemp = 
					getInitHmm(numStrategies);
			
			Hmm<SigActObservation<CandySignal, CandyReport>> learntHmmTemp = 
					bwl.learn(origHmmTemp, seq);		
		
			double loglkTemp = BWToleranceLearner.computeLogLk(learntHmmTemp, seq);

			if (loglkTemp > loglk) {

				origHmm = origHmmTemp;
				learntHmm = learntHmmTemp;
				loglk = loglkTemp;
			}
			
		}

		double[] initPi = new double[numStrategies];
		for (int i = 0; i < numStrategies; i++) {
			initPi[i] = learntHmm.getPi(i);
		}
		RealMatrix Aij = new Array2DRowRealMatrix(numStrategies, numStrategies);
		for (int i = 0; i < numStrategies; i++) {
			for (int j = 0; j < numStrategies; j++) {
				double val = learntHmm.getAij(i, j);
				Aij.setEntry(i, j, val);
			}
		}
		
		// compute steady state prob
		double[] steadyState = calcSteadyStateProb(learntHmm);

		// write HMM to console
		System.out.println("Starting HMM:\n" + origHmm);
		System.out.println("\nResulting HMM:\n" + learntHmm);	
		System.out.println("\nSteady state probabilities: " + Arrays.toString(steadyState));

		// write HMM to file
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ numStrategies + "StateHmm.txt"));
		writer.write("\nStarting HMM:\n" + origHmm);
		writer.write("\nResulting HMM:\n" + learntHmm);
		writer.write("\nSteady state probabilities: " + Arrays.toString(steadyState));
		writer.flush();
		writer.close();

	}

	private static void fillStrategyNames() throws IOException {
		
		System.out.println("\nGive strategies names");
		
		// fill the names of the strategies
		for (int i = 0; i < numStrategies; i++) {
			Opdf <SigActObservation<CandySignal, CandyReport>> opdf = learntHmm.getOpdf(i);
			if (isMMStrategy(opdf)) {
				strategyNames[i] = "MM";
			} else if (isGBStrategy(opdf)) {
				strategyNames[i] = "GB";
			} else if (isTruthfulStrategy(opdf)) {
				strategyNames[i] = "Truthful";
			} else {
				strategyNames[i] = "Mixed";
			}
		}
		System.out.println(Arrays.toString(strategyNames));
		

		for (int i = 0; i < numStrategies; i++) {
			if (strategyNames[i].startsWith("MM"))
				mmState = i;
			else if (strategyNames[i].startsWith("GB"))
				gbState = i;
			else if (strategyNames[i].startsWith("Truthful"))
				truthfulState = i;
			else if (strategyNames[i].startsWith("Mixed"))
				mixedState = i;
		}
		System.out.println(String.format("mmState: %d, gbState: %d, truthfulState: %d, mixedStates: %d", 
				mmState, gbState, truthfulState, mixedState));
	}
	
	private static void calculateMostLikelyStateSeq() throws IOException {
		
		// calculate most likely state sequence		
		System.out.println("Calculate most likely state sequence");
		for (Game game : expSet.games) {
			game.stateSeq = new HashMap<String, int[]>();
			
			for (String hitId: game.playerHitIds) {
				List<SigActObservation<CandySignal, CandyReport>> observList = 
						game.signalReportObjList.get(hitId);
				ViterbiCalculator vi = new ViterbiCalculator(observList, learntHmm);
				int[] stateSeq = vi.stateSequence();
				game.stateSeq.put(hitId, stateSeq);
			}	
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "stateSeq" + numStrategies + "States.csv"));
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				int[] stateSeq = game.stateSeq.get(hitId);
				for (int state : stateSeq) {
					writer.write(state + ",");
				}
				writer.write("\n");
			}
			writer.write("\n");
		}
		writer.flush();
		writer.close();
		
	}
	
	
	private static void determineHMMType() throws IOException {
		
		System.out.println("Determining HMM type");
		
		// determine hmm type
		for (Game game : expSet.games) {

			game.hmmTypeArray = new int[numRounds];
			
			// determining type array
			for (int i = 0; i < numRounds; i++) {
				int[] typeCountPerRound = new int[numStrategies];
				for (String hitId : game.playerHitIds) {
					typeCountPerRound[game.stateSeq.get(hitId)[i]]++;
				}

				if (!treatment.equals("prior2-uniquetruthful")) {
					for (int j = 0; j < typeCountPerRound.length; j++) {
						if (typeCountPerRound[j] == numPlayers) {
							game.hmmTypeArray[i] = j;
							break;
						} else if (typeCountPerRound[j] > 0) {
							game.hmmTypeArray[i] = -1;
							break;
						}
					}
				} else {
					//prior2-uniquetruthful
					if (typeCountPerRound[truthfulState] == numStrategies) {
						game.hmmTypeArray[i] = truthfulState;
					} else if (typeCountPerRound[mmState] == 1
							&& typeCountPerRound[gbState] == 3) {
						game.hmmTypeArray[i] = 13;
					} else if (typeCountPerRound[mmState] == 3
							&& typeCountPerRound[gbState] == 1) {
						game.hmmTypeArray[i] = 31;
					} else {
						game.hmmTypeArray[i] = -1;
					}

				}
			}
			System.out.println(Arrays.toString(game.hmmTypeArray));
			
		}
		
		int[][] hmmTypeCount = new int[numRounds][numStrategies];
		for (Game game : expSet.games) {
			for (int i = 0; i < numRounds; i++) {
				if (game.hmmTypeArray[i] == -1)
					continue;
				else
					hmmTypeCount[i][game.hmmTypeArray[i]]++;
			}
		}
		
//		Write to CSV file
		BufferedWriter writerCsv = new BufferedWriter(new FileWriter(rootDir
				+ "stateSeqDist" + numStrategies + "Strategies.csv"));
		for (int j = 0; j < numStrategies; j++) {
			writerCsv.write(String.format("%s,", strategyNames[j]));
		}
		writerCsv.write("\n");
		
		for (int i = 0; i < numRounds; i++) {
			for (int j = 0; j < numStrategies; j++) {
				writerCsv.write(String.format("%d,", hmmTypeCount[i][j]));
//				System.out.print(String.format("%d,", hmmTypeCount[i][j]));
			}
			writerCsv.write("\n");
//			System.out.print("\n");
		}
		writerCsv.flush();
		writerCsv.close();
		
		
		
		BufferedWriter writerMatlab = new BufferedWriter(new FileWriter(rootDir
				+ "hmmType" + numStrategies + "Strategies.m"));
		
		for (int j = 0; j < numStrategies; j++) {
			writerMatlab.write(String.format("%s = [", strategyNames[j]));
			for (int i = 0; i < numRounds; i++) {
				writerMatlab.write(String.format("%d ", hmmTypeCount[i][j]));
			}
			writerMatlab.write("]';\n");
		}
		writerMatlab.write("\n");
		
		writerMatlab.write("figure;\n");
		
		writerMatlab.write(String.format("bar(1:%d, [", numRounds));
		for (int j = 0; j < numStrategies; j++) {
			writerMatlab.write(String.format("%s ", strategyNames[j]));
		}
		writerMatlab.write("], 0.5, 'stack');\n");
		
		writerMatlab.write("xlabel('Round');\n");
		writerMatlab.write("ylabel('Number of games');\n");
		
		writerMatlab.write("legend(");
		for (int j = 0; j < numStrategies; j++) {
			writerMatlab.write(String.format("'%s', ", strategyNames[j]));
		}
		writerMatlab.write("'NorthWest');");
		
		writerMatlab.flush();
		writerMatlab.close();
		
		
	}

	private static void genStateSeqDistMatlabCode() throws IOException {
		// state sequence distribution
		BufferedWriter writer1 = new BufferedWriter(new FileWriter(rootDir
				+ "stateSeqDist" + numStrategies + "States.m"));
		int[][] distData = new int[numStrategies][numRounds];
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				int[] stateSeq = game.stateSeq.get(hitId);
				for (int i = 0; i < stateSeq.length; i++) {
					int state = stateSeq[i];
					distData[state][i]++;
				}
			}
		}
		double[][] dist = new double[numStrategies][numRounds];
		int total = expSet.games.size() * numPlayers;
		for (int j = 0; j < numRounds; j++) {
			for (int i = 0; i < numStrategies; i++) {
				dist[i][j] = distData[i][j] * 1.0 / total;
			}
		}

		writer1.write("x = [1:20]\n");

		for (int i = 0; i < numStrategies; i++) {
			writer1.write("y" + i + " = [");
			for (int j = 0; j < numRounds; j++) {
				writer1.write(dist[i][j] + ",");
			}
			writer1.write("]\n");
		}

		writer1.write("plot(");
		for (int i = 0; i < numStrategies; i++) {
			writer1.write("x, y" + i);
			if (i < numStrategies - 1)
				writer1.write(",");
		}
		writer1.write(")\n");

		writer1.write("legend(");
		for (int i = 0; i < numStrategies; i++) {
			writer1.write("'" + strategyNames[i] + "'");
			if (i < numStrategies - 1)
				writer1.write(",");
		}
		writer1.write(")");

		writer1.flush();
		writer1.close();
	}

	private static void genPredictedStrategyChangeCode()
			throws IOException {
		// Predicted strategy change
		BufferedWriter writer3 = new BufferedWriter(new FileWriter(rootDir
				+ "predictedStrategyChange.m"));
		
		writer3.write("a = [");
		for (int i = 0; i < numStrategies; i++) {
			for (int j = 0; j < numStrategies; j++) {
				double aij = learntHmm.getAij(i, j);
				writer3.write(" " + aij);
				if (j < numStrategies - 1) 
					writer3.write(",");
				else 
					writer3.write(";");
			}
		}
		writer3.write("]\n");
	
		writer3.write("p = [");
		for (int i = 0; i < numStrategies; i++) {
			writer3.write(learntHmm.getPi(i) + ",");
		}
		writer3.write("]\n");
		
		writer3.write("m = zeros(50,"+numStrategies+")\n" +
				"m(1,:) = p\n" +
				"for i =2:50\n" +
				"m(i,:) = m(i-1,:)*a\n" +
				"end\n" + 
				"x = 1:50\n" +
				"plot(");
		for (int i = 1; i <= numStrategies; i++) {
			writer3.write("x, m(:," + i + ")");
			if (i < numStrategies)
				writer3.write(",");
		}
		writer3.write(")\n");
		
		writer3.write("legend(");
		for (int i = 0; i < numStrategies; i++) {
			writer3.write("'" + strategyNames[i] + "'");
			if (i < numStrategies - 1)
				writer3.write(",");
		}
		writer3.write(")");
		
		writer3.flush();
		writer3.close();
	}

	private static boolean isTruthfulStrategy(
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf) {
		SigActObservation<CandySignal, CandyReport> mmMM = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.MM, CandyReport.MM);
		SigActObservation<CandySignal, CandyReport> gbGB = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.GB, CandyReport.GB);
		return opdf.probability(mmMM) > 0.7 && opdf.probability(gbGB) > 0.7; 
	}		
	
	private static boolean isGBStrategy(
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf) {
		SigActObservation<CandySignal, CandyReport> mmGB = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.MM, CandyReport.GB);
		SigActObservation<CandySignal, CandyReport> gbGB = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.GB, CandyReport.GB);
		return opdf.probability(mmGB) > 0.7 && opdf.probability(gbGB) > 0.7; 
	}	
	
	private static boolean isMMStrategy(
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf) {
		SigActObservation<CandySignal, CandyReport> mmMM = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.MM, CandyReport.MM);
		SigActObservation<CandySignal, CandyReport> gbMM = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.GB, CandyReport.MM);
//		System.out.println("Pr(MM,MM) = " + opdf.probability(mmMM) + " Pro(GB, MM) = " + opdf.probability(gbMM));
		return opdf.probability(mmMM) > 0.7 && opdf.probability(gbMM) > 0.7; 
	}

	private static double[] calcSteadyStateProb(
			Hmm<SigActObservation<CandySignal, CandyReport>> learntHmm) {
		int numStates = learntHmm.nbStates();
		RealMatrix Aij = new Array2DRowRealMatrix(numStates, numStates);
		for (int i = 0; i < numStates; i++) {
			for (int j = 0; j < numStates; j++) {
				double val = learntHmm.getAij(i, j);
				Aij.setEntry(i, j, val);
			}
		}
		
		int i = 15;
		while (i > 0) {
			Aij = Aij.multiply(Aij);
			i--;
		}
		return Aij.getRow(0);
	}

	
	
	/**
	 * A random 3-state HMM
	 * @param numStates 
	 * 
	 * @return
	 */
	private static Hmm<SigActObservation<CandySignal, CandyReport>> getInitHmm(int numStates) {
		
		double[] pi = AnalysisUtils.getRandomVec(numStates);

		double[][] a = new double[numStates][numStates];
		for (int i = 0; i < a.length; i++) {
			a[i] = AnalysisUtils.getRandomVec(numStates);
		}

		List<OpdfStrategy<CandySignal, CandyReport>> opdfs = 
				new ArrayList<OpdfStrategy<CandySignal, CandyReport>>();
		
		for (int i = 0; i < numStates; i++) {
			double[][] probs = new double[][] { 
					AnalysisUtils.getRandomTwoVec(),
					AnalysisUtils.getRandomTwoVec() };
			opdfs.add(getOpdf(probs));
		}

		return new Hmm<SigActObservation<CandySignal, CandyReport>>(
				pi, a, opdfs);
	}

	// Use an even prior to fit shit
	static double[] signalPrior = new double[] { 0.5, 0.5 };

	private static OpdfStrategy<CandySignal, CandyReport> getOpdf(
			double[][] probs) {
		return new OpdfStrategy<CandySignal, CandyReport>(CandySignal.class,
				CandyReport.class, signalPrior, probs);
	}
	
	private static void playerComments() throws IOException {
	
		System.out.println("Parsing player comments");
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "playerComments.csv"));
		writer.write("gameId,hitId,actions,bonus,strategy,otherStrategy,reason,change,comments\n");
	
		for (Game game : expSet.games) {
	
			for (String hitId : game.playerHitIds) {
				List<Pair<String, String>> signalReportPairs = game.signalReportPairList.get(hitId);
//						.getSignalReportPairsForPlayer(hitId);
				ExitSurvey survey = exitComments.get(hitId);
	
				if (survey == null)
					writer.write(String.format(
							"%s,%s,\"\"\"%s\"\"\",null,null,null,null,null\n",
							game.id, hitId, signalReportPairs));
				else
					writer.write(String
							.format("%s,%s," +
									"\"\"\"%s\"\"\"," +
									"%.2f," +
									"\"\"\"%s\"\"\",\"\"\"%s\"\"\",\"\"\"%s\"\"\",\"\"\"%s\"\"\",\"\"\"%s\"\"\"\n",
									game.id, hitId, 
									signalReportPairs,
									game.actualPayoff.get(hitId),
									survey.checkedStrategies,
									survey.otherStrategy,
									survey.strategyReason,
									survey.strategyChange, survey.comments));
			}
			writer.write("\n");
		}
	
		writer.flush();
		writer.close();
	
	}



	private static void gameSymmetricConvergenceType() throws IOException {
		
		int numHO = 0;
		int numMM = 0;
		int numGB = 0;
		int numUnclassified = 0;
		int numTotal = expSet.games.size();
		
		for (Game game : expSet.games) {

			game.fillConvergenceType();
			
			if (game.convergenceType.equals("MM"))
				numMM++;
			else if (game.convergenceType.equals("GB"))
				numGB++;
			else if (game.convergenceType.equals("HO"))
				numHO++;
			else 
				numUnclassified++;
		}
		
		System.out.println(String.format(
				"GB: %d (%d%%), MM: %d (%d%%), HO: %d (%d%%), Unclassified: %d(%d%%), Total: %d", 
				numGB, Math.round(numGB * 100.0 / numTotal), 
				numMM, Math.round(numMM * 100.0 / numTotal),
				numHO, Math.round(numHO * 100.0 / numTotal),
				numUnclassified, Math.round(numUnclassified * 100.0 / numTotal),
				numTotal));
	}

	private static void gameAsymmetricConvergenceType() {

		int numHO = 0;
		int num3MM = 0;
		int num3GB = 0;
		int numUnclassified = 0;
		
		for (Game game : expSet.games) {

			game.fillAsymmetricConvergenceType();
			
			if (game.convergenceType.equals("3MM"))
				num3MM++;
			else if (game.convergenceType.equals("3GB"))
				num3GB++;
			else if (game.convergenceType.equals("HO"))
				numHO++;
			else 
				numUnclassified++;
		}
		
		System.out.println(String.format("3GB: %d, 3MM: %d, HO: %d, Unclassified: %d, Total: %d", 
				num3GB, num3MM, numHO, numUnclassified, expSet.games.size()));
		
	}

	private static void gameSymmetricConvergenceTypeRelaxed(int i) throws IOException {

		int numHO = 0;
		int numMM = 0;
		int numGB = 0;
		int numUnclassified = 0;

		for (Game game : expSet.games) {
			game.fillConvergenceTypeRelaxed(i);
			
			if (game.convergenceTypeRelaxed.startsWith("MM"))
				numMM++;
			else if (game.convergenceTypeRelaxed.startsWith("GB"))
				numGB++;
			else if (game.convergenceTypeRelaxed.startsWith("HO"))
				numHO++;
			else 
				numUnclassified++;
		}
		
		System.out.println(String.format("i=%d, GB: %d, MM: %d, HO: %d, Unclassified: %d", 
				i, numGB, numMM, numHO, numUnclassified));

	}

	private static void gameAsymmetricConvergenceTypeRelaxed(int i) throws IOException {
		
		int numHO = 0;
		int num3MM = 0;
		int num3GB = 0;
		int numUnclassified = 0;

		for (Game game : expSet.games) {

			game.fillAsymmetricConvergenceTypeRelaxed(i);
			
			if (game.convergenceTypeRelaxed.startsWith("3MM"))
				num3MM++;
			else if (game.convergenceTypeRelaxed.startsWith("3GB"))
				num3GB++;
			else if (game.convergenceTypeRelaxed.startsWith("HO"))
				numHO++;
			else 
				numUnclassified++;
		}

		System.out.println(String.format("i=%d, 3GB: %d, 3MM: %d, HO: %d, Unclassified: %d", 
				i, num3GB, num3MM, numHO, numUnclassified));
	}

	private static void summarizeParticipationTime() {
		int[] count = new int[24];
		for (Game game : expSet.games) {
			String id = game.id.substring(0, 19);
			DateTimeFormatter format = DateTimeFormat
					.forPattern("yyyy-MM-dd HH.mm.ss");
			DateTime time = format.parseDateTime(id);
			int hour = time.getHourOfDay();
			count[hour]++;
		}

		System.out.println();
		System.out.println("Number of games for every hour of day");
		for (int i = 0; i < count.length; i++) {
			System.out.printf("%d hour: %d games\n", i, count[i]);
		}
	}


	private static void parseLog() {

		System.out.println("Parsing game log");
		
		expSet = new Experiment();
		expSet.setId = setId;

		Connection con = null;

		// get experiment log
		Statement expStmt = null;
		ResultSet expRS = null;

		// get bonus
		Statement bonusStmt = null;
		ResultSet bonusRS = null;
		
		// get round log
		Statement roundStmt = null;
		ResultSet roundRS = null;

		// get exit survey
		Statement exitSurveyStmt = null;
		ResultSet exitSurveyRS = null;

		try {
			Class.forName(dbClass);
			con = DriverManager.getConnection(dbUrl, "root", "");

			String expQuery = "select * from experiment "
					+ "where setId='"
					+ setId
					+ "' and inputdata = '"
					+ treatment
					+ "' "
					+ "and id not in (select distinct experimentId from round where results like '%killed%')";
			expStmt = con.createStatement();
			expRS = expStmt.executeQuery(expQuery);

			while (expRS.next()) {
				String gameId = expRS.getString("id");
				String expLog = expRS.getString("results");

				Game game = parseGameLog(expLog);
				game.id = gameId;

				for (int i = 0; i < game.numRounds; i++) {

					String roundQuery = String.format("select * from round "
							+ "where experimentId = '%s' and roundnum = %d",
							gameId, i + 1);
					roundStmt = con.createStatement();
					roundRS = roundStmt.executeQuery(roundQuery);

					String roundLog = "";
					if (roundRS.next()) {
						roundLog = roundRS.getString("results");
					}
					Round round = parseRoundLog(roundLog, game);
					game.addRound(round);
				}

				game.populateInfo();
				
				// Save exit survey strings
				Map<String, String> exitSurveys = new HashMap<String, String>();
				for (String hitId : game.playerHitIds) {
					String exitSurveyQuery = String.format(
							"select comment from session "
									+ "where experimentId='%s' and hitId='%s'",
							gameId, hitId);
					exitSurveyStmt = con.createStatement();
					exitSurveyRS = exitSurveyStmt.executeQuery(exitSurveyQuery);
					exitSurveyRS.next();

					String exitComments = exitSurveyRS.getString("comment");
					// System.out.printf("%s\n", exitComments);
					exitSurveys.put(hitId, exitComments);
				}
				game.exitSurvey = exitSurveys;

				
				String bonusQuery = String.format("select hitId, bonus from session where experimentId='%s'", gameId);
				bonusStmt = con.createStatement();
				bonusRS = bonusStmt.executeQuery(bonusQuery);
				while (bonusRS.next()) {
					String hitId = bonusRS.getString("hitId");
					double bonus = Double.parseDouble(bonusRS.getString("bonus"));
					game.actualPayoff.put(hitId, bonus);
				}
				
				expSet.addGame(game);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (expRS != null)
					expRS.close();
				if (roundRS != null)
					roundRS.close();
				if (expStmt != null)
					expStmt.close();
				if (con != null)
					con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static Game parseGameLog(String experimentLogString) {
		Game gameObj = new Game();

		Scanner sc = null;
		String currentLine = null;
		int lineIndex = 0;

		try {
			sc = new Scanner(experimentLogString);

			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcher = experimentStart.matcher(currentLine);
			if (matcher.matches()) {
				// System.out.println("Experiment start message found");
				// System.out.printf("%s, %s, %s\n\n", matcher.group(1),
				// matcher.group(2), matcher.group(3));
			} else
				throw new ParseException(
						"Did not find experiment start message", lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;
			matcher = prior.matcher(currentLine);
			if (matcher.matches()) {
				// System.out.println("Prior message found");
				// System.out.printf("%s, %s, %s\n\n", matcher.group(1),
				// matcher.group(2), matcher.group(3));
				gameObj.savePriorProb(matcher.group(2));
				gameObj.savePriorWorlds(matcher.group(3));
			} else
				throw new ParseException("Did not find prior message",
						lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;
			matcher = generalInfo.matcher(currentLine);
			if (matcher.matches()) {
				// System.out.println("General information message found");
				// System.out.printf("%s, %s, %s, %s, %s, %s\n\n",
				// matcher.group(1), matcher.group(2), matcher.group(3),
				// matcher.group(4), matcher.group(5), matcher.group(6));
				gameObj.numPlayers = Integer.parseInt(matcher.group(2));
				gameObj.numRounds = Integer.parseInt(matcher.group(3));
				gameObj.savePlayerHitIds(matcher.group(4));
				gameObj.savePaymentRule(matcher.group(5));
				// gameInfo.saveSignalList(matcher.group(6));
			} else
				throw new ParseException(
						"Did not find general information message", lineIndex);

			int numRounds = gameObj.numRounds;

			for (int i = 0; i < numRounds; i++) {
				currentLine = sc.nextLine();
				lineIndex++;
				matcher = experimentRoundStart.matcher(currentLine);
				if (matcher.matches()) {
					// System.out.println("Experiment round start message found");
					// System.out.printf("%s, %s\n\n", matcher.group(1),
					// matcher.group(2));
				} else
					throw new ParseException(
							"Did not find experiment round start message",
							lineIndex);

				currentLine = sc.nextLine();
				lineIndex++;
				matcher = experimentRoundFinish.matcher(currentLine);
				if (matcher.matches()) {
					// System.out.println("Experiment round finish message found");
					// System.out.printf("%s, %s\n\n", matcher.group(1),
					// matcher.group(2));
				} else
					throw new ParseException(
							"Did not find experiment round finish message",
							lineIndex);
			}

			currentLine = sc.nextLine();
			lineIndex++;
			matcher = experimentFinish.matcher(currentLine);
			if (matcher.matches()) {
				// System.out.println("Experiment finish message found");
				// System.out.printf("%s, %s, %s\n\n", matcher.group(1),
				// matcher.group(2), matcher.group(3));
			} else
				throw new ParseException(
						"Did not find experiment finish message", lineIndex);

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (sc != null)
				sc.close();
		}
		
		return gameObj;
	}

	private static Round parseRoundLog(String roundResults, Game game) {
		Round roundInfo = new Round();

		Scanner sc = null;
		String currentLine = null;
		int lineIndex = 0;
		try {
			sc = new Scanner(roundResults);

			// parse round start
			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcher = roundStart.matcher(currentLine);
			if (matcher.matches()) {
				// System.out.println("Round start message found");
				// System.out.printf("%s, %s\n\n", matcher.group(1),
				// matcher.group(2));
				roundInfo.roundNum = Integer.parseInt(matcher.group(2));
			} else
				throw new ParseException("Did not find round start message",
						lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcherChosenWorld = chosenWorld.matcher(currentLine);
			if (matcherChosenWorld.matches()) {
				// System.out.println("Chosen world message found");
				// System.out.printf("%s, %s\n\n", matcherChosenWorld.group(1),
				// matcherChosenWorld.group(2));
				roundInfo.saveChosenWorld(matcherChosenWorld.group(2));
			} else
				throw new ParseException("Did not find chosen world message",
						lineIndex);

			int numPlayers = game.numPlayers;
			int count = 0;
			while (count < numPlayers) {
				currentLine = sc.nextLine();
				lineIndex++;

				Matcher matcherSignal = gotSignal.matcher(currentLine);
				if (matcherSignal.matches()) {
					// System.out.println("Signal message found");
					// System.out.printf("%s, %s, %s, %s\n\n",
					// matcherSignal.group(1), matcherSignal.group(2),
					// matcherSignal.group(3), matcherSignal.group(4));
					roundInfo.saveSignal(matcherSignal.group(2),
							matcherSignal.group(3), matcherSignal.group(4),
							matcherSignal.group(1));
				} else {
					throw new ParseException(
							"Expected a signal message but did not find one",
							lineIndex);
				}
				count++;
			}

			// parse get signal and choose report messages
			int limit = numPlayers;
			count = 0;
			// boolean killedFound = false;
			while (count < limit) {
				currentLine = sc.nextLine();
				lineIndex++;

				// Matcher matcherKilledMsg = killed .matcher(currentLine);
				Matcher matcherReport = choseReport.matcher(currentLine);
				// Matcher matcherFakeReport = fakeReport.matcher(currentLine);

				// if (matcherKilledMsg.matches()) {
				// // killed message found, there are 3 messages in this section
				// if (killedFound == false) {
				// killedFound = true;
				// limit++;
				// }
				// // write into game, round num, worker id, disconnected time
				//
				// } else
				if (matcherReport.matches()) {

					// System.out.println("Real report message found");
					// System.out.printf("%s, %s, %s, %s\n\n",
					// matcherReport.group(1), matcherReport.group(2),
					// matcherReport.group(3), matcherReport.group(4));
					roundInfo.saveReport(matcherReport.group(2),
							matcherReport.group(3), matcherReport.group(4),
							matcherReport.group(1));

				}
				// else if (matcherFakeReport.matches()) {
				// // fake report found, there are 3 messages in this section
				//
				// // System.out.println("Fake report message found");
				// // System.out.printf("%s, %s, %s, %s\n\n",
				// // matcherFakeReport.group(1),
				// // matcherFakeReport.group(2),
				// // matcherFakeReport.group(3),
				// // matcherFakeReport.group(4));
				//
				// roundInfo.saveReport(matcherFakeReport.group(2),
				// matcherFakeReport.group(3), matcherFakeReport.group(4),
				// matcherFakeReport.group(1));
				// }
				else {
					throw new ParseException(
							"Did not find real or fake report messages.",
							lineIndex);
				}
				count++;
			}

			// parse round result
			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcherResult = roundResult.matcher(currentLine);
			if (matcherResult.matches()) {
				// System.out.println("Round result message found");
				// System.out.printf("%s, %s\n\n", matcherResult.group(1),
				// matcherResult.group(2));
				roundInfo.saveResult(matcherResult.group(2));
			} else
				throw new ParseException("Did not find round result message",
						lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;

			while (true) {
				Matcher matcherBonus = getBonus.matcher(currentLine);
				Matcher matcherNoBonus = noBonus.matcher(currentLine);
				if (matcherBonus.matches() || matcherNoBonus.matches()) {
					// System.out.println("Get bonus message found");
					// System.out.printf("%s, %s, %s, %s\n\n",
					// matcherBonus.group(1), matcherBonus.group(2),
					// matcherBonus.group(3), matcherBonus.group(4));

					currentLine = sc.nextLine();
					lineIndex++;
				} else {
					break;
				}
			}

			// parse round end
			Matcher matcherEnd = roundEnd.matcher(currentLine);
			if (matcherEnd.matches()) {
				// System.out.println("Round end message found");
				// System.out.printf("%s, %s\n\n", matcherEnd.group(1),
				// matcherEnd.group(2));
				String endTimeString = matcherEnd.group(1);
				roundInfo.endTimeString = endTimeString;
				int min = Integer.parseInt(endTimeString.substring(0, 2));
				int sec = Integer.parseInt(endTimeString.substring(3, 5));
				int millisec = Integer.parseInt(endTimeString.substring(6, 9));
				int totalMillisec = min * 60 * 1000 + sec * 1000 + millisec;
				roundInfo.durationInMS = totalMillisec;
			} else
				throw new ParseException("Did not find round end message",
						lineIndex);

			return roundInfo;

		} catch (ParseException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (sc != null)
				sc.close();
		}
		return null;
	}

	private static void parseExitSurvey() {

		System.out.println("Parsing exit survey");
		
		Connection con = null;
		exitComments = new HashMap<String, ExitSurvey>();

		try {
			Class.forName(dbClass);
			con = DriverManager.getConnection(dbUrl, "root", "");

			String query = String.format("select hitId, comment from session "
					+ "where comment is not null and setId='%s'", setId);
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				String hitId = rs.getString("hitId");
				String comment = rs.getString("comment");

				ExitSurvey survey = new ExitSurvey(comment);
				exitComments.put(hitId, survey);
			}
			rs.close();
			stmt.close();
			con.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
