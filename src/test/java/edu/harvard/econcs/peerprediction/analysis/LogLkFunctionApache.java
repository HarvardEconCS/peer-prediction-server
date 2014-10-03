package edu.harvard.econcs.peerprediction.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.MultivariateFunction;

public class LogLkFunctionApache implements MultivariateFunction {

	List<Game> games;
	String model;

	public LogLkFunctionApache(List<Game> g, String mod) {
		games = g;
		model = mod;
	}

	@Override
	public double value(double[] args) {

		Map<String, Object> params = new HashMap<String, Object>();

		if (model.equals("RLS")) {

			params.put("considerSignal", true);
			params.put("phi", args[0]);
			params.put("lambda", args[1]);
			return LogReader.computeLogLkRL(params, games);

		} else if (model.equals("RLNS")) {

			params.put("considerSignal", false);
			params.put("phi", args[0]);
			params.put("lambda", args[1]);
			return LogReader.computeLogLkRL(params, games);

		} else if (model.equals("SFPS")) {

			params.put("considerSignal", true);
			params.put("rho", args[0]);
			params.put("lambda", args[1]);
			return LogReader.computeLogLkSFP(params, games);

		} else if (model.equals("SFPNS")) {

			params.put("considerSignal", false);
			params.put("rho", args[0]);
			params.put("lambda", args[1]);
			return LogReader.computeLogLkSFP(params, games);

		} else if (model.equals("EWAS")) {

			params.put("considerSignal", true);
			params.put("rho", args[0]);
			params.put("phi", args[1]);
			params.put("delta", args[2]);
			params.put("lambda", args[3]);
			return LogReader.computeLogLkEWA(params, games);

		} else if (model.equals("EWANS")) {

			params.put("considerSignal", false);
			params.put("rho",    args[0]);
			params.put("phi",    args[1]);
			params.put("delta",  args[2]);
			params.put("lambda", args[3]);
			return LogReader.computeLogLkEWA(params, games);
			
		} else if (model.startsWith("S1")) {
			
			// parse k
			String[] givenParams = model.split("-");
			double switchRound = Double.parseDouble(givenParams[1]);
			params.put("switchRound", switchRound);
			
			params.put("diffThreshold", args[0]);
			params.put("probTruthful", 	args[1]);
			params.put("probMM", 		args[2]);
			params.put("probGB", 		args[3]);
			
			double loglk = LogReader.computeLogLkS1(params, games);
			return loglk;

		}

		return Double.NEGATIVE_INFINITY;
	}

}
