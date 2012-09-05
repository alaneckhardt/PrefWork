package prefwork.normalizer;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class PeakImproveHigher extends Peak {

	int addCount = 1;

	public String toString() {
		return "PeakImp"+addCount+lg1.getClass().getSimpleName();
	}

	public PeakImproveHigher() {
	}

	public PeakImproveHigher(Attribute attr) {
		init(attr);
	}

	protected void duplicateHigherRatings(){
		int count = isTrainingSet.numInstances();
		for (int i = 0; i < count; i++) {
			Instance iExample = isTrainingSet.instance(i);
			if(iExample.value(1)>=4){
				for(int j=0;j<addCount;j++)
					isTrainingSet.add(iExample);
			}
		}
		count = isTrainingSet1.numInstances();
		for (int i = 0; i < count; i++) {
			Instance iExample = isTrainingSet1.instance(i);
			if(iExample.value(1)>=4){
				for(int j=0;j<addCount;j++)
					isTrainingSet1.add(iExample);
			}
		}
	}


	public void init(Attribute attr) {
		index = attr.getIndex();
		this.attr = attr;
		fvWekaAttributes = new FastVector(2);
		fvWekaAttributes.addElement(new weka.core.Attribute("X"));
		fvWekaAttributes.addElement(new weka.core.Attribute("Rating"));

		isTrainingSet = new Instances("Rel", fvWekaAttributes, 10);
		isTrainingSet.setClassIndex(1);
		isTrainingSet1 = new Instances("Rel", fvWekaAttributes, 10);
		isTrainingSet1.setClassIndex(1);
		isTrainingSet2 = new Instances("Rel", fvWekaAttributes, 10);
		isTrainingSet2.setClassIndex(1);

		for (AttributeValue attrVal : attr.getValues()) {
			for (Double r : attrVal.getRatings()) {
				try {
					Instance iExample = new Instance(2);
					iExample.setDataset(isTrainingSet1);
					iExample.setValue((weka.core.Attribute) fvWekaAttributes
							.elementAt(0), CommonUtils.objectToDouble(attrVal
							.getValue()));
					iExample.setValue((weka.core.Attribute) fvWekaAttributes
							.elementAt(1), r);
					isTrainingSet1.add(iExample);
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
		PeakImproveHigher peak = new PeakImproveHigher();
		peak.addCount = addCount;
		return peak;
	}

	public void configClassifier(XMLConfiguration config, String section) {
		addCount = CommonUtils.getIntFromConfIfNotNull(config.configurationAt(section), "addCount", addCount);
	}
}
