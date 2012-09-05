package prefwork.rater;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import prefwork.normalizer.Normalizer;

public class InstancesRater implements Rater{
	//List<AttributeValue[]> instances;
	List<List<Object>> instances;

	List<Double[]> instancesLocPref;
	
	Attribute[] attributes;
	

	/**
	 * Minimal difference of attributes count in which one object is to be better than another
	 * to be considered better in general.
	 * (Difference between isBetter and isWorse count.)
	 */
	int thresholdIsBetter = 0;
	
	int targetAttribute;
	List<List<Object>> paretoup;
	List<List<Object>> paretodown;
	
	public String toString(){
		return "a";
	}

	
	public int getTargetAttribute() {
		return targetAttribute;
	}

	public void setTargetAttribute(int targetAttribute) {
		this.targetAttribute = targetAttribute;
	}

	public void configClassifier(XMLConfiguration config, String section) {
		//threshold = CommonUtils.getIntFromConfIfNotNull(methodConf, "threshold", threshold);
	}

	public Object clone() {
		InstancesRater r = new InstancesRater();
		r.thresholdIsBetter = thresholdIsBetter;
		return r;
	}
	
	protected boolean checkPareto(List<List<Object>> pareto, List<Object> newElement, boolean up){

		Double[] locPrefnewElement = instancesLocPref.get(findInstanceIndex(newElement));
		for (List<Object> instance : pareto) {
			int recordIsBetter = 0, recordIsWorse = 0;

			Double[] locPref = instancesLocPref.get(findInstanceIndex(instance));
			for (int i = 3; i < attributes.length; i++) {
				if( newElement.get(i) == null)
					continue;
				if( instance.get(i) == null)
					continue;
				if(locPrefnewElement[i] == null || locPref[i] == null)
					continue;
				
				// newElement is better, but we want higher pareto (worst objects from better objects)
				if ( locPrefnewElement[i] > locPref[i] && up) {
					recordIsBetter++;
					//return false;
				}
				// newElement is worse, but we want lower pareto (best objects from worse objects)
				if (locPrefnewElement[i] < locPref[i] && !up) {
					recordIsWorse++;
					//return false;
				}
			}		
			if (recordIsBetter > thresholdIsBetter) {
				return false;
			} 
			if (recordIsWorse  > thresholdIsBetter) {	
				return false;
			}
		}
		return true;
	}
	protected List<List<Object>> findPareto(Double[] record){
		List<List<Object>> pareto = CommonUtils.getList();
		for (List<Object> instance : instances) {
			int recordIsBetter = 0, recordIsWorse = 0;

			Double[] locPref = instancesLocPref.get(findInstanceIndex(instance));
			for (int i = 3; i < attributes.length; i++) {
				if(record[i] == null || instance.get(i) == null || instance.get(i) == null || locPref[i] == null)
					continue;
				if (record[i] > locPref[i]) {
					recordIsBetter++;
				}
				if (record[i] < locPref[i]) {
					recordIsWorse++;
				}
			}
			//Record is always better -> paretodown
			if (recordIsWorse <= thresholdIsBetter) {
				if(checkPareto(paretodown, instance, false)){
					paretodown.add(instance);
				}
			} 
			//Record is always worse -> paretoup
			if (recordIsBetter <= thresholdIsBetter) {
				if(checkPareto(paretoup, instance, true)){
					paretoup.add(instance);
				}
			}
		}
		return pareto;
	}
	
	protected int findInstanceIndex(List<Object> rec){
		for (int i = 0; i < instances.size(); i++) {
			List<Object> instance = instances.get(i);
			if(instance.get(1).equals(rec.get(1)))
				return i;
			
		}
		return -1;
	}
	
	protected Double getParetoRating(Double[] rec, List<List<Object>> pareto){
		if(pareto.size() == 0)
			return 0D;
		double sumRating = 0;
		double sumDistance = 0;
		for (List<Object> instance : pareto) {
			Double[] locPref = instancesLocPref.get(findInstanceIndex(instance));
			double distance = distance(locPref, rec);
			if(Double.isInfinite(distance) || Double.isNaN(distance) || distance == 0.0)
				distance = 0.001;
			sumRating += CommonUtils.objectToDouble(instance.get(targetAttribute))/distance;
			sumDistance += 1/distance;
		}
		if(sumDistance == 0)
			return 0D;
		return sumRating/sumDistance;			
	}

	public Double getRating(List<Object> record) {

/*
		double ratingAbove = 0, ratingBellow = 0;
		double ratingAboveCount = 0, ratingBellowCount = 0;
		for (AttributeValue[] instance : instances) {
			int recordIsBetter = 0, recordIsWorse = 0;
			for (int i = 3; i < attributes.length; i++) {
				Normalizer norm = attributes[i].getNorm();
				if(record.get(i) == null || instance[i] == null || instance[i].getValue() == null)
					continue;
				if (norm.compare(record.get(i).getValue(), instance[i].getValue()) > 0) {
					recordIsBetter++;
				}
				if (norm.compare(record.get(i).getValue(), instance[i].getValue()) < 0) {
					recordIsWorse++;
				}
			}
			if (recordIsBetter - recordIsWorse > thresholdIsBetter) {
				ratingAbove+=Double.parseDouble(instance[targetAttribute].getValue().toString());
				ratingAboveCount++;

			} 
			if (recordIsWorse - recordIsBetter > thresholdIsBetter) {
				ratingBellow+=Double.parseDouble(instance[targetAttribute].getValue().toString());
				ratingBellowCount++;
			}
		}*/
		Double[] locPrefRec = CommonUtils.getLocalPref(record, attributes);
		paretodown = CommonUtils.getList();
		paretoup = CommonUtils.getList();
		findPareto(locPrefRec);
		//findPareto(locPrefRec, false);
		double ratingUp = 0.0D;
		double ratingDown = 0.0D;
		ratingUp = getParetoRating(locPrefRec, paretoup);
		ratingDown = getParetoRating(locPrefRec, paretodown);
		/*
		if(paretoup.size()==0){
			ratingUp = distance(record, 1.0);
		}
		else
			ratingUp = getParetoRating(record, paretoup);

		if(paretodown.size()==0){
			ratingUp = distance(record, 0.0);
		}
		else
			ratingDown = getParetoRating(record, paretodown);
		*/
		if(ratingUp == 0)
			return ratingDown;
		if(ratingDown == 0)
			return ratingUp;
		return (ratingUp+ratingDown)/2;
		/*ratingAbove/=ratingAboveCount;
		ratingBellow/=ratingBellowCount;
		if(Double.isNaN(ratingBellow)||Double.isInfinite(ratingBellow))
			return ratingAbove;
		if(Double.isNaN(ratingAbove)||Double.isInfinite(ratingAbove))
			return ratingBellow;
		return  (ratingAbove + ratingBellow)/2 ;*/
	}


	protected double distance(List<Object> rec1, Double rating){
		double distance = 0;		
		int denominator = 0;
		for (int i = 3;i<rec1.size();i++) {
			Normalizer norm = attributes[i].getNorm();
			if(rec1.get(i) == null){
				continue;
			}
			double pref1 = norm.normalize(rec1);
			double pref2 = rating;
			distance += Math.abs(pref1 - pref2);
			denominator++;
		}
		return distance / rec1.size();
		
	}
	
	protected double distance(Double[] rec1, Double[] rec2){
		double distance = 0;		
		int denominator = 0;
		for (int i = 3;i<rec1.length;i++) {
			if(rec1[i] == null || rec2[i] == null ){
				continue;
			}
			double pref1 = rec1[i];
			double pref2 = rec2[i];
			distance += Math.abs(pref1 - pref2);
			denominator++;
		}
		return distance / rec1.length;
	}
	
	
	public void setInstances(List<List<Object>> instances) {
		this.instances = instances;
		instancesLocPref = CommonUtils.getList(instances.size());
		for(List<Object> rec : instances){
			Double[] locPref = CommonUtils.getLocalPref(rec, attributes);
				/*new Double[rec.size()];
			for (int i = 3; i < locPref.length; i++) {
				locPref[i] = attributes[i].getNorm().normalize(rec);
			}*/
			instancesLocPref.add(locPref);
		}		
	}
	
	public void init(Attribute[] attributes) {		
		this.attributes = attributes;
		this.thresholdIsBetter = (attributes.length-3)/4;
	}


	@Override
	public Double getRating(Double[] ratings, AttributeValue[] record) {
		List<Object> rec = CommonUtils.getList(record.length);
		for(AttributeValue val : record){
			rec.add(val);
		}
		return getRating(rec);
	}


	@Override
	public double compareTo(Rater n) {
		// TODO Auto-generated method stub
		return 0;
	}

}
