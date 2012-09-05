package prefwork.test;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import prefwork.CommonUtils;
import au.com.bytecode.opencsv.CSVReader;

public class SingificanceTest {
	private static Logger log = Logger.getLogger(SingificanceTest.class);

	static String path = "c:\\data\\progs\\eclipseProjects2\\PrefWork\\";
	String file = "";
	List<String> excludeMethods = CommonUtils.getList();
	List<String> includeMethods = CommonUtils.getList();
	//String[] names = {"cE","wCE","FM","WFM","buildTime","testTime","countTrain","countTest","countUnableToPredict"};
	            
	String[] names = {"mae","stdDevMae","rmse","weighted0Rmse","weighted1Rmse",
			//5
			"monotonicity","tauA","tauB","CorrB","weightedTau",
			//10
			"zeroedTau","F1Tau","roundedTau","correlation","buildTime",
			//15
			"testTime","countTrain","countTest","countUnableToPredict"};
	//int indexes[] = {2,5,6,8};
	//boolean[] desc = {true,false,false, false};
	int indexes[] = {2,5,6,13,14,15,18};
	boolean[] desc = {true,true,false,false,true,true,true};
	
	boolean considerRatio = true;
	boolean considerDataset = true;
	boolean considerUser = true;
	
	
	
	int methodIndex = 3;
	int ratioIndex = 1;
	int datasetIndex = 2;
	int userIndex = 4;
	int runIndex = 5;

	HashMap<Object, List<DataEntry>> mapOfDataEntries;

	HashMap<Object, MethodComparison> mapOfDifferences;
	
	//List<CCBetter> better;
	//HashMap<Long, CCBetter> map;
	//ratio;method; userId
	//int[] matters = {1,3,4};
	//method; userId
	
	
	public SingificanceTest(){
		/*excludeMethods.add("Ideal");
		excludeMethods.add("Mean");
		excludeMethods.add("Instances100MaxTextNormLinear0falseAvg2ListAvg2Avg");
		excludeMethods.add("Instances100aDistanceTextNormLinear0falseAvg3ListAvg2Avg");
		
		excludeMethods.add("Statistical3,MethodRaterUtaBridgeb2,Avg,TLCoef0false,BAvg2,LBAvg1");
		*/
		excludeMethods.add("weka,classifiers,functions,MultilayerPerceptron0");
		excludeMethods.add("Statistical3,WekaRaterweka,classifiers,functions,MultilayerPerceptron,Avg,TLCoef0false,BAvg2,LBAvg1");
		excludeMethods.add("Statistical3,WekaRaterweka,classifiers,functions,SMOreg,Avg,TLCoef0false,BAvg2,LBAvg1");
	//	excludeMethods.add("UtaBridgeb2");
	//	excludeMethods.add("Statistical3,MethodRaterUtaBridgeb2,Avg,TLCoef0false,BAvg2,LBAvg1");
		
	//	excludeMethods.add("ProgolBridge20false");
	//	excludeMethods.add("Statistical3,MethodRaterProgolBridge20false,Avg,TLCoef0false,BAvg2,LBAvg1");
	/*	excludeMethods.add("weka,classifiers,rules,PART1");
		excludeMethods.add("Statistical3,WekaRaterweka,classifiers,rules,PART,Avg,TLCoef0false,BAvg2,LBAvg1");
		excludeMethods.add("weka,classifiers,rules,JRip1");
		excludeMethods.add("Statistical3,WekaRaterweka,classifiers,rules,JRip,Avg,TLCoef0false,BAvg2,LBAvg1");
		*/
		//includeMethods.add("Statistical3,WekaRaterweka,classifiers,functions,MultilayerPerceptron,Avg,TLCoef0false,BAvg2,LBAvg1");
		//includeMethods.add("weka,classifiers,functions,MultilayerPerceptron0");

		//includeMethods.add("Statistical3,WekaRaterweka,classifiers,functions,SMOreg,Avg,TLCoef0false,BAvg2,LBAvg1");
		//includeMethods.add("weka,classifiers,functions,SMOreg0");
		//includeMethods.add("weka,classifiers,functions,SMOreg0");
		//includeMethods.add("Instances100aTLCoef0falseBAvg3LBAvg3Avg");
		//includeMethods.add("Statistical3,WAvgVWV,Avg,TLCoef0false,BAvg2,LBAvg1");
		/*includeMethods.add("weka,classifiers,rules,PART1");
		includeMethods.add("weka,classifiers,rules,JRip1");
		includeMethods.add("weka,classifiers,functions,SMOreg0");
		includeMethods.add("Instances100aTLCoefNulled0falseBAvg3LBAvg3Avg");
		includeMethods.add("Instances100aTLCoef0falseBAvg3LBAvg3Avg");
		includeMethods.add("Statistical3,WAvgVWV,Avg,TLCoef0false,BAvg2,LBAvg1");							 
		includeMethods.add("Statistical3,WAvgVV,Avg,TLCoefNulled0false,BAvg2,LBAvg1");
		*/}
	
	private boolean hasSameMethod(DataEntry r1, DataEntry r2){
		return r1.methodhash == (r2.methodhash);
	}

	private boolean hasSameRun(DataEntry r1, DataEntry r2){
		return r1.run == r2.run;
	}
	
	private Object getKey(DataEntry r1, DataEntry r2){
		return r1.methodhash+r2.methodhash+r1.datasetUser.hashCode();
	}

	MethodComparison findComparison(DataEntry r1, DataEntry r2){
		Object key = getKey(r1,r2);
		if(mapOfDifferences.containsKey(key))
			return mapOfDifferences.get(key);
		MethodComparison m = new MethodComparison();
		m.method1 = r1.method;
		m.method2 = r2.method;
		m.better1 = new int[r1.measures.length];
		m.better2 = new int[r1.measures.length];
		m.datasetUser = r1.datasetUser;
		m.measureDifferences = new double[r1.measures.length];
		mapOfDifferences.put(key, m);
		return m;
	}
	
	
	@SuppressWarnings("unchecked")
	void compareMethods(){
		mapOfDifferences = new HashMap();
		Long time = 0L;
		for(List<DataEntry> l1 : mapOfDataEntries.values()){
			Long start = System.currentTimeMillis();
			for (int i = 0; i < l1.size(); i++) {
				DataEntry r1 = l1.get(i);
				for (int j = i+1; j < l1.size(); j++) {
					DataEntry r2 = l1.get(j);
					//We want different methods
					if(hasSameMethod(r1,r2)){
						time+= System.currentTimeMillis() - start;
						continue;					
					}
					if(!hasSameRun(r1,r2)){
						time+= System.currentTimeMillis() - start;
						continue;					
					}

					if(!considerRatio && r1.ratio != r2.ratio){
						time+= System.currentTimeMillis() - start;
						continue;					
					}
					if(!considerUser && r1.userId != r2.userId){
						time+= System.currentTimeMillis() - start;
						continue;					
					}

					if(!considerDataset && !r1.dataset.equals(r2.dataset)){
						time+= System.currentTimeMillis() - start;
						continue;					
					}		
					MethodComparison b = findComparison(r1, r2);
					b.count++;
					//6-12 vetsi lepsi
					
					//buildTime==14","testTime","countTrain","countTest","countUnableToPredict"
					b.data1.add(r1.measures);
					b.data2.add(r2.measures);
				}
			}
		}		
	}
	private double[][] getAverages(MethodComparison b){
		double[][] average = new double[4][b.measureDifferences.length];
		int counts[] = new int[b.measureDifferences.length];
		for (int m = 0; m < indexes.length; m++) {
		for (int i = 0; i < b.data1.size(); i++) {
				if(Double.isNaN(b.data1.get(i)[m]) || Double.isNaN(b.data2.get(i)[m]) ||
						Double.isInfinite(b.data1.get(i)[m]) || Double.isInfinite(b.data2.get(i)[m]) )
					continue;
				counts[m]++;
				b.measureDifferences[m]+=b.data1.get(i)[m]-b.data2.get(i)[m];
				average[0][m]+=b.data1.get(i)[m];//-b.data2.get(i)[m]
				average[1][m]+=b.data2.get(i)[m];
				average[2][m]+=b.data1.get(i)[m]-b.data2.get(i)[m];
				average[3][m]+=0;
				if(b.data1.get(i)[m]<b.data2.get(i)[m] && desc[m])
					b.better1[m]++;
				else if(b.data1.get(i)[m]>b.data2.get(i)[m] && desc[m])
					b.better2[m]++;
				
				if(b.data1.get(i)[m]>b.data2.get(i)[m] && !desc[m])
					b.better1[m]++;
				else if(b.data1.get(i)[m]<b.data2.get(i)[m] && !desc[m])
					b.better2[m]++;
			}
		}			
		for (int m = 0; m < indexes.length; m++) {
			average[0][m]/=counts[m];				
			average[1][m]/=counts[m];				
			average[2][m]/=counts[m];				
			average[3][m]/=counts[m];				
		}
		return average;
	}
	

	private double[][] getVariances(MethodComparison b, double[][] average){
		double[][] variance = new double[4][b.measureDifferences.length];
		int counts[] = new int[b.measureDifferences.length];
		for (int i = 0; i < b.data1.size(); i++) {
			for (int m = 0; m < indexes.length; m++) {
				if(Double.isNaN(b.data1.get(i)[m]) || Double.isNaN(b.data2.get(i)[m]) ||
						Double.isInfinite(b.data1.get(i)[m]) || Double.isInfinite(b.data2.get(i)[m]) )
					continue;
				//variance[0][m] += //Math.pow(average[m]-(b.data1.get(i)[m]-b.data2.get(i)[m]),2);
				variance[0][m] += (average[0][m]-b.data1.get(i)[m])*(average[0][m]-b.data1.get(i)[m]);
				variance[1][m] += (average[1][m]-b.data2.get(i)[m])*(average[1][m]-b.data2.get(i)[m]);
				variance[2][m] += (average[2][m]-(b.data1.get(i)[m]-b.data2.get(i)[m]))*(average[2][m]-(b.data1.get(i)[m]-b.data2.get(i)[m]));
				variance[3][m] += (average[3][m]-0)*(average[3][m]-0);
				
				counts[m]++;
			}
		}
		for (int m = 0; m < indexes.length; m++) {
			variance[0][m]/=(counts[m]-1.0);		
			variance[1][m]/=(counts[m]-1.0);		
			variance[2][m]/=(counts[m]-1.0);		
			variance[3][m]/=(counts[m]-1.0);					
		}
		return variance;
	}
	
	void writeData(MethodComparison b) {
		int tmp = 0;
		/*for (int i = 0; i < b.data1.size(); i++) {
			if (Double.isNaN(b.data1.get(i)[tmp])
					|| Double.isNaN(b.data2.get(i)[tmp])
					|| Double.isInfinite(b.data1.get(i)[tmp])
					|| Double.isInfinite(b.data2.get(i)[tmp]))
				continue;
			// System.out.println(b.data1.get(i)[tmp]+"\t"+b.data2.get(i)[tmp]);
			System.out.print(b.data1.get(i)[tmp] + ", ");
		}
		System.out.println();
		for (int i = 0; i < b.data1.size(); i++) {
			if (Double.isNaN(b.data1.get(i)[tmp])
					|| Double.isNaN(b.data2.get(i)[tmp])
					|| Double.isInfinite(b.data1.get(i)[tmp])
					|| Double.isInfinite(b.data2.get(i)[tmp]))
				continue;
			System.out.print(b.data2.get(i)[tmp] + ",");
		}
		System.out.println();
		System.out.println();
*/
		for (int i = 0; i < b.data1.size(); i++) {
			if (Double.isNaN(b.data1.get(i)[tmp])
					|| Double.isNaN(b.data2.get(i)[tmp])
					|| Double.isInfinite(b.data1.get(i)[tmp])
					|| Double.isInfinite(b.data2.get(i)[tmp]))
				continue;
			System.out
					.println(b.data1.get(i)[tmp] + "\t" + b.data2.get(i)[tmp]);
		}
	}
	void estimateSignificance() {
		

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + file+"stat.csv"));
		
		for (Object key : mapOfDifferences.keySet()) {
			MethodComparison b = mapOfDifferences.get(key);
			//writeData(b);
			int count[] = new int[indexes.length];

			for (int i = 0; i < b.data1.size(); i++) {

				for (int m = 0; m < indexes.length; m++) {
					if(Double.isNaN(b.data1.get(i)[m]) || Double.isNaN(b.data2.get(i)[m]) ||
							Double.isInfinite(b.data1.get(i)[m]) || Double.isInfinite(b.data2.get(i)[m]) )
						continue;
					count[m]++;
				}
			}
			
			
			
			double[][] average = getAverages(b);
			double[][] variance = getVariances(b,average);

			for (int m = 0; m < indexes.length; m++) {
				double t = (average[0][m]-average[1][m])/
				 (Math.sqrt(((count[m]-1)*variance[0][m]+(count[m]-1)*variance[1][m])/(count[m]+count[m]-2))*
						 Math.sqrt((1.0/count[m]+1.0/count[m])));
				
				double t2 = (average[2][m]-average[3][m])/
				 (Math.sqrt(((count[m]-1)*variance[2][m]+(count[m]-1)*variance[3][m])/(count[m]+count[m]-2))*
						 Math.sqrt((1.0/count[m]+1.0/count[m])));
				
	            //new stuff
	
	            double df = count[m] + count[m] - 2.0;
	            double tt = Math.abs(t);
	            double tt2 = Math.abs(t2);

	            @SuppressWarnings("unused")
				double prob1 = 1, prob = 1;
	            if(tt!=0.0)
	            	prob1 = CommonUtils.stDist(df, tt);
	            if(tt2!=0.0)
	               prob = CommonUtils.stDist(df, tt2);
			    /*if (prob < 0.000001) {
			      prob = 0.0;
			    }  
			    else if (prob > 0.50) {
			      prob = 1.0 - prob;
			    }*/
			    prob = (Math.round(prob * 10000.0)) / 10000.0;
			    tt2 = (Math.round(tt2 * 10000.0)) / 10000.0;
			    out.write((
						b.datasetUser+ "\t"+
						b.method1 + "\t" + b.method2 + "\t"
						+ names[indexes[m]] + "\t"
						+ count[m] + "\t"
						+ average[2][m] + "\t"
						+ variance[2][m] + "\t"						
						+ (tt2)+ "\t"
						+ prob+ "\t"
						+ (b.better1[m]+0.0)/(b.better1[m]+b.better2[m]+0.0)+ "\t"
						+average[2][m]+ "\n").replaceAll("\\.", ",")
						);
			}
			
			

			/*
			for (int m = 0; m < indexes.length; m++) {
				double tValue = (average[0][m])/(variance[0][m]/Math.sqrt(b.data1.size()));
				log.info(
						b.datasetUser+ ";"+
						b.method1 + ";" + b.method2 + ";"
						+ names[indexes[m]] + ";"
						+ (tValue));
			}
			for (int j = 0; j < b.better1.length; j++) {
				if (b.count == 0)
					continue;
				// Method1 is better than Method2
				if (sig <= ((0.0 + b.better1[j] - b.better2[j]) / (b.count))) {
					System.out.println(b.method1 + ";" + b.method2 + ";"
							+ names[j] + ";"
							+ (0.0 + b.better1[j] - b.better2[j]) / (b.count));
				}
				// Method2 is better than Method1
				if (sig <= ((0.0 + b.better2[j] - b.better1[j]) / (b.count))) {
					System.out.println(b.method2 + ";" + b.method1 + ";"
							+ names[j] + ";"
							+ (0.0 + b.better2[j] - b.better1[j]) / (b.count));
				}
			}*/
		}
		out.flush();
		out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void averageAcrossRuns(){


		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + file+"statAvg.csv"));
		
		for (Object key : mapOfDifferences.keySet()) {
			MethodComparison b = mapOfDifferences.get(key);
			//writeData(b);
			int count[] = new int[indexes.length];

			for (int i = 0; i < b.data1.size(); i++) {

				for (int m = 0; m < indexes.length; m++) {
					if(Double.isNaN(b.data1.get(i)[m]) || Double.isNaN(b.data2.get(i)[m]) ||
							Double.isInfinite(b.data1.get(i)[m]) || Double.isInfinite(b.data2.get(i)[m]) )
						continue;
					count[m]++;
				}
			}
			double[][] average = getAverages(b);
			double[][] variance = getVariances(b,average);

			for (int m = 0; m < indexes.length; m++) {
			    out.write((
						b.datasetUser+ "\t"+
						b.method1 + "\t" + b.method2 + "\t"
						+ names[indexes[m]] + "\t"
						+ count[m] + "\t"
						+ average[2][m] + "\t"
						+ variance[2][m] + "\t"		
						+ (b.better1[m]+0.0)/(b.better1[m]+b.better2[m]+0.0)+ "\t"
						+average[2][m]+ "\n").replaceAll("\\.", ",")
						);
			}
		}
		out.flush();
		out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
		
	/**
	 * Loads results of a test into the map.
	 * @param file
	 * @param dataset
	 * @return
	 */
	private void loadResults(String file, String dataset) {

		try {
			CSVReader in = new CSVReader(new FileReader(path + file), ';');
			// CSVWriter writer = new CSVWriter(new FileWriter(outpath), ';',
			// '\"');
			String s[];
			mapOfDataEntries = new HashMap<Object, List<DataEntry>>();
			boolean oneDataset = dataset != null && !dataset.equals("");
			// Skipping the header
			s = in.readNext();
			//List<DataEntry> l = CommonUtils.getList();
			while ((s = in.readNext()) != null) {
				if (oneDataset && !dataset.equals(s[2]))
					continue;
				if(!includeMethods.contains(s[3]) && includeMethods.size()>0)
					continue;
				else if (excludeMethods.contains(s[3]))
					continue;
				
				// System.out.println(Arrays.deepToString(s));
				DataEntry r = new DataEntry();
				r.datasetUser = "a";
				int mattersCount = 0;
				if(considerDataset){
					r.datasetUser += s[2]+"_";
					mattersCount++;
				}
				if(considerUser){
					r.datasetUser += s[4]+"_";
					mattersCount++;
				}
				if(considerRatio){
					r.datasetUser += s[1]+"_";
					mattersCount++;
				}
				r.matters = new String[mattersCount];
				mattersCount = 0;
				if(considerDataset){
					r.matters[mattersCount] = s[2]+"_";
					mattersCount++;
				}
				if(considerUser){
					r.matters[mattersCount] = s[4]+"_";
					mattersCount++;
				}
				if(considerRatio){
					r.matters[mattersCount] = s[1]+"_";
					mattersCount++;
				}
				r.ratio =  CommonUtils.objectToDouble(s[1]);
				if(r.ratio>50)
					continue;
				if(r.ratio==3)
						continue;
				r.method = s[3];
				r.methodhash = r.method.hashCode();
				r.dataset = s[2];
				r.userId = CommonUtils.objectToInteger(s[4].replace(',', '.'));
				r.run = CommonUtils.objectToInteger(s[5].replace(',', '.'));
				if(!mapOfDataEntries.containsKey(r.datasetUser))
					mapOfDataEntries.put(r.datasetUser, new ArrayList<DataEntry>());
					
				mapOfDataEntries.get(r.datasetUser).add(r);
				//r.measures = new double[s.length - 7];
				r.measures = new double[indexes.length];
				
				for (int i = 0; i < indexes.length; i++) {
					if("null".equals(s[indexes[i]+6]))
						s[indexes[i]+6]="0.0";
					r.measures[i] = CommonUtils.objectToDouble(s[indexes[i]+6]);
				}
				//l.add(r);
			}
			in.close();

		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	public void testSignificance(String file, String dataset){
		this.file = file;
		log.info("Start.");
			loadResults(file, dataset);
			log.info("Loaded data.");
			compareMethods();
			log.info("Compared methods.");
			averageAcrossRuns();
			estimateSignificance();
			log.info("End.");
	}

/*
	@SuppressWarnings("unchecked")
	void getVariances(){
		mapOfDifferences = new HashMap();
		Long time = 0L;
		for(List<DataEntry> l1 : mapOfDataEntries.values()){
			Long start = System.currentTimeMillis();
			for (int i = 0; i < l1.size(); i++) {
				DataEntry r1 = l1.get(i);
				//We want different methods
				if(hasSameMethod(r1,r2)){
						time+= System.currentTimeMillis() - start;
						continue;					
				}
				if(!hasSameRun(r1,r2)){
						time+= System.currentTimeMillis() - start;
						continue;					
				}

				if(!considerRatio && r1.ratio != r2.ratio){
						time+= System.currentTimeMillis() - start;
						continue;					
				}
				if(!considerUser && r1.userId != r2.userId){
						time+= System.currentTimeMillis() - start;
						continue;					
				}

					if(!considerDataset && !r1.dataset.equals(r2.dataset)){
						time+= System.currentTimeMillis() - start;
						continue;					
					}		
					MethodComparison b = findComparison(r1, r2);
					b.count++;
					//6-12 vetsi lepsi
					
					//buildTime==14","testTime","countTrain","countTest","countUnableToPredict"
					b.data1.add(r1.measures);
					b.data2.add(r2.measures);
				}
			}
		}		
	}*/
	public void getVariance(String file, String dataset){
		this.file = file;
		log.info("Start.");
			loadResults(file, dataset);
			log.info("Loaded data.");
			log.info("End.");
	}

	public static void main(String[] args) {
	     PropertyConfigurator.configure("log4j.properties");
		SingificanceTest s = new SingificanceTest();
		s.testSignificance("resStatCollFlixTestNew50bStatistical.csv", "");//"Rater7null20carsVirtualMySQLvidomeRatingCarstrue");
		//s.testSignificance("resAll\\resAllNoveTH.csv", null);//"Rater7null20carsVirtualMySQLvidomeRatingCarstrue");
		//"Rater7null20carsVirtualMySQLvidomeRatingCars"
	}
}



class MethodComparison {
	public int count;
	String method1, method2;
	String datasetUser;
	ArrayList<double[]> data1 = new ArrayList<double[]>();
	ArrayList<double[]> data2 = new ArrayList<double[]>();
	double[] measureDifferences;
	int better1[], better2[];
	//List<String> runs;
	//List<Integer> runs;
	//long hash;
	//double ratio;
	//int count;
}
class DataEntry {
	public int methodhash;
	String method;
	String datasetUser;
	double[] measures;
	int run, userId; 
	String dataset;
	String[] matters;
	double ratio;
	
	//long hash;
	//long hashWithoutMethod;
	//long[] mattersHash;
	//String dataset;
	//String[] matters;
	//String mattersToStringed;
}
