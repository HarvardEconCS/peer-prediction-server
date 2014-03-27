package edu.harvard.econcs.peerprediction.mturk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.amazonaws.mturk.addon.HITProperties;
import com.amazonaws.mturk.addon.HITQuestion;
import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.Qualification;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.amazonaws.mturk.util.PropertiesClientConfig;

public class MTurk {

	static final String rootDir = "src/test/resources/";
	static final String qualificationTypeId = "2QYBMJTUHYW25N7F11YSAWM16CQNIL";

	// Qual values
	// 1, hasn't participated
	// 2, participated once
	// 100, failed quiz 3 times
	// 99, worker ID longer than 14 characters
	// 98, couldn't resolve connection issue
	// 97, does not want to participate

	public static void main(String[] args) throws ClassNotFoundException,
			SQLException, IOException {
		RequesterService service = new RequesterService(
				new PropertiesClientConfig(
						"src/test/resources/mturk.properties"));
		System.out.println("url is " + service.getWebsiteURL());

		 checkQual(service);
//		 sendWorkerEmail(service);

//		 assignQualForRecruited(service);
		
//		 sendEmailsToRandomWorkers(service);
//		 sendEmailToWorkersInList(service);

//		 updateQualTo2ForPlayed(service);
//		 updateQualTo100ForFailed(service);

//		 payBonusManually(service);

		// postRecruitingHIT(service);

		// getPassedNotWorked();
	}

	private static void sendEmailsToRandomWorkers(RequesterService service) {

		int requiredNum = 600;
		String relativeDate = "today";
		String dateNumerical = "01-29";
		String dateEnglish = "Wednesday January 29";
		String estTimes = "9PM";
		String pstTimes = "6PM";

		String subject = String.format(
				"Behavioral experiment HITs posted %s at %s EST (%s PST)",
				relativeDate, estTimes, pstTimes);
		String messageText = String
				.format("Hi everyone,\n\n"
						+

						"Thank you for signing up to particiate in our behavioral experiment!\n\n"
						+

						"We are posting behavioral experiment HITs %s (%s) at *%s EST* (%s PST).\n"
						+ "You may complete only *1 HIT*.\n\n"
						+

						"The HITs will only be available for 30 minutes. "
						+ "When 30 minutes is up, no new games can start but existing games are allowed to finish.\n\n"
						+

						"Link to HIT: https://www.mturk.com/mturk/searchbar?requesterId=A15OV4L8HXBKSM\n"
						+ "The HIT title is *play a game with other turkers and earn $0.10 to $1.50 bonus*\n\n"
						+

						"We appreciate your participation!  "
						+ "Please send your questions and comments to alice.gao11@gmail.com.",

				relativeDate, dateEnglish, estTimes, pstTimes);

		System.out.println(subject);
		System.out.println(messageText);

		List<String> allWorkers = new ArrayList<String>();
		try {
			Qualification[] quals = service
					.getAllQualificationsForQualificationType(qualificationTypeId);

			for (Qualification qual : quals) {
				if (qual.getIntegerValue() == 1) {
					allWorkers.add(qual.getSubjectId());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("num workers with qual = 1 " + allWorkers.size());

		// Randomize worker order
		Collections.shuffle(allWorkers);
		Collections.shuffle(allWorkers);

		String filename = "emailsSent-" + dateNumerical + ".txt";

		try {
			int count = 0;
			BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
					+ filename));
			for (String workerId : allWorkers) {
				try {
					service.notifyWorkers(subject, messageText,
							new String[] { workerId });
					System.out.println(workerId);
					writer.write(workerId);
					writer.write("\n");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				count++;
				if (count == requiredNum)
					break;
			}
			writer.flush();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void sendEmailToWorkersInList(RequesterService service) {

//		String estEndTime = "2:30PM";
//		String pstEndTime = "11:30AM";
		String estEndTime = "9:30PM";
		String pstEndTime = "6:30PM";
		String filename = "emailsSent-01-29.txt";

		String subject = String
				.format("Reminder: our behavioral experiment HITs posted NOW until %s EST (%s PST)",
						estEndTime, pstEndTime);
		String message = String
				.format("Hi everyone,\n\n"

						+ "Our behavioral experiment HITs are posted NOW and are available until %s EST (%s PST). "
						+ "(At %s EST, the server will switch to a mode such that new games cannot start "
						+ "but games in progress are allowed to finish.)\n\n"

						+ "Link to HIT: https://www.mturk.com/mturk/searchbar?requesterId=A15OV4L8HXBKSM\n"
						+ "The HIT title is *play a game with other turkers and earn $0.10 to $1.50 bonus*\n\n"

						+ "Thank you for participating!", estEndTime,
						pstEndTime, estEndTime);

		System.out.println(subject);
		System.out.println(message);

		try {
			int count = 0;
			BufferedReader reader = new BufferedReader(new FileReader(rootDir
					+ filename));
			String workerId = "";
			while ((workerId = reader.readLine()) != null) {
				try {
					int value = service.getQualificationScore(
							qualificationTypeId, workerId).getIntegerValue();
					if (value == 1) {
						service.notifyWorkers(subject, message,
								new String[] { workerId });
						System.out.println("sent email to " + workerId);
						count++;
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			reader.close();
			System.out.println("sent emails to " + count + " workers");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ServiceException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void checkQual(RequesterService service) {
		String workerId = "AHUI7QAYUMWWI";
		// service.updateQualificationScore(qualificationTypeId, workerId, 98);
		// int num = service.getQualificationScore(qualificationTypeId,
		// workerId)
		// .getIntegerValue();
		// System.out.println(num);
		updateQualScore(service, workerId, 97);
	}

	private static void updateQualScore(RequesterService service,
			String workerId, int newScore) {
		System.out.println("worker " + workerId + " value " + newScore);
		service.updateQualificationScore(qualificationTypeId, workerId,
				newScore);
	}

	public static void sendWorkerEmail(RequesterService service) {
		String[] workerIds = new String[] { "A1Y3P8LL2LCHJO", "A3V5ZWYFPIENOI" };
		String subject = "About behavioural experiment HITs";
		String message = "Dear worker," +
				"This is Xi Gao, the requester for the behavioural experiment HITs. " +
				"According to our records, you participated in our behavioural experiments at 2PM EST today, " +
				"but you were not able to complete the game because of connection issues.  " +
				"However, I do have records of the rounds of the game that you did play.  " +
				"If you like, I can calculate your average payment in those rounds and pay you the base" +
				"payment and bonus manually.  Please let me know if you want me to do this.  Thank you!";
		// String subject = "Server shut down and new game cannot start";
		// String message = "Dear worker,\n\n"
		// + "Thank you for participating in our behavioural experiment.  "
		// +
		// "I want to let you know that since it's past 10:30PM EST, our server has turned into a mode"
		// +
		// "where new games cannot start but existing games are allowed to finish.  "
		// +
		// "If you still like to participate, I will be posting the HITs in the next few days and"
		// +
		// "you are welcome to work on a new HIT then.  You will go directly into the lobby in a new HIT.\n\n"
		// +
		// "Please send me an email at alice.gao11@gmail.com if you have any questions.\n\n";

//		String subject = "It appears you did not submit the behavioral experiment HIT successfully";
//		String message = "Dear worker,\n\n"
//				+ "My record shows that you just participated in our behavioral experiment "
//				+ "but your HIT has not been submitted successfully. "
//				+ "I won't be able to pay you if your HIT is not submitted properly."
//				+ "Would you mind going to your dashboard and checking whether the HIT is still assigned to you? "
//				+ "Send me an email at alice.gao11@gmail.com if you have questions.\n\n"
//				+ "Best,\n\n" + "Alice";
		service.notifyWorkers(subject, message, workerIds);
	}

	private static void getPassedNotWorked() throws FileNotFoundException,
			IOException {
		BufferedReader reader;
		List<String> worked = new ArrayList<String>();

		reader = new BufferedReader(new FileReader(rootDir + "worked.txt"));
		String workerId = "";
		while ((workerId = reader.readLine()) != null) {
			worked.add(workerId);
		}
		reader.close();

		List<String> passedNotWorked = new ArrayList<String>();
		reader = new BufferedReader(new FileReader(rootDir + "passed.txt"));

		System.out.print("select * from turkserver.quiz where ");
		while ((workerId = reader.readLine()) != null) {
			if (!worked.contains(workerId)) {
				passedNotWorked.add(workerId);
				System.out.print("workerId ='" + workerId + "' or ");
			}
		}
		reader.close();
	}

	private static void assignQualForRecruited(RequesterService service)
			throws IOException {

		int numAlreadyQualified = 0;
		int numNewQualified = 0;
		BufferedReader reader = new BufferedReader(new FileReader(rootDir
				+ "recruit_hitid.txt"));
		String hitId = reader.readLine();
		Assignment[] assigns = service.getAllAssignmentsForHIT(hitId);
		for (Assignment assign : assigns) {
			String workerId = assign.getWorkerId();
			try {
				Qualification qualObj = service.getQualificationScore(
						qualificationTypeId, workerId);
				numAlreadyQualified++;
			} catch (Exception e) {
				if (workerId.length() > 14) {
					System.out.printf("Worker ID too long");
					service.updateQualificationScore(qualificationTypeId,
							workerId, 99);
				} else {
					System.out.printf(
							"Granting qualification = 1 to worker %s\n",
							workerId);
					service.assignQualification(qualificationTypeId, workerId,
							1, false);
					numNewQualified++;
				}

			}
		}
		reader.close();

		System.out
				.printf("%d workers already qualified\n", numAlreadyQualified);
		System.out.printf("granted qualification (=1) to %d workers\n",
				numNewQualified);

	}

	private static void updateQualTo2ForPlayed(RequesterService service) {

		BufferedReader reader;
		int count = 0;

		// Change the value of qual to 2 for workers who have participated.
		System.out
				.println("Changing qual to 2 for workers who have participated");
		try {
			reader = new BufferedReader(new FileReader(String.format(
					"%sworked.txt", rootDir)));
			String workerId = "";
			while ((workerId = reader.readLine()) != null) {
				service.updateQualificationScore(qualificationTypeId, workerId,
						2);
				System.out.println("changed qual to 2 for worker " + workerId);
				count++;
			}
			reader.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Updated qual=2 for " + count + " workers");
	}

	private static void updateQualTo100ForFailed(RequesterService service) {

		BufferedReader reader;

		try {
			List<String> passed = new ArrayList<String>();

			reader = new BufferedReader(new FileReader(String.format(
					"%spassed.txt", rootDir)));
			String workerId = "";
			while ((workerId = reader.readLine()) != null) {
				passed.add(workerId);
			}
			reader.close();

			int count = 0;
			reader = new BufferedReader(new FileReader(String.format(
					"%sthreerecords.txt", rootDir)));
			while ((workerId = reader.readLine()) != null) {
				if (!passed.contains(workerId)) {
					service.updateQualificationScore(qualificationTypeId,
							workerId, 100);
					System.out.println("changed qual to 100 for worker "
							+ workerId);
					count++;
				}
			}
			reader.close();

			System.out.println("Updated qual=100 for " + count + " workers");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void postRecruitingHIT(RequesterService service) {

		String propertiesFile = rootDir + "recruiting.properties";
		String questionFile = rootDir + "recruiting.question";
		HITProperties props;
		try {
			props = new HITProperties(propertiesFile);
			HITQuestion question = new HITQuestion(questionFile);

			System.out.println("Posting recruiting HIT");
			HIT hit = service.createHIT(null, props.getTitle(),
					props.getDescription(), props.getKeywords(),
					question.getQuestion(), props.getRewardAmount(),
					props.getAssignmentDuration(),
					props.getAutoApprovalDelay(), props.getLifetime(),
					props.getMaxAssignments(), props.getAnnotation(),
					props.getQualificationRequirements(), null);

			hit = service.getHIT(hit.getHITId());
			System.out.println("Created recruiting HIT " + hit.getHITId());

			BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
					+ "recruit_hitid.txt"));
			writer.write(hit.getHITId());
			writer.flush();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void payBonusManually(RequesterService service) {
		BufferedReader reader;

//		List<String> workerIds = new ArrayList<String>();
//		List<Double> bonuses = new ArrayList<Double>();
//		try {
//			reader = new BufferedReader(new FileReader(rootDir
//					+ "manualbonus.txt"));
//			String line = "";
//
//			while ((line = reader.readLine()) != null) {
//				String[] strings = line.split(",");
//				String workerId = strings[0];
//				workerIds.add(workerId);
//				bonuses.add(Double.parseDouble(strings[1]));
//			}
//			reader.close();
//
//			BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
//					+ "manualbonuswithassign.txt"));
//			reader = new BufferedReader(new FileReader(rootDir
//					+ "recruit_hitid.txt"));
//			String hitId = reader.readLine();
//			Assignment[] assigns = service.getAllAssignmentsForHIT(hitId);
//			System.out.println(assigns.length + " assignments");
//			for (Assignment assign : assigns) {
//				String workerId = assign.getWorkerId();
//				if (workerIds.contains(workerId)) {
//					int index = workerIds.indexOf(workerId);
//					writer.write(String.format("%s,%s,%.2f\n", workerId,
//							assign.getAssignmentId(), bonuses.get(index)));
//				}
//			}
//			writer.flush();
//			writer.close();
//			reader.close();
//		} catch (FileNotFoundException e1) {
//			e1.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		System.exit(0);

		String reason = "Thank you for participating in our experiment";
		try {
			reader = new BufferedReader(new FileReader(rootDir
					+ "manualbonuswithassign.txt"));
			String line = "";
			while ((line = reader.readLine()) != null) {
				String[] strings = line.split(",");
				String workerId = strings[0];
				String assignId = strings[1];
				double bonus = Double.parseDouble(strings[2]);
				double total = 1.0 + bonus;
				total = 1.0 * Math.round(total * 100) / 100;

				System.out.println("Paying worker " + workerId + " assignId "
						+ assignId + " total: " + total);
				service.grantBonus(workerId, total, assignId, reason);

			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void bonusManually(RequesterService service)
			throws ClassNotFoundException, SQLException {
		String dbUrl = "jdbc:mysql://localhost/turkserver";
		String dbClass = "com.mysql.jdbc.Driver";
		String setId = "mar-13-fixed";

		Class.forName(dbClass);
		Connection con = DriverManager.getConnection(dbUrl, "root", "");

		Statement stmt = con.createStatement();
		String query = "select hitId, assignmentId, payment, bonus from session "
				+ "where paid = 0 and bonusPaid = 0 and payment is null and bonus is not null "
				+ "and comment is not null";
		ResultSet rs = stmt.executeQuery(query);

		double totalBonus = 0.0;

		while (rs.next()) {
			String hitId = rs.getString("hitId");
			String assignmentId = rs.getString("assignmentId");
			String payment = rs.getString("payment");
			String bonusString = rs.getString("bonus");

			HIT hit = service.getHIT(hitId);
			Assignment[] assigns = service.getAllAssignmentsForHIT(hitId);

			if (assigns.length > 0 && bonusString != null) {

				String update = String
						.format("update session "
								+ "set payment=0.10, paid=1, bonusPaid=1, hitStatus='Disposed' "
								+ "where hitId='%s' ", hitId);
				Statement updateStmt = con.createStatement();
				updateStmt.executeUpdate(update);

				Assignment assign = assigns[0];
				String workerId = assign.getWorkerId();

				double bonus = Double.parseDouble(bonusString);
				totalBonus = totalBonus + bonus;

				System.out.printf(
						"HIT (%s), ASS (%s), Worker (%s), bonus %.2f\n", hitId,
						assignmentId, workerId, bonus);
				String msg = "Thank you for playing the trick or treat game!";

				// service.grantBonus(workerId, bonus, assignmentId, msg);
			}

		}
		System.out.printf("total bonus is %.2f", totalBonus);
	}

	private static void createPrivateHIT() {
		// String workerId = "AXGYF8LC3VU6K";
		//
		// String qualTypeId = "2JWHVWAVKI62CD88BYTZE9QH6EAHPG";
		// // service.assignQualification(qualTypeId, "A15OV4L8HXBKSM", 1,
		// true);
		//
		// QualificationRequirement qualRequirement = new
		// QualificationRequirement(qualTypeId , Comparator.EqualTo,
		// 1, null, null);
		// QualificationRequirement[] requirements = new
		// QualificationRequirement[]{qualRequirement};
		//
		// String hitTypeId = service.registerHITType((long)60*60*24, (long)
		// 60*60*24, 0.47,
		// "Private HIT", "private", "Private HIT", requirements);
		//
		// HIT hit = service.createHIT(hitTypeId,
		// "Private HIT",
		// "Private HIT",
		// "private",
		// RequesterService.getBasicFreeTextQuestion("Empty question"),
		// 0.47,
		// (long)60*60*24,
		// (long)60*60*24,
		// (long)60*60*24,
		// 1,
		// "", requirements, null, null, null, null);
		//
		// hit = service.getHIT(hit.getHITId());
		// System.out.println("hit Id is " + hit.getHITId());
	}

}
