package prefwork.normalizer;

import java.util.List;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
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
public class QuadraticImproveHigher extends Quadratic {

	//How many times the good objects are added to the training set.
	int addCount = 1;
	
	public String toString() {
		return "QuadImp";
	}
	
	public QuadraticImproveHigher() {
	}

	public QuadraticImproveHigher(Attribute attr) {
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
	}

	public void init(Attribute attr) {
		index = attr.getIndex();
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
		duplicateHigherRatings();
		computeRepresentants();
	}

	public Normalizer clone() {
		return new QuadraticImproveHigher();
	}
}

