package prefwork.datasource;

/**
 * Class that provides connection to MySQL database. 
 * @author Alan
 *
 */
public class OracleMultiDataSource extends SQLMultiSource {

	public OracleMultiDataSource(){
		randomFunction = "dbms_random.value";
		provider = new OracleConnectionProvider();
	}
}
