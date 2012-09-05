package prefwork;

import java.util.List;

import prefwork.normalizer.Normalizer;

public class Attribute {
	
	public static final int NOMINAL = 1;
	public static final int NUMERICAL = 2;
	public static final int LIST = 3;
	public static final int COLOR = 4;
	public static final int TEXT = 5;
	protected User user;

	protected int index;

	private List<AttributeValue> values = CommonUtils.getList();

	protected int type;

	protected Double variance;
	
	protected String name;

	//Only for LIST type attributes.
	protected String select;

	protected Normalizer norm = null;
	
	double max, min;

	public double getMax() {
		return max;
	}

	public void setMax(double max) {
		this.max = max;
	}

	public double getMin() {
		return min;
	}

	public void setMin(double min) {
		this.min = min;
	}

	public Attribute() {

	}

	public Attribute(User user, int index, String name) {
		this.user = user;
		this.index = index;
		this.name = name;
	}
	public Attribute clone(){
		Attribute attr = new Attribute(user, index, name);
		attr.values = CommonUtils.getList(this.values.size());

		attr.type = this.type;

		attr.variance = this.variance;
		attr.select = this.select;
		if(norm != null)
			attr.norm = this.norm.clone();
		return attr;
	}
	public String toString(){
		return name;
	}
	

	

	public AttributeValue getValue(Object value) {
		if (values == null)
			return null;
		
		for (AttributeValue attrValue : values) {
			if (attrValue.getValue()!= null && attrValue.getValue().equals(value))
				return attrValue;
		}
		return null;
	}

	public boolean contains(Object value) {
		if (values == null){
			values = CommonUtils.getList(10);			
		}
		for (AttributeValue attrValue : values) {
			if (attrValue.getValue().equals(value))
				return true;
		}
		return false;
	}

	public List<AttributeValue> getValues() {
		return values;
	}

	public void setValues(List<AttributeValue> values) {
		this.values = values;
		if(this.values == null)
			this.values = CommonUtils.getList(10);
	}

	public void addValue(AttributeValue value) {
		if (value == null)
			return;
		if (values == null)
			values = CommonUtils.getList(10);
		values.add(value);
	}
	

	public void addValue(List<Object> record) {
		if (record.get(index) == null)
			return;
		if (values == null){
			values = CommonUtils.getList(10);			
		}
		AttributeValue value;
		if(!contains(record.get(index))){
			value = new AttributeValue(this, record);
			values.add(value);			
		}
		else
			this.getValue(record.get(index)).addRecord(record);
	}

	public void addValue(AttributeValue value, Double rating) {
		if (value == null)
			return;
		if (values == null){
			values = CommonUtils.getList(10);			
		}
		values.add(value);
		value.addRating(rating);		
	}
	

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Normalizer getNorm() {
		return norm;
	}

	public void setNorm(Normalizer norm) {
		this.norm = norm;
	}

	public Double getVariance() {
		return variance;
	}

	public void setVariance(Double variance) {
		this.variance = variance;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}


	/**
	 * Only for LIST type attributes.
	 * @return Returns the select to obtain the values for given objectId.
	 */
	public String getSelect() {
		return select;
	}

	/**
	 * Only for LIST type attributes.
	 * @param select
	 */
	public void setSelect(String select) {
		this.select = select;
	}
}
