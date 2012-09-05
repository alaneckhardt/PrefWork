package prefwork.method;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;
import prefwork.rater.InstancesRater;

/**
 * This class computes ratings based on the position of the record regarding the
 * training set. Objects from training set are used to evaluate new objects. For
 * that, local preferences have to be computed.
 * 
 * @author Alan
 */
public class Instances extends ContentBased {

	List<List<Object>> instances = CommonUtils.getList();

	String raterName = "prefwork.rater.InstancesRater";

	// Maximal size of instances
	int size = 100;

	public String toString() {
		return "Instances" + size + rater.toString()+textNorm.toString()+numericalNorm.toString()+nominalNorm.toString()+listNorm.toString()+ representant.toString();
	}


	private void clear() {
		//rater = (InstancesRater) CommonUtils.getInstance(raterName);
		instances = CommonUtils.getList();
		if(attributes == null)
			return;
		for(Attribute attr:attributes){
			attr.setValues(null);
			attr.setVariance(0.0);
		}
	}

	/**
	 * Builds model for specified user. The model is represented by training
	 * data.
	 * 
	 * @param trainingDataset
	 *            The dataset with training data.
	 * @param splitValue
	 *            User id.
	 */
	public int buildModel(BasicDataSource trainingDataset, Integer user) {
		clear();
		trainingDataset.setFixedUserId(user);
		trainingDataset.restart();
		int target = trainingDataset.getTargetAttribute();
		//loadAttributes(trainingDataset);

		List<Object> rec;
		int count = 0;
		while ((rec = trainingDataset.getRecord()) != null) {
			// record[0] - uzivatelID
			// record[1] - itemID
			// record[2] - rating
			List<Object> newRecord = CommonUtils.getList(attributes.length);
			for (int i = 0; i < attributes.length; i++) {
				addAttributeValue(i, rec, CommonUtils.objectToDouble(rec.get(
						target)));
				newRecord.add( attributes[i].getValue(rec.get(i)));
			}
			if (instances.size() == size) {

				Collections.sort(instances, new RatingComparator());
				if (CommonUtils.objectToDouble(newRecord.get(2)) >= CommonUtils.objectToDouble(instances.get(size - 1).get(2))) {
					instances.remove(size - 1);
					instances.add(rec);
				}
			} else
				instances.add(rec);
			count++;
		}
		computeRatings();
		rater.init(attributes);
		((InstancesRater)rater).setInstances(instances);
		return count;
	}

	/**
	 * 
	 */
	public Double classifyRecord(List<Object> record, Integer targetAttribute) {
		if(record == null)
			return 0.0;
				
		((InstancesRater)rater).setTargetAttribute(targetAttribute);
		return ((InstancesRater)rater).getRating(record);
	}

	public void configClassifier(XMLConfiguration config, String section) {
		super.configClassifier(config,section);
		Configuration methodConf = config.configurationAt(section);
		if (methodConf.containsKey("size")) {
			size = methodConf.getInt("size");
		}
	}

}

class RatingComparator implements Comparator<List<Object>> {

	public int compare(List<Object> arg0, List<Object> arg1) {
		if (arg0 == null && arg1 == null)
			return 0;
		if (arg0 == null)
			return 1;
		if (arg1 == null)
			return -1;
		if (CommonUtils.objectToDouble(arg0.get(2)) >= CommonUtils.objectToDouble(arg1.get(2)))
			return -1;
		else if (CommonUtils.objectToDouble(arg0.get(2)) >= CommonUtils.objectToDouble(arg1.get(2)))
			return 1;
		return 0;

	}

}