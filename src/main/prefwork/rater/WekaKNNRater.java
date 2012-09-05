package prefwork.rater;

import java.lang.reflect.Constructor;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.NearestNeighbourSearch;

public class WekaKNNRater extends WekaRater {

	String knnName = "weka.core.neighboursearch.LinearNNSearch";
	NearestNeighbourSearch knnModel = (NearestNeighbourSearch) new weka.core.neighboursearch.LinearNNSearch();
	int N = 10;

	public String toString(){
		return "WekaKNN"+knnModel.getClass().getName();
	}
	@SuppressWarnings("unchecked")
	@Override
	public void configClassifier(XMLConfiguration config, String section) {
		super.configClassifier(config, section);
		Configuration methodConf = config.configurationAt(section);
		try {
			knnName = CommonUtils.getFromConfIfNotNull(methodConf, "knn", knnName);
			Class c = Class.forName(knnName);
			Constructor[] a = c.getConstructors();
			for(Constructor con : a){
				if(con.getParameterTypes().length==0){
					knnModel = (NearestNeighbourSearch) con.newInstance();							
				}
			}
		}catch (Exception e) {}
		N = CommonUtils.getIntFromConfIfNotNull(methodConf, "N", N);
	}

	@Override
	public Double getRating(Double[] ratings, AttributeValue[] record) {
		try {
			Instances neighbors = knnModel.kNearestNeighbours(getWekaInstance(ratings),N);
			double res=0.0, div = 0.0;
			double[] distances = knnModel.getDistances();

			
			for(int i=0;i<neighbors.numInstances();i++){
				Instance inst = neighbors.instance(i);
				res+=(1/distances[i])*inst.classValue();/*;*/
				div+=(1/distances[i]);
			}
			return res/div;
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return null;
		
	}

	public void init(Attribute[] attributes) {
		loadAttributes(attributes);
		try {
			knnModel.setInstances(isTrainingSet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
