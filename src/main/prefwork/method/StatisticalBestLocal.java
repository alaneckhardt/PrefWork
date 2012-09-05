package prefwork.method;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;
import prefwork.normalizer.Normalizer;
import prefwork.rater.Rater;
import prefwork.rater.WeightAverage;
import prefwork.representant.Representant;
import prefwork.test.MonotonicityTest;

public class StatisticalBestLocal extends Statistical {
	Integer nomCount[],numCount[];
	String[] nominalNormNames = new String[]{
        	"prefwork.normalizer.THNominalNormalizer",
	        "prefwork.normalizer.RepresentantNormalizer"};

	String[] numericalNormNames = new String[]{
	        "prefwork.normalizer.Linear", 
	        "prefwork.normalizer.THOrdinalNormalizer",
		        "prefwork.normalizer.Quadratic", 
		        "prefwork.normalizer.Peak",

		        "prefwork.normalizer.UpBottomClustering2CPNormalizer",
		        "prefwork.normalizer.Standard2CPNormalizer",
		        "prefwork.normalizer.LinearImproveHigher"};

	String[] listNormNames = new String[]{
	        "prefwork.normalizer.ListNormalizer"};
	Normalizer[] nom; 
	Normalizer[] num;
	Normalizer[] list;
	double[] errors;

	protected double getError(Normalizer norm, int index, BasicDataSource trainingDataset){
		List<Double[]> records = CommonUtils.getList();
		trainingDataset.restart();
		List<Object> rec = trainingDataset.getRecord();
		int size = 0;
		while (rec != null) {
			Double[] locPrefRec = CommonUtils.getLocalPref(rec, attributes);
			records.add(locPrefRec);
			size++;
			rec = trainingDataset.getRecord();
		}
		long[] res = MonotonicityTest.computeRatingChanges(records);
		long allComparablePairs  = res[0];
		long corruptedPairs = res[1];
		long allPairs = res[2];
		/*if(corruptedPairs==0)
			corruptedPairs=1;*/
		if(allComparablePairs==0)
			allComparablePairs=1;
		return (-((double)allComparablePairs-corruptedPairs)/(double)allComparablePairs);
		
		/*
		trainingDataset.restart();
		List<Object> rec = trainingDataset.getRecord();
		int size = 0;
		double err = 0;
		while (rec != null) {
			if(rec.get(index)==null){
				rec = trainingDataset.getRecord();
				continue;
			}
			double compRes = norm.normalize(rec);
			double res = CommonUtils.objectToDouble(rec.get(2));
			err+=Math.abs(compRes-res);
			size++;
			rec = trainingDataset.getRecord();
		}
		return err/size;*/
	}
	

/*Dobre 
StatisticalBestLocal-Com-Corr,WAvgVWV,Avg,TLCoef0false,BAvg2,LBAvg2
StatisticalBestLocalTHNumTHNom-(Com/All),WAvgVWV,Avg,TLCoef0false,BAvg2,LBAvg2
StatisticalBestLocalTHNumTHNom-(Corr/All),WAvgVWV,Avg,TLCoef0false,BAvg2,LBAvg2
StatisticalBestLocalCom/Corr,WAvgVWV,Avg,TLCoef0false,BAvg2,LBAvg2
*/
	public String toString() {
		return "StatisticalBestLocalAll-Com-Corr/Com," + rater.toString() + ","
				+ representant.toString() + "," +textNorm.toString()+ numericalNorm.toString()
				+ "," + nominalNorm.toString()
				+ "," + listNorm.toString();
	}
	
	
	/**
	 * Evaluates which of the normalizers is the best.
	 * 
	 * @param norms
	 * @param a
	 * @return The best of the given normalizers on the attribute a.
	 */
	public Normalizer evaluate(Normalizer[] norms, int index, BasicDataSource trainingDataset) {
		
		double[] errors=new double[norms.length];
		for (int i = 0; i < norms.length; i++) {
			attributes[index].setNorm(norms[i]);
			norms[i].init(attributes[index]);
			errors[i]=getError(norms[i], index,trainingDataset);
		}
		double minErr = Double.MAX_VALUE;
		int minIndex = -1;
		for (int i = 0; i < errors.length; i++) {
			if(errors[i]<minErr){
				minErr = errors[i];
				minIndex = i;
			}
		}
		if(minIndex==-1)
			return null;
		if(attributes[index].getType()==Attribute.NOMINAL){
			nomCount[minIndex]++;
		}
		if(attributes[index].getType()==Attribute.NUMERICAL){
			numCount[minIndex]++;
		}
		return norms[minIndex];
	}

	/**
	 * For each attribute finds the best possible normalizer. The criterion is the monotonicity violation.
	 * @param trainingDataset
	 */
	public void estimateBestNormalizers(BasicDataSource trainingDataset) {
		for (int i = 3; i < attributes.length; i++) {
			Normalizer[] norms = null;
			if(attributes[i].getType()==Attribute.NOMINAL)
				norms=nom;
			if(attributes[i].getType()==Attribute.NUMERICAL)
				norms=num;
			if(attributes[i].getType()==Attribute.LIST)
				norms=list;
			Normalizer n = evaluate(norms, i, trainingDataset);

			Normalizer n2 = n.clone();
			//System.out.print(""+i+";"+n2.toString()+"\n");
			n2.init(attributes[i]);			
			attributes[i].setNorm(n2);
		}
		/*for (int i = 3; i < attributes.length; i++) {
			Normalizer[] norms = null;
			if(attributes[i].getType()==Attribute.NOMINAL)
				norms=nom;
			if(attributes[i].getType()==Attribute.NUMERICAL)
				norms=num;
			if(attributes[i].getType()==Attribute.LIST)
				norms=list;
			Normalizer n = evaluate(norms, i, trainingDataset);
			Normalizer n2 = n.clone();
			n2.init(attributes[i]);
			attributes[i].setNorm(n2);
		}*/
	}
	
	public StatisticalBestLocal() {
		rater = new WeightAverage();
	}
	public StatisticalBestLocal clone(){
		StatisticalBestLocal st = new StatisticalBestLocal();
		if(this.attributes!= null){
			st.attributes = new Attribute[this.attributes.length];
			for (int i = 0; i < st.attributes.length; i++) {
				st.attributes[i]=this.attributes[i].clone();
			}
		}
		// Normalizer for nominal attributes
		st.nominalNormName = this.nominalNormName;
		st.nominalNorm = this.nominalNorm.clone();

		// Normalizer for numerical attributes
		st.numericalNormName = this.numericalNormName;
		st.numericalNorm = this.numericalNorm.clone();

		// Normalizer for list attributes
		st.listNormName = this.listNormName;
		st.listNorm = this.listNorm.clone();

		st.reprName = this.reprName;
		 st.representant = this.representant;

		 st.raterName = this.raterName;
		 st.rater = this.rater;
		
		return st;
		
	}

	/**
	 * Method for initialization of Statistical method.
	 */
	private void clear() {
		if(attributes == null)
			return;
		for(Attribute attr:attributes){
			attr.setValues(null);
			attr.setVariance(0.0);
		}
	}

	
	protected void loadNormalizers(){
		if(nom!=null)
			return;
		nom = new Normalizer[nominalNormNames.length];
		num = new Normalizer[numericalNormNames.length];
		nomCount = new Integer[nominalNormNames.length];
		numCount = new Integer[numericalNormNames.length];		
		list = new Normalizer[listNormNames.length];
		for (int i = 0; i < nominalNormNames.length; i++) {
			String nomName= nominalNormNames[i];
			nom[i] = (Normalizer) CommonUtils.getInstance(nomName);
			nomCount[i]=0;
		}
		for (int i = 0; i < numericalNormNames.length; i++) {
			String numName= numericalNormNames[i];
			num[i] = (Normalizer) CommonUtils.getInstance(numName);
			numCount[i]=0;
		}
		for (int i = 0; i < listNormNames.length; i++) {
			String listName= listNormNames[i];
			list[i] = (Normalizer) CommonUtils.getInstance(listName);
		}
	}
	/**
	 * Method builds the model for evaluation. For nominal attributes, it is
	 * based on statistical distribution of ratings of objects with given
	 * attribute values. For numerical, linear or other regression is used.<br />
	 * 
	 * @param trainingDataset
	 *            The dataset with training data.
	 * @param splitValue
	 *            User id.
	 */
	public int buildModel(BasicDataSource trainingDataset, Integer splitValue) {
		clear();
		loadNormalizers();
		trainingDataset.setFixedUserId(splitValue);
		trainingDataset.restart();
		int target = trainingDataset.getTargetAttribute();
		if (!trainingDataset.hasNextRecord())
			return 0;
		loadAttributes(trainingDataset);

		List<Object> rec;
		int count = 0;
		while ((rec = trainingDataset.getRecord()) != null) {
			// record[0] - uzivatelID
			// record[1] - itemID
			// record[2] - rating
			for (int i = 0; i < attributes.length; i++) {
				addAttributeValue(i, rec, CommonUtils.objectToDouble(rec.get(
						target)));
			}
			count++;
		}
		computeRatings();
		estimateBestNormalizers(trainingDataset);
		rater.init(attributes);
		for (int i = 0; i < attributes.length; i++) {
			for (AttributeValue val : attributes[i].getValues()) {
				CommonUtils.cleanAttributeValue(val);
			}
		}
		System.gc();
		// ((WeightRater) rater).setWeights(weights);
		return count;
	}



	public Integer[] getNomCount() {
		return nomCount;
	}



	public void setNomCount(Integer[] nomCount) {
		this.nomCount = nomCount;
	}



	public Integer[] getNumCount() {
		return numCount;
	}



	public void setNumCount(Integer[] numCount) {
		this.numCount = numCount;
	}


}
