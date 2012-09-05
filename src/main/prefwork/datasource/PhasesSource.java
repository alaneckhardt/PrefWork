package prefwork.datasource;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.CommonUtils;

public class PhasesSource extends InnerDataSource{
	List<List<Object>> records = null;
	HashMap<Integer, List<List<Object>>> userRecords;
	List<List<Object>> trainSet = null;
	HashMap<Long, Double> originalRatings;
	int phase;
	int trainSetSize;
	int classCount;
	int indexRecord = 0;
	boolean evaluationMode = false;
	boolean testMode = false;
	Integer userId;
	Integer targetAttribute;

	
	public PhasesSource(BasicDataSource innerDataSource, int trainSetSize, int classCount){
		init(innerDataSource, trainSetSize, classCount);				
	}
	
	

	/**
	 * Transforms the original rating to the rating suitable for current phase.
	 * The main reason is to increase the highest rating in each phase. 
	 * @param r
	 * @return
	 */
	private Double transformOrigRating(Double r){
		long rLong = Math.round(classCount*r/maxClass);
		double c = maxClass;
		double start = 0.0;
		// If 5 is the highest rating, we shrink r to 0-4 in the first phase,
		// to 0-4.5 in the second, 0-4.75 in the third etc.
		double end = c-1.0/(Math.pow(2, phase));
		//double res =end*(((maxClass*(double)rLong/classCount)-start)/c);
		return end*(((maxClass*(double)rLong/classCount)-start)/c);	
	}
	
	/**
	 * Prepares next phase - computes objects that will be present in the
	 * following phase in the trainingSet.
	 */
	public void prepareNextPhase() {
		phase++;
		indexRecord = 0;
		Collections.sort(userRecords.get(userId), new RatingComparator());
		List<List<Object>> list = userRecords.get(userId);
		// We add the first trainSetSize objects into training set (if they are not already present there).
		for (int i = 0; i  < trainSetSize; i++) {
			//If the object records.get(i) is not present in the trainSet, we add it.
			if(!trainSet.contains(list.get(i))){
				trainSet.add(list.get(i));
			}
			// Transformation of the original rating to the correct rating for this phase.
			long id = CommonUtils.objectToLong(trainSet.get(i).get(1));
			trainSet.get(i).set(2,transformOrigRating(originalRatings.get(id)));
		}
	}

	/**
	 * Prepares first phase - computes the object that will be present in the
	 * initial phase.
	 */
	public void prepareFirstPhase() {
		// We have to do this - in prepareNextPhase is called phase++.
		phase = 0;
		trainSet = CommonUtils.getList(trainSetSize);
		indexRecord = 0;
		Collections.sort(userRecords.get(userId), new RatingComparator());
		// We add the first trainSetSize objects into training set (if they are not already present there).
		int i;
		for (i = 0; i < trainSetSize; i++) {
			//If the object records.get(i) is not present in the trainSet, we add it.
			if(!trainSet.contains(userRecords.get(userId).get(i*userRecords.get(userId).size()/trainSetSize)))
				trainSet.add(userRecords.get(userId).get(i*userRecords.get(userId).size()/trainSetSize));
			// Transformation of the original rating to the correct rating for this phase.
			trainSet.get(i).set(2,transformOrigRating(originalRatings.get(CommonUtils.objectToLong(trainSet.get(i).get(1)))));
		}
	}

	public void init(BasicDataSource innerDataSource, int trainSetSize, int classCount){
		this.innerDataSource = innerDataSource;	
		this.trainSetSize = trainSetSize;	
		this.classCount = classCount;
		double[] classes = innerDataSource.getClasses();
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < classes.length; i++) {
            if (classes[i] > max) max = classes[i];
        }        
		this.maxClass = (int) Math.round(max);
		innerDataSource.restartUserId();
		restart();
		targetAttribute = innerDataSource.getTargetAttribute();
		originalRatings = new HashMap<Long, Double>();
		loadData();
	}
	
	
	/**
	 * Copies data from innerDataSource to this.records
	 */
	private void loadData(){
		Integer userIdDataset;
		records = CommonUtils.getList();
		userRecords = new HashMap<Integer, List<List<Object>>>();
		while( (userIdDataset= innerDataSource.getUserId())!=null){
			innerDataSource.setFixedUserId(userIdDataset);
			innerDataSource.restart();
			if (!innerDataSource.hasNextRecord())
				continue; 
			List<Object> rec;
			while ((rec = innerDataSource.getRecord()) != null) {

				records.add(rec);
				// We add this record to the list associated with current user;
				if(!userRecords.containsKey(CommonUtils.objectToInteger(rec.get(0)))){
					userRecords.put(CommonUtils.objectToInteger(rec.get(0)), CommonUtils.getListList());
				}
				userRecords.get(CommonUtils.objectToInteger(rec.get(0))).add(rec);
				originalRatings.put(CommonUtils.objectToLong(rec.get(1)),CommonUtils.objectToDouble(rec.get(2)));
			}
		}
		innerDataSource.restartUserId();
		innerDataSource.restart();
	}


	public List<Object> getRecord() {
		if (!hasNextRecord())
			return null;
		if(evaluationMode){
			return userRecords.get(userId).get(indexRecord++);
		}
		else if(testMode){
			// In testing, we use only records outside the training set.
			while(hasNextRecord() && trainSet.contains(userRecords.get(userId).get(indexRecord)))
				indexRecord++;
			if (!hasNextRecord())
				return null;
			// We put back the original rating. Otherwise the method will be always right!
			userRecords.get(userId).get(indexRecord).set(2, originalRatings.get(CommonUtils.objectToLong(userRecords.get(userId).get(indexRecord).get(1))));
			return userRecords.get(userId).get(indexRecord++);
		}
		else
			return trainSet.get(indexRecord++);
		
	}

	public boolean hasNextRecord() {
		//In evaluation, we traverse across all objects.
		if(evaluationMode || testMode){
			if(indexRecord>=userRecords.get(userId).size())
				return false;
			return true;
		}
		// Outside evaluation, we go only through trainSet.
		if(indexRecord>=trainSet.size())
			return false;
		return true;
	}

	/**
	 * Sets the dataset into evaluation mode - it return all the records for current user.
	 */
	public void setEvaluationMode(boolean evaluationMode){
		this.evaluationMode = evaluationMode;
		// We restart the set.
		indexRecord = 0;
	}


	public void restart() {
		indexRecord = 0;
	}

	
	public void restartUserId() {	
		userId = null;
		innerDataSource.restartUserId();	
	}

	public void setFixedUserId(Integer value) {	
		userId = value;
		innerDataSource.setFixedUserId(value);		
	}
	public String getName() {
		return "Phases"+innerDataSource.getName();
	}

	public int getRun() {
		return phase;
	}

	public void setRun(int run) {
		this.phase = run;
	}

	public int getTrainSetSize() {
		return trainSetSize;
	}

	public void setTrainSetSize(int trainSetSize) {
		this.trainSetSize = trainSetSize;
	}

	public boolean isTestMode() {
		return testMode;
	}

	public void setTestMode(boolean testMode) {
		this.testMode = testMode;
	}



	@Override
	public void setAttributes(Attribute[] attributes) {
		innerDataSource.setAttributes(attributes);		
	}



	@Override
	public String getRandomColumn() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void setRandomColumn(String randomColumn) {
		// TODO Auto-generated method stub
		
	}


	/**
	 * Return initial training set, not ordered.
	 * @param count
	 * @return
	 */
	/*public List<List<Object>> getFirstSet(int count){
		return records.subList(0, count);
	}*/
	/**
	 * Returns set in the i-th phase. It is ordered by ratings specified by the method. These ratings are stored in the 2nd position in the list.
	 * @param count
	 * @return
	 */
	/*public List<List<Object>> getRecords(int count, int phase){
		Collections.sort(records,new RatingComparator());
		return records.subList(0, count);
	}*/

	/**
	 * Transforms given rating back to original scale [0-classes];
	 * @param r
	 * @return
	 */
	/*public Double transformRating(Double r){
		double c = innerDataSource.getClasses();
		double start = 0;
		double end = c-1/(2^phase);
		return c*((r-start)/end);	
	}*/
}

class OriginalRatingComparator implements Comparator<List<Object>>{

	public HashMap<Integer, Double> originalRatings;

	public int compare(List<Object> o1, List<Object> o2) {
		return -Double.compare(originalRatings.get((Integer)o1.get(0)),originalRatings.get((Integer)o2.get(0)));
	}
}

class RatingComparator implements Comparator<List<Object>>{

	public int compare(List<Object> o1, List<Object> o2) {
		if(o1 == null || o1.get(2) == null)
			return 1;
		if(o2 == null || o2.get(2) == null)
			return -1;
		if(o1.get(2) == null)
			return -1;
		if(o2.get(2) == null)
			return 1;
		return -Double.compare(CommonUtils.objectToDouble(o1.get(2)),CommonUtils.objectToDouble(o2.get(2)));		
	}
}