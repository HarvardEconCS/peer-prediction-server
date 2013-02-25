package edu.harvard.econcs.peerprediction;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.peerprediction.PaymentRule;
import edu.harvard.econcs.peerprediction.PeerPrior;

public class PaymentRuleTest {

	double eps = 0.00000000000001;

	PaymentRule rule;
	PaymentRule rule2;
	
	PeerPrior prior;
	
	@Before
	public void setup() {
		
		prior = PeerPrior.getTestPrior();
		
		rule = new PaymentRule();
		rule.addRule("MM", "MM", 0.5);
		rule.addRule("MM", "GB", 0.1);
		rule.addRule("GB", "MM", 0.23);
		rule.addRule("GB", "GB", 0.43);
		
		rule2 = new PaymentRule(prior);
		
	}
	
	@Test
	public void testPaymentFromPrior() {
		
		assertEquals(0.50, rule2.getPayment("MM", "MM"), eps);
		assertEquals(0.10, rule2.getPayment("MM", "GB"), eps);
		assertEquals(0.23, rule2.getPayment("GB", "MM"), eps);
		assertEquals(0.43, rule2.getPayment("GB", "GB"), eps);
	}
	
	@Test
	public void testGetPayment() {

		assertEquals(rule.getPayment("MM", "MM"), 0.5, eps);
		assertEquals(rule.getPayment("MM", "GB"), 0.1, eps);
		assertEquals(rule.getPayment("GB", "MM"), 0.23, eps);
		assertEquals(rule.getPayment("GB", "GB"), 0.43, eps);
		
	}

	@Test
	public void testPaymentArray() {
		
		double[] array = rule.getPaymentArray();
		assertEquals(array[0], 0.5, eps);
		assertEquals(array[1], 0.1, eps);
		assertEquals(array[2], 0.23, eps);
		assertEquals(array[3], 0.43, eps);
		
		assertEquals(rule.rules.size(), 4, 0);
		
	
	}
	
}
