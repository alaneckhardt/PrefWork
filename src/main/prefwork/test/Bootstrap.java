package prefwork.test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import prefwork.CommonUtils;
import prefwork.ConfigurationParser;
import prefwork.datasource.BasicDataSource;
import prefwork.method.InductiveMethod;

public class Bootstrap implements Test {
	protected int[] trainSets;
	protected TestInterpreter resultsInterpreter;
	protected TestResults results;
	protected int run = 0;
	protected int numberOfRuns = 0;
	private static Logger log = Logger.getLogger(Bootstrap.class);
	protected Double[] randoms;

	@SuppressWarnings("unchecked")
	public void configTest(XMLConfiguration config, String section) {
		Configuration testConf = config.configurationAt(section);
		trainSets = CommonUtils.stringListToIntArray(testConf
				.getList("trainSets"));
		numberOfRuns = CommonUtils.getIntFromConfIfNotNull(testConf, "numberOfRuns", numberOfRuns);
		try {
			resultsInterpreter = CommonUtils
					.getTestInterpreter(config, section);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		resultsInterpreter.setFilePrefix(testConf.getString("path"));

	}

	/**
	 * Gets all random numbers that are present for a given user.
	 * 
	 * @param ds
	 */
	protected static Double[] getRandoms(BasicDataSource ds, int userId) {
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
	//		System.out.print(rec.get(1)+", ");
			rec = ds.getRecord();
			
		}
//		System.out.println(" ");
		Double[] randoms = new Double[rands.size()];
		rands.toArray(randoms);
		Arrays.sort(randoms);
		if(randoms.length > 0)
			randoms[0]=0.0;
		return randoms;
		//ds.setAttributes(attrs);
	}
	
	public static void configTrainDatasource(BasicDataSource ds, int run, int trainSet, Double[] randoms) {
		if((run + 1) * trainSet==randoms.length)
			ds.setLimit(randoms[(run) * trainSet], randoms[randoms.length-1]+1, true);
		else
			ds.setLimit(randoms[(run) * trainSet], randoms[(run+1) * trainSet],
				true);
	}
/*randoms.length-1-*/
	public static void configTestDatasource(BasicDataSource ds, int run, int trainSet, Double[] randoms) {
		if((run + 1) * trainSet==randoms.length)
			ds.setLimit(randoms[(run) * trainSet], randoms[randoms.length-1]+1, false);
		else
			ds.setLimit(randoms[(run) * trainSet], randoms[(run+1) * trainSet], false);
	}

	protected void testMethod(InductiveMethod ind, Integer userId, BasicDataSource testDataSource, Integer targetAttribute ){
		testDataSource.setFixedUserId(userId);
		testDataSource.restart();
		List<Object> rec = testDataSource.getRecord();
		int size = 0;
		//log.debug("Testing user.");
		Long startTestUser = 0L;
		Long endTestUser = 0L;
		while (rec != null) {
			//System.out.print(rec.get(2)+", ");
			startTestUser = System.currentTimeMillis();
			Double compRes = ind.classifyRecord(rec,
					targetAttribute);
			endTestUser+=System.currentTimeMillis()-startTestUser;
			size++;
			if (compRes != null && !Double.isNaN(compRes) && !Double.isInfinite(compRes))
				results.addResult(userId, run, Integer.parseInt(rec.get(
						1).toString()), // TODO rewrite
						CommonUtils.objectToDouble(rec.get(targetAttribute)
								)
						// originalRatings.get(Integer.parseInt(rec.get(1).toString()))
						, compRes);
			else
				results.addUnableToPredict(userId, run, Integer
						.parseInt(rec.get(1).toString()), // TODO
						// rewrite
						CommonUtils.objectToDouble(rec.get(targetAttribute)
								)
				// originalRatings.get(Integer.parseInt(rec.get(1).toString()))
						);

			rec = testDataSource.getRecord();
		}

		//System.out.println(" ");
		results.addTestTimeUser(userId, run, endTestUser);
	}

	public void test(InductiveMethod ind, BasicDataSource trainDataSource,
			BasicDataSource testDataSource) {
		// loadOriginalRatings();
		resultsInterpreter.setHeaderPrefix("date;ratio;dataset;method;");
		results = new TestResults(trainDataSource);
		log.info("Testing method " + ind.toString());
			log.debug("Configuring " + testDataSource.getName());
			trainDataSource.restartUserId();
			List<Integer> userIds = CommonUtils.getList();
			Integer userId = trainDataSource.getUserId();
			while(userId != null){
				userIds.add(userId);
				userId = trainDataSource.getUserId();
			}
			trainDataSource.setRandomColumn(trainDataSource.getAttributes()[1].getName());
			testDataSource.setRandomColumn(testDataSource.getAttributes()[1].getName());
			testDataSource.fillRandomValues();
			trainDataSource.fillRandomValues();
			Integer targetAttribute = testDataSource.getTargetAttribute();

			for (int trainSet : trainSets) {
				resultsInterpreter.setRowPrefix(""
						+ new Date(System.currentTimeMillis()).toString() + ";"
						+ Double.toString(trainSet) + ";"
						+ trainDataSource.getName() + ";" + ind.toString() + ";");
				log.info("trainSet " + trainSet);		
				for (int i = 0; i < userIds.size(); i++) {
					userId = userIds.get(i);		
					randoms = getRandoms(trainDataSource, userId);

					if(trainSet > randoms.length)
						continue;
					run = 0;
					//testDataSource.setLimit(randoms[0], randoms[trainSet-1], false);
					//trainDataSource.setLimit(randoms[0], randoms[trainSet-1], true);
					results.reset();
				while (run   < numberOfRuns) {
					testDataSource.fillRandomValues();
					trainDataSource.fillRandomValues();
					int runInner = 0;
					while ((runInner + 1) * trainSet  <= randoms.length && run   < numberOfRuns) {
					//	log.info("run "+run+", tr "+trainSet);
						trainDataSource.restartUserId();
						configTestDatasource(testDataSource, runInner, trainSet, randoms);
						configTrainDatasource(trainDataSource, runInner, trainSet, randoms);
						Long startBuildUser = System.currentTimeMillis();
						int trainCount = ind.buildModel(trainDataSource, userId);
						results.setTrainCount(userId,  run,trainCount);
						Long endBuildUser = System.currentTimeMillis();
						results.addBuildTimeUser(userId, run, endBuildUser - startBuildUser);
						testMethod(ind, userId,testDataSource,targetAttribute);
						run++;
						runInner++;
					}
				}
				synchronized (ConfigurationParser.semWrite) {
					resultsInterpreter.setRowPrefix(""
							+ new Date(System.currentTimeMillis()).toString() + ";"
							+ Double.toString(trainSet) + ";"
							+ trainDataSource.getName() + ";" + ind.toString() + ";");
					ConfigurationParser.semWrite.acquire();
					resultsInterpreter.writeTestResults(results);
					ConfigurationParser.semWrite.release();						
				}
				//log.debug("User tested.");
			}

		}
		log.info("Ended method " + ind.toString());
	}

	@Override
	public TestInterpreter getResultsInterpreter() {
		return resultsInterpreter;
	}

}
