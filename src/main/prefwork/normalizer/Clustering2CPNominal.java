package prefwork.normalizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.CommonUtils;

public class Clustering2CPNominal extends Standard2CPNormalizer {

	//HashMap<List<Object>, List<List<Object>>> clusteredMap = new HashMap<List<Object>, List<List<Object>>>();
	/** Map of list of clustered values and their normalizer*/
	HashMap<List<Object>, RepresentantNormalizer> clusteredNormMap = new HashMap<List<Object>, RepresentantNormalizer>();

	/**
	 * Threshold for clustering two values. It is defined as fraction of standard deviation of linear parameters.
	 * threshold = 2 means that two values with difference of linear parameter less than stdDev/2 will be clustered together.
	 */
	double threshold = 2;
	double stdDev;
	
	public String toString() {
		return "Clustering2CPMean"+threshold;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Double normalize(List<Object> record) {
		for (Object o : record) {
			for( Entry<List<Object>, RepresentantNormalizer> entry: clusteredNormMap.entrySet()){
				List<Object> list = entry.getKey();
				if(list.contains(o+";"+record.indexOf(o))){
					RepresentantNormalizer repr = entry.getValue();
					if (record.get(index) instanceof List) {
						return getListRating(repr, (List) record.get(index));
					}
					double r = repr.normalize(getRecord(record.get(index)));
					if (r != 0.0)
						return r;
					else
						return overallNorm.normalize(getRecord(record.get(index)));
					
				}
			}
		}
		return 0.0;
	}
	
	/**
	 * @return Standard deviation of linear parameters of linear fuzzy sets from mapRegr.
	 */
	protected double getStdDeviationOfRegr(){
		if(mapNorm.values().size() == 0)
			return 0;
		
		RepresentantNormalizer reprMean = new RepresentantNormalizer();
		
		for(Normalizer norm1 :mapNorm.values()){
			if(!(norm1 instanceof RepresentantNormalizer) )
				continue;
			@SuppressWarnings("unused")
			RepresentantNormalizer norm = (RepresentantNormalizer)norm1;
			//TODO - distance of two representant normalizers.
		}		
		double stdDev = 0;
		for(Normalizer norm1 :mapNorm.values()){
			if(!(norm1 instanceof Linear))
				continue;
			
			stdDev +=getDistance(reprMean, ((RepresentantNormalizer)norm1));			
		}
		return stdDev/=mapNorm.values().size();
	}
	
	protected RepresentantNormalizer clusterRepresentants(RepresentantNormalizer l1, RepresentantNormalizer l2){
		//TODO
		return null;
	}
	/**
	 * Cluster two given values together.
	 * @param o1
	 * @param o2
	 */
	protected void clusterValues(Object o1, Object o2,RepresentantNormalizer norm1, RepresentantNormalizer norm2 ){
		@SuppressWarnings("unused")
		boolean added1=false, added2=false;
		for(Entry<List<Object>, RepresentantNormalizer> entry : clusteredNormMap.entrySet()){
			RepresentantNormalizer n = entry.getValue();
			List<Object> list = entry.getKey();
			if(list.contains(o1)){
				//Already clustered values
				if(list.contains(o2))
					return;
				for(Entry<List<Object>, RepresentantNormalizer> entry2 : clusteredNormMap.entrySet()){
					List<Object> list2 = entry2.getKey();
					if(list2.contains(o2)){
						RepresentantNormalizer n2 = entry2.getValue();
						n = clusterRepresentants(n, n2);
						list.addAll(list2);
						clusteredNormMap.remove(list2);
						return;
					}
				}
				list.add(o2);
				n = clusterRepresentants(n, norm2);
				return;
			}
			if(list.contains(o2)){	
				//Already clustered values
				if(list.contains(o1))
					return;		
				for(Entry<List<Object>, RepresentantNormalizer> entry2 : clusteredNormMap.entrySet()){
					List<Object> list2 = entry2.getKey();
					if(list2.contains(o1)){
						RepresentantNormalizer n2 = entry2.getValue();
						n = clusterRepresentants(n, n2);
						list.addAll(list2);
						clusteredNormMap.remove(list2);
						return;
					}				
				}
				list.add(o1);
				n = clusterRepresentants(n, norm2);
				return;
			}
		}
			List<Object> l = CommonUtils.getList(2);
			l.add(o1);
			l.add(o2);
			RepresentantNormalizer n = clusterRepresentants(norm1, norm2);
			clusteredNormMap.put(l, n);		
	}
	

	protected double getDistance(RepresentantNormalizer repr1, RepresentantNormalizer repr2) {
		//TODO
		return 0;
	}
	
	
	/**
	 * Clusted those attribute values that have similar fuzzy sets in mapRegr.
	 */
	protected void clusterMaps(){
		// For all attribute values that are in set map and mapRegr
		// we will try to cluster those with similar fuzzy set (represented by linear functions in mapRegr)
		stdDev = getStdDeviationOfRegr();
		
		for(Object o1 :map.keySet()){
			RepresentantNormalizer norm1  = (RepresentantNormalizer)mapNorm.get(o1);
			for(Object o2 :map.keySet()){
				if(o1 == o2)
					continue;
				RepresentantNormalizer norm2  = (RepresentantNormalizer)mapNorm.get(o2);
				if(norm1 == null || norm2 == null)
					continue;
				if(!(norm1 instanceof RepresentantNormalizer) ||!(norm2 instanceof RepresentantNormalizer))
					continue;
				
				if(stdDev/threshold > getDistance(((RepresentantNormalizer)norm1), ((RepresentantNormalizer)norm2)))
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
			clusteredNormMap.put(l, (RepresentantNormalizer)entry.getValue());
		}
		this.clusterMaps();
	}

	public Normalizer clone() {
		Clustering2CPNominal norm = new Clustering2CPNominal();
		norm.normName = this.normName;
		norm.overallNorm = getNorm();
		norm.attr = this.attr;
		norm.index = this.index;
		norm.threshold = this.threshold;
		return norm ;
	}
}
