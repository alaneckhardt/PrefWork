package prefwork.test;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;
import prefwork.method.InductiveMethod;

public class AlphaCuts implements Test {
	TestResults results;
	TestInterpreter resultsInterpreter;
	int run = 0;
	double ratio;
	int[] cuts = new int[]{1,2,3,4,5};
	
	private static Logger log = Logger.getLogger(AlphaCuts.class);

	public void configTest(XMLConfiguration config, String section) {
		Configuration testConf = config.configurationAt(section);

		try {
			resultsInterpreter = CommonUtils.getTestInterpreter(config, section);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		resultsInterpreter.setFilePrefix(testConf.getString("path"));
	//	results.setPath(testConf.getString("path"));
	//	results.setTestParamsHeader("AlphaCuts;"+cuts);
	}

	public void configTrainDatasource(BasicDataSource ds) {
		ds.setLimit(run * ratio, (run +1) * ratio, true);
		ds.restartUserId();
	}

	public void configTestDatasource(BasicDataSource ds) {
		ds.setLimit(run * ratio, (run +1) * ratio, false);
		ds.restartUserId();
	}

	public void test(InductiveMethod ind, BasicDataSource trainDataSource,
			BasicDataSource testDataource) {
		//loadOriginalRatings();
		while ((run +1) * ratio <= 1) {
			results = new TestResults(trainDataSource);
			results.reset();

			log.debug("Testing method " + ind.toString());

			log.debug("Configuring " + testDataource.getName());
			configTestDatasource(testDataource);
			log.debug("Configuring " + trainDataSource.getName());
			configTrainDatasource(trainDataSource);
			Integer userId = trainDataSource.getUserId();
			Integer targetAttribute = testDataource.getTargetAttribute();
			while (userId != null) {
				results.reset();
				log.debug("Training user " + userId);

				Long startUser = System.currentTimeMillis();
				int trainCount = ind.buildModel(trainDataSource, userId);
				results.setTrainCount(userId, run, trainCount);
				log.debug("User trained.");

				testDataource.setFixedUserId(userId);
				testDataource.restart();
				List<Object> rec = testDataource.getRecord();
				int size = 0;
				Long endUser = System.currentTimeMillis();
				results.addBuildTimeUser(userId, run, endUser - startUser);
				log.debug("Testing user.");
				startUser = System.currentTimeMillis();
				while (rec != null) {
					Double compRes = ind.classifyRecord(rec, targetAttribute);
					size++;
					if (compRes != null && !Double.isNaN(compRes))
						results.addResult(userId,run, Integer.parseInt(rec.get(1)
								.toString()), //TODO rewrite
								Double.parseDouble(rec.get(targetAttribute).toString())
								//originalRatings.get(Integer.parseInt(rec.get(1).toString()))
								,compRes);
					else
						results.addUnableToPredict(userId, run, Integer.parseInt(rec.get(1)
								.toString()), //TODO rewrite
								Double.parseDouble(rec.get(targetAttribute).toString())
								//originalRatings.get(Integer.parseInt(rec.get(1).toString()))
								);

					rec = testDataource.getRecord();
				}
				endUser = System.currentTimeMillis();
				log.debug("User tested.");
				results.addTestTimeUser(userId, run, endUser - startUser);
				userId = trainDataSource.getUserId();
			}
			resultsInterpreter.writeTestResults(results);
			run++;
		}
	}
	public TestInterpreter getResultsInterpreter() {
		return resultsInterpreter;
	}

}
