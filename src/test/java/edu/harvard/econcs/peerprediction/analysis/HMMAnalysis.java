package edu.harvard.econcs.peerprediction.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.Opdf;
import be.ac.ulg.montefiore.run.jahmm.ViterbiCalculator;
import net.andrewmao.models.games.BWToleranceLearner;
import net.andrewmao.models.games.OpdfStrategy;
import net.andrewmao.models.games.SigActObservation;

public class HMMAnalysis {

	public static void performAnalysis() throws IOException {

		learnHMM();
		setStrategyNames();
		writeStateSeq();
		eqConvergenceHmm();
		writeStrategyChangeHeatMap();

		writeStrategyDistribution();
		genStrategyChangePredictedByHmm();
		graphLogLikelihood();
	}

	
	public static void learnHMM() throws IOException {
		System.out.println("Learning HMM");
	
		// Set number of strategies
		if (LogReader.treatment.equals("prior2-basic")
				|| LogReader.treatment.equals("prior2-outputagreement")) {
			LogReader.numStrategies = 4;
		} else if (LogReader.treatment.equals("prior2-uniquetruthful")) {
			LogReader.numStrategies = 4;
		} else if (LogReader.treatment.equals("prior2-symmlowpay")) {
			LogReader.numStrategies = 4;
		} else if (LogReader.treatment.equals("prior2-constant")) {
			LogReader.numStrategies = 4;
		}
		LogReader.strategyNames = new String[LogReader.numStrategies];
		System.out.printf("numStrategies: %d\n", LogReader.numStrategies);
	
		LogReader.learntHmm = HMMAnalysis
				.learnHMM(LogReader.expSet.games, LogReader.numStrategies, LogReader.numRestarts);
		List<List<SigActObservation<CandySignal, CandyReport>>> seq = HMMAnalysis
				.getActObsSequence(LogReader.expSet.games);
		double loglk = BWToleranceLearner.computeLogLk(LogReader.learntHmm, seq);
	
		/*
		 * // list of signal and report observations
		 * List<List<SigActObservation<CandySignal, CandyReport>>> seq =
		 * LogReader.getActObsSequence(expSet.games); BWToleranceLearner bwl =
		 * new BWToleranceLearner(); double loglk = Double.NEGATIVE_INFINITY;
		 * 
		 * // load last best HMM if it exists String filename =
		 * String.format("%slearntHMM%dstrategies.txt", rootDir, numStrategies);
		 * File hmmFile = new File(filename); if (hmmFile.exists()) { learntHmm
		 * = createHMMFromFile(filename); loglk =
		 * BWToleranceLearner.computeLogLk(learntHmm, seq);
		 * System.out.printf("Starting loglikelihood : %.5f\n", loglk); }
		 * 
		 * for (int i = 0; i < numRestarts; i++) {
		 * 
		 * Hmm<SigActObservation<CandySignal, CandyReport>> origHmmTemp =
		 * Utils.getRandomHmm(numStrategies);
		 * 
		 * Hmm<SigActObservation<CandySignal, CandyReport>> learntHmmTemp =
		 * bwl.learn(origHmmTemp, seq);
		 * 
		 * double loglkTemp = BWToleranceLearner.computeLogLk(learntHmmTemp,
		 * seq);
		 * 
		 * if (loglkTemp > loglk) {
		 * 
		 * learntHmm = learntHmmTemp; loglk = loglkTemp;
		 * 
		 * saveHMMDataToFile(filename, learntHmm); } }
		 */
		// compute steady state prob
		double[] steadyState = HMMAnalysis.calcSteadyStateProb(LogReader.learntHmm);
	
		// write HMM to console
		System.out.printf("Ending loglikelihood : %.5f\n"
				+ "Resulting HMM: %s\n" + "Steady state probabilities: %s\n",
				loglk, LogReader.learntHmm, Arrays.toString(steadyState));
	
		// write HMM to file
		BufferedWriter writer = new BufferedWriter(new FileWriter(LogReader.rootDir
				+ LogReader.numStrategies + "StateHmm.txt"));
		writer.write(String.format("Ending loglikelihood : %.5f\n"
				+ "Resulting HMM: %s\n" + "Steady state probabilities: %s\n",
				loglk, LogReader.learntHmm, Arrays.toString(steadyState)));
		writer.flush();
		writer.close();
	
	}

	public static Hmm<SigActObservation<CandySignal, CandyReport>> learnHMM(
			List<Game> games, int numStrategies, int numRestarts)
			throws IOException {
	
		Hmm<SigActObservation<CandySignal, CandyReport>> bestHMM = null;
	
		List<List<SigActObservation<CandySignal, CandyReport>>> seq = HMMAnalysis
				.getActObsSequence(LogReader.expSet.games);
		BWToleranceLearner bwl = new BWToleranceLearner();
		double loglk = Double.NEGATIVE_INFINITY;
	
		String fileame = String.format("%slearntHMM%dstrategies.txt", LogReader.rootDir,
				numStrategies);
	
		// load last best HMM if it exists
		File hmmFile = new File(fileame);
		if (hmmFile.exists()) {
			bestHMM = HMMAnalysis.createHMMFromFile(fileame);
			loglk = BWToleranceLearner.computeLogLk(bestHMM, seq);
			// System.out.printf("Starting loglikelihood : %.5f\n", loglk);
		}
	
		for (int i = 0; i < numRestarts; i++) {
	
			Hmm<SigActObservation<CandySignal, CandyReport>> origHmmTemp = Utils
					.getRandomHmm(numStrategies);
	
			Hmm<SigActObservation<CandySignal, CandyReport>> learntHmmTemp = bwl
					.learn(origHmmTemp, seq);
	
			double loglkTemp = BWToleranceLearner.computeLogLk(learntHmmTemp,
					seq);
	
			if (loglkTemp > loglk) {
				// save better hmm
				bestHMM = learntHmmTemp;
				loglk = loglkTemp;
	
				HMMAnalysis.saveHMMDataToFile(fileame, bestHMM);
			}
		}
	
		return bestHMM;
	}

	public static void setStrategyNames() throws IOException {
		System.out.println("Give strategies names");
	
		for (int i = 0; i < LogReader.numStrategies; i++) {
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf = LogReader.learntHmm
					.getOpdf(i);
	
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf1 = LogReader.learntHmm
					.getOpdf(i);
	
			if (Utils.isMMStrategy(opdf)) {
				if (LogReader.mmState == -1) {
					LogReader.strategyNames[i] = "MM";
					LogReader.mmState = i;
				} else {
					Opdf<SigActObservation<CandySignal, CandyReport>> opdf2 = LogReader.learntHmm
							.getOpdf(LogReader.mmState);
	
					if (Utils.isBetterMMStrategy(opdf1, opdf2)) {
	
						LogReader.strategyNames[LogReader.mmState] = "Mixed";
						LogReader.mixedState = LogReader.mmState;
	
						LogReader.strategyNames[i] = "MM";
						LogReader.mmState = i;
					}
				}
			} else if (Utils.isGBStrategy(opdf)) {
				if (LogReader.gbState == -1) {
					LogReader.strategyNames[i] = "GB";
					LogReader.gbState = i;
				} else {
					Opdf<SigActObservation<CandySignal, CandyReport>> opdf4 = LogReader.learntHmm
							.getOpdf(LogReader.gbState);
	
					if (Utils.isBetterGBStrategy(opdf1, opdf4)) {
						LogReader.strategyNames[LogReader.gbState] = "Mixed";
						LogReader.mixedState = LogReader.gbState;
	
						LogReader.strategyNames[i] = "GB";
						LogReader.gbState = i;
					}
				}
			} else if (Utils.isTruthfulStrategy(opdf)) {
				if (LogReader.truthfulState == -1) {
					LogReader.strategyNames[i] = "Truthful";
					LogReader.truthfulState = i;
				} else {
					Opdf<SigActObservation<CandySignal, CandyReport>> opdf3 = LogReader.learntHmm
							.getOpdf(LogReader.truthfulState);
	
					if (Utils.isBetterTruthfulStrategy(opdf1, opdf3)) {
	
						LogReader.strategyNames[LogReader.truthfulState] = "Mixed";
						LogReader.mixedState = LogReader.truthfulState;
	
						LogReader.strategyNames[i] = "Truthful";
						LogReader.truthfulState = i;
					}
				}
			}
	
			if (LogReader.strategyNames[i] == null) {
				if (LogReader.mixedState == -1) {
					LogReader.strategyNames[i] = "Mixed";
					LogReader.mixedState = i;
				} else {
					LogReader.strategyNames[i] = "Mixed2";
					LogReader.mixed2State = i;
				}
			}
		}
		System.out.printf("Strategy names: %s\n"
				+ "MM strategy: %d, GB strategy: %d, "
				+ "Truthful strategy: %d, Mixed strategies: %d, %d\n",
				Arrays.toString(LogReader.strategyNames), LogReader.mmState, LogReader.gbState,
				LogReader.truthfulState, LogReader.mixedState, LogReader.mixed2State);
	}

	public static void writeStateSeq() throws IOException {
		System.out.println("Write state sequence");
	
		// Calculate most likely state sequence
		for (Game game : LogReader.expSet.games) {
			game.stateSeq = new HashMap<String, int[]>();
	
			for (String hitId : game.playerHitIds) {
				List<SigActObservation<CandySignal, CandyReport>> observList = game.getSignalReportPairList(hitId);
				ViterbiCalculator vi = new ViterbiCalculator(observList,
						LogReader.learntHmm);
				int[] stateSeq = vi.stateSequence();
				game.stateSeq.put(hitId, stateSeq);
			}
		}
	
		// Write state sequence to csv
		BufferedWriter writer = new BufferedWriter(new FileWriter(LogReader.rootDir
				+ "stateSeq" + LogReader.numStrategies + "States.csv"));
	
		writer.write(String.format("hitId,"));
		for (int i = 1; i <= LogReader.expSet.numRounds; i++) {
			writer.write(String.format("%d,", i));
		}
		writer.write(String.format("actual payoff\n"));
	
		for (Game game : LogReader.expSet.games) {
			for (String hitId : game.playerHitIds) {
				int[] stateSeq = game.stateSeq.get(hitId);
				writer.write(String.format("%s,", hitId));
				for (int state : stateSeq) {
					writer.write(state + ",");
				}
				writer.write(String.format("%.2f", game.bonus.get(hitId)));
				writer.write("\n");
			}
			writer.write("\n");
		}
		writer.flush();
		writer.close();
	
	}

	public static void eqConvergenceHmm() throws IOException {
		System.out.println("Classify equilibrium convergence using HMM");
	
		for (Game game : LogReader.expSet.games) {
	
			game.strategyComboTypeArray = new int[LogReader.expSet.numRounds];
	
			for (int roundIndex = 0; roundIndex < LogReader.expSet.numRounds; roundIndex++) {
				int[] numPlayerForStrategy = new int[LogReader.numStrategies];
				for (String hitId : game.playerHitIds) {
	
					int strategyIndexForRound = game.stateSeq.get(hitId)[roundIndex];
					numPlayerForStrategy[strategyIndexForRound]++;
				}
	
				if (!LogReader.treatment.equals("prior2-uniquetruthful")) {
					// other treatments
	
					for (int strategyIndex = 0; strategyIndex < LogReader.numStrategies; strategyIndex++) {
						if (numPlayerForStrategy[strategyIndex] == LogReader.expSet.numPlayers
								&& (strategyIndex == LogReader.mmState
										|| strategyIndex == LogReader.gbState || strategyIndex == LogReader.truthfulState)) {
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
					if (LogReader.truthfulState != -1
							&& numPlayerForStrategy[LogReader.truthfulState] == LogReader.numStrategies) {
						// truthful equilibrium
						game.strategyComboTypeArray[roundIndex] = LogReader.truthfulState;
					} else if (LogReader.mixedState != -1
							&& numPlayerForStrategy[LogReader.mixedState] == LogReader.numStrategies) {
						// mixed strategy equilibrium
						game.strategyComboTypeArray[roundIndex] = LogReader.mixedState;
					} else if (LogReader.mmState != -1 && LogReader.gbState != -1
							&& numPlayerForStrategy[LogReader.mmState] == 1
							&& numPlayerForStrategy[LogReader.gbState] == 3) {
						// 1 MM 3 GB
						game.strategyComboTypeArray[roundIndex] = 4;
					} else if (LogReader.mmState != -1 && LogReader.gbState != -1
							&& numPlayerForStrategy[LogReader.mmState] == 3
							&& numPlayerForStrategy[LogReader.gbState] == 1) {
						// 3 MM 1 GB
						game.strategyComboTypeArray[roundIndex] = 5;
					} else {
						// unclassified
						game.strategyComboTypeArray[roundIndex] = -1;
					}
	
				}
			}
		}
	
		// Write hmm type to CSV
		BufferedWriter writerCsv = new BufferedWriter(new FileWriter(LogReader.rootDir
				+ "hmmType" + LogReader.numStrategies + "Strategies.csv"));
		for (Game game : LogReader.expSet.games) {
			writerCsv.write(String.format("%s,", game.id));
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
	
		// Determine hmm type count
		int[][] hmmTypeCount = new int[LogReader.expSet.numRounds][LogReader.numStrategies + 2];
		for (Game game : LogReader.expSet.games) {
			for (int i = 0; i < LogReader.expSet.numRounds; i++) {
				if (game.strategyComboTypeArray[i] == -1)
					continue;
				else
					hmmTypeCount[i][game.strategyComboTypeArray[i]]++;
			}
		}
	
		// Write hmmTypeCount to CSV file
		writerCsv = new BufferedWriter(new FileWriter(LogReader.rootDir + "hmmTypeCount"
				+ LogReader.numStrategies + "Strategies.csv"));
	
		// write headings
		for (int j = 0; j < LogReader.numStrategies; j++) {
			writerCsv.write(String.format("%s,", LogReader.strategyNames[j]));
		}
		if (LogReader.treatment.equals("prior2-uniquetruthful")) {
			writerCsv.write(String.format("%s,", "1MM3GB"));
			writerCsv.write(String.format("%s,", "3MM1GB"));
		}
		writerCsv.write("\n");
	
		// write data
		for (int i = 0; i < LogReader.expSet.numRounds; i++) {
			for (int j = 0; j < LogReader.numStrategies; j++) {
				writerCsv.write(String.format("%d,", hmmTypeCount[i][j]));
			}
			if (LogReader.treatment.equals("prior2-uniquetruthful")) {
				writerCsv.write(String.format("%d,",
						hmmTypeCount[i][LogReader.numStrategies]));
				writerCsv.write(String.format("%d,",
						hmmTypeCount[i][LogReader.numStrategies + 1]));
			}
			writerCsv.write("\n");
		}
		writerCsv.flush();
		writerCsv.close();
	
		// Write hmmTypeCount to matlab file
		BufferedWriter writerMatlab = new BufferedWriter(new FileWriter(LogReader.rootDir
				+ "hmmType" + LogReader.numStrategies + "Strategies.m"));
	
		// write data arrays
		for (int j = 0; j < LogReader.numStrategies; j++) {
	
			if (LogReader.treatment.equals("prior2-symmlowpay")) {
				// treatment 4
				if (LogReader.strategyNames[j].equals("Mixed")
						|| LogReader.strategyNames[j].equals("Mixed2")) {
					continue;
				}
			} else if (LogReader.treatment.equals("prior2-uniquetruthful")) {
				// treatment 3, skip MM and GB strategies
				if (LogReader.strategyNames[j].equals("MM")
						|| LogReader.strategyNames[j].equals("GB")) {
					continue;
				}
			} else {
				// not treatment 3, skip mixed strategy
				if (LogReader.strategyNames[j].equals("Mixed")) {
					continue;
				}
			}
	
			writerMatlab.write(String.format("%s = [", LogReader.strategyNames[j]));
			for (int i = 0; i < LogReader.expSet.numRounds; i++) {
				double percent = hmmTypeCount[i][j] * 1.0 / LogReader.expSet.numGames;
				writerMatlab.write(String.format("%.10f ", percent));
			}
			writerMatlab.write("]';\n");
	
		}
		if (LogReader.treatment.equals("prior2-uniquetruthful")) {
			// write data for treatment 3
			writerMatlab.write(String.format("oneMMThreeGB = ["));
			for (int i = 0; i < LogReader.expSet.numRounds; i++) {
				double percent = hmmTypeCount[i][LogReader.numStrategies] * 1.0
						/ LogReader.expSet.numGames;
				writerMatlab.write(String.format("%.10f ", percent));
			}
			writerMatlab.write("]';\n");
	
			writerMatlab.write(String.format("threeMMOneGB = ["));
			for (int i = 0; i < LogReader.expSet.numRounds; i++) {
				double percent = hmmTypeCount[i][LogReader.numStrategies + 1] * 1.0
						/ LogReader.expSet.numGames;
				writerMatlab.write(String.format("%.10f ", percent));
			}
			writerMatlab.write("]';\n");
		}
	
		writerMatlab.write(String.format("Unclassified = ["));
		for (int i = 0; i < LogReader.expSet.numRounds; i++) {
			double num = 1.0;
			for (int j = 0; j < LogReader.numStrategies; j++) {
				num -= hmmTypeCount[i][j] * 1.0 / LogReader.expSet.numGames;
			}
			if (LogReader.treatment.equals("prior2-uniquetruthful")) {
				num -= hmmTypeCount[i][LogReader.numStrategies] * 1.0 / LogReader.expSet.numGames;
				num -= hmmTypeCount[i][LogReader.numStrategies + 1] * 1.0
						/ LogReader.expSet.numGames;
			}
			num -= 0.0000000001;
			writerMatlab.write(String.format("%.10f ", num));
		}
		writerMatlab.write("]';\n\n");
	
		writerMatlab.write("fH = figure;\n"
				+ "set(fH, 'Position', [300, 300, 500, 400]);\n");
	
		// plot bar chart
		writerMatlab.write(String.format("hBar = bar(0:%d, [",
				LogReader.expSet.numRounds - 1));
		if (LogReader.treatment.equals("prior2-basic")) {
			writerMatlab.write("MM GB Truthful");
		} else if (LogReader.treatment.equals("prior2-outputagreement")) {
			writerMatlab.write("GB MM Truthful");
		} else if (LogReader.treatment.equals("prior2-uniquetruthful")) {
			writerMatlab.write("Mixed threeMMOneGB oneMMThreeGB ");
		} else if (LogReader.treatment.equals("prior2-symmlowpay")) {
			writerMatlab.write("MM");
		} else {
			writerMatlab.write("Truthful GB MM");
		}
		writerMatlab.write(String.format("], 'BarWidth', 0.7, "
				+ "'BarLayout', 'stack', 'LineStyle', 'none');\n"));
	
		// labels
		int xylabelfontSize = 26;
		int axisFontSize = 20;
	
		String ytick = "";
		if (LogReader.treatment.equals("prior2-basic")) {
			ytick = "[0 0.45 0.54 1]";
		} else if (LogReader.treatment.equals("prior2-outputagreement")) {
			ytick = "[0 0.36 0.46 1]";
		} else if (LogReader.treatment.equals("prior2-uniquetruthful")) {
			ytick = "[0 0.12 0.17 1]";
		} else if (LogReader.treatment.equals("prior2-symmlowpay")) {
			ytick = "[0.02 1]";
		} else {
			ytick = "[0 1]";
		}
	
		writerMatlab.write(String.format("box off;\n"
				+ "set(gca,'Position',[.03 .15 .8 .8]);\n"
				+ "xlh = xlabel('Round');\n" + "set(xlh, 'FontSize', %d);\n"
				+ "axes = findobj(gcf,'type','axes');\n"
				+ "set(axes, 'FontSize', %d);\n"
				+ "set(axes, 'XTick', [0 %d]);\n" + "B = axes;\n"
				+ "set(B, 'yaxislocation', 'right', 'ytick', %s);\n"
				+ "ylh = ylabel('Percentage of games');\n"
				+ "set(ylh, 'FontSize', %d);\n", xylabelfontSize, axisFontSize,
				LogReader.expSet.numRounds - 1, ytick, xylabelfontSize));
	
		// set axis range
		writerMatlab.write(String.format("axis([-1 %d 0 1]);\n",
				LogReader.expSet.numRounds));
	
		// write legend
		writerMatlab.write("lh = legend(");
	
		if (LogReader.treatment.equals("prior2-uniquetruthful")) {
			writerMatlab.write("'Mixed', 'threeMMOneGB', 'oneMMThreeGB', ");
		} else if (LogReader.treatment.equals("prior2-symmlowpay")) {
			writerMatlab.write("'MM', ");
		} else if (LogReader.treatment.equals("prior2-outputagreement")) {
			writerMatlab.write("'GB', 'MM', 'Truthful', ");
		} else {
			writerMatlab.write("'MM', 'GB', 'Truthful', ");
		}
		writerMatlab.write(String.format("'Location', 'Best');\n"
				+ "set(lh, 'FontSize', 20);\n"));
	
		// set colors
		writerMatlab.write(String.format("set(hBar,{'FaceColor'},{"));
		if (LogReader.treatment.equals("prior2-uniquetruthful")) {
			writerMatlab.write("[0.6 0.6 0.6];'y';'r';");
		} else if (LogReader.treatment.equals("prior2-symmlowpay")) {
			writerMatlab.write("[1 0.64 0];");
		} else if (LogReader.treatment.equals("prior2-outputagreement")) {
			writerMatlab.write("'b';[1 0.64 0];'g';");
		} else {
			writerMatlab.write("[1 0.64 0];'b';'g';");
		}
		writerMatlab.write(String.format("});\n"));
	
		writerMatlab.flush();
		writerMatlab.close();
	
	}

	public static void writeStrategyChangeHeatMap() throws IOException {
		System.out.println("Write hmm strategy change heat map");
	
		BufferedWriter writer1 = new BufferedWriter(new FileWriter(LogReader.rootDir
				+ "heatMap" + LogReader.numStrategies + "StrategiesReverseCompare.m"));
	
		List<int[]> seqList = new ArrayList<int[]>();
		for (Game game : LogReader.expSet.games) {
			for (String hitId : game.playerHitIds) {
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
						return util(o1, o2, index - 1);
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
					if (seq[LogReader.expSet.numRounds - 1] == endStrIndex) {
						blockFound = true;
						inBlock = true;
					}
				} else {
					if (seq[LogReader.expSet.numRounds - 1] != endStrIndex)
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
			writer1.write(String.format("x%d = %s;\n", index,
					Arrays.toString(strategySeq)));
			index++;
		}
	
		index = 0;
		writer1.write("m = [");
		for (int i = 0; i < seqList.size(); i++) {
			writer1.write(String.format("x%d;", index));
			index++;
		}
		writer1.write("];\n\n");
		writer1.write("api_path = '~/plotly';\n"
				+ "addpath(genpath(api_path));\n" + "api_key = 'vivdmp4vf4';\n"
				+ "username = 'alice.gao';\n" + "signin(username, api_key);\n");
	
		String truthfulColor = "50,205,50";
		String gbColor = "0,0,255";
		String mixedColor = "190,190,190";
		String mixed2Color = "127,127,127";
		String mmColor = "255,140,0";
	
		writer1.write("cs={{");
		for (int i = 0; i <= 3; i++) {
			double num = i * 1.0 / 3;
			if (LogReader.mmState == i) {
				writer1.write(String.format("{%.2f, 'rgb(%s)'},", num, mmColor));
			} else if (LogReader.gbState == i) {
				writer1.write(String.format("{%.2f, 'rgb(%s)'},", num, gbColor));
			} else if (LogReader.truthfulState == i) {
				writer1.write(String.format("{%.2f, 'rgb(%s)'},", num,
						truthfulColor));
			} else if (LogReader.mixedState == i) {
				writer1.write(String.format("{%.2f, 'rgb(%s)'},", num,
						mixedColor));
			} else if (LogReader.mixed2State == i) {
				writer1.write(String.format("{%.2f, 'rgb(%s)'},", num,
						mixed2Color));
			}
		}
		writer1.write("}};\n");
		writer1.write("plotly({struct('z', m,'scl',cs,'type', 'heatmap')})\n");
		writer1.flush();
		writer1.close();
	}

	public static void writeStrategyDistribution() throws IOException {
		System.out.println("Write hmm strategy distribution");
	
		int[][] strategyCount = new int[LogReader.numStrategies][LogReader.expSet.numRounds];
		for (Game game : LogReader.expSet.games) {
			for (String hitId : game.playerHitIds) {
				int[] strategySeq = game.stateSeq.get(hitId);
				for (int roundIndex = 0; roundIndex < LogReader.expSet.numRounds; roundIndex++) {
					int strategyIndex = strategySeq[roundIndex];
					strategyCount[strategyIndex][roundIndex]++;
				}
			}
		}
		double[][] strategyDistribution = new double[LogReader.numStrategies][LogReader.expSet.numRounds];
		int totalNumPlayers = LogReader.expSet.numGames * LogReader.expSet.numPlayers;
		for (int roundIndex = 0; roundIndex < LogReader.expSet.numRounds; roundIndex++) {
			for (int strategyIndex = 0; strategyIndex < LogReader.numStrategies; strategyIndex++) {
				strategyDistribution[strategyIndex][roundIndex] = strategyCount[strategyIndex][roundIndex]
						* 1.0 / totalNumPlayers;
			}
		}
	
		BufferedWriter writer = new BufferedWriter(new FileWriter(LogReader.rootDir
				+ "strategyDistribution" + LogReader.numStrategies + "Strategies.m"));
	
		for (int strategyIndex = 0; strategyIndex < LogReader.numStrategies; strategyIndex++) {
			writer.write(LogReader.strategyNames[strategyIndex] + " = [");
			for (int roundIndex = 0; roundIndex < LogReader.expSet.numRounds; roundIndex++) {
				writer.write(String.format("%.10f ",
						strategyDistribution[strategyIndex][roundIndex]));
			}
			writer.write("]';\n");
		}
	
		writer.write(String.format("\n\n" + "figure;\n" + "hBar = bar(1:%d, ",
				LogReader.expSet.numRounds));
		if (LogReader.treatment.equals("prior2-symmlowpay")) {
			if (LogReader.numStrategies == 4)
				writer.write("[Truthful MM Mixed Mixed2]");
			else if (LogReader.numStrategies == 3)
				writer.write("[Truthful MM Mixed]");
		} else {
			writer.write("[Truthful GB MM Mixed]");
		}
		writer.write(", 0.5, 'stack');\n");
	
		writer.write("xlh = xlabel('Round');\n"
				+ "ylh = ylabel('Percentage of players');\n"
				+ "set(xlh, 'FontSize', 26);\n" + "set(ylh, 'FontSize', 26);\n"
				+ "axes = findobj(gcf,'type','axes');\n"
				+ "set(axes, 'FontSize', 20);\n");
	
		writer.write(String.format("axis([0 %d 0 1]);\n", LogReader.expSet.numRounds + 1));
	
		writer.write("lh = legend(");
		if (LogReader.treatment.equals("prior2-symmlowpay")) {
			if (LogReader.numStrategies == 4)
				writer.write("'Truthful', 'MM', 'Mixed', 'Mixed2'");
			else if (LogReader.numStrategies == 3)
				writer.write("'Truthful', 'MM', 'Mixed'");
		} else {
			writer.write("'Truthful', 'GB', 'MM', 'Mixed'");
		}
		writer.write(", 'Location', 'Best');\n" + "set(lh, 'FontSize', 20);\n");
	
		if (LogReader.treatment.equals("prior2-symmlowpay")) {
			if (LogReader.numStrategies == 4)
				writer.write("set(hBar,{'FaceColor'},{'g';[1 0.64 0];[0.5 0.5 0.5];[0.8 0.8 0.8];});\n");
			else if (LogReader.numStrategies == 3)
				writer.write("set(hBar,{'FaceColor'},{'g';[1 0.64 0];[0.8 0.8 0.8];});\n");
		} else {
			writer.write("set(hBar,{'FaceColor'},{'g';'b';[1 0.64 0];[0.8 0.8 0.8];});\n");
		}
		writer.flush();
		writer.close();
	}

	public static void genStrategyChangePredictedByHmm() throws IOException {
		System.out.println("Write strategy change predicted by HMM");
	
		BufferedWriter writer3 = new BufferedWriter(new FileWriter(LogReader.rootDir
				+ "strategyChangePredictedByHmm.m"));
	
		writer3.write("a = [");
		for (int i = 0; i < LogReader.numStrategies; i++) {
			for (int j = 0; j < LogReader.numStrategies; j++) {
				double aij = LogReader.learntHmm.getAij(i, j);
				writer3.write(" " + aij);
				if (j < LogReader.numStrategies - 1)
					writer3.write(",");
				else
					writer3.write(";");
			}
		}
		writer3.write("];\n");
	
		writer3.write("p = [");
		for (int i = 0; i < LogReader.numStrategies; i++) {
			writer3.write(LogReader.learntHmm.getPi(i) + ",");
		}
		writer3.write("];\n");
	
		writer3.write("m = zeros(50," + LogReader.numStrategies + ");\n"
				+ "m(1,:) = p;\n" + "for i =2:50\n" + "m(i,:) = m(i-1,:)*a;\n"
				+ "end\n" + "x = 1:50;\n" + "plot(");
		for (int i = 1; i <= LogReader.numStrategies; i++) {
			writer3.write("x, m(:," + i + ")");
			if (i < LogReader.numStrategies)
				writer3.write(",");
		}
		writer3.write(")\n");
	
		writer3.write("legend(");
		for (int i = 0; i < LogReader.numStrategies; i++) {
			writer3.write("'" + LogReader.strategyNames[i] + "'");
			if (i < LogReader.numStrategies - 1)
				writer3.write(",");
		}
		writer3.write(")");
	
		writer3.flush();
		writer3.close();
	}

	public static void graphLogLikelihood() throws IOException {
		System.out.println("Graph log likelihood");
	
		List<List<SigActObservation<CandySignal, CandyReport>>> seq = HMMAnalysis
				.getActObsSequence(LogReader.expSet.games);
		double loglk;
	
		BufferedWriter writer = new BufferedWriter(new FileWriter(LogReader.rootDir
				+ "logLikelihood.m"));
	
		if (LogReader.treatment.equals("prior2-basic"))
			writer.write("treatment1loglk = [");
		else if (LogReader.treatment.equals("prior2-outputagreement"))
			writer.write("treatment2loglk = [");
		else if (LogReader.treatment.equals("prior2-uniquetruthful"))
			writer.write("treatment3loglk = [");
		else if (LogReader.treatment.equals("prior2-symmlowpay"))
			writer.write("treatment4loglk = [");
		else if (LogReader.treatment.equals("prior2-constant"))
			writer.write("treatment5loglk = [");
	
		for (int numStates = 2; numStates <= 6; numStates++) {
	
			String filename = String.format("%slearntHMM%dstrategies.txt",
					LogReader.rootDir, numStates);
			Hmm<SigActObservation<CandySignal, CandyReport>> savedHmm = HMMAnalysis.createHMMFromFile(filename);
			loglk = BWToleranceLearner.computeLogLk(savedHmm, seq);
	
			writer.write(String.format("%.6f ", loglk));
		}
		writer.write("];\n");
	
		System.out.println("Graph Bayesian information criterion");
		double bic;
	
		if (LogReader.treatment.equals("prior2-basic"))
			writer.write("treatment1bic = [");
		else if (LogReader.treatment.equals("prior2-outputagreement"))
			writer.write("treatment2bic = [");
		else if (LogReader.treatment.equals("prior2-uniquetruthful"))
			writer.write("treatment3bic = [");
		else if (LogReader.treatment.equals("prior2-symmlowpay"))
			writer.write("treatment4bic = [");
		else if (LogReader.treatment.equals("prior2-constant"))
			writer.write("treatment5bic = [");
	
		for (int numStates = 2; numStates <= 6; numStates++) {
	
			String filename = String.format("%slearntHMM%dstrategies.txt",
					LogReader.rootDir, numStates);
			Hmm<SigActObservation<CandySignal, CandyReport>> savedHmm = HMMAnalysis.createHMMFromFile(filename);
			loglk = BWToleranceLearner.computeLogLk(savedHmm, seq);
	
			int numParams = (numStates * numStates + 2 * numStates - 1);
			int numData = LogReader.expSet.numGames * LogReader.expSet.numPlayers
					* LogReader.expSet.numRounds;
			bic = -2 * loglk + numParams * Math.log(numData);
	
			writer.write(String.format("%.6f ", bic));
		}
		writer.write("];\n");
		writer.flush();
		writer.close();
	
	}

	static List<List<SigActObservation<CandySignal, CandyReport>>> getActObsSequence(
			List<Game> games) {
		List<List<SigActObservation<CandySignal, CandyReport>>> seq = new ArrayList<List<SigActObservation<CandySignal, CandyReport>>>();
		for (Game game : games) {
			for (String hitId : game.playerHitIds) {
				List<SigActObservation<CandySignal, CandyReport>> list = game.getSignalReportPairList(hitId);
				seq.add(list);
			}
		}
		return seq;
	}

	static double[] calcSteadyStateProb(
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

	static void saveHMMDataToFile(String fileName,
			Hmm<SigActObservation<CandySignal, CandyReport>> hmmToSave)
			throws IOException {
	
		BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
		int numStates = hmmToSave.nbStates();
		writer.write(String.format("numStates:%d\n", numStates));
	
		for (int i = 0; i < numStates; i++) {
			writer.write(String.format("pi,%d:%.17f\n", i, hmmToSave.getPi(i)));
		}
	
		for (int i = 0; i < numStates; i++) {
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf = hmmToSave
					.getOpdf(i);
			double reportMMGivenSignalMM = opdf
					.probability(new SigActObservation<CandySignal, CandyReport>(
							CandySignal.MM, CandyReport.MM));
			double reportMMGivenSignalGB = opdf
					.probability(new SigActObservation<CandySignal, CandyReport>(
							CandySignal.GB, CandyReport.MM));
			writer.write(String.format("opdf,%d:%.17f,%.17f\n", i,
					reportMMGivenSignalMM, reportMMGivenSignalGB));
		}
	
		for (int i = 0; i < numStates; i++) {
			for (int j = 0; j < numStates; j++) {
				writer.write(String.format("aij,%d,%d:%.17f\n", i, j,
						hmmToSave.getAij(i, j)));
			}
		}
		writer.flush();
		writer.close();
	}

	static Hmm<SigActObservation<CandySignal, CandyReport>> createHMMFromFile(
			String filename) throws IOException {
	
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		Matcher matcher = Pattern.compile("numStates:(.*)").matcher(line);
		int numStates = -1;
		if (matcher.matches())
			numStates = Integer.parseInt(matcher.group(1));
	
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
	
		List<OpdfStrategy<CandySignal, CandyReport>> opdfs = new ArrayList<OpdfStrategy<CandySignal, CandyReport>>();
	
		for (int i = 0; i < numStates; i++) {
			line = reader.readLine();
			matcher = Pattern.compile("opdf,(.*):(.*),(.*)").matcher(line);
			if (matcher.matches()) {
				double[][] probs = new double[2][2];
				probs[0][0] = Double.parseDouble(matcher.group(2));
				probs[0][1] = 1 - probs[0][0];
				probs[1][0] = Double.parseDouble(matcher.group(3));
				probs[1][1] = 1 - probs[1][0];
				OpdfStrategy<CandySignal, CandyReport> opdf = new OpdfStrategy<CandySignal, CandyReport>(
						CandySignal.class, CandyReport.class,
						Utils.signalPrior, probs);
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
					matcher = Pattern.compile("aij,(.*),(.*):(.*)").matcher(
							line);
					if (matcher.matches()) {
						int ii = Integer.parseInt(matcher.group(1));
						int jj = Integer.parseInt(matcher.group(2));
						double prob = Double.parseDouble(matcher.group(3));
						aij[ii][jj] = prob;
					}
				}
			}
		}
		reader.close();
		Hmm<SigActObservation<CandySignal, CandyReport>> hmm = new Hmm<SigActObservation<CandySignal, CandyReport>>(
				pi, aij, opdfs);
		return hmm;
	}

}
