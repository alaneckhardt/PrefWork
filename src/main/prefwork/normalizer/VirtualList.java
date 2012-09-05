package prefwork.normalizer;

import java.util.List;

import prefwork.CommonUtils;

public class VirtualList  extends VirtualNominal {


	protected double average(List<Object> list) {
		List<Double> l = CommonUtils.getList(3);
		for(Object o : list){
			if(map.containsKey(o))
				l.add(map.get(o));
		}
		return CommonUtils.average(l);
	}
	@SuppressWarnings("unchecked")
	@Override
	public Double normalize(List<Object> record) {
		if(record.get(index) instanceof List)
			return average((List)record.get(index));
		String[] array = record.get(index).toString().split(",");
		List<Object> val = CommonUtils.getList(array.length);
		for(String s : array){
			val.add(s);
		}
		return average(val);
	}

	/**
	 * Method for cloning Normalizer. Used for easier configuration.
	 * @return Instance of Normalizer.
	 */
	public Normalizer clone(){
		return new VirtualList();
	}
}
