package prefwork.test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import prefwork.VirtualUser;
import prefwork.datasource.BasicDataSource;
import prefwork.datasource.RatingDataSource;
import prefwork.method.InductiveMethod;
import prefwork.method.Statistical;
import prefwork.normalizer.Linear;
import prefwork.normalizer.Normalizer;
import prefwork.normalizer.Peak;
import prefwork.normalizer.Quadratic;
import prefwork.normalizer.VirtualPeak;
import au.com.bytecode.opencsv.CSVWriter;

public class TestTrainAbsolutePeakQuadr extends TestTrainAbsolute {
	private static Logger log = Logger.getLogger(TestTrainAbsolutePeakQuadr.class);
	double errPeak = 0, errQuadr = 0;
	int testPeak = 0, testQuadr = 0;
	double err = 0;
	int test=0;
	
	protected double getMax(Attribute attr){
		Double max= null;		
		for(AttributeValue val : attr.getValues()){
			double d = CommonUtils.objectToDouble(val.getValue());
			if(max == null)
				max = d;
			if(d>max)
				max = d;
		}
		return max;
	}
	
	protected double getQuadrPeak(Quadratic quadratic, Attribute attr){
		double[] co = quadratic.getCoefficients();
		double a=co[1],b=co[0],c=co[3];
		double d1=(-b+Math.sqrt(b*b-4*a*c))/(2*a);
		double d2=(-b-Math.sqrt(b*b-4*a*c))/(2*a);
		if(Double.isInfinite(d1)||Double.isInfinite(d2)||
				Double.isNaN(d1)||Double.isNaN(d2)){
			//Decreasing function, optimum is in 0
			if(b<0)
				return 0;
			//Increasing function, optimum is in MAX.
			else if(b>0)
				return getMax(attr);
			//Monotone, returning the middle of domain.
			else
				return getMax(attr)/2;
		}
		return (d1+d2)/2;
	}
	protected double getLinearPeak(Linear linear, Attribute attr){
		double[] co = linear.getCoefficients();
		double a=co[2];
		// Decreasing function, optimum is in 0
		if (a < 0)
			return 0;
		// Increasing function, optimum is in MAX.
		else if (a > 0)
			return getMax(attr);
		// Monotone, returning the middle of domain.
		else
			return getMax(attr) / 2;
	}
	protected double getPeakPeak(Peak peak, Attribute attr){
			return peak.getCutValue();
	}
	protected double getPeak(Normalizer norm, Attribute attr){

		if(norm instanceof Peak){
			return getPeakPeak((Peak)norm,attr);
		}
		if(norm instanceof Quadratic){
			return getQuadrPeak((Quadratic)norm,attr);
		}
		if(norm instanceof Linear){
			return getLinearPeak((Linear)norm,attr);
		}
		return 0;
	}
	protected void compare(int userid, Normalizer norm, Attribute attr, RatingDataSource source){
		List<VirtualUser> users = source.getUsers();
		VirtualUser u = users.get(userid);
		Normalizer[] norms = u.getNorms();
		VirtualPeak vp = ((VirtualPeak)norms[attr.getIndex()-2]);
		double realPeak = vp.getPeak();
		double peak = getPeak(norm, attr);
		if(!Double.isInfinite(peak)&&!Double.isNaN(peak)){
			err+=Math.abs(realPeak - peak)/realPeak;
			test++;
		}
		
		/*
		for(VirtualUser u : users){
			if(u.getUserId()==userid){
				break;
			}				
		}*/
	}
	public void test(InductiveMethod ind, BasicDataSource trainDataSource,
			BasicDataSource testDataSource) {
		// loadOriginalRatings();

		Statistical statistical=((Statistical)ind);

		CSVWriter writer;
		try {
			writer = new CSVWriter(new FileWriter("C:\\outLog2.csv",true), ';');
		
		
		resultsInterpreter.setHeaderPrefix("date;ratio;dataset;method;");
		results = new TestResults(trainDataSource);
		log.info("Testing method " + ind.toString());
			log.debug("Configuring " + testDataSource.getName());
			trainDataSource.restartUserId();
			trainDataSource.setRandomColumn(trainDataSource.getAttributes()[1].getName());
			testDataSource.setRandomColumn(testDataSource.getAttributes()[1].getName());
			Integer userId = trainDataSource.getUserId();
			while (userId != null) {
				log.debug("User " + userId);				
				getRandoms(trainDataSource, userId);

				for (int trainSet : trainSets) {
					run = 0;
					Long trainTime =0L, testTime=0L;
				while ((run + 1) * trainSet  < randoms.length) {
					resultsInterpreter.setRowPrefix(""
							+ new Date(System.currentTimeMillis()).toString() + ";"
							+ Double.toString(trainSet) + ";"
							+ trainDataSource.getName() + ";" + ind.toString() + ";");
					//log.info("run "+run+", tr "+trainSet);
					results.reset();
					configTestDatasource(testDataSource, run, trainSet);
					configTrainDatasource(trainDataSource, run, trainSet);
					Long start = System.currentTimeMillis();
					int trainCount = statistical.buildModel(trainDataSource, userId);
					results.setTrainCount(userId, run, trainCount);					
					trainTime += System.currentTimeMillis() - start;
					start = System.currentTimeMillis();
					Attribute[] attr = statistical.getAttributes();
					for (int i = 3; i < attr.length; i++) {
						if(attr[i].getType()!=Attribute.NUMERICAL)
							continue;
						Normalizer  n = statistical.getAttributes()[i].getNorm();
						compare(userId, n, attr[i], (RatingDataSource)trainDataSource);
						
					}						
					testTime += System.currentTimeMillis() - start;
					run++;
				}

				writer.writeNext(new String[]{trainDataSource.getName(),statistical.getNumericalNorm().toString(),
						Integer.toString(trainSet),Double.toString(err), Integer.toString(test/run),
						Double.toString(err/(test/run)), trainTime.toString(), testTime.toString()});
				err=0;
				test=0;
				}
				//log.debug("User tested.");
				userId = trainDataSource.getUserId();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public TestInterpreter getResultsInterpreter() {
		return resultsInterpreter;
	}

}
