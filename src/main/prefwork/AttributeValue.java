package prefwork;

import java.util.Collections;
import java.util.List;

public class AttributeValue {

	User user;

	Attribute attribute;

	Object value;

	public void setValue(Object value) {
		this.value = value;
	}

	List<Double> ratings;

	List<List<Object>> records;

	Double representant;

	Double variance;

	public AttributeValue(){
		
	}
	
	public String toString(){
		return value.toString();
	}	
	public AttributeValue(Attribute attribute){
		this.attribute=attribute;
	}

	public AttributeValue(Attribute attribute, Object value){
		this.attribute=attribute;
		this.value = value;
	}
	
	public AttributeValue(Attribute attribute, List<Object> record){
		this.attribute=attribute;
		this.value = record.get(attribute.index);
		records = CommonUtils.getList(10);
		records.add(record);
	}
	
	
	public Attribute getAttribute() {
		return attribute;
	}

	public void setAttribute(Attribute attribute) {
		this.attribute = attribute;
	}

	public Object getValue() {
		return value;
	}

	public List<Double> getRatings() {
		return ratings;
	}

	public void setRatings(List<Double> ratings) {
		this.ratings = ratings;
	}
	public void addRating(Double rating) {
		if(ratings == null)
			ratings = CommonUtils.getList(10);
		ratings.add(rating);
		Collections.sort(ratings);
	}

	public void addRecord(List<Object> record) {
		if(records == null)
			records = CommonUtils.getList(10);
		records.add(record);
	}

	public void setRecords(List<List<Object>> records) {
		this.records = records;
	}

	public List<List<Object>> getRecords() {
		return records;
	}
	
	public Double getRepresentant() {
		return representant;
	}

	public void setRepresentant(Double representant) {
		this.representant = representant;
	}

	public Double getVariance() {
		return variance;
	}

	public void setVariance(Double variance) {
		this.variance = variance;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
}
