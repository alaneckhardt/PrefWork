package prefwork.datasource;

import java.sql.DriverManager;
import java.sql.SQLException;

import oracle.jdbc.pool.OracleDataSource;

public class OracleConnectionProvider extends SQLConnectionProvider {

	public OracleConnectionProvider(){		
		/*try {
		    String url = "jdbc:oracle:oci8:@";
		    try {
		      String url1 = System.getProperty("JDBC_URL");
		      if (url1 != null)
		        url = url1;
		    } catch (Exception e) {
		      // If there is any security exception, ignore it
		      // and use the default
		    }

		    // Create an OracleDataSouce instance and set properties
		    OracleDataSource ods = new OracleDataSource();
		    ods.setUser(userName);
		    ods.setPassword(password);
		    ods.setURL(url);
		    
		    // Connect to the database
		    conn = (OracleConnection)ods.getConnection ();		*/
			//PreparedStatement stm=conn.prepareStatement("use "+db);
			//stm.execute();			
		/*}  catch (SQLException e) {
			e.printStackTrace();
		}*/
	}

	public void connect() {
		try {
	   /* String url = "jdbc:oracle:oci8:@";
	    url = "jdbc:oracle:oci:netflix/aaa@//10.10.21.105:1521/ORCL";
	    url = "jdbc:oracle:thin:netflix/aaa@//10.10.21.105:1521/ORCL";*/
	    
	    


	    /*
	     * 
	     * 
	    url = "jdbc:oracle:thin:"+userName+"/"+password+"@//10.10.21.105:1521/ORCL";
	    String url      = "jdbc:oracle:thin:@localhost:1521:orcl";
	    String user     = "scott";
	    String password = "tiger";

	    // Create the properties object that holds all database details
	    Properties props = new Properties();
	    props.put("user", user );
	    props.put("password", password);
	    props.put("SetBigStringTryClob", "true");

	    // Load the Oracle JDBC driver class.
	    DriverManager.registerDriver(new OracleDriver());     
	     
	    // Get the database connection 
	    Connection conn = DriverManager.getConnection( this.url, props );*/

	    /*
	    try {
	      String url1 = System.getProperty("JDBC_URL");
	      if (url1 != null)
	        url = url1;
	    } catch (Exception e) {
	      // If there is any security exception, ignore it
	      // and use the default
	    }*/

	    // Create an OracleDataSouce instance and set properties
	    OracleDataSource ods = new OracleDataSource();
	    ods.setUser(userName);
	    ods.setPassword(password);
	   // ods.setURL(url);
	    ods.setDatabaseName(db);
	    ods.setURL(url);
	    //conn = ods.getConnection();
	    // Connect to the database
	    conn = DriverManager.getConnection(this.url);
	    //conn = (OracleConnection)ods.getConnection ();			
		//PreparedStatement stm=conn.prepareStatement("use "+db);
		//stm.execute();			
	}  catch (SQLException e) {
		e.printStackTrace();
	}
		
	}

}
