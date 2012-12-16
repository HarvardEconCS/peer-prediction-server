package edu.harvard.econcs.peerprediction;

import java.util.HashMap;
import java.util.Map;

import net.andrewmao.misc.Pair;

public class PaymentRule {

	PeerPrior prior;
	
	Map<Pair<String, String>, Double> rules;
	
	public PaymentRule() {
		rules = new HashMap<Pair<String, String>, Double>();
		prior = PeerPrior.getTestPrior();
	}

	public PaymentRule(
			PeerPrior givenPrior,
			Map<Pair<String, String>, Double> rules) {

		this();
		this.prior = givenPrior;
		this.rules.putAll(rules);

	}
	
	public static PaymentRule getTestPaymentRule() {
		PaymentRule rule = new PaymentRule();
		
		rule.addRule("MM", "MM", 0.58);
		rule.addRule("MM", "GM", 0.36);
		rule.addRule("GM", "MM", 0.43);
		rule.addRule("GM", "GM", 0.54);
		
		return rule;
	}
	
	public PaymentRule(PeerPrior prior) {
		// TODO: payment rule should be related to the prior
	}

	public void addRule(String myReport, String otherReport, double myPayment) {
		rules.put(new Pair<String, String>(myReport, otherReport), myPayment);
	}
	
	public double getPayment(String myReport, String otherReport) {
		Pair<String, String> key = new Pair<String, String>(myReport, otherReport);
		return rules.get(key);
	}

	public double[] getPaymentArray() {
		
		String[] signals = prior.getSignalArray();
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
