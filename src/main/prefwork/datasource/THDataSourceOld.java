package prefwork.datasource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.CommonUtils;


public class THDataSourceOld extends CacheDataSource{

	// Name of dataset, for statistics
	protected String name;

	// Index of attribute that contains rating
	protected int targetAttribute;

	// Name of random column
	protected String randomColumn = null;


	// Count of possible classes
	protected double[] classes = null;

	protected boolean userIdSent = false;

	/** Attributes of the dataset */
	protected Attribute[] attributes;
	String file;
	List<List<Object>> recordsList;
	int objectIndex = 0;
	double from, to;
	boolean fromRange;
	protected int randomIndexInList;
	protected List<Integer> ignoreList;
	protected int size = 200;
	protected int maxLoad = 5000;
	double coef = 0,add = 0;
	int userId = 1;
	protected List<Object> cloneList(List<Object> rec) {
		List<Object> l = CommonUtils.getList();
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
		return cloneList(recordsList.get(objectIndexTemp));
	}

	public String[] getAttributesNames() {
		String[] names = new String[attributes.length];
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
		
	}

	protected void getData() {
		try {
			BufferedReader in = new BufferedReader(
					new FileReader(file + ".dat"));
			String line;
			int i = 0;
			double max = Double.MIN_VALUE,min = Double.MAX_VALUE;
			recordsList = CommonUtils.getList();
			while ((line = in.readLine()) != null && i < maxLoad) {
				String[] rec = line.split(";");
				if(rec.length<2)
					continue;
				List<Object> l = CommonUtils.getList();
				//UserId
				l.add(userId);
				//ObjectId
				l.add(i);
				boolean nullFound = false;
				for (int j = 0; j < rec.length; j++) {
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
				recordsList.add(l);
				i++;
			}
			in.close();
			add = -min ;
			coef = 4/(max-min);
			fillRandomValues();
			List<Double> classes = CommonUtils.getList();
			for (int j = 0; j < recordsList.size(); j++) {
				List<Object> rec = recordsList.get(j);
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
	public String getName() {
		return "TH"+file.substring(file.lastIndexOf('\\'));
	}

	public void configDataSource(XMLConfiguration config, String section) {
		Configuration dsConf = config.configurationAt(section);
		file = CommonUtils.getFromConfIfNotNull(dsConf, "file", file);
		if(dsConf.containsKey("file")){
			getDefs();
			getData();
		}
	}

	public void fillRandomValues() {
		Random rand = new Random();
		for (int i = 0; i < recordsList.size(); i++) {
			List<Object> l = recordsList.get(i);
			l.add(rand.nextDouble());			
		}
		RandomComparator comp = new RandomComparator(attributes.length);
		Collections.sort(recordsList,comp);
		for (int j = 0; j < recordsList.size(); j++) {
			List<Object> rec = recordsList.get(j);
			//Removing the random column
			rec.remove(attributes.length);		
			//Setting new objectid
			rec.set(1, j);
		}
		/*if(recordsList.size() > size)
			recordsList = recordsList.subList(0, size);*/
		
	}

	public void setFixedUserId(Integer value) {
		;
	}

	public Integer getUserId() {
		if (userIdSent)
			return null;
		userIdSent = true;
		return userId;
	}

	public void restart() {
		objectIndex=0;
		objectIndex=getNextIndex();
	}

	public void restartUserId() {
		userIdSent = false;
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
		if (objectIndex >= recordsList.size())
			return objectIndex;
		int objectIndex = this.objectIndex;
		List<Object> l = recordsList.get(objectIndex);
		while (objectIndex < recordsList.size() - 1 && !satisfyCondition(l)) {
			objectIndex++;
			l = recordsList.get(objectIndex);
		}
		return objectIndex;
		
	}
	public boolean hasNextRecord() {
		if (objectIndex >= Math.min(recordsList.size(), size))
			return false;
		int objectIndex = getNextIndex();
		List<Object> l = recordsList.get(objectIndex);
		if (objectIndex > Math.min(recordsList.size(), size))
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

}
