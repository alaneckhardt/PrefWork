package conversion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import prefwork.CommonUtils;
import prefwork.datasource.CSVDataSource;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class SortCSVFiles {
	static String path = "c:\\data\\datasets\\THSources\\";

	static List<String[]> recs;
	static HashMap<Double, List<List<Object>>> recsMap;
	

	public static void filterData(String inpath, String outpath) {
		try {
			recsMap = new HashMap<Double, List<List<Object>>>();
			CSVReader in =  new CSVReader(new FileReader(inpath),';');
			CSVWriter writer = new CSVWriter(new FileWriter(outpath), ';');	
			String s[];
			while ((s=in.readNext()) != null) {	
				Double d = CommonUtils.objectToDouble(s[10]);
				if(!recsMap.containsKey(d)){
					recsMap.put(d, new ArrayList<List<Object>>());
				}
				List<Object> l = new ArrayList();
				for (int i = 0; i < s.length; i++) {
					l.add(s[i]);
				}
				recsMap.get(d).add(l);
			}

			RandomComparator comp = new RandomComparator(10);

			for (Double d : recsMap.keySet()) {
				Random rand = new Random((int) (d * 1000));
				List<List<Object>> l = recsMap.get(d);
				for (int i = 0; i < l.size(); i++) {
					l.get(i).add(rand.nextDouble());

				}
				Collections.sort(recsMap.get(d), comp);
			}
			int zbytek = 0;
			//for (Double d : recsMap.keySet()) {
			for(Double d = 9.0;d>=0;d--){
				List<List<Object>> l = recsMap.get(d);
				for (int i = 0; i < l.size() && i < 20+zbytek ; i++) {
					String[] s2 = new String[l.get(i).size()-1];
					for (int j = 0; j < s2.length; j++) {
						s2[j] = l.get(i).get(j).toString();
					}
					writer.writeNext(s2);					
				}
				zbytek = Math.max(0,20+zbytek-l.size());
			}
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void transformData(String inpath, String outpath) {
		recs = CommonUtils.getList();
		try {
			CSVReader in =  new CSVReader(new FileReader(inpath),';');	
			CSVWriter writer = new CSVWriter(new FileWriter(outpath), ';');
			String s[];
			while ((s=in.readNext()) != null) {	
					recs.add(s);
			}
			in.close();
			final int i = 10;
			Collections.sort(recs, new Comparator<String[]>() {
				@Override
				public int compare(String[] o1, String[] o2) {
					double d1 = CommonUtils.objectToDouble(o1[i]);

					double d2 = CommonUtils.objectToDouble(o2[i]);
					//We want higher up.
					return -Double.compare(d1, d2);
				}
				
			});
			for (int j = 0; j < recs.size(); j++) {
				writer.writeNext(recs.get(j));
			}
			writer.flush();
			writer.close();
			replace(inpath, outpath);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static void replace(String inpath, String outpath) {
	try {

		BufferedReader in = new BufferedReader(new FileReader(inpath));
		BufferedWriter out = new BufferedWriter(new FileWriter(outpath));
		String s;
		while ((s=in.readLine()) != null) {
			out.write(s.replaceAll("\"", "")+"\n");
		}
		out.flush();
		out.close();
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}
	public static void main(String[] args) {
		//transformRatings(path+"sushi3b.5000.10.score",path+"sushiRatings.csv");
		//transformData(path+"sushi3.idata", path+"sushiData.csv");
		//insertData("sushiRatings", "userid, sushiid, rating" ,path+"sushiRatings.csv");
		filterData(path+"OKpoker\\poker-hand-all.data3.dat",path+"OKpoker\\poker-hand-all.data5.dat");
		
	}
}

class RandomComparator implements Comparator<List<Object>>{

	public RandomComparator(int randomizeIndex){
		this.randomizeIndex = randomizeIndex;
	}
	// Index of column with randomized variable
	protected int randomizeIndex;
	public int compare(List<Object> o1, List<Object> o2) {
		if(o1 == null || o1.get(randomizeIndex) == null)
			return -1;
		if(o2 == null || o2.get(randomizeIndex) == null)
			return 1;
		if(o1.get(randomizeIndex) == null)
			return 1;
		if(o2.get(randomizeIndex) == null)
			return -1;
		return -Double.compare(CommonUtils.objectToDouble(o1.get(randomizeIndex)),CommonUtils.objectToDouble(o2.get(randomizeIndex)));		
	}
}
