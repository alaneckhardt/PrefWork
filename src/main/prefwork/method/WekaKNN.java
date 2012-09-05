package prefwork.method;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.NearestNeighbourSearch;

public class WekaKNN extends WekaBridge {

	String knnName = "weka.core.neighboursearch.LinearNNSearch";
	NearestNeighbourSearch knnModel = (NearestNeighbourSearch) new weka.core.neighboursearch.LinearNNSearch();
	HashMap<Integer,Double> ratings = new HashMap<Integer, Double> ();
	int N = 10;
	public String toString() {
		return knnModel.getClass().getName()+N+"R";
	}


	protected Instance getWekaInstance(List<Object> rec) {
		Instance iExample = super.getInstanceFromIndexes(rec);
		ratings.put(CommonUtils.objectToInteger(rec.get(1)), CommonUtils.objectToDouble(rec.get(2)));
		iExample.setDataset(isTrainingSet);
		return iExample;
	}
	
	public int buildModel(BasicDataSource trainingDataset, Integer user) {
		clear();
		init(trainingDataset);

		getAttributes(trainingDataset, user);
		trainingDataset.setFixedUserId(user);
		trainingDataset.restart();
		if (!trainingDataset.hasNextRecord())
			return 0;
		// Create an empty training set
		isTrainingSet = new Instances("Rel", fvWekaAttributes, 10);
		// Set class index
		isTrainingSet.setClassIndex(targetAttribute);

		List<Object> rec;
		int count = 0;
		while ((rec = trainingDataset.getRecord()) != null) {
			// record[0] - uzivatelID
			// record[1] - itemID
			// record[2] - rating
			// Create the instance

			// add the instance
			isTrainingSet.add(getWekaInstance(rec));
			count++;
		}
		try {
			knnModel.setInstances(isTrainingSet);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return count;
	}

	public Double classifyRecord(List<Object> record, Integer targetAttribute) {
		try {
			Instance instance = getWekaInstance(record);
			Instances neighbors = knnModel.kNearestNeighbours(instance,N);
			double res=0.0, div = 0.0;
			double[] distances = knnModel.getDistances();

			
			for(int i=0;i<neighbors.numInstances();i++){
				Instance inst = neighbors.instance(i);
				
				/*int j=0;
				for(;j<isTrainingSet.numInstances();j++)
					if(isTrainingSet.instance(j).value(1)==inst.value(1))
						break;*/
				res+=(1/distances[i])*ratings.get(new Integer(new Double(inst.value(0)).intValue()))/**inst.classValue()*/;/*;*/
				//res+=(1/distances[i])*inst.classValue();
				div+=(1/distances[i]);
			}
			return res/div;
		} catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}

	protected void init(BasicDataSource trainingDataset) {
		super.init(trainingDataset);
		indexes.add(0,1);
		indexes.remove(new Integer(2));
	}
	@SuppressWarnings("unchecked")
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

}
