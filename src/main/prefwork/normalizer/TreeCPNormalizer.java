package prefwork.normalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import weka.classifiers.functions.LinearRegression;
import weka.core.Capabilities;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Capabilities.Capability;

public class TreeCPNormalizer implements Normalizer {

	protected Normalizer overallNorm;
	weka.classifiers.Classifier tree = new weka.classifiers.trees.BFTree();
	Instances isTrainingSet;
	FastVector fvWekaAttributes;
	int index;
	
	
	public String toString() {
			return "TreeCP" ;
	}

	

	/**
	 * Returns record that contains only one value.
	 * 
	 * @param o
	 * @return
	 */
	protected List<Object> getRecord(Object o) {
		List<Object> l = CommonUtils.getList(1);
		l.add(o);
		return l;
	}


	@Override
	public void configClassifier(XMLConfiguration config, String section) {
		Configuration dbConf = config.configurationAt(section);
	}

	/**
	 * Adds given value and rating to virualAttr.
	 * 
	 * @param o
	 * @param r
	 */
	protected void addToAttr(Attribute attr, Object o, Double r) {
		AttributeValue virtualAttrVal = attr.getValue(o);
		if (virtualAttrVal == null) {
			virtualAttrVal = new AttributeValue(attr, o);
			attr.addValue(virtualAttrVal);
		}
		virtualAttrVal.addRating(r);
	}

	public void init(Attribute attr) {
		index = attr.getIndex();
		fvWekaAttributes = new FastVector(3);

		FastVector vec = new FastVector();
		FastVector ratings = new FastVector();
		for (AttributeValue attrVal2 : attr.getValues()) {
			for (List<Object> rec : attrVal2.getRecords()) {
				for (int i = 0; i < rec.size(); i++) {
					if (i == index)
						continue;
					try {
						// We don't want to consider numerical domains
						CommonUtils.objectToDouble(rec.get(i));
					} catch (Exception e) {
						if(!vec.contains(i + "_" + rec.get(i)))
							vec.addElement(i + "_" + rec.get(i));
						if(!ratings.contains(rec.get(2).toString()))
							ratings.addElement(rec.get(2).toString());
					}
				}
			}
		}
		
		Capabilities cap = tree.getCapabilities();
		fvWekaAttributes.addElement(new weka.core.Attribute("X"));
		fvWekaAttributes.addElement(new weka.core.Attribute("X_2", vec));
		if(cap.getClassCapabilities().handles(Capability.NUMERIC_CLASS)){
			fvWekaAttributes.addElement(new weka.core.Attribute("Rating"));			
		}
		else{
			fvWekaAttributes.addElement(new weka.core.Attribute("Rating",ratings));				
		}

		isTrainingSet = new Instances("Rel", fvWekaAttributes, 10);
		isTrainingSet.setClassIndex(2);

		//List<Double> values = CommonUtils.getList();
		for (AttributeValue attrVal : attr.getValues()) {
			for (List<Object> rec : attrVal.getRecords()) {
				// We construct the map of pairs attribute value - list of all
				// objects containing this value
				for (int i = 0; i < rec.size(); i++) {
					if(i==index)
						continue;

					try {
						// We don't want to consider numerical domains
						CommonUtils.objectToDouble(rec.get(i));
					} catch (Exception e) {
						Double r = CommonUtils.objectToDouble(rec.get(2));
						Instance iExample = new Instance(3);
						iExample.setDataset(isTrainingSet);
						iExample.setValue((weka.core.Attribute) fvWekaAttributes
								.elementAt(0), CommonUtils.objectToDouble(rec.get(index)));
						// Normalization of lists - we decompose them to objects
						// in the list.
						if (rec.get(i) instanceof List) {
							List l = (List) rec.get(i);
							for (Object o2 : l) {
								iExample.setValue((weka.core.Attribute) fvWekaAttributes
										.elementAt(1), i + "_" + o2.toString());
								iExample.setValue((weka.core.Attribute) fvWekaAttributes
										.elementAt(2), r);
								isTrainingSet.add(iExample);
							}
						}
						// Otherwise we add the value
						else{
							iExample.setValue((weka.core.Attribute) fvWekaAttributes
									.elementAt(1), i + "_" + rec.get(i).toString());
							iExample.setValue((weka.core.Attribute) fvWekaAttributes
									.elementAt(2), r);
							isTrainingSet.add(iExample);
						}
					}
					
				}
			}
		}
		try {
			tree.buildClassifier(isTrainingSet);
		} catch (Exception e) {
			e.printStackTrace();
			tree = null;
		}
	}

	public Normalizer clone() {
		TreeCPNormalizer norm = new TreeCPNormalizer();
		return norm;
	}

	@Override
	public double compareTo(Normalizer n) {
		// TODO Auto-generated method stub
		return 0;
	}



	@Override
	public Double normalize(List<Object> record) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public int compare(List<Object> arg0, List<Object> arg1) {
		// TODO Auto-generated method stub
		return 0;
	}
}
