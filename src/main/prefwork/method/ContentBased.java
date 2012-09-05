package prefwork.method;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;
import prefwork.normalizer.Normalizer;
import prefwork.rater.Rater;
import prefwork.representant.AvgRepresentant;
import prefwork.representant.Representant;

public abstract class ContentBased implements InductiveMethod {
	
	protected Integer userId;

	// Attributes, containing all attribute values.
	protected Attribute attributes[];

	// Normalizer for nominal attributes
	String nominalNormName = "prefwork.normalizer.RepresentantNormalizer";
	
	Normalizer nominalNorm;

	// Normalizer for numerical attributes
	String numericalNormName = "prefwork.normalizer.Linear";
	Normalizer numericalNorm;

	// Normalizer for list attributes
	String listNormName = "prefwork.normalizer.ListNormalizer";
	Normalizer listNorm;

	// Normalizer for nominal attributes
	String colorNormName = "prefwork.normalizer.ColorNormalizer";
	Normalizer colorNorm;
	
	// Normalizer for text attributes
	String textNormName = "prefwork.normalizer.TextNormalizer";
	Normalizer textNorm;
	

	String reprName = "prefwork.representant.AvgRepresentant";
	Representant representant = new AvgRepresentant();

	String raterName = "prefwork.rater.WeightAverage";
	Rater rater;

	public ContentBased(){

		//Testing null values

		if (numericalNorm == null)
			numericalNorm = (Normalizer) CommonUtils
					.getInstance(numericalNormName);

		if (nominalNorm == null)
			nominalNorm = (Normalizer) CommonUtils
					.getInstance(nominalNormName);

		if (listNorm == null)
			listNorm = (Normalizer) CommonUtils.getInstance(listNormName);

		if (colorNorm == null)
			colorNorm = (Normalizer) CommonUtils.getInstance(colorNormName);

		if (textNorm == null)
			textNorm = (Normalizer) CommonUtils.getInstance(textNormName);

		if (representant == null)
			representant = (Representant) CommonUtils.getInstance(reprName);

		if (rater == null)
			rater = (Rater) CommonUtils.getInstance(raterName);

	}
	/**
	 * Computes the local preferences. For each attribute, one normalizer is
	 * constructed. Then the variance is computed - it may be used by rater as
	 * weights.
	 */
	protected void computeRatings() {
		for (Attribute attr : attributes) {
			Normalizer norm = null;
			if (attr.getType() == Attribute.NOMINAL) {
				norm = nominalNorm.clone();
			} else if (attr.getType() == Attribute.NUMERICAL) {
				norm = numericalNorm.clone();
			} else if (attr.getType() == Attribute.LIST) {
				norm = listNorm.clone();
			} else if (attr.getType() == Attribute.COLOR) {
				norm = colorNorm.clone();
			}
			 else if (attr.getType() == Attribute.TEXT) {
				norm = textNorm.clone();
			}

			computeVariances(attr);
			norm.init(attr);
			attr.setNorm(norm);
			List<AttributeValue> l = attr.getValues();
			if (l == null)
				continue;
		}
	}

	/**
	 * Adds attribute value to this.attributes. If the value is already present,
	 * only the rating is added.
	 * 
	 * @param attribute
	 *            Index of attribute.
	 * @param value
	 *            Value to be inserted.
	 * @param rating
	 *            Rating of that value.
	 */
	protected void addAttributeValue(int attribute, List<Object> record,
			Double rating) {
		// TODO tohle s nullem chce vymyslet lepe. Do it better.
		if (record.get(attribute) == null) {
			return;
		}
		for (Attribute attr : attributes) {
			if (attr.getIndex() == attribute) {
				AttributeValue attrValue = attr.getValue(record.get(attribute));
				if (attrValue == null) {
					attrValue = new AttributeValue(attr, record.get(attribute));
					attr.addValue(attrValue);
				}
				attrValue.addRating(rating);
				attrValue.addRecord(record);
			}
		}
	}

	/**
	 * Initializes this.attributes with names and indexes.
	 * 
	 * @param trainingDataset
	 *            The dataset the attribute names are taken from.
	 */
	protected void loadAttributes(BasicDataSource trainingDataset) {
		/*
		 * String[] attributeNames = trainingDataset.getAttributesNames();
		 * attributes = new Attribute[attributeNames.length]; for (int i = 0; i
		 * < attributeNames.length; i++) { attributes[i] = new Attribute();
		 * attributes[i].setName(attributeNames[i]); attributes[i].setIndex(i);
		 * }
		 */

		    Attribute[] dAttr = trainingDataset.getAttributes();
			attributes = new Attribute[dAttr.length];
			for (int i = 0; i < attributes.length; i++) {
				attributes[i]=dAttr[i].clone();
			}
		
	}

	/**
	 * The weights of attributes are computed based on the variance of the
	 * attribute values ratings.
	 * 
	 * @return The weights of attributes.
	 */
	protected double[] getWeights() {
		double[] weights = new double[attributes.length];
		for (int i = 0; i < weights.length; i++)
			if (attributes[i] != null && attributes[i].getVariance() != null)
				weights[i] = 1 / attributes[i].getVariance();
			else
				weights[i] = 1.0;
		return weights;
	}

	/**
	 * Computes the variance of ratings of given attribute value.
	 * 
	 * @param attrValue
	 *            Value, for which the variance is computed.
	 */
	protected void computeVariance(AttributeValue attrValue) {

		List<Double> ratings = attrValue.getRatings();
		double avg = 0;
		// Computing the average rating
		for (Double r : ratings)
			avg += r;
		avg /= ratings.size();
		double diff = 0;
		// Computing the variance from average rating
		for (Double r : ratings)
			diff += Math.abs(avg - r);
		attrValue.setVariance(diff / ratings.size());
	}

	/**
	 * Compute the overall variance of given attribute.
	 * 
	 * @param attr
	 *            Attribute, for which the variance is computed.
	 */
	protected void computeVariances(Attribute attr) {
		double var = 0;
		for (AttributeValue val : attr.getValues()) {
			computeVariance(val);
			var += val.getVariance();
		}
		attr.setVariance(var / attr.getValues().size());
	}

	/**
	 * Configures the methods normalizers and rater
	 */
	public void configClassifier(XMLConfiguration config, String section) {

		try {
			Configuration methodConf = config.configurationAt(section);
			// Setting all normalizers
			if (methodConf.containsKey("numericalNormalizer")) {
				numericalNormName = methodConf.getString("numericalNormalizer");
				numericalNorm = (Normalizer) CommonUtils
						.getInstance(numericalNormName);
				numericalNorm.configClassifier(config, section
						+ ".numericalNormalizer");
			}

			if (methodConf.containsKey("nominalNormalizer")) {
				nominalNormName = methodConf.getString("nominalNormalizer");
				nominalNorm = (Normalizer) CommonUtils
						.getInstance(nominalNormName);
				nominalNorm.configClassifier(config, section
						+ ".nominalNormalizer");
			}

			if (methodConf.containsKey("listNormalizer")) {
				listNormName = methodConf.getString("listNormalizer");
				listNorm = (Normalizer) CommonUtils.getInstance(listNormName);
				listNorm.configClassifier(config, section + ".listNormalizer");
			}

			if (methodConf.containsKey("colorNormalizer")) {
				colorNormName = methodConf.getString("colorNormalizer");
				colorNorm = (Normalizer) CommonUtils.getInstance(colorNormName);
				colorNorm.configClassifier(config, section + ".colorNormalizer");
			}

			if (methodConf.containsKey("textNormalizer")) {
				textNormName = methodConf.getString("textNormalizer");
				textNorm = (Normalizer) CommonUtils.getInstance(textNormName);
				textNorm.configClassifier(config, section + ".textNormalizer");
			}

			if (methodConf.containsKey("representant.class")) {
				// Setting the representant
				reprName = methodConf.getString("representant.class");
				representant = (Representant) CommonUtils.getInstance(reprName);
				representant
						.configClassifier(config, section + ".representant");
			}

			if (methodConf.containsKey("rater.class")) {
				// Setting the rater
				raterName = CommonUtils.getFromConfIfNotNull(methodConf, "rater.class", raterName);
				//if (rater == null)
					rater = (Rater) CommonUtils.getInstance(raterName);
				rater.configClassifier(config, section + ".rater");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Normalizer[] getNormalizers(){
		Normalizer[] normalizers = new Normalizer[attributes.length];
		for (int i = 0; i < normalizers.length; i++) {
			normalizers[i]=attributes[i].getNorm();
		}
		return normalizers;
	}

	public Normalizer getNominalNorm() {
		return nominalNorm;
	}

	public void setNominalNorm(Normalizer nominalNorm) {
		this.nominalNorm = nominalNorm;
	}

	public Normalizer getNumericalNorm() {
		return numericalNorm;
	}

	public void setNumericalNorm(Normalizer numericalNorm) {
		this.numericalNorm = numericalNorm;
	}

	public Normalizer getListNorm() {
		return listNorm;
	}

	public void setListNorm(Normalizer listNorm) {
		this.listNorm = listNorm;
	}

	public Normalizer getColorNorm() {
		return colorNorm;
	}

	public void setColorNorm(Normalizer colorNorm) {
		this.colorNorm = colorNorm;
	}

	public Attribute[] getAttributes() {
		return attributes;
	}

	public void setAttributes(Attribute[] attributes) {
		this.attributes = attributes;
	}

	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
}
