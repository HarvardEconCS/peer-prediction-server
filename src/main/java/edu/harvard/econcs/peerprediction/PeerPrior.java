package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.andrewmao.math.RandomSelection;

import com.google.common.collect.ImmutableMap;

public class PeerPrior {

	/**
	 * the signal array
	 */
	private String[] signals;

	/**
	 * prior on the two worlds
	 */
	private double[] priorOnWorlds;

	/**
	 * the two worlds
	 */
	private List<Map<String, Double>> worlds;
	
	private Random rnd = new Random();
	
	/**
	 * 
	 */
	public static PeerPrior getTestPrior() {
		
		String[] signals = new String[]{"MM", "GM"};
		double[] priorOnWorlds = new double[] {0.5, 0.5};
		Map<String, Double> probs1 = ImmutableMap.of(
				"MM", 0.4,
				"GM", 0.6);
		Map<String, Double> probs2 = ImmutableMap.of(
				"MM", 0.8,
				"GM", 0.2);	
		return new PeerPrior(signals, priorOnWorlds, probs1, probs2); 

	}
	
	/**
	 * 
	 * @param signals
	 * @param priorOnWorlds
	 * @param world1
	 * @param world2
	 */
	public PeerPrior(String[] signals, double[] priorOnWorlds, 
			Map<String, Double> world1, Map<String, Double> world2) {
		this.worlds = new ArrayList<Map<String, Double>>();
		worlds.add(world1);
		worlds.add(world2);
		this.priorOnWorlds = priorOnWorlds;
		this.signals = signals;
		
	}
	
	/**
	 * Choose a world based on the prior
	 * @return
	 */
	public Map<String, Double> chooseWorld(){
		int worldIdx = RandomSelection.selectRandomWeighted(this.priorOnWorlds, rnd);
		Map<String, Double> probs = null;
		if (worldIdx == 0)
			probs = worlds.get(0);
		else
			probs = worlds.get(1);
		return probs;
	}
	
	/**
	 * Choose signal given the chosen world
	 * @param chosenWorld
	 * @return
	 */
	public String chooseSignal(Map<String, Double> chosenWorld) {
		double[] probArray = new double[chosenWorld.size()];
		for (int i = 0; i < this.signals.length; i++) {
			probArray[i] = chosenWorld.get(this.signals[i]);
		}
		int signalIdx = RandomSelection.selectRandomWeighted(priorOnWorlds, rnd);
		return this.signals[signalIdx];
	}
	
	public String[] getSignalArray() {
		return this.signals;
	}
	
}
