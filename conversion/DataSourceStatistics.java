package conversion;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.PropertyConfigurator;

import prefwork.Attribute;
import prefwork.datasource.BasicDataSource;

public class DataSourceStatistics {

	String pathOut = "C:\\data\\progs\\eclipseProjects\\PrefWork\\";
	String fileName = "sourcestatistics.csv";
	Attribute[] attributes;
	Map<Integer, List<Object>> objects;
	Map<Integer, List<Double>> ratings;

	public static String transform(String in){
		String tmp = in;
		if(tmp.equals("name")){
			tmp = "myname";
		}
		tmp = tmp.replaceAll("'", "").toLowerCase();

		tmp = tmp.replaceAll(".csv", "");
		tmp = tmp.replaceAll("-", "");
		tmp = tmp.replaceAll("\\.", "");
		tmp = tmp.replaceAll("\\\"", "");
		tmp = tmp.replaceAll("\\\\", "");		
		if(tmp.length() > 100)
			tmp = tmp.substring(0,100);
		return tmp;
	}
	
	public DataSourceStatistics(String fileName){
		this.fileName=transform(fileName);
	}
	@SuppressWarnings({ "unused", "unchecked" })
	private void writeStatistics(BasicDataSource trainingDataset,
			BufferedWriter out) {

		for (Entry e : objects.entrySet()) {
			List<Object> rec = (List<Object>) e.getValue();
			/*
			 * out.write((trainingDataset.getName()+";"+
			 * attributes[i].getName()+";"+ rec.get(i)+"\n").replace('.', ','));
			 */
		}

	}
	/*
	 * if(!ratings.containsKey(id)) ratings.put(id, new
	 * ArrayList<Double>());
	 * ratings.get(id).add(CommonUtils.objectToDouble
	 * (rec.get(2)));
	 */
	private void getUserStatistics(BasicDataSource trainingDataset,
			Integer user, BufferedWriter out) {
		List<Object> rec;
		trainingDataset.setFixedUserId(user);
		trainingDataset.restart();
		try {
			while ((rec = trainingDataset.getRecord()) != null) {
				out
				.write((trainingDataset.getName() + ";"
						+ attributes[2].getName() + ";"
						+ rec.get(2) + "\n").replace('.', ','));
				/*
				Integer id = CommonUtils.objectToInteger(rec.get(1));
				if (objects.containsKey(id)) {
					out
							.write((trainingDataset.getName() + ";"
									+ attributes[2].getName() + ";"
									+ rec.get(2) + "\n").replace('.', ','));
					continue;
				}
				for (int i = 0; i < attributes.length; i++) {
					objects.put(id, rec);
					if (attributes[i].getType() == Attribute.LIST) {
						List l = (List) rec.get(i);
						if(l==null)
							continue;
						for (Object o : l)
							out.write((trainingDataset.getName() + ";"
									+ attributes[i].getName() + ";" + o + "\n")
									.replace('.', ','));
					} else
						out
								.write((trainingDataset.getName() + ";"
										+ attributes[i].getName() + ";"
										+ rec.get(i) + "\n").replace('.', ','));

					
					if (!attributes[i].contains(rec.get(i)))
						attributes[i].addValue(rec);
					else
						attributes[i].getValue(rec.get(i)).addRecord(rec);
					
				}*/
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void getStatistics(BasicDataSource trainingDataset) {
		attributes = trainingDataset.getAttributes();
		objects = new HashMap<Integer, List<Object>>();
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(pathOut
					/*+ transform(trainingDataset.getName()) + fileName*/+"all" + ".csv",true));
			trainingDataset.restartUserId();
			Integer userId;
			while ((userId = trainingDataset.getUserId()) != null) {
				getUserStatistics(trainingDataset, userId, out);
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// BasicConfigurator replaced with PropertyConfigurator.
		PropertyConfigurator.configure("log4j.properties");
		@SuppressWarnings("unused")
		DataSourceStatistics b = new DataSourceStatistics("");
	}
}
