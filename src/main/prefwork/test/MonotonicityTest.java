package prefwork.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import prefwork.CommonUtils;
import prefwork.ConfigurationParser;
import prefwork.datasource.BasicDataSource;
import prefwork.method.ContentBased;
import prefwork.method.InductiveMethod;
import prefwork.method.StatisticalBestLocal;

public class MonotonicityTest extends Bootstrap {
	private static Logger log = Logger.getLogger(MonotonicityTest.class);
	//double degreeCPtoAP;
	MonotonicityTestInterpreter resultsMonoInterpreter = new MonotonicityTestInterpreter();
	TestMonotonicityResults resultsMono;
	
	
	public MonotonicityTest(){
		resultsInterpreter = new DataMiningStatistics();
	}
	

	public void test(InductiveMethod ind, BasicDataSource trainDataSource,
			BasicDataSource testDataSource) {
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
			boolean foundBiggerTrainset = false;
			for (int trainSet : trainSets) {
				resultsInterpreter.setRowPrefix(""
						+ new Date(System.currentTimeMillis()).toString() + ";"
						+ Double.toString(trainSet) + ";"
						+ trainDataSource.getName() + ";" + ind.toString() + ";");
				log.info("trainSet " + trainSet);		
				for (int i = 0; i < userIds.size(); i++) {
					userId = userIds.get(i);		
					randoms = getRandoms(trainDataSource, userId);

					if(trainSet > randoms.length){
						if(foundBiggerTrainset){
							continue;
						}
						else{
							//If the trainset is bigger, we do a test one time with the size of the entire dataset.
							trainSet = randoms.length;
							foundBiggerTrainset = true;
						}
							
					} 
					else if(trainSet == randoms.length){
						foundBiggerTrainset = true;						
					}
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
						configTrainDatasource(testDataSource, runInner, trainSet, randoms);
						//configTestDatasource(testDataSource, runInner, trainSet, randoms);
						configTrainDatasource(trainDataSource, runInner, trainSet, randoms);
						Long startBuildUser = System.currentTimeMillis();
						int trainCount = ind.buildModel(trainDataSource, userId);
						results.setTrainCount(userId,  run,trainCount);
						Long endBuildUser = System.currentTimeMillis();
						results.addBuildTimeUser(userId, run, endBuildUser - startBuildUser);
						trainTime = endBuildUser - startBuildUser;
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
					//log.error(Arrays.deepToString(((StatisticalBestLocal)ind).getNomCount()));
					//log.error(Arrays.deepToString(((StatisticalBestLocal)ind).getNumCount()));
				}
				//log.debug("User tested.");
			}

		}
		log.info("Ended method " + ind.toString());
	}
	
	// if o1 == o2 in all attributes (without the head)
	public static boolean equalsInAllAttributes(Double[] o1, Double[] o2) {
		for (int i=3; i<o1.length; i++) {
			if (o1[i]==null || o2[i]==null || (o1[i] == Double.MIN_VALUE) || (o2[i] == Double.MIN_VALUE))
				return false;
			if (!o1[i].equals(o2[i])) {
				return false;
			}
		}
		return true;
	}

	// if o1 <= o2 in all attributes (without the head)
	public static boolean leqInAttributes(Double[] o1, Double[] o2) {
		for (int i=3; i<o1.length; i++) {
			if (o1[i]==null || o2[i]==null || (o1[i] == Double.MIN_VALUE) || (o2[i] == Double.MIN_VALUE))
				return false;
			if (o1[i] > o2[i]) {
				return false;
			}
		}
		return true;
	}
	long allComparablePairs=0;
	long corruptedPairs=0;
	long allPairs=0;
	long trainTime=0;
	long testTime=0;
	
	public static long[] computeRatingChanges(List<Double[]> records) {
		long allComparablePairs=0;
		long corruptedPairs=0;
		long allPairs=0;		
		int numberOfObjects = records.size(); 
		for (int i=0; i<numberOfObjects-1; i++) {
			Double[] o1 = records.get(i);
			for (int j=i+1; j<numberOfObjects; j++) {
				Double[] o2 = records.get(j);
				if(o1[2] == null  ||  o2[2] == null){
					continue;
				}
				allPairs++;
				if (equalsInAllAttributes(o1,o2)) {
					allComparablePairs++;
					if (!o1[2].equals(o2[2])) {
						corruptedPairs++;
					}
				} else
				if (leqInAttributes(o1,o2)) {
					allComparablePairs++;
					if (o1[2] > o2[2]) {
						corruptedPairs++;
					}
				} else
				if (leqInAttributes(o2,o1)) {
					allComparablePairs++;
					if (o2[2] > o1[2]) { 
						corruptedPairs++;
					}
				}
			}
		}

//		long allPairs = (numberOfObjects * (numberOfObjects-1))/2;
		//double degree = (double) (allComparablePairs - corruptedPairs) / allComparablePairs;
		/*
		degreeCPtoAP = (double) allComparablePairs / allPairs;
		degreeCPtoAP = degreeCPtoAP * 100;*/
		//System.out.print(""+degreeCPtoAP + "\t"+degreeCPtoAP+"\n" );
		return new long[]{allComparablePairs,corruptedPairs,allPairs};
	}  
	

	synchronized public void writeTestResults(String filePrefix, String headerPrefix, String rowPrefix, int userId) {
		try {
			File f = new File(filePrefix + "2.csv");
			BufferedWriter out;
			if (!f.exists()) {
				out = new BufferedWriter(new FileWriter(filePrefix + "2.csv",
						true));
				out
						.write(headerPrefix
								+ "userId;run;Com-Corr/All;Com/All;Corr/All;Comp;Corr;All;Com-Corr/Com;trainTime;testTime\n");
			} else

			out = new BufferedWriter(new FileWriter(filePrefix + "2.csv",
						true));
			out.write((rowPrefix + userId + ";" + run + ";"
					+ (double) (allComparablePairs - corruptedPairs)/ allPairs + ";" 
					+ (double) allComparablePairs / allPairs + ";"
					+ (double) corruptedPairs / allPairs + ";"
					+ allComparablePairs + ";" 
					+ corruptedPairs + ";"
					+ allPairs + ";" 
					+ (double) (allComparablePairs - corruptedPairs)/ allComparablePairs + ";" 
					+ trainTime + ";"
					+ testTime + ";"
					+ "\n").replace('.', ','));
			out.flush();
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	
	protected void testMethod(InductiveMethod ind, Integer userId, BasicDataSource testDataSource, Integer targetAttribute )
	{
		testDataSource.setFixedUserId(userId);
		testDataSource.restart();
		ContentBased cb = (ContentBased)ind;
		List<Object> rec = testDataSource.getRecord();
		int size = 0;
		Long startTestUser = 0L;
		Long endTestUser = 0L;
		List<Double[]> records = CommonUtils.getList();
		startTestUser = System.currentTimeMillis();
		while (rec != null) {
			Double[] locPrefRec = CommonUtils.getLocalPref(rec, cb.getAttributes());
			records.add(locPrefRec);
			size++;
			rec = testDataSource.getRecord();
		}		
		long[] res = computeRatingChanges(records);
		endTestUser =System.currentTimeMillis()-startTestUser;
		testTime = endTestUser;
		allComparablePairs  = res[0];
		corruptedPairs = res[1];
		allPairs = res[2];

		synchronized (ConfigurationParser.semWrite) {
			writeTestResults(resultsInterpreter.getFilePrefix(),resultsInterpreter.getHeaderPrefix(),resultsInterpreter.getRowPrefix(),
					userId);
			ConfigurationParser.semWrite.release();						
		}
		
		results.addTestTimeUser(userId, run, endTestUser);
	}

}
