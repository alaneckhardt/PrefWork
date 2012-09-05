package prefwork.normalizer;

import java.util.HashMap;
import java.util.List;

import prefwork.Attribute;
import prefwork.CommonUtils;
import weka.classifiers.functions.LinearRegression;
import weka.core.FastVector;
import weka.core.Instances;
//TODO - rewrite whole class and check the behaviour.
public class BottomUpClustering2CPNormalizer extends Clustering2CPNormalizer {
	protected Instances isTrainingSet; 
	protected FastVector fvWekaAttributes;
   protected Instances representants; 
   protected int numberOfClusters = 0; 
protected LinearRegression overallRegression = new LinearRegression(); 
protected Normalizer overallRegressionNorm =getNorm(); 
protected HashMap<Object, Linear> mapRegression =  new HashMap<Object, Linear>(); 
protected HashMap<Object, Normalizer> mapRegressionNorm = new HashMap<Object, Normalizer>(); 
//	  Classification 
	  protected HashMap<Object, Double> overallClassification =
	  new HashMap<Object, Double>();
	  protected HashMap<Object, HashMap<Object,Double>> mapClassification = new HashMap<Object, HashMap<Object,Double>>();

	public BottomUpClustering2CPNormalizer(){
		threshold = 2;
	}
	public String toString() {
		return "BottomUp2CP"+threshold;
	}

	protected void insertSingleValues() {
		for (Object o : mapRegression.keySet()) {
			List<Object> list = CommonUtils.getList(1);
			list.add(o);
			//clusteredMap.put(list, map.get(o));
			clusteredNormMap.put(list, (Linear)mapRegressionNorm.get(o));
		}
	}

	protected void cluster(List<Object> list1, List<Object> bestList) {
	/*	List<List<Object>> objects = clusteredMap.get(list1);
	//	objects.addAll(clusteredMap.get(bestList));
	//	clusteredMap.remove(list1);
	//	clusteredMap.remove(bestList);
		clusteredNormMap.remove(list1);
		clusteredNormMap.remove(bestList);
		list1.addAll(bestList);
	//	clusteredMap.put(list1, objects);
		Instances train = new Instances("Rel", fvWekaAttributes, 10);
		train.setClassIndex(1);
		for (List<Object> rec : objects) {
			Instance iExample = new Instance(2);
			iExample.setDataset(train);
			iExample.setValue((weka.core.Attribute) fvWekaAttributes
					.elementAt(0), Double
					.parseDouble(rec.get(index).toString()));
			iExample.setValue((weka.core.Attribute) fvWekaAttributes
					.elementAt(1), Double.parseDouble(rec.get(2).toString()));
			train.add(iExample);
		}
		try {
			LinearRegression lg = new LinearRegression();
			lg.buildClassifier(train);
			mapRegression.put(list1, lg);
		} catch (Exception e) {
			e.printStackTrace();
		}*/

	}


	/**
	 * Clusters most similar values from the map.
	 * 
	 * @return True if something was clustered, otherwise false.
	 */
	protected boolean performClustering() {
		for (List<Object> list1 : clusteredNormMap.keySet()) {
			double distance = -1;
			List<Object> bestList = null;
			for (List<Object> list2 : clusteredNormMap.keySet()) {
				if (list1 == list2)
					continue;
				if (distance == -1
						|| distance > mapRegression.get(list1).compareTo(mapRegression.get(list2))) {
					distance = mapRegression.get(list1).compareTo(mapRegression.get(list2));
					bestList = list2;
				}
			}
			if (distance != -1 && distance < stdDev / threshold) {
				cluster(list1, bestList);
				return true;
			}
		}
		return false;
	}


	/**
	 * @return Standard deviation of linear parameters of linear fuzzy sets from mapRegr.
	 */
	protected double getStdDeviationOfRegr(){
		double meana = 0, meanb = 0;
		int counta = 0, countb = 0; 
		for(Linear l :mapRegression.values()){
			if(l.lg.coefficients().length > 1){
				meana+= l.lg.coefficients()[0];
				counta ++;	

				meanb+= l.lg.coefficients()[2];
				countb ++;	
			}else{
				meanb+= l.lg.coefficients()[0];
				countb ++;				
			}
			
		}
		meana /= counta;
		meanb /= countb;
		double stdDev = 0;
		for(Linear l :mapRegression.values()){
			if(l.lg.coefficients().length > 1){
				stdDev+= (2*Math.abs(l.lg.coefficients()[0] - meana)+Math.abs(l.lg.coefficients()[2] - meanb))/3;
				
			}else{
				stdDev+= Math.abs(l.lg.coefficients()[0] - meanb);							
			}
		}
		return stdDev;
	}
	
	/**
	 * Clusted those attribute values that have similar fuzzy sets in mapRegr.
	 */
	protected void clusterMaps() {
		// Initialization with single values
		insertSingleValues();

		// Now we perform bottom up clustering , stopped by threshold value
		boolean clusteredSomething = true;
		while (clusteredSomething) {
			stdDev = getStdDeviationOfRegr();
			clusteredSomething = performClustering();
		}
	}

	@Override
	public void init(Attribute attribute) {
		super.init(attribute);
		this.clusterMaps();
	}

	public Normalizer clone() {
		return new BottomUpClustering2CPNormalizer();
	}
}
