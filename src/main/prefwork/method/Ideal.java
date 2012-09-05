package prefwork.method;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;

public class Ideal implements InductiveMethod{

    public String toString(){
    	return "Ideal";
    }
    

	/**
	 * Builds model for specified user.
	 */
	public int buildModel(BasicDataSource trainingDataset, Integer user) {
		return 0;
	}

	/**
	 * 
	 */
	public Double classifyRecord(List<Object> record, Integer targetAttribute) {
		return CommonUtils.objectToDouble(record.get(2));
	}

	public void configClassifier(XMLConfiguration config, String section) {
		
	}

}
