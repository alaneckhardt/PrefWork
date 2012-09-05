package prefwork.normalizer;

import prefwork.Attribute;
import prefwork.CommonUtils;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

public class VirtualPeak  implements Normalizer {

	double peak;

	double max = 0;
	double min = 0;
	double divider;

	// Attribute that stores lists of values
	Attribute attr = new Attribute();

	int index;
	
	public VirtualPeak(double min, double max){
		this.max = max;
		this.min = min;		
	}
	@Override
	public void configClassifier(XMLConfiguration config, String section) {
		Configuration dbConf = config.configurationAt(section);
		peak = dbConf.getDouble("peak");
	}

	@Override
	public void init(Attribute attribute) {
		this.attr = attribute;
		index = attr.getIndex();
		if(Math.abs(peak-max)>Math.abs(peak-min))
			divider = Math.abs(peak-max);
		else
			divider = Math.abs(peak-min);
	}

	@Override
	public Double normalize(List<Object> record) {
		double val = CommonUtils.objectToDouble(record.get(index));
		
		return Math.abs(val-peak)/divider;
	}

	@Override
	public int compare(List<Object> o1, List<Object> o2) {
		return 0;
	}

	/**
	 * Method for cloning Normalizer. Used for easier configuration.
	 * @return Instance of Normalizer.
	 */
	public Normalizer clone(){
		return new VirtualPeak(min, max);
	}

	public double getPeak() {
		return peak;
	}
	public void setPeak(double peak) {
		/*if(max != 0 && peak > max)
			this.peak = max;
		else if(min != 0 && peak < min)
			this.peak = min;
		else*/
			this.peak = peak;
	}
	public double getMax() {
		return max;
	}
	public void setMax(double max) {
		this.max = max;
	}
	public double getMin() {
		return min;
	}
	public void setMin(double min) {
		this.min = min;
	}
	@Override
	public double compareTo(Normalizer n) {
		// TODO Auto-generated method stub
		return 0;
	}
}
