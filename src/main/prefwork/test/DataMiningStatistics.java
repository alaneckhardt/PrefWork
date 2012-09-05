package prefwork.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.configuration.XMLConfiguration;

public class DataMiningStatistics extends TestInterpreter {

	double cutValue = 3.5;
	TestResults testResults;
	/** concordant, discordant, 1. equal, 2. equal, size*/
	int[] counts;
	int run;
	@Override
	synchronized public void writeTestResults(TestResults testResults) {
		this.testResults = testResults;
		//writeRawRatings(testResults,filePrefix+"ratings",headerPrefix,rowPrefix);
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
				List<Stats> l = testResults.getListStats(userId);
				for (int i = 0; i < l.size(); i++) {
					run = i;
					Stats stat = testResults.getStatNoAdd(userId, run);
					if (stat == null)
						continue;
					getConcordantDiscordant(stat);
					// TODO upravit
					out
							.write((rowPrefix + userId + ";" + run + ";" + computeMae(stat)
									+ ";" + computeRmse(stat) + ";" 
									+ computeWeighed0Rmse(stat) + ";"
									+ computeWeighed1Rmse(stat) + ";"
									+ computeMonotonicity(stat) + ";"
									+ computeTauACorrelation(stat) + ";"
									+  computeTauBCorrelation(stat) + ";"
									+  computeGoodmanCorrelation(stat) + ";" 
									+ computeWeightedTauCorrelation(stat) + ";" 
									+ computeZeroedTauCorrelation(stat)+ ";"
									+ computeF1TauCorrelation(stat) + ";"
									+ computeRoundedTauCorrelation(stat) + ";" 
									+ computeCorrelation(stat) + ";"
									+ stat.buildTime + ";" + stat.testTime
									+ ";" + stat.countTrain + ";"
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


	private String computeMae(Stats stat) {
		Set<Entry<Integer, Double[]>> set = stat.getSet();
		if (set == null || set.size() <= 0)
			return "0;0";
		double mae = 0;
		for (Entry<Integer, Double[]> entry : set) {
			mae += Math.abs(entry.getValue()[1] - entry.getValue()[0]);
		}
		mae /= set.size();
		double stdDevmae = 0;
		for (Entry<Integer, Double[]> entry : set) {
			stdDevmae += Math.abs(mae- Math.abs(entry.getValue()[1] - entry.getValue()[0]));
		}

		if(mae > 1000)
			return  ""+ 1000 +";"+1000;
		
		return  ""+ mae +";"+stdDevmae/set.size();
	}

	private Double computeRmse(Stats stat) {
		Set<Entry<Integer, Double[]>> set = stat.getSet();
		if (set == null || set.size() <= 0)
			return 0.0D;
		double rmse = 0;
		for (Entry<Integer, Double[]> entry : set) {
			rmse += (entry.getValue()[1] - entry.getValue()[0])
					* (entry.getValue()[1] - entry.getValue()[0]);
		}
		if(Math.sqrt(rmse / set.size()) > 1000)
			return 1000.0;
		return Math.sqrt(rmse / set.size());
	}
	
	/**
	 * Computes the weighted root mean squared error. Weights used are the
	 * ratings done by the user. This reflects the fact that we are more
	 * interested in precision of higher ratings than of lower ratings.
	 * 
	 * @return
	 */
	private String computeWeighed0Rmse(Stats stat) {

		Set<Entry<Integer, Double[]>> set = stat.getSet();
		if (set == null || set.size() <= 0)
			return "0";
		CompareRatings cd = new CompareRatings();
		Rating[] array1;
		//Exclude unableToPredict
		array1 = getArray(set, 0, null);
		java.util.Arrays.sort(array1, cd);
		
		double weighedRmse = 0;
		double denominator = 0;
		double diff = 0;
		for (Entry<Integer, Double[]> entry : set) {
			diff = (entry.getValue()[1] - entry.getValue()[0])
			* (entry.getValue()[1] - entry.getValue()[0]);
			double pos = findObject(entry.getKey(), array1);
			double weight = 0;
			if(pos < 10)
				weight = 50;
			else if(pos <20)
				weight = 30;
			else if(pos < 30)
				weight = 15;
			else if(pos < 40)
				weight = 5;
			else
				weight = 0;
			
			weighedRmse += weight* diff;
			denominator += weight;
			/*weighedRmse += entry.getValue()[0]
			            					* (entry.getValue()[1] - entry.getValue()[0])
			            					* (entry.getValue()[1] - entry.getValue()[0]);
			            			denominator += entry.getValue()[1];*/
		}
		if(Math.sqrt(weighedRmse / denominator) > 1000)
			return  ""+ 1000 ;
		
		return ""+Math.sqrt(weighedRmse / denominator);
	}

	/**
	 * Computes the weighted root mean squared error. Weights used are the
	 * ratings guessed by the method. This reflects the fact that we are more
	 * interested in precision of higher ratings than of lower ratings.
	 * 
	 * @return
	 */
	private String computeWeighed1Rmse(Stats stat) {
		Set<Entry<Integer, Double[]>> set = stat.getSet();
		if (set == null || set.size() <= 0)
			return "0";

		CompareRatings cd = new CompareRatings();
		Rating[] array1;
		//Exclude unableToPredict
		array1 = getArray(set, 1, null);
		java.util.Arrays.sort(array1, cd);
		
		
		double weighedRmse = 0;
		double denominator = 0;
		double diff = 0;
		for (Entry<Integer, Double[]> entry : set) {
			diff = (entry.getValue()[1] - entry.getValue()[0])
			* (entry.getValue()[1] - entry.getValue()[0]);

			double pos = findObject(entry.getKey(), array1);
			double weight = 0;
			if(pos < 10)
				weight = 50;
			else if(pos <20)
				weight = 30;
			else if(pos < 30)
				weight = 15;
			else if(pos < 40)
				weight = 5;
			else
				weight = 0;
			
			weighedRmse += weight* diff;
			denominator += weight;
		}
		if(Math.sqrt(weighedRmse / denominator) > 1000)
			return  ""+ 1000 ;
		
		return ""+Math.sqrt(weighedRmse / denominator);
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

	/**
	 * @param x1
	 * @param x2
	 * @param y1
	 * @param y2
	 * @return
	 * 0 - concordant,
	 * 1 - discordant,
	 * 2 - x1 and x2 are the same,
	 * 3 - y1 and y2 are the same
	 */
	static int isConcordant(Rating x1, Rating x2, Rating y1, Rating y2){
		// Both equal - concordant
		if(isBigger(x1, x2, 0)==0 && isBigger(y1, y2, 0)==0)
			return 0;
		// x are the same
		if(isBigger(x1, x2, 0)==0)
			return 2;
		// y are the same
		if(isBigger(y1, y2, 0) == 0)
			return 3;
		// Concordant
		if(isBigger(x1, x2, 0)==isBigger(y1, y2, 0))
			return 0;
		//Else discordant
		return 1;	}
	
	static int isBigger(Rating x1, Rating x2, double epsilon){
		if(Math.abs(x1.rating-x2.rating)<=epsilon)
			return 0;
		if(x1.rating<x2.rating)
			return -1;
		return 1;
	}
	static int isConcordant(Rating x1, Rating x2, Rating y1, Rating y2, double epsilon){
		// Both equal - concordant
		if(isBigger(x1, x2, epsilon)==0 && isBigger(y1, y2, epsilon)==0)
			return 0;
		// x are the same
		if(isBigger(x1, x2, epsilon)==0)
			return 2;
		// y are the same
		if(isBigger(y1, y2, epsilon) == 0)
			return 3;
		// Concordant
		if(isBigger(x1, x2, epsilon)==isBigger(y1, y2, epsilon))
			return 0;
		//Else discordant
		return 1;
	}
	
	static int findObject(int objectId, Rating[] array){
		for (int i = 0; i < array.length; i++) {
			if(array[i].objectId == objectId)
				return i;
		}
		return -1;
	}
	
	

	/**
	 * Computes Tau correlation coefficient, but the values are rounded to whole numbers.
	 * 
	 * @return
	 */
	private Double computeRoundedTauCorrelation(Stats stat) {
		Set<Entry<Integer, Double[]>> set = stat.getSet();
		if (set == null || set.size() <= 0)
			return 0.0D;

		CompareRatings cd = new CompareRatings();
		Rating[] array1;
		//Include unableToPredict
		array1 = getArray(set, 0, stat.unableToPredict);
		Rating[] array2;
		//Exclude unableToPredict
		array2 = getArray(set, 1, null);
		/*for (int i = 0; i < array1.length; i++) {
			array1[i].rating = CommonUtils.roundToDecimals(array1[i].rating, 0);
		}
		for (int i = 0; i < array2.length; i++) {
			try{
			array2[i].rating = CommonUtils.roundToDecimals(array2[i].rating, 0);
			}catch (Exception e) {
			}
		}*/
		java.util.Arrays.sort(array1, cd);
		java.util.Arrays.sort(array2, cd);
		
		int concordant = 0;
		int discordant = 0;
		for (int i = 0; i < array1.length; i++) {
			int j = findObject(array1[i].objectId, array2);
			if(j == -1)
				continue;
			for (int k = i + 1; k < array1.length; k++) {
				int l = findObject(array1[k].objectId, array2);
				if (l == -1)
					continue;
				int isCon = isConcordant(array1[i], array1[k], array2[j], array2[l], 0.5);
				if (isCon == 0)
					concordant++;
				else if (isCon == 1)
					discordant++;
			}
		}
		return (concordant-discordant)/(0.5*array1.length*(array1.length-1));

		/*
		for (int i = 0; i < array1.length; i++) {
			int j = findObject(array1[i].objectId, array2);
			for (int k = i+1; k < array1.length; k++) {
				int l = findObject(array1[k].objectId, array2);
				if(isConcordant(array1[i], array1[k], array2[j], array2[l]))
					concordant++;
				else
					discordant++;
			}
		}
		return (concordant-discordant)/(0.5*array1.length*(array1.length-1));
		*/
	}
	
	/*double[] getDoubles(Rating[] ratings){
		double[] arr = new double[ratings.length];
		for (int i = 0; i < ratings.length; i++) {
			arr[i]=ratings[i].rating;
		}
		return arr;
	}
	int[] getIds(Rating[] ratings){
		int[] arr = new int[ratings.length];
		for (int i = 0; i < ratings.length; i++) {
			arr[i]=ratings[i].objectId;
		}
		return arr;
	}*/
	
	void getConcordantDiscordant(Stats stat){

		Set<Entry<Integer, Double[]>> set = stat.getSet();
		//concordant, discordant, 1. equal, 2. equal, size
		counts = new int[5];
		if (set == null || set.size() <= 0)
			return;

		CompareRatings cd = new CompareRatings();
		Rating[] array1;

		//Include unableToPredict
		array1 = getArray(set, 0, stat.unableToPredict);
		Rating[] array2;

		//Exclude unableToPredict
		array2 = getArray(set, 1, null);
		counts[4]= array1.length;

		java.util.Arrays.sort(array1, cd);
		java.util.Arrays.sort(array2, cd);
		
		for (int i = 0; i < array1.length; i++) {
			int j = findObject(array1[i].objectId, array2);
			if(j == -1){
				counts[1]++;
				continue;
			}
			for (int k = i+1; k < array1.length; k++) {
				int l = findObject(array1[k].objectId, array2);
				if(l == -1){
					counts[1]++;
					continue;
				}
				double x1 = array1[i].rating, x2= array1[k].rating,
				  y1 = array2[j].rating, y2 = array2[l].rating;
				
				if(x1==x2)
					counts[2]++;
				else if(y1==y2)
					counts[3]++;
				else if((x1<x2 && y1<y2) ||
						(x1>x2 && y1>y2) )
					counts[0]++;
				else
					counts[1]++;
			}
		}
	}
	
	
	/**
	 * Computes Tau A correlation coefficient.
	 * 
	 * @return
	 */
	private Double computeTauACorrelation(Stats stat) {
		return (0.0+counts[0]-counts[1])/(0.5*counts[4]*(counts[4]-1));
	}
	

	
	/**
	 * Computes Tau B correlation coefficient.
	 * 
	 * @return
	 */
	private Double computeTauBCorrelation(Stats stat) {
		return (0.0+counts[0]-counts[1])/Math.sqrt((0.0+(counts[0] + counts[1] + counts[2])*(counts[0] + counts[1] + counts[3])));
	}

	/**
	 * Computes Goodman correlation coefficient.
	 * 
	 * @return
	 */
	private  Double computeGoodmanCorrelation(Stats stat) {
		return (0.0+counts[0]-counts[1])/(0.0+counts[0] + counts[1]);
	}

	/**
	 * Sets all ratings that are lower than 3 to zero
	 * 
	 * @param array1
	 */
	protected static void setZeros(Rating[] array1,Rating[] array2) {
		double max=-1, min=1000;
		for (int i = 0; i < array1.length; i++) {
			if (array1[i].rating < min){
				min=array1[i].rating;
			}			
			if (array1[i].rating > max){
				max=array1[i].rating;
			}			
		}
		for (int i = 0; i < array1.length; i++) {
			if (array1[i].rating <= min+(max-min)/2){
				array1[i].rating=0.0;
				array2[i].rating=0.0;
			}			
		}
	}

	/**
	 * Computes Tau correlation coefficient, where all ratings less than
	 * classes[0]/2 were set to 0;
	 * 
	 * @return
	 */
	private Double computeZeroedTauCorrelation(Stats stat) {

		Set<Entry<Integer, Double[]>> set = stat.getSet();
		if (set == null || set.size() <= 0)
			return 0.0D;

		CompareRatings cd = new CompareRatings();
		Rating[] array1;
		array1 = getArray(set, 0, stat.unableToPredict);
		
		Rating[] array2;
		//Exclude unableToPredict
		array2 = getArray(set, 1, null);

		java.util.Arrays.sort(array1, cd);
		array1 = java.util.Arrays.copyOf(array1, Math.min(20, array1.length));
		//array1 = setWeights(array1);
		java.util.Arrays.sort(array2, cd);
		//array2 = setWeights(array2);
		array2 = java.util.Arrays.copyOf(array2, Math.min(20, array2.length));
		
		int concordant = 0;
		int discordant = 0;
		for (int i = 0; i < array1.length; i++) {
			if(array1[i]==null)
				break;
			int j = findObject(array1[i].objectId, array2);
			if(j == -1){
				discordant++;//=array1.length-i;
				continue;
			}
			/*else
				concordant++;//=array1.length-i;*/
			for (int k = i+1; k < array1.length; k++) {
				int l = findObject(array1[k].objectId, array2);
				if(l == -1){
					discordant++;
					continue;
				}
				int isCon = isConcordant(array1[i], array1[k], array2[j], array2[l]);
				if (isCon == 0)
					concordant++;
				else if (isCon == 1)
					discordant++;
			}
			/*
			
			for (int k = i+1; k < array1.length; k++) {
				int l = findObject(array1[k].objectId, array2);
				if(l == -1){
					discordant++;
					continue;
				}
				
				if(isConcordant(array1[i], array1[k], array2[j], array2[l]))
					concordant++;
				else
					discordant++;
			}*/
		}		
		return (concordant-discordant+0.0)/(array1.length+0.0);
		
		/*
		Set<Entry<Integer, Double[]>> set = stat.getSet();
		if (set == null || set.size() <= 0)
			return 0.0D;
		CompareRatings cd = new CompareRatings();

		Rating[] array1;
		//Include unableToPredict
		array1 = getArray(set, 0, stat.unableToPredict);
		
		Rating[] array2;
		//Exclude unableToPredict
		array2 = getArray(set, 1, null);
		
		setZeros(array1, array2);
		setZeros(array1, array2);
		java.util.Arrays.sort(array1, cd);
		java.util.Arrays.sort(array2, cd);

		//array1 = setZeros(array1, array2, true);
		//array2 = setZeros(array1, array2, false);

		int concordant = 0;
		int discordant = 0;
		for (int i = 0; i < array1.length; i++) {
			int j = findObject(array1[i].objectId, array2);
			if(j == -1){
				discordant++;		
				continue;
			}
			for (int k = i+1; k < array1.length; k++) {
				int l = findObject(array1[k].objectId, array2);
				if(l == -1){
					discordant++;		
					continue;
				}

				int isCon = isConcordant(array1[i], array1[k], array2[j], array2[l]);
				if (isCon == 0)
					concordant++;
				else if (isCon == 1)
					discordant++;				
			}
		}
		return (concordant-discordant)/(0.5*array1.length*(array1.length-1));*/
	}

	/**
	 * Sets all ratings that are lower than 3 to zero
	 * 
	 * @param array1
	 */
	protected static Rating[] setWeights(Rating[] array1) {
		/*
		 * int k = 0; for (; k < array1.length; k++) { if
		 * (array1[k].getValue()[index] < cutValue) break; } Entry<Integer,
		 * Double[]>[] newArr = new Entry[k]; for (int i = 0; i< newArr.length;
		 * i++) { newArr[i] = array1[i]; }
		 */

		Rating[] newArr = new Rating[Math.min(30,array1.length)];
		for (int i = 0; i < 10 && i < array1.length; i++) {
			newArr[i] = array1[i];
			newArr[i].rating = 3.0;
		}
		for (int i = 10; i < 20 && i < array1.length; i++) {
			newArr[i] = array1[i];
			newArr[i].rating = 2.0;
		}

		for (int i = 20; i < 30 && i < array1.length; i++) {
			newArr[i] = array1[i];
			newArr[i].rating = 1.0;
		}

		/*
		 * for (int i = 30; i < array1.length; i++) {
		 * array1[i].getValue()[index] = 0.0; }
		 */
		return newArr;
	}

     static Rating[] getArray(Set<Entry<Integer, Double[]>> set, int index, HashMap<Integer, Double> unableToPredict) {
		int size = set.size();
    	if(unableToPredict != null)
    		 size += unableToPredict.size();
    	Rating[] newArr = new Rating[size];
		int i = 0;
		for (Entry<Integer, Double[]> entry : set) {
			newArr[i] = new Rating();
			newArr[i].objectId = entry.getKey();
			newArr[i].rating = entry.getValue()[index];
			i++;
		}
		if (unableToPredict != null) {
			for (Integer objectId : unableToPredict.keySet()) {
				Double r = unableToPredict.get(objectId);
				newArr[i] = new Rating();
				newArr[i].objectId = objectId;
				newArr[i].rating = r;
				i++;
			}
		}
		
		return newArr;
	}

	/**
	 * Computes Tau correlation coefficient.
	 * 
	 * @return
	 */
	private Double computeWeightedTauCorrelation(Stats stat) {

		Set<Entry<Integer, Double[]>> set = stat.getSet();
		if (set == null || set.size() <= 0)
			return 0.0D;
		CompareRatings cd = new CompareRatings();
		Rating[] array1;
		array1 = getArray(set, 0, stat.unableToPredict);
		
		Rating[] array2;
		//Exclude unableToPredict
		array2 = getArray(set, 1, null);

		java.util.Arrays.sort(array1, cd);
		java.util.Arrays.sort(array2, cd);
		int div = 0;
		int concordant = 0;
		int discordant = 0;
		for (int i = 0; i < array1.length; i++) {
			int j = findObject(array1[i].objectId, array2);
			if(j == -1){
				discordant+=array1[i].rating;
				continue;
			}
			for (int k = i+1; k < array1.length; k++) {
				int l = findObject(array1[k].objectId, array2);
				if(l == -1){
					discordant+=Math.max(array1[i].rating,array1[k].rating);
					continue;
				}
				div += Math.max(array1[i].rating,array1[k].rating);
				//array1[i].rating * array2[j].rating * (array1.length - i + 1);

				int isCon = isConcordant(array1[i], array1[k], array2[j], array2[l]);
				if (isCon == 0)
					concordant+=Math.max(array1[i].rating,array1[k].rating);
				else if (isCon == 1)
					discordant+=Math.max(array1[i].rating,array1[k].rating);
			}
		}
		return (concordant-discordant+0.0)/div;
	}

	/**
	 * Computes weighted Tau correlation coefficient.
	 * 
	 * @return
	 */
	private Double computeF1TauCorrelation(Stats stat) {
		Set<Entry<Integer, Double[]>> set = stat.getSet();
		if (set == null || set.size() <= 0)
			return 0.0D;

		CompareRatings cd = new CompareRatings();
		Rating[] array1;
		array1 = getArray(set, 0, stat.unableToPredict);
		
		Rating[] array2;
		//Exclude unableToPredict
		array2 = getArray(set, 1, null);

		java.util.Arrays.sort(array1, cd);
		array1 = java.util.Arrays.copyOf(array1, Math.min(20, array1.length));
		//array1 = setWeights(array1);
		java.util.Arrays.sort(array2, cd);
		//array2 = setWeights(array2);
		array2 = java.util.Arrays.copyOf(array2, Math.min(20, array2.length));
		
		int concordant = 0;
		int discordant = 0;
		for (int i = 0; i < array1.length; i++) {
			if(array1[i]==null)
				break;
			int j = findObject(array1[i].objectId, array2);
			if(j == -1){
				discordant++;			
			}
			else {
				concordant++;
			}
			/*
			
			for (int k = i+1; k < array1.length; k++) {
				int l = findObject(array1[k].objectId, array2);
				if(l == -1){
					discordant++;
					continue;
				}
				
				if(isConcordant(array1[i], array1[k], array2[j], array2[l]))
					concordant++;
				else
					discordant++;
			}*/
		}		
		return (0.0+concordant)/(0.0+array1.length);
	}

	private Double computeCorrelation(Stats stat) {
		double x_sum, y_sum, xx_sum, yy_sum, xy_sum;

		Set<Entry<Integer, Double[]>> set = stat.getSet();
		if (set == null || set.size() <= 0)
			return 0.0D;
		double r;
		x_sum = y_sum = xx_sum = yy_sum = xy_sum = 0.0;
		for (Entry<Integer, Double[]> entry : set) {
			x_sum += entry.getValue()[0];
			y_sum += entry.getValue()[1];
			xx_sum += entry.getValue()[0] * entry.getValue()[0];
			yy_sum += entry.getValue()[1] * entry.getValue()[1];
			xy_sum += entry.getValue()[0] * entry.getValue()[1];
		}

		if ((set.size() * xx_sum - x_sum * x_sum)
				* (set.size() * yy_sum - y_sum * y_sum) == 0)
			return 0.0D;
		r = ((set.size() * xy_sum - x_sum * y_sum) / (Math.sqrt((set.size()
				* xx_sum - x_sum * x_sum)
				* (set.size() * yy_sum - y_sum * y_sum))));
		return r;
	}
	

	@SuppressWarnings("unused")
	private Double computeCorrelationB(Stats stat) {
		double x_mean = 0, y_mean = 0, x_dev = 0, y_dev = 0, corr = 0;

		Set<Entry<Integer, Double[]>> set = stat.getSet();
		if (set == null || set.size() <= 0)
			return 0.0D;

		for (Entry<Integer, Double[]> entry : set) {
			x_mean += entry.getValue()[0];
			y_mean += entry.getValue()[1];
		}
		x_mean /= set.size();
		y_mean /= set.size();
		for (Entry<Integer, Double[]> entry : set) {
			x_dev += (entry.getValue()[0]-x_mean)*(entry.getValue()[0]-x_mean);
			y_dev += (entry.getValue()[1]-y_mean)*(entry.getValue()[1]-y_mean);
			corr += (entry.getValue()[0]-x_mean)*(entry.getValue()[1]-y_mean);
		}
		x_dev /= set.size();
		y_dev /= set.size();
		x_dev = Math.sqrt(x_dev);
		y_dev = Math.sqrt(y_dev);
		if(x_dev == 0 || y_dev == 0)
			return 0.0;
		return corr/((set.size()-1)*x_dev*y_dev);
	}

	@Override
	public void configTestInterpreter(XMLConfiguration config, String section) {
		// TODO Auto-generated method stub

	}
}

