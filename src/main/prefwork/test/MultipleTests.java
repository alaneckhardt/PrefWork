package prefwork.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.CommonUtils;

public class  MultipleTests extends TestInterpreter{
	
	double[] classes = null;
	TestResults testResults;
	List<TestInterpreter> interpreters;
	@Override
	public void writeTestResults(TestResults testResults) {
		for(TestInterpreter tri : interpreters){
			tri.writeTestResults(testResults);
		}
	}


	@SuppressWarnings("unchecked")
	public TestInterpreter getTestInterpreter(XMLConfiguration config, String section,  int i) throws SecurityException, ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException{
		String testResultsInterpreterClass = config.getString(section+".testInterpreters.testInterpreter("+i+").class");
		Constructor[] a = Class.forName(testResultsInterpreterClass).getConstructors();
		TestInterpreter tri = (TestInterpreter) a[0].newInstance();
		tri.configTestInterpreter(config,  section+".testInterpreters.testInterpreter("+i+")");
		return tri;
	}

	public  void configTestInterpreter(XMLConfiguration config, String section){

		int testInterpreterId=0;
		interpreters = CommonUtils.getList();
		while(config.getProperty(section+ ".testInterpreters.testInterpreter("+testInterpreterId+").class") != null){
			try {
				interpreters.add(getTestInterpreter(config, section, testInterpreterId));
				testInterpreterId++;
			}  catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}

	/** 
	 * Sets the prefix that identifies current test results
	 * @param path
	 */
	public void setFilePrefix(String prefix) {
		this.filePrefix = prefix;		
		for(TestInterpreter tri : interpreters){
			tri.setFilePrefix(prefix+interpreters.indexOf(tri));
		}
	}

	/** 
	 * Sets the prefix that preceeds every line in the csv file
	 * @param path
	 */
	public void setRowPrefix(String prefix) {
		this.rowPrefix = prefix;		
		for(TestInterpreter tri : interpreters){
			tri.setRowPrefix(prefix);
		}
	}

	/** 
	 * Sets the prefix that preceeds the header line in the csv file
	 * @param path
	 */
	public void setHeaderPrefix(String headerPrefix) {
		this.headerPrefix = headerPrefix;
		for(TestInterpreter tri : interpreters){
			tri.setHeaderPrefix(headerPrefix);
		}
	}
	
}



