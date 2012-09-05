package prefwork;

import java.util.List;

public class NaturalPrefUser extends VirtualUser {

	/**
	 * Number of preferences should be even number - 1,3,5,7,9.
	 */
	int numOfPreferences = 9;
	
	/**
	 * Function for natural preferences, that is conjunctive at value 0,0 and disjunctive at 1,1
	 * @param pref1
	 * @param pref2
	 * @return
	 */
	private Double getNaturalPreference(Double pref1, Double pref2) {

		//Transformation of pref1 - for using sqrt and power, we need to reverse  the axis of x
		pref1=1-pref1;
		
		// x^(2^i) power
		for (int i = (numOfPreferences - 1) / 2; i > 0 ; i--) {
			if (Math.pow(pref1,  (i+1)) > pref2)
			//if (Math.pow(pref1, Math.pow(2, i)) > pref2)
			//if (Math.pow(pref1, maxDegree*((double)(i+1)/((numOfPreferences - 1) / 2))) > pref2)
				return (((double) ((numOfPreferences - 1) / 2)-(i)) / (double) numOfPreferences);
		}
		// linear
		if (pref1 > pref2)
			return 0.5;

		// x^(1/2^i) sqrts
		for (int i = 1; i <= (numOfPreferences - 1) / 2; i++) {
			if (Math.pow(pref1,  1/(i+1)) > pref2)
			//if (Math.pow(pref1, 1/Math.pow(2, i)) > pref2)
			//if (Math.pow(pref1, 1/(maxDegree*((double)(i+1)/((numOfPreferences - 1) / 2)))) > pref2)
				return (((double) i+(numOfPreferences - 1) / 2)  / (double) numOfPreferences);
		}
		return 1.0;
	}
	
	public Double getRating(List<Object> record){
		Double[] ratings = getRatings(record);

		//res=getNaturalPreference(CommonUtils.objectToDouble(record.get(3)), CommonUtils.objectToDouble(record.get(4)));
		Double res = 0.0;
		if (record.size() > 3) {

			res = getNaturalPreference(ratings[1], ratings[2]);

			if (record.size() >= 3)
				for (int i = 3; i < ratings.length; i++) {
					res = getNaturalPreference(res, ratings[i]);
				}
		}
		return res;
	}
	

	public Double[] getRatings(List<Object> record){
		Double[] ratings = new Double[record.size()-1];
		for (int i = 1; i < record.size(); i++) {
			ratings[i-1] = norms[i].normalize(record);
		}
		return ratings;
	}
}
