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

public class TestTrainAbsolute implements Test {
	protected int[] trainSets;
	protected TestInterpreter resultsInterpreter;
	protected TestResults results;
	protected int run = 0;
	private static Logger log = Logger.getLogger(TestTrainAbsolute.class);
	protected Double[] randoms;

	@SuppressWarnings("unchecked")
	public void configTest(XMLConfiguration config, String section) {
		Configuration testConf = config.configurationAt(section);
		trainSets = CommonUtils.stringListToIntArray(testConf
				.getList("trainSets"));
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
	protected void getRandoms(BasicDataSource ds, int userId) {
		ds.setLimit(0.0, 0.0, false);
		List<Double> rands = CommonUtils.getList();
		/*Attribute[] attrs = ds.getAttributes();
		Attribute[] attrs2 = new Attribute[4];
		//We want the object id, user id and rating just for sure
  		attrs2[0]=new Attribute(null, 0,attrs[0].getName());
  		attrs2[1]=new Attribute(null, 1,attrs[1].getName());
  		attrs2[2]=new Attribute(null, 2,attrs[2].getName());
		//We use the id column as the randomization
  		attrs2[3]=new Attribute(null, 3,attrs[1].getName());
		
		ds.setAttributes(attrs2);*/
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
		randoms = new Double[rands.size()];
		rands.toArray(randoms);
		Arrays.sort(randoms);
		if(randoms.length > 0)
			randoms[0]=0.0;
		//ds.setAttributes(attrs);
	}

	public void configTrainDatasource(BasicDataSource ds, int run, int trainSet) {
		if((run + 1) * trainSet==randoms.length)
			ds.setLimit(randoms[(run) * trainSet], randoms[randoms.length-1]+1, true);
		else
			ds.setLimit(randoms[(run) * trainSet], randoms[(run+1) * trainSet],
				true);
	}
/*randoms.length-1-*/
	public void configTestDatasource(BasicDataSource ds, int run, int trainSet) {
		if((run + 1) * trainSet==randoms.length)
			ds.setLimit(randoms[(run) * trainSet], randoms[randoms.length-1]+1, false);
		else
			ds.setLimit(randoms[(run) * trainSet], randoms[(run+1) * trainSet], false);
	}
	
	private void testMethod(InductiveMethod ind, Integer userId, BasicDataSource testDataSource, Integer targetAttribute ){
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
			trainDataSource.setRandomColumn(trainDataSource.getAttributes()[1].getName());
			testDataSource.setRandomColumn(testDataSource.getAttributes()[1].getName());
			Integer userId = trainDataSource.getUserId();
			Integer targetAttribute = testDataSource.getTargetAttribute();
			while (userId != null) {
				log.info("User " + userId);				
				getRandoms(trainDataSource, userId);

				for (int trainSet : trainSets) {
					run = 0;
					results.reset();
				while ((run + 1) * trainSet  <= randoms.length) {
				//	log.info("run "+run+", tr "+trainSet);
					configTestDatasource(testDataSource, run, trainSet);
					configTrainDatasource(trainDataSource, run, trainSet);
					Long startBuildUser = System.currentTimeMillis();
					int trainCount = ind.buildModel(trainDataSource, userId);
					results.setTrainCount(userId,  run,trainCount);
					Long endBuildUser = System.currentTimeMillis();
					results.addBuildTimeUser(userId, run, endBuildUser - startBuildUser);
					testMethod(ind, userId,testDataSource,targetAttribute);
					run++;
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
			userId = trainDataSource.getUserId();
		}
		log.info("Ended method " + ind.toString());
	}

	@Override
	public TestInterpreter getResultsInterpreter() {
		return resultsInterpreter;
	}

}
