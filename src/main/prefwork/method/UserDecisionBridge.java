package prefwork.method;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.userdecision.Model;
import org.userdecision.ModelProbability;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;

public class UserDecisionBridge implements InductiveMethod {

	/**Model for user decision.*/
	Model model;
	Attribute[] attributes;
	Integer targetAttribute;
	String[] attributeNames;

	public String toString() {
		return model.toString();
	}


	protected void getAttributes(BasicDataSource trainingDataset, Integer user) {
		trainingDataset.setFixedUserId(user);
		trainingDataset.restart();
		attributes = trainingDataset.getAttributes();
		attributeNames = trainingDataset.getAttributesNames();
		List<Object> rec;
			while ((rec = trainingDataset.getRecord()) != null) {
				//TODO - inicializace metody pro diskretizaci.
			}
	}

	@SuppressWarnings("unchecked")
	protected void clear(){
		attributes = null;
		attributeNames = null;
		
	}	
	/**
	 * Transforms the record from List<Object> to HashMap<String,String>. Main work is to discretize the numerical attributes.
	 * @param rec
	 * @return
	 */
	HashMap<String,String> transform(List<Object> rec){
		HashMap<String,String> rec2 = new HashMap<String,String>(attributes.length);
		for (int i = 0; i < attributes.length; i++) {			
			if(attributes[i].getType() == Attribute.NUMERICAL){
				//TODO - diskretizace
				rec.get(i);
			}
			else if(attributes[i].getType() == Attribute.NOMINAL){
				//TODO - prevest do mapy.
				rec.get(i);
			}
		}
		return rec2;
	}

	private void initDiscretization() {
		for (int i = 0; i < attributes.length; i++) {
			if (attributes[i].getType() != Attribute.NUMERICAL) {
				continue;
			}
			List<Double> values = CommonUtils.getList();
			for (AttributeValue val : attributes[i].getValues()) {
				values.add(CommonUtils.objectToDouble(val.getValue()));
			}
			// TODO - nejak inicializovat diskretizaci. Ve values jsou vsechny hodnoty atributu.
		}

	}
	/**
	 * Initializes the model with attribute values.
	 * Numerical attributes are discretized.
	 */
	private void initModel(){
		//TODO - pouzit ModelProbability nebo neco jineho? Pripadne nastavit nezbytne veci v nem.		
		model = new ModelProbability();
		HashMap<String, ArrayList<String>> attrs = new HashMap<String, ArrayList<String>>();
		initDiscretization();
		for (int i = 0; i < attributes.length; i++) {
			ArrayList<String> attrValues = new ArrayList<String>();
			for (AttributeValue val : attributes[i].getValues()) {				
				if(attributes[i].getType() == Attribute.NUMERICAL){
					//TODO - diskretizace hodnoty val.getValue()
					attrValues.add(val.getValue().toString());
				}
				else if(attributes[i].getType() == Attribute.NOMINAL){
					attrValues.add(val.getValue().toString());
				}
			}
			attrs.put(attributeNames[i], attrValues);
		}
		model.create(attrs);
	}

	public int buildModel(BasicDataSource trainingDataset, Integer user) {
		clear();
		getAttributes(trainingDataset, user);
		initModel();
		
		
		trainingDataset.setFixedUserId(user);
		trainingDataset.restart();
		if (!trainingDataset.hasNextRecord())
			return 0;
		List<Object> rec;
		int count = 0;
		while ((rec = trainingDataset.getRecord()) != null) {
			//TODO - prevest rec na HashMap<String,String>
			HashMap<String,String> rec2 = transform(rec);
			BigDecimal rating = new BigDecimal(CommonUtils.objectToDouble(rec.get(2)));
			model.change(rec2, rating);
			count++;
		}
		return count;
	}

	public Double classifyRecord(List<Object> record, Integer targetAttribute) {
		try {
			Integer objectId = CommonUtils.objectToInteger(record.get(1));
			HashMap<String,String> rec2 = transform(record);
			HashMap<Object,HashMap<String,String>> recs = new HashMap<Object, HashMap<String,String>>();
			recs.put(objectId, rec2);
			HashMap<Object,BigDecimal> r = model.rateAllObject(recs);			
			return CommonUtils.objectToDouble(r.get(objectId));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public void configClassifier(XMLConfiguration config, String section) {
		Configuration methodConf = config.configurationAt(section);
	}

}
