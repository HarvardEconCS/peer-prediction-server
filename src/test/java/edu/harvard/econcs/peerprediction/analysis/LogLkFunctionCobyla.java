package edu.harvard.econcs.peerprediction.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cureos.numerics.Calcfc;

public class LogLkFunctionCobyla implements Calcfc {

	List<Game> games;
	String model;
	double penCoeff;

	public LogLkFunctionCobyla(List<Game> g, String mod) {
		games = g;
		model = mod;
		penCoeff = 2;
	}

	/**
	 * n: number of variables m: number of constraints x: variable array con:
	 * array of calculated constraints function values
	 */
	@Override
	public double Compute(int n, int m, double[] point, double[] con) {

		Map<String, Object> params = new HashMap<String, Object>();
		double loglk = Double.NEGATIVE_INFINITY;
			
		if (model.startsWith("s3")) {

			// constraints
			con[0] = point[0];
			con[1] = point[1];
			con[2] = point[2];
			con[3] = point[3];
			con[4] = 1.0 - point[0] - point[1] - point[2] - point[3];
			con[5] = point[4];
			con[6] = LearningModelsCustom.getUBCobyla(model, "eps") - point[4];
			con[7] = point[5] - LearningModelsCustom.getLBCobyla(model, "delta");
			con[8] = LearningModelsCustom.getUBCobyla(model, "delta") - point[5];

			params = LearningModelsCustom.pointToMap(model, point);
			loglk = LearningModelsCustom.computeLogLkS3(params, games);
			loglk = addPenaltyTermsS3(point, loglk);
			
		} else if (model.equals("s1-1")) {
			
			double epsUB = LearningModelsCustom.getUBCobyla(model, "eps");
			double epsLB = LearningModelsCustom.getLBCobyla(model, "eps");
	
			// constraints
			con[0] = point[0];
			con[1] = point[1];
			con[2] = point[2];
			con[3] = point[3];
			con[4] = 1.0 - point[0] - point[1] - point[2] - point[3];
			con[5] = point[4] - epsLB;
			con[6] = epsUB - point[4];
			con[7] = point[5];
			con[8] = 1.0 - point[5];
			con[9] = point[6];
			con[10] = 1.0 - point[6];

			params = LearningModelsCustom.pointToMap(model, point);
			loglk = LearningModelsCustom.computeLogLkS1Dash1(params, games);
			loglk = addPenaltyTermsS1Dash1(point, loglk);
			
		} else if (model.equals("s1")) {
			
			double epsUB = LearningModelsCustom.getUBCobyla(model, "eps");
			double epsLB = LearningModelsCustom.getLBCobyla(model, "eps");
	
			// constraints
			con[0] = point[0];
			con[1] = point[1];
			con[2] = point[2];
			con[3] = point[3];
			con[4] = 1.0 - point[0] - point[1] - point[2] - point[3];
			con[5] = point[4] - epsLB;
			con[6] = epsUB - point[4];

			params = LearningModelsCustom.pointToMap(model, point);
			loglk = LearningModelsCustom.computeLogLkS1(params, games);
			loglk = addPenaltyTermsS1(point, loglk);

		} else if (model.equals("SFPS")) {

			con[0] = point[0];
			con[1] = 1 - point[0];
			con[2] = point[1] - 1;
			con[3] = 10 - point[1];

			params.put("considerSignal", true);
			params.put("rho", point[0]);
			params.put("lambda", point[1]);

			loglk = LearningModelsExisting.computeLogLkSFP(params, games);

		} else if (model.equals("RLS")) {

			con[0] = point[0];
			con[1] = 1 - point[0];
			con[2] = point[1] - 1;
			con[3] = 10 - point[1];

			params.put("considerSignal", true);
			params.put("phi", point[0]);
			params.put("lambda", point[1]);

			loglk = LearningModelsExisting.computeLogLkRL(params, games);

		}

		return -loglk; // because we are doing minimization
	}

	private double addPenaltyTermsS1Dash1(double[] point, double loglk) {
		double logLK = addPenaltyTermsS1(point, loglk);
		
		if (point[5] < 0)
			logLK = logLK - penCoeff * Math.pow(0.0 - point[5], 2);
		if (point[5] > 1)
			logLK = logLK - penCoeff * Math.pow(point[5] - 1.0, 2);
		if (point[6] < 0)
			logLK = logLK - penCoeff * Math.pow(0.0 - point[6], 2);
		if (point[6] > 1)
			logLK = logLK - penCoeff * Math.pow(point[6] - 1.0, 2);
	
		return logLK;
	}

	private double addPenaltyTermsS1(double[] point, double loglk) {
		
		if (point[0] < 0)
			loglk = loglk - penCoeff * Math.pow(0 - point[0], 2);
		if (point[1] < 0)
			loglk = loglk - penCoeff * Math.pow(0 - point[1], 2);
		if (point[2] < 0)
			loglk = loglk - penCoeff * Math.pow(0 - point[2], 2);
		if (point[3] < 0)
			loglk = loglk - penCoeff * Math.pow(0 - point[3], 2);
		
		if (point[0] + point[1] + point[2] + point[3] > 1)
			loglk = loglk - penCoeff * Math.pow(point[0] + point[1] + point[2] + point[3] - 1, 2);

		double epsUB = LearningModelsCustom.getUBCobyla(model, "eps");
		double epsLB = LearningModelsCustom.getLBCobyla(model, "eps");
		if (point[4] < epsLB)
			loglk = loglk - penCoeff * Math.pow(epsLB - point[4], 2);
		if (point[4] > epsUB)
			loglk = loglk - penCoeff * Math.pow(point[4] - epsUB, 2);
		
		return loglk;
	}

	private double addPenaltyTermsS3(double[] point, double loglk) {
		double logLK = addPenaltyTermsS1(point, loglk);
		if (point[5] < LearningModelsCustom.getLBCobyla(model, "delta"))
			logLK = logLK - penCoeff * Math.pow(LearningModelsCustom.getLBCobyla(model, "delta") - point[5], 2);
		return logLK;
	}

	public void squarePenCoeff() {
		penCoeff = penCoeff * penCoeff;
	}

}
