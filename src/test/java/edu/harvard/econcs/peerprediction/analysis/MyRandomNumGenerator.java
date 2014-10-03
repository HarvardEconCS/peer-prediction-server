package edu.harvard.econcs.peerprediction.analysis;

import java.util.Random;

import org.apache.commons.math3.random.RandomVectorGenerator;

public class MyRandomNumGenerator implements RandomVectorGenerator {

	Random rand = new Random();
	int length;
	double[] lb;
	double[] ub;
	public MyRandomNumGenerator(int l, double[] lb, double[] ub) {
		length = l;
		this.lb = lb;
		this.ub = ub;
	}
	
	@Override
	public double[] nextVector() {
		double[] vec = new double[length];
		for (int i = 0; i < length; i++) {
			vec[i] = lb[i] + rand.nextDouble() * (ub[i] - lb[i]); 
		}
		return vec;
	}

}
