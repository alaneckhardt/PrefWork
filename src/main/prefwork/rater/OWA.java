package prefwork.rater;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.AttributeValue;

public class OWA extends WeightRater {


	public OWA(double weights[]) {
		super(weights);
	}

	public OWA() {
		super();
	}

	private double[] orderArray(double[] ratings){
		double[] ratingsOrder = ratings.clone();
		java.util.Arrays.sort(ratingsOrder);
		return ratingsOrder;		
	}
	
	private Double[] orderArray(Double[] ratings){
		Double[] ratingsOrder = ratings.clone();
		for (int i = 0; i < ratingsOrder.length; i++) {
			if(ratingsOrder[i]==null)
				ratingsOrder[i] = 0.0;
		}

		java.util.Arrays.sort(ratingsOrder);
		return ratingsOrder;		
	}
	public Double getRating(Double[] ratings, AttributeValue[] record) {
		double res = 0;
		double divider = 0;
		boolean foundNonNull = false;
		Double[] ratingsOrder = orderArray(ratings);
		double[] weightsOrder = orderArray(weights);
		for (int i = 0; i < ratingsOrder.length; i++) {
			if (ratingsOrder[i] == null || ratingsOrder[i] == 0.0)
				res += 0.0;
			else {
				res += ratingsOrder[i] * weightsOrder[i];
				divider += weightsOrder[i];
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
		methodName = methodConf.getString("weights");
	}

	public String toString(){
		return "OWA"+methodName;
	}

	
}


