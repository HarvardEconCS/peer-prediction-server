package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.andrewmao.math.RandomSelection;

import com.google.common.collect.ImmutableMap;

public class PeerPrior {

	private String[] signals;
	private double[] priorOnWorlds;
	private List<Map<String, Double>> worlds;
	
	private Random rnd = new Random();
	
	/**
	 * Prior used for the experiment
	 * @return
	 */
	public static PeerPrior getTestPrior() {
		double[] priorOnWorlds = new double[] {0.5, 0.5};
		
		Map<String, Double> probs1 = ImmutableMap.of(
				"MM", 0.20,
				"GB", 0.80);
		Map<String, Double> probs2 = ImmutableMap.of(
				"MM", 0.70,
				"GB", 0.30);	
		
		List<Map<String, Double>> probs = new ArrayList<Map<String, Double>>();
		probs.add(probs1);
		probs.add(probs2);
		
		return new PeerPrior(priorOnWorlds, probs); 
	}

	public PeerPrior(
			double[] priorOnWorlds, 
			List<Map<String, Double>> ws) {

		this.priorOnWorlds = priorOnWorlds;
		
		this.worlds = new ArrayList<Map<String, Double>>();
		this.worlds.addAll(ws);
		
		
		Object[] signalObjArray = worlds.get(0).keySet().toArray();
		this.signals = new String[signalObjArray.length];
		System.arraycopy(signalObjArray, 0, signals, 0, signalObjArray.length);
	}
	
	public Map<String, Double> chooseWorld(){

		int worldIdx = RandomSelection.selectRandomWeighted(this.priorOnWorlds, rnd);
		Map<String, Double> probs = null;
		if (worldIdx == 0)
			probs = worlds.get(0);
		else
			probs = worlds.get(1);
		return probs;

	}
	
	public String chooseSignal(Map<String, Double> chosenWorld) {

		double[] probArray = new double[chosenWorld.size()];
		for (int i = 0; i < this.signals.length; i++) {
			probArray[i] = chosenWorld.get(this.signals[i]);
		}
		int signalIdx = RandomSelection.selectRandomWeighted(probArray, rnd);
		return this.signals[signalIdx];
		
	}
	
	
	public String[] getSignalArray() {
		return this.signals;
	}
	
	/**
	 * Return Pr(Signal)
	 * @param signal
	 * @return
	 */
	public double getProbForSignal(String signal) {
		double prob = 0;
		for (int i = 0; i < worlds.size(); i++) {
			prob += priorOnWorlds[i] * worlds.get(i).get(signal);
		}
		return prob;
	}
	
	/**
	 * Return Pr(Signal1, Signal2)
	 * @param signal1
	 * @param signal2
	 * @return
	 */
	public double getProbForSignalPair(String signal1, String signal2) {
		double prob = 0;
		for (int i = 0; i < worlds.size(); i++) {
			prob += priorOnWorlds[i] * worlds.get(i).get(signal1) * worlds.get(i).get(signal2);
		}
		return prob;
	}
	
	/**
	 * Return Pr(Signal1 | Signal2)
	 * @param signal1
	 * @param signal2
	 * @return
	 */
	public double getProbSignal1GivenSignal2(String signal1, String signal2) {
		double probNum  = this.getProbForSignalPair(signal1, signal2);
		double probDenom = this.getProbForSignal(signal2);
		return probNum / probDenom;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("prob=%s, ", Arrays.toString(priorOnWorlds)));
		sb.append(String.format("worlds=%s", worlds.toString()));
		return sb.toString();
	}
}
