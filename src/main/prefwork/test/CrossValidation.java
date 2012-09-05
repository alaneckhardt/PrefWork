package prefwork.test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;
import prefwork.method.InductiveMethod;

public class CrossValidation implements Test {
	int nfolds = 5;
	double trainRun = 0;
	double testRun = 0;
	TestInterpreter resultsInterpreter;
	TestResults results;
	protected Double[] randoms;
	private static Logger log = Logger.getLogger(CrossValidation.class);

	public void configTest(XMLConfiguration config, String section) {
		Configuration testConf = config.configurationAt(section);
		nfolds = CommonUtils.getIntFromConfIfNotNull(testConf, "nfolds", nfolds);
		trainRun = 0;
		testRun = 0;

		try {
			resultsInterpreter = CommonUtils
					.getTestInterpreter(config, section);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		resultsInterpreter.setFilePrefix(testConf.getString("path"));
	}

	public void configTrainDatasource(BasicDataSource ds) {
		if (trainRun >= nfolds)
			return;

		ds.setLimit(randoms[(int) (trainRun * (1.0 / nfolds)*randoms.length)], randoms[(int) ((trainRun + 1) * (1.0 / nfolds)*randoms.length-1)],
				false);
		//ds.restart();
		trainRun++;
	}

	public void configTestDatasource(BasicDataSource ds) {
		if (testRun >= nfolds)
			return;

		ds.setLimit(randoms[(int) ((testRun + 0.0) * (1.0 / nfolds)*randoms.length)], randoms[(int) ((testRun + 1.0)
				* (1.0 / nfolds)*randoms.length)-1], true);
		// ds.restart();
		testRun++;
	}
	protected void getRandoms(BasicDataSource ds, int userId) {
		ds.setLimit(0.0, 0.0, false);
		List<Double> rands = CommonUtils.getList();
		ds.setFixedUserId(userId);
		ds.restart();
		List<Object> rec = ds.getRecord();
		double max = 0;
		while (rec != null) {
			rands.add(CommonUtils.objectToDouble(rec.get(1)));
			if(max<CommonUtils.objectToDouble(rec.get(2)))
				max = CommonUtils.objectToDouble(rec.get(2));
			rec = ds.getRecord();
			
		}
		randoms = new Double[rands.size()];
		rands.toArray(randoms);
		Arrays.sort(randoms);
		if(randoms.length > 0)
			randoms[0]=0.0;
	}
	public void test(InductiveMethod ind, BasicDataSource trainDataSource,
			BasicDataSource testDataSource) {
		resultsInterpreter.setHeaderPrefix("date;Nfolds;dataset;method;");

		resultsInterpreter.setRowPrefix(
				"" + new Date(System.currentTimeMillis()).toString()+";"
				+ Double.toString(nfolds)+";"
				+ trainDataSource.getName() + ";"
				+ ind.toString() + ";");
		trainDataSource.setRandomColumn(trainDataSource.getAttributes()[1].getName());
		testDataSource.setRandomColumn(testDataSource.getAttributes()[1].getName());
		for (int run = 0; run < this.nfolds; run++) {
			results = new TestResults(trainDataSource);
			log.debug("Testing method " + ind.toString());

			trainDataSource.restartUserId();
			testDataSource.restartUserId();
			Integer userId = trainDataSource.getUserId();
			Integer targetAttribute = testDataSource.getTargetAttribute();
			while (userId != null) {	
				getRandoms(trainDataSource, userId);
				log.debug("Configuring " + testDataSource.getName());
				configTestDatasource(testDataSource);
				log.debug("Configuring " + trainDataSource.getName());
				configTrainDatasource(trainDataSource);
				log.debug("Training user " + userId);

				Long startUser = System.currentTimeMillis();
				int trainCount = ind.buildModel(trainDataSource, userId);
				results.setTrainCount(userId, run, trainCount);
				log.debug("User trained.");

				testDataSource.setFixedUserId(userId);
				testDataSource.restart();
				List<Object> rec = testDataSource.getRecord();
				int size = 0;
				Long endUser = System.currentTimeMillis();
				results.addBuildTimeUser(userId, run, endUser - startUser);
				log.debug("Testing user.");
				startUser = System.currentTimeMillis();
				while (rec != null) {
					Double compRes = ind.classifyRecord(rec, targetAttribute);
					size++;
					if (compRes != null && !Double.isNaN(compRes))
						results.addResult(userId, run, Integer.parseInt(rec.get(1)
								.toString()), Double.parseDouble(rec.get(
								targetAttribute).toString()), compRes);
					else
						results.addUnableToPredict(userId, run, Integer.parseInt(rec.get(1)
								.toString()), //TODO rewrite
								Double.parseDouble(rec.get(targetAttribute).toString())
								//originalRatings.get(Integer.parseInt(rec.get(1).toString()))
								);
					rec = testDataSource.getRecord();
				}
				endUser = System.currentTimeMillis();
				log.debug("User tested.");
				results.addTestTimeUser(userId,run,  endUser - startUser);
				resultsInterpreter.writeTestResults(results);
				userId = trainDataSource.getUserId();
			}
		}
	}
	public TestInterpreter getResultsInterpreter() {
		return resultsInterpreter;
	}

}
