package prefwork.method;

import prefwork.Attribute;
import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;
import prefwork.rater.WeightAverage;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

public class CombinedMethod implements InductiveMethod {

	CofiBridge cofi;
	Statistical statistical;
	DirectQuery directQuery;
	WeightAverage rater;
	double[] weights = new double[]{1,1,1};
	Attribute[] attributes;
	public CombinedMethod(){
		directQuery = new DirectQuery();
		cofi = new CofiBridge();
		statistical = new Statistical();
		rater = new WeightAverage();
		rater.setWeights(weights);
	}

	public String toString() {
		String out = "CombinedMethod2";
		for(double d : weights)
			out += ""+Double.toString(d).replace(",", ".")+",";
		return out;
	}
	
	public int buildModel(BasicDataSource trainingDataset, Integer user) {
		attributes = trainingDataset.getAttributes();
		directQuery.loadOriginalRatings(user);
		int count= cofi.buildModel(trainingDataset, user);
		 count += statistical.buildModel(trainingDataset, user);
		return count;
	}

	
	@Override
	public Double classifyRecord(List<Object> record, Integer targetAttribute) {
		Double r1 = statistical.classifyRecord(record, targetAttribute);
		Double r2 = cofi.classifyRecord(record, targetAttribute);
		Double r3 = directQuery.getRating(CommonUtils.objectToInteger(record.get(1)))+0.00001;
		return rater.getRating(new Double[] {r1,r2,r3}, CommonUtils.getValuesFromList(record, attributes));
	}

	@Override
	public void configClassifier(XMLConfiguration config, String section) {
		if(config.configurationsAt(section + ".Statistical")!=null &&
				config.configurationsAt(section + ".Statistical").size() > 0)
			statistical.configClassifier(config, section + ".Statistical");
		
	}

}
