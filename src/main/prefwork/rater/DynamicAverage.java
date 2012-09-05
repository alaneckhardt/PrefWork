package prefwork.rater;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;

/**
 * <p>
 * DynamicWeightAverage computes rating as x*wx+y*wy+../wx+wy+... It is weighed
 * average. Weights are variances of ratings of attribute values.
 * </p>
 * <p>
 * Copyright (c) 2006
 * </p>
 * 
 * @author Alan Eckhardt
 * @version 1.0
 */
public class DynamicAverage extends WeightRater {

	Attribute[] attr;

	public DynamicAverage() {
		methodName = "VARIANCE";
	}

	public Double getRating(Double[] ratings, AttributeValue[] record) {

		double res = 0;
		double divider = 0;
		boolean foundNonNull = false;
		for (int i = 0; i < ratings.length; i++) {
			if (ratings[i] == null || ratings[i] == 0.0 || Double.isInfinite(ratings[i])  || Double.isNaN(ratings[i]))
				res += 0.0;
			// Attribute value is missing, we use overall weight
			else if(record[i] == null || record[i].getVariance() == 0.0){
				res += ratings[i] * weights[i];
				divider += weights[i];
				foundNonNull = true;
			}
			// Attribute value is present, we use variance of the value
			else {	
					res += ratings[i] / record[i].getVariance();
					divider +=  1.0 / record[i].getVariance();
			}
			
		}
		if(!foundNonNull)
			return null;
		if (divider != 0)
			return res / divider;
		else
			return 0D;
	}

	public void configClassifier(XMLConfiguration config, String section) {
	}

	public String toString(){
		return "DynamicAvg";
	}

	public void init(Attribute[] attributes) {
		super.init(attributes);
		this.attr=attributes;
	}


}

