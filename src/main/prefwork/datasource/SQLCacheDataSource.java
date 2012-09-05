package prefwork.datasource;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.CommonUtils;

/**
 * SQLDataSource provides connection to arbitrary SQL database. It contains
 * methods for querying for records and users. Every subclass must instantiate
 * provider in its constructor with a database provider.
 * 
 * @author Alan
 * 
 */
public abstract class SQLCacheDataSource extends SQLDataSource {

	protected List<List<Object>> recordsList;
	protected List<Integer> usersList;
	protected int randomIndexInList;
	protected int objectIndex;
	protected int userIndexInList;
	double from, to;
	boolean fromRange;
	
	public boolean satisfyCondition(List<Object> l){
		if(randomColumn == null)
			return true;
		Double r = CommonUtils.objectToDouble(l.get(randomIndexInList)); 
		if((r>=from && r < to && !fromRange) || (r<from && r >= to && fromRange))
			return false;
		return true;
	}
	protected int getNextIndex(int objectIndex){
		if(userColumn == null){
			return objectIndex+1;
		}
		return -1;
		/*List l = recordsList.get(objectIndex);		
		int userID = CommonUtils.objectToInteger(l.get(userIndexInList));
		while(userID != this.userID && objectIndex<recordsList.size()){
			objectIndex++;
			l = recordsList.get(objectIndex);
			userID = CommonUtils.objectToInteger(l.get(userIndexInList));
		}
		return objectIndex;*/
	}
	
	public boolean hasNextRecord() {
		//if(userID != null){
			if(objectIndex>=recordsList.size())
				return false;
			int objectIndex=this.objectIndex;
			List<Object> l = recordsList.get(objectIndex);
			while(objectIndex<recordsList.size()-1 && !satisfyCondition(l)){
				objectIndex=getNextIndex(objectIndex);
				l = recordsList.get(objectIndex); 
			}
			if(objectIndex>recordsList.size())
				return false;
			if(satisfyCondition(l))
				return true;
			return false;
		//}
	}

	public void configDriver(XMLConfiguration config, String section) {
		super.configDriver(config, section);		
	}

	public void configDataSource(XMLConfiguration config, String section) {
		super.configDataSource(config, section);
		super.restart();
		List<Object> l;
		recordsList = CommonUtils.getListList();
		while(super.hasNextRecord()){

			l = CommonUtils.getList(attributes.length);
			try {
				for (int i = 1; i <= attributes.length; i++)
					l.add(records.getObject(i));
				records.next();
			} catch (SQLException e) {
				e.printStackTrace();			
			}
			recordsList.add(l);
		}	
		if(userColumn != null)
			for (int i = 0; i < attributes.length; i++) {
				if(userColumn.equals(attributes[i].getName())){
					userIndexInList = i;
						break;
				}
			}	
		
	}
	
	protected List<Object> cloneList(List<Object> rec){
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
		objectIndex=getNextIndex(objectIndex);		 
		return cloneList(recordsList.get(objectIndexTemp));
	}


	public void restart() {
		objectIndex = 0;
	}

	public void setLimit(Double fromPct, Double toPct, boolean recordsFromRange) {
		this.from =fromPct;
		this.to =toPct;
		this.fromRange = recordsFromRange;
	}

	public String getRandomColumn() {
		return randomColumn;
	}

	public void setRandomColumn(String randomColumn) {
		this.randomColumn = randomColumn;
		for (int i = 0; i < attributes.length; i++) {
			if(randomColumn.equals(attributes[i].getName())){
				randomIndexInList = i;
					break;
			}
		}
	}


}