package prefwork.representant;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

public class AvgRepresentant implements Representant {

	public Double getRepresentant(Double[] array) {
		double avg = 0;
		for (int i = 0; i < array.length; i++)
			avg += array[i];
		avg /= array.length;
		return avg;
	}

	public Double getRepresentant(List<Double> array) {
		double avg = 0;		
		for (int i = 0; i < array.size(); i++)
			avg += array.get(i);
		avg /= array.size();		
		return avg;
	}

	public void configClassifier(XMLConfiguration config, String section) {
		// TODO Auto-generated method stub

	}

	public String toString() {
		return "Avg";
	}

}
