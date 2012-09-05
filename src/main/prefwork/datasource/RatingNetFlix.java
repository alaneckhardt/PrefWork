package prefwork.datasource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import prefwork.CommonUtils;

/**
 * Datasource that computes ratings and adds them to the objects obtained by inner datasource.
 * The method of computing ratings is in the configuration confDataSources.xml
 * @author Alan
 *
 */
public class RatingNetFlix extends RatingCollaborative {
	int ratingsCount = 200;
	private static Logger log = Logger.getLogger(RatingNetFlix.class);
	String basePath = "C:\\tmp\\";
	String lastFile = null;
	public void configDataSource(XMLConfiguration config, String section) {		
		// Configure the inner datasource
		innerDataSource.configDataSource(config,  section);
		//innerName = innerDataSource.getName();
		innerAttributes = innerDataSource.getAttributes();
		innerDataSource.setName(innerDataSource.getName()+"inner");
		if(lastFile == null || !lastFile.equals("FlixR"+getName())){
			loadData();
			lastFile = "FlixR"+getName();
		}
	}
	

	@SuppressWarnings("unchecked")
	protected void loadDataFromFile(String fileName){
		Long start;
			java.io.File f = new java.io.File(basePath+fileName+".ser");
			if(f.exists()){
				start = System.currentTimeMillis();

				userRecords = (List<Object>[][]) CommonUtils.loadDataFromFile(basePath+fileName+".ser");
				log.info("casFile" + (System.currentTimeMillis()-start));
			}
			else{

				start = System.currentTimeMillis();
				loadDataFromDb();
				log.info("casDb" + (System.currentTimeMillis()-start));
				//log.debug("Loaded file:"+fileName);

				start = System.currentTimeMillis();
				CommonUtils.writeDataToFile(basePath+fileName+".ser", userRecords);
				log.info("casWriteFile" + (System.currentTimeMillis()-start));
			}
	}
	private void loadData() {
		userCursor = 0;		
		// Configure the inner datasource
		attributes = innerDataSource.getAttributes();
	
		loadDataFromFile("FlixR"+getName());
		//loadDataFromDb();
	}
	/**
	 * Copies data from innerDataSource to this.records
	 */
	@SuppressWarnings("unchecked")
	private void loadDataFromDb() {
		//Random r = new Random(seed);
		((IMDbMemory)innerDataSource).usersSelect="select userid from UserCounts where  count>="+ratingsCount+" and count<"+ratingsCount+20+"  and rownum <= "+userCount+ " order by count";
		innerDataSource.restartUserId();
		Integer userIdDataset;
		//HashMap<Integer, List<List<Object>>> userRecords = new HashMap<Integer, List<List<Object>>>();
		List<Integer> userIds = CommonUtils.getList(userCount);
		this.userRecords = new List[userCount][];
		int userCount = 0;		
		while( (userIdDataset= innerDataSource.getUserId())!=null){
			userIds.add(userIdDataset);
			innerDataSource.setFixedUserId(userIdDataset);
			innerDataSource.restart();
			if (!innerDataSource.hasNextRecord())
				continue;
			List<Object> rec2;
			int count = 0;
			// We add this record to the list associated with current user;
			/*if (!userRecords.containsKey(userIdDataset)) {
				userRecords.put(userIdDataset, CommonUtils.getListList());
			}*/
			this.userRecords[userCount] = new List[ratingsCount+10];
			while ((rec2 = innerDataSource.getRecord()) != null && count < ratingsCount) {
				//userRecords.get(userIdDataset).add(rec2);
				this.userRecords[userCount][count] = rec2;
				rec2.set(0, userCount);
				count++;
			}
			int i = userRecords[userCount].length-1;
			for (; i >= 0; i--) {
				if(userRecords[userCount][i]!=null)
					break;
			}
			userRecords[userCount] = Arrays.copyOf(userRecords[userCount], i+1);
			if(count == 0){
				userCount--;
				//userRecords.remove(userIdDataset);
			}
			userCount++;
			//if(userCount%100==0)
			//System.out.println(" "+userCount);
			
		}
		//System.out.println("Done");
		//this.userRecords = ;
		/*this.userRecords = new List[userIds.size()][];
		for (int i = 0; i < userIds.size(); i++) {
			this.userRecords[i] = new List[userRecords.get(userIds.get(i)).size()];
			for (int j2 = 0; j2 < userRecords.get(userIds.get(i)).size(); j2++) {
				this.userRecords[i][j2] = userRecords.get(userIds.get(i)).get(j2);
			}
			userRecords.remove(userIds.get(i));
		}*/

		fillRandomValues();
		// innerDataSource.restartUserId();
		innerDataSource.restart();
	}


	public void configDriver(XMLConfiguration config, String section) {	
		super.configDriver(config, section);
		Configuration dbConf = config.configurationAt( section);
		ratingsCount = CommonUtils.getIntFromConfIfNotNull(dbConf, "ratingsCount", ratingsCount);
		basePath = CommonUtils.getFromConfIfNotNull(dbConf, "basePath", basePath);
		
	}
	
	
	public void fillRandomValues() {
		if(userRecords == null)
			return;
		
		RandomComparator comp = null;

		for (int i = 0; i < userRecords.length; i++) {
		//for(int userId : userRecords.keySet()){
			
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

	public Integer getUserId() {
		if(userCursor>=userRecords.length)
			return null;
		currentUser = userCursor;
		userCursor++;
		return currentUser;
	}
	
	public boolean hasNextRecord() {
		if(userRecords[currentUser] == null)
			return false;
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
		return record;
	}

	public String getName(){
		return "RatingNetFlix2"+(((IMDbMemory)innerDataSource).getPlot?"Plot":"")+(((IMDbMemory)innerDataSource).getLaserDisc?"Laser":"")+IMDbMemory.IMDBMaps.size()+innerDataSource.getClass().getSimpleName()+userCount+ratingsCount;
	}

}
