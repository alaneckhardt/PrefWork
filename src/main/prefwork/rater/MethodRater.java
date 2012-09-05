package prefwork.rater;

import java.lang.reflect.Constructor;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;
import prefwork.datasource.CacheDataSource;
import prefwork.datasource.THDataSource;
import prefwork.method.InductiveMethod;
import weka.classifiers.Classifier;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class MethodRater implements Rater {

	// Create a weka classifier
	InductiveMethod method = null;
	String methodName = "";
	Integer targetAttribute = 2;
	Attribute[] attributes;
	FastVector fvWekaAttributes;
	Instances isTrainingSet;
	int userId = -1;
	double[] classes;
	public String toString(){
		
		return "MethodRater"+method.toString();
	}
	@SuppressWarnings("unchecked")
	@Override
	public void configClassifier(XMLConfiguration config, String section) {
		Configuration methodConf = config.configurationAt(section);
		try {
			methodName = CommonUtils.getFromConfIfNotNull(methodConf, "method", methodName);
			Class c = Class.forName(methodName);
			Constructor[] a = c.getConstructors();
			method = (InductiveMethod) a[0].newInstance();
			method.configClassifier(config, section+".method");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	public Double getRating(Double[] ratings, AttributeValue[] record) {
		if(method == null)
			return null;
		List<Object> rec = CommonUtils.getList(ratings.length+3);
		rec.add(1);
		rec.add("0");
		rec.add(0);
		for (int i = 3; i < ratings.length; i++) {
			rec.add(ratings[i]);
		}
		return method.classifyRecord(rec, targetAttribute);			
	}
	
	List<Object>[][] getUserRecords(Attribute[] attributes){
		Attribute classes = attributes[2];
		List<Object>[][] userRecords;
		List<List<Object>> recs = CommonUtils.getList(classes.getValues().size()*5);
		this.classes = new double[classes.getValues().size()];
		int i = 0;
		for (AttributeValue val : classes.getValues()) {
			if(userId == -1)
				userId = CommonUtils.objectToInteger(val.getRecords().get(0).get(0));
			for (List<Object> list : val.getRecords()) {
				List<Object> newList = CommonUtils.getList(list.size());
				newList.add(list.get(0));
				newList.add(list.get(1));
				newList.add(list.get(2));
				for (int j = 3; j < list.size(); j++) {
					newList.add(attributes[j].getNorm().normalize(list));
				}
				recs.add(newList);
			}			
			this.classes[i]=CommonUtils.objectToDouble(val.getValue());
			i++;
		}
		userRecords = new List[1][recs.size()];
		userRecords[0] = recs.toArray(userRecords[0]);
		return userRecords;
	}
	@Override	
	public void init(Attribute[] attributes) {
		this.attributes = attributes;
		THDataSource source = new THDataSource();
		Attribute[] newAttributes = new Attribute[attributes.length];
		for (int i = 0; i < newAttributes.length; i++) {
			newAttributes[i] = new Attribute(null, i, attributes[i].getName());
			newAttributes[i].setType(Attribute.NUMERICAL);
		}
		source.setAttributes(newAttributes);
		source.setUserRecords(getUserRecords(attributes));
		source.setFile("\\Test");
		source.setClasses(classes);
		method.buildModel(source, userId);
	}
	@Override
	public double compareTo(Rater n) {
		// TODO Auto-generated method stub
		return 0;
	}
}
