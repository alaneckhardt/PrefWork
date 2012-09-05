package prefwork;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import prefwork.normalizer.Normalizer;
import prefwork.normalizer.VirtualList;
import prefwork.normalizer.VirtualNominal;
import prefwork.normalizer.VirtualPeak;

public class CombinationUser extends VirtualUser {
	Attribute[] attributes;
	int maxAttributes = 50;
	public Double getRating(List<Object> record){
		Double[] ratings = getRatings(record);
		
		Double res = 0.0;
		/*for (int i = 0; i < ratings.length; i++) {
			for (int j = i + 1; j < ratings.length; j++) {
				res+=ratings[i]*ratings[j]*weights[i]*weights[j];
			}
		}*/
		for (int i = 0; i < ratings.length; i++) {
				res+=ratings[i]*weights[i];
		}
		if (res != null)
			return res;
		return res;
	}

	@SuppressWarnings("unchecked")
	Object getValue(Object o1, Object o2){
		if(o1 instanceof List)
		{
			if(o2 instanceof List)
			{
				List<Object> l = CommonUtils.getList(((List)o1).size()*((List)o2).size());
				for (Object obj1 : (List)o1) {
					for (Object obj2 : (List)o2) {
						l.add(obj1.toString()+";"+obj2.toString());
					}
				}
				return l;
			}
			List<Object> l = CommonUtils.getList(((List)o1).size());
			for (Object obj1 : (List)o1) {
					l.add(obj1.toString()+";"+o2.toString());
			}
			return l;
		}
		if(o2 instanceof List)
		{
			if(o1 instanceof List)
			{
				List<Object> l = CommonUtils.getList(((List)o1).size()*((List)o2).size());
				for (Object obj1 : (List)o2) {
					for (Object obj2 : (List)o1) {
						l.add(obj1.toString()+";"+obj2.toString());
					}
				}
				return l;
			}
			List<Object> l = CommonUtils.getList(((List)o2).size());
			for (Object obj1 : (List)o2) {
					l.add(o1.toString()+";"+obj1.toString());
			}			
			return l;
		}
		return o1.toString()+";"+o2.toString();
	}
	
	/**Adds the combination of attributes
	 * 
	 * @param l
	 * @return
	 */
	List<Object> transformRecord(List<Object> l){
		List<Object> l2 = CommonUtils.getList(Math.min(l.size()*l.size(),maxAttributes));
		
		for (int i = 0; i < l.size(); i++) {
			l2.add(l.get(i));
		}
		for (int i = 1; i < l.size()-1 && l2.size() < maxAttributes; i++) {
			for (int j = i+1; j < l.size() && l2.size() < maxAttributes; j++) {
				l2.add(getValue(l.get(i),l.get(j)));
			}
		}
		return l2;
	}
	public Double[] getRatings(List<Object> record){
		List<Object> rec2 = transformRecord(record);
		Double[] ratings = new Double[rec2.size()-1];
		for (int i = 1; i < rec2.size(); i++) {
			ratings[i-1] = Math.sin(1+norms[i].normalize(rec2));
		}
		return ratings;
	}
	

	@SuppressWarnings("unchecked")
	public void init(Attribute[] attributes, Random r){

		norms= new Normalizer[attributes.length+(attributes.length-1)*(attributes.length-2)/2];
		weights = new double[attributes.length+(attributes.length-1)*(attributes.length-2)/2];
		int attrIndex = 0;
		for (int j = 0; j < attributes.length; j++) {
			if(attributes[j].getType()==Attribute.NUMERICAL){
				norms[j] = new VirtualPeak(attributes[j].getMin(),attributes[j].getMax());
				((VirtualPeak)norms[j]).setPeak(attributes[j].getMin()+r.nextDouble()*(attributes[j].getMax()-attributes[j].getMin()));					
				}
			else if(attributes[j].getType()==Attribute.NOMINAL){					
				HashMap<String, Double> map = new HashMap<String, Double>();	
				for (AttributeValue val : attributes[j].getValues()) {
					map.put(val.getValue().toString(), r.nextDouble());
				}
				norms[j] = new VirtualNominal();	
				((VirtualNominal)norms[j]).setMap(map);
			}
			else if(attributes[j].getType()==Attribute.LIST){					
				HashMap<String, Double> map = new HashMap<String, Double>();	
				for (AttributeValue val : attributes[j].getValues()) {
					map.put(val.getValue().toString(), r.nextDouble());
				}
				norms[j] = new VirtualList();	
				((VirtualList)norms[j]).setMap(map);
			}
			weights[j] = r.nextDouble();
			attrIndex++;
		}

		for (int i = 1; i < attributes.length && attrIndex < maxAttributes; i++) {
			for (int j = i+1; j < attributes.length && attrIndex < maxAttributes; j++) {
				HashMap map = new HashMap();
				List<AttributeValue> val1 = attributes[i].getValues();
				List<AttributeValue> val2 = attributes[j].getValues();
				for (int k = 0; k < val1.size(); k++) {
					for (int l = 0; l < val2.size(); l++) {
						map.put(getValue(val1.get(k).getValue(),val2.get(l).getValue()), r.nextDouble());
					}
				}

				if(attributes[i].getType()==Attribute.LIST || attributes[j].getType()==Attribute.LIST)
					norms[attrIndex] = new VirtualList();	
				else
					norms[attrIndex] = new VirtualNominal();
				((VirtualNominal)norms[attrIndex]).setMap(map);
				weights[attrIndex] = r.nextDouble();
				attrIndex++;
			}
		}
		norms = Arrays.copyOf(norms, attrIndex);
		weights = Arrays.copyOf(weights, attrIndex);
		//u.setNorms(norms);
		//u.setWeights(weights);
		this.attributes = new Attribute[attrIndex];
		for (int i = 0; i < attributes.length; i++) {
			this.attributes[i] = attributes[i].clone();
		}
		//Adding combination of attributes
		for (int i = attributes.length; i < attrIndex; i++) {
			this.attributes[i] = new Attribute(null, i, "Attr"+i);
			this.attributes[i].setType(Attribute.NOMINAL);
		}

		for (int i = 1; i < norms.length; i++) {
			norms[i].init(this.attributes[i]);
			
		}
		rater.setWeights(weights);
	}
}
