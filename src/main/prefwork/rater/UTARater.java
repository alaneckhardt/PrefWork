package prefwork.rater;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;
import prefwork.method.UtaBridge;
import utanm.Alternative;
import utanm.BadInputException;
import utanm.UTAADJ;
import utanm.UTATask;
import weka.classifiers.Classifier;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class UTARater extends UtaBridge implements Rater {

	// Create a weka classifier
	UTATask solution;
	Integer targetAttribute = 2;
	Attribute[] attributes;

	public String toString(){
		
		return "UtaRaterb2"+maxNonmonotonicityDegree;
	}
	@SuppressWarnings("unchecked")
	@Override
	public void configClassifier(XMLConfiguration config, String section) {
	}


	@Override
	public Double getRating(Double[] ratings, AttributeValue[] record) {
		if(solution == null)
			return null;
        List<String> l = CommonUtils.getList(ratings.length);
        for (int i = 3; i < record.length; i++) {
			if (minmax[i][0] == minmax[i][1])
				continue;
			l.add(""+ratings[i]);
		}
        String criterionValues[] = new String[l.size()];
        criterionValues = l.toArray(criterionValues);

        String testTaskName = "test";
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
			//testSetNoPredict.add(record);
			;
		}
		if(max<r)
			max = r;
        return r*maxClass;
	}
	protected void getClasses(){
		List<AttributeValue> list = attributes[2].getValues();
		classes = new double[list.size()];
		for (int i = 0; i < list.size(); i++) {
			classes[i] = CommonUtils.objectToDouble(list.get(i).getValue());
		}
	}

	protected void getValues(BasicDataSource trainingDataset){
		minmax = new double[attributes.length][];
		for (int i = 3; i < attributes.length; i++) {
			minmax[i]=new double[2];
			minmax[i][0]=Double.MAX_VALUE;
			minmax[i][1]=Double.MIN_VALUE;
		}			

		Attribute attr = attributes[3];
		for (AttributeValue attrVal : attr.getValues()) {
			for (List<Object> rec : attrVal.getRecords()) {
				for (int i = 3; i < attributes.length; i++) {
					double d = CommonUtils.objectToDouble(attributes[i].getNorm().normalize(rec));
					if(d>minmax[i][1]){
						minmax[i][1]=d;
					}
					if(d<minmax[i][0]){
						minmax[i][0]=d;
					}
					
				}			
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
			if(minmax[i][0] == minmax[i][1])
				continue;

			String attrName = transform(attributes[i].getName());
			double d = CommonUtils.objectToDouble(attributes[i].getNorm().normalize(rec));
			
			out.write("        <Criterion name=\""+attrName+"\">"+d+"</Criterion>\n");					
		}
        out.write("      </CriteriaValues>\n"+
        "    </Alternative>\n");
	}
	
	private void getAttributes(BasicDataSource trainingDataset, Integer user,
			BufferedWriter out) {
		maxClass = 0;
		ratings = new HashMap<Double, List<Integer>>();
		try {
			countTrain = 0;
			out.write("  <Alternatives>\n");
			Attribute attr = attributes[3];
			for (AttributeValue attrVal : attr.getValues()) {
				for (List<Object> rec : attrVal.getRecords()) {
					writeRecord(out, rec);
					countTrain++;
				}
			}
			out.write("  </Alternatives>\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	protected int writeCriteria(BufferedWriter out, String[] attributeNames) throws IOException{
		out.write("<Criteria>\n");
		int count = 0;
		for (int i = 3; i < attributeNames.length; i++) {
			String attrName = transform(attributeNames[i]);
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

		out.write("</Criteria>\n");
		return count;
	}
	
	@Override	
	public void init(Attribute[] attributes) {

		//writeTest();
		clear();
		this.attributes = attributes;
		userId = CommonUtils.objectToInteger(attributes[0].getValues().get(0).getValue());
		getClasses();
		Arrays.sort(classes);
	    Attribute[] dAttr = attributes;
		attributes = new Attribute[dAttr.length];
		for (int i = 0; i < attributes.length; i++) {
			attributes[i]=dAttr[i].clone();
		}

		directory = "vw"+""+this.hashCode();
	    File folder = new File(".//"+directory+"//");
	    folder.mkdir();
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(getFileName("UtaRater", userId)));
			writeHeader(out);
			String[] attributeNames = new String[attributes.length];
			for (int i = 0; i < attributeNames.length; i++) {
				attributeNames[i] = attributes[i].getName();
			}
			getValues(null);
			int j = 0;
			for (; j < minmax.length; j++) {
				if(minmax[j]!=null && minmax[j][0] != minmax[j][1])
					break;
			}
			int count = writeCriteria(out, attributeNames);
			//We do not have enough data for all the attributes
			//We need at least two distinct value of preferences.
			if(count == 0){
				out.flush();
				out.close();
				solution = null;
				return;				
			}
			getAttributes(null, null, out);
			getPreferences(out);

			out.write("</UTATASK>");
		    
		
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
        UTAADJ task  = new UTAADJ();
        try {
			solution =task.findBestFittingUTATask(getFileName("UtaRater", userId));
		} catch (BadInputException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    File f = new File(getFileName("UtaRater", userId));
	    f.delete();
        cleanUp();
	}
	@Override
	public double compareTo(Rater n) {
		// TODO Auto-generated method stub
		return 0;
	}

}
