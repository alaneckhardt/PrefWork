package prefwork.test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import prefwork.Attribute;
import prefwork.CommonUtils;
import prefwork.ConfigurationParser;
import prefwork.datasource.BasicDataSource;
import prefwork.datasource.RatingCollaborative;
import prefwork.method.InductiveMethod;

public class TestTrainAbsoluteCollaborative implements Test {
	protected int[] trainSets;
	protected TestInterpreter resultsInterpreter;
	protected TestResults results;
	protected int numberOfruns = 20;
	protected int run = 0;
	protected int totalRun = 0;
	private static Logger log = Logger.getLogger(TestTrainAbsoluteCollaborative.class);
	protected Double[] randoms;
	protected List<Integer> userIds;
	public static int usersToTest = 100;

	@SuppressWarnings("unchecked")
	public void configTest(XMLConfiguration config, String section) {
		Configuration testConf = config.configurationAt(section);
		trainSets = CommonUtils.stringListToIntArray(testConf
				.getList("trainSets"));
		numberOfruns = CommonUtils.getIntFromConfIfNotNull(testConf, "numberOfruns", numberOfruns);
		usersToTest = CommonUtils.getIntFromConfIfNotNull(testConf, "usersToTest", usersToTest);
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
		Attribute[] attrs = ds.getAttributes();
		Attribute[] attrs2 = new Attribute[4];
		//We want the object id, user id and rating just for sure
  		attrs2[0]=new Attribute(null, 0,attrs[0].getName()/*"randomize"*/);
  		attrs2[1]=new Attribute(null, 1,attrs[1].getName()/*"randomize"*/);
  		attrs2[2]=new Attribute(null, 2,attrs[2].getName()/*"randomize"*/);
		//We use the id column as the randomization
  		attrs2[3]=new Attribute(null, 3,attrs[1].getName()/*"randomize"*/);
		
		ds.setAttributes(attrs2);
		ds.setFixedUserId(userId);
		ds.restart();
		List<Object> rec = ds.getRecord();
		double max = 0;
		while (rec != null) {
			rands.add(CommonUtils.objectToDouble(rec.get(3)));
			if(max<CommonUtils.objectToDouble(rec.get(2)))
				max = CommonUtils.objectToDouble(rec.get(2));
			rec = ds.getRecord();
		}
		randoms = new Double[rands.size()];
		rands.toArray(randoms);
		Arrays.sort(randoms);
		if(randoms.length > 0)
			randoms[0]=0.0;
		ds.setAttributes(attrs);
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
			//System.out.print(rec.get(1)+", ");
			startTestUser = System.currentTimeMillis();
			Double compRes = ind.classifyRecord(rec,
					targetAttribute);
			endTestUser+=System.currentTimeMillis()-startTestUser;
			size++;
			if (compRes != null && !Double.isNaN(compRes))
				results.addResult(userId, totalRun, Integer.parseInt(rec.get(
						1).toString()), // TODO rewrite
						CommonUtils.objectToDouble(rec.get(targetAttribute)
								)
						// originalRatings.get(Integer.parseInt(rec.get(1).toString()))
						, compRes);
			else
				results.addUnableToPredict(userId, totalRun, Integer
						.parseInt(rec.get(1).toString()), // TODO
						// rewrite
						CommonUtils.objectToDouble(rec.get(targetAttribute)
								)
				// originalRatings.get(Integer.parseInt(rec.get(1).toString()))
						);

			rec = testDataSource.getRecord();
		}

		//System.out.println(" ");
		results.addTestTimeUser(userId,  totalRun,endTestUser);
	}
	private int getCount(RatingCollaborative source, int userId){
		int count=0;
		source.setFixedUserId(userId);
		source.restart();
		List<Object> rec = source.getRecord();
		while (rec != null) {
			count++;
			//System.out.print(rec.get(1)+", ");
			rec = source.getRecord();
		}
		//System.out.println(" ");
		return count;

	}
	
	protected void loadUserIds(BasicDataSource trainDataSource){
		userIds = CommonUtils.getList();
		trainDataSource.restartUserId();
		Integer userId = trainDataSource.getUserId();
		while (userId != null) {
			userIds.add(userId);
			userId = trainDataSource.getUserId();
		}
		trainDataSource.restartUserId();
	}
	
	
	public void test(InductiveMethod ind, BasicDataSource trainDataSource,
			BasicDataSource testDataSourc2e) {
		// loadOriginalRatings();
		resultsInterpreter.setHeaderPrefix("date;ratio;dataset;method;");
		loadUserIds(trainDataSource);
		results = new TestResults(trainDataSource);
		RatingCollaborative trainSource = (RatingCollaborative)trainDataSource;
		//RatingCollaborative testSource = (RatingCollaborative)testDataSource;
		log.info("Testing method " + ind.toString());
			log.debug("Configuring " + trainDataSource.getName());
			int user = 0;
			Integer targetAttribute = trainDataSource.getTargetAttribute();
				//Run 0
				trainSource.setRun(0);
				//testSource.setRun(0);
				//We want all records
				trainSource.setTrainSetSize(0);
				//testSource.setTrainSetSize(0);
				
				int count = getCount(trainSource, 0);		
				for (int trainSet : trainSets) {
					results.reset();
					run = 0;
					totalRun = 0;
					log.info("trainSet " + trainSet + ", method "+ind.toString());
					//testSource.setTrainSetSize(trainSet);
					while (totalRun < numberOfruns) {
						while ((run + 1) * trainSet < count && totalRun < numberOfruns) {
							System.gc();
							//Setting the test mode.
							trainSource.setTestMode(false);
							trainSource.setRun(run);
							trainSource.setTrainSetSize(trainSet);
		
							// testSource.setRun(run);
							Integer userId = userIds.get(user);
							Long startBuildUser = System.currentTimeMillis();
							int trainCount = ind.buildModel(trainDataSource, userId);
							results.setTrainCount(userId, totalRun, trainCount);
		
							// Setting the test mode.
							trainSource.setTestMode(true);
		
							// log.info("User " + userId+", method "+ind.toString());
							Long endBuildUser = System.currentTimeMillis();
							log.info("totalRun "+totalRun+", tr "+trainSet);
							while (user < usersToTest) {
								userId = userIds.get(user);
								results.addBuildTimeUser(userId, totalRun, endBuildUser
										- startBuildUser);
								testMethod(ind, userId, trainSource, targetAttribute);
								user++;
							}
							run++;
							totalRun++;
							user = 0;
						}
						if(totalRun<numberOfruns){
							trainSource.fillRandomValues();
							run = 0;
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
	public void testOneMEthod(InductiveMethod ind,
			BasicDataSource trainDataSource, BasicDataSource testDataSourc2e) {
		// loadOriginalRatings();
		resultsInterpreter.setHeaderPrefix("date;ratio;dataset;method;");
		loadUserIds(trainDataSource);
		results = new TestResults(trainDataSource);
		RatingCollaborative trainSource = (RatingCollaborative) trainDataSource;
		// RatingCollaborative testSource = (RatingCollaborative)testDataSource;
		log.info("Testing method " + ind.toString());
		log.debug("Configuring " + trainDataSource.getName());
		int user = 0;
		Integer targetAttribute = trainDataSource.getTargetAttribute();
		// Run 0
		trainSource.setRun(0);
		// testSource.setRun(0);
		// We want all records
		trainSource.setTrainSetSize(0);
		// testSource.setTrainSetSize(0);

		int count = getCount(trainSource, 0);
		for (int trainSet : trainSets) {
			log.info("trainSet " + trainSet + ", method " + ind.toString());
			
			user = 0;
			while (user < usersToTest) {
				int userId = userIds.get(user);
				results.reset();
				run = 0;
				totalRun = 0;
				// testSource.setTrainSetSize(trainSet);
				while (totalRun < numberOfruns) {
					while ((run + 1) * trainSet < count
							&& totalRun < numberOfruns) {

						log.info("totalRun " + totalRun);
						// Setting the test mode.
						trainSource.setTestMode(false);
						trainSource.setRun(run);
						trainSource.setTrainSetSize(trainSet);

						Long startBuildUser = System.currentTimeMillis();
						int trainCount = ind
								.buildModel(trainDataSource, userId);
						results.setTrainCount(userId, totalRun, trainCount);

						// Setting the test mode.
						trainSource.setTestMode(true);

						// log.info("User " +
						// userId+", method "+ind.toString());
						Long endBuildUser = System.currentTimeMillis();
						// log.info("run "+run+", tr "+trainSet);
						results.addBuildTimeUser(userId, totalRun, endBuildUser
								- startBuildUser);
						testMethod(ind, userId, trainSource, targetAttribute);
						run++;
						totalRun++;
					}
					if (totalRun < numberOfruns) {
						trainSource.fillRandomValues();
						run = 0;
					}
				}
				user++;
				synchronized (ConfigurationParser.semWrite) {
					resultsInterpreter.setRowPrefix(""
							+ new Date(System.currentTimeMillis()).toString() + ";"
							+ Double.toString(trainSet) + ";"
							+ trainDataSource.getName() + ";" + ind.toString() + ";");
					ConfigurationParser.semWrite.acquire();
					resultsInterpreter.writeTestResults(results);
					ConfigurationParser.semWrite.release();
				}
			}
		}
	}
	@Override
	public TestInterpreter getResultsInterpreter() {
		return resultsInterpreter;
	}

}
