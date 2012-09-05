package prefwork.normalizer;

import java.util.Arrays;
import java.util.List;

import mappings.Polynomial;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import prefwork.representant.AvgRepresentant;
import prefwork.representant.Representant;
import sun.font.AttributeValues;

public class THOrdinalNormalizer implements Normalizer{

	public Polynomial polynomial;	
	public double beta = 0.3;
	String[] mode = {"","2"};
	public String[] getMode() {
		return mode;
	}

	public void setMode(String[] mode) {
		this.mode = mode;
	}

	int index;
	Double realValues[], ratings[], mappedValues[];
	

	public String toString(){
		return "THNum"+beta+","+Arrays.toString(mode);
	}
	
	@Override
	public int compare(List<Object> arg0, List<Object> arg1) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Normalizer clone(){
		THOrdinalNormalizer thO = new THOrdinalNormalizer();
		thO.polynomial = polynomial;
		thO.beta = beta;
		thO.mode = mode;
		return thO;
	}
	
	@Override
	public Double normalize(List<Object> record) {
		if(record.get(index) == null)
			return null;
		return polynomial.getYValue(CommonUtils.objectToDouble(record.get(index)), mode);
	}

	@Override
	public void init(Attribute attribute) {
		index = attribute.getIndex();
		polynomial = new Polynomial(beta);
		List<AttributeValue> val = attribute.getValues();
		int count = 0;
		for (int i = 0; i < val.size(); i++) {
			count+=val.get(i).getRecords().size();
		}
		realValues = new Double[count];
		ratings = new Double[count];
		mappedValues = new Double[count];
		count = 0;
		for (int i = 0; i < val.size(); i++) {
			List<List<Object>> recs = val.get(i).getRecords();
			Double value = CommonUtils.objectToDouble(val.get(i).getValue());
			for (int j = 0; j < recs.size(); j++) {
				realValues[count]= value;
				ratings[count] = CommonUtils.objectToDouble(recs.get(j).get(2));
				count++;
			}
		}
		polynomial.compute(realValues, ratings);//, minValue, maxValue);
		mapValues(mode);
		
	}
	public void mapValues(String[] mode) {
		for (int i=0; i < realValues.length; i++) {
			if (realValues[i] != null) {
				mappedValues[i] = polynomial.getYValue(realValues[i], mode);
			} else {
				mappedValues[i] = new Double(Double.MIN_VALUE);
			}
		}
	}

	@Override
	public void configClassifier(XMLConfiguration config, String section) {
		
	}

	@Override
	public double compareTo(Normalizer n) {
		return 0;
	}

}
