package edu.harvard.econcs.peerprediction;

import java.util.HashMap;
import java.util.Map;

import net.andrewmao.misc.Pair;

public class PaymentRule {

	PeerPrior prior;

	Map<Pair<String, String>, Double> rules;
	
	/**
	 * Default constructor
	 */
	public PaymentRule() {
		rules = new HashMap<Pair<String, String>, Double>();
	}
	
	// TODO: payment rule should be related to the prior
	
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
	public double[] getPaymentArray(String[] signals) {
		double[] array = new double[signals.length * signals.length];
		
		for (int i = 0; i < signals.length; i++) {
			for (int j = 0; j < signals.length; j++) {
				int index = i * 2 + j;
				array[index] = rules.get(new Pair<String, String>(signals[i], signals[j]));
			}
		}
		
		return array;
	}
	
}
