package prefwork.normalizer;

import prefwork.Attribute;
import prefwork.CommonUtils;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

public class VirtualNominal  implements Normalizer {

	// Attribute that stores lists of values
	Attribute attr = new Attribute();

	int index;
	
	HashMap<String, Double> map = new HashMap<String, Double>();
	@Override
	public void configClassifier(XMLConfiguration config, String section) {
		int i = 0;
		while(config.getProperty( section+".value("+i+")") != null){
			String value = config.getString(section+".value("+i+")");
			Double rating = CommonUtils.objectToDouble(value.substring(value.lastIndexOf(":")+1));
			String name = value.substring(0,value.lastIndexOf(":"));
			map.put(name, rating);		
			i++;
		}
		
	}

	public HashMap<String, Double> getMap() {
		return map;
	}

	public void setMap(HashMap<String, Double> map) {
		this.map = map;
	}

	@Override
	public void init(Attribute attribute) {
		this.attr = attribute;
		index = attr.getIndex();		
	}

	@Override
	public Double normalize(List<Object> record) {
		if(!map.containsKey(record.get(index)))
			return null;
		return map.get(record.get(index));
	}

	@Override
	public int compare(List<Object> arg0, List<Object> arg1) {
		return 0;
	}

	/**
	 * Method for cloning Normalizer. Used for easier configuration.
	 * @return Instance of Normalizer.
	 */
	public Normalizer clone(){
		return new VirtualNominal();
	}

	@Override
	public double compareTo(Normalizer n) {
		// TODO Auto-generated method stub
		return 0;
	}
}
