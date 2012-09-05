package prefwork.test;

import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Contains information about results of an inductive metod for a specific user.
 * 
 * @author Alan
 *
 */
public class Stats {
	Integer userId = 0;
	Integer run = 0;

	Double mae = 0.0;

	Double rmse = 0.0;

	Long buildTime = 0L;
	Long testTime = 0L;

	int countTrain = 0;
	int countTest = 0;
	int countUnableToPredict = 0;

	/**
	 * All objects with rating computed by the method and done by the user. Key is object id and value is a tuple [user rating, computed rating].
	 */
	HashMap<Integer, Double[]> ratings = new HashMap<Integer, Double[]>();

	HashMap<Integer, Double[]> mapped = new HashMap<Integer, Double[]>();
	
	
	HashMap<Integer, Double> unableToPredict  = new HashMap<Integer, Double>();

	/**
	 * 
	 * @return set of user ratings. Key of entry is object id, value is a tuple [user rating, computed rating].
	 */
	public Set<Entry<Integer, Double[]>> getSet(){
		Set<Entry<Integer, Double[]>> set = ratings.entrySet();
		if (set.size() <= 0)
			return null;
		//set = filterSet(set);
		return set;
	}
	
	


	
}