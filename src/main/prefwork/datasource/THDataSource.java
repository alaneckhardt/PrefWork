package prefwork.datasource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.CommonUtils;


public class THDataSource extends CacheDataSource{
	protected boolean userIdSent = false;
	protected boolean hasUserId = false;
	protected boolean hasObjectId = false;

	protected boolean allowNulls = false;
	String file;
	
	/*public List<Object> getRecord() {
		if (!hasNextRecord())
			return null;
		int objectIndexTemp = objectIndex;
		objectIndex++;
		objectIndex=getNextIndex();
		return cloneList(userRecords[0][objectIndexTemp]);
	}*/

	public THDataSource(){
	}
	protected void getData() {
		try {
			BufferedReader in = new BufferedReader(
					new FileReader(file + ".dat"));
			String line;
			int i = 0;
			double max = Double.MIN_VALUE,min = Double.MAX_VALUE;
			List<List<Object>> userRecords = CommonUtils.getList();
			while ((line = in.readLine()) != null && i < maxLoad) {
				String[] rec = line.split(";");
				if(rec.length<2)
					continue;
				List<Object> l = CommonUtils.getList(rec.length+3);
				int j = 0;
				//UserId
				if(hasUserId){
					l.add(CommonUtils.objectToInteger(rec[0]));
					j++;
				}
				else
					l.add(userCursor);
				//ObjectId
				if(hasObjectId && hasUserId){
					l.add(CommonUtils.objectToInteger(rec[1]));
					j++;
				}
				else
					l.add(i);
				boolean nullFound = false;
				int index = j;
				for (; j < rec.length; j++) {
					if(ignoreList.contains(j+2)){
						continue;
					}
					if(attributes[index].getType()==Attribute.LIST){
						String val = rec[j];
						val = val.substring(1,val.length()-1);
						String [] values = val.split(",");
						List valuesList = CommonUtils.getList(values.length);
						for (int k = 0; k < values.length; k++) {
							valuesList.add(values[k]);
						}
						l.add(valuesList);						
					}
					else if("?".equals(rec[j]) ){
							nullFound = true;	
							if(!allowNulls)
								break;
							l.add(null);
						}
					else
						l.add(rec[j]);
					index++;
				}
				if(!allowNulls && nullFound)
					continue;
				//Switch so that rating is on position 2
				l.add(2,l.get(targetAttribute));
				l.remove(targetAttribute+1);
				double r= CommonUtils.objectToDouble(l.get(2));
				if(r>max)
					max = r;
				if(r<min)
					min = r;
				l.set(2, r);
				userRecords.add(l);
				i++;
			}
			in.close();
			add = -min ;
			coef = 4/(max-min);
			this.userRecords = new List[1][userRecords.size()];
			for (int j = 0; j < this.userRecords[0].length; j++) {
				this.userRecords[0][j]=userRecords.get(j);				
			}
			fillRandomValues();
			List<Double> classes = CommonUtils.getList(5);
			for (int j = 0; j < userRecords.size(); j++) {
				List<Object> rec = userRecords.get(j);
				double r= CommonUtils.objectToDouble(rec.get(2));
				if(!classes.contains((r+add)*coef+1)){
					classes.add((r+add)*coef+1);					
				}
				//Setting new rating
				rec.set(2,(r+add)*coef+1);
			}
			this.classes = new double[classes.size()];
			for (int j = 0; j < this.classes.length; j++) {
				this.classes[j]=classes.get(j);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	protected void getDefs() {
		try {

			ignoreList = CommonUtils.getList();
			BufferedReader in = new BufferedReader(
					new FileReader(file + ".def"));
			String line;
			line = in.readLine();
			line = in.readLine();
			int i = 2;
			List<Attribute> attrs = CommonUtils.getList();
			Attribute attr = new Attribute();
			attr.setIndex(0);
			attr.setName("userId");
			attr.setType(Attribute.NUMERICAL);
			attrs.add(attr);
			attr = new Attribute();
			attr.setIndex(1);
			attr.setName("objectId");
			attr.setType(Attribute.NUMERICAL);
			attrs.add(attr);
			int attrCount = 2;
			while ((line = in.readLine()) != null) {
				String[] attrProp = line.split(";");
				if ("I".equals(attrProp[0])) {
					ignoreList.add(i);
					i++;
					continue;
				}
				if(attrProp[1].equals("userId")){
					hasUserId = true;
					continue;
				}
				if(attrProp[1].equals("objectId")){
					hasObjectId = true;
					continue;
				}
				attr = new Attribute();
				attr.setIndex(attrCount);
				attr.setName(attrProp[1]);
				if ("O".equals(attrProp[0])) {
					attr.setType(Attribute.NUMERICAL);
				} else if ("N".equals(attrProp[0])) {
					attr.setType(Attribute.NOMINAL);
				}else if ("L".equals(attrProp[0])) {
					attr.setType(Attribute.LIST);
				} else if ("R".equals(attrProp[0])) {
					attr.setType(Attribute.NUMERICAL);
					targetAttribute = attrCount;
				}
				attrs.add(attr);
				i++;
				attrCount++;
			}
			//Switch so that rating is on position 2
			attrs.add(2,attrs.get(targetAttribute));
			attrs.remove(targetAttribute+1);
			in.close();
			attributes = new Attribute[attrs.size()];
			attrs.toArray(attributes);
			for (int j = 0; j < attributes.length; j++) {
				attributes[j].setIndex(j);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public String getName() {
		return "TH"+file.substring(file.lastIndexOf('\\')+1);
	}

	public void configDataSource(XMLConfiguration config, String section) {
		Configuration dsConf = config.configurationAt(section);
		maxLoad = CommonUtils.getIntFromConfIfNotNull(dsConf, "maxLoad", maxLoad);
		maxSize = CommonUtils.getIntFromConfIfNotNull(dsConf, "maxSize", maxSize);
		
		file = CommonUtils.getFromConfIfNotNull(dsConf, "file", file);
		if(dsConf.containsKey("file")){
			getDefs();
			getData();
		}
	}

	/*public void fillRandomValues() {
		Random rand = new Random();
		for (int i = 0; i < userRecords[0].length; i++) {
			List<Object> l = userRecords[0][i];
			l.add(rand.nextDouble());			
		}
		RandomComparator comp = new RandomComparator(attributes.length);
		Arrays.sort(userRecords[0],comp);
		for (int j = 0; j < userRecords[0].length; j++) {
			List<Object> rec = userRecords[0][j];
			//Removing the random column
			rec.remove(attributes.length);		
			//Setting new objectid
			rec.set(1, j);
		}
		
	}*/
	/*if(userRecords.size() > size)
	userRecords = userRecords.subList(0, size);*/
	
	


	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public void setFixedUserId(Integer value) {
		;
	}

	public Integer getUserId() {
		if (userIdSent)
			return null;
		userIdSent = true;
		return userCursor;
	}

	public void restartUserId() {
		userIdSent = false;
	}


	public void setAttributes(Attribute[] attributes) {
		this.attributes = attributes;
		this.names = null;
	}
	public void setClasses(double[] classes) {
		this.classes = classes;
	}

	public int getTargetAttribute() {
		return 2;
	}

}
