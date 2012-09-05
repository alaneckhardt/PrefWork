package prefwork.rater;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.AttributeValue;
import prefwork.CommonUtils;

/**
 * <p>
 * WeightAverage computes rating as x*wx+y*wy+../wx+wy+... It is weighed
 * average.
 * </p>
 * <p>
 * Copyright (c) 2006
 * </p>
 * 
 * @author Alan Eckhardt
 * @version 1.0
 */
public class WeightAverage extends WeightRater {


	public WeightAverage(double weights[]) {
		super(weights);
	}

	public WeightAverage() {
		super();
	}

	public Double getRating(Double[] ratings, AttributeValue[] record) {
		double res = 0;
		double divider = 0;
		boolean foundNonNull = false;
		for (int i = 0; i < ratings.length; i++) {
			if (ratings[i] == null || ratings[i] == 0.0 || Double.isInfinite(ratings[i])  || Double.isNaN(ratings[i]))
				res += 0.0;
			else {
				res += ratings[i] * weights[i];
				divider += weights[i];
				foundNonNull = true;
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
		Configuration methodConf = config.configurationAt(section);
		methodName = CommonUtils.getFromConfIfNotNull(methodConf,"weights",methodName);
		useWeights = CommonUtils.getBooleanFromConfIfNotNull(methodConf, "useWeights", useWeights);
	}

	public String toString(){
		return "WAvg"+methodName.substring(0,1)+(useWeights?"WV":"V");
	}

}

