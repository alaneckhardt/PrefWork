package prefwork.datasource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.CommonUtils;
import prefwork.VirtualUser;

/**
 * Datasource that computes ratings and adds them to the objects obtained by inner datasource.
 * The method of computing ratings is in the configuration confDataSources.xml
 * @author Alan
 *
 */
public class RatingCollaborative extends RatingCars {


	//HashMap<Integer, List<List<Object>>> userRecords;
	//int objectIndex = 0;
	int run = 0;
	boolean testMode = false;
	public boolean isTestMode() {
		return testMode;
	}
	public void setTestMode(boolean testMode) {
		this.testMode = testMode;
	}
	public int getRun() {
		return run;
	}
	public void setRun(int run) {
		this.run = run;
	}


	int trainSetSize = 0;
	public int getTrainSetSize() {
		return trainSetSize;
	}
	public void setTrainSetSize(int trainSetSize) {
		this.trainSetSize = trainSetSize;
	}


	Integer targetAttribute;



	public void configDataSource(XMLConfiguration config, String section) {
		super.configDataSource(config, section);
		//loadData();
	}
	/**
	 * Copies data from innerDataSource to this.records
	 */
	@SuppressWarnings("unchecked")
	/*protected void loadData(List<VirtualUser> users) {
		//Random r = new Random(seed);
		int userId = 0;
		RandomComparator comp = null;
		userRecords = new HashMap<Integer, List<List<Object>>>();
		innerDataSource.restart();
		if (!innerDataSource.hasNextRecord())
			return;
		List<Object> rec2;
		while ((rec2 = innerDataSource.getRecord()) != null) {
			Integer objectId = CommonUtils.objectToInteger(rec2.get(0));
			userId = 0;
			while (userId < userCount) {
				//Random r = new Random(objectId);
				List<Object> rec = CommonUtils.getList();
				rec.addAll(rec2);
				Double res = users.get(userId).getRating(rec);
				if(roundRatings)
					res = CommonUtils.roundToDecimals(coefs[userId]*(res+adds[userId]),0);
				else
					res = coefs[userId]*(res+adds[userId]);
				
				rec.remove(0);
				rec.add(0, res);
				rec.add(0, objectId);
				rec.add(0, userId);

				for (int i = 0; i < rec.size(); i++) {
					if (attributes[i].getType() == Attribute.LIST
							&& !(rec.get(i) instanceof List)) {
						String[] array = rec.get(i).toString().split(",");
						List<Object> val = CommonUtils.getList();
						for (String s : array) {
							val.add(s);
						}
						rec.set(i, val);
					}
				}
				// We add this record to the list associated with current user;
				if (!userRecords.containsKey(userId)) {
					userRecords.put(userId, CommonUtils.getListList());
				}
				userRecords[userId].add(rec);
				userId++;
			}
		}
		fillRandomValues();

		// innerDataSource.restartUserId();
		innerDataSource.restart();
	}*/

	public void fillRandomValues() {
		RandomComparator comp = null;

		for (int i = 0; i < userRecords.length; i++) {
			Random rand = new Random(i);
			for (List<Object> rec3 : userRecords[i]) {
				rec3.add(rand.nextDouble());
				if (comp == null)
					comp = new RandomComparator(rec3.size() - 1);
			}
			
			Arrays.sort(userRecords[i], comp);			
			// Removing the random number at the end of the record
			for (int j = 0; j < userRecords[i].length; j++) {
				List<Object> rec3 = userRecords[i][j];
				//Removing the random column
				rec3.remove(rec3.size() - 1);
				//Setting new objectid
				//rec3.set(1, j);
				
			}
		}
	}
	public boolean hasNextRecord() {
		if(objectIndex>=userRecords[currentUser].length)
			return false;
		if(trainSetSize == 0)
			return true;
		//Test mode
		if(testMode && (objectIndex<run*trainSetSize || objectIndex>=(run+1)*trainSetSize))
			return true;
		//Train mode
		if(!testMode && objectIndex>=run*trainSetSize && objectIndex<(run+1)*trainSetSize)
			return true;
		return false;
	}

	public void restart() {
		if(testMode && run == 0)
			objectIndex = (run+1)*trainSetSize;
		else if(testMode)
			objectIndex = 0;
		else
			objectIndex = run*trainSetSize;
	}
	
	public List<Object> getRecord() {
		if (!hasNextRecord())
			return null;
		List<Object> record = userRecords[currentUser][objectIndex];
		//In test mode, skip the object used in training phase
		if(testMode && objectIndex+1==run*trainSetSize){
			objectIndex = (run+1)*trainSetSize;
		}
		else{
			objectIndex++;
		}
		
		/*Integer objectId = CommonUtils.objectToInteger(record.get(0));
		Double res = users.get(currentUser).getRating(record);

		record.remove(0);
		record.add(0, coefs[currentUser]*(res+adds[currentUser]));
		record.add(0, objectId);
		record.add(0, users.get(currentUser).getUserId());*/
		return record;
	}
	
	public String getName() {
		return "RaterCollaborativeb"+userType+userCount+innerName;
	}

	
	
	

}
/*
class RandomComparator implements Comparator<List<Object>>{

	public int compare(List<Object> o1, List<Object> o2) {
		if(o1 == null || o1.get(2) == null)
			return 1;
		if(o2 == null || o2.get(2) == null)
			return -1;
		if(o1.get(2) == null)
			return -1;
		if(o2.get(2) == null)
			return 1;
		return -Double.compare(CommonUtils.objectToDouble(o1.get(o1.size()-1)),CommonUtils.objectToDouble(o2.get(o2.size()-1)));		
	}
}*/
