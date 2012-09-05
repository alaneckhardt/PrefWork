package prefwork.datasource;



import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;

public interface BasicDataSource{

    //public void basic_data_source( PyObject config, PyString dataSourceSection, PyString driverSection);
                
    public List<Object> getRecord();
                                   
    public String[] getAttributesNames();
    
    public Attribute[] getAttributes();

	public void setAttributes(Attribute[] attributes);
    
    public int getTargetAttribute();
        
    public double[] getClasses();
	
	public void setLimit(Double fromPct,Double toPct,boolean recordsFromRange);        
	        
	public void configDriver( XMLConfiguration config, String section);             
	
	public void configDataSource(XMLConfiguration config, String section);
	
	public void fillRandomValues();
	
	public void setFixedUserId(Integer value);
	
	public Integer getUserId();
	
	public void restart();
	
	public void restartUserId();
	
	public boolean hasNextRecord();
	
	public String getName();
	public void setName(String name);


	public String getRandomColumn();

	public void setRandomColumn(String randomColumn) ;

}
