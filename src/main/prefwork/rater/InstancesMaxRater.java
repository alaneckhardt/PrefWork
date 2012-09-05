package prefwork.rater;

import java.util.List;

import prefwork.CommonUtils;

public class InstancesMaxRater extends InstancesRater{
	
	public String toString(){
		return "Max";
	}
	
	public Double getRating( List<Object> record) {		
		Double[] locPrefRec = CommonUtils.getLocalPref(record, attributes);
		paretodown = CommonUtils.getList();
		paretoup = CommonUtils.getList();
		findPareto( locPrefRec);
		double ratingUp = 0.0D;
		double ratingDown = 0.0D;
		ratingUp = getParetoRating(locPrefRec, paretoup);
		ratingDown = getParetoRating(locPrefRec, paretodown);
		if(ratingDown == 0)
			return ratingUp;
		if(ratingUp == 0)
			return ratingDown;
		return (ratingUp+ratingDown)/2;
	}


}
