package prefwork.method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;

import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.impl.correlation.PearsonCorrelation;
import com.planetj.taste.impl.model.GenericDataModel;
import com.planetj.taste.impl.model.GenericItem;
import com.planetj.taste.impl.model.GenericPreference;
import com.planetj.taste.impl.model.GenericUser;
import com.planetj.taste.impl.neighborhood.NearestNUserNeighborhood;
import com.planetj.taste.impl.recommender.GenericUserBasedRecommender;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;
import com.planetj.taste.neighborhood.UserNeighborhood;
import com.planetj.taste.recommender.Recommender;

public class CofiBridge implements InductiveMethod {

		DataModel trainset  = null;
		Recommender recommender = null;
		int knn = 30;
		public String toString() {
			return "CofiBridge"+knn;
		}

		
		private ArrayList<User> processUsers(HashMap<String, ArrayList<Preference>> preferences){

			final ArrayList<User> users = new ArrayList<User>(preferences.size());
			for (final Map.Entry<String, ArrayList<Preference>> entries : preferences.entrySet()) {
				users.add(new GenericUser<String>(entries.getKey(), entries.getValue()));
			}
			return users;
		}

		public int buildModel(BasicDataSource trainingDataset, Integer user) {
			trainingDataset.restartUserId();
			Integer userIdDataset;
			int count = 0;
			HashMap<String, ArrayList<Preference>> preferences = new HashMap<String, ArrayList<Preference>>();
			while( (userIdDataset= trainingDataset.getUserId())!=null){
				trainingDataset.setFixedUserId(userIdDataset);
				trainingDataset.restart();
				if (!trainingDataset.hasNextRecord())
					continue; 
				
				List<Object> rec;
				while ((rec = trainingDataset.getRecord()) != null) {
					// record[0] - uzivatelID
					// record[1] - itemID
					// record[2] - rating
					// Create the instance
	
					// add the instance
					String userId = rec.get(0).toString();
					String objectId = rec.get(1).toString();
					String rating = rec.get(2).toString();
					if(!preferences.containsKey(userId)){
						preferences.put(userId, new ArrayList<Preference>());
					}
					ArrayList<Preference> pref = preferences.get(userId);
					pref.add(new GenericPreference(null , new GenericItem<String>(objectId), CommonUtils.objectToDouble(rating)));
					count++;
				}
			}
			ArrayList<User> users = processUsers(preferences);
			try {
				trainset = new GenericDataModel(users);
				UserCorrelation userCorrelation = new PearsonCorrelation(trainset);
				UserNeighborhood neighborhood =
					  new NearestNUserNeighborhood(knn, userCorrelation,  trainset);
				
				// Construct the list of pre-compted correlations
				/*Collection<GenericItemCorrelation.ItemItemCorrelation> correlations =
					  ...;
				ItemCorrelation itemCorrelation = new GenericItemCorrelation(correlations);
				recommender = new GenericItemBasedRecommender(trainset, itemCorrelation);*/
				recommender = new GenericUserBasedRecommender(trainset, neighborhood, userCorrelation);					
			} catch (Exception e) {
				e.printStackTrace();
			}
			return count;
		}

		public Double classifyRecord(List<Object> rec, Integer targetAttribute) {
			try {
				String userId = rec.get(0).toString();
				String objectId = rec.get(1).toString();
				return recommender.estimatePreference(userId, objectId);	
			} catch (Exception e) {
			//	e.printStackTrace();
			}
			return null;
		}

		public void configClassifier(XMLConfiguration config, String section) {
			Configuration methodConf = config.configurationAt(section);
			if (methodConf.containsKey("knn")) {
				knn = methodConf.getInt("knn");
			}
		}


}
