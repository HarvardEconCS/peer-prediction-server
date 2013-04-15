package edu.harvard.econcs.peerprediction;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.peerprediction.PaymentRule;
import edu.harvard.econcs.peerprediction.PeerPrior;

public class TestPaymentRule {

	double eps = 0.00000000000001;

	PaymentRule rule;
	PaymentRule rule2;
	
	PeerPrior prior;
	
	@Before
	public void setup() {
		
		prior = PeerPrior.getTestPrior();
		
		rule = new PaymentRule();
		rule.addRule("MM", "MM", 1.50);
		rule.addRule("MM", "GB", 0.10);
		rule.addRule("GB", "MM", 0.40);
		rule.addRule("GB", "GB", 1.20);
		
	}
	
	@Test
	public void testGetPayment() {

		assertEquals(rule.getPayment("MM", "MM"), 1.50, eps);
		assertEquals(rule.getPayment("MM", "GB"), 0.10, eps);
		assertEquals(rule.getPayment("GB", "MM"), 0.40, eps);
		assertEquals(rule.getPayment("GB", "GB"), 1.20, eps);
		
	}

	@Test
	public void testPaymentArray() {
		
		double[] array = rule.getPaymentArray();
		assertEquals(array[0], 1.50, eps);
		assertEquals(array[1], 0.10, eps);
		assertEquals(array[2], 0.40, eps);
		assertEquals(array[3], 1.20, eps);
		
		assertEquals(rule.rules.size(), 4, 0);
	}
	
}
