package edu.harvard.econcs.peerprediction;

import java.util.HashMap;
import java.util.Map;

import net.andrewmao.misc.Pair;

public class PaymentRule {
	
	Map<Pair<String, String>, Double> rules;
	
	/**
	 * Default constructor
	 */
	public PaymentRule() {
		rules = new HashMap<Pair<String, String>, Double>();
	}
	
	/**
	 * Constructor
	 * @param rules
	 */
	public PaymentRule(Map<Pair<String, String>, Double> rules) {
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
		rules.put(new Pair<String, String>(myReport, otherReport), myPayment);
	}
	
	/**
	 * Get payment for a report pair
	 * @param myReport
	 * @param otherReport
	 * @return
	 */
	public double getPayment(String myReport, String otherReport) {
		return rules.get(new Pair<String, String>(myReport, otherReport));
	}

	/**
	 * Get the payment rule in array form
	 * @return
	 */
	public double[] getPaymentArray() {
		double[] array = new double[PeerPrior.signals.length * PeerPrior.signals.length];
		
		for (int i = 0; i < PeerPrior.signals.length; i++) {
			for (int j = 0; j < PeerPrior.signals.length; j++) {
				int index = i * 2 + j;
				array[index] = rules.get(new Pair<String, String>(PeerPrior.signals[i], PeerPrior.signals[j]));
			}
		}
		
		return array;
	}
	
}
