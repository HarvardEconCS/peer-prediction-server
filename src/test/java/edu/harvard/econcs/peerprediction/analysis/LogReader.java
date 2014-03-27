package edu.harvard.econcs.peerprediction.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import java.util.Collections;
import java.util.Comparator;
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
	static final Pattern priorPattern = Pattern
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
//	static String treatment = "prior2-basic";
//	static String treatment = "prior2-outputagreement";
//  static String treatment = "prior2-uniquetruthful";
	static String treatment = "prior2-symmlowpay";
//	static String treatment = "prior2-constant";

	static final String rootDir = "/Users/alicexigao/Dropbox/peer_prediction/data/"
			+ treatment + "/";

	static Experiment expSet;
	static Map<String, ExitSurvey> exitComments;
	
	static int numStrategies = -1;
	static int numPlayers = -1;
	static String[] strategyNames = null;

	static int numRounds = 20;
	static int numRestarts = 10;
	
	static double tol = 0.02;
	
	static Hmm<SigActObservation<CandySignal, CandyReport>> origHmm = null;
	static Hmm<SigActObservation<CandySignal, CandyReport>> learntHmm = null;
	
	static int mmState = -1;
	static int gbState = -1;
	static int truthfulState = -1;
	static int mixedState = -1;
	static int mixed2State = -1;
	
	static double eps = 0.000000001;

	static double[] priorProbs = null;
	static List<Map<String, Double>> prior = null;
	

	public static void main(String[] args) throws Exception {

		File dir = new File(rootDir);
		dir.mkdirs();

		// Parsing game log and exit survey, and print info
		parseLog();
		parseExitSurvey();
		playerComments();
		printInfo();
		
		// HMM analysis		
		setNumberOfPlayersStrategiesRounds();
		learnHMM();
		setStrategyNames();
		getStateSeq();
		getHMMType();
		writeStrategyDistributionMatlabCode();
		writeStrategyHeatMap();
		genPredictedStrategyChangeCode();

//		strategyChangeT1();
		
		graphLogLikelihood();

//		mixedStrategyPayoff("T3");
		
		// Analyze payoffs of different strategies
//		strategyPayoffAnalysis("T3");
//		strategyPayoffAnalysis("T5");
		if (treatment.equals("prior2-uniquetruthful"))
			strategyPayoffAnalysis("T3");
		else if (treatment.equals("prior2-symmlowpay"))
			strategyPayoffAnalysis("T5");
//		AnalysisUtils.getSymmMixedStrEq3Players("T1");
//		AnalysisUtils.getSymmMixedStrEq4Players("T3");
//		AnalysisUtils.getSymmMixedStrEq4Players("T5");
//		System.exit(0);

		
		// Simple convergence analysis
		if (treatment.equals("prior2-basic") 
				|| treatment.equals("prior2-outputagreement")
				|| treatment.equals("prior2-symmlowpay")) {
			gameSymmetricConvergenceType();
//			gameSymmetricConvergenceTypeRelaxed(3);
		} else if (treatment.equals("prior2-uniquetruthful")) {
			gameConvergenceTypeT3();
//			gameAsymmetricConvergenceTypeRelaxed(3);
		}

		// Learning analysis
//		learningAnalysis("BR");
//		learningAnalysis("FP");
//		noRegretLearningAnalysis();

	}

	private static void mixedStrategyPayoff(String rule) {
		if (!treatment.equals("prior2-uniquetruthful"))
			return;
		
//		double truthfulPayoffT3 = AnalysisUtils.getTruthfulPayoff(rule, priorProbs, prior);
//		System.out.printf("Payoff at truthful equilibrium: %.6f\n", truthfulPayoffT3);	
//
//		double mixedPayoffT3 = AnalysisUtils.getMixedPayoff(rule, priorProbs, prior, strategyMMGivenMM, strategyMMGivenGB);
		
		
	}

	public static void printInfo() {
		System.out.println("\ntreatment: " + treatment);
		System.out.printf("non-killed games: %d\n\n", expSet.games.size());
		
		priorProbs = expSet.games.get(0).priorProbs;
		prior = expSet.games.get(0).worlds;
		System.out.printf("Prior probabilities: %s, ", Arrays.toString(priorProbs));
		System.out.printf("%s\n", prior.toString());
	}

	public static void setNumberOfPlayersStrategiesRounds() {
		if (treatment.equals("prior2-basic") 
				|| treatment.equals("prior2-outputagreement")) {	
			numPlayers = 3;
			numStrategies = 4;
			numRounds = 20;
		} else if (treatment.equals("prior2-uniquetruthful")) {
			numPlayers = 4;
			numStrategies = 4;
			numRounds = 20;
		} else if (treatment.equals("prior2-symmlowpay")) {
			numPlayers = 4;
			numStrategies = 4;
			numRounds = 30;
		} else if (treatment.equals("prior2-constant")) {
			numPlayers = 1;
			numStrategies = 4;
			numRounds = 20;
		}
		strategyNames = new String[numStrategies];
		System.out.printf("numPlayers: %d\n", numPlayers);
		System.out.printf("numStrategies: %d\n", numStrategies);
		System.out.printf("numRounds: %d\n", numRounds);
	}

	private static void strategyChangeT1() {
		if (!treatment.equals("prior2-basic"))
			return;
		
		int numMMEq = 0;
		int numMixedToMM = 0;
		int numTruthfulToMM = 0;
		for (Game game : expSet.games) {
			
			boolean convergedToMM = true;
			for (String hitId : game.playerHitIds) {
				if (game.stateSeq.get(hitId)[numRounds - 1] != mmState) {
					convergedToMM = false;
					break;
				}
			}
			
			if (!convergedToMM)
				continue;
			
			numMMEq++;
			for (String hitId : game.playerHitIds) {
				int[] stateSeq = game.stateSeq.get(hitId);
				if (hasMixedAndMM(stateSeq))
					numMixedToMM++;
				else if (hasTruthfulAndMM(stateSeq))
					numTruthfulToMM++;
			}
		}
		System.out.printf("MM eq : %d, mixed to MM: %d, truthful to MM: %d\n", 
				numMMEq, numMixedToMM, numTruthfulToMM);
		
	}

	private static boolean hasTruthfulAndMM(int[] stateSeq) {
		boolean hasTruthful = false;
		for (int i = 0; i < stateSeq.length; i++) {
			if (stateSeq[i] == truthfulState) {
				hasTruthful = true;
			} else if (stateSeq[i] == mmState) {
			} else { 
				return false;
			}
		}
		return hasTruthful;
	}

	private static boolean hasMixedAndMM(int[] stateSeq) {
		boolean hasMixed = false;
		for (int i = 0; i < stateSeq.length; i++) {
			if (stateSeq[i] == mixedState) {
				hasMixed = true;
			} else if (stateSeq[i] == mmState) {
			} else { 
				return false;
			}
		}
		return hasMixed;
	}

	private static void graphLogLikelihood() throws IOException {
		List<List<SigActObservation<CandySignal, CandyReport>>> seq = getActObsSequence();
		double loglk;


		BufferedWriter writerMatlab = new BufferedWriter(new FileWriter(rootDir
				+ "logLikelihood.m"));
		System.out.println("Graph log likelihood");

		if (treatment.equals("prior2-basic"))
			writerMatlab.write("treatment1loglk = [");
		else if (treatment.equals("prior2-outputagreement"))
			writerMatlab.write("treatment2loglk = [");
		else if (treatment.equals("prior2-uniquetruthful"))
			writerMatlab.write("treatment3loglk = [");
		else if (treatment.equals("prior2-symmlowpay"))
			writerMatlab.write("treatment4loglk = [");
		else if (treatment.equals("prior2-constant"))
			writerMatlab.write("treatment5loglk = [");
		
		for (int numStates = 2; numStates <= 6; numStates++) {
			
			Hmm<SigActObservation<CandySignal, CandyReport>> savedHmm = createHMMFromFile(numStates);
			loglk = BWToleranceLearner.computeLogLk(savedHmm, seq);
			
			writerMatlab.write(String.format("%.6f ", loglk));
		}
		writerMatlab.write("];\n");

		
		double bic;
		System.out.println("Graph Bayesian information criterion");
		
		if (treatment.equals("prior2-basic"))
			writerMatlab.write("treatment1bic = [");
		else if (treatment.equals("prior2-outputagreement"))
			writerMatlab.write("treatment2bic = [");
		else if (treatment.equals("prior2-uniquetruthful"))
			writerMatlab.write("treatment3bic = [");
		else if (treatment.equals("prior2-symmlowpay"))
			writerMatlab.write("treatment4bic = [");
		else if (treatment.equals("prior2-constant"))
			writerMatlab.write("treatment5bic = [");
		
		for (int numStates = 2; numStates <= 6; numStates++) {
			
			Hmm<SigActObservation<CandySignal, CandyReport>> savedHmm = createHMMFromFile(numStates);
			loglk = BWToleranceLearner.computeLogLk(savedHmm, seq);
			
			int numParams = (numStates * numStates + 2 * numStates - 1);
			int numData = expSet.games.size() * numPlayers  * numRounds;
			bic = -2 * loglk + numParams * Math.log(numData);
			
			writerMatlab.write(String.format("%.6f ", bic));
		}
		writerMatlab.write("];\n");
		
		
//		writerMatlab.write("x = 2:6;\n");
//		writerMatlab.write("figure;\n");
//		writerMatlab.write("plot(x,loglk)\n");
//		writerMatlab.write("xlabel('Number of strategies');\n");
//		writerMatlab.write("ylabel('Log likelihood');\n");
//		writerMatlab.write("set(gca,'XTick',2:6);\n");
		writerMatlab.flush();
		writerMatlab.close();
		
	}

//	private static void strategyPayoffAnalysis(String rule) {
//		
//		System.out.println("\n" + "Strategy payoff analysis -- " + rule);
//		
//		double truthfulPayoffT3 = AnalysisUtils.getTruthfulPayoff(rule, priorProbs, prior);
//		System.out.printf("Payoff at truthful equilibrium: %.6f\n", truthfulPayoffT3);	
//		
//		double mixedPayoff = 0.0;
//		double[] strategy = new double[2];
//		
//		double unit = 0.1;
//		int numUnit = Math.round((int)(1 / unit));
//		
//		for (int i = 0; i <= numUnit; i++) {
//			for (int j = 0; j <= numUnit; j++) {
//				double strategyMMGivenMM = unit * i;
//				double strategyMMGivenGB = unit * j;
//				double mixedPayoffT5 = AnalysisUtils.getMixedPayoff(rule,
//						priorProbs, prior, strategyMMGivenMM, strategyMMGivenGB);
////				System.out.printf("mixed strategy is (%.2f, %.2f), payoff is %.6f\n",
////						strategyMMGivenMM, strategyMMGivenGB, mixedPayoffT5);
//				if (mixedPayoffT5 > mixedPayoff) {
//					mixedPayoff = mixedPayoffT5;
//					strategy[0] = strategyMMGivenMM;
//					strategy[1] = strategyMMGivenGB;
//				}
//			}
//		}
//		System.out.printf("best mixed strategy; (%.2f, %.2f), best payoff; %.6f", 
//				strategy[0], strategy[1], mixedPayoff);
//	}
	
	private static void strategyPayoffAnalysis(String rule) {

		System.out.println("\n" + "Strategy payoff analysis");
		
		double truthfulPayoffT3 = AnalysisUtils.getTruthfulPayoff(rule, priorProbs, prior);
		System.out.printf("Payoff at truthful equilibrium: %.6f\n", truthfulPayoffT3);
		
		// Payoff of mixed strategies
		if (mixedState == -1) {
			System.out.printf("HMM did not learn a mixed strategy.  Do not analyze its payoff");
		} else {
			Opdf<SigActObservation<CandySignal, CandyReport>> opdfMixed = learntHmm.getOpdf(mixedState);
			System.out.printf("Mixed strategy: %s", opdfMixed.toString());
			double probMMGivenMM = opdfMixed
					.probability(new SigActObservation<CandySignal, CandyReport>(
							CandySignal.MM, CandyReport.MM));
			double probMMGivenGB = opdfMixed
					.probability(new SigActObservation<CandySignal, CandyReport>(
							CandySignal.GB, CandyReport.MM));
			double mixedPayoffT3 = AnalysisUtils.getMixedPayoff(rule, 
					priorProbs, prior, probMMGivenMM, probMMGivenGB);
			System.out.printf("Payoff with symmetric mixed strategies:  %.6f\n", mixedPayoffT3);
			
			// Average of payoffs for players using mixed strategy by the end
			int count = 0;
			double totalPayoff = 0.0;
			for (Game game : expSet.games) {
				for (String hitId : game.playerHitIds) {
					if (game.stateSeq.get(hitId)[numRounds - 1] == mixedState) {
						count++;
						totalPayoff += game.actualPayoff.get(hitId);
					}
				}
			}
			double avgPayoff = totalPayoff / count;
			System.out.printf("Average of payoffs for mixed strategy:  %.6f\n", avgPayoff);
		}
		
		if (mixed2State == -1) {
			System.out.printf("HMM did not learn a mixed 2 strategy.  Do not analyze its payoff");
		} else {
			Opdf<SigActObservation<CandySignal, CandyReport>> opdfMixed = learntHmm.getOpdf(mixed2State);
			System.out.printf("Mixed strategy: %s", opdfMixed.toString());
			double probMMGivenMM = opdfMixed
					.probability(new SigActObservation<CandySignal, CandyReport>(
							CandySignal.MM, CandyReport.MM));
			double probMMGivenGB = opdfMixed
					.probability(new SigActObservation<CandySignal, CandyReport>(
							CandySignal.GB, CandyReport.MM));
			double mixedPayoffT3 = AnalysisUtils.getMixedPayoff(rule, 
					priorProbs, prior, probMMGivenMM, probMMGivenGB);
			System.out.printf("Payoff with symmetric mixed strategies:  %.6f\n", mixedPayoffT3);
			
			
			// Average of payoffs for players using mixed strategy by the end
			int count = 0;
			double totalPayoff = 0.0;
			for (Game game : expSet.games) {
				for (String hitId : game.playerHitIds) {
					if (game.stateSeq.get(hitId)[numRounds - 1] == mixed2State) {
						count++;
						totalPayoff += game.actualPayoff.get(hitId);
					}
				}
			}
			double avgPayoff = totalPayoff / count;
			System.out.printf("Average of payoffs for mixed 2 strategy:  %.6f\n",
					avgPayoff);
		}
		

		
		
	}
	
	

	private static void noRegretLearningAnalysis() {
		
		List<Map<String, Map<String, Double>>> experts = 
				new ArrayList<Map<String, Map<String, Double>>>();
		double unit = 0.25;
		int numUnit = Math.round((int) (1 / unit));
		for (int i = 0; i <= numUnit; i++) {
			for (int j = 0; j <= numUnit; j++) {
				double probMMGivenMM = i * unit;
				double probMMGivenGB = j * unit;

				Map<String, Map<String, Double>> strategy = 
						new HashMap<String, Map<String, Double>>();

				Map<String, Double> mmStrategy = new HashMap<String, Double>();
				mmStrategy.put("MM", probMMGivenMM);
				mmStrategy.put("GB", 1 - probMMGivenMM);
				Map<String, Double> gbStrategy = new HashMap<String, Double>();
				gbStrategy.put("MM", probMMGivenGB);
				gbStrategy.put("GB", 1 - probMMGivenGB);

				strategy.put("MM", mmStrategy);
				strategy.put("GB", mmStrategy);

				experts.add(strategy);
			}
		}
		System.out.printf("number of experts %d\n", experts.size());
		
		// Put in initial weights
		List<List<Double>> weights = new ArrayList<List<Double>>();
		List<Double> initWeights = new ArrayList<Double>();
		for (int i = 0; i < experts.size(); i++) {
			initWeights.add(1.0);
		}
		weights.add(initWeights);
		
		for (Game game : expSet.games) {
//		Game game = expSet.games.get(10);
		
			for (String hitId : game.playerHitIds) {
//			String hitId = game.playerHitIds[0];
		
			System.out.printf("actual payoff %.2f\n", game.actualPayoff.get(hitId));
			
				for (int i = 0; i < numRounds; i++) {
					
					// Calculate payoffs
					List<Double> expertPayoffs = new ArrayList<Double>();
					for (int j = 0; j < experts.size(); j++) {
						
						if (treatment.equals("prior2-uniquetruthful")) {
							double currPayoff = getPayoffT3(hitId, game, i, experts.get(j));
							expertPayoffs.add(currPayoff);
						} else {
							
						}
					}
					
					// Calculate player payoff
					double playerPayoff = 0.0;
					double totalWeight = 0.0;
					for (int j = 0; j < experts.size(); j++) {
						totalWeight += weights.get(i).get(j);
					}
					for (int j = 0;  j < experts.size(); j++) {
						playerPayoff += weights.get(i).get(j) / totalWeight * expertPayoffs.get(j);
					}
					System.out.printf("round %s: my payoff %.2f\n", i, playerPayoff);
					
					// Update weights
					List<Double> currWeights = new ArrayList<Double>();
					for (int j = 0; j < experts.size(); j++) {
						double weight = weights.get(i).get(j);
						double newWeight = weight * (1 + 0.1 * expertPayoffs.get(j));
						currWeights.add(newWeight);
					}
					weights.add(currWeights);
//					System.out.printf("round %s: %s\n", i, currWeights.toString());
					
				}
				
			}
		}
	}
	
	private static double getPayoffT3(String hitId, Game game, int i,
			Map<String, Map<String, Double>> expert) {
		
		Round currRound = game.rounds.get(i);
		String signal = currRound.getSignal(hitId);
		Map<String, Double> strategyForSignal = expert.get(signal);
		List<String> otherReports = game.getOtherReportList(currRound, hitId);

		int numMMInOtherReports = game.getNumMMInRefReports(otherReports);

		return strategyForSignal.get("MM") * AnalysisUtils.getPaymentT3("MM", numMMInOtherReports)
				+ strategyForSignal.get("GB") * AnalysisUtils.getPaymentT3("GB", numMMInOtherReports);
	}

	private static double getPayoffT3(Map<String, Double> strategyForSignal,
			int numMMInOtherReports) {
		return strategyForSignal.get("MM") * AnalysisUtils.getPaymentT3("MM", numMMInOtherReports)
				+ strategyForSignal.get("GB") * AnalysisUtils.getPaymentT3("GB", numMMInOtherReports);
	}

	private static void learningAnalysis(String type) throws IOException {
		if (type.equals("BR"))
			System.out.println("\n" + "Best response analysis");
		else if (type.equals("FP"))
			System.out.println("\n" + "Fictitious play analysis");
		else if (type.equals("NR")) {
			System.out.println("No regret learning analysis -- not implemented yet");
			return;
		}
		
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
						
						if (treatment.equals("prior2-uniquetruthful")
								|| treatment.equals("prior2-symmlowpay")) {
							myReport = game.getBestResponseT3(oppPopStrategy);							
						} else {
							myReport = game.getBestResponseT1N2(oppPopStrategy, paymentArray);							
						}
					}
					
					myReports[i] = myReport;
					
					if (treatment.equals("prior2-uniquetruthful")
							|| treatment.equals("prior2-symmlowpay")) {
						List<String> refReports = game.getRefReports(hitId, i);
						int numMM = game.getNumMMInRefReports(refReports);						
						myPayoffs[i] = AnalysisUtils.getPaymentT3(myReport, numMM);
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
	
	private static void learnHMM() throws IOException {

		System.out.println("\n\nLearning HMM");
		
		// produce list of signal, report pairs
		List<List<SigActObservation<CandySignal, CandyReport>>> seq = getActObsSequence();
		
		BWToleranceLearner bwl = new BWToleranceLearner();
	
		double loglk = Double.NEGATIVE_INFINITY;
		File hmmFile = new File(String.format("%slearntHMM%dstrategies.txt", rootDir, numStrategies));
		if (hmmFile.exists()) {
			learntHmm = createHMMFromFile(numStrategies);
			loglk = BWToleranceLearner.computeLogLk(learntHmm, seq);
			System.out.printf("Starting loglikelihood : %.5f\n", loglk);			
		}

		for (int i = 0; i < numRestarts; i++) {

			Hmm<SigActObservation<CandySignal, CandyReport>> origHmmTemp = 
					getInitHmm(numStrategies);
			
			Hmm<SigActObservation<CandySignal, CandyReport>> learntHmmTemp = 
					bwl.learn(origHmmTemp, seq);		
		
			double loglkTemp = BWToleranceLearner.computeLogLk(learntHmmTemp, seq);

//			Investigate robustness
//			if (loglkTemp > -2000) {
//				System.out.println("\nGood HMM:\n" + learntHmmTemp);	
//			}
			
			if (loglkTemp > loglk) {

				origHmm = origHmmTemp;
				learntHmm = learntHmmTemp;
				loglk = loglkTemp;
				
				System.out.println(loglk);
				saveHMMToFile();
			}
			
		}

//		saveHMMToFile();
		
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
//		System.out.println("Starting HMM:\n" + origHmm);
		System.out.printf("Ending loglikelihood : %.5f\n", loglk);
		System.out.println("\nResulting HMM:\n" + learntHmm);	
		System.out.println("\nSteady state probabilities: " + Arrays.toString(steadyState));

		// write HMM to file
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ numStrategies + "StateHmm.txt"));
//		writer.write("\nStarting HMM:\n" + origHmm);
		writer.write(String.format("Ending loglikelihood : %.5f\n", loglk));
		writer.write("\nResulting HMM:\n" + learntHmm);
		writer.write("\nSteady state probabilities: " + Arrays.toString(steadyState));
		writer.flush();
		writer.close();

	}

	public static List<List<SigActObservation<CandySignal, CandyReport>>> getActObsSequence() {
		List<List<SigActObservation<CandySignal, CandyReport>>> seq = 
				new ArrayList<List<SigActObservation<CandySignal, CandyReport>>>();
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				List<SigActObservation<CandySignal, CandyReport>> list = 
						game.signalReportObjList.get(hitId);
				seq.add(list);
			}
		}
		return seq;
	}

	private static void setStrategyNames() throws IOException {
		
		System.out.println("\nGive strategies names");
		
		// fill the names of the strategies
		for (int i = 0; i < numStrategies; i++) {
			Opdf <SigActObservation<CandySignal, CandyReport>> opdf = learntHmm.getOpdf(i);
			if (isMMStrategy(opdf)) {
				if (mmState == -1) {
					strategyNames[i] = "MM";
					mmState = i;
				}else if (isBetterMMStrategy(i, mmState)) {
					strategyNames[mmState] = "Mixed";
					mixedState = mmState;
					strategyNames[i] = "MM";
					mmState = i;					
				}
			} else if (isGBStrategy(opdf)) {
				if (gbState == -1) {
					strategyNames[i] = "GB";
					gbState = i;					
				} else if (isBetterGBStrategy(i, gbState)) {
					strategyNames[gbState] = "Mixed";
					mixedState = gbState;
					strategyNames[i] = "GB";
					gbState = i;
				}
			} else if (isTruthfulStrategy(opdf)) {
				if (truthfulState == -1) {
					strategyNames[i] = "Truthful";		
					truthfulState = i;				
				} else if ( isBetterTruthfulStrategy(i, truthfulState)) {
					strategyNames[truthfulState] = "Mixed";
					mixedState = truthfulState;
					
					strategyNames[i] = "Truthful";		
					truthfulState = i;
				}
			}
			
			if (strategyNames[i] == null) {
				if (mixedState == -1) {
					strategyNames[i] = "Mixed";
					mixedState = i;					
				} else {
					strategyNames[i] = "Mixed2";
					mixed2State = i;					
				}
			}
		}
		System.out.println(Arrays.toString(strategyNames));
		System.out.println(String.format("MM strategy: %d, GB strategy: %d, " +
				"Truthful strategy: %d, Mixed strategy: %d, Mixed 2: %d", 
				mmState, gbState, truthfulState, mixedState, mixed2State));

	}
	
	private static void getStateSeq() throws IOException {
		
		// Calculate most likely state sequence		
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
		
		// Write state sequence to csv
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "stateSeq" + numStrategies + "States.csv"));
		
		writer.write(String.format("hitId,"));
		for (int i = 1; i <= numRounds; i++) {
			writer.write(String.format("%d,", i));
		}
		writer.write(String.format("actual payoff\n"));
		
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				int[] stateSeq = game.stateSeq.get(hitId);
				writer.write(String.format("%s,",hitId));
				for (int state : stateSeq) {
					writer.write(state + ",");
				}
				writer.write(String.format("%.2f", game.actualPayoff.get(hitId)));
				writer.write("\n");
			}
			writer.write("\n");
		}
		writer.flush();
		writer.close();
		
	}
	
	
	private static void getHMMType() throws IOException {

		System.out.println("Determining HMM type");

		// Determine HMM type
		for (Game game : expSet.games) {

			game.strategyComboTypeArray = new int[numRounds];

			for (int roundIndex = 0; roundIndex < numRounds; roundIndex++) {
				int[] numPlayerForStrategy = new int[numStrategies];
				for (String hitId : game.playerHitIds) {

					int strategyIndexForRound = game.stateSeq.get(hitId)[roundIndex];
					numPlayerForStrategy[strategyIndexForRound]++;
				}

				if (!treatment.equals("prior2-uniquetruthful")) {
					// other treatments

					for (int strategyIndex = 0; strategyIndex < numStrategies; strategyIndex++) {
						if (numPlayerForStrategy[strategyIndex] == numPlayers
								&& (strategyIndex == mmState
										|| strategyIndex == gbState || strategyIndex == truthfulState)) {
							// all players are playing MM, GB or Truthful
							// strategy
							game.strategyComboTypeArray[roundIndex] = strategyIndex;
							break;
						} else if (numPlayerForStrategy[strategyIndex] > 0) {
							game.strategyComboTypeArray[roundIndex] = -1;
							break;
						}
					}
				} else {
					// "prior2-uniquetruthful"
					if (truthfulState != -1
							&& numPlayerForStrategy[truthfulState] == numStrategies) {
						// truthful equilibrium
						game.strategyComboTypeArray[roundIndex] = truthfulState;
					} else if (mixedState != -1
							&& numPlayerForStrategy[mixedState] == numStrategies) {
						// mixed strategy equilibrium
						game.strategyComboTypeArray[roundIndex] = mixedState;
					} else if (mmState != -1 && gbState != -1
							&& numPlayerForStrategy[mmState] == 1
							&& numPlayerForStrategy[gbState] == 3) {
						// 1 MM 3 GB
						game.strategyComboTypeArray[roundIndex] = 4;
					} else if (mmState != -1 && gbState != -1
							&& numPlayerForStrategy[mmState] == 3
							&& numPlayerForStrategy[gbState] == 1) {
						// 3 MM 1 GB
						game.strategyComboTypeArray[roundIndex] = 5;
					} else {
						// unclassified
						game.strategyComboTypeArray[roundIndex] = -1;
					}

				}
			}
		}

		// Write HMM type to csv
		BufferedWriter writerCsv = new BufferedWriter(new FileWriter(rootDir
				+ "hmmType" + numStrategies + "Strategies.csv"));
		for (Game game : expSet.games) {
			for (int i = 0; i < game.strategyComboTypeArray.length; i++) {
				if (i == game.strategyComboTypeArray.length - 1) {
					writerCsv.write(String.format("%d\n",
							game.strategyComboTypeArray[i]));
				} else {
					writerCsv.write(String.format("%d,",
							game.strategyComboTypeArray[i]));
				}
			}
		}
		writerCsv.flush();
		writerCsv.close();

		// Determine HMM Type Count
		int[][] hmmTypeCount = new int[numRounds][numStrategies + 2];
		for (Game game : expSet.games) {
			for (int i = 0; i < numRounds; i++) {
				if (game.strategyComboTypeArray[i] == -1)
					continue;
				else
					hmmTypeCount[i][game.strategyComboTypeArray[i]]++;
			}
		}

		// Write hmmTypeCount to CSV file
		writerCsv = new BufferedWriter(new FileWriter(rootDir + "stateSeqDist"
				+ numStrategies + "Strategies.csv"));
		for (int j = 0; j < numStrategies; j++) {
			writerCsv.write(String.format("%s,", strategyNames[j]));
		}
		if (treatment.equals("prior2-uniquetruthful")) {
			writerCsv.write(String.format("%s,", "1MM3GB"));
			writerCsv.write(String.format("%s,", "3MM1GB"));
		}
		writerCsv.write("\n");

		for (int i = 0; i < numRounds; i++) {
			for (int j = 0; j < numStrategies; j++) {
				writerCsv.write(String.format("%d,", hmmTypeCount[i][j]));
			}
			if (treatment.equals("prior2-uniquetruthful")) {
				writerCsv.write(String.format("%d,",
						hmmTypeCount[i][numStrategies]));
				writerCsv.write(String.format("%d,",
						hmmTypeCount[i][numStrategies + 1]));
			}
			writerCsv.write("\n");
		}
		writerCsv.flush();
		writerCsv.close();

		// Generate Matlab file
		BufferedWriter writerMatlab = new BufferedWriter(new FileWriter(rootDir
				+ "hmmType" + numStrategies + "Strategies.m"));

		// write data arrays
		for (int j = 0; j < numStrategies; j++) {

			if (treatment.equals("prior2-symmlowpay")) {
				// treatment 4
				if (strategyNames[j].equals("Mixed")
						|| strategyNames[j].equals("Mixed2")) {
					continue;
				}
			} else if (treatment.equals("prior2-uniquetruthful")) {
				// treatment 3, skip MM and GB strategies
				if (strategyNames[j].equals("MM")
						|| strategyNames[j].equals("GB")) {
					continue;
				}
			} else {
				// not treatment 3, skip mixed strategy
				if (strategyNames[j].equals("Mixed")) {
					continue;
				}
			}

			writerMatlab.write(String.format("%s = [", strategyNames[j]));
			for (int i = 0; i < numRounds; i++) {
				double percent = hmmTypeCount[i][j] * 1.0 / expSet.games.size();
				writerMatlab.write(String.format("%.10f ", percent));
			}
			writerMatlab.write("]';\n");

		}
		if (treatment.equals("prior2-uniquetruthful")) {
			// write data for treatment 3
			writerMatlab.write(String.format("oneMMThreeGB = ["));
			for (int i = 0; i < numRounds; i++) {
				double percent = hmmTypeCount[i][numStrategies] * 1.0
						/ expSet.games.size();
				writerMatlab.write(String.format("%.10f ", percent));
			}
			writerMatlab.write("]';\n");

			writerMatlab.write(String.format("threeMMOneGB = ["));
			for (int i = 0; i < numRounds; i++) {
				double percent = hmmTypeCount[i][numStrategies + 1] * 1.0
						/ expSet.games.size();
				writerMatlab.write(String.format("%.10f ", percent));
			}
			writerMatlab.write("]';\n");
		}
		
		writerMatlab.write(String.format("Unclassified = ["));
		for (int i = 0; i < numRounds; i++) {
			double num = 1.0;
			for (int j = 0; j < numStrategies; j++) {
				num -= hmmTypeCount[i][j] * 1.0 / expSet.games.size();
			}
			if (treatment.equals("prior2-uniquetruthful")) {
				num -= hmmTypeCount[i][numStrategies] * 1.0
						/ expSet.games.size();
				num -= hmmTypeCount[i][numStrategies + 1] * 1.0
						/ expSet.games.size();
			}
			num -= 0.0000000001;
			writerMatlab.write(String.format("%.10f ", num));
		}
		writerMatlab.write("]';\n\n");

		writerMatlab.write("fH = figure;\n" +
				"set(fH, 'Position', [300, 300, 500, 400]);\n");

		// plot bar chart
		writerMatlab.write(String.format("hBar = bar(0:%d, [", numRounds-1));
		if (treatment.equals("prior2-basic")) {
			writerMatlab.write("MM GB Truthful");
		} else if (treatment.equals("prior2-outputagreement")){
	    	writerMatlab.write("GB MM Truthful");
	    } else if (treatment.equals("prior2-uniquetruthful")) {
			writerMatlab.write("Mixed threeMMOneGB oneMMThreeGB ");
		} else if (treatment.equals("prior2-symmlowpay")) {
			writerMatlab.write("MM");
		} else {
			writerMatlab.write("Truthful GB MM");
		}
		writerMatlab.write(String.format("], 'BarWidth', 0.7, " +
				"'BarLayout', 'stack', 'LineStyle', 'none');\n"));

		// labels
		int xylabelfontSize = 26;
		int axisFontSize = 20;
		
		String ytick = "";
		if (treatment.equals("prior2-basic")) {
			ytick = "[0 0.45 0.54 1]";
		} else if (treatment.equals("prior2-outputagreement")) {
			ytick = "[0 0.36 0.46 1]";
		} else if (treatment.equals("prior2-uniquetruthful")) {
			ytick = "[0 0.12 0.17 1]";
		} else if (treatment.equals("prior2-symmlowpay")) {
			ytick = "[0.02 1]";
		} else {
			ytick = "[0 1]";
		}
		
		writerMatlab.write(String.format("box off;\n" +
				"set(gca,'Position',[.03 .15 .8 .8]);\n" +
				"xlh = xlabel('Round');\n"
				+ "set(xlh, 'FontSize', %d);\n" 
				+ "axes = findobj(gcf,'type','axes');\n"
				+ "set(axes, 'FontSize', %d);\n"
				+ "set(axes, 'XTick', [0 %d]);\n" 
				+ "B = axes;\n" +
				"set(B, 'yaxislocation', 'right', 'ytick', %s);\n" +
				"ylh = ylabel('Percentage of games');\n" +
				"set(ylh, 'FontSize', %d);\n",
				xylabelfontSize, axisFontSize, numRounds - 1, 
				ytick, xylabelfontSize));
		
		// set axis range
		writerMatlab.write(String
				.format("axis([-1 %d 0 1]);\n", numRounds));
		
		// write legend
		writerMatlab.write("lh = legend(");

		if (treatment.equals("prior2-uniquetruthful")) {
			writerMatlab
					.write("'Mixed', 'threeMMOneGB', 'oneMMThreeGB', ");
		} else if (treatment.equals("prior2-symmlowpay")) {
			writerMatlab.write("'MM', ");
		} else if (treatment.equals("prior2-outputagreement")) {
			writerMatlab.write("'GB', 'MM', 'Truthful', ");
		} else {
			writerMatlab.write("'MM', 'GB', 'Truthful', ");
		}
		writerMatlab.write(String
				.format("'Location', 'Best');\n"
						+ "set(lh, 'FontSize', 20);\n"));


		// set colors
		writerMatlab.write(String.format("set(hBar,{'FaceColor'},{"));
		if (treatment.equals("prior2-uniquetruthful")) {
			writerMatlab.write("[0.6 0.6 0.6];'y';'r';");
		} else if (treatment.equals("prior2-symmlowpay")) {
			writerMatlab.write("[1 0.64 0];");
		} else if (treatment.equals("prior2-outputagreement")) {
			writerMatlab.write("'b';[1 0.64 0];'g';");
		} else {
			writerMatlab.write("[1 0.64 0];'b';'g';");
		}
		writerMatlab.write(String.format("});\n"));

		writerMatlab.flush();
		writerMatlab.close();

	}

	private static void writeStrategyHeatMap() throws IOException {
		
		BufferedWriter writer1 = new BufferedWriter(new FileWriter(rootDir
				+ "heatMap" + numStrategies + "StrategiesReverseCompare.m"));
		
		List<int[]> seqList = new ArrayList<int[]>();
		for (Game game : expSet.games) {
			for (String hitId: game.playerHitIds) {
				int[] strategySeq = game.stateSeq.get(hitId);
				seqList.add(strategySeq);
			}
		}		
		
		Collections.sort(seqList, new Comparator<int[]>() {

			@Override
			public int compare(int[] o1, int[] o2) {
				return util(o1, o2, o1.length - 1);
			}
			
			private int util(int[] o1, int[] o2, int index) {
				if (index == 0) {
					return o1[index] - o2[index]; 
				} else {
					if (o1[index] < o2[index])
						return -1;
					else if (o1[index] > o2[index])
						return 1;
					else 
						return util(o1, o2, index-1);
				}
					
			}
			
		});

		List<int[]> newSeqList = new ArrayList<int[]>();
		
		for (int endStrIndex = 0; endStrIndex < 4; endStrIndex++) {

			boolean blockFound = false;
			boolean inBlock = false;

			List<int[]> str0List = new ArrayList<int[]>();
			List<int[]> str1List = new ArrayList<int[]>();
			List<int[]> str2List = new ArrayList<int[]>();
			List<int[]> str3List = new ArrayList<int[]>();

			for (int seqIndex = 0; seqIndex < seqList.size(); seqIndex++) {
				
				int[] seq = seqList.get(seqIndex);
				
				if (!inBlock) {
					if (seq[numRounds - 1] == endStrIndex) {
						blockFound = true;
						inBlock = true;
					}
				} else {
					if (seq[numRounds - 1] != endStrIndex) 
						inBlock = false;
				}
				
				if (blockFound == false) {
					continue;
				} else if (inBlock == false) {
					break;
				} else {
					if (seq[0] == 0)
						str0List.add(seq);
					else if (seq[0] == 1)
						str1List.add(seq);
					else if (seq[0] == 2)
						str2List.add(seq);
					else if (seq[0] == 3)
						str3List.add(seq);
				}
			}
			
			newSeqList.addAll(str0List);
			newSeqList.addAll(str1List);
			newSeqList.addAll(str2List);
			newSeqList.addAll(str3List);
			
		}
		
		seqList.clear();
		seqList.addAll(newSeqList);

		int index = 0;
		for (int i = 0; i < seqList.size(); i++) {
				int[] strategySeq = seqList.get(i);
				writer1.write(String.format("x%d = %s;\n", index, Arrays.toString(strategySeq)));
				index++;
		}
		
		index = 0;
		writer1.write("m = [");
		for (int i = 0; i < seqList.size(); i++) {
			writer1.write(String.format("x%d;", index));
			index++;			
		}
		writer1.write("];\n\n");
		writer1.write("api_path = '~/plotly';\n" +
				"addpath(genpath(api_path));\n" +
				"api_key = 'vivdmp4vf4';\n" +
				"username = 'alice.gao';\n" +
				"signin(username, api_key);\n");
		
		String truthfulColor = "50,205,50";
		String gbColor 		 = "0,0,255";
		String mixedColor 	 = "190,190,190";
		String mixed2Color 	 = "127,127,127";
		String mmColor 		 = "255,140,0";
		
		writer1.write("cs={{");
		for (int i = 0; i <= 3; i++) {
			double num = i * 1.0 / 3;
			if (mmState == i) {
					writer1.write(String.format("{%.2f, 'rgb(%s)'},", num, mmColor));
			} else if (gbState == i) {
					writer1.write(String.format("{%.2f, 'rgb(%s)'},", num, gbColor));
			} else if (truthfulState == i) {
					writer1.write(String.format("{%.2f, 'rgb(%s)'},", num, truthfulColor));
			} else if (mixedState == i) {
					writer1.write(String.format("{%.2f, 'rgb(%s)'},", num, mixedColor));				
			} else if (mixed2State == i) {
				writer1.write(String.format("{%.2f, 'rgb(%s)'},", num, mixed2Color));				
			}
		}
		writer1.write("}};\n");
		writer1.write("plotly({struct('z', m,'scl',cs,'type', 'heatmap')})\n");
		writer1.flush();
		writer1.close();
	}
	
	private static void writeStrategyDistributionMatlabCode() throws IOException {
		
		// state sequence distribution
		int[][] strategyCount = new int[numStrategies][numRounds];
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				int[] strategySeq = game.stateSeq.get(hitId);
				for (int roundIndex = 0; roundIndex < numRounds; roundIndex++) {
					int strategyIndex = strategySeq[roundIndex];
					strategyCount[strategyIndex][roundIndex]++;
				}
			}
		}
		double[][] strategyDistribution = new double[numStrategies][numRounds];
		int totalNumPlayers = expSet.games.size() * numPlayers;
		for (int roundIndex = 0; roundIndex < numRounds; roundIndex++) {
			for (int strategyIndex = 0; strategyIndex < numStrategies; strategyIndex++) {
				strategyDistribution[strategyIndex][roundIndex] 
						= strategyCount[strategyIndex][roundIndex] * 1.0 / totalNumPlayers;
			}
		}

		BufferedWriter writer1 = new BufferedWriter(new FileWriter(rootDir
				+ "stateSeqDist" + numStrategies + "Strategies.m"));

		for (int strategyIndex = 0; strategyIndex < numStrategies; strategyIndex++) {
			writer1.write(strategyNames[strategyIndex] + " = [");
			for (int roundIndex = 0; roundIndex < numRounds; roundIndex++) {
				writer1.write(String.format("%.10f ", strategyDistribution[strategyIndex][roundIndex]));
			}
			writer1.write("]';\n");
		}

		writer1.write(String.format("\n\n" +
				"figure;\n" +
				"hBar = bar(1:%d, ", numRounds));
		if (treatment.equals("prior2-symmlowpay")) {
			if (numStrategies == 4)
				writer1.write("[Truthful MM Mixed Mixed2]");
			else if (numStrategies == 3)			
				writer1.write("[Truthful MM Mixed]");
		} else {
			writer1.write("[Truthful GB MM Mixed]");
		}
		writer1.write(", 0.5, 'stack');\n");
		
		writer1.write(
				"xlh = xlabel('Round');\n" +
				"ylh = ylabel('Percentage of players');\n" +
				"set(xlh, 'FontSize', 26);\n" +
				"set(ylh, 'FontSize', 26);\n" +
				"axes = findobj(gcf,'type','axes');\n" +
				"set(axes, 'FontSize', 20);\n");
				
		writer1.write(String.format("axis([0 %d 0 1]);\n", numRounds + 1));
		
		// legend
		writer1.write("lh = legend(");
		if (treatment.equals("prior2-symmlowpay")) {
			if (numStrategies == 4)
				writer1.write("'Truthful', 'MM', 'Mixed', 'Mixed2'");
			else if (numStrategies == 3)
				writer1.write("'Truthful', 'MM', 'Mixed'");
		} else {
			writer1.write("'Truthful', 'GB', 'MM', 'Mixed'");
		}
		writer1.write(", 'Location', 'Best');\n" +
				"set(lh, 'FontSize', 20);\n");
		
		
		if (treatment.equals("prior2-symmlowpay")) {
			if (numStrategies == 4)
				writer1.write("set(hBar,{'FaceColor'},{'g';[1 0.64 0];[0.5 0.5 0.5];[0.8 0.8 0.8];});\n");
			else if (numStrategies == 3)
				writer1.write("set(hBar,{'FaceColor'},{'g';[1 0.64 0];[0.8 0.8 0.8];});\n");
		} else {
			writer1.write("set(hBar,{'FaceColor'},{'g';'b';[1 0.64 0];[0.8 0.8 0.8];});\n");
		}
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

	private static boolean isGBStrategy(
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf) {
		SigActObservation<CandySignal, CandyReport> mmGB = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.MM, CandyReport.GB);
		SigActObservation<CandySignal, CandyReport> gbGB = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.GB, CandyReport.GB);
		return opdf.probability(mmGB) > 0.8 && opdf.probability(gbGB) > 0.8; 
	}

	private static boolean isBetterGBStrategy(int i, int gbStrIndex) {
		Opdf <SigActObservation<CandySignal, CandyReport>> opdf1 = learntHmm.getOpdf(i);
		Opdf <SigActObservation<CandySignal, CandyReport>> opdf2 = learntHmm.getOpdf(gbStrIndex);
		SigActObservation<CandySignal, CandyReport> mmGB = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.MM, CandyReport.GB);
		SigActObservation<CandySignal, CandyReport> gbGB = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.GB, CandyReport.GB);
		return opdf1.probability(mmGB) > opdf2.probability(mmGB) 
				&& opdf1.probability(mmGB) > opdf2.probability(mmGB) ;
	}

	private static boolean isMMStrategy(
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf) {
		SigActObservation<CandySignal, CandyReport> mmMM = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.MM, CandyReport.MM);
		SigActObservation<CandySignal, CandyReport> gbMM = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.GB, CandyReport.MM);
		return opdf.probability(mmMM) > 0.8 && opdf.probability(gbMM) > 0.8; 
	}

	private static boolean isBetterMMStrategy(int i, int mmStrIndex) {
		Opdf <SigActObservation<CandySignal, CandyReport>> opdf1 = learntHmm.getOpdf(i);
		Opdf <SigActObservation<CandySignal, CandyReport>> opdf2 = learntHmm.getOpdf(mmStrIndex);
		SigActObservation<CandySignal, CandyReport> mmMM = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.MM, CandyReport.MM);
		SigActObservation<CandySignal, CandyReport> gbMM = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.GB, CandyReport.MM);
		return opdf1.probability(mmMM) > opdf2.probability(mmMM) 
				&& opdf1.probability(gbMM) > opdf2.probability(gbMM) ;
	}

	private static boolean isTruthfulStrategy(
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf) {
		SigActObservation<CandySignal, CandyReport> mmMM = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.MM, CandyReport.MM);
		SigActObservation<CandySignal, CandyReport> gbGB = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.GB, CandyReport.GB);
		return opdf.probability(mmMM) > 0.8 && opdf.probability(gbGB) > 0.8; 
	}		
	
	private static boolean isBetterTruthfulStrategy(int i, int truthfulState2) {
		Opdf <SigActObservation<CandySignal, CandyReport>> opdf1 = learntHmm.getOpdf(i);
		Opdf <SigActObservation<CandySignal, CandyReport>> opdf2 = learntHmm.getOpdf(truthfulState2);
		SigActObservation<CandySignal, CandyReport> mmMM = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.MM, CandyReport.MM);
		SigActObservation<CandySignal, CandyReport> gbGB = 
				new SigActObservation<CandySignal, CandyReport>(CandySignal.GB, CandyReport.GB);
		return opdf1.probability(mmMM) > opdf2.probability(mmMM) 
				&& opdf1.probability(gbGB) > opdf2.probability(gbGB);
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

	private static void saveHMMToFile() throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(
				String.format("%slearntHMM%dstrategies.txt", rootDir, numStrategies)));
		int numStates = learntHmm.nbStates();
		writer.write(String.format("numStates:%d\n", numStates));
		
		for (int i = 0; i < numStates; i++) {
			writer.write(String.format("pi,%d:%.17f\n", i, learntHmm.getPi(i)));
		}
		
		for (int i = 0; i < numStates; i++) {
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf = learntHmm.getOpdf(i);
			double reportMMGivenSignalMM = opdf.probability(
					new SigActObservation<CandySignal, CandyReport>(CandySignal.MM, CandyReport.MM));
			double reportMMGivenSignalGB = opdf.probability(
					new SigActObservation<CandySignal, CandyReport>(CandySignal.GB, CandyReport.MM));
			writer.write(String.format("opdf,%d:%.17f,%.17f\n", 
					i, reportMMGivenSignalMM, reportMMGivenSignalGB));
		}
		
		for (int i = 0; i < numStates; i++) {
			for (int j = 0; j < numStates; j++) {
				writer.write(String.format("aij,%d,%d:%.17f\n", i, j, learntHmm.getAij(i, j)));
			}
		}
		writer.flush();
		writer.close();
	}

	private static Hmm<SigActObservation<CandySignal, CandyReport>> createHMMFromFile(int numStr) 
			throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(
				String.format("%slearntHMM%dstrategies.txt", rootDir, numStr)));
		String line = reader.readLine();
		Matcher matcher = Pattern.compile("numStates:(.*)").matcher(line);
		int numStates = -1;
		if (matcher.matches())
			numStates = Integer.parseInt(matcher.group(1));
//		System.out.printf("num states: %d\n", numStates);
		
		double[] pi = new double[numStates];
		for (int i = 0; i < numStates; i++) {
			if (i == numStates - 1) {
				line = reader.readLine();
				double left = 1.0;
				for (int j = 0; j < numStates - 1; j++) {
					left -= pi[j];
				}
				pi[numStates - 1] = left;
			} else {
				line = reader.readLine();
				matcher = Pattern.compile("pi,(.*):(.*)").matcher(line);
				if (matcher.matches()) {
					pi[i] = Double.parseDouble(matcher.group(2));
				}
			}	
		}
//		System.out.printf("pi: %s\n", Arrays.toString(pi));

		List<OpdfStrategy<CandySignal, CandyReport>> opdfs = 
				new ArrayList<OpdfStrategy<CandySignal, CandyReport>>();

		for (int i = 0; i < numStates; i++) {
			line = reader.readLine();
			matcher = Pattern.compile("opdf,(.*):(.*),(.*)").matcher(line);
			if (matcher.matches()) {
				double[][] probs = new double[2][2];
				probs[0][0] = Double.parseDouble(matcher.group(2));
				probs[0][1] = 1 - probs[0][0];
				probs[1][0] = Double.parseDouble(matcher.group(3));
				probs[1][1] = 1 - probs[1][0];
				OpdfStrategy<CandySignal, CandyReport> opdf = 
						new OpdfStrategy<CandySignal, CandyReport>(CandySignal.class,
						CandyReport.class, signalPrior, probs);
//				System.out.printf("opdf,%d:%s", i, opdf.toString());
				opdfs.add(opdf);
			}
		}
		
		double[][] aij = new double[numStates][numStates];
		for (int i = 0; i < numStates; i++) {
			for (int j = 0; j < numStates; j++) {
				line = reader.readLine();
				if (j == numStates - 1) {
					double left = 1.0;
					for (int jj = 0; jj < numStates - 1; jj++) {
						left -= aij[i][jj];
					}
					aij[i][numStates - 1] = left;
				} else {
					matcher = Pattern.compile("aij,(.*),(.*):(.*)").matcher(line);
					if (matcher.matches()) {
						int ii = Integer.parseInt(matcher.group(1));
						int jj = Integer.parseInt(matcher.group(2));
						double prob = Double.parseDouble(matcher.group(3));
						aij[ii][jj] = prob;
					}
				}
			}
		}
//		System.out.println("Aij:");
//		for (int i = 0; i < numStates; i++) {
//			System.out.println(Arrays.toString(aij[i]));
//		}
		reader.close();		
		Hmm<SigActObservation<CandySignal, CandyReport>> hmm = 
				new Hmm<SigActObservation<CandySignal, CandyReport>>(
						pi, aij, opdfs);
		return hmm;
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
					AnalysisUtils.getRandomVec(2),
					AnalysisUtils.getRandomVec(2) };
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
		
		System.out.println("Classify equilibrium convergence using simple method");
		
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

	private static void gameConvergenceTypeT3() {

		int numHO = 0;
		int num3MM1GB = 0;
		int num1MM3GB = 0;
		int numUnclassified = 0;
		
		for (Game game : expSet.games) {

			game.fillAsymmetricConvergenceType();
			
			if (game.convergenceType.equals("3MM"))
				num3MM1GB++;
			else if (game.convergenceType.equals("3GB"))
				num1MM3GB++;
			else if (game.convergenceType.equals("HO"))
				numHO++;
			else 
				numUnclassified++;
		}
		
		System.out.println(String.format("3GB: %d, 3MM: %d, HO: %d, Unclassified: %d, Total: %d", 
				num1MM3GB, num3MM1GB, numHO, numUnclassified, expSet.games.size()));
		
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
		
		if (treatment.equals("prior2-constant") || treatment.equals("prior2-symmlowpay"))
			choseReport = Pattern.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) chose report ([A-Z]{2}) \\(radio: [0-9]\\)");
		else 
			choseReport = Pattern.compile("^(\\d{2}:\\d{2}.\\d{3}) ([a-zA-Z\\s0-9]+) @ HIT ([a-zA-Z\\s0-9]+) chose report ([A-Z]{2})");
		
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
			matcher = priorPattern.matcher(currentLine);
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
