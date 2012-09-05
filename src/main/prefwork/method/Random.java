package prefwork.method;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.datasource.BasicDataSource;

public class Random implements InductiveMethod{

	private Long seed = 45468782313L;
	private double max,min;
	private java.util.Random r;

    public String toString(){
    	return "Random2";
    }
    

	/**
	 * Builds model for specified user.
	 */
	public int buildModel(BasicDataSource trainingDataset, Integer user) {
		r = new java.util.Random(seed);
    	trainingDataset.setFixedUserId(user);
        trainingDataset.restart();
        max = Double.MIN_VALUE;
        min = Double.MAX_VALUE;
        double []classes = trainingDataset.getClasses();
        for (int i = 0; i < classes.length; i++) {
        	if(max<classes[i])
        		max = classes[i];
        	if(min>classes[i])
        		min = classes[i];
		}
        /*while((rec = trainingDataset.getRecord())!= null) {
        	double r = CommonUtils.objectToDouble(rec.get(target));
        	if(max<r)
        		max = r;
        	if(min>r)
        		min = r;
        	length++;
        }		*/
        return 0;
	}

	/**
	 * 
	 */
	public Double classifyRecord(List<Object> record, Integer targetAttribute) {
		return min+(r.nextDouble()*(max-min));
	}

	public void configClassifier(XMLConfiguration config, String section) {
		
	}

}
