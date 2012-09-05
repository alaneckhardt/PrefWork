package prefwork;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.normalizer.Normalizer;
import prefwork.normalizer.VirtualList;
import prefwork.normalizer.VirtualNominal;
import prefwork.normalizer.VirtualPeak;
import prefwork.rater.WeightAverage;

public class VirtualUser {

	int userId;
	Normalizer[] norms;
	Attribute[] attributes;
	public Normalizer[] getNorms() {
		return norms;
	}
	public void setNorms(Normalizer[] norms) {
		this.norms = norms;
	}
	public double[] getWeights() {
		return weights;
	}
	public void setWeights(double[] weights) {
		this.weights = weights;
	}
	double[] weights;
	WeightAverage rater = new WeightAverage();
	
	public Double getRating(List<Object> record){
		Double[] ratings = getRatings(record);
		
		Double res = 1.0;
		res = rater.getRating(ratings, CommonUtils.getValuesFromList(record, attributes));/*
		for (int i = 0; i < ratings.length; i++) {
			if(ratings[i] != null && ratings[i]>0.3 && !Double.isInfinite(ratings[i]) && !Double.isNaN(ratings[i]))
			res *=ratings[i];
		}*/
		if (res != null)
			return /*res*/res/*CommonUtils.roundToDecimals(18.0*res*res,0)*/;
		return res;
	}
	

	public Double[] getRatings(List<Object> record){
		Double[] ratings = new Double[record.size()-1];
		for (int i = 1; i < record.size(); i++) {
			ratings[i-1] = norms[i].normalize(record);
		}
		return ratings;
	}
	@SuppressWarnings("unchecked")
	public void configUser(XMLConfiguration config, String section) {
		int attributeId = 0;
		List<Normalizer> norms = CommonUtils.getList();
		while(config.getProperty( section+".attribute("+attributeId+").class") != null){
			String className = config.getString(section+".attribute("+attributeId+").class");
			Normalizer norm = null;
			if("prefwork.normalizer.VirtualPeak".equals(className)){
				norm = new VirtualPeak(0,0);
			}
			else{
				Constructor[] a;
				try {
					a = Class.forName(className).getConstructors();
					norm = (Normalizer) a[0].newInstance();
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
			norm.configClassifier(config, section+".attribute("+attributeId+")");
			norms.add(norm);
			attributeId ++;
		}
		this.norms = new Normalizer[norms.size()];
		this.norms = norms.toArray(this.norms);
		weights = new double[norms.size()];
		attributeId = 0;
		while(config.getProperty( section+".attribute("+attributeId+").class") != null){
			weights[attributeId] = CommonUtils.getDoubleFromConfIfNotNull(config, section+".attribute("+attributeId+").weight", 1);
			attributeId ++;
		}
		
		rater.setWeights(weights);
	}
	
	public void init(Attribute[] attributes, Random r){
		this.attributes = attributes;
		norms= new Normalizer[attributes.length];
		weights = new double[attributes.length];
		for (int j = 1; j < attributes.length; j++) {
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
		}
		//u.setNorms(norms);
		//u.setWeights(weights);
		
		for (int i = 1; i < norms.length; i++) {
			norms[i].init(attributes[i]);
			
		}
		rater.setWeights(weights);
	}/*
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}*/
}
