package prefwork.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.configuration.XMLConfiguration;

//TODO - do the actual computation of precision and recall statistics.
public class PrecisionRecallResults extends TestInterpreter {
	String path;

	String testParams = "";
	String testParamsHeader = "";

	double[] classes = null;
	TestResults testResults;
	
	@SuppressWarnings("unchecked")
	public String getAllResults(Stats stat, int run) {
		StringBuilder out = new StringBuilder();
			Set<Entry<Integer, Double[]>> set = stat.getSet();
			if (set == null || set.size() <= 0){
				out.append("0;0;0;0;0;0;0;");
				return "";
			}
			CompareEntry c1 = new CompareEntry();
			c1.index = 0;
			Entry<Integer, Double[]>[] array1 = new Entry[set.size()];
			set.toArray(array1);
			//java.util.Arrays.sort(array1, c1);
			
			for(double cut:classes){
				FFTable table = new FFTable();
				for (Entry<Integer, Double[]> entry : array1) {
						addResult(entry,cut,table, false);
				}
				table.computeStats();
	
				out.append(rowPrefix + stat.userId + ";"  );
				out.append(cut+";"+table.a+";"+table.b+";"+table.c+";"+table.d+";"+table.precision+";"+table.recall+";"+table.Fmeasure+";"+stat.countTest+";");
				out.append("\n");
			}
			
		return out.toString();
	}
	private boolean hit(double cut, double res){
		//if(res-cut<1 && res>=cut)
		if(Math.abs(res-cut)<=0.5)
			return true;
		return false;
	}
	private void addResult(Entry<Integer, Double[]> entry, double cut, FFTable table, boolean substract){
		boolean hitR = hit(cut,entry.getValue()[0]),hitM = hit(cut,entry.getValue()[1]);
		if(hitR && hitM){
			table.a++;
		}
		if(hitR && !hitM){
			table.b++;
		}
		if(!hitR && hitM){
			table.c++;
		}
		if(!hitR && !hitM){
			table.d++;
		}
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
				out.write(headerPrefix+"userId;cut;a;b;c;d;precision;recall;Fmeasure;countTest;");
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
