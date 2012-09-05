package prefwork.datasource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.CommonUtils;


public class THMultiUserDataSource extends THDataSource{
	protected boolean hasUserId = false;
	protected boolean hasObjectId = false;
	
	protected void getData() {
		try {
			maxLoad = 1000000;
			BufferedReader in = new BufferedReader(
					new FileReader(file + ".dat"));
			String line;
			int i = 0;
			double max = Double.MIN_VALUE,min = Double.MAX_VALUE;
			HashMap<Integer,List<List<Object>>> userRecords = new HashMap<Integer, List<List<Object>>>();
			while ((line = in.readLine()) != null && i < maxLoad) {
				String[] rec = line.split(";");
				if(rec.length<2)
					continue;
				List<Object> l = CommonUtils.getList();
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
				for (; j < rec.length; j++) {
					if(ignoreList.contains(j+2))
						continue;
					l.add(rec[j]);
					if("?".equals(rec[j])){
						nullFound = true;					
						break;
					}
				}
				if(nullFound)
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
				if(!userRecords.containsKey(CommonUtils.objectToInteger(l.get(0))))
						userRecords.put(CommonUtils.objectToInteger(l.get(0)), CommonUtils.getListList());
				
				userRecords.get(CommonUtils.objectToInteger(l.get(0))).add(l);
				i++;
			}
			in.close();
			add = -min ;
			coef = 4/(max-min);
			this.userRecords = new List[userRecords.size()][];
			int j = 0;
			for (Integer userId : userRecords.keySet()) {
				this.userRecords[j]=new List[userRecords.get(userId).size()];
				for (int j2 = 0; j2 < this.userRecords[j].length; j2++) {
					this.userRecords[j][j2]=userRecords.get(userId).get(j2);		
					this.userRecords[j][j2].set(0, j);				
				}				
				j++;
			}
			fillRandomValues();
			List<Double> classes = CommonUtils.getList();
				for (j = 0; j < this.userRecords.length; j++) {
					for (int j2 = 0; j2 < this.userRecords[j].length; j2++) {
						List<Object> rec = this.userRecords[j][j2];	
						double r= CommonUtils.objectToDouble(rec.get(2));
						if(!classes.contains((r+add)*coef+1)){
							classes.add((r+add)*coef+1);					
						}
						//Setting new rating
						rec.set(2,(r+add)*coef+1);
					}
				}
			
			this.classes = new double[classes.size()];
			for ( j = 0; j < this.classes.length; j++) {
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

	public void setFixedUserId(Integer value) {
		currentUser = value;
	}

	public Integer getUserId() {
		if (this.userCursor >= userCount)
			return null;
		currentUser = this.userCursor;
		this.userCursor++;
		return currentUser;
	}
	
	public void restartUserId() {	
		userCursor = 0;
		currentUser = 0;
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
