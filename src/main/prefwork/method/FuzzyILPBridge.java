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

public class FuzzyILPBridge extends WekaBridge {

	List<Rule> rules = CommonUtils.getList();
	
	public String toString() {
		return "F"+classifierName+(wantsNumericClass?"0":"1");
	}


	private void parseRule(String line){
/*has_rating_atleast(id_0,3).
has_rating_atleast(A,2) :-
   has_producer_atleast(A,2).
has_rating_atleast(A,1) :-
   has_producer_atleast(A,2).
has_rating_atleast(A,0).
has_rating_atleast(id_4,1).*/
		String[] lines = line.split("\n");
		for (int i = 2; i < lines.length; i++) {
			Rule rule = new Rule();
			rule.head = lines[i].substring(lines[i].indexOf("(") , lines[i].indexOf(")") + 1);
			String[] heads = rule.head.split(",");
			//Rule for only one id
			if(heads[0].startsWith("(id_"))
				continue;			
			if(lines[i].endsWith(".\r")){
				rules.add(rule);
				continue;
			}
			//Body is on the next line
			i++;							
			String body = lines[i].substring(lines[i].indexOf("has_"));
			//skipping . at the end
			body = body.substring(0, body.length() - 2);
			rule.body = body.split(", ");
			//skipping has_
			for (int j = 0; j < rule.body.length; j++) {
				rule.body[j] = rule.body[j].substring(rule.body[j].indexOf("has_")+4);
				
			} 
			rules.add(rule);
		}
	}
	
	public int buildModel(BasicDataSource trainingDataset, Integer user) {
		int count = super.buildModel(trainingDataset, user);

		rules = CommonUtils.getList();
		parseRule(this.cModel.toString());
		return count;
	}

	public Double classifyRecord(List<Object> record, Integer targetAttribute) {
		double res = 0, max = -1;//
		@SuppressWarnings("unused")
		int count = 0;
		for (Rule r : rules) {
			if (match(r, record, attributes)) {
				res = CommonUtils.objectToDouble(ProgolBridge.getValue(r.head, 1));
				if(res>max)
					max = res;
				//count++;
			}
		}
		if(max!=-1)
			return max;
		return null;
	}	
	

	@SuppressWarnings("unchecked")
	protected static boolean match(Rule r, List<Object> record, prefwork.Attribute[] attributes) {
		if (r.body == null || r.body.length == 0) {
			// TODO Find if the id in the rule corresponds with id in record.
			return false;
		}

		for (int i = 0; i < r.body.length; i++) {
			boolean found = false;
			for (int j = 0; j < attributes.length; j++) {
				String attrName = ProgolBridge.transform(attributes[j].getName());
				if (r.body[i].startsWith(attrName)) {

					if( attributes[i].getType() == prefwork.Attribute.NUMERICAL ){
						if (record.get(j)!=null && 
								CommonUtils.objectToDouble(record.get(j))>=
						CommonUtils.objectToDouble(ProgolBridge.getValue(r.body[i], 1))
								) {
							found = true;
							break;
						}
					}
					else if (attributes[j].getType() == Attribute.NOMINAL
							|| attributes[j].getType() == prefwork.Attribute.COLOR) {
						if (record.get(j)!=null && ProgolBridge.transform(record.get(j).toString()).equals(
								ProgolBridge.getValue(r.body[i], 1))) {
							found = true;
							break;
						}
					}else if (attributes[j].getType() == prefwork.Attribute.LIST) {
							List<Object> l = (List<Object>) record.get(j);
							if(l == null)
								continue;
							for (Object o : l) {
								if(o == null)
									continue;
								if (ProgolBridge.transform(o.toString()).equals(
										ProgolBridge.getValue(r.body[i], 1))) {
									found = true;
									break;
								}
							}
					}
				}
			}
			// We didn't find the corresponding value for this body element
			if (!found)
				return false;
		}
		return true;
	}

}
