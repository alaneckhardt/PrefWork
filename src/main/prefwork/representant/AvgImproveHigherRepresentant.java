package prefwork.representant;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.CommonUtils;

public class AvgImproveHigherRepresentant implements Representant {

	int addCount = 3;
	double threshold = 3.0;

	public Double getRepresentant(Double[] array) {
		double avg = 0;
		int count = 0;
		for (int i = 0; i < array.length; i++) {
			if (array[i] > threshold) {
				avg += array[i] * (addCount + 1);
				count += addCount + 1;
			} else {
				avg += array[i];
				count++;
			}

		}
		avg /= count;
		return avg;
	}

	public Double getRepresentant(List<Double> array) {
		double avg = 0;
		int count = 0;
		for (int i = 0; i < array.size(); i++)
			if (array.get(i) > threshold) {
				avg += array.get(i) * (addCount + 1);
				count += addCount + 1;
			} else {
				avg += array.get(i);
				count++;
			}
		avg /= count;
		return avg;
	}

	public void configClassifier(XMLConfiguration config, String section) {
		if(config.containsKey(section))
		addCount = CommonUtils.getIntFromConfIfNotNull(config
				.configurationAt(section), "addCount", addCount);
		threshold = CommonUtils.getDoubleFromConfIfNotNull(config
				.configurationAt(section), "threshold", threshold);
	}

	public String toString() {
		return "AvgImprove"+addCount;
	}

}
