package prefwork.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.configuration.XMLConfiguration;

public class MonotonicityTestInterpreter  {

	protected String filePrefix;
	protected String rowPrefix;
	protected String headerPrefix;
	
	double cutValue = 3.5;
	TestMonotonicityResults testResults;
	/** concordant, discordant, 1. equal, 2. equal, size*/
	int[] counts;
	int run;
	
	synchronized public void writeTestResults(TestMonotonicityResults testResults) {
		this.testResults = testResults;
		try {
			testResults.processResults();
			File f = new File(filePrefix + ".csv");
			BufferedWriter out;
			if (!f.exists()) {
				out = new BufferedWriter(new FileWriter(filePrefix + ".csv",
						true));
				out
						.write(headerPrefix
								+ "userId;run;mae;stdDevMae;rmse;weighted0Rmse;weighted1Rmse;monotonicity;tauA;tauB;CorrB;weightedTau;zeroedTau;F1Tau;roundedTau;correlation;buildTime;testTime;countTrain;countTest;countUnableToPredict;\n");
			} else
				out = new BufferedWriter(new FileWriter(filePrefix + ".csv",
						true));

			for (Integer userId : testResults.getUsers()) {
				
			
				Map<Integer,Double[]> userRes = testResults.getListStats(userId);
				if (userRes == null)
						continue;
				// TODO upravit
				out
							.write((rowPrefix + userId + ";" + run + ";" + 
									/*+ computeMonotonicity(stat) + ";"
									+ stat.buildTime + ";" + stat.testTime
									+ ";" + stat.countTrain + ";"
									+ stat.countTest + ";"
									+ stat.countUnableToPredict + ";"+ */
									"\n")
									.replace('.', ','));

				
			}
			out.flush();
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Computes monotonicity of the ratings. It compares every pair of objects
	 * and check whether the computed rating preserve ordering of this pair.
	 * 
	 * @return
	 */
	private Double computeMonotonicity(Stats stat) {
		Set<Entry<Integer, Double[]>> set = stat.getSet();
		if (set == null || set.size() <= 0)
			return 0.0D;
		int countInconsitency = 0;
		int denominator = 0;
		for (Entry<Integer, Double[]> entry : set) {
			Set<Entry<Integer, Double[]>> set2 = stat.getSet();
			for (Entry<Integer, Double[]> entry2 : set2) {
				if (entry2 == entry)
					continue;
				denominator++;
				if (entry.getValue()[1] > entry2.getValue()[1]) {
					if (entry.getValue()[0] < entry2.getValue()[0])
						countInconsitency += 3;
					if (entry.getValue()[0].equals(entry2.getValue()[0]))
						countInconsitency++;
				} else if (entry.getValue()[1] < entry2.getValue()[1]) {
					if (entry.getValue()[0] > entry2.getValue()[0])
						countInconsitency += 3;
					if (entry.getValue()[0].equals(entry2.getValue()[0]))
						countInconsitency++;
				}

				else if (entry.getValue()[1].equals(entry2.getValue()[1])) {
					if (!entry.getValue()[0].equals(entry2.getValue()[0]))
						countInconsitency += 3;
				}
			}
		}
		return (0.0 + countInconsitency) / denominator;
	}

	public void configTestInterpreter(XMLConfiguration config, String section) {
		// TODO Auto-generated method stub

	}
	

	/** 
	 * Sets the prefix that identifies current test results
	 * @param path
	 */
	public void setFilePrefix(String prefix) {
		this.filePrefix = prefix;		
	}
	

	
	/** 
	 * Gets the prefix that identifies current test results
	 * @param path
	 */
	public String getFilePrefix() {
		return filePrefix;		
	}

	/** 
	 * Sets the prefix that preceeds every line in the csv file
	 * @param path
	 */
	public void setRowPrefix(String prefix) {
		this.rowPrefix = prefix;		
	}

	/** 
	 * Sets the prefix that preceeds the header line in the csv file
	 * @param path
	 */
	public void setHeaderPrefix(String headerPrefix) {
		this.headerPrefix = headerPrefix;
	}
}

