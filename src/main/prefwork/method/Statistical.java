package prefwork.method;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;
import prefwork.rater.Rater;
import prefwork.rater.WeightAverage;
import prefwork.representant.Representant;

public class Statistical extends ContentBased {
	Long normalizeNumTime = 0L;
	Long normalizeNomTime = 0L;
	Long raterTime = 0L;
	public Statistical() {
		rater = new WeightAverage();
	}
	public Statistical clone(){
		Statistical st = new Statistical();
		if(this.attributes!= null){
			st.attributes = new Attribute[this.attributes.length];
			for (int i = 0; i < st.attributes.length; i++) {
				st.attributes[i]=this.attributes[i].clone();
			}
		}
		st.colorNorm = this.colorNorm.clone();

		// Normalizer for nominal attributes
		st.nominalNormName = this.nominalNormName;
		st.nominalNorm = this.nominalNorm.clone();

		// Normalizer for numerical attributes
		st.numericalNormName = this.numericalNormName;
		st.numericalNorm = this.numericalNorm.clone();

		// Normalizer for list attributes
		st.listNormName = this.listNormName;
		st.listNorm = this.listNorm.clone();

		// Normalizer for nominal attributes
		st.colorNormName = this.colorNormName;
		 st.colorNorm = this.colorNorm.clone();

		 st.reprName = this.reprName;
		 st.representant = this.representant;

		 st.raterName = this.raterName;
		 st.rater = this.rater;
		
		return st;
		
	}

	/**
	 * Method for initialization of Statistical method.
	 */
	private void clear() {
		if(attributes == null)
			return;
		for(Attribute attr:attributes){
			attr.setValues(null);
			attr.setVariance(0.0);
		}
	}

	/**
	 * Method builds the model for evaluation. For nominal attributes, it is
	 * based on statistical distribution of ratings of objects with given
	 * attribute values. For numerical, linear or other regression is used.<br />
	 * 
	 * @param trainingDataset
	 *            The dataset with training data.
	 * @param splitValue
	 *            User id.
	 */
	public int buildModel(BasicDataSource trainingDataset, Integer splitValue) {
		clear();
		trainingDataset.setFixedUserId(splitValue);
		trainingDataset.restart();
		int target = trainingDataset.getTargetAttribute();
		if (!trainingDataset.hasNextRecord())
			return 0;
		loadAttributes(trainingDataset);

		List<Object> rec;
		int count = 0;
		while ((rec = trainingDataset.getRecord()) != null) {
			// record[0] - uzivatelID
			// record[1] - itemID
			// record[2] - rating
			for (int i = 0; i < attributes.length; i++) {
				addAttributeValue(i, rec, CommonUtils.objectToDouble(rec.get(
						target)));
			}
			count++;
		}
		computeRatings();

		rater.init(attributes);
		for (int i = 0; i < attributes.length; i++) {
			for (AttributeValue val : attributes[i].getValues()) {
				CommonUtils.cleanAttributeValue(val);
			}
		}
		System.gc();
		// ((WeightRater) rater).setWeights(weights);
		return count;
	}

	/**
	 * Computes the rating of given object. It is done in two steps - first
	 * attribute values are normalized using local preferences. Then, local
	 * preferences are aggregated using Rater to overall score of object.
	 * 
	 * @param record
	 *            The record to be classified.
	 * @param targetAttribute
	 *            Index, where rating is stored.
	 */
	public Double classifyRecord(List<Object> record, Integer targetAttribute) {
		if(attributes == null)
			return 0.0;
		// Ratings are the preferences over attribute values.
 		Double[] ratings = new Double[record.size()];
		AttributeValue[] object = new AttributeValue[ratings.length];
		for (int i = 0; i < ratings.length; i++) {
			AttributeValue val = attributes[i].getValue(record.get(i));
			object[i] = val;
			if (val == null
					&& (attributes[i] == null
							|| attributes[i].getValues() == null
							|| attributes[i].getNorm() == null ))
				ratings[i] = null;
			else{
				Long start = System.currentTimeMillis();
				ratings[i] = attributes[i].getNorm().normalize(record);
				Long end = System.currentTimeMillis();
				if(attributes[i].getType()==Attribute.NOMINAL)
					normalizeNomTime+= end-start; 
				if(attributes[i].getType()==Attribute.NUMERICAL)
					normalizeNumTime+= end-start; 
			}
		}
		// Final aggregation
		Double res=rater.getRating(ratings, object);
		return res;
	}

	public void configClassifier(XMLConfiguration config, String section) {
		super.configClassifier(config,section);

	}

	public Representant getRepresentant() {
		return representant;
	}

	public void setRepresentant(Representant representant) {
		this.representant = representant;
	}

	public Rater getRater() {
		return rater;
	}

	public void setRater(Rater rater) {
		this.rater = rater;
	}

	public String toString() {
		return "Statistical3," + rater.toString() + ","
				+ representant.toString() + "," +textNorm.toString()+ numericalNorm.toString()
				+ "," + nominalNorm.toString()
				+ "," + listNorm.toString();
	}

}
