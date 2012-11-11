package edu.harvard.econcs.peerprediction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.andrewmao.misc.Pair;

public class PaymentRule {
	
	Map<String, Map<String, Double>> rules;
	
	/**
	 * Default constructor
	 */
	public PaymentRule() {
		rules = new HashMap<String, Map<String, Double>>();
	}
	
	/**
	 * Constructor
	 * @param rules
	 */
	public PaymentRule(Map<String, Map<String, Double>> rules) {
		this();
		
		this.rules.putAll(rules);
	}

	/**
	 * Add rule
	 * @param myReport
	 * @param otherReport
	 * @param myPayment
	 */
	public void addRule(String myReport, String otherReport, double myPayment) {
		Map<String, Double> insideRule = new HashMap<String, Double>();
		insideRule.put(otherReport, myPayment);
		rules.put(myReport, insideRule);
	}
	
	/**
	 * Get payment for a report pair
	 * @param myReport
	 * @param otherReport
	 * @return
	 */
	public double getPayment(String myReport, String otherReport) {
		Map<String, Double> insideRule = rules.get(myReport);
		return insideRule.get(otherReport);
	}

	/**
	 * Get the payment rule in array form
	 * @return
	 */
	public double[] getPaymentArray() {
		double[] array = new double[PeerPrior.signals.length^2];
		
		for (int i = 0; i < PeerPrior.signals.length; i++) {
			for (int j = 0; j < PeerPrior.signals.length; j++) {
				int index = i * 2 + j;
				
				array[index] = rules.get(PeerPrior.signals[i]).get(PeerPrior.signals[j]); 
			}
		}
		
		return array;
	}
	
}
