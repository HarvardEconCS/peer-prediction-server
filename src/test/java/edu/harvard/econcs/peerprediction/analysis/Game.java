package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.andrewmao.models.games.SigActObservation;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Game {

	String id;

	List<Map<String, Double>> worlds;
	double[] priorProbs;

	int numRounds;
	int numPlayers;
	String[] playerHitIds;
	List<Round> rounds;
	
	double[] paymentArrayT1N2;

	Map<String, ExitSurvey> exitSurvey;
	
	Map<String, Double> actualPayoff;
	Map<String, Double> simulatedFPPayoff;
	Map<String, Double> simuatedBRPayoff;
	
	Map<String, List<SigActObservation<CandySignal, CandyReport>>> signalReportObjList;
	Map<String, int[]> stateSeq;
		
	Gson gson = new Gson();
	
	String convergenceType;
	int roundConverged;
	String convergenceTypeRelaxed;
	int roundConvergedRelaxed;

	int[] strategyComboTypeArray;

	public Game() {

		worlds = new ArrayList<Map<String, Double>>();
		rounds = new ArrayList<Round>();
		exitSurvey = new HashMap<String, ExitSurvey>();
				
		actualPayoff = new HashMap<String, Double>();
		simulatedFPPayoff = new HashMap<String, Double>();
		simuatedBRPayoff = new HashMap<String, Double>();
	
		convergenceType = "";
	}

	public void addRound(Round round) {
		rounds.add(round);
	}

	public void savePriorProb(String probString) {
		priorProbs = gson.fromJson(probString, double[].class);
	}

	public void savePriorWorlds(String worldsString) {
		Object[] worldsArray = gson.fromJson(worldsString, Object[].class);
		for (int i = 0; i < worldsArray.length; i++) {
			Map<String, Double> worldMap = gson.fromJson(
					worldsArray[i].toString(),
					new TypeToken<Map<String, Double>>() {
					}.getType());
			worlds.add(worldMap);
		}
	}

	public void savePlayerHitIds(String playerNamesString) {
		playerHitIds = gson.fromJson(playerNamesString, String[].class);
	}

	public void savePaymentRule(String paymentRuleString) {
		if (numPlayers == 3)
			paymentArrayT1N2 = gson.fromJson(paymentRuleString, double[].class);	
	}
	
	public int getCandyStart(String hitId, String candy) {
		for (int i = rounds.size() - 1; i >= 0; i--) {
			if (rounds.get(i).getReport(hitId).equals(candy))
				continue;
			else 
				return i + 1;
		}
		return 1;
	}

	private int getCandyStartRelaxed(String hitId, String candy, int num) {
		int countRelaxed = 0;
		for (int i = rounds.size() - 1; i >= 0; i--) {
			if (rounds.get(i).getReport(hitId).equals(candy))
				continue;
			else {
				if (countRelaxed < num) {
					countRelaxed++;
				} else {
					return i + 1;
				}
			}
		}
		return 1;
	}

	public int getHonestStart(String hitId) {
		for (int i = rounds.size() - 1; i >= 0; i--) {
			String signal = rounds.get(i).getSignal(hitId);
			String report = rounds.get(i).getReport(hitId);
			if (signal.equals(report))
				continue;
			else 
				return i + 1;
		}
		return 1;
	}

	private int getHonestStartRelaxed(String hitId, int num) {
		int countRelaxed = 0;
		for (int i = rounds.size() - 1; i >= 0; i--) {
			String signal = rounds.get(i).getSignal(hitId);
			String report = rounds.get(i).getReport(hitId);
			if (signal.equals(report))
				continue;
			else {
				if (countRelaxed < num) {
					countRelaxed++;
				} else {
					return i + 1;
				}
			}
		}
		return 1;
	}

	public int getNumMM(String hitId) {
		int count = 0;
		for (Round round : rounds) {
			if (round.getReport(hitId).equals("MM")) count++;
		}
		return count;
	}

	public int getNumGB(String hitId) {
		int count = 0;
		for (Round round : rounds) {
			if (round.getReport(hitId).equals("GB")) count++;
		}
		return count;
	}

	public int getNumHonest(String hitId) {
		int count = 0;
		for (Round round : rounds) {
			String signal = round.getSignal(hitId);
			String report = round.getReport(hitId);
			if (signal.equals(report)) count++;
		}
		return count;
	}
	
	public void fillConvergenceType() {
		
		int gameMMStart = 0;
		int gameGBStart = 0;
		int gameHOStart = 0; 
		
		for (String hitId : this.playerHitIds) {
			
			int playerMMStart = this.getCandyStart(hitId, "MM");
			gameMMStart = Math.max(gameMMStart, playerMMStart);
			
			int playerGBStart = this.getCandyStart(hitId, "GB");
			gameGBStart = Math.max(gameGBStart, playerGBStart);
			
			int playerHOStart = this.getHonestStart(hitId);
			gameHOStart = Math.max(gameHOStart, playerHOStart);
		}
		
		int min = Math.min(Math.min(gameMMStart, gameGBStart), gameHOStart);
		this.roundConverged = min;
		
		String gameType = "";
		if (min > (numRounds - 5)) {
			gameType = "undecided";
		} else {
			if (gameMMStart == min) {
				gameType = "MM";
			}
			if (gameGBStart == min) {
				gameType = "GB";
			}
			if (gameHOStart == min) {
				gameType = "HO";
			}
		}
		this.convergenceType = gameType;
	}
	
	public void fillConvergenceTypeRelaxed(int i) {
		String gameType = "";
		
		int gameMMStart = 0;
		int gameGBStart = 0;
		int gameHOStart = 0; 
		
		for (String hitId : playerHitIds) {
			
			int playerMMScore = this.getCandyStartRelaxed(hitId, "MM", i);
			gameMMStart = Math.max(gameMMStart, playerMMScore);
			
			int playerGBScore = this.getCandyStartRelaxed(hitId, "GB", i);
			gameGBStart = Math.max(gameGBStart, playerGBScore);
			
			int playerHOScore = this.getHonestStartRelaxed(hitId, i);
			gameHOStart = Math.max(gameHOStart, playerHOScore);
		}
		
		int min = Math.min(Math.min(gameMMStart, gameGBStart), gameHOStart);
		this.roundConvergedRelaxed = min;
		
		if (min > ((numRounds - 5) - i)) {
			gameType = "undecided";
		} else {
			if (gameMMStart == min) {
				gameType = "MM";
			}
			if (gameGBStart == min) {
				gameType = "GB";
			}
			if (gameHOStart == min) {
				gameType = "HO";
			}
		}
		this.convergenceTypeRelaxed = gameType;
	}

	public void fillAsymmetricConvergenceType() {
		// 3 MM, 1 GB
		int best3MMStart = Integer.MAX_VALUE;
		
		
		for (String hitId1 : playerHitIds) {
			
			int threeMMStart = 0;
			
			for (String hitId2 : playerHitIds) {
				
				if (hitId1.equals(hitId2)) {
					int playerGBScore = this.getCandyStart(hitId2, "GB");
					threeMMStart = Math.max(threeMMStart, playerGBScore);
				} else {
					int playerMMScore = this.getCandyStart(hitId2, "MM");
					threeMMStart = Math.max(threeMMStart, playerMMScore);
				}
			}
			
			if (threeMMStart < best3MMStart)
				best3MMStart = threeMMStart;
			
		}
		
		// 3 GB, 1 MM
		int best3GBStart = Integer.MAX_VALUE;
		for (String hitId1 : playerHitIds) {
			
			int threeGBStart = 0;
			
			for (String hitId2 : playerHitIds) {
				if (hitId1.equals(hitId2)) {
					int playerMMScore = this.getCandyStart(hitId2, "MM");
					threeGBStart = Math.max(threeGBStart, playerMMScore);
				} else {
					int playerGBScore = this.getCandyStart(hitId2, "GB");
					threeGBStart = Math.max(threeGBStart, playerGBScore);
				}
			}
			
			if (threeGBStart < best3GBStart)
				best3GBStart = threeGBStart;
			
		}
	
		int gameHOStart = 0; 
		for (String hitId : this.playerHitIds) {
			
			int playerHOStart = this.getHonestStart(hitId);
			gameHOStart = Math.max(gameHOStart, playerHOStart);
		}
		
		int min = Math.min(Math.min(best3MMStart, best3GBStart), gameHOStart);
		this.roundConverged = min;
		
		String gameType = "";
		if (min > (numRounds - 5)) {
			gameType = "undecided";
		} else {
			if (min == best3MMStart) {
				gameType = "3MM";					
			} else if (min == best3GBStart) {
				gameType = "3GB"; 
			} else if (min == gameHOStart) {
				gameType = "HO";
			}
		}
		this.convergenceType = gameType;
	}

	public void fillAsymmetricConvergenceTypeRelaxed(int i) {
	
		// 3 MM, 1 GB
		int bestThreeMMOneGBStart = Integer.MAX_VALUE;
		for (String hitId1 : playerHitIds) {
			
			int threeMMOneGBStart = 0;
			
			for (String hitId2 : playerHitIds) {
				if (hitId1.equals(hitId2)) {
					int playerGBScore = this.getCandyStartRelaxed(hitId2, "GB", i);
					threeMMOneGBStart = Math.max(threeMMOneGBStart, playerGBScore);
				} else {
					int playerMMScore = this.getCandyStartRelaxed(hitId2, "MM", i);
					threeMMOneGBStart = Math.max(threeMMOneGBStart, playerMMScore);
				}
			}
			
			if (threeMMOneGBStart < bestThreeMMOneGBStart)
				bestThreeMMOneGBStart = threeMMOneGBStart;
			
		}
		
		// 3 GB, 1 MM
		int bestThreeGBOneMMStart = Integer.MAX_VALUE;
		for (String hitId1 : playerHitIds) {
	
			int threeGBOneMMStart = 0;
			
			for (String hitId2 : playerHitIds) {
				if (hitId1.equals(hitId2)) {
					int playerMMScore = this.getCandyStartRelaxed(hitId2, "MM", i);
					threeGBOneMMStart = Math.max(threeGBOneMMStart, playerMMScore);
				} else {
					int playerGBScore = this.getCandyStartRelaxed(hitId2, "GB", i);
					threeGBOneMMStart = Math.max(threeGBOneMMStart, playerGBScore);
				}
			}
			
			if (threeGBOneMMStart < bestThreeGBOneMMStart)
				bestThreeGBOneMMStart = threeGBOneMMStart;
			
		}
		
		int gameHOStart = 0; 
		for (String hitId : this.playerHitIds) {
			
			int playerHOStart = this.getHonestStartRelaxed(hitId, i);
			gameHOStart = Math.max(gameHOStart, playerHOStart);
		}
	
		int min = Math.min(Math.min(bestThreeMMOneGBStart, bestThreeGBOneMMStart), gameHOStart);
		this.roundConvergedRelaxed = min;
	
		String gameType = "";
		if (min > ((numRounds - 5) - i)) {
			gameType = "undecided";
		} else {
			if (min == bestThreeMMOneGBStart) {
				gameType = "3MM";
			} else if (min == bestThreeGBOneMMStart) {
				gameType = "3GB";
			} else if (min == gameHOStart) {
				gameType = "HO";
			}
		}
		this.convergenceTypeRelaxed = gameType;
	}

	public void saveSignalReportPairList() {

		signalReportObjList = new HashMap<String, List<SigActObservation<CandySignal, CandyReport>>>();
		for (String hitId: playerHitIds) {
			List<SigActObservation<CandySignal, CandyReport>> list = 
					new ArrayList<SigActObservation<CandySignal, CandyReport>>();
			for (Round round : rounds) {
				String signal = round.getSignal(hitId);
				String report = round.getReport(hitId);
				list.add(new SigActObservation<CandySignal, CandyReport>(
					CandySignal.valueOf(signal), CandyReport.valueOf(report)));
			}
			signalReportObjList.put(hitId, list);
		}

	}

	public Map<String, Double> getOppPopStrFull(int i, String excludeHitId) {
		
		List<String> otherReports = new ArrayList<String>();
		for (Round round : this.rounds) {
			List<String> reports = this.getOtherReportList(round, excludeHitId);
			otherReports.addAll(reports);
		}
		
		return AnalysisUtils.getOppPopStr(otherReports);
	}

	public Map<String, Double> getOppPopStrPrevRound(int i, String excludeHitId) {
		
		Round prevRound = rounds.get(i - 1);
		List<String> otherReports = this.getOtherReportList(prevRound, excludeHitId);
		
		return AnalysisUtils.getOppPopStr(otherReports);
	}

	public List<String> getOtherReportList(Round round, String excludeHitId) {
		List<String> reportArray = new ArrayList<String>();
		for (String hitId: this.playerHitIds) {
			if (hitId.equals(excludeHitId))
				continue;
			reportArray.add(round.getReport(hitId));
		}
		return reportArray;
	}

	public double getPaymentT1N2(String myReport, String refReport, double[] paymentArray) {
		if (myReport.equals("MM") && refReport.equals("MM"))
			return paymentArray[0];
		else if (myReport.equals("MM") && refReport.equals("GB"))
			return paymentArray[1];
		else if (myReport.equals("GB") && refReport.equals("MM"))
			return paymentArray[2];
		else if (myReport.equals("GB") && refReport.equals("GB"))
			return paymentArray[3];
		return -1;
	}

	public List<String> getOtherHitIds(String hitId) {

		List<String> otherHitIds = new ArrayList<String>();
		for (String innerHitId: this.playerHitIds) {
			if (innerHitId.equals(hitId))
				continue;
			else
				otherHitIds.add(innerHitId);
		}
		return otherHitIds;
	}

	public List<String> getRefReports(String hitId, int roundIndex) {
		List<String> otherHitIds = this.getOtherHitIds(hitId);
		
		List<String> refReports = new ArrayList<String>();
		for (String otherHitId: otherHitIds) {
			refReports.add( this.rounds.get(roundIndex).getReport(otherHitId));
		}
		return refReports;
	}

	public int getNumMMInRefReports(List<String> refReports) {
		int numMM = 0;
		for (String report : refReports) {
			if (report.equals("MM"))
				numMM++;
		}
		return numMM;
	}

	public String getRefReport(String hitId, int i) {
		Round currRound = this.rounds.get(i);

		Map<String, Map<String, Object>> roundResult = currRound.result;
		Map<String, Object> myResult = roundResult.get(hitId);
		
		String refPlayerHitId = (String) myResult.get("refPlayer");
		Map<String, Object> refPlayerResult = roundResult.get(refPlayerHitId);
		
		String refReport = (String) refPlayerResult.get("report");

		return refReport;
	}

	public double getPayoffT3(String myReport,
			Map<String, Double> oppPopStrategy) {
		return 
				AnalysisUtils.getPaymentT3(myReport, 0) 
					* oppPopStrategy.get("GB") 
					* oppPopStrategy.get("GB") 
					* oppPopStrategy.get("GB")
				
				+ AnalysisUtils.getPaymentT3(myReport, 1) * 
					(3 * oppPopStrategy.get("MM")  
					   * oppPopStrategy.get("GB") 
					   * oppPopStrategy.get("GB")
					)
						
				+ AnalysisUtils.getPaymentT3(myReport, 2) * 
					(3 * oppPopStrategy.get("MM")  
					   * oppPopStrategy.get("MM") 
					   * oppPopStrategy.get("GB")
						)
						
				+ AnalysisUtils.getPaymentT3(myReport, 3) 
					* oppPopStrategy.get("MM") 
					* oppPopStrategy.get("MM") 
					* oppPopStrategy.get("MM")
				;
	}

	public double getPayoffT1N2(String myReport, 
			Map<String, Double> oppPopStrategy, double[] paymentArray) {
		
		return this.getPaymentT1N2(myReport, "MM", paymentArray) 
				* oppPopStrategy.get("MM") 
				* oppPopStrategy.get("MM")
				
				+  (this.getPaymentT1N2(myReport, "MM", paymentArray) * 0.5 
						+ this.getPaymentT1N2(myReport, "GB", paymentArray) * 0.5)
					* 2 * oppPopStrategy.get("MM") 
					    * oppPopStrategy.get("GB")
				
				+ this.getPaymentT1N2(myReport, "GB", paymentArray) 
					* oppPopStrategy.get("GB") 
					* oppPopStrategy.get("GB");
	}

	public String getBestResponseT3(Map<String, Double> oppPopStrategy) {
		String myReport;

		double payoffMM = this.getPayoffT3("MM", oppPopStrategy);
		double payoffGB = this.getPayoffT3("GB", oppPopStrategy);
		
		if ((payoffMM - payoffGB) > AnalysisUtils.eps) 
			myReport = "MM";
		else
			myReport = "GB";
		return myReport;
	}
	
	public String getBestResponseT1N2(Map<String, Double> oppPopStrategy, double[] paymentArray) {
		String myReport;

		double payoffMM = this.getPayoffT1N2("MM", oppPopStrategy, paymentArray);
		double payoffGB = this.getPayoffT1N2("GB", oppPopStrategy, paymentArray);
		
		if ((payoffMM - payoffGB) > AnalysisUtils.eps) 
			myReport = "MM";
		else
			myReport = "GB";
		return myReport;
	}
	
}
