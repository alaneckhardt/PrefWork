package prefwork.normalizer;

import prefwork.Attribute;
import weka.core.Instance;

public class LinearWeighted extends Linear {

	public String toString() {
		return "LWR"+numberOfClusters+varNumberOfClusters;
	}
	
	public LinearWeighted() {
	}

	public LinearWeighted(Attribute attr) {
		init(attr);
	}

	public void init(Attribute attr) {
		super.init(attr);
		for (int i = 0; i < isTrainingSet.numInstances(); i++) {
			Instance iExample = isTrainingSet.instance(i);
			iExample.setWeight(iExample.classValue());
		}
		computeRepresentants();
	}

	public Normalizer clone() {
		LinearWeighted l = new LinearWeighted();
		l.numberOfClusters = numberOfClusters;
		l.varNumberOfClusters = varNumberOfClusters;
		return l;
	}


}
