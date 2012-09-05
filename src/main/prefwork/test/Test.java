package prefwork.test;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.datasource.BasicDataSource;
import prefwork.method.InductiveMethod;

public interface Test {
	public void configTest(XMLConfiguration config, String section);
	public void test(InductiveMethod ind, BasicDataSource trainDataSource, BasicDataSource testDataource);
	public TestInterpreter getResultsInterpreter();
}
