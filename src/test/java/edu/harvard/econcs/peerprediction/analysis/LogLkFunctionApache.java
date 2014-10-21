package edu.harvard.econcs.peerprediction.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.MultivariateFunction;

public class LogLkFunctionApache implements MultivariateFunction {

	List<Game> games;
	String model;
	double penCoeff;

	public LogLkFunctionApache(List<Game> g, String mod) {
		games = g;
		model = mod;
		penCoeff = 2;
	}

	public void squarePenCoeff() {
		penCoeff = Math.pow(penCoeff, 2);
	}

	@Override
	public double value(double[] point) {

		Map<String, Object> params = new HashMap<String, Object>();
		double loglk = Double.NEGATIVE_INFINITY;

		if (model.startsWith("S2")) {

			params.put("probTr", point[1]);
			params.put("probMM", point[2]);
			params.put("probGB", point[3]);
			params.put("eps", 	 point[3]);
			params.put("delta",  point[4]);
			String[] givenParams = model.split("-");
			double switchRound = Double.parseDouble(givenParams[1]);
			params.put("switchRound", switchRound);

			loglk = LearningModelsCustom.computeLogLkS3(params, games);
			if (point[0] + point[1] + point[2] > 1)
				loglk = loglk - penCoeff
						* Math.pow(point[0] + point[1] + point[2] - 1, 2);

		} else if (model.startsWith("S1")) {

			params.put("probTr", point[0]);
			params.put("probMM", point[1]);
			params.put("probGB", point[2]);
			params.put("eps", 	 point[3]);

			loglk = LearningModelsCustom.computeLogLkS1(params, games);
			if (point[0] + point[1] + point[2] > 1)
				loglk = loglk - penCoeff
						* Math.pow(point[0] + point[1] + point[2] - 1, 2);
			
		} else if (model.equals("RLS")) {

			params.put("considerSignal", true);
			params.put("phi", 	 point[0]);
			params.put("lambda", point[1]);
			loglk = LearningModelsExisting.computeLogLkRL(params, games);

		} else if (model.equals("RLNS")) {

			params.put("considerSignal", false);
			params.put("phi", 	 point[0]);
			params.put("lambda", point[1]);
			loglk =  LearningModelsExisting.computeLogLkRL(params, games);

		} else if (model.equals("SFPS")) {

			params.put("considerSignal", true);
			params.put("rho", 	 point[0]);
			params.put("lambda", point[1]);
			loglk = LearningModelsExisting.computeLogLkSFP(params, games);

		} else if (model.equals("SFPNS")) {

			params.put("considerSignal", false);
			params.put("rho", 	 point[0]);
			params.put("lambda", point[1]);
			loglk = LearningModelsExisting.computeLogLkSFP(params, games);

		} else if (model.equals("EWAS")) {

			params.put("considerSignal", true);
			params.put("rho", 	 point[0]);
			params.put("phi", 	 point[1]);
			params.put("delta",  point[2]);
			params.put("lambda", point[3]);
			loglk =  LearningModelsExisting.computeLogLkEWA(params, games);

		} else if (model.equals("EWANS")) {

			params.put("considerSignal", false);
			params.put("rho", 	 point[0]);
			params.put("phi", 	 point[1]);
			params.put("delta",  point[2]);
			params.put("lambda", point[3]);
			loglk = LearningModelsExisting.computeLogLkEWA(params, games);

		}

		return loglk;
	}

}
