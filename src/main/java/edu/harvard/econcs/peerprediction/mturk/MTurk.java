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
import java.util.List;

import com.amazonaws.mturk.addon.HITProperties;
import com.amazonaws.mturk.addon.HITQuestion;
import com.amazonaws.mturk.dataschema.QuestionFormAnswers;
import com.amazonaws.mturk.dataschema.QuestionFormAnswersType;
import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.AssignmentStatus;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.HITStatus;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.PropertiesClientConfig;

public class MTurk {

	static final String rootDir = "src/test/resources/";
	
	/**
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static void main(String[] args) throws ClassNotFoundException,
			SQLException {

		RequesterService service = new RequesterService(
				new PropertiesClientConfig("src/test/resources/mturk.properties"));

		System.out.println("url is " + service.getWebsiteURL());

//		processResultsRecruitingHIT(service);
//		deleteAllExpiredUnavailHITS(service);

		postAndDeleteUnavailHIT(service);
	}

	private static void postAndDeleteUnavailHIT(RequesterService service) {
		while (true) {

			postUnavailHIT(service);

			try {
				System.out.println("Sleeping for 1 minute");
				Thread.sleep(60000);
				System.out.println("Finished sleeping for 1 minutes");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			deleteUnavailHIT(service);

		}
	}

	private static void processResultsRecruitingHIT(RequesterService service) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(rootDir + "recruit_hitid.txt"));
			String hitId = reader.readLine();
			// HIT recruitHIT = service.getHIT(hitId);

			int[] time = new int[24];

			Assignment[] assigns = service.getAllAssignmentsForHIT(hitId);
			System.out.println(assigns.length + " assignments");
			for (Assignment assign : assigns) {
				if (assign.getAssignmentStatus() == AssignmentStatus.Submitted) {
					String message = "Thank you for completing our recruiting HIT! "
							+ "Once we have enough participants, we will notify you " +
							"by email 1 day before"
							+ " we post the HITs for our experiment.";
					service.approveAssignment(assign.getAssignmentId(), message);

				}

				String answerXML = assign.getAnswer();

				QuestionFormAnswers qfa = RequesterService
						.parseAnswers(answerXML);
				List<QuestionFormAnswersType.AnswerType> answers =
						(List<QuestionFormAnswersType.AnswerType>) qfa
						.getAnswer();

				for (QuestionFormAnswersType.AnswerType answer : answers) {
					String assignmentId = assign.getAssignmentId();
					String answerValue = RequesterService.getAnswerValue(
							assignmentId, answer);

					// if (answer.getQuestionIdentifier().equals("consent")) {
					// if (!answerValue.equals("true"))
					// System.out.println("no consent from worker " +
					// assign.getWorkerId());
					// }

					if (answer.getQuestionIdentifier().equals("firstTime")
							|| answer.getQuestionIdentifier().equals(
									"secondTime")) {
						int chosenTime = Integer.parseInt(answerValue);
						time[chosenTime]++;
					}

				}
			}

			BufferedWriter writer = new BufferedWriter(new FileWriter(
					rootDir + "times.csv"));
			for (int i = 0; i < time.length; i++) {
				writer.write(time[i] + ",");
			}
			writer.flush();
			writer.close();

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

			BufferedWriter writer = new BufferedWriter(new FileWriter(
					rootDir + "recruit_hitid.txt"));
			writer.write(hit.getHITId());
			writer.flush();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void expireRecruitingHIT(RequesterService service) {

		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					rootDir + "recruit_hitid.txt"));
			String hitId = reader.readLine();
			reader.close();
			service.forceExpireHIT(hitId);
			// service.disposeHIT(hitId);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void postUnavailHIT(RequesterService service) {
		String propertiesFile = rootDir + "unavail.properties";
		String questionFile = rootDir + "unavail.question";
		HITProperties props;
		try {
			props = new HITProperties(propertiesFile);
			HITQuestion question = new HITQuestion(questionFile);

			System.out.println("Creating unavail HIT...");
			HIT hit = service.createHIT(null, props.getTitle(),
					props.getDescription(), props.getKeywords(),
					question.getQuestion(), props.getRewardAmount(),
					props.getAssignmentDuration(),
					props.getAutoApprovalDelay(), props.getLifetime(),
					props.getMaxAssignments(), props.getAnnotation(),
					props.getQualificationRequirements(), null);

			String hitId = hit.getHITId();
			hit = service.getHIT(hit.getHITId());
			System.out.println("Created unavail hit " + hitId);

			BufferedWriter writer = new BufferedWriter(new FileWriter(
					rootDir + "unavail_hitid.txt"));
			writer.write(hit.getHITId());
			writer.flush();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void deleteAllExpiredUnavailHITS(RequesterService service) {
		HIT[] allHits = service.searchAllHITs();
		for (HIT hit : allHits) {
			String hitId = hit.getHITId();
			HIT h = service.getHIT(hitId);
			if (h.getHITStatus() == HITStatus.Reviewable
					&& h.getRequesterAnnotation() != null
					&& h.getRequesterAnnotation().equals("unavail")) {
				service.disposeHIT(hitId);
			}
		}
	}

	private static void deleteUnavailHIT(RequesterService service) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(rootDir + "unavail_hitid.txt"));
			String hitId = reader.readLine();
			reader.close();

			service.forceExpireHIT(hitId);
			System.out.println("Force expired unavail hit " + hitId);

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

	private static void notifyWorkers(RequesterService service)
			throws ClassNotFoundException, SQLException {

		String subject = "Play a 2 minute game with other turkers and earn up to $0.50 bonus";
		String message = "Dear MTurk worker,\n\n"
				+ "I'd like to invite you to work on a new batch of HITs.  "
				+ "You will be playing a game (~2 minutes) with other turkers in real time and earn up to $0.50 bonus.\n\n"
				+ "The HITs will be posted tomorrow *Saturday March 30 10am-noon and 2-5pm EST*.  "
				+ "Since each game requires 3 players at the same time, we suggest all of you come *at the top of each hour*.\n\n"
				+ "Please find the HITS here: https://www.mturk.com/mturk/searchbar?requesterId=A15OV4L8HXBKSM \n\n"
				+ "We appreciate your participation! :)\n\n"
				+ "Note: (1) For this batch only, you'll need to go through a tutorial and a quiz (~5 minutes), "
				+ "but you can directly start playing for future batches.  "
				+ "(2) When you are waiting for other players to join, feel free to leave the HIT open and work on other HITs. "
				+ "The page will play a sound when there are enough players to join a game.";

		String message2 = "Dear MTurk worker,\n\n"
				+ "You may be interested in working on a HIT posted by a colleague of mine.  Details are below:\n\n\n"
				+ "You will be playing a game (~2 minutes) with other turkers in real time and earn up to $0.50 bonus.  "
				+ "For this batch, you can only complete one HIT. \n\n"
				+ "Please come work on the HIT *tomorrow Tuesday April 2nd, at 2pm Eastern Standard Time*.\n"
				+ "Link to HIT: https://www.mturk.com/mturk/searchbar?requesterId=A15OV4L8HXBKSM \n\n"
				+ "We appreciate your participation! If all of you come at 2pm EST, we'll get all games done very quickly!\n\n"
				+ "(If you don't want to receive this type of notification, please let me know.)\n"
				+ "(For *this batch only*, you need to spend another 5 minutes for a tutorial and a quiz, "
				+ "but for future batches, you can start playing directly.)\n\n";

		String subjectReminder = "Reminder to come play a game at 2pm EST";
		String messageReminder = "Just a gentle reminder to come play a game with other turkers at 2pm EST today\n"
				+ "Link to HIT: https://www.mturk.com/mturk/searchbar?requesterId=A15OV4L8HXBKSM \n\n"
				+ "We really appreciate your participation!";

		String aliceWorkerId = "A15OV4L8HXBKSM";
		service.notifyWorkers(subject, message2, new String[] { aliceWorkerId });
		System.exit(0);

		String dbUrl = "jdbc:mysql://localhost/turksorting";
		String dbClass = "com.mysql.jdbc.Driver";

		Class.forName(dbClass);
		Connection con = DriverManager.getConnection(dbUrl, "root", "");

		Statement stmt = con.createStatement();
		String query = "select id from worker where country='USA'";
		ResultSet rs = stmt.executeQuery(query);

		while (rs.next()) {
			try {

				String workerId = rs.getString("id");
				service.notifyWorkers(subject, message,
						new String[] { workerId });
				System.out.println("sent email to worker " + workerId);
			} catch (Exception ex) {
				ex.printStackTrace();
			}

		}

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
