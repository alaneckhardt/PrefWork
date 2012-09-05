package prefwork;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.management.RuntimeErrorException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.normalizer.Normalizer;
import prefwork.test.TestInterpreter;

public final class CommonUtils {

	/*public static double roundToDecimals(double d, int c) {
		int temp = (int) ((d * Math.pow(10, c)));
		return (((double) temp) / Math.pow(10, c));
	}*/

	public static void cleanAttributeValue(AttributeValue val){
		val.setRatings(null);
		val.setRecords(null);
		
		
	}
	public static double roundToDecimals(double d, int decimalPlace){
		    // see the Javadoc about why we use a String in the constructor
		    // http://java.sun.com/j2se/1.5.0/docs/api/java/math/BigDecimal.html#BigDecimal(double)
		    try {
		    	BigDecimal bd = new BigDecimal(Double.toString(d));
			    bd = bd.setScale(decimalPlace,BigDecimal.ROUND_HALF_UP);
			    return bd.doubleValue();
				
			} catch (Exception e) {
				//System.err.println("d "+d+",place "+decimalPlace);
				//e.printStackTrace();
			}
		    
		    return 0;
		    
	}


	
	public static String getFromConfIfNotNull(Configuration dbConf, String key, String replacement){

		if(dbConf.containsKey(key))
			return dbConf.getString(key);
		else 
			return replacement;
	}
	

	public static int getIntFromConfIfNotNull(Configuration dbConf, String key, int replacement){

		if(dbConf.containsKey(key))
			return dbConf.getInt(key);
		else 
			return replacement;
	}

	public static boolean getBooleanFromConfIfNotNull(Configuration dbConf, String key, boolean replacement){
		if(dbConf.containsKey(key))
			return dbConf.getBoolean(key);
		else 
			return replacement;
	}
	
	

	/**
	 * Load data serialized in a file.
	 * @param fileName
	 * @return
	 */
	public static Object loadDataFromFile(String fileName){
		java.io.File f = new java.io.File(fileName);
		if(f.exists()){
			InputStream file;
			try {
				file = new FileInputStream(fileName);
				InputStream buffer = new BufferedInputStream(file,1000000);
				ObjectInputStream in = new ObjectInputStream(buffer);
				Object o= in.readObject();
				in.close();
				return o;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Serializes data into a file.
	 * @param fileName
	 * @param o
	 */
	public static void writeDataToFile(String fileName, Object o) {
		try {
			OutputStream file = new FileOutputStream(fileName);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutputStream out = new ObjectOutputStream(buffer);

			out.writeObject(o);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static double getDoubleFromConfIfNotNull(Configuration dbConf, String key, double replacement){

		if(dbConf.containsKey(key))
			return dbConf.getDouble(key);
		else 
			return replacement;
	}
	
	public static <T> List<T> getList(int capacity){
		return new ArrayList<T>(capacity);
	}
	public static <T> List<T> getList(){
		return new ArrayList<T>();
	}
	public static <T> List<List<T>> getListList(){
		return new ArrayList<List<T>>();
	}
	public static List<Object> getListFromValues(AttributeValue[] record){
		List<Object> list = getList();
		for(AttributeValue val : record){
			if(val == null)
				list.add(null);
			else
				list.add(val.getValue());
		}
		return list;
	}

	public static AttributeValue[]  getValuesFromList(List<Object> record, Attribute[]  attributes){
		AttributeValue[] rec = new AttributeValue[record.size()];
		for (int i = 0; i < rec.length; i++) {
			Object o = record.get(i);
			AttributeValue val = attributes[i].getValue(o);
			rec[i]=val;
		}
		return rec;
	}
	
	public static Attribute[] loadAttributes(XMLConfiguration config,
			String section) {
		Configuration dsConf = config.configurationAt(section);

		List<Attribute> attrs = CommonUtils.getList();
		int attrId = 0;
		// Iterate through attributes
		while (dsConf.getProperty("attributes.attribute(" + attrId + ").name") != null) {
			String attrName = dsConf.getString("attributes.attribute(" + attrId
					+ ").name");
			String attrType = dsConf.getString("attributes.attribute(" + attrId
					+ ").type");
			Attribute attr = new Attribute();
			attr.setIndex(attrId);
			attr.setName(attrName);
			if ("numerical".equals(attrType)) {
				attr.setType(Attribute.NUMERICAL);
			} else if ("nominal".equals(attrType)) {
				attr.setType(Attribute.NOMINAL);
			} else if ("list".equals(attrType)) {
				attr.setType(Attribute.LIST);
			} else if ("color".equals(attrType)) {
				attr.setType(Attribute.COLOR);
			}
			attrs.add(attr);
			attrId++;
		}
		Attribute[] attributes = new Attribute[attrs.size()];
		attrs.toArray(attributes);
		return attributes;
	}


	/**
	 * Returns object of given type. Constructor should be parameter-less.
	 * @param className
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Object getInstance(String className) {
		try {
			Class c = Class.forName(className);
			for (Constructor cons : c.getConstructors()) {
				if (cons.getParameterTypes() == null
						|| cons.getParameterTypes().length == 0) {
					return cons.newInstance();
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static double[] stringListToDoubleArray(List<String> list) {
		double[] array = new double[list.size()];
		for (int i = 0; i < list.size(); i++) {
			array[i] = Double.parseDouble(list.get(i));
		}
		return array;
	}
	
	public static int[] stringListToIntArray(List<String> list) {
		int[] array = new int[list.size()];
		for (int i = 0; i < list.size(); i++) {
			array[i] = Integer.parseInt(list.get(i));
		}
		return array;
	}
	
	/**
	 * Computes average of given list of double
	 * @param list
	 * @return
	 */
	public static double average(List<Double> list) {
		double avg = 0;
		if(list.size()==0)
			return 0;
		for (int i = 0; i < list.size(); i++) {
			if(!Double.isNaN(list.get(i)) && !Double.isInfinite(list.get(i)))
				avg += list.get(i);
		}
		return avg/list.size();
	}
	

	/**
	 * Computes average of given array of double
	 * @param list
	 * @return
	 */
	public static double average(Double[] list) {
		double avg = 0;
		if(list.length==0)
			return 0;
		for (int i = 0; i < list.length; i++) {
			if(!Double.isNaN(list[i]) && !Double.isInfinite(list[i]))
				avg += list[i];
		}
		return avg/list.length;
	}
	
	

	@SuppressWarnings("unchecked")
	public static TestInterpreter getTestInterpreter(XMLConfiguration config,
			String section) throws SecurityException, ClassNotFoundException,
			IllegalArgumentException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		String testResultsInterpreterClass = config.getString(section
				+ ".testInterpreter.class");
		Constructor[] a = Class.forName(testResultsInterpreterClass)
				.getConstructors();
		TestInterpreter tri = (TestInterpreter) a[0].newInstance();
		tri.configTestInterpreter(config, section + ".testInterpreter");
		return tri;
	}


	public static Double[] getLocalPref(List<Object> record, Attribute[] attributes){
		Double[] ratingsRecord = new Double[record.size()];
		for (int i = 0; i < ratingsRecord.length; i++) {
			Normalizer norm = attributes[i].getNorm();
			ratingsRecord[i] = norm.normalize(record);
		}
		return ratingsRecord;
	}
	public static int objectToInteger(Object number) {
		if (number instanceof Number)
			((Number) number).intValue();

		if (number instanceof BigDecimal)
			return ((BigDecimal) number).intValueExact();
		if (number instanceof Integer)
			return (Integer) number;
		try{
			String s=number.toString();
			if(s.length()==0)
				return 0;
			int d = Integer.parseInt(s);
			return d;
		}catch(Exception e){}
		throw new RuntimeErrorException(null, "Unable to process "
				+ number.getClass().getCanonicalName()+". Value "+number);

	}

	public static long objectToLong(Object number) {
		if (number instanceof Number)
			((Number) number).longValue();

		if (number instanceof BigDecimal)
			return ((BigDecimal) number).longValueExact();
		if (number instanceof Long)
			return (Long) number;
		
		try{
			String s=number.toString();
			long d = Long.parseLong(s);
			return d;
		}catch(Exception e){}
		throw new RuntimeErrorException(null, "Unable to process "+number.getClass().getCanonicalName()+". Value "+number);

	}

	/**
	 * Transforms given object to Double
	 * @param number
	 * @return
	 */
	public static double objectToDouble(Object number) {
		if (number instanceof Double)
			return (Double) number;
		if (number instanceof Number)
			((Number) number).doubleValue();
		if (number instanceof BigDecimal)
			return ((BigDecimal) number).doubleValue();
		if (number instanceof Integer)
			return (Integer) number;
		if (number instanceof Float)
			return (Float) number;
		try{
			String s=number.toString();
			double d = Double.parseDouble(s.replace(',', '.'));
			return d;
		}catch(Exception e){
			try{
				String s=number.toString();
				double d = Double.parseDouble(s);
				return d;
			}catch(Exception e1){				
			}
		}
		throw new RuntimeErrorException(null, "Unable to process "+number.getClass().getCanonicalName()+". Value "+number);
	}

	/**
	 * Closes given statement.
	 * @param stmt
	 * @param valuesSet
	 */
	public static void closeStatement(PreparedStatement stmt,
			ResultSet valuesSet) {
		try {
			if (valuesSet != null)
				valuesSet.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if (stmt != null)
				stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	

	/** An approximation to ln(gamma(x))*/
	public static double logGamma( double xx) {
		// define some constants...
		int j;
		double stp = 2.506628274650;
		double cof[] = new double[6];
		cof[0]=76.18009173;
		cof[1]=-86.50532033;
		cof[2]=24.01409822;
		cof[3]=-1.231739516;
		cof[4]=0.120858003E-02;
		cof[5]=-0.536382E-05;
		
		double x = xx-1;
		double tmp = x + 5.5;
		tmp = (x + 0.5)*Math.log(tmp) - tmp;
		double ser = 1;
		for(j=0;j<6;j++){
			x++;
			ser = ser + cof[j]/x;
		}
		double retVal = tmp + Math.log(stp*ser);
		return retVal;
	}

	/** An approximation of gamma(x)*/
	public static double gamma( double x) {
		double f = 10E99;
		double g = 1;
		if ( x > 0 ) {
			while (x < 3) {
				g = g * x;
				x = x + 1;
			}
			f = (1 - (2/(7*Math.pow(x,2))) * (1 - 2/(3*Math.pow(x,2))))/(30*Math.pow(x,2));
			f = (1-f)/(12*x) + x*(Math.log(x)-1);
			f = (Math.exp(f)/g)*Math.pow(2*Math.PI/x,0.5);
		}
		else {
			f = Double.POSITIVE_INFINITY;
		}
		return f;
	}  
	
	public static double student_c(double v) {
		// Coefficient appearing in Student's t distribution
		return Math.exp(logGamma( (v+1)/2)-logGamma(v/2)) / (Math.sqrt(Math.PI*v));
		
	}
	
	/** 	Student's t density with v degrees of freedom
	Requires gamma, student_c functions
	Part of Bryan's Java math classes (c) 1997
*/
	public static double student_tDen(double v, double t) {
		
		
		return student_c(v)*Math.pow( 1 + (t*t)/v, -0.5*(v+1) );
	}

	/** 	Student's t distribution with v degrees of freedom
		Requires gamma, student_c functions
		Part of Bryan's Java math classes (c) 1997
		This only uses compound trapezoid, pending a good integration package
		Returned value is P( x > t) for a r.v. x with v deg. freedom. 
		NOTE: With the gamma function supplied here, and the simple trapeziodal
		sum used for integration, the accuracy is only about 5 decimal places.
		Values below 0.00001 are returned as zero.
	*/
	public static double stDist(double v, double t) {
		
		
		double sm = 0.5;
		double u = 0;
		double sign = 1;
		double stepSize = t/5000;
		if ( t < 0) {
		 sign = -1;
		}
		for (u = 0; u <= (sign * t) ; u = u + stepSize) {
			sm = sm + stepSize * student_tDen( v, u);
		}
		if ( sign < 0 ) {
		 sm = 0.5 - sm;
		}
		else {
		 sm = 1 - sm;
		}
		if (sm < 0) {
		 sm = 0;		// do not allow probability less than zero from roundoff error
		}
		else if (sm > 1) {
		 sm = 1;		// do not allow probability more than one from roundoff error
		}
		return  sm ;
	}  
}
