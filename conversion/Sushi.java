package conversion;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;

import prefwork.datasource.MySQLConnectionProvider;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class Sushi {
	static String path = "c:\\data\\datasets\\sushi\\";

	static MySQLConnectionProvider provider = new MySQLConnectionProvider();
	private static void insertData( String table, String columns, String path){

		initSql();
		String nextLine[];
		try {
			CSVReader reader = new CSVReader(new FileReader(path), ';', '\"');
			while ((nextLine = reader.readNext()) != null) {
				String insert = "INSERT INTO "+table+"("+columns+") VALUES (";
				for(String s:nextLine)
					insert+="\""+s+"\""+", ";
				insert = insert.substring(0,insert.length()-2);
				insert+=") ";
				try {
					Statement stat = provider.getConn().createStatement();
					stat.executeUpdate(insert);
					stat.close();
				} catch (SQLException e) {
					
					System.out.print(java.util.Arrays.toString(nextLine));
					e.printStackTrace();
				}
			}
			reader.close();
			try {
				provider.getConn().commit();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private static void initSql(){
		provider = new MySQLConnectionProvider();
		provider.setDb("db");
		provider.setPassword("pass");
		provider.setUserName("user");
		try {
			provider.connect();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void transformRatings(String inpath, String outpath) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(inpath));
			CSVWriter writer = new CSVWriter(new FileWriter(outpath), ';', '\"');
			String line;
			String outline[] = new String[3];
			int userId = 1;
			while ((line = in.readLine()) != null) {
				String[] ratings = line.split(" ");
				int count = 0;
				for (int i = 0; i < ratings.length; i++) {
					//-1 if user didnt rated this sushi
					if("-1".equals(ratings[i]))
						continue;
					
					outline[0]=Integer.toString(userId);
					outline[1]=Integer.toString(i);
					outline[2]=ratings[i];					
					writer.writeNext(outline);
					count++;					
				}				
				userId++;
				if(count != 10)
					System.out.print(""+userId+": "+count+"\n");
			}
			in.close();

			writer.flush();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void transformData(String inpath, String outpath) {
		try {
			CSVReader in =  new CSVReader(new FileReader(inpath),'\t');	
			CSVWriter writer = new CSVWriter(new FileWriter(outpath), ';', '\"');
			String s[];
			while ((s=in.readNext()) != null) {	
					writer.writeNext(s);				
			}
			in.close();

			writer.flush();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		//transformRatings(path+"sushi3b.5000.10.score",path+"sushiRatings.csv");
		//transformData(path+"sushi3.idata", path+"sushiData.csv");
		//insertData("sushiRatings", "userid, sushiid, rating" ,path+"sushiRatings.csv");
		insertData("sushiData","sushiid, name, style, majorgroup, minorgroup, heaviness, frequentlyusereats, price, frequentlysushisold" ,path+"sushiData.csv");
	}
}
