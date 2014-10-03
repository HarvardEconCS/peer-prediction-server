package edu.harvard.econcs.peerprediction.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cureos.numerics.Calcfc;

public class LogLkFunctionCobyla implements Calcfc {

	List<Game> games;
	String model;
	
	public LogLkFunctionCobyla(List<Game> g, String mod) {
		games = g;
		model = mod;
	}
	
	/**
	 * n: number of variables
	 * m: number of constraints
	 * x: variable array
	 * con: array of calculated constraints function values
	 */
	@Override
	public double Compute(int n, int m, double[] x, double[] con) {
		
		Map<String, Object> params = new HashMap<String, Object>();
		double loglk = Double.NEGATIVE_INFINITY;

		if (model.equals("SFPS")) {
			
			con[0] = x[0];
			con[1] = 1 - x[0];
			con[2] = x[1] - 1;
			con[3] = 10 - x[1];
			
			params.put("considerSignal", true);
			params.put("rho",    x[0]);
			params.put("lambda", x[1]);
			
			if (x[0] < 0 || x[0] > 1 || x[1] < 1 || x[1] > 10)
				loglk = Double.NEGATIVE_INFINITY;
			else
				loglk = LogReader.computeLogLkSFP(params, games);
			
		} else if (model.equals("RLS")) {
			
			con[0] = x[0];
			con[1] = 1 - x[0];
			con[2] = x[1] - 1;
			con[3] = 10 - x[1];
			
			params.put("considerSignal", true);
			params.put("phi",    x[0]);
			params.put("lambda", x[1]);
			
			if (x[0] < 0 || x[0] > 1 || x[1] < 1 || x[1] > 10)
				loglk = Double.NEGATIVE_INFINITY;
			else
				loglk = LogReader.computeLogLkRL(params, games);
			
		} else if (model.startsWith("S1")) {

			con[0] = 1.0 - x[0];
			con[1] = 1.0 - x[1] - x[2] - x[3]; // valid probabilities
			con[2] = x[0];
			con[3] = x[1];
			con[4] = x[2];
			con[5] = x[3];
			con[6] = 1.0 - x[1];
			con[7] = 1.0 - x[2];
			con[8] = 1.0 - x[3];

			String[] givenParams = model.split("-");
			double switchRound = Double.parseDouble(givenParams[1]);
			params.put("switchRound", switchRound);

			params.put("diffThreshold", x[0]);
			params.put("probTruthful", x[1]);
			params.put("probMM", x[2]);
			params.put("probGB", x[3]);
			
			if (x[0] > 1 || x[0] < 0 || 
					x[1] < 0 || x[2] < 0 || x[3] < 0 || 
					x[1] + x[2] + x[3] > 1 || 
					x[1] > 1 || x[2] > 1 || x[3] > 1)
				loglk = Double.NEGATIVE_INFINITY;
			else
				loglk = LogReader.computeLogLkS1(params, games);

		} else if (model.equals("S2")) {
			
			con[0] = 1.0 - x[0] - x[1] - x[2]; // valid probabilities
			con[1] = x[0];
			con[2] = x[1];
			con[3] = x[2];
			con[4] = 1.0 - x[0];
			con[5] = 1.0 - x[1];
			con[6] = 1.0 - x[2];

			params.put("probTruthful", x[0]);
			params.put("probMM", x[1]);
			params.put("probGB", x[2]);
			
			if (x[0] < 0 || x[1] < 0 || x[2] < 0 || 
					x[0] + x[1] + x[2] > 1 || 
					x[0] > 1 || x[1] > 1 || x[2] > 1)
				loglk = Double.NEGATIVE_INFINITY;
			else
				loglk = LogReader.computeLogLkS2(params, games);
			
		}

		return -loglk; // because we are doing minimization
	}

}
