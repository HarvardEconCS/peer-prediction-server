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
		rule.addRule("MM", "0", 0.90);
		rule.addRule("MM", "1", 0.10);
		rule.addRule("MM", "2", 1.50);
		rule.addRule("MM", "3", 0.80);
		rule.addRule("GB", "0", 0.80);
		rule.addRule("GB", "1", 1.50);
		rule.addRule("GB", "2", 0.10);
		rule.addRule("GB", "3", 0.90);
		
	}
	
	@Test
	public void testGetPayment() {

		assertEquals(rule.getPayment("MM", "0"), 0.90, eps);
		assertEquals(rule.getPayment("MM", "1"), 0.10, eps);
		assertEquals(rule.getPayment("MM", "2"), 1.50, eps);
		assertEquals(rule.getPayment("MM", "3"), 0.80, eps);
		assertEquals(rule.getPayment("GB", "0"), 0.80, eps);
		assertEquals(rule.getPayment("GB", "1"), 1.50, eps);
		assertEquals(rule.getPayment("GB", "2"), 0.10, eps);
		assertEquals(rule.getPayment("GB", "3"), 0.90, eps);
		
	}

	@Test
	public void testPaymentArray() {
		
		double[][] array = rule.getPayment2DArray();
		assertEquals(array[0][0], 0.90, eps);
		assertEquals(array[0][1], 0.10, eps);
		assertEquals(array[0][2], 1.50, eps);
		assertEquals(array[0][3], 0.80, eps);
		
		assertEquals(rule.rules.size(), 8, 0);
	}
	
}
