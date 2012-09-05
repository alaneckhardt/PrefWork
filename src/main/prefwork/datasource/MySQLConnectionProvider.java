package prefwork.datasource;
import java.sql.DriverManager;
import java.sql.SQLException;
public class MySQLConnectionProvider  extends SQLConnectionProvider {

	public MySQLConnectionProvider(){		
	}
	

	public void connect() throws SQLException {
		// Load the Driver
		String className = "com.mysql.jdbc.Driver"; // path of driver class
		try {
			Class.forName(className); // load driver
		} catch (ClassNotFoundException ex) {
		}

		String DB_URL = "jdbc:mysql://localhost/" + db; // URL of database

		try {
			conn = DriverManager.getConnection( DB_URL, userName, password );
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
		
	}
	
}
