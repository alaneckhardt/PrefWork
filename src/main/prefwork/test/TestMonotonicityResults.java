package prefwork.test;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;


/**
 * Contains results of an inductive method. Every result is stored with the rating computed by the method and with the original rating done by the user.
 * 
 * @author Alan
 *
 */
public class TestMonotonicityResults {

	/**
	 * Results of a method. Key is user id, value is map of object ids and the normalized attribute values.
	 */
	HashMap<Integer, Map<Integer,Double[]>> userResults = new HashMap<Integer, Map<Integer,Double[]>>();

	public TestMonotonicityResults(BasicDataSource datasource){
	}
	
	/**
	 * 
	 * @return All user ids.
	 */
	public Set<Integer> getUsers(){
		return userResults.keySet();
	}
	
	@SuppressWarnings("unchecked")
	public Map<Integer,Double[]> getListStats(Integer userId){
		if(userResults.get(userId) == null){
			userResults.put(userId, new HashMap<Integer,Double[]>());
		}
		return userResults.get(userId);
	}
	/**
	 * 
	 * @param userId
	 * @return Stats for specified user id
	 */
	@SuppressWarnings("unchecked")
	public Stats getStat(Integer userId, int run) {
		if(userResults.get(userId) == null){
			List l = CommonUtils.getList();
			userResults.put(userId, l);
		}
		if(userResults.get(userId).size()-1<run || userResults.get(userId).get(run) == null){
			Stats stat = new Stats();
			stat.userId = userId;
			List l = userResults.get(userId);
			for (int i = userResults.get(userId).size(); i < run; i++) {				
				l.add(i, null);							
			}
			l.add(run, stat);			
		}
		return userResults.get(userId).get(run);
	}

	/**
	 * 
	 * @param userId
	 * @return Stats for specified user id
	 */
	public Stats getStatNoAdd(Integer userId, int run) {
		if(userResults.get(userId) == null){
			return null;
		}
		return userResults.get(userId).get(run);
	}

	/**
	 * Deletes all results.
	 */
	public void reset() {
		userResults = new HashMap<Integer, List<Stats>>();
	}

	public void setTrainCount(Integer userId, int run, int countTrain) {
		Stats stat = getStat(userId,run );
		stat.countTrain = countTrain;
	}

	/**
	 * Adds result for specified user and object. User and computed ratings are stored.
	 * @param userId
	 * @param objectId
	 * @param res
	 * @param compRes
	 */
	public void addResult(Integer userId,int run, Integer objectId, Double res,
			Double compRes) {
		Stats stat = getStat(userId, run);
		stat.mae += Math.abs(res - compRes);
		stat.rmse += Math.abs(res - compRes) * Math.abs(res - compRes);
		stat.countTest++;
		Double[] rs = { res, compRes };
		stat.ratings.put(objectId, rs);
	}
	/**
	 * Adds one to the number of objects unable to predict for specified user.
	 * @param userId
	 * @param objectId
	 * @param res
	 */
	public void addUnableToPredict(Integer userId, int run, Integer objectId, Double res) {
		Stats stat = getStat(userId, run);
		stat.countUnableToPredict++;
		stat.unableToPredict.put(objectId, res);
	}

	public void addBuildTimeUser(Integer userId,int run,  Long time) {
		Stats stat = getStat(userId, run);
		stat.buildTime += time;
	}

	public void addTestTimeUser(Integer userId,int run,  Long time) {
		Stats stat = getStat(userId, run);
		stat.testTime += time;
	}
	public void processResults() {
		for (List<Stats> stats : userResults.values()) {
			for (int i = 0; i < stats.size(); i++) {
				Stats stat = stats.get(i);
				if(stat == null)
					continue;
				stat.rmse /= stat.countTest;
				stat.rmse = Math.sqrt(stat.rmse);
				stat.mae /= stat.countTest;		
			}
		}
	}
	public double[] getClasses() {
		return classes;
	}
	public void setClasses(double[] classes) {
		this.classes = classes;
	}

}