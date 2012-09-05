package prefwork.normalizer;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import weka.classifiers.functions.LinearRegression;
import weka.clusterers.SimpleKMeans;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class Linear implements Normalizer {

	weka.classifiers.functions.LinearRegression lg = new LinearRegression();
	//weka.classifiers.functions.SMOreg lg = new weka.classifiers.functions.SMOreg();
	Instances isTrainingSet;
	FastVector fvWekaAttributes;
	Attribute attr;
	Instances representants;
	double[] coefficients;
	public double[] getCoefficients() {
		return coefficients;
	}
	public void setCoefficients(double[] coefficients) {
		this.coefficients=coefficients;
	}

	int numberOfClusters = 0;
	boolean varNumberOfClusters = false;
	int index;
	
	public String toString() {
		return "LCoef"+numberOfClusters+varNumberOfClusters/*+lg.getClass().getSimpleName()*/;
	}
	
	public Linear() {
	}

	public Linear(Attribute attr) {
		init(attr);
	}

	protected void computeRepresentants() {
		SimpleKMeans cluster = new SimpleKMeans();
		try {
			if(numberOfClusters != 0){
				cluster.setNumClusters(numberOfClusters);
				isTrainingSet.setClassIndex(-1);
				cluster.buildClusterer(isTrainingSet);
				representants = cluster.getClusterCentroids();
				representants.setClassIndex(1);
				lg.buildClassifier(representants);
			}
			else
				lg.buildClassifier(isTrainingSet);
			coefficients = lg.coefficients();
			/*if(representants.numInstances()<5)
				lg.setSampleSize(3);*/
			
		} catch (weka.core.WekaException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Double normalize(List<Object> record) {
		/*return 0.0;
		Instance iExample = new Instance(2);
		iExample.setDataset(isTrainingSet);
		iExample.setValue((weka.core.Attribute) fvWekaAttributes
				.elementAt(0), CommonUtils.objectToDouble(record.get(index)));*/

		try {
			//return lg.classifyInstance(iExample);
			return coefficients[2]+coefficients[0]*CommonUtils.objectToDouble(record.get(index));
		} catch (Exception e) {
			return null;
		}/*CommonUtils.objectToDouble(record.get(index))*/
	}

	public void computeCoefs(List<Double> l, List<Double> ratings) {

	}

	public int compare(List<Object> arg0, List<Object> arg1) {
		if (normalize(arg0) > normalize(arg1))
			return 1;
		else if (normalize(arg0) < normalize(arg1))
			return -1;
		else
			return 0;
	}

	public void init(Attribute attr) {
		coefficients = null;
		lg = new LinearRegression();
		index = attr.getIndex();
		fvWekaAttributes = new FastVector(2);
		fvWekaAttributes.addElement(new weka.core.Attribute("X"));
		fvWekaAttributes.addElement(new weka.core.Attribute("Rating"));

		isTrainingSet = new Instances("Rel", fvWekaAttributes, 10);
		isTrainingSet.setClassIndex(1);

		for (AttributeValue attrVal : attr.getValues()) {
			for (Double r : attrVal.getRatings()) {
				try {
					Instance iExample = new Instance(2);
					iExample.setDataset(isTrainingSet);
					iExample.setValue((weka.core.Attribute) fvWekaAttributes
							.elementAt(0), CommonUtils.objectToDouble(attrVal
							.getValue()));
					iExample.setValue((weka.core.Attribute) fvWekaAttributes
							.elementAt(1), r);
					isTrainingSet.add(iExample);
				} catch (NumberFormatException e) {
					// Do nothing
				}
			}

		}
		if(varNumberOfClusters == true)
			numberOfClusters = Math.max(3, attr.getValues().size()/2);
		computeRepresentants();
	}

	public Normalizer clone() {
		Linear l = new Linear();
		l.numberOfClusters = numberOfClusters;
		l.varNumberOfClusters = varNumberOfClusters;
		return l;
	}

	public void configClassifier(XMLConfiguration config, String section) {
		numberOfClusters = CommonUtils.getIntFromConfIfNotNull(config.configurationAt(section), "numberOfClusters", numberOfClusters);
		varNumberOfClusters = CommonUtils.getBooleanFromConfIfNotNull(config.configurationAt(section), "varNumberOfClusters", varNumberOfClusters);
	}

	@Override
	public double compareTo(Normalizer n) {
		if(n == null || !(n instanceof Linear))
			return 0;
		Linear n2 = (Linear)n;
		
		double dist = 0;
		int i = 0;
		if(coefficients == null || n2.coefficients == null)
			return 0;
		
		double max=Double.MIN_VALUE;
		for (; i < coefficients.length && i < n2.coefficients.length; i++) {
			dist += Math.abs(coefficients[i] - n2.coefficients[i]);
			if(Math.abs(coefficients[i])>max)
				max =  Math.abs(coefficients[i]);
			if(Math.abs(n2.coefficients[i])>max)
				max = Math.abs(n2.coefficients[i]);
		}
		return 1.0-(dist/(i*max));
	}


}
