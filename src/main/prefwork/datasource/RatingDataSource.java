package prefwork.datasource;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CombinationUser;
import prefwork.CommonUtils;
import prefwork.LogUser;
import prefwork.NaturalPrefUser;
import prefwork.SinUser;
import prefwork.VirtualUser;

/**
 * Datasource that computes ratings and adds them to the objects obtained by inner datasource.
 * The method of computing ratings is in the configuration confDataSources.xml
 * @author Alan
 *
 */
public class RatingDataSource extends CacheDataSource {
	//String basePath = "C:\\tmp\\";

	BasicDataSource innerDataSource;
	String innerName;
	int innerClassCount;
	XMLConfiguration configDriver;
	String sectionDriver;
	//int userCount = 1;
	long seed = 1056718L;
	int classCount = 5;
	double[] coefs;
	double[] adds;
	protected Attribute[] innerAttributes;
	
	//Minimums for numerical attributes
	double[] min;
	//Maximums for numerical attributes	
	double[] max;
	//Values of nominal and list attributes
	List<String> [] nominalValues;
	
	String userType;

	// UserId for traversing users set. currentUser cannot be used because of setFixedUserId
	//int usersCursor;
	
	//Whether to round ratings to integer or not.
	boolean roundRatings = true;
	
	public RatingDataSource(){		
	}


	public HashMap<Integer, Double> getOriginalRatings(){
		throw new UnsupportedOperationException();
	}

	protected void loadClasses(){
		List<Double> classes = CommonUtils.getList(5);
		for (int i = 0; i < userRecords.length; i++) {
			for (int j = 0; j < userRecords[i].length; j++) {
				double r = CommonUtils.objectToDouble(userRecords[i][j].get(2));
				if(!classes.contains(r))
					classes.add(r);				
			}
		}
		
		this.classes = new double[classes.size()];
		for (int i = 0; i < classes.size(); i++) {
			this.classes[i]=classes.get(i);
		}
		Arrays.sort(this.classes);
	}

	/**
	 * Copies data from innerDataSource to this.records
	 */
	@SuppressWarnings("unchecked")
	protected void loadData(List<VirtualUser> users) {
		//Random r = new Random(seed);
		userCursor = 0;
		RandomComparator comp = null;
		HashMap<Integer, List<List<Object>>> userRecords = new HashMap<Integer, List<List<Object>>>();
		innerDataSource.restart();
		if (!innerDataSource.hasNextRecord())
			return;
		List<Object> rec2;
		//innerDataSource.getRecord()
		while ((rec2 = getInnerRecord()) != null) {
			Integer objectId = CommonUtils.objectToInteger(rec2.get(0));
			userCursor = 0;
			while (userCursor < userCount) {
				//Random r = new Random(objectId);
				List<Object> rec = CommonUtils.getList(rec2.size()+4);
				rec.addAll(rec2);
				Double res = users.get(userCursor).getRating(rec);
				if(roundRatings)
					res = CommonUtils.roundToDecimals(coefs[userCursor]*(res+adds[userCursor]),0);
				else
					res = coefs[userCursor]*(res+adds[userCursor]);
				rec.remove(0);
				rec.add(0, res);
				rec.add(0, objectId);
				rec.add(0, userCursor);

				for (int i = 0; i < rec.size(); i++) {
					if (attributes[i].getType() == Attribute.LIST
							&& !(rec.get(i) instanceof List)) {
						String[] array = rec.get(i).toString().split(",");
						List<Object> val = CommonUtils.getList(array.length);
						for (String s : array) {
							val.add(s);
						}
						rec.set(i, val);
					}
				}
				// We add this record to the list associated with current user;
				if (!userRecords.containsKey(userCursor)) {
					userRecords.put(userCursor, CommonUtils.getListList());
				}
				userRecords.get(userCursor).add(rec);
				userCursor++;
			}
		}

		this.userRecords = new List[userRecords.size()][];
		for (int i = 0; i < userRecords.size(); i++) {
			this.userRecords[i] = new List[userRecords.get(i).size()];
			for (int j2 = 0; j2 < userRecords.get(i).size(); j2++) {
				this.userRecords[i][j2] = userRecords.get(i).get(j2);
			}
			//userRecords.remove(i);
		}
		
		fillRandomValues();

		// innerDataSource.restartUserId();
		innerDataSource.restart();
	}

	
	public List<Object> getRecord() {
		if (!hasNextRecord())
			return null;
		List<Object> record = super.getRecord();
		//Integer objectId = CommonUtils.objectToInteger(record.get(0));
		//Double res = users.get(userId).getRating(record);
		//record.remove(0);
		/*if(roundRatings)
			record.add(0, CommonUtils.roundToDecimals(1+coefs[userId]*(res+adds[userId]),0));
		else
			record.add(0, 1+coefs[userId]*(res+adds[userId]));
		
		record.add(0, objectId);
		record.add(0, userId);*/
		return record;
		

		
	}


	@SuppressWarnings("unchecked")
	protected void initMaxMinNominal(){
		innerDataSource.restart();
		Attribute[] attributes = innerDataSource.getAttributes();
		if (!innerDataSource.hasNextRecord())
			return;
		List<Object> rec = getInnerRecord();
		nominalValues = new List[rec.size()];
		min = new double[rec.size()];
		max = new double[rec.size()];
		for (int i = 0; i < rec.size(); i++) {
			nominalValues[i]= CommonUtils.getList();
			min[i]=Double.MAX_VALUE;
			max[i]=Double.MIN_VALUE;
		}
		int count = 0;
		while(rec!= null){
			count++;
			for (int i = 0; i < rec.size(); i++) {
				if(!innerAttributes[i].contains(rec.get(i))){
					AttributeValue val=new AttributeValue(innerAttributes[i], rec.get(i));
					innerAttributes[i].addValue(val);
				}
				if(attributes[i].getType()==Attribute.NOMINAL){
					if(!nominalValues[i].contains(rec.get(i).toString()))
						nominalValues[i].add(rec.get(i).toString());
				}
				else if(attributes[i].getType()==Attribute.NUMERICAL){
					double d = CommonUtils.objectToDouble(rec.get(i));
					if(min[i]>d)
						min[i]=d;
					if(max[i]<d)
						max[i]=d;					
				}
				else if(attributes[i].getType()==Attribute.LIST){
					//String[] list = rec.get(i).toString().split(",");
					for(String s : ((List<String>)rec.get(i))){
						if(!nominalValues[i].contains(s))
							nominalValues[i].add(s);
					}
					
				}
			}
			rec = getInnerRecord();
		}
		for (int i = 0; i < attributes.length; i++) {
			if(attributes[i].getType()==Attribute.NUMERICAL){
				innerAttributes[i].setMax(max[i]);
				innerAttributes[i].setMin(min[i]);
			}
			else if(attributes[i].getType()==Attribute.LIST){
				innerAttributes[i].setValues(null);
				for (String s : nominalValues[i]) {
					if(!innerAttributes[i].contains(s)){
						AttributeValue val=new AttributeValue(innerAttributes[i], s);
						innerAttributes[i].addValue(val);
					}
				}				
			}
		}
	}
	
	protected void initCoefs(List<VirtualUser> users){
		innerDataSource.restart();
		if (!innerDataSource.hasNextRecord())
			return;
		List<Object> record = getInnerRecord();
		//List<Double>max = CommonUtils.getList(), min = CommonUtils.getList();
		Double[]max = new Double[users.size()];
		Double[]min = new Double[users.size()];
		while(record!= null){
			Integer objectId = CommonUtils.objectToInteger(record.get(0));
			for (int i = 0; i < users.size(); i++) {
				VirtualUser u = users.get(i);
				Double res = u.getRating(record);		
				if(max[i] == null){
					max[i] = res;					
				}
				else if( max[i]< res){
					max[i]= res;
				}	
				if( min[i] == null){
					min[i] = res;					
				}
				else if(min[i]> res){
					min[i] = res;
				}
			}
			record = getInnerRecord();
		}
		coefs=new double[max.length];
		adds=new double[max.length];
		for (int i = 0; i < users.size(); i++) {
			adds[i]=-min[i];
			coefs[i]=(classCount-0.6)/(max[i]-min[i]);
		}
		innerDataSource.restart();
	}

	protected void getInnerData(XMLConfiguration config, String section){
		// Configure the inner datasource
		Attribute attributesTemp[] = innerDataSource.getAttributes();
		attributes = new Attribute[attributesTemp.length+2];
		attributes[0] = new Attribute(null,0,"userid");
		attributes[0].setType(Attribute.NUMERICAL);
		attributes[1] = new Attribute(null,1,attributesTemp[0].getName());
		attributes[1].setType(Attribute.NUMERICAL);
		attributes[2] = new Attribute(null,2,"rating");
		attributes[2].setType(Attribute.NUMERICAL);
		for (int i = 1; i < attributesTemp.length; i++) {
			attributes[i+2] = attributesTemp[i].clone();
			attributes[i+2].setIndex(i+2);
		}
		
		
	}
	protected void initUsers(List<VirtualUser> users){
		Random r = new Random(seed);
		for (int i = 0; i < userCount; i++) {
			double userType = r.nextInt(4);
			
			VirtualUser u = null;
			if("norm".equals(this.userType))
				u = new VirtualUser();
			else if("log".equals(this.userType))
				u = new LogUser();
			else if("sin".equals(this.userType))
				u = new SinUser();
			else if("comb".equals(this.userType))
				u = new CombinationUser();
			else if("nat".equals(this.userType))
				u = new NaturalPrefUser();
			else if(userType == 0)
				u = new VirtualUser();
			else if(userType == 1)
				u = new LogUser();
			else if(userType == 2)
				u = new SinUser();
			else if(userType == 3)
				u = new CombinationUser();
			//u.setUserId(i);
			u.init(innerAttributes, r);
			users.add(u);
		}
	}
	List<VirtualUser> users;
	public void configDataSource(XMLConfiguration config, String section) {	
		//if(users != null && users.size() == userCount)
		//	return;
		// Configure the inner datasource
		innerDataSource.configDataSource(config,  section);
		//innerName = innerDataSource.getName();
		innerAttributes = innerDataSource.getAttributes();
		innerDataSource.setName(innerDataSource.getName()+"inner");
		getInnerData(config,section);

		//String fileName = basePath+getName()+".ser";		
		//java.io.File f = new java.io.File(fileName);		
		/*if(f.exists()){
				userRecords = (List<Object>[][]) CommonUtils.loadDataFromFile(fileName);
			}
		else{*/
			users = CommonUtils.getList(userCount);
			initMaxMinNominal();
			initUsers(users);
			initCoefs(users);
			loadData(users);
		//	CommonUtils.writeDataToFile(fileName, userRecords);
		//}
		loadClasses();
			
	}
	protected List<Object> getInnerRecord(){
		return innerDataSource.getRecord();
	}

	@SuppressWarnings("unchecked")
	public void configDriver(XMLConfiguration config, String section) {	
		Configuration dbConf = config.configurationAt( section);
		userCount = CommonUtils.getIntFromConfIfNotNull(dbConf, "userCount", userCount);
		this.userRecords = new List[userCount][];
		userType = CommonUtils.getFromConfIfNotNull(dbConf, "userType", userType);
		roundRatings = CommonUtils.getBooleanFromConfIfNotNull(dbConf, "roundRatings", roundRatings);
		
		String dbClass = dbConf.getString("innerclass");
		if(innerDataSource!= null)
			return;
			Constructor[] a;
			try {
				a = Class.forName(dbClass).getConstructors();
				innerDataSource = (BasicDataSource) a[0].newInstance();
				innerDataSource.configDriver(config, section);
			} catch (Exception e) {
				e.printStackTrace();
			} 
		configDriver = config;
		sectionDriver = section;
	}

	public void setName(String name) {
		this.innerName = name;
	}
	
	public void setFixedUserId(Integer value) {	
		currentUser = value;
	}
	
	public String getName() {
		return "Rater7"+userType+userCount+innerName+roundRatings;
	}
	
	@Override
	public void setAttributes(Attribute[] attributes) {
		Attribute[] attributesTemp = new Attribute[attributes.length-2];
		//Add objectid at the beginning
		attributesTemp[0]=attributes[1];
		for (int i = 1; i < attributesTemp.length; i++) {
			attributesTemp[i]=attributes[i+2];
		}
		innerDataSource.setAttributes(attributesTemp);				
	}
	public List<VirtualUser> getUsers() {
		return users;
	}
	
	public int getCurrentUser() {
		return currentUser;
	}
	
/*
	public void fillRandomValues() {		
		innerDataSource.fillRandomValues();
	}


	public Attribute[] getAttributes() {
		return attributes;
	}


	public String[] getAttributesNames() {
		String[] names = new String[attributes.length];
		for (int i = 0; i < names.length; i++)
			names[i] = attributes[i].getName();
		return names;
	}


	public double[]getClasses() {
		double[] classes = new double[classCount];
		for (int i = 0; i < classes.length; i++) {
			classes[i]=i;
		}
		return classes;
		//return innerDataSource.getClasses();
	}
	
	public void setLimit(Double fromPct, Double toPct, boolean recordsFromRange) {		
		innerDataSource.setLimit(fromPct, toPct, recordsFromRange);		
	}
	
	public int getTargetAttribute() {
		return 2;
	}


	public void setName(String name) {
		innerDataSource.setName(name);				
	}*/


/*
	public String getRandomColumn() {
		return innerDataSource.getRandomColumn();
	}

	public void setRandomColumn(String randomColumn) {
		innerDataSource.setRandomColumn(randomColumn);
	}*/
}
