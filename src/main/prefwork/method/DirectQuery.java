package prefwork.method;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;
import prefwork.datasource.MySQLConnectionProvider;
import prefwork.rater.WeightAverage;

public class DirectQuery implements InductiveMethod {


	private HashMap<Integer, List<Object>> originalRatings = new HashMap<Integer, List<Object>>();
	@SuppressWarnings("unused")
	private HashMap<Integer, Double> queryRatings = new HashMap<Integer, Double>();
	int userId;
	private Double smallRating = 0.0001D;
	
	WeightAverage rater;
	
	public DirectQuery(){
		rater = new WeightAverage();
		rater.setWeights(new double[]{1,1,1,1,1});
	}
	
	public String toString() {
		return "DirectQuery";
	}
	
	public void loadOriginalRatings(int userId){
		this.userId = userId;
		MySQLConnectionProvider provider = new MySQLConnectionProvider();
		provider.setDb("vidome");
		provider.setPassword("aaa");
		provider.setUserName("root");
		try {
			provider.connect();
			PreparedStatement origStatement = provider.getConn().prepareStatement("SELECT userid, notebookid, rating, hdd, display, price,  producer, ram  FROM note_userall where userid = " + userId);
			ResultSet orig = origStatement.executeQuery();
			while (orig.next() != false){
				List<Object> list = CommonUtils.getList(8);
				for(int i = 1; i <=8 ; i++)
					list.add(orig.getObject(i));
				originalRatings.put(orig.getInt(2), list);
			}
			transform();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void transform(){
		Set<Integer> keyset = originalRatings.keySet();
		for(Integer key : keyset){
			//TODO transform 
			/*
            <attribute><name>hdd</name><type>nominal</type></attribute>
            <attribute><name>display</name><type>nominal</type></attribute>
            <attribute><name>price</name><type>nominal</type></attribute>
            <attribute><name>producer</name><type>nominal</type></attribute>
            <attribute><name>ram</name><type>nominal</type></attribute>
            */
			List<Object> rec = originalRatings.get(key);
			Integer hdd = CommonUtils.objectToInteger(rec.get(3));
			switch(userId){
			case 1:
			case 2:
			case 3:
				if(hdd==400 || hdd == 30 || hdd == 60)
					rec.set(3, 5D);
				else
					rec.set(3, smallRating);
				break;
			case 4:
				if(hdd>=300)
					rec.set(3, 5D);
				else
					rec.set(3, smallRating);
				break;
			}
			
			Double display = CommonUtils.objectToDouble(rec.get(4).toString());
			switch(userId){
				case 1:
					if(display>12.1)
						rec.set(4, smallRating);
					else
						rec.set(4, 5D);
					break;
				case 2:
					if(display==20)
						rec.set(4, 5D);
					else
						rec.set(4, smallRating);
					break;
				case 3:
					if(display>=17)
						rec.set(4, 5D);
					else
						rec.set(4, smallRating);
					break;
				case 4:
					if(display>13.3)
						rec.set(4, smallRating);
					else
						rec.set(4, 5D);
					break;
			}
			
			Double price = CommonUtils.objectToDouble(rec.get(5));
			if(price>5)
				rec.set(5, smallRating);
			else
				rec.set(5, 5D);
			
			Integer ram = CommonUtils.objectToInteger(rec.get(7));			
				if(ram == 2000)
					rec.set(7, 5D);
				else
					rec.set(7, smallRating);

			String producer = rec.get(6).toString();	
			switch(userId){
			case 1:
				if(producer.equals("HP") ||
						producer.equals("FUJITSU") ||
						producer.equals("IBM")
						)
					rec.set(6, 5D);
				else
					rec.set(6, smallRating);
				break;
			case 2:
				if(producer.equals("ACER") ||
						producer.equals("TOSHIBA") 
						)
					rec.set(6, 5D);
				else
					rec.set(6, smallRating);
				break;
			case 3:
				if(producer.equals("HP") ||
						producer.equals("LENOVO")||
						producer.equals("TOSHIBA")  
						)
					rec.set(6, 5D);
				else
					rec.set(6, smallRating);
				break;
			case 4:
				if(producer.equals("HP") ||
						producer.equals("IBM")  ||
						producer.equals("MSI") 
						)
					rec.set(6, 5D);
				else
					rec.set(6, smallRating);
				break;
			}
			/*Double r = rater.getRating(new Double[]{ 
					(Double) rec.get(3),
					(Double) rec.get(4),
					(Double) rec.get(5),
					(Double) rec.get(6),
					(Double) rec.get(7)					
						});
			rec.set(2, r);*/
			int countZeros = 0;
			if((Double)rec.get(3)<1D )
				countZeros++;
			if((Double)rec.get(4)<1D )
				countZeros++;
			if((Double)rec.get(5)<1D )
				countZeros++;
			if((Double)rec.get(6)<1D )
				countZeros++;
			if((Double)rec.get(7)<1D )
				countZeros++;
			if(countZeros<3)
				rec.set(2, 5D);
			else
				rec.set(2, 0D);
			/*if(r<0.8)
				rec.set(2, smallRating);
			else
				rec.set(2, 5D);*/
			

		}
	}
	public Double getRating(Integer objectId){
		return (Double)originalRatings.get(objectId).get(2);
	}

	@Override
	public int buildModel(BasicDataSource trainingDataset, Integer user) {
		 loadOriginalRatings(user);
		 return 0;
	}

	@Override
	public Double classifyRecord(List<Object> record, Integer targetAttribute) {
		return getRating(CommonUtils.objectToInteger(record.get(1)));
	}

	@Override
	public void configClassifier(XMLConfiguration config, String section) {
		
	}
}
