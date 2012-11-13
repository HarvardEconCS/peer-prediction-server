package edu.harvard.econcs.peerprediction;

import java.util.Map;
import java.util.Random;

import net.andrewmao.math.RandomSelection;

import com.google.common.collect.ImmutableMap;

public class PeerPrior {

	/**
	 * number of worlds
	 */
	public static final int nWorlds = 2;

	/**
	 * the signal array
	 */
	public static final String[] signals = new String[]{"MM", "GM"};

	/**
	 * prior on the two worlds
	 */
	public static final double[] priorOnWorlds = new double[] {0.3, 0.7};

	/**
	 * world 1
	 */
	public static final Map<String, Double> probs1 = ImmutableMap.of(
			"MM", 0.4,
			"GM", 0.6);
	
	/**
	 * world 2
	 */
	public static final Map<String, Double> probs2 = ImmutableMap.of(
			"MM", 0.8,
			"GM", 0.2);	
	
	/**
	 * the two worlds
	 */
	public static final Object[] worlds = new Object[]{probs1, probs2};
	
	public static final Random rnd = new Random();
	
	/**
	 * Choose a world based on the prior
	 * @return
	 */
	public static Map<String, Double> chooseWorld(){
		int worldIdx = RandomSelection.selectRandomWeighted(priorOnWorlds, rnd);
		Map<String, Double> probs = null;
		if (worldIdx == 0)
			probs = probs1;
		else
			probs = probs2;
		return probs;
	}
	
	/**
	 * Choose signal given the chosen world
	 * @param chosenWorld
	 * @return
	 */
	public static String chooseSignal(Map<String, Double> chosenWorld) {
		double[] probArray = new double[chosenWorld.size()];
		for (int i = 0; i < PeerPrior.signals.length; i++) {
			probArray[i] = chosenWorld.get(PeerPrior.signals[i]);
		}
		int signalIdx = RandomSelection.selectRandomWeighted(priorOnWorlds, rnd);
		return PeerPrior.signals[signalIdx];
	}
	
}
