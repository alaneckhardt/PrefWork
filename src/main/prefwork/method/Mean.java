package prefwork.method;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;

public class Mean implements InductiveMethod{

	private double mean = 0D;
	

    public String toString(){
    	return "Mean";
    }
    

	private void clear(){
		mean = 0D;
	}
	/**
	 * Builds model for specified user.
	 */
	public int buildModel(BasicDataSource trainingDataset, Integer user) {
		clear();
    	trainingDataset.setFixedUserId(user);
        trainingDataset.restart();
        int target = trainingDataset.getTargetAttribute();
        List<Object> rec;
        int length=0;
        while((rec = trainingDataset.getRecord())!= null) {
        	mean+=CommonUtils.objectToDouble(rec.get(target));
        	length++;
        }		
        mean/=length;
        return length;
	}

	/**
	 * 
	 */
	public Double classifyRecord(List<Object> record, Integer targetAttribute) {
		return mean;
	}

	public void configClassifier(XMLConfiguration config, String section) {
		
	}

}
