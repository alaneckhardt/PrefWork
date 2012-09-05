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

/**
 * This class does simple linear regression on given data. 
 * It first duplicates higher ratings and then does the linear regression.
 * This should improve the accuracy for higher ratings and thus improve the measures as WRMSE and ZeroedTau.
 * @author Alan
 *
 */
public class LinearImproveHigher implements Normalizer {

	//weka.classifiers.functions.LinearRegression lg = new LinearRegression();
	weka.classifiers.functions.LinearRegression lg = new LinearRegression();
	//MultilayerPerceptron lg = new MultilayerPerceptron();
	Instances isTrainingSet;
	FastVector fvWekaAttributes;
	Attribute attr;
	Instances representants;
	int numberOfClusters = 0;
	//How many times the good objects are added to the training set.
	int addCount = 3;
	double threshold = 3;
	int index;
	
	public String toString() {
		return "LImp"+addCount;
	}
	
	public LinearImproveHigher() {
	}

	public LinearImproveHigher(Attribute attr) {
		init(attr);
	}

	protected void duplicateHigherRatings(){
		int count = isTrainingSet.numInstances();
		for (int i = 0; i < count; i++) {
			Instance iExample = isTrainingSet.instance(i);
			if(iExample.value(1)>=threshold){
				for(int j=0;j<addCount;j++)
					isTrainingSet.add(iExample);
			}
		}
	}
	
	private void computeRepresentants() {
		SimpleKMeans cluster = new SimpleKMeans();
		try {
			if(numberOfClusters != 0){
			cluster.setNumClusters(numberOfClusters);
			cluster.buildClusterer(isTrainingSet);
			representants = cluster.getClusterCentroids();
			representants.setClassIndex(1);
			lg.buildClassifier(representants);
			}
			else
				lg.buildClassifier(isTrainingSet);
			
			/*if(representants.numInstances()<5)
				lg.setSampleSize(3);*/
			
		}  catch (weka.core.WekaException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Double normalize(List<Object> record) {
		Instance iExample = new Instance(2);
		iExample.setDataset(isTrainingSet);
		try {
			iExample.setValue((weka.core.Attribute) fvWekaAttributes
					.elementAt(0), CommonUtils.objectToDouble(record.get(index)));
			double[] fDistribution;
			fDistribution = lg.distributionForInstance(iExample);
			double res = 0.0;
			for (int i = 0; i < fDistribution.length; i++)
				res += fDistribution[i];
			return res;

		} catch (Exception e) {
			return 0.0;
		}
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
		index = attr.getIndex();
		this.attr = attr;
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
		duplicateHigherRatings();
		computeRepresentants();
	}

	public Normalizer clone() {
		LinearImproveHigher linear = new LinearImproveHigher();
		linear.addCount = addCount;
		return linear;
	}

	public void configClassifier(XMLConfiguration config, String section) {
		addCount = CommonUtils.getIntFromConfIfNotNull(config.configurationAt(section), "addCount", addCount);
		threshold= CommonUtils.getDoubleFromConfIfNotNull(config.configurationAt(section), "threshold", threshold);
	}

	@Override
	public double compareTo(Normalizer n) {
		// TODO Auto-generated method stub
		return 0;
	}


}

