package prefwork.normalizer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mappings.AverageValues;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import prefwork.representant.AvgRepresentant;
import prefwork.representant.Representant;

public class THSetNormalizer implements Normalizer{

	Set<String> realValues[];
	Double  ratings[], mappedValues[];

	public String toString(){
		return "THSet";
	}
	
	@Override
	public int compare(List<Object> arg0, List<Object> arg1) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Normalizer clone(){
		THSetNormalizer thO = new THSetNormalizer();
		return thO;
	}
	
	@Override
	public Double normalize(List<Object> record) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(Attribute attribute) {
		List<AttributeValue> val = attribute.getValues();
		int count = 0;
		for (int i = 0; i < val.size(); i++) {
			count+=val.get(i).getRecords().size();
		}
		realValues = new Set[count];
		ratings = new Double[count];
		mappedValues = new Double[count];
		AverageValues<Set<String>> averageValues = new AverageValues<Set<String>>();
		averageValues.compute(realValues, ratings);
		mapValues(averageValues);
		
	}

	// transfer average values to the mappedValues list (i.e. ordering values)
	public void mapValues(AverageValues<Set<String>> averageValues) {
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
