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
import java.util.List;
import java.util.Map;
import java.util.Scanner;
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
	// private static Pattern killed = Pattern
	// .compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) killed, because disconnected for ([0-9]+) milliseconds");
	static final Pattern choseReport = Pattern
			.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) chose report ([A-Z]{2})");
	// static final Pattern fakeReport = Pattern
	// .compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) killed, put in fake report ([A-Z]{2})");
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

	static final Pattern timeStamp = Pattern
			.compile("([0-9]{2}):([0-9]{2}).([0-9]{3})");

	static String setId = "vary-payment";
//	static String treatment = "prior2-basic";	
//	static String treatment = "prior2-outputagreement";	
	static String treatment = "prior2-uniquetruthful";	

	static int numStates = 4;
	static String[] stateNames = new String[numStates];
	static int numPlayers = 3;
	static int stateSeqLen = 20;
	static int nonRestarts = 100;

//	static String setId = "mar-13-fixed";
//	static String treatment = "ten-cent-base";
	
	static final String rootDir = "/Users/alicexigao/Dropbox/peer_prediction/data/"
			+ treatment + "/";

	static Experiment expSet;
	static Map<String, ExitSurvey> exitComments;

	static String dbUrl = "jdbc:mysql://localhost/turkserver";
	static String dbClass = "com.mysql.jdbc.Driver";

	static double tol = 0.02;
	
	public static void main(String[] args) throws Exception {

		File dir = new File(rootDir);
		dir.mkdirs();

		parseLog();
		parseExitSurvey();

		System.out.println("treatment: " + treatment);
		System.out.printf("%d non-killed games\n\n", expSet.games.size());

//		playerComments();

		learnHMM();
		printHMMInfo();
		
		// Classify equilibrium convergence
		if (treatment.equals("prior2-basic") 
				|| treatment.equals("prior2-outputagreement")) {
			gameSymmetricConvergenceType();
			gameSymmetricConvergenceTypeRelaxed(3);

		} else if (treatment.equals("prior2-uniquetruthful")) {
			gameAsymmetricConvergenceType();
			gameAsymmetricConvergenceTypeRelaxed(3);
		}
		printGameTypes();
		
//		summarizeParticipationTime();
	}

	private static void learnHMM() throws IOException {

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
		
		Hmm<SigActObservation<CandySignal, CandyReport>> origHmm = null;
		Hmm<SigActObservation<CandySignal, CandyReport>> learntHmm = null;
		double loglk = Double.NEGATIVE_INFINITY;

		for (int i = 0; i < nonRestarts; i++) {

			Hmm<SigActObservation<CandySignal, CandyReport>> origHmmTemp = 
					getInitHmm(numStates);
			
			Hmm<SigActObservation<CandySignal, CandyReport>> learntHmmTemp = 
					bwl.learn(origHmmTemp, seq);		
		
			double loglkTemp = BWToleranceLearner.computeLogLk(learntHmmTemp, seq);

			if (loglkTemp > loglk) {

				origHmm = origHmmTemp;
				learntHmm = learntHmmTemp;
				loglk = loglkTemp;
			}
			
		}

		double[] initPi = new double[numStates];
		for (int i = 0; i < numStates; i++) {
			initPi[i] = learntHmm.getPi(i);
		}
		RealMatrix Aij = new Array2DRowRealMatrix(numStates, numStates);
		for (int i = 0; i < numStates; i++) {
			for (int j = 0; j < numStates; j++) {
				double val = learntHmm.getAij(i, j);
				Aij.setEntry(i, j, val);
			}
		}
		
		// compute steady state prob
		double[] steadyState = calcSteadyStateProb(learntHmm);

		// write HMM to file
		System.out.println("Starting HMM:\n" + origHmm);
		System.out.println("\nResulting HMM:\n" + learntHmm);	
		System.out.println("\nSteady state probabilities: " + Arrays.toString(steadyState));

		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ numStates + "StateHmm.txt"));
		writer.write("\nStarting HMM:\n" + origHmm);
		writer.write("\nResulting HMM:\n" + learntHmm);
		writer.write("\nSteady state probabilities: " + Arrays.toString(steadyState));
		writer.flush();
		writer.close();
		
		// fill the names of the strategies
		for (int i = 0; i < numStates; i++) {
			Opdf <SigActObservation<CandySignal, CandyReport>> opdf = learntHmm.getOpdf(i);
			if (isMMStrategy(opdf)) {
				stateNames[i] = "MM";
			} else if (isGBStrategy(opdf)) {
				stateNames[i] = "GB";
			} else if (isTruthfulStrategy(opdf)) {
				stateNames[i] = "Truthful";
			} else {
				stateNames[i] = "Mixed";
			}
		}
		System.out.println(Arrays.toString(stateNames));
		
		int mmState = 0;
		int gbState = 0;
		int truthfulState = 0;
		for (int i = 0; i < numStates; i++) {
			if (stateNames[i].startsWith("MM"))
				mmState = i;
			else if (stateNames[i].startsWith("GB"))
				gbState = i;
			else if (stateNames[i].startsWith("Truthful"))
				truthfulState = i;
		}

		// calculate most likely state sequence
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
		
		determineHMMType(mmState, gbState, truthfulState);
		
		genPredictedStrategyChangeCode(learntHmm);
	}

	public static void determineHMMType(int mmState, int gbState,
			int truthfulState) throws IOException {
		
		// determine hmm beginning and ending type
		for (Game game : expSet.games) {

			game.hmmTypeArray = new int[20];
			
			// determining type array
			for (int i = 0; i < 20; i++) {
				int[] typeCount = new int[numStates];
				for (String hitId : game.playerHitIds) {
					typeCount[game.stateSeq.get(hitId)[i]]++;
				}

				if (treatment.equals("prior2-uniquetruthful")) {
					if (typeCount[truthfulState] == numStates) {
						game.hmmTypeArray[i] = truthfulState;
					} else if (typeCount[mmState] == 1
							&& typeCount[gbState] == 3) {
						game.hmmTypeArray[i] = 13;
					} else if (typeCount[mmState] == 3
							&& typeCount[gbState] == 1) {
						game.hmmTypeArray[i] = 31;
					} else {
						game.hmmTypeArray[i] = -1;
					}
				} else {
					for (int j = 0; j < typeCount.length; j++) {
						if (typeCount[j] == numStates) {
							game.hmmTypeArray[i] = j;
							break;
						} else if (typeCount[j] != 0) {
							game.hmmTypeArray[i] = -1;
							break;
						}
					}
				}
			}
			System.out.println(Arrays.toString(game.hmmTypeArray));

			
//			// determining beginning type
//			int[] startTypeCount = new int[numStates];
//			for (String hitId : game.playerHitIds) {
//				startTypeCount[game.stateSeq.get(hitId)[0]]++;
//			}
//
//			if (treatment.equals("prior2-uniquetruthful")) {
//
//				if (startTypeCount[truthfulState] == numStates) {
//					game.hmmStartType = truthfulState;
//				} else if (startTypeCount[mmState] == 1
//						&& startTypeCount[gbState] == 3) {
//					game.hmmStartType = 13;
//				} else if (startTypeCount[mmState] == 3
//						&& startTypeCount[gbState] == 1) {
//					game.hmmStartType = 31;
//				} else {
//					game.hmmStartType = -1;
//				}
//
//			} else {
//				for (int i = 0; i < startTypeCount.length; i++) {
//					if (startTypeCount[i] == numStates) {
//						game.hmmStartType = i;
//						break;
//					} else if (startTypeCount[i] != 0) {
//						game.hmmStartType = -1;
//						break;
//					}
//				}
//			}
			
			
//			// determining ending type
//			int[] endTypeCount = new int[numStates];
//			for (String hitId : game.playerHitIds) {
//				int seqLen = game.stateSeq.get(hitId).length;
//				endTypeCount[game.stateSeq.get(hitId)[seqLen - 1]]++;
//			}
//
//			if (treatment.equals("prior2-uniquetruthful")) {
//
//				if (endTypeCount[truthfulState] == numStates) {
//					game.hmmEndType = truthfulState;
//				} else if (endTypeCount[mmState] == 1
//						&& endTypeCount[gbState] == 3) {
//					game.hmmEndType = 13;
//				} else if (endTypeCount[mmState] == 3
//						&& endTypeCount[gbState] == 1) {
//					game.hmmEndType = 31;
//				} else {
//					game.hmmEndType = -1;
//				}
//
//			} else {
//				for (int i = 0; i < endTypeCount.length; i++) {
//					if (endTypeCount[i] == numStates) {
//						game.hmmEndType = i;
//						break;
//					} else if (endTypeCount[i] != 0) {
//						game.hmmEndType = -1;
//						break;
//					}
//				}
//			}
			
		}
		
		int[][] typeCount = new int[20][3];
		for (Game game : expSet.games) {
			for (int i = 0; i < 20; i++) {
				if (game.hmmTypeArray[i] == -1)
					continue;
				else
					typeCount[i][game.hmmTypeArray[i]]++;
			}
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "statesEqDist" + numStates + ".csv"));
		for (int i = 0; i < 20; i++) {
			for (int j = 0; j < 3; j++) {
				writer.write(String.format("%d", typeCount[i][j]));
				System.out.print(typeCount[i][j]);

				if (j < 2) {
					writer.write(",");
					System.out.print(",");
				} else {
					writer.write("\n");
					System.out.print("\n");
				}
			}
		}
		writer.flush();
		writer.close();
		
	}

	private static void printHMMInfo() throws IOException {
		// state sequence
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "stateSeq" + numStates + "states.csv"));
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

		genStateSeqDistMatlabCode();

		// HMM Ending type
		BufferedWriter writer2 = new BufferedWriter(new FileWriter(rootDir
				+ "hmmEndType.csv"));
		for (Game game : expSet.games) {
			writer2.write(game.hmmEndType + "\n");
		}
		writer2.flush();
		writer2.close();

		// HMM Start type
		writer2 = new BufferedWriter(new FileWriter(rootDir
				+ "hmmStartType.csv"));
		for (Game game : expSet.games) {
			writer2.write(game.hmmStartType + "\n");
		}
		writer2.flush();
		writer2.close();

	}

	public static void genStateSeqDistMatlabCode() throws IOException {
		// state sequence distribution
		BufferedWriter writer1 = new BufferedWriter(new FileWriter(rootDir
				+ "stateSeqDist" + numStates + "states.m"));
		int[][] distData = new int[numStates][stateSeqLen];
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				int[] stateSeq = game.stateSeq.get(hitId);
				for (int i = 0; i < stateSeq.length; i++) {
					int state = stateSeq[i];
					distData[state][i]++;
				}
			}
		}
		double[][] dist = new double[numStates][stateSeqLen];
		int total = expSet.games.size() * numPlayers;
		for (int j = 0; j < stateSeqLen; j++) {
			for (int i = 0; i < numStates; i++) {
				dist[i][j] = distData[i][j] * 1.0 / total;
			}
		}

		writer1.write("x = [1:20]\n");

		for (int i = 0; i < numStates; i++) {
			writer1.write("y" + i + " = [");
			for (int j = 0; j < stateSeqLen; j++) {
				writer1.write(dist[i][j] + ",");
			}
			writer1.write("]\n");
		}

		writer1.write("plot(");
		for (int i = 0; i < numStates; i++) {
			writer1.write("x, y" + i);
			if (i < numStates - 1)
				writer1.write(",");
		}
		writer1.write(")\n");

		writer1.write("legend(");
		for (int i = 0; i < numStates; i++) {
			writer1.write("'" + stateNames[i] + "'");
			if (i < numStates - 1)
				writer1.write(",");
		}
		writer1.write(")");

		writer1.flush();
		writer1.close();
	}

	public static void genPredictedStrategyChangeCode(
			Hmm<SigActObservation<CandySignal, CandyReport>> learntHmm)
			throws IOException {
		// Predicted strategy change
		BufferedWriter writer3 = new BufferedWriter(new FileWriter(rootDir
				+ "predictedStrategyChange.m"));
		
		writer3.write("a = [");
		for (int i = 0; i < numStates; i++) {
			for (int j = 0; j < numStates; j++) {
				double aij = learntHmm.getAij(i, j);
				writer3.write(" " + aij);
				if (j < numStates - 1) 
					writer3.write(",");
				else 
					writer3.write(";");
			}
		}
		writer3.write("]\n");
	
		writer3.write("p = [");
		for (int i = 0; i < numStates; i++) {
			writer3.write(learntHmm.getPi(i) + ",");
		}
		writer3.write("]\n");
		
		writer3.write("m = zeros(50,"+numStates+")\n" +
				"m(1,:) = p\n" +
				"for i =2:50\n" +
				"m(i,:) = m(i-1,:)*a\n" +
				"end\n" + 
				"x = 1:50\n" +
				"plot(");
		for (int i = 1; i <= numStates; i++) {
			writer3.write("x, m(:," + i + ")");
			if (i < numStates)
				writer3.write(",");
		}
		writer3.write(")\n");
		
		writer3.write("legend(");
		for (int i = 0; i < numStates; i++) {
			writer3.write("'" + stateNames[i] + "'");
			if (i < numStates - 1)
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
	
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "playerComments.csv"));
		writer.write("gameId,hitId,actions,strategy,otherStrategy,reason,change,comments\n");
	
		for (Game game : expSet.games) {
	
			for (String hitId : game.playerHitIds) {
				List<Pair<String, String>> signalReportPairs = game
						.getSignalReportPairsForPlayer(hitId);
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
									game.bonus.get(hitId),
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
		
		System.out.println(String.format("GB: %d, MM: %d, HO: %d, Unclassified: %d", 
				numGB, numMM, numHO, numUnclassified));
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
		
		System.out.println(String.format("3GB: %d, 3MM: %d, HO: %d, Unclassified: %d", 
				num3GB, num3MM, numHO, numUnclassified));
		
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
	
	private static void printGameTypes() throws IOException {
	
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "gameConvergenceTypes.csv"));
		writer.write("gameId,convergenceType,roundConverged,convergenceTypeRelaxed,roundConvergedRelaxed\n");
		for (Game game : expSet.games) {
			writer.write(String.format("%s,%s,%s,%s,%s\n", game.id,
					game.convergenceType,game.roundConverged,
					game.convergenceTypeRelaxed,game.roundConvergedRelaxed));
		}
		writer.flush();
		writer.close();
	
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
					Round roundInfo = parseRoundLog(roundLog, game);
					game.addRoundInfo(roundInfo);
				}

				game.populateInfo();
				
				// Save exit survey strings
				Map<String, String> exitSurveys = new HashMap<String, String>();
				for (int i = 0; i < game.playerHitIds.length; i++) {
					String hitId = game.playerHitIds[i];
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
					game.bonus.put(hitId, bonus);
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
//				gameInfo.savePaymentRule(matcher.group(5));
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
		}

	}

	/*
	 * private static void mixedStrategyPerWorker() throws IOException {
	 * 
	 * BufferedWriter writerHO = new BufferedWriter(new FileWriter(rootDir +
	 * "worker-honest.csv"));
	 * 
	 * BufferedWriter writerMM = new BufferedWriter(new FileWriter(rootDir +
	 * "worker-alwaysmm.csv"));
	 * 
	 * BufferedWriter writerGB = new BufferedWriter(new FileWriter(rootDir +
	 * "worker-alwaysgb.csv"));
	 * 
	 * BufferedWriter writerOP = new BufferedWriter(new FileWriter(rootDir +
	 * "worker-opposite.csv"));
	 * 
	 * BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir +
	 * "mixedStrategyPerWorker.csv"));
	 * 
	 * writer.write(",MM->MM,GB->MM\n"); int count = 0; for (Game game :
	 * expSet.games) { for (int i = 0; i < game.playerHitIds.length; i++) {
	 * 
	 * String hitId = game.playerHitIds[i]; Strategy strategy =
	 * AnalysisUtils.getStrategyForPlayer(game, hitId);
	 * 
	 * if (strategy.isHonest()) { writerHO.write(String.format("%s\n", hitId));
	 * } else if (strategy.isMM()) { writerMM.write(String.format("%s\n",
	 * hitId)); } else if (strategy.isGB()) {
	 * writerGB.write(String.format("%s\n", hitId)); } else if
	 * (strategy.isOpposite()) { writerOP.write(String.format("%s\n", hitId)); }
	 * 
	 * // System.out.printf("%d, %.2f, %.2f\n", count, //
	 * strategy.getPercent("MM", "MM"), // strategy.getPercent("GB", "GB"));
	 * writer.write(String.format("%d, %.2f, %.2f\n", count,
	 * strategy.getPercent("MM", "MM"), strategy.getPercent("GB", "MM")));
	 * count++; } }
	 * 
	 * writerOP.flush(); writerOP.close();
	 * 
	 * writerGB.flush(); writerGB.close();
	 * 
	 * writerMM.flush(); writerMM.close();
	 * 
	 * writerHO.flush(); writerHO.close();
	 * 
	 * writer.flush(); writer.close();
	 * 
	 * }
	 * 
	 * private static void mixedStrategyPerRound() throws IOException {
	 * BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir +
	 * "mixedStrategyPerRound.csv")); writer.write(",MM->MM,GB->MM\n"); int
	 * numRounds = expSet.games.get(0).numRounds;
	 * 
	 * for (int i = 0; i < numRounds; i++) { Strategy strategy =
	 * AnalysisUtils.getStrategyForRound(expSet, i);
	 * 
	 * writer.write(String.format("%d, %.2f, %.2f\n", i,
	 * strategy.getPercent("MM", "MM"), strategy.getPercent("GB", "MM"))); }
	 * writer.flush(); writer.close(); }
	 * 
	 * private static void mixedStrategyPerRound(List<String> workersToExclude)
	 * throws IOException {
	 * 
	 * BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir +
	 * "mixedStrategyPerRound_excludeWorkers.csv"));
	 * writer.write(",MM->MM,GB->MM\n"); int numRounds =
	 * expSet.games.get(0).numRounds;
	 * 
	 * for (int i = 0; i < numRounds; i++) { Strategy strategy = AnalysisUtils
	 * .getStrategyForRoundExcludeWorkers(expSet, i, workersToExclude);
	 * 
	 * writer.write(String.format("%d, %.2f, %.2f\n", i,
	 * strategy.getPercent("MM", "MM"), strategy.getPercent("GB", "MM"))); }
	 * writer.flush(); writer.close(); }
	 * 
	 * private static void mixedStrategyPerNRounds(int numRounds) throws
	 * IOException { String filename = String.format(rootDir +
	 * "mixedStrategyPer%dRounds.csv", numRounds); BufferedWriter writer = new
	 * BufferedWriter(new FileWriter(filename));
	 * writer.write(",MM->MM,GB->MM\n");
	 * 
	 * int totalNumRounds = expSet.games.get(0).numRounds; for (int i = 0; i <
	 * totalNumRounds; i += numRounds) { List<Pair<String, String>>
	 * signalReportPairs = expSet .getSignalReportPairsForRoundRange(i, i +
	 * numRounds - 1); Strategy strategy = AnalysisUtils.getMixedStrategy(
	 * signalReportPairs, AnalysisUtils.signalList);
	 * 
	 * writer.write(String.format("%d, %.2f, %.2f\n", i,
	 * strategy.getPercent("MM", "MM"), strategy.getPercent("GB", "MM"))); }
	 * writer.flush(); writer.close();
	 * 
	 * }
	 * 
	 * private static void mixedStrategyPerNRounds(List<String> hitIds, int
	 * numRounds) throws IOException { String filename = String.format(rootDir +
	 * "mixedStrategyPer%dRounds_excludeWorkers.csv", numRounds); BufferedWriter
	 * writer = new BufferedWriter(new FileWriter(filename));
	 * writer.write(",MM->MM,GB->MM\n");
	 * 
	 * int totalNumRounds = expSet.games.get(0).numRounds;
	 * 
	 * for (int i = 0; i < totalNumRounds; i += numRounds) { List<Pair<String,
	 * String>> signalReportPairs = expSet
	 * .getSignalReportPairsForRoundRangeExcludeWorkers(i, i + numRounds - 1,
	 * hitIds); Strategy strategy = AnalysisUtils.getMixedStrategy(
	 * signalReportPairs, AnalysisUtils.signalList);
	 * 
	 * writer.write(String.format("%d, %.2f, %.2f\n", i,
	 * strategy.getPercent("MM", "MM"), strategy.getPercent("GB", "MM"))); }
	 * writer.flush(); writer.close(); }
	 */
}
