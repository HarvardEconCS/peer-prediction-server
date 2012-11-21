package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
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
	
	public static PeerPrior getTestPrior() {
		
		String[] signals = new String[]{"MM", "GM"};
		double[] priorOnWorlds = new double[] {0.5, 0.5};
		
		Map<String, Double> probs1 = ImmutableMap.of(
				"MM", 0.4,
				"GM", 0.6);
		Map<String, Double> probs2 = ImmutableMap.of(
				"MM", 0.8,
				"GM", 0.2);	
		List<Map<String, Double>> probs = new ArrayList<Map<String, Double>>();
		probs.add(probs1);
		probs.add(probs2);
		
		return new PeerPrior(signals, priorOnWorlds, probs); 

	}
	

	public PeerPrior(
			String[] signals,
			double[] priorOnWorlds, 
			List<Map<String, Double>> ws) {

		this.signals = signals;

		this.priorOnWorlds = priorOnWorlds;
		
		this.worlds = new ArrayList<Map<String, Double>>();
		this.worlds.addAll(ws);

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
	
}
