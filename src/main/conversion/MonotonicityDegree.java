package conversion;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import prefwork.datasource.BasicDataSource;

public class MonotonicityDegree {

	private static Logger log = Logger.getLogger(MonotonicityDegree.class);

	XMLConfiguration confRuns = new XMLConfiguration();
	XMLConfiguration confDbs = new XMLConfiguration();
	XMLConfiguration confDatasources = new XMLConfiguration();
	XMLConfiguration confMethods = new XMLConfiguration();

	@SuppressWarnings("unchecked")
	public BasicDataSource getDataSource(String dataSourceName, String dbName)
			throws SecurityException, ClassNotFoundException,
			IllegalArgumentException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		Configuration dbConf = confDbs.configurationAt(dbName);
		String dbClass = dbConf.getString("class");

		Constructor[] a = Class.forName(dbClass).getConstructors();
		BasicDataSource ds = (BasicDataSource) a[0].newInstance();
		ds.configDriver(confDbs, dbName);
		ds.configDataSource(confDatasources, dataSourceName);
		if (confDatasources.getProperty(dataSourceName + ".fillRandom") != null) {
			int fillRandomValues = confDatasources.getInt(dataSourceName
					+ ".fillRandom");

			if (fillRandomValues == 1)
				ds.fillRandomValues();
		}
		ds.setName(dataSourceName + dbName);
		return ds;
	}
	
	public void testMonotonicity(BasicDataSource dataSource){
		
	}

	@SuppressWarnings("unchecked")
	public MonotonicityDegree() {

		try {
			confRuns.setFileName("confRuns.xml");
			confDbs.setFileName("confDbs.xml");
			confDatasources.setFileName("confDatasources.xml");
			confMethods.setFileName("confMethods.xml");

			confRuns.load();
			confDbs.load();
			confDatasources.load();
			confMethods.load();

			List<String> runs = confRuns.getList("run");
			log.info("Start of testing");
			// runs = new TestRun[confRuns.getInt("threads")];
			// Iterate through runs
			for (String run : runs) {
				int dbId = 0;
				run = "runs." + run;
				// Iterate through dbs
				while (confRuns.getProperty(run + ".dbs.db(" + dbId + ").name") != null) {
					String dbName = confRuns.getString(run + ".dbs.db(" + dbId
							+ ").name");
					List<String> datasources = confRuns.getList(run
							+ ".dbs.db(" + dbId + ").datasources");
					int datasourceId = 0;
					// Iterate through datasources
					for (String datasourceName : datasources) {
						log.info("Datasource " + datasourceName);
						BasicDataSource dataSource = getDataSource(
								datasourceName, dbName);
						dataSource.configDriver(confRuns, run + ".dbs.db("
								+ dbId + ")");
						dataSource.configDataSource(confRuns, run
								+ ".dbs.db(" + dbId + ").datasources("
								+ datasourceId + ")");
						testMonotonicity(dataSource);
						datasourceId++;
					}
					dbId++;
				}
			}
			log.info("End of testing");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		PropertyConfigurator.configure("log4j.properties");
		new MonotonicityDegree();
	}
}
