package prefwork.datasource;

/**
 * Class that provides connection to MySQL database. 
 * @author Alan
 *
 */
public class OracleDataSource extends SQLDataSource {

	public OracleDataSource(){
		provider = new OracleConnectionProvider();
		randomFunction = "dbms_random.value";
	}
}
