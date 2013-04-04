package edu.harvard.econcs.peerprediction.mturk;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.PropertiesClientConfig;

public class MTurk {

	/**
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static void main(String[] args) throws ClassNotFoundException, SQLException {

		RequesterService service = new RequesterService(
				new PropertiesClientConfig("mturk.properties"));

//		notifyWorkers(service);


		bonusManually(service);
		
/*
		createPrivateHIT();
*/

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
	
				 String update = String.format("update session " +
				 "set payment=0.10, paid=1, bonusPaid=1, hitStatus='Disposed' "
				 +
				 "where hitId='%s' ", hitId);
				 Statement updateStmt = con.createStatement();
				 updateStmt.executeUpdate(update);
	
				 Assignment assign = assigns[0];
				 String workerId = assign.getWorkerId();
				
				 double bonus = Double.parseDouble(bonusString);
				 totalBonus = totalBonus + bonus;
				
				 System.out.printf("HIT (%s), ASS (%s), Worker (%s), bonus %.2f\n",
				 hitId, assignmentId, workerId, bonus);
				 String msg =
				 "Thank you for playing the trick or treat game!";
				 
				 
//				 service.grantBonus(workerId, bonus, assignmentId, msg);
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
		service.notifyWorkers(subject, message2, new String[]{aliceWorkerId});
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
