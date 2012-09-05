package prefwork;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;

public class CSVToConfDataSource {
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(CSVToConfDataSource.class);

	static String path = "c:\\data\\datasets\\fotografie\\lireOk.csv";
	static String name = "lire";

	public static void main(String[] args) {

		String s[];
		try {
			CSVReader in = new CSVReader(new FileReader(path),';');	

		     PrintStream out = System.out;
		     out.println("<"+name+">");
		     out.println("<attributes>");

			s=in.readNext();
					for(String name:s){
						out.println("<attribute><name>"+name+"</name><type>numerical</type></attribute>");
					}

			
			 out.println("</attributes>");
			 out.println("<targetAttribute>2</targetAttribute>");
			 out.println("<classes>1,2,3,4,5</classes>");
			 out.println("<objectIndex>0</objectIndex>");
			 out.println("<userIndex>1</userIndex>");
			 out.println("<randomizeIndex>3</randomizeIndex>");
			 out.println("<loadOnlySpecifiedUsers>false</loadOnlySpecifiedUsers>");
		     out.println("</"+name+">");
				
		          
		          
		          
		          
		          
		          
		           
		           
		          

			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
