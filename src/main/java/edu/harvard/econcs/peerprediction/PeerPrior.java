package edu.harvard.econcs.peerprediction;

import java.util.Map;
import java.util.Random;

import net.andrewmao.math.RandomSelection;

import com.google.common.collect.ImmutableMap;

public class PeerPrior {

	/**
	 * 
	 */
	public static final int nWorlds = 2;

	/**
	 * 
	 */
	public static final String[] signals = new String[]{"MM", "GM"};

	/**
	 * 
	 */
	public static final double[] priorOnWorlds = new double[] {0.3, 0.7};

	/**
	 * 
	 */
	public static final Map<String, Double> probs1 = ImmutableMap.of(
			"MM", 0.4,
			"GM", 0.6);
	
	/**
	 * 
	 */
	public static final Map<String, Double> probs2 = ImmutableMap.of(
			"MM", 0.8,
			"GM", 0.2);	
	
	/**
	 * 
	 */
	public static final Object[] worlds = new Object[]{probs1, probs2};

	/**
	 * 
	 * @return
	 */
	public static Map<String, Double> chooseWorld(){
		Random rnd = new Random();

		int worldIdx = RandomSelection.selectRandomWeighted(priorOnWorlds, rnd);
		Map<String, Double> probs = null;
		if (worldIdx == 0)
			probs = probs1;
		else
			probs = probs2;
		return probs;
	}
	
	
}
