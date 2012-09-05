package prefwork.normalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import prefwork.representant.AvgRepresentant;

public class ListNormalizer implements Normalizer{

	Normalizer repr = new RepresentantNormalizer();
	// Virtual attribute that stores values from lists
	Attribute virtualAttr = new Attribute();
	
	/**Map of object of list and the rating of the list.**/
	HashMap<Object,Double> mapOfRatings = new HashMap<Object,Double>();
	int index;
	
	// Attribute that stores lists of values
	Attribute attr = new Attribute();
	
	public ListNormalizer(){	
	}
	
	public ListNormalizer(Attribute attr){	
		init(attr);		
	}

	private Double average(List<Double> list){
		double sum = 0, count = 0;
		for(Double r : list){
			if(r == null)
				continue;
			count ++;
			sum += r;			
		}
		if(count == 0)
			return null;
		return sum/count;
	}
	
	/**
	 * Finds all list containing the specified value
	 * @param val Value to search for
	 * @return List of all lists that contain val.
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	private List<AttributeValue> findValues(Object val){
		if(val == null)
			return null;
		List<AttributeValue> l = CommonUtils.getList(3);		
		for(AttributeValue tempVal:attr.getValues()){
			for(Object o :(List<Object>)tempVal.getValue()){
				if(o == null)
					return null;
				if(o.equals(val))
					l.add(tempVal);
			}
		}
		return l;
	}
	
	@SuppressWarnings("unchecked")
	public Double normalize(List<Object> o) {
		List<Object> l = (List<Object>) o.get(index);
		if(l == null || l.size() == 0)
			return null;
		/*for(Object val : l){
			List<AttributeValue> values = findValues(val);
			// Value o wasn't encountered during training phase
			if(values.isEmpty())
				continue;
			AttributeValue tempValue = new AttributeValue(attr, val);
			for(AttributeValue val2 : values){
				for(Double r : val2.getRatings())
					tempValue.addRating(r);		
				for(List<Object> rec : val2.getRecords())		
					tempValue.addRecord(rec);
			}
			
			virtualAttr.addValue(tempValue);
		}
		repr.init(virtualAttr);
		List<Double> ratings = CommonUtils.getList();
		for(Object val : l){
			List lTemp = CommonUtils.getList();
			lTemp.add(val);
			ratings.add(repr.normalize(lTemp));			
		}
		virtualAttr = new Attribute();
		virtualAttr.setIndex(0);*/
		double rating = 0;
		int count = 0;
		for(Object val : l){
			Double d = mapOfRatings.get(val);
			if(d == null)
				continue;
			rating+=d;
			count++;
		}
		if(count == 0)
			return 0.0;
		return rating/count;		
	}

	@SuppressWarnings("unchecked")
	public void init(Attribute attr) {
		this.attr = attr;
		HashMap<Object,List<Double>> mapOfRatings = new HashMap<Object,List<Double>>();
		for ( AttributeValue val : attr.getValues()) {
			List<Object> l = (List)val.getValue();
			for (Object o : l) {
				if(!mapOfRatings.containsKey(o)){
					mapOfRatings.put(o, new ArrayList());
				}
				mapOfRatings.get(o).addAll(val.getRatings());
			}

		}
		for (Object o : mapOfRatings.keySet()) {
			this.mapOfRatings.put(o, average(mapOfRatings.get(o)));
		}
		index = attr.getIndex();
	}

	public Normalizer clone() {
		ListNormalizer l = new ListNormalizer();
		l.repr = this.repr.clone();
		return l;
	}

	public int compare(List<Object> arg0, List<Object> arg1) {
		if(normalize(arg0)>normalize(arg1))
			return 1;
		else if(normalize(arg0)<normalize(arg1))
			return -1;
		else
			return 0;
	}
	public String toString(){
		return "L"+repr.toString();
	}

	public void configClassifier(XMLConfiguration config, String section) {
		Configuration methodConf = config.configurationAt(section);
		if (methodConf.containsKey("normalizer")) {
			String normalizerName = methodConf.getString("normalizer");
			repr = (Normalizer) CommonUtils.getInstance(normalizerName);
			repr.configClassifier(config, section+".normalizer");
		}
		else {
			repr = new RepresentantNormalizer();
			((RepresentantNormalizer)repr).representant =  new AvgRepresentant();
		}
	}

	@Override
	public double compareTo(Normalizer n) {
		if(!(n instanceof ListNormalizer))
			return 0;
		ListNormalizer n2 = (ListNormalizer)n;
		Set<Object> values = mapOfRatings.keySet();
		Set<Object> n2values =  n2.mapOfRatings.keySet();
		double diff=0;
		int count =0;
		for (Object av1 : values) {
			if(n2values.contains(av1)){
				count++;
				diff+=Math.abs(mapOfRatings.get(av1)-n2.mapOfRatings.get(av1));				
			}
		}
		if(count==0)
			return 0;
		return 1-diff/(5*count);
	}
}
