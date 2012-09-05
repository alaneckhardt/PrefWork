package prefwork.datasource;

/**
 * Class that provides connection to MySQL database. 
 * @author Alan
 *
 */
public class MySQLMultiDataSource extends SQLMultiSource {

	public MySQLMultiDataSource(){
			provider = new MySQLConnectionProvider();
			randomFunction = "rand()";
	}
}
