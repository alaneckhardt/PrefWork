package prefwork.datasource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.collections.functors.ForClosure;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import prefwork.Attribute;
import prefwork.CommonUtils;
import au.com.bytecode.opencsv.CSVReader;

public class IMDbMemory extends OracleMultiDataSource {


	Long IMDb = 0L;
	Long Oracle = 0L;
	Attribute[] allAttributes = null;
	private static Logger log = Logger.getLogger(IMDbMemory.class);
	String basePath = "D:\\data2\\datasets\\imdb2008mapped\\";
	protected static HashMap<Integer, String> IMDBPlotMovies;
	protected static HashMap<Integer, Integer> IMDBMapFromFlix;
	protected static List<HashMap<Integer, List<Integer>>> IMDBMaps = CommonUtils.getList();
	protected static HashMap<Integer, LaserDisc> IMDBLaserDiscs;

	protected String[] names;
	boolean getPlot = false;
	boolean getLaserDisc = false;
	// Set of predefined users
	protected List<Integer> usersList = CommonUtils.getList();
	protected boolean[] loadFile = {
			true,//"actorsRel.csv",
			true,//"certificatesRel.csv",
			false,//"IMDBCinematographMovies.csv",
			true,//"color-infoRel.csv",
			false,//"IMDBComposerMovies.csv",
			false,//"IMDBCountryMovies.csv",
			true,//"countriesRel.csv",
			true,//"directorsRel.csv",
			false,//"IMDBDistributorMovies.csv",
			true,//"editorsRel.csv",
			true,//"genresRel.csv",
			true,//"keywordsRel.csv",
			false,//"IMDBLanguageMovies.csv",
			false,//"IMDBLaserDiscMovies.csv",
			false,//"IMDBLocationMovies.csv",
			true,//"producersRel.csv",
			false,//"IMDBProductionDesignerMovies.csv",
			false,//"IMDBSoundMovies.csv",
			false,//"IMDBWriterMovies.csv",
			true //"plotRel.csv"
	};
	protected String[] files = {
		"actorsRel.csv",
		"certificatesRel.csv",
		"IMDBCinematographMovies.csv",
		"color-infoRel.csv",
		"IMDBComposerMovies.csv",
		"IMDBCountryMovies.csv",
		"countriesRel.csv",
		"directorsRel.csv",
		"IMDBDistributorMovies.csv",
		"editorsRel.csv",
		"genresRel.csv",
		"keywordsRel.csv",
		"IMDBLanguageMovies.csv",
		"IMDBLaserDiscMovies.csv",
		"IMDBLocationMovies.csv",
		"producersRel.csv",
		"IMDBProductionDesignerMovies.csv",
		"IMDBSoundMovies.csv",
		"IMDBWriterMovies.csv",
		"plotRel.csv"};

	public String getName() {
		return name+IMDBMaps.size()+(getPlot?"Plot":"")+(getLaserDisc?"Laser":"");
	}
	
	@SuppressWarnings("unchecked")
	private void load(String fileName, HashMap<Integer, List<Integer>> map){
		try {
			java.io.File f = new java.io.File(basePath + fileName + ".ser");
			if (f.exists()) {

				InputStream file = new FileInputStream(basePath + fileName
						+ ".ser");
				InputStream buffer = new BufferedInputStream(file);
				ObjectInputStream in = new ObjectInputStream(buffer);
				map.putAll((HashMap<Integer, List<Integer>>) in.readObject());
				in.close();
			} else {

				loadData(fileName, map);
				log.debug("Loaded file:" + fileName);

				try {
					OutputStream file = new FileOutputStream(basePath
							+ fileName + ".ser");
					OutputStream buffer = new BufferedOutputStream(file);
					ObjectOutputStream out = new ObjectOutputStream(buffer);
					out.writeObject(map);
					out.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void restart() {
		IMDb = 0L;
		Oracle = 0L;

		try {
			clearRecords();

			recordsSelect = "SELECT ";
			for (Attribute attr : attributes) {
				if (attr.getType() == Attribute.LIST || attr.getName().isEmpty())
					continue;
				recordsSelect += attr.getName() + " ,";
			}
			recordsSelect = recordsSelect.substring(0,
					recordsSelect.length() - 1);

			recordsSelect += " FROM " + recordsTable;
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
			e.printStackTrace();
		}
		
	}
	
	public IMDbMemory() {

		// We have already loaded the data.
		if (IMDBMaps != null || IMDBMaps.size() > 0)
			return;

		log.info("Start of loading");
		IMDBPlotMovies = new HashMap<Integer, String>();
		IMDBLaserDiscs = new HashMap<Integer, LaserDisc>();
		IMDBMapFromFlix = new HashMap<Integer, Integer>();

		loadLaserDiscs("laserFiltered.csv", IMDBLaserDiscs);
		
		for(int i = 0;i< loadFile.length;i++){
			if(loadFile[i] == false)
				continue;
			HashMap<Integer, List<Integer>> map = new HashMap<Integer, List<Integer>>(2000);
			load(files[i], map);
			IMDBMaps.add(map);
		}
		if(loadFile[loadFile.length -1] == true){
			loadPlotData("plotRel.csv","plotValues.csv", IMDBPlotMovies);
		}
		loadMapping();
		/*IMDBMaps.add(IMDBGenreMovies);
		load("genresRel.csv", IMDBGenreMovies);
		log.debug("Loaded file:"+"genresRel.csv");
		
		IMDBMaps.add(IMDBActorMovies);
		load("actressesRel.csv", IMDBActorMovies);
		log.debug("Loaded file:"+"actressesRel.csv");
		load("actorsRel.csv", IMDBActorMovies);
		log.debug("Loaded file:"+"actorsRel.csv");	
		
		IMDBMaps.add(IMDBDirectorMovies);
		load("directorsRel.csv", IMDBDirectorMovies);
		log.debug("Loaded file:"+"directorsRel.csv");
		
		IMDBMaps.add(IMDBCertificateMovies);
		load("certificatesRel.csv", IMDBCertificateMovies);
		log.debug("Loaded file:"+"certificatesRel.csv");
		
		IMDBMaps.add(IMDBCountryMovies);
		load("countriesRel.csv", IMDBCountryMovies);
		log.debug("Loaded file:"+"countriesRel.csv");
		IMDBMaps.add(IMDBEditorMovies);
		load("editorsRel.csv", IMDBEditorMovies);
		log.debug("Loaded file:"+"editorsRel.csv");
		IMDBMaps.add(IMDBKeywordMovies);
		load("keywordsRel.csv", IMDBKeywordMovies);
		log.debug("Loaded file:"+"keywordsRel.csv");
		
		IMDBMaps.add(IMDBProducerMovies);
		load("producersRel.csv", IMDBProducerMovies);
		IMDBMaps.add(IMDBColorMovies);
		load("color-infoRel.csv", IMDBColorMovies);
		log.debug("Loaded file:"+"color-infoRel.csv");

		IMDBMaps.add(IMDBDistributorMovies);
		 load("Rel.csv",IMDBDistributorMovies);
		IMDBMaps.add(IMDBCinematographMovies);
		 load("Rel.csv",IMDBCinematographMovies);
		IMDBMaps.add(IMDBComposerMovies);
		 load("Rel.csv",IMDBComposerMovies);
		IMDBMaps.add(IMDBCostumeDesignerMovies);
		 load("Rel.csv",IMDBCostumeDesignerMovies);
		IMDBMaps.add(IMDBLanguageMovies);
		 loadData("Rel.csv",IMDBLanguageMovies);
		IMDBMaps.add(IMDBLaserDiscMovies);
		 loadData("Rel.csv",IMDBLaserDiscMovies);
		IMDBMaps.add(IMDBLocationMovies);
		 loadData("Rel.csv",IMDBLocationMovies);
		IMDBMaps.add(IMDBProductionDesignerMovies);
		 loadData("Rel.csv",IMDBProductionDesignerMovies);
		IMDBMaps.add(IMDBSoundMovies);
		 loadData("Rel.csv",IMDBSoundMovies);
		IMDBMaps.add(IMDBWriterMovies);
		 loadData("Rel.csv",IMDBWriterMovies);*/
		
		
		loadPlotData("plotRel.csv","plotValues.csv", IMDBPlotMovies);
		loadMapping();
		
		log.info("End of loading");
	}

	public List<Object> getMovie(Integer flixMovieId) {
		List<Object> l = new ArrayList<Object>();
		Integer movieId = IMDBMapFromFlix.get(flixMovieId);
		for (int i = 0; i < IMDBMaps.size(); i++) {
			l.add(getIMBDAttribute(i, movieId));
		}
		return l;
	}
	public String getPlot(Integer flixMovieId) {
		Integer movieId = IMDBMapFromFlix.get(flixMovieId);
		return IMDBPlotMovies.get(movieId);
		
	}
	public String[] getAttributesNames(){
		if(allAttributes != null)
			getAttributes();
		if(names != null)
			return names;
		names = new String[allAttributes.length];
		for (int i = 0; i < names.length; i++) {
			names[i]=allAttributes[i].getName();
		}
		return names;		
	}

	public Attribute[] getAttributes(){
		
		if(allAttributes != null)
			return allAttributes;
		int size = attributes.length+IMDBMaps.size();
		if(getLaserDisc)
			size += 29;
		if(getPlot)
			size += 1;
		Attribute[] attrs = new Attribute[size];
		for (int i = 0; i < attributes.length; i++) {
			attrs[i]=attributes[i];
		}
		//IMDB
		for (int i = 0; i < IMDBMaps.size(); i++) {
			attrs[attributes.length+i]=new Attribute(null, attributes.length+i, files[i]);
			attrs[attributes.length+i].setType(Attribute.LIST);
		}
		//Adding LaserDisc data
		if(getLaserDisc){
			for (int i = 0; i < 29; i++) {
				attrs[attributes.length + IMDBMaps.size() + i] = new Attribute(null, attributes.length + IMDBMaps.size() + i,LaserDisc.names[i]);
				attrs[attributes.length + IMDBMaps.size() + i].setType(Attribute.NOMINAL);
			}
		}

		//Adding the plot
		if(getPlot){
			attrs[attrs.length-1]=new Attribute(null, attributes.length+IMDBMaps.size(), "plotRel.csv");
			attrs[attrs.length-1].setType(Attribute.TEXT);
		}
		allAttributes = attrs;
		return attrs;
	}
	protected void loadMapping(){
		try {
			CSVReader reader = new CSVReader(
					new FileReader(basePath + "movie_titlesMappedNoNulls.csv"), ';', '\"');

			String[] nextLine;
			Integer key;
			Integer value;
			while ((nextLine = reader.readNext()) != null) {
				key = Integer.parseInt(nextLine[0]);
				value = Integer.parseInt(nextLine[3]);
				IMDBMapFromFlix.put(key, value);
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public double[] getClasses(){
		return new double[]{1,2,3,4,5};
	}

	public void setAttributes(Attribute[] attrs){
		for (int i = 0; i < attrs.length; i++) {
			if(attrs[i].getName().endsWith(".csv")){
				Attribute[] attrs2 = new Attribute[attrs.length-1];
				for ( int j = 0; j < i; j++) {
					attrs2[j] = attrs[j];
				}
				for ( int j = i; j < attrs2.length; j++) {
					attrs2[j] = attrs[j+1];
				}
				attrs=attrs2;
				i=-1;
			}
		}
		super.setAttributes(attrs);
		allAttributes = null;
	}
	/**
	 * Overriden method from SQLMultiSource
	 */
	public List<Object> getRecord() {
		if (!hasNextRecord())
			return null;

		List<Object> l = new ArrayList<Object>();
		Attribute[] attributes = getAttributes();
		Long start;
		try {
			int flixId = Integer.parseInt(records.getObject(2).toString());
			int imdbId = IMDBMapFromFlix.get(flixId);
			int i = 0;
			for (; i < this.attributes.length; i++) {
				start = System.currentTimeMillis();
				l.add(records.getObject(i + 1));
				Oracle += System.currentTimeMillis() - start;
			}
			// IMDB
			for (i = 0; i < IMDBMaps.size(); i++) {
				if (attributes[i + this.attributes.length].getType() == Attribute.LIST) {
					start = System.currentTimeMillis();
					l.add(getIMBDAttribute(i, imdbId));
					IMDb += System.currentTimeMillis() - start;
				}
			}
			// LaserDisc
			if (getLaserDisc) {
				LaserDisc ld = IMDBLaserDiscs.get(imdbId);
				for (i = 0; i < 29; i++) {
					if (ld == null)
						l.add("");
					else
						l.add(ld.line[i]);
				}
			}
			if (getPlot) {
				l.add(IMDBPlotMovies.get(imdbId));
			}
			start = System.currentTimeMillis();
			records.next();
			Oracle += System.currentTimeMillis() - start;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		return l;
	}

	private void loadData(String fileName, HashMap<Integer, List<Integer>> map) {
		try {
			CSVReader reader = new CSVReader(
					new FileReader(basePath + fileName), ';', '\"');

			String[] nextLine;
			Integer key;
			Integer value;
			while ((nextLine = reader.readNext()) != null) {
				key = Integer.parseInt(nextLine[0]);
				value = Integer.parseInt(nextLine[1]);
				List<Integer> list = map.get(key);
				if (list == null) {
					list = CommonUtils.getList();
					map.put(key, list);
				}
				list.add(value);
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void loadLaserDiscs(String fileName, HashMap<Integer, LaserDisc> map) {
		try {
			CSVReader reader = new CSVReader(
					new FileReader(basePath + fileName), ';', '\"');
			String[] nextLine;
			LaserDisc ld;
			while ((nextLine = reader.readNext()) != null) {
				ld = new LaserDisc();
				ld.load(nextLine);
				map.put(ld.MOVIEID, ld);
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void loadPlotData(String fileName, String fileNamePlots, HashMap<Integer, String> map) {
		try {
			CSVReader reader = new CSVReader(
					new FileReader(basePath + fileNamePlots), ';', '\"');
			String[] nextLine;
			Integer key;
			Integer value;
			String plot;
			HashMap<Integer, String> mapValues = new HashMap<Integer, String>();
			while ((nextLine = reader.readNext()) != null) {
				key = Integer.parseInt(nextLine[0]);
				plot = nextLine[1];
				mapValues.put(key, plot);				
			}
			reader.close();
			reader = new CSVReader(
					new FileReader(basePath + fileName), ';', '\"');
			while ((nextLine = reader.readNext()) != null) {
				key = Integer.parseInt(nextLine[0]);
				value = Integer.parseInt(nextLine[1]);
				map.put(key, mapValues.get(value));
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Integer getUserId() {
		if(!usersList.isEmpty()){
			int index;
			if(userID == null){
				index = 0;		
			} 
			else {
				index = usersList.indexOf(userID.toString())+1;
			}
			if(index >= usersList.size()){
				userID = null;
				return null;
			}
			userID = CommonUtils.objectToInteger(usersList.get(index));
			return userID;
		}
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
			if(usersList != null && !usersList.isEmpty()){
				clearUsers();
				userID = null;
			}
			else
				super.restartUserId();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void configDataSource(XMLConfiguration config, String section) {
		super.configDataSource(config, section);
		Configuration dsConf = config.configurationAt(section);

		if(dsConf.containsKey("users")){
			usersList = dsConf.getList("users");
		}
		if(dsConf.containsKey("getLaserDisc")){
			getLaserDisc = dsConf.getBoolean("getLaserDisc");
		}
		if(dsConf.containsKey("getPlot")){
			getPlot = dsConf.getBoolean("getPlot");
		}
	}
	public List<Integer> getIMBDAttribute(Integer attributeId, Integer movie) {
		if (IMDBMaps.get(attributeId) == null)
			return null;
		return IMDBMaps.get(attributeId).get(movie);
	}


}

class LaserDisc{
	int MOVIEID;
	String LN;
	String LB;
	String CN;
	String LT;
	String OT;
	String PC;
	int YR;
	String CF;
	String CA;
	String GR;
	String LA;
	String SU;
	int LE;
	String RD;
	String ST;
	String PR;
	String QP;
	String CC;
	String PF;
	String DF;
	int SI;
	String MF;
	String AR;
	String AL;
	String DS;
	String SE;
	String CO;
	String VS;
	String RC;
	String[] line;
	static String[] names = {
		"LN.csv",
		"LB.csv",
		"CN.csv",
		"LT.csv",
		"OT.csv",
		"PC.csv",
		"YR.csv",
		"CF.csv",
		"CA.csv",
		"GR.csv",
		"LA.csv",
		"SU.csv",
		"LE.csv",
		"RD.csv",
		"ST.csv",
		"PR.csv",
		"QP.csv",
		"CC.csv",
		"PF.csv",
		"DF.csv",
		"SI.csv",
		"MF.csv",
		"AR.csv",
		"AL.csv",
		"DS.csv",
		"SE.csv",
		"CO.csv",
		"VS.csv",
		"RC.csv"};
	public void load(String[] line){
		this.line = line; 
		 MOVIEID=CommonUtils.objectToInteger(line[0]);
		 LN=line[1];
		 LB=line[2];
		 CN=line[3];
		 LT=line[4];
		 OT=line[5];
		 PC=line[6];
		 YR=CommonUtils.objectToInteger(line[7]);
		 CF=line[8];
		 CA=line[9];
		 GR=line[10];
		 LA=line[11];
		 SU=line[12];
		 LE=CommonUtils.objectToInteger(line[13]);
		 RD=line[14];
		 ST=line[15];
		 PR=line[16];
		 QP=line[17];
		 CC=line[18];
		 PF=line[19];
		 DF=line[20];
		 SI=CommonUtils.objectToInteger(line[21]);
		 MF=line[22];
		 AR=line[23];
		 AL=line[24];
		 DS=line[25];
		 SE=line[26];
		 CO=line[27];
		 VS=line[28];
		 RC=line[29];
	}
}
