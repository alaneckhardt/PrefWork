package prefwork.method;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.PropertyConfigurator;

import prefwork.Attribute;
import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;

public class ProgolBridge implements InductiveMethod {

	String pathExec = "C:\\data\\progs\\ilp\\";
	String binaryName = "progol4.2.exe";
	Process p;
	BufferedReader stdOut;
	BufferedReader stdErr;
	PrintStream stdIn;
	List<Rule> rules = CommonUtils.getList();
	Attribute[] attributes;
	int countTrain = 0;
	float noise = 60;
	boolean monotonize = false;
	double mean;
	static boolean allowGeneralRules = true;
	
	public String toString() {
		return "ProgolBridge"+noise+monotonize;
	}

	@SuppressWarnings("unchecked")
	private void getAttributes(BasicDataSource trainingDataset, Integer user,
			BufferedWriter out) {
		List<Object> rec;
		double[] classes = trainingDataset.getClasses();
		Arrays.sort(classes);
		try {
			countTrain = 0;
			while ((rec = trainingDataset.getRecord()) != null) {
				mean+=CommonUtils.objectToDouble(rec.get(2));
				out.write("object(" + rec.get(1) + ").\n");

				//If the rating is not present, we skip this object.
				if(rec.get(2) == null)
					continue;
				// Write out the rating of the object
				if(monotonize){
					//out.write("rating(" + rec.get(1).toString().toLowerCase() + ", '" + rec.get(2) + "').\n");
					
					for (int i = 0; i < classes.length; i++) {
						if((CommonUtils.objectToDouble(rec.get(2)))<classes[i])
							out.write(":- rating(" + rec.get(1).toString().toLowerCase() + ", '" + classes[i]
									+ "').\n");
						else
							out.write("rating(" + rec.get(1).toString().toLowerCase() + ", '" + classes[i] + "').\n");
						
					}	
				}
				else {
					for (int i = 0; i < classes.length; i++) {
						if((CommonUtils.objectToDouble(rec.get(2))) != classes[i])
							out.write(":- rating(" + rec.get(1).toString().toLowerCase() + ", '" + classes[i]
									+ "').\n");
						else
							out.write("rating(" + rec.get(1).toString().toLowerCase() + ", '" + classes[i] + "').\n");
						
					}	
				}
				/*
				for (int i = 1; i <= 5; i++) {
					if (((Number) rec.get(2)).intValue() == i ||
							// If we monotonize, we add all values of ratings below the current as true.
							(monotonize && i <= ((Number) rec.get(2)).intValue())	) {
						out.write("rating(" + rec.get(1).toString().toLowerCase() + ", '" + i + "').\n");
					} 
					else {
						out.write(":- rating(" + rec.get(1).toString().toLowerCase() + ", '" + i
								+ "').\n");
					}

				}*/
				// Write out the attributes of the object
				for (int i = 3; i < rec.size(); i++) {
					if(rec.get(i) == null)
						continue;

					String attrName = attributes[i].getName().toLowerCase();
					
					attrName = transform(attrName);

					if( attributes[i].getType() == Attribute.NUMERICAL ){
						out.write( attrName + "(" + rec.get(1).toString()
								+ ", '" + rec.get(i).toString() + "').\n");
						out.write("const" + i + "('" + transform(rec.get(i).toString()) + "').\n");
					}
					else if (attributes[i].getType() == Attribute.NOMINAL || attributes[i].getType() == Attribute.COLOR) {
						out.write( attrName + "(" + transform(rec.get(1).toString())
								+ ", '" + transform(rec.get(i).toString()) + "').\n");
						out.write("const" + i + "('" + transform(rec.get(i).toString()) + "').\n");
					} else if (attributes[i].getType() == Attribute.LIST) {
						List<Object> l = (List<Object>) rec.get(i);
						for (Object o : l) {
							if(o == null)
								continue;
							out.write(attrName + "("
									+ rec.get(1).toString().toLowerCase() + ", '" + transform(o.toString()) + "').\n");
							out.write("const" + i + "('" + transform(o.toString()) + "').\n");
						}
					}
				}
				countTrain++;
			}
			mean/=countTrain;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected static String transform(String in){
		String tmp = in.trim();
		if(tmp.equals("name")){
			tmp = "myname";
		}
		tmp = tmp.replaceAll("'", "").toLowerCase();

		tmp = tmp.replaceAll(" ", "");
		tmp = tmp.replaceAll(".csv", "");
		tmp = tmp.replaceAll("-", "");
		tmp = tmp.replaceAll("\\.", "");
		tmp = tmp.replaceAll("\\\"", "");
		tmp = tmp.replaceAll("\\\\", "");		
		if(tmp.length() > 100)
			tmp = tmp.substring(0,100);
		return tmp;
	}

	
	protected void writeHeader(BasicDataSource trainingDataset, Integer user) {
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(pathExec + getFileName(trainingDataset, user) + ".pl"));
			out.write("");

			out
					.write(":- set(c,7), set(i,7), set(h,100), set(nodes,100000)?\n");
			out.write(":- set(verbose,0)?\n");
			out.write(":- set(noise,"+((int)noise)+")?\n");
			
			/*if(monotonize)
				out.write(":- set(posonly)?\n");*/
			
			out.write(":- modeh(1,rating(+object,#const2))?\n");
			String[] attributeNames = trainingDataset.getAttributesNames();
			for (int i = 3; i < attributeNames.length; i++) {
				String attrName = transform(attributeNames[i]);
				
				if (attributes[i].getType() == Attribute.NOMINAL || attributes[i].getType() == Attribute.NUMERICAL || attributes[i].getType() == Attribute.COLOR) {
					out.write(":- modeb(1," + attrName.toLowerCase() + "(+object,#const" + i
							+ "))?\n");
				}else if (attributes[i].getType() == Attribute.LIST) {
					out.write(":- modeb(*," + attrName.toLowerCase() + "(+object,#const" + i
							+ "))?\n");
				}
			}
			// Write possible classes
			double[] classes = trainingDataset.getClasses();
			Arrays.sort(classes);
			for (int i = 0; i < classes.length; i++) {
				out.write(":- rating('"+ classes[i] + "').\n");
			}
			out.write("\n");
			getAttributes(trainingDataset, user, out);

			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public int buildModel(BasicDataSource trainingDataset, Integer user) {
		rules = CommonUtils.getList();
		mean = 0;
		attributes = trainingDataset.getAttributes();
		trainingDataset.setFixedUserId(user);
		trainingDataset.restart();
		writeHeader(trainingDataset, user);

		loadILP(trainingDataset, user);
		return countTrain;
	}

	@SuppressWarnings("unused")
	private void writeRules(String inpath){
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(inpath + ".rules", true));
			for(Rule r : rules){
				out.write(r.head+" "+Arrays.deepToString(r.body)+"\n");
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private void parseResults(String inpath) {
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader(inpath));
			String line;
			while ((line = in.readLine()) != null) {
				if (!line.startsWith("rating("))
					continue;
				Rule rule = new Rule();
				rule.head = line.substring(0, line.indexOf(")") + 1);
				//We have at least some body
				if (rule.head.length() + 4 < line.length()) {
					String body = line.substring(rule.head.length() + 4);
					if (!body.endsWith(".")) {
						line = in.readLine();
						body += line.substring(1);
					}
					body = body.substring(0, body.length() - 1);
					rule.body = body.split(", ");
					rules.add(rule);
				}
				//General rule - only head with A but no body
				if(rule.head.charAt(7)==('A')){
					rules.add(rule);					
				}
			}
 			in.close();
			//writeRules(inpath);
			/*File f = new File(inpath);
			f.delete();*/
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getFileName(BasicDataSource trainingDataset, Integer userId){
		return  transform(trainingDataset.getName()+this.toString()+userId);
	}
	private void loadILP(BasicDataSource trainingDataset, Integer userId) {

		/*
		 * read("CProgol");
		 * 
		 * stdIn.write(("["+trainingDataset.getName()+"]? \n").getBytes());
		 * stdIn.flush(); read("[:- ["+trainingDataset.getName()+"]? - Time
		 * taken"); stdIn.write(("generalise(rating/2)? \n").getBytes());
		 * stdIn.flush(); read("[:- generalise(rating/2)? - Time taken");
		 */
		runProcess(trainingDataset, userId);
		parseResults(pathExec + getFileName(trainingDataset, userId)+".txt");

	}
	private void runProcess(BasicDataSource trainingDataset, Integer userId) {
		try {
			p = Runtime.getRuntime().exec(
					"cmd " 
					, null, new File(pathExec));
			p.getOutputStream().write((binaryName +" "+ getFileName(trainingDataset, userId)
							+ " > "+getFileName(trainingDataset, userId)+".txt 2>>pp.txt\n").getBytes());
			p.getOutputStream().flush();
			p.getOutputStream().write("exit\n".getBytes());
			p.getOutputStream().flush();
			
			p.waitFor();
			stdOut = new BufferedReader(new InputStreamReader(p
					.getInputStream()));
			stdErr = new BufferedReader(new InputStreamReader(p
					.getErrorStream()));
			stdIn = new PrintStream(new BufferedOutputStream(p
					.getOutputStream()), true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static String getValue(String body, int index) {
		String parameters = body.substring(body.indexOf('('));
		if (index == 0)
			parameters = parameters.substring(0, parameters.indexOf(','));
		else if (index == 1)
			parameters = parameters.substring(parameters.indexOf(',') + 1,
					parameters.length() - 1);
		if (parameters.startsWith("\'"))
			parameters = parameters.substring(1);
		if (parameters.endsWith("\'"))
			parameters = parameters.substring(0, parameters.length() - 1);
		return parameters;
	}

	@SuppressWarnings("unchecked")
	protected static boolean match(Rule r, List<Object> record, Attribute[] attributes) {
		if (r.body == null || r.body.length == 0) {
			// General rule for all records.
			if(allowGeneralRules)
				return true;
			else
				return false;
		}

		for (int i = 0; i < r.body.length; i++) {
			boolean found = false;
			for (int j = 0; j < attributes.length; j++) {
				String attrName = transform(attributes[j].getName());
				if (r.body[i].startsWith(attrName)) {

					if( attributes[j].getType() == Attribute.NUMERICAL ){
						if (record.get(j)!=null && record.get(j).toString().equals(
								getValue(r.body[i], 1))) {
							found = true;
							break;
						}
					}
					else if (attributes[j].getType() == Attribute.NOMINAL
							|| attributes[j].getType() == Attribute.COLOR) {
						if (record.get(j)!=null && transform(record.get(j).toString()).equals(
								getValue(r.body[i], 1))) {
							found = true;
							break;
						}
					}else if (attributes[j].getType() == Attribute.LIST) {
							List<Object> l = (List<Object>) record.get(j);
							if(l == null)
								continue;
							for (Object o : l) {
								if(o == null)
									continue;
								if (transform(o.toString()).equals(
										getValue(r.body[i], 1))) {
									found = true;
									break;
								}
							}
					}
				}
			}
			// We didn't find the corresponding value for this body element
			if (!found)
				return false;
		}
		return true;
	}


	public Double classifyRecord(List<Object> record, Integer targetAttribute) {
		double res = 0, max = -1;//
		@SuppressWarnings("unused")
		int count = 0;
		for (Rule r : rules) {
			if (match(r, record, attributes)) {
				res =CommonUtils.objectToDouble(getValue(r.head, 1));
				if(!monotonize)
					return  res;
				
				if(res>max)
					max = res;
				//count++;
			}
		}
		if(max!=-1)
			return max;
		return null;
	}	


	public void configClassifier(XMLConfiguration config, String section) {
		Configuration methodConf = config.configurationAt(section);
		if(methodConf.containsKey("noise"))
			noise = methodConf.getFloat("noise");
		else
			noise = 0;
		if(methodConf.containsKey("pathToProgol"))
			pathExec = methodConf.getString("pathToProgol");
		if(methodConf.containsKey("monotonize"))
			monotonize = methodConf.getBoolean("monotonize");
	}

	public static void main(String[] args) {
		// BasicConfigurator replaced with PropertyConfigurator.
		PropertyConfigurator.configure("log4j.properties");
		ProgolBridge b = new ProgolBridge();
		b.classifyRecord(null, 2);

	}
}

class Rule {
	String head;
	String[] body;
}
/*
 * Runtime now = Runtime.getRuntime(); // try { try { String line; /*
 * 
 * p = Runtime.getRuntime().exec("cmd", null, new File(pathExec));
 * BufferedReader stdOut = new BufferedReader(new InputStreamReader(p
 * .getInputStream())); BufferedReader stdErr = new BufferedReader(new
 * InputStreamReader(p .getErrorStream())); PrintStream stdIn = new
 * PrintStream(new BufferedOutputStream(p .getOutputStream()), true); read(
 * "Microsoft Windows"); stdIn.write("C:\\data\\progs\\ilp\\progol4.5.exe >p.txt
 * 2> pp.txt\n".getBytes()); stdIn.flush();
 * 
 * read( "|-"); stdIn.write("[animals]?\n".getBytes()); stdIn.flush(); read(
 * "generalise(class/1?"); stdIn.write("./progol \n".getBytes()); stdIn.flush();
 * read( "|-"); stdIn.write("consult('c:/install/ilp/aleph.pl').\n".getBytes());
 * stdIn.flush(); read( "true."); stdIn.write("read_all(train).\n".getBytes());
 * stdIn.flush(); read( "true"); stdIn.write(".\n".getBytes()); stdIn.flush();
 * read( ""); stdIn.write("induce.\n".getBytes()); stdIn.flush(); read(
 * "true."); stdIn.write("eastbound(west7).\n".getBytes()); stdIn.flush();
 * String res = getResult(stdOut, stdErr); res = "";
 * 
 * /*Process p = Runtime.getRuntime().exec(pathExec + "Progol421.exe", null, new
 * File(pathExec)); BufferedReader stdOut = new BufferedReader(new
 * InputStreamReader(p .getInputStream())); BufferedReader stdErr = new
 * BufferedReader(new InputStreamReader(p .getErrorStream())); PrintStream stdIn =
 * new PrintStream(new BufferedOutputStream(p .getOutputStream()), true);
 * stdIn.write("consult('c:/install/ilp/aleph.pl').\n".getBytes());
 * stdIn.flush(); read( "true."); stdIn.write("read_all(train).\n".getBytes());
 * stdIn.flush(); read( "true"); stdIn.write(".\n".getBytes()); stdIn.flush();
 * read( ""); stdIn.write("induce.\n".getBytes()); stdIn.flush(); read(
 * "true."); stdIn.write("eastbound(west7).\n".getBytes()); stdIn.flush();
 * String res = getResult(stdOut, stdErr); res = "";
 * 
 * 
 * 
 * 
 * Process pp = now .exec("c:\\F\\devel\\ilp\\SWI\\bin\\plcon.exe");
 */
/*
 * Process pp = now .exec("c:\\F\\devel\\ilp\\SWI\\bin\\plcon.exe",
 * "c:\\install\\ilp\\aleph.pl"}); // consult('c:/install/ilp/aleph.pl'). }
 * catch (Exception e) { e.printStackTrace(); } return 0.0D;
 */