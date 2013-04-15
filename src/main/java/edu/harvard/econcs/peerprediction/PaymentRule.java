package edu.harvard.econcs.peerprediction;

import java.util.HashMap;
import java.util.Map;

import net.andrewmao.misc.Pair;

public class PaymentRule {

	PeerPrior prior;
	
	Map<Pair<String, String>, Double> rules;
	
	/**
	 * Payment rule used by the experiment
	 * @return
	 */
	public static PaymentRule getTestPaymentRule() {
		PaymentRule rule = new PaymentRule();
		
		rule.addRule("MM", "MM", 1.50);
		rule.addRule("MM", "GB", 0.10);
		rule.addRule("GB", "MM", 0.40);
		rule.addRule("GB", "GB", 1.20);
		
		return rule;
	}

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
	
	public PaymentRule(PeerPrior prior) {
		rules = new HashMap<Pair<String, String>, Double>();
		String[] signalList = prior.getSignalArray();
		
		double currMax = Double.NEGATIVE_INFINITY;
		double currMin = Double.POSITIVE_INFINITY;
		for (int i = 0; i < signalList.length; i++) {
			for (int j = 0; j < signalList.length; j++) {
				double prob = prior.getProbSignal1GivenSignal2(signalList[j], signalList[i]);
				double reward = 2 * prob - prob * prob;
				this.addRule(signalList[i], signalList[j], reward);
				
				if (reward > currMax)
					currMax = reward;
				else if (reward < currMin)
					currMin = reward;
			}
		}
		
//		double scaleMax = 0.50;
		double scaleDiff = 0.40;
		double scaleMin = 0.10;
		
		double a = scaleDiff / (currMax - currMin);
		double b = currMin * a - scaleMin;
		
		for (Pair<String, String> pair : rules.keySet()) {
			double val = rules.get(pair);
			val = val * a - b;
			val = 1.0 * Math.round(val * 100) / 100;
			rules.put(pair, val);
		}
		
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
