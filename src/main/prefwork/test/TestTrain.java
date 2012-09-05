package prefwork.test;

import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.DefaultConfigurationBuilder.ConfigurationProvider;
import org.apache.log4j.Logger;

import prefwork.CommonUtils;
import prefwork.ConfigurationParser;
import prefwork.datasource.BasicDataSource;
import prefwork.method.InductiveMethod;

public class TestTrain implements Test {
	double[] ratios;
	TestInterpreter resultsInterpreter;
	TestResults results;
	int run = 0;
	private static Logger log = Logger.getLogger(TestTrain.class);

	@SuppressWarnings("unchecked")
	public void configTest(XMLConfiguration config, String section) {
		Configuration testConf = config.configurationAt(section);
		ratios = CommonUtils.stringListToDoubleArray(testConf.getList("ratio"));
		try {
			resultsInterpreter = CommonUtils
					.getTestInterpreter(config, section);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		resultsInterpreter.setFilePrefix(testConf.getString("path"));

	}

	public void configTrainDatasource(BasicDataSource ds, double ratio) {
		ds.setLimit(run * ratio, (run + 1) * ratio, true);
	}

	public void configTestDatasource(BasicDataSource ds, double ratio) {
		ds.setLimit(run * ratio, (run + 1) * ratio, false);
	}

	public void test(InductiveMethod ind, BasicDataSource trainDataSource,
			BasicDataSource testDataource) {
		// loadOriginalRatings();

		results = new TestResults(trainDataSource);
		resultsInterpreter
		.setHeaderPrefix("date;ratio;dataset;method;");
		Integer targetAttribute = testDataource
		.getTargetAttribute();
		for (double ratio : ratios) {
				log.info("Testing method " + ind.toString() + ", ratio:"
					+ ratio);

				trainDataSource.restartUserId();
				Integer userId = trainDataSource.getUserId();
				while (userId != null) {
					log.debug("Testing user " + userId);
					testDataource.setFixedUserId(userId);
					run = 0;

				while ((run + 1) * ratio <= 1) {
					resultsInterpreter.setRowPrefix(""
							+ new Date(System.currentTimeMillis()).toString() + ";"
							+ Double.toString(ratio) + ";"
							+ trainDataSource.getName() + ";" + ind.toString()
							+ ";");

					log.debug("Configuring " + testDataource.getName());
					configTestDatasource(testDataource, ratio);
					configTrainDatasource(trainDataSource, ratio);
					results.reset();

					Long startUser = System.currentTimeMillis();
					int trainCount = ind.buildModel(trainDataSource, userId);
					results.setTrainCount(userId,  run,trainCount);

					testDataource.restart();
					List<Object> rec = testDataource.getRecord();
					int size = 0;
					Long endUser = System.currentTimeMillis();
					results.addBuildTimeUser(userId,  run,endUser - startUser);
					startUser = System.currentTimeMillis();
					while (rec != null) {
						Double compRes = ind.classifyRecord(rec,
								targetAttribute);
						size++;
						if (compRes != null && !Double.isNaN(compRes))
							results.addResult(userId, run, Integer.parseInt(rec.get(
									1).toString()), // TODO rewrite
									CommonUtils.objectToDouble(rec
											.get(targetAttribute))
									// originalRatings.get(Integer.parseInt(rec.get(1).toString()))
									, compRes);
						else
							results.addUnableToPredict(userId, run, Integer
									.parseInt(rec.get(1).toString()), // TODO
									// rewrite
									CommonUtils.objectToDouble(rec
											.get(targetAttribute))
							// originalRatings.get(Integer.parseInt(rec.get(1).toString()))
									);

						rec = testDataource.getRecord();
					}
					endUser = System.currentTimeMillis();
					results.addTestTimeUser(userId, run, endUser - startUser);
					synchronized (ConfigurationParser.semWrite) {						
						ConfigurationParser.semWrite.acquire();
						resultsInterpreter.writeTestResults(results);
						ConfigurationParser.semWrite.release();
					}
					run++;
				}
				userId = trainDataSource.getUserId();
			}
		}
	}
	public TestInterpreter getResultsInterpreter() {
		return resultsInterpreter;
	}

}
