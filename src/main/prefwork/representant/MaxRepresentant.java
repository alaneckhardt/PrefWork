package prefwork.representant;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

public class MaxRepresentant implements Representant {

	public Double getRepresentant(Double[] array) {
		double max = 0;		
		for (int i = 0; i < array.length; i++)
			if(max<array[i])
				max= array[i];
		return max;
	}

	public Double getRepresentant(List<Double> array) {
		double max = 0;		
		for (int i = 0; i < array.size(); i++)
			if(max<array.get(i))
				max= array.get(i);
		return max;
	}

	public void configClassifier(XMLConfiguration config, String section) {
	}

	public String toString() {
		return "Max";
	}

}
