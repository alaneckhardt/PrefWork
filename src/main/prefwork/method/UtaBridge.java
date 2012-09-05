package prefwork.method;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.PropertyConfigurator;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;
import utanm.Alternative;
import utanm.BadInputException;
import utanm.Factory;
import utanm.UTAADJ;
import utanm.UTATask;

public class UtaBridge implements InductiveMethod {

	protected String pathExec = "C:\\data\\progs\\eclipseProjects2\\PrefWork\\";
	protected Attribute[] attributes;
	protected int countTrain = 0;
	protected double maxClass = 0;
	protected double max = 0.0;
	protected double[][] minmax;
	protected Map<Double, List<Integer>> ratings = new HashMap<Double, List<Integer>>();
	protected UTATask solution;
	protected int segments = 4;
	protected Integer userId;
	protected List<List<Object>> testSet=CommonUtils.getList();
	protected List<List<Object>> testSetNoPredict=CommonUtils.getList();
	protected boolean preparedForTesting = false;
	protected double[] classes;
	double mean = 0;
	protected int maxNonmonotonicityDegree = 2;
	protected String directory = "vw";
	public String toString() {
		return "UtaBridgeb"+maxNonmonotonicityDegree;
	}
	
	protected void getValues(BasicDataSource trainingDataset){

		List<Object> rec;
		minmax = new double[attributes.length][];
		for (int i = 3; i < attributes.length; i++) {
			minmax[i]=new double[2];
			minmax[i][0]=Double.MAX_VALUE;
			minmax[i][1]=Double.MIN_VALUE;
		}			
			while ((rec = trainingDataset.getRecord()) != null) {
				for (int i = 3; i < attributes.length; i++) {
					if(attributes[i].getType()==Attribute.NUMERICAL){
						double d = CommonUtils.objectToDouble(rec.get(i));
						if(d>minmax[i][1]){
							minmax[i][1]=d;
						}
						if(d<minmax[i][0]){
							minmax[i][0]=d;
						}
					} //else if(attributes[i].getType()==Attribute.NOMINAL){
						if(!attributes[i].contains(rec.get(i))){
							AttributeValue val = new AttributeValue(attributes[i],rec);
							attributes[i].addValue(val);
						}
					//}
				}			
			}
	}

	protected void writeRecord(BufferedWriter out, List<Object> rec) throws IOException{
		out.write("    <Alternative>\n"+
        "      <Name>"+rec.get(1)+"</Name>\n"+
        "      <CriteriaValues>\n");
		Double r = CommonUtils.objectToDouble(rec.get(2));
		if(maxClass<r)
			maxClass = r;
		if(!ratings.containsKey(r)){
			ratings.put(r, new ArrayList<Integer>());
		}
		ratings.get(r).add(CommonUtils.objectToInteger(rec.get(1)));
		for (int i = 3; i < attributes.length; i++) {
			if(minmax[i][0] == minmax[i][1] && attributes[i].getType()==Attribute.NUMERICAL)
				continue;
			if (attributes[i].getType() == Attribute.NOMINAL && attributes[i].getValues().size()==1)
				continue;

			if (attributes[i].getType() != Attribute.NUMERICAL && attributes[i].getType() != Attribute.NOMINAL) {
				continue;
			}
			String attrName = transform(attributes[i].getName());
			out.write("        <Criterion name=\""+attrName+"\">"+transform(rec.get(i).toString())+"</Criterion>\n");					
		}
        out.write("      </CriteriaValues>\n"+
        "    </Alternative>\n");
	}
	private void getAttributes(BasicDataSource trainingDataset, Integer user,
			BufferedWriter out) {
		List<Object> rec;
		maxClass = 0;
		ratings = new HashMap<Double, List<Integer>>();
		try {
			countTrain = 0;
			out.write("  <Alternatives>\n");
			while ((rec = trainingDataset.getRecord()) != null) {
				writeRecord(out, rec);
				countTrain++;
			}
			out.write("  </Alternatives>\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	protected void getPreferences(BufferedWriter out) {
		try {
			double rating = 0;
			out.write("  <StatedPreferences>\n");
			List<Entry> l = new ArrayList<Entry>(ratings.entrySet());
			Collections.sort(l, new EntryComparator());
			for(Entry e : l){
				Double r = CommonUtils.objectToDouble(e.getKey());
				rating+=r;
				int classIndex = 0;
				for (; classIndex < classes.length; classIndex++) {
					if(classes[classIndex]==r){
						break;
					}						
				}
				out.write("  <Rank order=\""+(classes.length-classIndex+1)+"\">\n");
				for(Integer i : ((List<Integer>)e.getValue())){
					out.write("    <Alternative>"+i+"</Alternative>\n");
							
				}
				out.write("  </Rank>\n");
			}
			mean = rating/l.size();
			out.write("  </StatedPreferences>\n");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	protected String transform(String in){
		String tmp = in;
		if(tmp.equals("name")){
			tmp = "myname";
		}
		tmp = tmp.replaceAll("'", "").toLowerCase();

		tmp = tmp.replaceAll(".csv", "");
		tmp = tmp.replaceAll("-", "");
		//tmp = tmp.replaceAll("\\.", "");
		tmp = tmp.replaceAll("\\\"", "");
		tmp = tmp.replaceAll("Ã©", "");
		tmp = tmp.replaceAll("ã½", "");
		tmp = tmp.replaceAll("\\\\", "");		
		if(tmp.length() > 100)
			tmp = tmp.substring(0,100);
		return tmp;
	}

	
	protected void writeHeader(BufferedWriter out) throws IOException {		
			out.write("");
			out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
			    "<UTATASK name=\"vw\">\n"+
			    "<TaskSettings>\n"+
			    "<!-- Used algorithm is not specified here-->\n"+
			    "    <Sigma>0.001</Sigma>\n"+
			    "    <BIGM>1000</BIGM>\n"+
			    "    <INEQ>0.001</INEQ>\n"+
			    "    <AlgorithmSetting type=\"UTAADJ\">\n"+
			    "      <MaxNonmonotonicityDegree>"+/*(attributes.length-3)*/maxNonmonotonicityDegree+"</MaxNonmonotonicityDegree>\n"+
			    "      <ObjectiveThreshold>0.02</ObjectiveThreshold>\n"+
			    "      <MinimumImprovementOfObjectiveByAdditionalDegree>0.00</MinimumImprovementOfObjectiveByAdditionalDegree>\n"+
			    "      <MissingValueTreatmentForCardinalAndNominalCriteria>assumeAverageValue</MissingValueTreatmentForCardinalAndNominalCriteria>"+
			    "    </AlgorithmSetting>\n"+
			    "    <!-- Include two forward slashes in the end-->\n"+
			    "    <OutputFolder>"+directory+"//</OutputFolder>\n"+
			    "</TaskSettings>\n"); 
	}
	
	protected int writeCriteria(BufferedWriter out, String[] attributeNames) throws IOException{
		out.write("<Criteria>\n");
		int count = 0;
		for (int i = 3; i < attributeNames.length; i++) {
			String attrName = transform(attributeNames[i]);
			
			if (attributes[i].getType() == Attribute.NUMERICAL) {	
				if(minmax[i][0] == minmax[i][1])
					continue;
				out.write("  <CardinalCriterion>\n"+
						  "    <Name>"+attrName+"</Name>\n"+					
				          "    <NumberOfSegments>"+Math.max(attributes[i].getValues().size()/3,segments)+"</NumberOfSegments>\n"+
				          "    <Shape>Gain</Shape>\n"+
						  "    <Min>"+minmax[i][0]+"</Min> \n"+	
						  "    <Max>"+minmax[i][1]+"</Max> \n"+	
						  "  </CardinalCriterion>\n");
				count++;
			}
	        
			if (attributes[i].getType() == Attribute.NOMINAL ) {	
				if (attributes[i].getValues().size()==1)
					continue;
				out.write("  <NominalCriterion>\n"+
						  "    <Name>"+attrName+"</Name>\n"+
		            	  "    <Values>\n");
				for(AttributeValue val : attributes[i].getValues()){		
					out.write("      <Value>"+transform(val.getValue().toString())+"</Value>\n");
				}	
		        out.write("    </Values>\n"+	
					      "  </NominalCriterion>\n");
		        count++;
			}
		}

		out.write("</Criteria>\n");
		return count;
	}
	
	protected String getFileName(String trainingDataset, Integer user){
		return pathExec
		+ transform( trainingDataset+ user + ".xml");
	}
	
	private void writeTest(){
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter("C:\\testset"+userId+".xml"));
			out.write("<Alternatives>\n");
			for(List<Object> rec : testSet){
				out.write("    <Alternative>\n"+
	            "      <Name>"+rec.get(1)+"</Name>\n"+
	            "      <CriteriaValues>\n");
				Double r = CommonUtils.objectToDouble(rec.get(2));
				if(maxClass<r)
					maxClass = r;
				if(!ratings.containsKey(r)){
					ratings.put(r, new ArrayList<Integer>());
				}
				ratings.get(r).add(CommonUtils.objectToInteger(rec.get(1)));
				for (int i = 3; i < attributes.length; i++) {
					if(minmax[i][0] == minmax[i][1] && attributes[i].getType()==Attribute.NUMERICAL)
						continue;
					if (attributes[i].getType() == Attribute.NOMINAL && attributes[i].getValues().size()==1)
						continue;
	
					if (attributes[i].getType() != Attribute.NUMERICAL && attributes[i].getType() != Attribute.NOMINAL) {
						continue;
					}
					String attrName = transform(attributes[i].getName());
					out.write("        <Criterion name=\""+attrName+"\">"+transform(rec.get(i).toString())+"</Criterion>\n");					
				}
	            out.write("      </CriteriaValues>\n"+
	            "    </Alternative>\n");
			}
			out.write("</Alternatives>");
			out.flush();
			out.close();
			
			
			out = new BufferedWriter(new FileWriter("C:\\testsetNoPredict"+userId+".xml"));
			out.write("<Alternatives>\n");
			for(List<Object> rec : testSetNoPredict){
				out.write("    <Alternative>\n"+
	            "      <Name>"+rec.get(1)+"</Name>\n"+
	            "      <CriteriaValues>\n");
				Double r = CommonUtils.objectToDouble(rec.get(2));
				if(maxClass<r)
					maxClass = r;
				if(!ratings.containsKey(r)){
					ratings.put(r, new ArrayList<Integer>());
				}
				ratings.get(r).add(CommonUtils.objectToInteger(rec.get(1)));
				for (int i = 3; i < attributes.length; i++) {
					if(minmax[i][0] == minmax[i][1] && attributes[i].getType()==Attribute.NUMERICAL)
						continue;
					if (attributes[i].getType() == Attribute.NOMINAL && attributes[i].getValues().size()==1)
						continue;
	
					if (attributes[i].getType() != Attribute.NUMERICAL && attributes[i].getType() != Attribute.NOMINAL) {
						continue;
					}
					String attrName = transform(attributes[i].getName());
					out.write("        <Criterion name=\""+attrName+"\">"+transform(rec.get(i).toString())+"</Criterion>\n");					
				}
	            out.write("      </CriteriaValues>\n"+
	            "    </Alternative>\n");
			}
			out.write("</Alternatives>");
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void clear(){
		//writeTest();
		solution = null;
		attributes= null;
		maxClass = 0;
		minmax = null;
		ratings = null;
		testSet = CommonUtils.getList();
		testSetNoPredict = CommonUtils.getList();
		preparedForTesting = false;
		mean = 0;
	}
	protected void cleanUp(){
		/*int max = Factory.getMaxTaskID();
	    for (int i = 0; i <= max; i++) {
	        Factory.removeTask(0);			
		}*/
		if(solution == null)
			return ;
	    File folder = new File(".//"+directory+"//");

	    for(File f : folder.listFiles())
	    	f.delete();
	    folder.delete();
	    //folder.mkdir();
	}

	public int buildModel(BasicDataSource trainingDataset, Integer user) {
		//writeTest();
		clear();
		userId = user;
		classes =trainingDataset.getClasses();
		Arrays.sort(classes);
		System.out.print("build");
	    Attribute[] dAttr = trainingDataset.getAttributes();
		attributes = new Attribute[dAttr.length];
		for (int i = 0; i < attributes.length; i++) {
			attributes[i]=dAttr[i].clone();
		}
		directory = "vw"+""+this.hashCode();

	    File folder = new File(".//"+directory+"//");
	    folder.mkdir();
		trainingDataset.setFixedUserId(user);
		trainingDataset.restart();
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(getFileName(trainingDataset.getName(), user)));
			writeHeader(out);
			String[] attributeNames = trainingDataset.getAttributesNames();
			getValues(trainingDataset);
			int count = writeCriteria(out, attributeNames);
			//We do not have enough data for all the attributes
			//We need at least two distinct value of preferences.
			if(count == 0){
				out.flush();
				out.close();
				solution = null;
		        System.out.print("end build "+countTrain+" \n");
				return countTrain;				
			}
			trainingDataset.restart();
			getAttributes(trainingDataset, user, out);
			getPreferences(out);

			out.write("</UTATASK>");
		    
		
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
	
	
        UTAADJ task  = new UTAADJ();
        try {
			solution =task.findBestFittingUTATask(getFileName(trainingDataset.getName(), user));
		} catch (BadInputException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    File f = new File(getFileName(trainingDataset.getName(), user));
	    f.delete();
        cleanUp();
        System.out.print("end build "+countTrain+" \n");
		return countTrain;
	}



	public Double classifyRecord(List<Object> record, Integer targetAttribute) {
		if(solution == null)
			return mean;
		// create a test alternative
		testSet.add(record);
        List<String> l = CommonUtils.getList(record.size());
        for (int i = 3; i < record.size(); i++) {
			if (attributes[i].getType() == Attribute.NUMERICAL && minmax[i][0] == minmax[i][1])
					continue;
			if (attributes[i].getType() == Attribute.NOMINAL && attributes[i].getValues().size()==1)
				continue;
			if (attributes[i].getType() != Attribute.NUMERICAL && attributes[i].getType() != Attribute.NOMINAL) {
				continue;
			}
			l.add(transform(record.get(i).toString()));
		}
        String criterionValues[] = new String[l.size()];
        criterionValues = l.toArray(criterionValues);

        String testTaskName = record.get(1).toString();
        if(!preparedForTesting){
        	solution.prepareForTesting();
        	preparedForTesting = true;
        }
        //append it to the model
        Alternative a = null;
        try {
        		a = solution.addTestAlternative(testTaskName, criterionValues);
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        // get alternative utility
		double r = a.getTotalUtility();
		if(Double.isInfinite(r) || Double.isNaN(r)){
			testSetNoPredict.add(record);
		}
		if(max<r)
			max = r;
        return r*maxClass;
	}


	public void configClassifier(XMLConfiguration config, String section) {
	}

	public static void main(String[] args) {
		// BasicConfigurator replaced with PropertyConfigurator.
		PropertyConfigurator.configure("log4j.properties");
		UtaBridge b = new UtaBridge();
		b.classifyRecord(null, 2);
	}
}
@SuppressWarnings("unchecked")
class EntryComparator<HashMap$Entry> implements Comparator<Entry>{

	@Override
	public int compare(Entry arg0, Entry arg1) {
		Entry o1=(Entry)arg0;
		Entry o2=(Entry)arg1;
		return -((Double)o1.getKey()).compareTo(((Double)o2.getKey()));
	}
	
}