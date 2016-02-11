package dbtLab3;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Database is a class that specifies the interface to the movie database. Uses
 * JDBC and the MySQL Connector/J driver.
 */
public class Database {
	/**
	 * The database connection.
	 */
	private Connection conn;

	/**
	 * Create the database interface object. Connection to the database is
	 * performed later.
	 */
	public Database() {
		conn = null;
	}

	/**
	 * Open a connection to the database, using the specified user name and
	 * password.
	 * 
	 * @param userName
	 *            The user name.
	 * @param password
	 *            The user's password.
	 * @return true if the connection succeeded, false if the supplied user name
	 *         and password were not recognized. Returns false also if the JDBC
	 *         driver isn't found.
	 */
	public boolean openConnection(String userName, String password) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://puccini.cs.lth.se/" + userName, userName, password);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Close the connection to the database.
	 */
	public void closeConnection() {
		try {
			if (conn != null)
				conn.close();
		} catch (SQLException e) {
		}
		conn = null;
	}

	/**
	 * Check if the connection to the database has been established
	 * 
	 * @return true if the connection has been established
	 */
	public boolean isConnected() {
		return conn != null;
	}

	/* --- insert own code here --- */

	public boolean isUser(String userId) {
		String s = "SELECT username FROM users WHERE username = ?";
		try {
			PreparedStatement ps = conn.prepareStatement(s);
			ps.setString(1, userId);
			ResultSet rs = ps.executeQuery();
			return rs.next();

		} catch (SQLException e) {
			return false;
		}
	}

	public ArrayList<String> getMovies() {
		String s = "SELECT title FROM movies";
		ArrayList<String> res = new ArrayList<String>();
		try {
			PreparedStatement ps = conn.prepareStatement(s);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				res.add(rs.getString("title"));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return res;
	}

	public ArrayList<String> getDates(String movie) {
		String s = "SELECT day FROM performances WHERE title = ?";
		ArrayList<String> res = new ArrayList<String>();
		try {
			PreparedStatement ps = conn.prepareStatement(s);
			ps.setString(1, movie);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				res.add(rs.getString("day"));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return res;
	}

	public String getTheatre(String movie, String date) {
		try {
			String s = "SELECT theatreName FROM performances WHERE title = ? AND day = ?";
			PreparedStatement ps = conn.prepareStatement(s);
			ps.setString(1, movie);
			ps.setString(2, date);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getString("theatreName");
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;

	}

	public int getFreeSeats(String movie, String date) {
		try {
			String s = "SELECT freeSeats FROM performances WHERE title = ? AND day = ?";
			PreparedStatement ps = conn.prepareStatement(s);
			ps.setString(1, movie);
			ps.setString(2, date);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getInt("freeSeats");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public Performance getPerformance(String movie, String date) {
		Performance p = null;
		try {
			String s = "SELECT theatreName, freeSeats FROM performances WHERE title = ? AND day = ?";
			PreparedStatement ps = conn.prepareStatement(s);
			ps.setString(1, movie);
			ps.setString(2, date);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				String theatre = rs.getString("theatreName");
				int freeSeats = rs.getInt("freeSeats");
				p = new Performance(movie, date, theatre, freeSeats);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return p;
	}

	public boolean book(String user, String movie, String date) {
		try {
			PreparedStatement ps = conn.prepareStatement("START TRANSACTION");
			ps.executeQuery();

			String l = "SELECT * FROM performances WHERE title=? AND day=? FOR UPDATE";
			ps = conn.prepareStatement(l);
			ps.setString(1, movie);
			ps.setString(2, date);
			ps.executeQuery();

			String s1 = "UPDATE performances SET freeSeats=freeSeats-1 WHERE title=? AND day=?";
			ps = conn.prepareStatement(s1);
			ps.setString(1, movie);
			ps.setString(2, date);
			ps.executeUpdate();

			if (getFreeSeats(movie, date) < 0)
				ps.execute("ROLLBACK");
			else {
				ps.execute("COMMIT");

				String s2 = "INSERT INTO reservations (username, title, day) VALUES(?, ?, ?)";
				ps = conn.prepareStatement(s2);
				ps.setString(1, user);
				ps.setString(2, movie);
				ps.setString(3, date);
				ps.executeUpdate();

				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
}
