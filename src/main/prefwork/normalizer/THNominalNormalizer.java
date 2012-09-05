package prefwork.normalizer;

import java.util.List;

import mappings.AverageValues;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import prefwork.representant.AvgRepresentant;
import prefwork.representant.Representant;

public class THNominalNormalizer implements Normalizer{
	String realValues[];
	Double  ratings[], mappedValues[];
	int index;
	AverageValues<String> averageValues;
	@Override
	public String toString(){
		return "THNom";
	}
	
	public int compare(List<Object> arg0, List<Object> arg1) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public Normalizer clone(){
		THNominalNormalizer thO = new THNominalNormalizer();
		return thO;
	}

	@Override
	public Double normalize(List<Object> record) {
		if(record.get(index) == null)
			return null;
		return averageValues.getYValue(record.get(index).toString());
	}

	@Override
	public void init(Attribute attribute) {
		index = attribute.getIndex();
		List<AttributeValue> val = attribute.getValues();
		int count = 0;
		for (int i = 0; i < val.size(); i++) {
			count+=val.get(i).getRecords().size();
		}
		realValues = new String[count];
		ratings = new Double[count];
		mappedValues = new Double[count];

		count = 0;
		for (int i = 0; i < val.size(); i++) {
			List<List<Object>> recs = val.get(i).getRecords();
			String value = val.get(i).getValue().toString();
			for (int j = 0; j < recs.size(); j++) {
				realValues[count]= value;
				ratings[count] = CommonUtils.objectToDouble(recs.get(j).get(2));
				count++;
			}
		}
		
		averageValues = new AverageValues<String>();
		averageValues.compute(realValues, ratings);
		mapValues(averageValues);		
	}


	// transfer average values to the mappedValues list (i.e. ordering values)
	public void mapValues(AverageValues<String> averageValues) {
		for (int i=0; i<realValues.length; i++) {
			mappedValues[i] = averageValues.getYValue(realValues[i]);
		}
	}
	
	@Override
	public void configClassifier(XMLConfiguration config, String section) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double compareTo(Normalizer n) {
		// TODO Auto-generated method stub
		return 0;
	}

}
