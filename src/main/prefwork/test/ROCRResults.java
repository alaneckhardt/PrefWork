package prefwork.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.configuration.XMLConfiguration;

/**
 * Class that computes ROC curve from given results. It uses splitting the results into two sets -
 * those with rating higher than x and those with lower rating.<br />
 * It measures, how correctly the classifier  separates the results into these two sets.<br />
 * The value of x corresponds with possible ratings in the dataset.
 * @author Alan
 *
 */
public class ROCRResults extends TestInterpreter {
	HashMap<Integer, Stats> userResults = new HashMap<Integer, Stats>();

	String path;

	/**
	 * Set of possible cuttings of rating values. Typically it will be the set of all ratings,
	 * but in case of continuous ratings, these may differ.
	 */
	int[] cuts = new int[]{0,1,2,3,4,5};
	
	String testParams = "";
	String testParamsHeader = "";

	double[] classes = null;
	TestResults testResults;
	
	@SuppressWarnings("unchecked")
	public String getAllResults(Stats stat, int run) {
		StringBuilder out = new StringBuilder();
			Set<Entry<Integer, Double[]>> set = stat.getSet();
			CompareEntry c1 = new CompareEntry();
			c1.index = 0;
			Entry<Integer, Double[]>[] array1 = new Entry[set.size()];
			set.toArray(array1);
			//java.util.Arrays.sort(array1, c1);
			
			if (set == null || set.size() <= 0){
				out.append("0;0;0;0;0;0;0;");
				return "";
			}
			FFTable table[] = new FFTable[cuts.length];
			for (int i = 0; i < table.length; i++) {
				table[i] = new FFTable();
			}
			for (Entry<Integer, Double[]> entry : array1) {
				for (int i = 0; i < table.length; i++) {
					computeCut(entry,cuts[i],table[i], false);	
				}				
			}
			for (int i = 0; i < table.length; i++) {
				if(table[i].b>0)
					table[i].count=table[i].b;
				else
					table[i].count=1;
				table[i].a=0;
				table[i].b=0;
				table[i].c=0;
				table[i].d=0;
			}
			//table.initTable(cut, array1);
			for (Entry<Integer, Double[]> entry : array1) {
				for (int i = 0; i < table.length; i++) {
					computeCut(entry,cuts[i],table[i], false);
					table[i].computeStats();

					out.append(rowPrefix + stat.userId + ";"+cuts[i]+";"  );
					out.append(""+table[i].a+";"+(0.0+table[i].b)/table[i].count+";");
					out.append("\n");
				}
					
				//out.append(table.precision+";");
				//out.append(table.recall+";");
				//out.append(table.FPR+";\n");
			}
			/*out.append(rowPrefix + stat.userId + ";"  + 0  + ";" + cut + ";" +table.a+";"+table.b+";"+table.c+";"+table.d+";");
				out.append(table.precision+";");
				out.append(table.recall+";");
				out.append(table.FPR+";\n");*/
			
		//}
		return out.toString();
	}
	
	private void computeCut(Entry<Integer, Double[]> entry, int cut, FFTable table, boolean substract){
		if(entry.getValue()[0]>=cut && entry.getValue()[1]>=cut){
			table.a++;
			if(substract)
				table.b--;
		}
		if(entry.getValue()[0]<cut && entry.getValue()[1]>=cut){
			if(!substract)
				table.b++;
		}
		if(entry.getValue()[0]>=cut && entry.getValue()[1]<cut){

			if(!substract)
				table.c++;
		}
		if(entry.getValue()[0]<cut && entry.getValue()[1]<cut){
			table.d++;
			if(substract)
				table.c--;
		}
	}
	
	@SuppressWarnings("unused")
	private String computeCuts(Stats stat, int run){
		String out = "";
		FFTable table = new FFTable();
		for(int cut : cuts){
			Set<Entry<Integer, Double[]>> set = stat.getSet();
			if (set == null || set.size() <= 0){
				out +="0;0;0;0;";
				continue;
			}
			for (Entry<Integer, Double[]> entry : set) {
				computeCut(entry,cut,table, false);
			}
			table.computeStats();
			out +=table.a+";"+table.b+";"+table.c+";"+table.d+";";
			out +=table.precision+";";
			out +=table.recall+";";
			out +=table.FPR+";";
		}
		return out;
	}


	@Override
	public void configTestInterpreter(XMLConfiguration config, String section) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void writeTestResults(TestResults testResults) {
		classes = testResults.getClasses();
		this.testResults = testResults;
		try{
			testResults.processResults();
			File f = new File(filePrefix+".csv");
			BufferedWriter out;
			if (!f.exists()) {
				out = new BufferedWriter(new FileWriter(filePrefix+".csv", true));
				//out.write(headerPrefix+"userId;rating;cut;a;b;c;d;precision;recall;FPR;\n");
				out.write(headerPrefix+"userId;cut;");
				out.write("cuta;cutb;");/*
				for (int i = 0; i < cuts.length; i++) {
					out.write("cut"+cuts[i]+"a;cut"+cuts[i]+"b;");										
				}*/
				out.write("\n");
			} else
				out = new BufferedWriter(new FileWriter(filePrefix+".csv", true));

			for(Integer userId :testResults.getUsers()){
				List<Stats> l = testResults.getListStats(userId);
				int run;
				for (int i = 0; i < l.size(); i++) {
					run = i;
					Stats stat = testResults.getStatNoAdd(userId, run);
					if(stat == null )
						continue;
					
					out.write(getAllResults(stat, run).replace('.', ','));
				}
				
			}
			out.flush();
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
