package prefwork.datasource;

import java.util.List;

import prefwork.CommonUtils;

/**
 * Datasource that computes ratings and adds them to the objects obtained by inner datasource.
 * The method of computing ratings is in the configuration confDataSources.xml
 * @author Alan
 *
 */
public class RatingCars extends RatingDataSource {	
	public RatingCars(){		
	}

	protected static List<Object> getListFromString(String s){
			String[]array = s.split(",");
			List<Object> val = CommonUtils.getList(array.length);
			for(String st : array){
				val.add(st);
			}
			return val;
	}
	
	protected static List<Object> transformLists(List<Object> l){
		if(l== null)
			return null;
		for (int i = 16; i < l.size(); i++) {			
			l.set(i, getListFromString(l.get(i).toString()));
		}
		return l;
	}
	
/*
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
		List<List<Object>> records = CommonUtils.getList();
		innerDataSource.restart();
		List<Object> rec = innerDataSource.getRecord();
		while(rec != null){
			//UserId
			rec.add(0, 0);
			//Rating
			rec.add(2, 0);
			records.add(transformLists(rec));
			rec = innerDataSource.getRecord();
		}
		this.records = new List[records.size()];
		this.records = records.toArray(this.records);
		
	}*/

	protected List<Object> getInnerRecord(){
		return transformLists(innerDataSource.getRecord());
	}
	
}
