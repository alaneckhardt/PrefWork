package prefwork.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.configuration.XMLConfiguration;

public class ClassificationError extends TestInterpreter {

	TestResults testResults;
	int run;
	@Override
	synchronized public void writeTestResults(TestResults testResults) {
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
								+ "userId;run;cE;wCE;FM;WFM;buildTime;testTime;countTrain;countTest;countUnableToPredict;\n");
			} else
				out = new BufferedWriter(new FileWriter(filePrefix + ".csv",
						true));

			for (Integer userId : testResults.getUsers()) {
				List<Stats> l = testResults.getListStats(userId);
				for (int i = 0; i < l.size(); i++) {
					run = i;
					Stats stat = testResults.getStatNoAdd(userId, run);
					if (stat == null)
						continue;
					// TODO upravit
					out
							.write((rowPrefix + userId + ";" + run + ";" + 
									computeClassificationError(stat) + ";" +
									computeWeightedClassificationError(stat) + ";" +
									computeFirstMatch(stat) + ";" +
									computeFirstWMatch(stat) + ";" +
									stat.buildTime + ";" + stat.testTime
									+ ";" + stat.countTrain 
									+ stat.countTest + ";"
									+ stat.countUnableToPredict + ";"+ "\n")
									.replace('.', ','));

				}
			}
			out.flush();
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private Rating[] getArray(Stats stat, int index){
		Set<Entry<Integer, Double[]>> set = stat.getSet();
		if (set == null || set.size() <= 0)
			return null;
		Rating[] array1;
		if(index == 0)
		array1 = DataMiningStatistics.getArray(set, index, stat.unableToPredict);
		else
			array1 = DataMiningStatistics.getArray(set, index, null);
		return array1;
	}
	private Double computeClassificationError(Stats stat) {
		Rating[] array1 = getArray(stat, 0);
		if(array1 == null)
			return null;
		Rating[] array2 = getArray(stat, 1);
		int classificationError = 0;

		for (int i = 0; i < array1.length; i++) {
			if(array1[i]==null)
				break;
			if(DataMiningStatistics.isBigger(array1[i], array2[i], 0.5) != 0)
				classificationError++;
		}
		return (double)classificationError/array1.length;
	}

	private Double computeWeightedClassificationError(Stats stat) {
		Rating[] array1 = getArray(stat, 0);
		if(array1 == null)
			return null;
		Rating[] array2 = getArray(stat, 1);
		int classificationError = 0;

		for (int i = 0; i < array1.length; i++) {
			if(array1[i]==null)
				break;
			if(DataMiningStatistics.isBigger(array1[i], array2[i], 0.5) != 0)
				classificationError+=array1[i].rating;
		}
		return (double)classificationError/array1.length;
	}
	

	private Double computeFirstMatch(Stats stat) {
		Rating[] array1 = getArray(stat, 0);
		if(array1 == null)
			return null;
		Rating[] array2 = getArray(stat, 1);
		int classificationError = 0;

		CompareRatings cd = new CompareRatings();

		java.util.Arrays.sort(array1, cd);
		array1 = java.util.Arrays.copyOf(array1, 30);
		java.util.Arrays.sort(array2, cd);
		array2 = java.util.Arrays.copyOf(array2, 30);
		
		
		for (int i = 0; i < array1.length; i++) {
			if(array1[i]==null)
				break;

			int j = DataMiningStatistics.findObject(array1[i].objectId, array2);
			if(j == -1)
				classificationError+=10;
			else if(i!=j)
				classificationError++;
		}
		return (double)classificationError/array1.length;
	}

	private Double computeFirstWMatch(Stats stat) {
		Rating[] array1 = getArray(stat, 0);
		if(array1 == null)
			return null;
		Rating[] array2 = getArray(stat, 1);
		int classificationError = 0;

		CompareRatings cd = new CompareRatings();

		java.util.Arrays.sort(array1, cd);
		array1 = java.util.Arrays.copyOf(array1, 30);
		java.util.Arrays.sort(array2, cd);
		array2 = java.util.Arrays.copyOf(array2, 30);
		
		
		for (int i = 0; i < array1.length; i++) {
			if(array1[i]==null)
				break;

			int j = DataMiningStatistics.findObject(array1[i].objectId, array2);
			if(j == -1)
				classificationError+=10;
			else if(i!=j)
				classificationError+=Math.abs(i-j);
		}
		return (double)classificationError/array1.length;
	}
	
	
	@Override
	public void configTestInterpreter(XMLConfiguration config, String section) {
		// TODO Auto-generated method stub

	}
}

