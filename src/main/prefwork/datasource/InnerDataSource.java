package prefwork.datasource;



import java.lang.reflect.Constructor;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;

public abstract class InnerDataSource implements BasicDataSource{

	BasicDataSource innerDataSource;
	// Max class in the inner dataset
	int maxClass;
	XMLConfiguration configDriver;
	String sectionDriver;

	public void configDataSource(XMLConfiguration config, String section) {
		innerDataSource.configDataSource(config, section);
		innerDataSource.setName(getName() + "inner");
	}

	@SuppressWarnings("unchecked")
	public void configDriver(XMLConfiguration config, String section) {
		Configuration dbConf = config.configurationAt(section);
		String dbClass = dbConf.getString("innerclass");
		if(dbClass== null)
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

	public boolean hasNextRecord() {
		return innerDataSource.hasNextRecord();
	}

	public void restart() {
		innerDataSource.restart();
	}

	public void restartUserId() {
		innerDataSource.restartUserId();
	}

	public void setFixedUserId(Integer value) {
		innerDataSource.setFixedUserId(value);
	}

	public void fillRandomValues() {
		innerDataSource.fillRandomValues();
	}

	public Attribute[] getAttributes() {
		return innerDataSource.getAttributes();
	}

	public String[] getAttributesNames() {
		return innerDataSource.getAttributesNames();
	}

	public double[] getClasses() {
		return innerDataSource.getClasses();
	}

	public void setLimit(Double fromPct, Double toPct, boolean recordsFromRange) {
		innerDataSource.setLimit(fromPct, toPct, recordsFromRange);
	}

	public int getTargetAttribute() {
		return innerDataSource.getTargetAttribute();
	}

	public Integer getUserId() {
		return innerDataSource.getUserId();
	}

	public void setName(String name) {
		innerDataSource.setName(name);
	}
}
