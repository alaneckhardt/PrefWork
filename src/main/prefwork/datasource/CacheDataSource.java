package prefwork.datasource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.CommonUtils;


public abstract class CacheDataSource implements BasicDataSource {

	/** Name of dataset, for statistics*/
	protected String name;

	/** Index of attribute that contains rating*/
	protected int targetAttribute;

	/** Name of random column*/
	protected String randomColumn = null;


	/** Count of possible classes*/
	protected double[] classes = null;
	
	/** Array of users' records. User's records are arrays of lists.*/
	List<Object>[][] userRecords;

	/** Attributes of the dataset */
	protected Attribute[] attributes;
	String[] names = null;
	
	int objectIndex = 0;
	double from, to;
	boolean fromRange;
	protected int randomIndexInList;
	protected List<Integer> ignoreList;
	protected int maxLoad = 5000;
	protected int maxSize = 200;
	/**Index of run. Used for setting random values. */
	int runNumber = 1;
	

	double coef = 0,add = 0;
	
	
	int userCount = 1;
	/**UserId of current user*/
	int currentUser;
	
	/** Cursor for traversing users set. currentUser cannot be used because of setFixedUserId*/
	int userCursor = 0;

	public void restartUserId() {	
		userCursor = 0;
		currentUser = 0;
	}

	public Integer getUserId() {
		if(this.userCursor>=userCount)
			return null;
		currentUser = this.userCursor;
		this.userCursor++;
		return currentUser;
	}

	protected List<Object> cloneList(List<Object> rec) {
		List<Object> l = CommonUtils.getList(rec.size());
		for (int i = 0; i < rec.size(); i++) {
			l.add(rec.get(i));
		}
		return l;
	}

	public List<Object> getRecord() {
		if (!hasNextRecord())
			return null;
		int objectIndexTemp = objectIndex;
		objectIndex++;
		objectIndex=getNextIndex();
		return cloneList(userRecords[currentUser][objectIndexTemp]);
	}

	public String[] getAttributesNames() {
		if(names != null)
			return names;
		names = new String[attributes.length];
		for (int i = 0; i < names.length; i++)
			names[i] = attributes[i].getName();
		return names;
	}

	public Attribute[] getAttributes() {
		return attributes;
	}

	public void setAttributes(Attribute[] attributes) {

	}

	public int getTargetAttribute() {
		return 2;
	}

	public double[] getClasses() {
		return classes;
	}

	public void setLimit(Double fromPct, Double toPct, boolean recordsFromRange) {
		this.from =fromPct;
		this.to =toPct;
		this.fromRange = recordsFromRange;
	}

	public void configDriver(XMLConfiguration config, String section) {
		Configuration dbConf = config.configurationAt( section);
		userCount = CommonUtils.getIntFromConfIfNotNull(dbConf, "userCount", userCount);
		maxLoad = CommonUtils.getIntFromConfIfNotNull(dbConf, "maxLoad", maxLoad);
		maxSize = CommonUtils.getIntFromConfIfNotNull(dbConf, "maxSize", maxSize);
	
		
	}

	public void fillRandomValues() {
		//synchronized (semRandoms) {
			// Waiting for a free slot;
		//	semRandoms.acquire();
		RandomComparator comp = new RandomComparator(attributes.length);
		
		for (int i = 0; i < userCount; i++) {
			Random rand = new Random(i*runNumber);
			for (int j = 0; j < userRecords[i].length; j++) {
				List<Object> l = userRecords[i][j];
				l.add(rand.nextDouble());			
			}
			Arrays.sort(userRecords[i],comp);
		}
		for (int i = 0; i < userCount; i++) {
			for (int j = 0; j < userRecords[i].length; j++) {
				List<Object> rec = userRecords[i][j];
				//Removing the random column
				rec.remove(attributes.length);		
				//Setting new objectid
				rec.set(1, j);
			}
		}
		runNumber++;
		//semRandoms.release();
		//}
	}

	public void setFixedUserId(Integer value) {
		;
	}


	public void restart() {
		objectIndex=0;
		objectIndex=getNextIndex();
	}


	public boolean satisfyCondition(List<Object> l) {
		if (randomColumn == null)
			return true;
		Double r = CommonUtils.objectToDouble(l.get(randomIndexInList));
		if ((r >= from && r < to && !fromRange)
				|| ((r < from || r >= to) && fromRange))
			return false;
		return true;
	}

	protected Integer getNextIndex(){
		if (objectIndex >= userRecords[currentUser].length || objectIndex >= maxSize)
			return objectIndex;
		int objectIndex = this.objectIndex;
		List<Object> l = userRecords[currentUser][objectIndex];
		while (objectIndex < userRecords[currentUser].length - 1 && objectIndex < maxSize -1 && !satisfyCondition(l)) {
			objectIndex++;
			l = userRecords[currentUser][objectIndex];
		}		
		return objectIndex;
		
	}
	public boolean hasNextRecord() {
		if (objectIndex >= userRecords[currentUser].length || objectIndex >= maxSize)
			return false;
		int objectIndex = getNextIndex();
		List<Object> l = userRecords[currentUser][objectIndex];
		if (objectIndex > userRecords[currentUser].length)
			return false;
		if (satisfyCondition(l))
			return true;
		return false;
	}


	public void setName(String name) {
		this.name = name;
	}

	public String getRandomColumn() {
		return randomColumn;
	}

	public void setRandomColumn(String randomColumn) {
		this.randomColumn = randomColumn;
		for (int i = 0; i < attributes.length; i++) {
			if (randomColumn.equals(attributes[i].getName())) {
				randomIndexInList = i;
				break;
			}
		}
	}

	public List<Object>[][] getUserRecords() {
		return userRecords;
	}

	public void setUserRecords(List<Object>[][] userRecords) {
		this.userRecords = userRecords;
	}
	
	/*
	public static Map<String,SemaphoreCache> semRandoms = new HashMap<String,SemaphoreCache>();
	
	protected void addToMap(String key, List<Object>[][] value){
		staticData.put(key, value);
		semRandoms.put(key, new SemaphoreCache(1));
	}
	protected List<Object>[][] getFromMap(String key){
		synchronized (semRandoms.get(key)) {
			// Waiting for a free slot;
			semRandoms.get(key).acquire();
			List<Object>[][] t =staticData.get(key);
			semRandoms.get(key).release();
			return t;
		}
	}
	class SemaphoreCache {
	private int count;

	public SemaphoreCache(int n) {
		this.count = n;
	}

	public synchronized void acquire() {
		while (count == 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				// keep trying
			}
		}
		count--;
	}

	public synchronized void release() {
		count++;
		notify(); // alert a thread that's blocking on this semaphore
	}
}*/
	

}

 