package prefwork.normalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;

public class Standard2CPNormalizer implements Normalizer {

	protected Normalizer overallNorm;
	/**Map of attribute values and object with that attribute value*/
	protected HashMap<Object, List<List<Object>>> map = new HashMap<Object, List<List<Object>>>();
	protected int index;
	protected Attribute attr;
	protected Attribute virtualAttr;
	protected String normName;
	/**Map of (attribute value+";"+index of the attribute of that value) and normalizer for the attribute <i>index</i> */
	protected HashMap<Object, Normalizer> mapNorm = new HashMap<Object, Normalizer>();
	protected double mean = 0;
	int targetAttribute = 0;

	public String toString() {
		if(overallNorm==null)
			return "2CP" ;
		else
			return "2CP" +overallNorm.toString();
	}

	protected Normalizer getNorm() {
		if(normName!=null)
			return (Normalizer) CommonUtils.getInstance(normName);
		else
			return new Linear();
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

	/**
	 * Returns rating of a list of values. It averages ratings of values inside
	 * the list.
	 * 
	 * @param map
	 * @param value
	 * @return
	 */
	protected double getListRating(Normalizer norm, List<Object> value) {
		Double ratings = 0.0;
		int count = 0;
		for (Object o : value) {
			double r = norm.normalize(getRecord(o));
			if (r != 0) {
				ratings += r;
				count++;
			}
		}
		// There is no record in normalizer for any object from the given list
		if (count == 0) {
			for (Object o : value) {
				double r = overallNorm.normalize(getRecord(o));
				if (r != 0) {
					ratings += r;
					count++;
				}
			}
		}
		// There is no record in overall normalizer for any object from the
		// given list either
		if (count == 0)
			return mean;

		return ratings / count;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Double normalize(List<Object> record) {
		for (Object o : record) {
			if (mapNorm.containsKey(o + ";" + record.indexOf(o))) {
				Normalizer norm = mapNorm.get(o + ";" + record.indexOf(o));
				if(record.get(index) == null)
					continue;
				if (record.get(index) instanceof List) {
					return getListRating(norm, (List) record.get(index));
				}
				double r = norm.normalize(getRecord(record.get(index)));
				if (r != 0.0)
					return r;
				else
					return overallNorm.normalize(getRecord(record.get(index)));
			}
		}
		return overallNorm.normalize(getRecord(record.get(index)));
	}

	@Override
	public void configClassifier(XMLConfiguration config, String section) {
		Configuration dbConf = config.configurationAt(section);
		if(dbConf.containsKey("innerNormalizer")){
				normName = dbConf.getString("innerNormalizer");
				overallNorm = getNorm();
		}
	}

	private void addValueToMap(Object o, int index, List<Object> rec) {
		if (!map.containsKey(o + ";" + index)) {
			map.put(o + ";" + index, CommonUtils.getListList());
		}
		List<List<Object>> l = map.get(o + ";" + index);
		l.add(rec);
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

	/**
	 * Builds a classifier for values too few. 
	 */
	@SuppressWarnings("unchecked")
	protected void buildGeneralClassifier() {
		// This is the way to access all objects in training set -
		// 1) Examine every attribute value of attribute attr
		// 2) Examine all objects that have this attribute value
		virtualAttr = new Attribute(null, 0, "Virtual");
		virtualAttr.setType(attr.getType());
		for (AttributeValue attrVal : attr.getValues()) {
			// All objects that have this attribute value are processed.
			for (List<Object> rec : attrVal.getRecords()) {
				// We construct the map of pairs attribute value - list of all
				// objects containing this value
				for (Object o : rec) {
					try {
						// We don't want to consider numerical domains
						CommonUtils.objectToDouble(o);
					} catch (Exception e) {
						// Normalization of lists - we decompose them to objects
						// in the list.
						if (o instanceof List) {
							List l = (List) o;
							int index = rec.indexOf(o);
							for (Object o2 : l) {
								addValueToMap(o2, index, rec);
							}
						}
						// Otherwise we add the value
						else
							addValueToMap(o, rec.indexOf(o), rec);
					}
				}/*
				for (Object o : rec) {
					// Normalization of lists - we decompose them to objects
					// in the list.
					if (o instanceof List) {
						List l = (List) o;
						int index = rec.indexOf(o);
						for (Object o2 : l) {
							addValueToMap(o2, index, rec);
						}
					}
					// Otherwise we add the value
					else
						addValueToMap(o, rec.indexOf(o), rec);
				}*/
			}
			// We add all ratings into virtual attribute.
			// This is because we want all normalizers to work on single values,
			// not whole records.
			if(attrVal.getRatings() == null)
				continue;
			for (Double r : attrVal.getRatings()) {
				// If it is list, we add all values in the list into the virtual
				// attribute
				if (attrVal.getValue() instanceof List) {
					List l2 = (List) attrVal.getValue();
					for (Object o2 : l2) {

						addToAttr(virtualAttr, o2, r);
					}
				}
				// Otherwise we add the value
				addToAttr(virtualAttr, attrVal.getValue(), r);
			}
		}
		if(overallNorm == null)
			overallNorm = getNorm();
		overallNorm.init(virtualAttr);
	}

	@SuppressWarnings("unused")
	private void addValueToMap(Object oClass,
			HashMap<Object, List<Double>> mapClass, Double rating) {
		if (!mapClass.containsKey(oClass)) {
			mapClass.put(oClass, new ArrayList<Double>());
		}
		List<Double> lClass = mapClass.get(oClass);
		lClass.add(rating);
	}

	/**
	 * Builds classifier for values that are more common - at least 2 of them is in the training set.
	 */
	protected void buildSpecificClassifiers() {
		for (Object o : map.keySet()) {
			List<List<Object>> l = map.get(o);
			if (l.size() < 2)
				continue;

			Attribute virtualObjectAttr = new Attribute(null, 0,
					"Virtual for object o");
			virtualObjectAttr.setType(attr.getType());
			// We construct normalizer for new attribute
			for (List<Object> rec : l) {
				Object val = rec.get(index);
				if(val == null)
					continue;
				Double r = CommonUtils.objectToDouble(rec.get(targetAttribute));
				addToAttr(virtualObjectAttr, val, r);

			}
			try {
				Normalizer norm = getNorm();
				norm.init(virtualObjectAttr);
				mapNorm.put(o, norm);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public int getTargetAttribute() {
		return targetAttribute;
	}

	public void setTargetAttribute(int targetAttribute) {
		this.targetAttribute = targetAttribute;
	}

	@Override
	public void init(Attribute attribute) {
		index = attribute.getIndex();
		attr = attribute;
		buildGeneralClassifier();
		buildSpecificClassifiers();
	}

	@Override
	public int compare(List<Object> arg0, List<Object> arg1) {
		double a = normalize(arg0);
		double b = normalize(arg1);
		if (a < b)
			return -1;
		if (a > b)
			return 1;
		return 0;
	}

	public Normalizer clone() {
		Standard2CPNormalizer norm = new Standard2CPNormalizer();
		norm.normName = this.normName;
		norm.overallNorm = getNorm();
		norm.attr = this.attr;
		norm.index = this.index;
		norm.targetAttribute = this.targetAttribute;
		return norm;
	}

	@Override
	public double compareTo(Normalizer n) {
		// TODO Auto-generated method stub
		return 0;
	}
}
