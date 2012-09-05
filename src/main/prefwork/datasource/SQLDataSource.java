package prefwork.datasource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.CommonUtils;

/**
 * SQLDataSource provides connection to arbitrary SQL database. It contains
 * methods for querying for records and users. Every subclass must instantiate
 * provider in its constructor with a database provider.
 * 
 * @author Alan
 * 
 */
public abstract class SQLDataSource implements BasicDataSource {
	// Provider of connection to SQL datase
	protected SQLConnectionProvider provider;

	// Name of dataset, for statistics
	protected String name;

	// Index of attribute that contains rating
	protected int targetAttribute;

	// SQL select for users
	protected String usersSelect = null;

	// SQL select for records with ratings
	protected String recordsSelect = null;

	// Condition on random column
	protected String betweenCondition = null;

	// Name of random column
	protected String randomColumn = null;

	// Function used for generating random values. It is database dependent.
	protected String randomFunction = "rand()";

	// Name of table that contains records and ratings
	protected String recordsTable = null;

	// Name of column with user ids
	protected String userColumn;

	// Current user id
	protected Integer userID;

	// Name of table that contains random values (needed for update command)
	protected String randomColumnTable = null;

	// Attributes to fetch from db
	protected Attribute[] attributes;

	// Statement for records with ratings
	protected PreparedStatement recordsStatement;

	// Set of records with ratings
	protected ResultSet records;

	// Statement for users
	protected PreparedStatement usersStatement;

	// Set of users
	protected ResultSet users;
	
	// Count of possible classes
	protected double[] classes = null;

	public void configDriver(XMLConfiguration config, String section) {
		Configuration dbConf = config.configurationAt(section);
		provider.setDb(CommonUtils.getFromConfIfNotNull(dbConf, "db", provider.getDb()));
		provider.setPassword(CommonUtils.getFromConfIfNotNull(dbConf, "password", provider.getPassword()));
		provider.setUserName(CommonUtils.getFromConfIfNotNull(dbConf, "userName", provider.getUserName()));
		provider.setUrl(CommonUtils.getFromConfIfNotNull(dbConf, "url", provider.getUrl()));
		try {
			provider.connect();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean hasNextRecord() {
		try {
			// Try if we are at the end.
			records.getObject(1);
			return true;
		} catch (SQLException e) {
			// We are at the end of cursor, so we close it.
			try {
				records.close();
				if (recordsStatement != null)
					recordsStatement.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public void configDataSource(XMLConfiguration config, String section) {
		Configuration dsConf = config.configurationAt(section);

		if(dsConf.containsKey("attributes.attribute(" + 0 + ").name")){
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
			attributes = new Attribute[attrs.size()];
			attrs.toArray(attributes);
		}
		targetAttribute = CommonUtils.getIntFromConfIfNotNull(dsConf,"targetAttribute", targetAttribute);
		recordsTable = CommonUtils.getFromConfIfNotNull(dsConf, "recordsTable", recordsTable);
		randomColumn = CommonUtils.getFromConfIfNotNull(dsConf, "randomColumn", randomColumn);
		randomColumnTable = CommonUtils.getFromConfIfNotNull(dsConf, "randomColumnTable", randomColumnTable);
		userColumn = CommonUtils.getFromConfIfNotNull(dsConf, "userID", userColumn);
		
		if(dsConf.containsKey("classes")){
			List classesTemp = dsConf.getList("classes");
			classes = new double[classesTemp.size()]; 
			for (int i = 0; i < classesTemp.size(); i++) {
				classes[i]=CommonUtils.objectToDouble(classesTemp.get(i));
			}
		}
		if(dsConf.containsKey("usersSelect")){
			usersSelect = Arrays
			.deepToString(dsConf.getStringArray("usersSelect"))
			.substring(
					1,
					Arrays.deepToString(
							dsConf.getStringArray("usersSelect")).length() - 1);
		}
	}

	public void fillRandomValues() {
		String updateCommand = "update " + randomColumnTable + " set "
				+ randomColumn + "= " + randomFunction;
		try {
			PreparedStatement stat = provider.getConn().prepareStatement(
					updateCommand);
			stat.execute();
			provider.getConn().commit();
			stat.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public String[] getAttributesNames() {
		String[] names = new String[attributes.length];
		for (int i = 0; i < names.length; i++)
			names[i] = attributes[i].getName();
		return names;
	}

	public Attribute[] getAttributes() {
		return attributes;
	}


	public void setAttributes(Attribute[] attributes) {
		this.attributes = attributes;
	}

	@SuppressWarnings("unchecked")
	public double[] getClasses() {
		if(classes != null)
			return classes;
		/*return new double[]{5,4,3,2,1};*/
		String countSelect = "SELECT distinct "
				+ getAttributesNames()[targetAttribute] + " FROM "
				+ recordsTable;
		PreparedStatement stmt;
		try {
			stmt = provider.getConn().prepareStatement(countSelect);
			ResultSet rs = stmt.executeQuery();
			List classesList = CommonUtils.getList();
			while(rs.next()){
				classesList.add(rs.getObject(1));
			}
			classes = new double[classesList.size()];
			for(int i=0;i<classesList.size();i++){
				Object o = classesList.get(i);
				if(o instanceof Double)
					classes[i]=(Double)o;				
				else
					classes[i]=CommonUtils.objectToDouble(o);
									
			}
			Arrays.sort(classes);
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return classes;
	}

	public List<Object> getRecord() {
		if (!hasNextRecord())
			return null;

		List<Object> l = CommonUtils.getList(attributes.length);
		try {
			for (int i = 1; i <= attributes.length; i++)
				l.add(records.getObject(i));
			records.next();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		return l;
	}

	public int getTargetAttribute() {
		return targetAttribute;
	}

	public Integer getUserId() {
		try {
			if (users.next() == false)
				return null;

			int userId = users.getInt(1);
			return userId;
		} catch (Exception e) {
			e.printStackTrace();
			try {
				usersStatement.close();
				// We are at the end of users cursor, so we close it.
				users.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			return 0;
		}
	}

	public void restartUserId() {
		try {
			clearUsers();
			usersStatement = provider.getConn().prepareStatement(usersSelect);
			users = usersStatement.executeQuery();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	protected void clearUsers() {
		try{
			if (usersStatement != null)
				usersStatement.close();
			if (users != null)
				users.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	protected void clearRecords() {
		for (Attribute attr : attributes) {
			attr.setValues(null);
			attr.setVariance(null);
		}
		try {
			if (records != null)
				records.close();
			if (recordsStatement != null)
				recordsStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void restart() {
		try {
			clearRecords();
			// Getting rid of "[" and "]"
			recordsSelect = "SELECT "
					+ Arrays.toString(getAttributesNames()).substring(1,
							Arrays.toString(getAttributesNames()).length() - 1)
					+ " FROM " + recordsTable;
			if (betweenCondition != null) {
				recordsSelect += " WHERE " + betweenCondition;
			}
			if (userID != null) {
				// We add where clause, if it is not present
				if (betweenCondition == null
						|| !recordsSelect.endsWith(betweenCondition))
					recordsSelect += " WHERE ";
				// AND if where is already there
				else
					recordsSelect += " AND ";

				recordsSelect += userColumn + " = " + userID;
			}
			recordsStatement = provider.getConn().prepareStatement(
					recordsSelect);
			records = recordsStatement.executeQuery();
			records.next();
		} catch (Exception e) {
			System.err.println(recordsSelect);
			e.printStackTrace();
		}
	}

	public void setFixedUserId(Integer value) {
		userID = value;
	}
	


	public void setLimit(Double fromPct, Double toPct, boolean recordsFromRange) {
		
		String from = Double.toString(CommonUtils.roundToDecimals(fromPct, 5));
		String to = Double.toString(CommonUtils.roundToDecimals(toPct, 5));
		if (recordsFromRange)
			betweenCondition = (randomColumn + " >= " + from + " and "
					+ randomColumn + " < " + to);
		else
			betweenCondition = (randomColumn + " < " + from + " or "
					+ randomColumn + " >= " + to);
		betweenCondition = " (" + betweenCondition + ") ";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRandomColumn() {
		return randomColumn;
	}

	public void setRandomColumn(String randomColumn) {
		this.randomColumn = randomColumn;
	}


}