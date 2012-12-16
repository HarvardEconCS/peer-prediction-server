package edu.harvard.econcs.peerprediction;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.peerprediction.PaymentRule;
import edu.harvard.econcs.peerprediction.PeerPrior;

public class PaymentRuleTest {

	double eps = 0.00000000000001;

	PaymentRule rule;
	
	PeerPrior prior;
	
	@Before
	public void setup() {
		
		prior = PeerPrior.getTestPrior();
		
		rule = new PaymentRule();
		rule.addRule("MM", "MM", 0.58);
		rule.addRule("MM", "GM", 0.36);
		rule.addRule("GM", "MM", 0.43);
		rule.addRule("GM", "GM", 0.54);
		
	}
	
	@Test
	public void testGetPayment() {

		assertEquals(rule.getPayment("MM", "MM"), 0.58, eps);
		assertEquals(rule.getPayment("MM", "GM"), 0.36, eps);
		assertEquals(rule.getPayment("GM", "MM"), 0.43, eps);
		assertEquals(rule.getPayment("GM", "GM"), 0.54, eps);
		
	}

	@Test
	public void testPaymentArray() {
		
		double[] array = rule.getPaymentArray();
		assertEquals(array[0], 0.58, eps);
		assertEquals(array[1], 0.36, eps);
		assertEquals(array[2], 0.43, eps);
		assertEquals(array[3], 0.54, eps);
		
		assertEquals(rule.rules.size(), 4, 0);
		
//		array = rule.getPaymentArray(new String[]{"GM", "MM"});
//		assertEquals(array[0], 0.54, eps);
//		assertEquals(array[1], 0.43, eps);
//		assertEquals(array[2], 0.36, eps);
//		assertEquals(array[3], 0.58, eps);
	
	}
	
}
