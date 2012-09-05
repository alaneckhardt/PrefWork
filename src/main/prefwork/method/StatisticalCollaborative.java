package prefwork.method;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;
import prefwork.normalizer.Normalizer;
import prefwork.rater.WeightAverage;
import prefwork.test.TestTrainAbsoluteCollaborative;

public class StatisticalCollaborative implements InductiveMethod {
		int knn = 5;
		Attribute[] attributes;
		Statistical[] users;
		Statistical[][] usersSorted;
		boolean computeOthers = true;
		boolean computeSelf = true;
		HashMap<Integer,HashMap<Integer,Double>> ratings  = new HashMap<Integer,HashMap<Integer,Double>>();
		double[][] sim;
		double threshold = 0.8;
		public String toString() {
			return "StatCollNoClean"+computeOthers+computeSelf+threshold+","+knn;
		}

		public double compare(Statistical s1, Statistical s2){
			Normalizer[] n1 = s1.getNormalizers();
			Normalizer[] n2 = s2.getNormalizers();
			double similarity = 0;
			int count = 0;
			/*WeightAverage wa1 = (WeightAverage) s1.getRater();
			WeightAverage wa2 = (WeightAverage) s1.getRater();
			double[] w1=wa1.getWeights();
			double[] w2=wa2.getWeights();*/
			for (int k = 3; k < attributes.length; k++) {
				double s = n1[k].compareTo(n2[k]);
				if(Double.isInfinite(s) || Double.isNaN(s))
					continue;
				similarity += s;
				count++;
			}
			/*double s =s1.getRater().compareTo(s2.getRater());
			if(!Double.isInfinite(s) && !Double.isNaN(s)){
				similarity+=s;
				count+=1;
			}*/
			if(count == 0)
				return 0;
				
			return similarity/count;
		}
		
		public int buildModel(BasicDataSource trainingDataset, Integer user) {
			trainingDataset.restartUserId();
			attributes = trainingDataset.getAttributes();
			Integer userIdDataset;
			int count = 0;
			int i=0;
			List<Statistical> l = CommonUtils.getList();
			while( (userIdDataset= trainingDataset.getUserId())!=null){
				ratings.put(userIdDataset, new HashMap<Integer, Double>());
				trainingDataset.setFixedUserId(userIdDataset);
				trainingDataset.restart();
				if (!trainingDataset.hasNextRecord())
					continue; 
				Statistical s = new Statistical();
				s.setUserId(userIdDataset);
				//((WeightAverage)s.rater).setUseWeights(false);
				//((WeightAverage)s.rater).setMethodName("ALL_1");
				count += s.buildModel(trainingDataset, userIdDataset);

				trainingDataset.restart();
				List<Object> rec;
				while ((rec = trainingDataset.getRecord()) != null) {
					// record[0] - uzivatelID
					// record[1] - itemID
					// record[2] - rating
					ratings.get(userIdDataset).put(CommonUtils.objectToInteger(rec.get(1)), CommonUtils.objectToDouble(rec.get(2)));
					//count++;
				}
				l.add(s);	
				i++;
			}
			users = new Statistical[l.size()];
			l.toArray(users);
			//Avoid making sim = new array[][]
			if(sim != null){
				if(sim.length != users.length)
					sim = new double[users.length][users.length];
				else {
					for (int j = 0; j < sim.length; j++) {
						if(sim[j].length != users.length){
							sim = new double[users.length][users.length];
							break;
						}
					}
				}
				for (int j = 0; j < sim.length; j++) {
					for (int j2 = 0; j2 < sim.length; j2++) {
						sim[j][j2] = 0.0;						
					}
				}
			}
			else
				sim = new double[users.length][users.length];
			
		for (i = 0; i < users.length && i < TestTrainAbsoluteCollaborative.usersToTest; i++) {

			for (int j = 0; j < users.length; j++) {
				//Skipping symmetric similarities
				if(sim[i][j] != 0)
					continue;
				Statistical s1 = users[i];
				Statistical s2 = users[j];
				/*
				 * if(s1==s2) continue;
				 */
				double s = compare(s1, s2);
				sim[i][j] = s;
				sim[j][i] = s;
			}
		}
			UsersComp comp = new UsersComp();
			
			comp.userId = user;
			
			comp.st=this;
			usersSorted = new Statistical[users.length][];
			for (i = 0; i < users.length; i++) {
				comp.userId = i;
				usersSorted[i] = users.clone();
				Arrays.sort(usersSorted[i], comp);
			}
			return count;
		}

		public Double classifyRecord(List<Object> rec, Integer targetAttribute) {
			try {
				Integer objectId = CommonUtils.objectToInteger(rec.get(1));
				Integer userId = CommonUtils.objectToInteger(rec.get(0));
				int count = 0;
				double div = 0;
				double res = 0;
				if(computeSelf){
					div = 1;
					res = usersSorted[userId][0].classifyRecord(rec, targetAttribute);					
				}
				for (int i = 0; i < usersSorted.length && count < knn; i++) {
					if(sim[userId][usersSorted[userId][i].getUserId()]<threshold)
						break;
					Double compRes = ratings.get(usersSorted[userId][i].getUserId()).get(objectId);
					if(compRes == null && computeOthers){
						compRes = usersSorted[userId][i].classifyRecord(rec, targetAttribute);										
					}
					if (compRes == null || Double.isNaN(compRes) || Double.isInfinite(compRes))
						continue;	
					
					count++;
					div+=sim[userId][usersSorted[userId][i].getUserId()];					
					res+=sim[userId][usersSorted[userId][i].getUserId()]*compRes;
				}
				if(div == 0)
					return null;
				return res/div;	
			} catch (Exception e) {
			//	e.printStackTrace();
			}
			return null;
		}

		public void configClassifier(XMLConfiguration config, String section) {
			Configuration methodConf = config.configurationAt(section);
			computeOthers = CommonUtils.getBooleanFromConfIfNotNull(methodConf, "computeOthers", computeOthers);
			computeSelf = CommonUtils.getBooleanFromConfIfNotNull(methodConf, "computeSelf", computeSelf);
			knn = CommonUtils.getIntFromConfIfNotNull(methodConf, "knn", knn);
			threshold = CommonUtils.getDoubleFromConfIfNotNull(methodConf, "threshold", threshold);
			
		}


		
}
class UsersComp implements Comparator<Statistical>{
	int userId = 0;
	StatisticalCollaborative st;
	@Override
	public int compare(Statistical o1, Statistical o2) {
		/*double sim1 = st.compare(o1,st.users[userId]);
		double sim2 = st.compare(o2,st.users[userId]);*/
		double sim1 = st.sim[userId][o1.getUserId()];
		double sim2 = st.sim[userId][o2.getUserId()];
		return -Double.compare(sim1, sim2);
	}
	
}
