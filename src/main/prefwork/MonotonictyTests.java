package prefwork;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import prefwork.ConfigurationParser.Semaphore;
import prefwork.ConfigurationParser.TestRun;
import prefwork.datasource.BasicDataSource;
import prefwork.method.InductiveMethod;
import prefwork.method.Statistical;
import prefwork.method.StatisticalBestLocal;
import prefwork.normalizer.Normalizer;
import prefwork.test.Bootstrap;
import prefwork.test.DataMiningStatistics;
import prefwork.test.MonotonicityTest;
import prefwork.test.Test;
import prefwork.test.TestInterpreter;
import prefwork.test.TestResults;

public class MonotonictyTests {

	private static Logger log = Logger.getLogger(MonotonictyTests.class);

	public static Semaphore semRuns;
	public static Semaphore semWrite;

	XMLConfiguration confMonoTests = new XMLConfiguration();
	XMLConfiguration confDbs = new XMLConfiguration();
	XMLConfiguration confDatasources = new XMLConfiguration();

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

	private Normalizer[] getNorms(List<String> norms) {
		Normalizer[] norms2 = new Normalizer[norms.size()];
		for (int i = 0; i < norms2.length; i++) {
			norms2[i] = (Normalizer) CommonUtils.getInstance(norms.get(i));
		}
		return norms2;
	}

	@SuppressWarnings("unchecked")
	public void test() {
		try {
			confMonoTests.setFileName("confMonoTests.xml");
			confDbs.setFileName("confDbs.xml");
			confDatasources.setFileName("confDatasources.xml");

			confMonoTests.load();
			confDbs.load();
			confDatasources.load();

			List<String> runs = confMonoTests.getList("run");
			log.info("Start of testing");
			// runs = new TestRun[confRuns.getInt("threads")];
			int numRuns = confMonoTests.getInt("threads");
			String resultsFile = "";
			semRuns = new Semaphore(numRuns);
			// Iterate through runs
			for (String run : runs) {
				int dbId = 0;
				run = "runs." + run;
				// Iterate through dbs
				while (confMonoTests.getProperty(run + ".dbs.db(" + dbId
						+ ").name") != null) {
					String dbName = confMonoTests.getString(run + ".dbs.db("
							+ dbId + ").name");
					List<String> datasources = confMonoTests.getList(run
							+ ".dbs.db(" + dbId + ").datasources");
					int datasourceId = 0;
					// Iterate through datasources
					for (String datasourceName : datasources) {
						int methodId = 0;
						// Load possible normalizers
						List<String> numericalNorms = confMonoTests.getList(run
								+ ".normalizers.numericalNormalizer");
						List<String> nominalNormalizers = confMonoTests
								.getList(run + ".normalizers.nominalNormalizer");
						List<String> listNormalizer = confMonoTests.getList(run
								+ ".normalizers.listNormalizer");
						// Iterate through tests
						if (confMonoTests.getProperty(run + ".test.path") != null) {
							synchronized (semRuns) {
								// Waiting for a free slot;
								semRuns.acquire();

								System.gc();
								BasicDataSource trainDataSource = getDataSource(
										datasourceName, dbName);
								trainDataSource.configDriver(confMonoTests, run
										+ ".dbs.db(" + dbId + ")");
								trainDataSource.configDataSource(confMonoTests,
										run + ".dbs.db(" + dbId
												+ ").datasources("
												+ datasourceId + ")");
								BasicDataSource testDataSource = getDataSource(
										datasourceName, dbName);
								testDataSource.configDriver(confMonoTests, run
										+ ".dbs.db(" + dbId + ")");
								testDataSource.configDataSource(confMonoTests,
										run + ".dbs.db(" + dbId
												+ ").datasources("
												+ datasourceId + ")");

								MonotonicityTest mt = new MonotonicityTest();
								mt.configTest(confMonoTests, run + ".test");
								log.info("Testing datasource "
										+ trainDataSource.getName());
								// Making new thread for a new test.
								new TestRun(this, mt, trainDataSource,
										testDataSource,
										getNorms(nominalNormalizers),
										getNorms(numericalNorms),
										getNorms(listNormalizer)).start();
								// Wait 1 second for avoid of the colision
								// in writing of results.
								Thread.sleep(1000);
							}
						}
						datasourceId++;
					}
					dbId++;
				}
			}

			// Waiting for all other threads to finish.
			for (int i = 0; i < numRuns; i++)
				semRuns.acquire();

			log.info("End of testing");

			Process p = Runtime.getRuntime().exec("cmd ", null, null);
			p.getOutputStream().write(
					("loadResults.bat " + resultsFile + "\n").getBytes());
			p.getOutputStream().flush();
			p.getOutputStream().write("exit\n".getBytes());
			p.getOutputStream().flush();
			BufferedReader stdOut = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			BufferedReader stdErr = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			while (true) {
				try {
					p.exitValue();
					break;
				} catch (Exception e) {
					while (stdOut.ready() || stdErr.ready()) {
						if (stdOut.ready())
							stdOut.readLine();
						else
							stdErr.readLine();
					}
				}
			}
			p.waitFor();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		PropertyConfigurator.configure("log4j.properties");
		MonotonictyTests mt = new MonotonictyTests();
		ConfigurationParser c = new ConfigurationParser();
		mt.test();
	}

	class TestRun extends Thread {
		BasicDataSource trainDataSource;
		BasicDataSource testDataSource;
		Statistical method;
		MonotonictyTests conf;
		protected TestResults results;
		protected Double[] randoms;
		protected int[] trainSets;
		protected int run = 0;
		protected int numberOfRuns = 0;
		protected TestInterpreter resultsInterpreter;
		MonotonicityTest mt;
		Normalizer[] nom;
		Normalizer[] num; Normalizer[] list;

		public TestRun(MonotonictyTests conf, MonotonicityTest mt,
				BasicDataSource trainDataSource,
				BasicDataSource testDataSource, Normalizer[] nom,
				Normalizer[] num, Normalizer[] list) {
			this.trainDataSource = trainDataSource;
			this.testDataSource = testDataSource;
			this.conf = conf;
			this.nom = nom;
			this.num = num;
			this.list = list;
			this.mt = mt;
		}

		public void run() {
			//method = new StatisticalBestLocal();
			//mt.test(method, trainDataSource, testDataSource);
			method = new Statistical();
			for (int i = 0; i < nom.length; i++) {
				for (int j = 0; j < num.length; j++) {
					for (int k = 0; k < list.length; k++) {
						method.setNominalNorm(nom[i]);
						if(num[j] instanceof prefwork.normalizer.THOrdinalNormalizer){
							for (int k2 = 1; k2 < 5; k2++) {
								prefwork.normalizer.THOrdinalNormalizer th = (prefwork.normalizer.THOrdinalNormalizer)num[j];
								th.setMode(new String[]{"",""+k2});
								method.setNumericalNorm(num[j]);
								method.setListNorm(list[k]);
								mt.test(method, trainDataSource, testDataSource);								
							}
						}
						else{
							method.setNumericalNorm(num[j]);
							method.setListNorm(list[k]);
							mt.test(method, trainDataSource, testDataSource);						
						}
					}
				}
				
			}
			semRuns.release();
		}

	}

	public class Semaphore {
		private int count;

		public Semaphore(int n) {
			this.count = n;
		}

		public synchronized void acquire() {
			while (count == 0) {
				try {
					wait();
				} catch (InterruptedException e) {
					// keep trying
				}
			}
			count--;
		}

		public synchronized void release() {
			count++;
			notify(); // alert a thread that's blocking on this semaphore
		}
	}

}
