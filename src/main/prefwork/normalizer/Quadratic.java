package prefwork.normalizer;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import weka.clusterers.SimpleKMeans;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class Quadratic extends Linear{


	/*weka.classifiers.functions.LinearRegression lg = new LinearRegression();
	Instances isTrainingSet;
	FastVector fvWekaAttributes;
	Attribute attr;
	int numberOfClusters = 0;
	int index;
	Instances representants;*/
	

	public Quadratic(){
	}
	
	public Quadratic(Attribute attr){	
		init(attr);
	}
	protected void computeRepresentants(){
		SimpleKMeans cluster = new SimpleKMeans();
		try {

			if(numberOfClusters != 0){
				cluster.setNumClusters(numberOfClusters);
				cluster.buildClusterer(isTrainingSet);
				representants = cluster.getClusterCentroids();
				representants.setClassIndex(2);
				lg.buildClassifier(representants);
			}
			else
				lg.buildClassifier(isTrainingSet);

			coefficients = lg.coefficients();
			coefficients = lg.coefficients();
			if(	coefficients[1] != 0.0 ){
//				System.out.println("X^2="+coefficients[1]+",X="+coefficients[0]+",C="+coefficients[3]);
			}
		} catch (weka.core.WekaException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public Double normalize(List<Object> record) {
		try {
			return coefficients[3]+coefficients[0]*CommonUtils.objectToDouble(record.get(index))+
			coefficients[1]*CommonUtils.objectToDouble(record.get(index))*CommonUtils.objectToDouble(record.get(index));
		} catch (Exception e) {
			return 0.0;
		}
		/*
		Instance iExample = new Instance(3);
		iExample.setDataset(isTrainingSet);		
		iExample.setValue(
				(weka.core.Attribute) fvWekaAttributes.elementAt(0), 
				Double.parseDouble(o.get(index).toString()));

		iExample.setValue(
				(weka.core.Attribute) fvWekaAttributes.elementAt(1), 
				Math.pow(Double.parseDouble(o.get(index).toString()),2));
		
		double[] fDistribution;
		try {
			fDistribution = lg.distributionForInstance(iExample);		
		} catch (Exception e) {
			return 0.0;
		}
		double res=0.0;
		for(int i=0;i<fDistribution.length;i++)
			res+=fDistribution[i];
		return res;*/
	}
	

	public int compare(List<Object> arg0, List<Object> arg1) {
		if(normalize(arg0)>normalize(arg1))
			return 1;
		else if(normalize(arg0)<normalize(arg1))
			return -1;
		else
			return 0;
	}
	private void printMaxMin(Attribute attr){
		Double max= null,min= null;
		
		for(AttributeValue val : attr.getValues()){
			double d = CommonUtils.objectToDouble(val.getValue());
			if(max == null)
				max = d;
			if(min == null)
				min = d;
			if(d<min)
				min = d;
			if(d>max)
				max = d;
		}
		//System.out.println("Max="+max+", min"+min);
	}
	
	public void init(Attribute attr) {
		index = attr.getIndex();
		if(index>2)
		printMaxMin(attr);
		this.attr=attr;
		fvWekaAttributes = new FastVector(3);
		fvWekaAttributes.addElement(new weka.core.Attribute("X"));
		fvWekaAttributes.addElement(new weka.core.Attribute("X^2"));
		fvWekaAttributes.addElement(new weka.core.Attribute("Rating"));

		isTrainingSet = new Instances("Rel", fvWekaAttributes, 10);
		isTrainingSet.setClassIndex(2);
		
		for(AttributeValue attrVal : attr.getValues()){
			for(Double r :attrVal.getRatings()){
				Instance iExample = new Instance(3);
				iExample.setDataset(isTrainingSet);				
				iExample.setValue(
						(weka.core.Attribute) fvWekaAttributes.elementAt(0), 
						CommonUtils.objectToDouble(attrVal.getValue()));
				iExample.setValue(
						(weka.core.Attribute) fvWekaAttributes.elementAt(1), 
						Math.pow(CommonUtils.objectToDouble(attrVal.getValue()),2));
				iExample.setValue(
						(weka.core.Attribute) fvWekaAttributes.elementAt(2), 
						r);
				isTrainingSet.add(iExample);
			}
		}
		computeRepresentants();		
	}

	public String toString(){
		return "Quad";
	}

	public Normalizer clone() {
		return new Quadratic();
	}

	public void configClassifier(XMLConfiguration config, String section) {
		
	}
}
