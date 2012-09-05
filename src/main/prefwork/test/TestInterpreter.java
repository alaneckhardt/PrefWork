package prefwork.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;


/**
 * TestIntepreter works with results of an inductive method. Results are interpreted in a form of some error measures. 
 * These measures are written into a csv file. Each row contain error values for a method, a dataset - train and test set and a user.
 * @author Alan
 *
 */
public abstract class TestInterpreter {

	protected String filePrefix;
	protected String rowPrefix;

	protected String headerPrefix;
	
	/**
	 * Writes ratings into a csv file.
	 * @param testResults
	 */
	synchronized public void writeRawRatings(TestResults testResults, String fileName, String header, String rowStart) {
		try {
			testResults.processResults();
			File f = new File(fileName + ".csv");
			BufferedWriter out;
			if (!f.exists()) {
				out = new BufferedWriter(new FileWriter(fileName + ".csv",
						true));
				out
						.write(header
								+ "userId;run;objectId;userRating;methodRating \n");
			} else
				out = new BufferedWriter(new FileWriter(fileName + ".csv",
						true));

			for (Integer userId : testResults.getUsers()) {
				List<Stats> l = testResults.getListStats(userId);
				for (int i = 0; i < l.size(); i++) {
					int run = i;
					Stats stat = testResults.getStatNoAdd(userId, run);
					if (stat == null)
						continue;
					for (Integer key : stat.ratings.keySet()) {
						Double[]ratings = stat.ratings.get(key);
						out.write((rowStart + userId + ";" + run + ";" + key+ ";"+ratings[0]+";"+ratings[1]+ "\n")
								.replace('.', ','));
					}

					for (Integer key : stat.unableToPredict.keySet()) {
						Double rating = stat.unableToPredict.get(key);
						out.write((rowStart + userId + ";" + run + ";" + key+ ";"+ rating +";"+"null"+ "\n")
								.replace('.', ','));
					}

				}
			}
			out.flush();
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 *  Writes some statistics given the test results data.
	 * @param stats Results of testing of a procedure.
	 * @param userId Id of the tested user.
	 * 
	 */
	public abstract void writeTestResults(TestResults testResults);

	/**
	 * Configurates the results interpreter.
	 * @param config XMLConfiguration
	 * @param section In which path in XML the current instance is.
	 */
	public abstract void configTestInterpreter(XMLConfiguration config, String section);
	
	
	/** 
	 * Sets the prefix that identifies current test results
	 * @param path
	 */
	public void setFilePrefix(String prefix) {
		this.filePrefix = prefix;		
	}
	

	
	/** 
	 * Gets the prefix that identifies current test results
	 * @param path
	 */
	public String getFilePrefix() {
		return filePrefix;		
	}

	/** 
	 * Gets the prefix that preceeds the header line in the csv file
	 */
	public String getHeaderPrefix() {
		return headerPrefix;
	}
	
	/** 
	 * Sets the prefix that preceeds every line in the csv file
	 * @param path
	 */
	public void setRowPrefix(String prefix) {
		this.rowPrefix = prefix;		
	}

	/** 
	 * Sets the prefix that preceeds the header line in the csv file
	 * @param path
	 */
	public void setHeaderPrefix(String headerPrefix) {
		this.headerPrefix = headerPrefix;
	}
	

	public String getRowPrefix() {
		return rowPrefix;
	}

	
	
}