package prefwork.datasource;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MSSQLConnectionProvider extends SQLConnectionProvider {
	
	public MSSQLConnectionProvider(){
	}

	public void connect() {
		try {
		// Load the Driver		
		Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		String connectionUrl = "jdbc:sqlserver://localhost:1433;" +
		   "databaseName="+db+";user="+userName+";password="+password+";";
		conn = DriverManager.getConnection(connectionUrl);
		
		PreparedStatement stm=conn.prepareStatement("use "+db);
		stm.execute();			
	} catch (ClassNotFoundException e) {
		e.printStackTrace();
	} catch (SQLException e) {
		e.printStackTrace();
	}
		
	}
	
}
