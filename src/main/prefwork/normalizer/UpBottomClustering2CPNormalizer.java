package prefwork.normalizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.CommonUtils;
public class UpBottomClustering2CPNormalizer extends Clustering2CPNormalizer {
	/** How many times the clustering of pairs is performed.*/
	int classCount = 1;
	double[][] distances;
	/** Attribute values and the normalizer. These values are divided according to the similarity of the normalizer.*/
	List<Map<Object,Linear>> valuesToCluster = CommonUtils.getList();
	public String toString() {
		if(overallNorm==null)
			return "2CPUpBottom" + classCount ;
		else
			return "2CPUpBottom" + classCount + overallNorm.toString();
	}

	@Override
	public void init(Attribute attribute) {
		index = attribute.getIndex();
		attr = attribute;
		buildGeneralClassifier();
		buildSpecificClassifiers();
		if(mapNorm.size()==0)
			return;
		@SuppressWarnings("unused")
		//List<Object> l= CommonUtils.getList();
		Map<Object, Linear> m = new HashMap<Object, Linear>();
		for(Object o1 :mapNorm.keySet()){
			Linear norm1  = (Linear)mapNorm.get(o1);
			m.put(o1, norm1);
		}
		valuesToCluster.add(m);
		this.clusterMaps();
	}
	/**
	 * Finds the most distant attribute values in the given keys. Distances are computed using associated normalizers in m.
	 * @param keys
	 * @param m
	 * @return
	 */
	protected int[] getDistances(Object[] keys, Map<Object,Linear> m){
		distances = new double[m.entrySet().size()][m.entrySet().size()];
		double max = 0;
		int maxi=-1,maxj=-1;
		for (int i = 0; i < keys.length; i++) {
			Object o1 = keys[i];
			Linear norm1  = (Linear)m.get(o1);
			for (int j = 0; j < keys.length; j++) {
				Object o2 = keys[j];
				Linear norm2  = (Linear)m.get(o2);		
				distances[i][j]=1-norm1.compareTo(norm2); 
				//getDistance(((Linear)norm1).lg.coefficients(), ((Linear)norm2).lg.coefficients());
				if(distances[i][j]> max){
					max = distances[i][j];
					maxi=i;
					maxj=j;
				}
			}			
		}
		return new int[]{maxi,maxj};
	}
	/**
	 *  Divides the normalizers into two groups by given distances.
	 */
	@SuppressWarnings("unchecked")
	protected List<Integer>[] getLists(int[] newCentroids){
		List<Integer> l1= CommonUtils.getList(),l2= CommonUtils.getList();		
		l1.add(newCentroids[0]);
		l2.add(newCentroids[1]);
		for (int i = 0; i < distances.length; i++) {
			if(i==newCentroids[0] || i == newCentroids[1])
				continue;
			if(distances[i][newCentroids[0]]<distances[i][newCentroids[1]])
				l1.add(i);
			else if(distances[i][newCentroids[0]]>distances[i][newCentroids[1]])
				l2.add(i);
			else{
				if(l1.size()<l2.size())
					l1.add(i);
				else
					l2.add(i);
			}				
		}
		return new List[]{l1,l2};
	}
	/**
	 * Divides valuesToCluster according to the given index. 
	 * @param index
	 */
	protected void divide(int index){
		Map<Object,Linear> m = valuesToCluster.get(index);
		Object[] keys = m.keySet().toArray();
		//Get the new centeroids of the clusters.
		int[] newCentroids = getDistances(keys, m);
		// Divides the normalizers into two groups by the distance to the centers maxI
		List<Integer>[] l = getLists(newCentroids);
		Map<Object,Linear> m1 = new HashMap<Object, Linear>();
		Map<Object,Linear> m2 = new HashMap<Object, Linear>();
		for (int i = 0; i < l[0].size(); i++) {
			Object o = keys[l[0].get(i)];
			m1.put(o, m.get(o));
		}
		for (int i = 0; i < l[1].size(); i++) {
			Object o = keys[l[1].get(i)];
			m2.put(o, m.get(o));
		}
		valuesToCluster.remove(index);
		valuesToCluster.add(m1);
		valuesToCluster.add(m2);
	}
	protected double getDistanceOfInnerClusters(Map<Object,Linear> m){
		Object[] keys = m.keySet().toArray();
		int[] maxI = getDistances(keys, m);
		if(maxI[0]==-1)
			return 0;
		double sum = 0;
		List<Integer>[] l = getLists(maxI);
		for (Integer i : l[0]) {
			for (Integer j : l[1]) {
				sum+=distances[i][j];
			}
		}
		return sum;
	}
	
	/**
	 * Cluster those attribute values that have similar fuzzy sets in map.
	 */
	@Override
	protected void clusterMaps(){
		
		for (int i = 0; i < classCount; i++) {
			double max = 0;
			int maxIndex = -1;
			for (int j = 0; j < valuesToCluster.size(); j++) {
				if(valuesToCluster.get(j).size()==1)
					continue;
				double dist = getDistanceOfInnerClusters(valuesToCluster.get(j));	
				if(max < dist){
					max = dist;
					maxIndex = j;
				}
			}
			//Dividing the valuesToCluster
			if(maxIndex>-1)
				divide(maxIndex);			
		}
		for (Map<Object, Linear> m : valuesToCluster) {
			Object[] os = m.keySet().toArray(); 
			if(os.length==1){
				List<Object> l= CommonUtils.getList(1);
				l.add(os[0]);
				clusteredNormMap.put(l, (Linear)mapNorm.get(os[0]));
			}
			for (int i = 1; i < os.length; i++) {
				Linear norm1  = (Linear)mapNorm.get(os[0]);
				Linear norm2  = (Linear)mapNorm.get(os[i]);
				clusterValues(os[0], os[i], norm1, norm2);
			}
		}
	}
	

	
	public Normalizer clone() {
		UpBottomClustering2CPNormalizer norm = new UpBottomClustering2CPNormalizer();
		norm.normName = this.normName;
		norm.overallNorm = getNorm();
		norm.attr = this.attr;
		norm.index = this.index;
		norm.threshold = this.threshold;
		norm.classCount = this.classCount;
		
		return norm ;
	}

	public void configClassifier(XMLConfiguration config, String section) {
		super.configClassifier(config, section);
		Configuration dbConf = config.configurationAt(section);
		classCount = CommonUtils.getIntFromConfIfNotNull(dbConf, "classCount", classCount);
	}
}
