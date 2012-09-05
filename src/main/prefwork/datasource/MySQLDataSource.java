package prefwork.datasource;

/**
 * Class that provides connection to MySQL database. 
 * @author Alan
 *
 */
public class MySQLDataSource extends SQLDataSource {

	public MySQLDataSource(){
		provider = new MySQLConnectionProvider();
		randomFunction = "rand()";
	}

}
