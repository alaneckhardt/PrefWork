package prefwork.normalizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.CommonUtils;
import weka.classifiers.functions.LinearRegression;

public class Clustering2CPNormalizer extends Standard2CPNormalizer {

	//HashMap<List<Object>, List<List<Object>>> clusteredMap = new HashMap<List<Object>, List<List<Object>>>();
	/** Map of list of clustered values and their normalizer*/
	HashMap<List<Object>, Linear> clusteredNormMap = new HashMap<List<Object>, Linear>();

	/**
	 * Threshold for clustering two values. It is defined as fraction of standard deviation of linear parameters.
	 * threshold = 2 means that two values with difference of linear parameter less than stdDev/2 will be clustered together.
	 */
	double threshold = 2;
	double stdDev;
	
	public String toString() {
		return "Clustering2CPMean"+threshold+getNorm().toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Double normalize(List<Object> record) {
		for (Object o : record) {
			for( Entry<List<Object>, Linear> entry: clusteredNormMap.entrySet()){
				List<Object> list = entry.getKey();
				if(list.contains(o+";"+record.indexOf(o))){
					Normalizer norm = entry.getValue();
					if (record.get(index) instanceof List) {
						return getListRating(norm, (List) record.get(index));
					}
					if(norm == null || record.get(index) == null)
						continue;
					double r = norm.normalize(getRecord(record.get(index)));
					if (r != 0.0)
						return r;
					else
						return overallNorm.normalize(getRecord(record.get(index)));
					
				}
			}
		}
		return overallNorm.normalize(getRecord(record.get(index)));
	}
	
	/**
	 * @return Standard deviation of linear parameters of linear fuzzy sets from mapRegr.
	 */
	protected double getStdDeviationOfRegr(){
		if(mapNorm.values().size() == 0)
			return 0;
		int counts[] = new int[3];

		Linear lMean = new Linear();
		double[] lgMean = new double[3];
		
		for(Normalizer norm1 :mapNorm.values()){
			if(!(norm1 instanceof Linear) )
				continue;
			LinearRegression lg1 = ((Linear)norm1).lg;
			if(lg1.coefficients() == null)
				continue;
			for (int i = 0; i < lg1.coefficients().length; i++) {
				lgMean[i] +=lg1.coefficients()[i];
				counts[i]++;				
			}			
		}
		for (int i = 0; i < lgMean.length; i++) {
			lgMean[i] /=counts[i];
		}			
		
		double stdDev = 0;
		lMean.setCoefficients(lgMean);
		for(Normalizer norm1 :mapNorm.values()){
			/*if(!(norm1 instanceof Linear))
				continue;*/

			double dist = norm1.compareTo(lMean);
			//getDistance(lgMean, ((Linear)norm1).lg.coefficients())
			stdDev += dist;			
		}
		return stdDev/=mapNorm.values().size();
	}
	
	protected Linear clusterLinears(Linear l1, Linear l2){
		/*l1.coefficients[0]=(l1.coefficients[0]+l2.coefficients[0])/2;
		l1.coefficients[2]=(l1.coefficients[2]+l2.coefficients[2])/2;*/
		
		 for (int i = 0; l2.isTrainingSet.numInstances()>0; i++) {
				l1.isTrainingSet.add(l2.isTrainingSet.instance(0));
				l2.isTrainingSet.delete(0);
		}
		
		l1.computeRepresentants();
		return l1;
	}
	/**
	 * Cluster two given values together.
	 * @param o1
	 * @param o2
	 */
	protected void clusterValues(Object o1, Object o2,Linear norm1, Linear norm2 ){
		@SuppressWarnings("unused")
		boolean added1=false, added2=false;
		for(Entry<List<Object>, Linear> entry : clusteredNormMap.entrySet()){
			Linear n = entry.getValue();
			List<Object> list = entry.getKey();
			if(list.contains(o1)){
				//Already clustered values
				if(list.contains(o2))
					return;
				for(Entry<List<Object>, Linear> entry2 : clusteredNormMap.entrySet()){
					List<Object> list2 = entry2.getKey();
					if(list2.contains(o2)){
						Linear n2 = entry2.getValue();
						n = clusterLinears(n, n2);
						list.addAll(list2);
						clusteredNormMap.remove(list2);
						return;
					}
				}
				list.add(o2);
				n = clusterLinears(n, norm2);
				return;
			}
			if(list.contains(o2)){	
				//Already clustered values
				if(list.contains(o1))
					return;		
				for(Entry<List<Object>, Linear> entry2 : clusteredNormMap.entrySet()){
					List<Object> list2 = entry2.getKey();
					if(list2.contains(o1)){
						Linear n2 = entry2.getValue();
						n = clusterLinears(n, n2);
						list.addAll(list2);
						clusteredNormMap.remove(list2);
						return;
					}				
				}
				list.add(o1);
				n = clusterLinears(n, norm2);
				return;
			}
		}
			List<Object> l = CommonUtils.getList(2);
			l.add(o1);
			l.add(o2);
			Linear n = clusterLinears(norm1, norm2);
			clusteredNormMap.put(l, n);		
	}
	
/*
	protected double getDistance(double[] lg1, double[] lg2) {
		double dist = 0;
		int i = 0;
		for (; i < lg1.length && i < lg2.length; i++) {
			dist += Math.abs(lg1[i] - lg2[i]);
		}
		return dist / (i-1);
	}*/
	
	
	/**
	 * Cluster those attribute values that have similar fuzzy sets in map.
	 */
	protected void clusterMaps(){
		// For all attribute values that are in set map and mapRegr
		// we will try to cluster those with similar fuzzy set (represented by linear functions in mapRegr)
		stdDev = getStdDeviationOfRegr();
		
		for(Object o1 :map.keySet()){
			Linear norm1  = (Linear)mapNorm.get(o1);
			for(Object o2 :map.keySet()){
				if(o1 == o2)
					continue;
				Linear norm2  = (Linear)mapNorm.get(o2);
				if(norm1 == null || norm2 == null)
					continue;
				if(!(norm1 instanceof Linear) ||!(norm2 instanceof Linear))
					continue;
				double dist = norm1.compareTo(norm2);
				//getDistance(((Linear)norm1).lg.coefficients(), ((Linear)norm2).lg.coefficients())
				if(stdDev/threshold > dist)
				{
					clusterValues(o1,o2, norm1, norm2);
				}				
			}
		}
	}

	public void configClassifier(XMLConfiguration config, String section) {
		super.configClassifier(config, section);
		Configuration dbConf = config.configurationAt(section);
		if(dbConf.containsKey("threshold")){
			threshold = dbConf.getDouble("threshold");
		}
	}
	
	@Override
	public void init(Attribute attribute) {
		super.init(attribute);
		for (Entry<Object, Normalizer> entry : mapNorm.entrySet()) {
			List<Object> l= CommonUtils.getList(1);
			l.add(entry.getKey());
			clusteredNormMap.put(l, (Linear)entry.getValue());
		}
		this.clusterMaps();
	}

	public Normalizer clone() {
		Clustering2CPNormalizer norm = new Clustering2CPNormalizer();
		norm.normName = this.normName;
		norm.overallNorm = getNorm();
		norm.attr = this.attr;
		norm.index = this.index;
		norm.threshold = this.threshold;
		return norm ;
	}
}
