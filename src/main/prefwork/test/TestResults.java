package prefwork.test;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
public class TestResults {

	/**
	 * Results of a method. Key is user id, value are list of stats (for each run one stat)for this user.
	 */
	HashMap<Integer, List<Stats>> userResults = new HashMap<Integer, List<Stats>>();

	double[] classes = null;
	public TestResults(BasicDataSource datasource){
		classes = datasource.getClasses();
	}
	
	/**
	 * 
	 * @return All user ids.
	 */
	public Set<Integer> getUsers(){
		return userResults.keySet();
	}
	
	@SuppressWarnings("unchecked")
	public List<Stats> getListStats(Integer userId){
		if(userResults.get(userId) == null){
			List l = CommonUtils.getList();
			userResults.put(userId, l);
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

/**
 * Contains information about results of testing in form of a four-folds table.
 * @author Alan
 *
 */
class FFTable{
	public int a =0,b=0,c=0,d=0,count=0;
	public double recall = 0.0, FPR = 0.0, precision = 0.0, Fmeasure = 0.0;

	public void initTable(int cut, Entry<Integer,Double[]>[] array1){
		for (Entry<Integer, Double[]> entry : array1) {
			if(entry.getValue()[0]>=cut ){
				b++;
			}
			if(entry.getValue()[0]<cut ){
				c++;
			}
		}
	}
	
	public void computeStats(){
		if(a+b>0)
			precision =(0.0+a)/(0.0+a+b);
		else
			precision = 0;
		
		if(a+c>0)
			recall =(0.0+a)/(0.0+a+c);
		else
			recall = 0;
		
		if(b+d>0)
			FPR =(0.0+b)/(0.0+b+0.0+d);
		else
			FPR = 0;
		

		if(a+b+d>0)
			Fmeasure =(0.0+a)/(0.0+a+b+c);
		else
			Fmeasure = 0;	
	}
}

/**
 * Compares two hashmap entries according to the value at the given index.
 * @author Alan
 *
 */
class CompareEntry implements Comparator<Entry<Integer, Double[]>> {

	public int index = 0;

	public int compare(Entry<Integer, Double[]> o1, Entry<Integer, Double[]> o2) {
		if(Double.compare(o1.getValue()[index], o2.getValue()[index])!=0)
			// We want the highest rating at the top
			return -Double.compare(o1.getValue()[index], o2.getValue()[index]);
		if(o1.getKey().equals( o2.getKey()))
			return 0;
		if(o1.getKey()> o2.getKey())
			return 1;
		return -1;
	}
}


class Rating{
	int objectId;
	double rating;
	public String toString(){
		return objectId+";"+rating;
	}
}

/**
 * Compares two double values and order them descendingly.
 * @author Alan
 *
 */
class CompareIds implements Comparator<Rating> {


	public int compare(Rating o1, Rating o2) {		
		return -Double.compare(o1.objectId, o2.objectId);		
	}
}

/**
 * Compares two double values and order them descendingly.
 * @author Alan
 *
 */
class CompareRatings implements Comparator<Rating> {


	public int compare(Rating o1, Rating o2) {		
		return -Double.compare(o1.rating, o2.rating);		
	}
}