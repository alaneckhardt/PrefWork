package conversion;

import java.io.File;

public class THSources {
	static String path = "c:\\data\\datasets\\THSources\\";

	public static void processDir(String dir){
		/*if(!dir.startsWith("OK")  )
			return;*/
		File f = new File(path+'\\'+dir);
		String[] files = f.list();
		if(files == null)
			return;
		for (String file : files) {
			File potentialDir = new File(path + '\\' + dir + '\\' + file);
			if(potentialDir.isDirectory())
				processDir(dir + '\\' + file);
			File fData = new File(path + '\\' + dir + '\\' + file
					+ ".dat");

			File fDef = new File(path + '\\' + dir + '\\' + file
					+ ".def");
			if (fData.exists() && fDef.exists()) {
				System.out.print(path  + dir + '\\' + file+"\n");
			}
		}
	}
	public static void getFiles() {		
		File f = new File(path);
		String[] dirs = f.list();
		for (String dir : dirs) {
			processDir(dir);
		}

	}

	public static void main(String[] args) {
		//transformRatings(path+"sushi3b.5000.10.score",path+"sushiRatings.csv");
		//transformData(path+"sushi3.idata", path+"sushiData.csv");
		//insertData("sushiRatings", "userid, sushiid, rating" ,path+"sushiRatings.csv");
		getFiles();
	}
}
