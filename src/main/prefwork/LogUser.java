package prefwork;

import java.util.List;

public class LogUser extends VirtualUser {

	public Double getRating(List<Object> record){
		Double[] ratings = getRatings(record);
		
		Double res = 1.0;
		res = rater.getRating(ratings, CommonUtils.getValuesFromList(record, attributes));
		if (res != null)
			return Math.log(1+res);
		return res;
	}
	

	public Double[] getRatings(List<Object> record){
		Double[] ratings = new Double[record.size()-1];
		for (int i = 1; i < record.size(); i++) {
			ratings[i-1] = Math.log(1+norms[i].normalize(record));
		}
		return ratings;
	}
}
