package prefwork.method;

import java.lang.reflect.Constructor;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.CommonUtils;
import prefwork.datasource.BasicDataSource;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class WekaBridge implements InductiveMethod {

	// Create a naive bayes classifier
	String classifierName = "weka.classifiers.bayes.NaiveBayes";
	Classifier cModel = (Classifier) new weka.classifiers.bayes.NaiveBayes();

	boolean wantsNumericClass = true;

	boolean onlyNumericAttributes = false;

	boolean onlyNominalAttributes = false;
	
	boolean noMissing = false;

	prefwork.Attribute[] attributes;
	List<Integer> numerical;
	List<Integer> nominal;
	List<Integer> indexes;
	Integer targetAttribute;

	FastVector fvWekaAttributes;

	Instances isTrainingSet;

	public String toString() {
		return classifierName+(wantsNumericClass?"0":"1");
	}

	protected Instance getInstanceFromIndexes(List<Object> rec) {

		Instance iExample = null;
		iExample = new Instance(indexes.size());

		
		for (int i = 0; i < indexes.size(); i++) {
			try {
				if(rec.get(indexes.get(i)) == null)
						continue;
				if (numerical.contains(indexes.get(i)))
					iExample.setValue(
							(Attribute) fvWekaAttributes.elementAt(i), Double
									.parseDouble(rec.get(indexes.get(i))
											.toString()));
				else
					iExample.setValue(
							(Attribute) fvWekaAttributes.elementAt(i), rec.get(
									indexes.get(i)).toString());
			} catch (IllegalArgumentException e) {
				// e.printStackTrace();
				if (noMissing) {
					iExample.setValue(
							(Attribute) fvWekaAttributes.elementAt(i),
							((Attribute) fvWekaAttributes.elementAt(i))
									.value(0));
				}

				//System.err.print("" + rec.get(i).toString() + "," + i + "\n");
			}
		}
		try {
			if (wantsNumericClass)
				iExample.setValue((Attribute) fvWekaAttributes
						.elementAt(targetAttribute), CommonUtils.objectToDouble(rec
						.get(indexes.get(targetAttribute))));
			else
				iExample.setValue((Attribute) fvWekaAttributes
						.elementAt(targetAttribute), rec.get(
						indexes.get(targetAttribute)).toString());
		} catch (IllegalArgumentException e) {
			// e.printStackTrace();
			if (noMissing) {
				iExample.setValue((Attribute) fvWekaAttributes
						.elementAt(targetAttribute),
						((Attribute) fvWekaAttributes
								.elementAt(targetAttribute)).value(0));
			}
			/*System.err.print(""
					+ rec.get(indexes.get(targetAttribute)).toString() + ","
					+ targetAttribute + "\n");*/
		}

		return iExample;
	}

	protected Instance getWekaInstance(List<Object> rec) {
		Instance iExample;
		iExample = getInstanceFromIndexes(rec);
		iExample.setDataset(isTrainingSet);
		return iExample;
	}

	protected void processAttribute(List<Object> rec, FastVector vec, int i) {
		if(rec.get(indexes.get(i)) == null)
			return;
		if (!vec.contains(rec.get(indexes.get(i)).toString()))
			vec.addElement(rec.get(indexes.get(i)).toString());
	}

	protected void getAttributes(BasicDataSource trainingDataset, Integer user) {
		trainingDataset.setFixedUserId(user);
		trainingDataset.restart();
		String[] attributeNames = trainingDataset.getAttributesNames();
		int size = indexes.size();
		FastVector[] vec = new FastVector[size];
		for (int i = 0; i < vec.length; i++) {
			vec[i] = new FastVector();
		}

		List<Object> rec;
		if (!onlyNumericAttributes) {
			while ((rec = trainingDataset.getRecord()) != null) {
				for (int i = 0; i < indexes.size(); i++) {
					if(( i == targetAttribute && !wantsNumericClass) || nominal.contains(indexes.get(i)))
					processAttribute(rec, vec[i], i);
				}
			}
		}

		fvWekaAttributes = new FastVector(size);
		for (int i = 0; i < size; i++) {
			// Add a nominal attribute
			if ((vec[i].size() > 0 && !onlyNumericAttributes) || onlyNominalAttributes)
				fvWekaAttributes.addElement(new Attribute(
						ProgolBridge.transform(attributeNames[indexes.get(i)]), vec[i], i));
			// Add a numerical attribute
			else if (!onlyNominalAttributes || ( i == targetAttribute && wantsNumericClass))
				// fvWekaAttributes.addElement(new Attribute(attributeNames[i],
				// new FastVector()));
				fvWekaAttributes.addElement(new Attribute(
						ProgolBridge.transform(attributeNames[indexes.get(i)]), i));
		}
	}

	@SuppressWarnings("unchecked")
	protected void clear(){
		nominal = CommonUtils.getList();
		numerical = CommonUtils.getList();
		indexes = CommonUtils.getList();
		fvWekaAttributes = null;
		isTrainingSet = null;
		try {
			Class c = Class.forName(classifierName);
			Constructor[] a = c.getConstructors();
			cModel = (Classifier) a[0].newInstance();		
		}catch (Exception e) {}
	}
	
	protected void init(BasicDataSource trainingDataset) {
		prefwork.Attribute[] attrs = trainingDataset.getAttributes();
		for (int i = 2; i < attrs.length; i++) {
			prefwork.Attribute attr = attrs[i];
			if (attr.getType() == prefwork.Attribute.NUMERICAL)
				numerical.add(i);
			else if (attr.getType() == prefwork.Attribute.NOMINAL)
				nominal.add(i);
		}
		if (onlyNumericAttributes)
			indexes.addAll(numerical);
		else if (onlyNominalAttributes)
			indexes.addAll(nominal);

		if (onlyNumericAttributes
				&& !numerical.contains(trainingDataset.getTargetAttribute())) {
			indexes.add(trainingDataset.getTargetAttribute());
		} else if (onlyNominalAttributes
				&& !nominal.contains(trainingDataset.getTargetAttribute())) {
			indexes.add(trainingDataset.getTargetAttribute());
		}
		
		else {
			List<Integer> all = CommonUtils.getList();
			all.addAll(numerical);
			all.addAll(nominal);
			indexes = all;
		}

		targetAttribute = indexes.indexOf(trainingDataset.getTargetAttribute());

	}

	public int buildModel(BasicDataSource trainingDataset, Integer user) {
		clear();
		init(trainingDataset);

		attributes = trainingDataset.getAttributes();
		getAttributes(trainingDataset, user);
		trainingDataset.setFixedUserId(user);
		trainingDataset.restart();
		if (!trainingDataset.hasNextRecord())
			return 0;
		// Create an empty training set
		isTrainingSet = new Instances("Rel", fvWekaAttributes, 10);
		// Set class index
		isTrainingSet.setClassIndex(targetAttribute);

		List<Object> rec;
		int count = 0;
		while ((rec = trainingDataset.getRecord()) != null) {
			// record[0] - uzivatelID
			// record[1] - itemID
			// record[2] - rating
			// Create the instance

			// add the instance
			isTrainingSet.add(getWekaInstance(rec));
			count++;
		}
		try {
			cModel.buildClassifier(isTrainingSet);
		} catch (Exception e) {
		//	e.printStackTrace();
			cModel = null;
		}
		return count;
	}

	public Double classifyRecord(List<Object> record, Integer targetAttribute) {
		try {
			//The model wasn't built, we return null.
			if(cModel == null)
				return null;
			double r = cModel.classifyInstance(getWekaInstance(record));
			double[] fDistribution = cModel.distributionForInstance(getWekaInstance(record));
			double res=0.0;
			for(int i=0;i<fDistribution.length;i++)
				if(wantsNumericClass)
					res+=fDistribution[i];
				else
					res+=CommonUtils.objectToDouble(((Attribute) fvWekaAttributes.elementAt(this.targetAttribute)).value(i))*fDistribution[i];
			return res;
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public void configClassifier(XMLConfiguration config, String section) {
		Configuration methodConf = config.configurationAt(section);
		try {
			classifierName = CommonUtils.getFromConfIfNotNull(methodConf, "classifier", classifierName);
			Class c = Class.forName(classifierName);
			Constructor[] a = c.getConstructors();
			cModel = (Classifier) a[0].newInstance();		
		}catch (Exception e) {
			e.printStackTrace();
		}
		try {
		    wantsNumericClass = methodConf.getBoolean("wantsNumericClass", wantsNumericClass);
		}catch (Exception e) {}
		try {
			onlyNominalAttributes = methodConf.getBoolean("onlyNominalAttributes", onlyNominalAttributes);
		}catch (Exception e) {}
		try {
			onlyNumericAttributes = methodConf.getBoolean("onlyNumericAttributes", onlyNumericAttributes);
		}catch (Exception e) {}
	}

}
