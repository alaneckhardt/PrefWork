package prefwork.test;

import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;
import prefwork.datasource.PhasesSource;
import prefwork.method.InductiveMethod;

public class Phases implements Test {

	int[] trainSetSizes = { 20 };
	int[] phasesCounts = { 20 };
	int[] classCounts = { 5 };
	int maxTrain = 500;
	TestInterpreter resultsInterpreter;
	TestResults results;
	private static Logger log = Logger.getLogger(Phases.class);

	@SuppressWarnings("unchecked")
	public void configTest(XMLConfiguration config, String section) {
		Configuration testConf = config.configurationAt(section);
		trainSetSizes = CommonUtils.stringListToIntArray(testConf
				.getList("trainSetSize"));
		phasesCounts = CommonUtils.stringListToIntArray(testConf
				.getList("phasesCount"));
		classCounts = CommonUtils.stringListToIntArray(testConf
				.getList("classCount"));
		maxTrain = CommonUtils.getIntFromConfIfNotNull(testConf, "maxTrain", maxTrain);
		try {
			resultsInterpreter = CommonUtils.getTestInterpreter(config, section);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		resultsInterpreter.setFilePrefix(testConf.getString("path"));
	}

	public void configDatasource(BasicDataSource ds) {
		//ds.setFixedUserId(null);
		ds.restartUserId();
		ds.restart();
	}

	/**
	 * Executes one phase - trains the method, updates the ratings of all
	 * objects and prepares new training set for the next phase.
	 * 
	 * @param ind
	 *            Method to be trained
	 * @param phasesSource
	 *            Datasource that fetch data.
	 * @param userId
	 *            Current user.
	 */
	protected void doPhase(InductiveMethod ind, PhasesSource phasesSource,
			int targetAttribute, int userId) {
		Long startUser = System.currentTimeMillis();
		int trainCount = ind.buildModel(phasesSource, userId);
		trainCount +=results.getStat(userId, 0).countTrain;
		results.setTrainCount(userId, 0, trainCount);
		Long endUser = System.currentTimeMillis();
		results.addBuildTimeUser(userId, 0, endUser - startUser);
		phasesSource.setEvaluationMode(true);
		List<Object> rec = phasesSource.getRecord();
		int size = 0;
		while (rec != null) {
			Double compRes = ind.classifyRecord(rec, targetAttribute);
			size++;
			// We associate the computed rating to the record. It is then used
			// for enlarging the training set for the next phase.
			if (compRes != null && !Double.isNaN(compRes)) {
				rec.set(2, compRes);
			}
			// TODO What to do here?
			else{
				//rec.set(2, compRes);
			}

			rec = phasesSource.getRecord();
		}
		phasesSource.setEvaluationMode(false);
	}

	protected void testMethod(InductiveMethod ind, PhasesSource phasesSource,
			int targetAttribute, int userId) {
		Long startUser = System.currentTimeMillis();
		log.debug("User trained.");
		phasesSource.setTestMode(true);
		List<Object> rec = phasesSource.getRecord();
		int size = 0;
		log.debug("Testing method.");
		while (rec != null) {
			Double compRes = ind.classifyRecord(rec, targetAttribute);
			size++;
			if (compRes != null && !Double.isNaN(compRes))
				results.addResult(userId, 0, Integer.parseInt(rec.get(1)
						.toString()), CommonUtils.objectToDouble(rec.get(
						targetAttribute)), compRes);
			else
				results.addUnableToPredict(userId, 0, Integer.parseInt(rec.get(1)
						.toString()), CommonUtils.objectToDouble(rec.get(
						targetAttribute)));

			rec = phasesSource.getRecord();
		}
		Long endUser = System.currentTimeMillis();
		log.debug("User tested.");
		results.addTestTimeUser(userId, 0, endUser - startUser);
		phasesSource.setTestMode(false);
	}

	private void test(InductiveMethod ind, BasicDataSource trainDataSource,
			BasicDataSource testDataource, int trainSetSize, int phasesCount,
			int classCount) {

		results = new TestResults(trainDataSource);
		results.reset();
		log.debug("Configuring " + trainDataSource.getName());
		configDatasource(trainDataSource);
		Integer targetAttribute = testDataource.getTargetAttribute();
		PhasesSource phasesSource = new PhasesSource(trainDataSource,
				trainSetSize, classCount);
		Integer userId = phasesSource.getUserId();
		while (userId != null) {
			log.debug("Training user " + userId);
			phasesSource.setFixedUserId(userId);
			phasesSource.prepareFirstPhase();
			for (int i = 0; i < phasesCount; i++) {
				log.debug("Phase " + i + ".");
				doPhase(ind, phasesSource, targetAttribute, userId);
				phasesSource.prepareNextPhase();
			}
			testMethod(ind, phasesSource, targetAttribute, userId);
			userId = phasesSource.getUserId();
			//resultsInterpreter.writeTestResults(results);
		}
	}

	public void test(InductiveMethod ind, BasicDataSource trainDataSource,
			BasicDataSource testDataource) {

			for (int phasesCount : phasesCounts) {
				for (int trainSetSize = 5; trainSetSize < 100; trainSetSize++) {
					
				//for (int trainSetSize : trainSetSizes) {
				if(phasesCount*trainSetSize>=maxTrain)
					continue;
				for (int classCount : classCounts) {
					
					resultsInterpreter.setHeaderPrefix("date;trainSetSize;PhasesCount;ClassCount;dataset;method;");

					log.info("Testing method " + ind.toString()+", TSS "+trainSetSize+", PC"+phasesCount+", CC "+classCount);
					resultsInterpreter.setRowPrefix(
							"" + new Date(System.currentTimeMillis()).toString()+";"
							+ Integer.toString(trainSetSize) + ";"
							+ Integer.toString(phasesCount) + ";"
							+ Integer.toString(classCount) + ";"
							+ trainDataSource.getName() + ";"
							+ ind.toString() + ";");
					test(ind, trainDataSource, testDataource, trainSetSize,
							phasesCount, classCount);
					resultsInterpreter.writeTestResults(results);
				}
			//}
				}
		}
	}
	public TestInterpreter getResultsInterpreter() {
		return resultsInterpreter;
	}
}