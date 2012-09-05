package prefwork.rater;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;

public interface Rater {

	/**
	 * Return the rating, where all missing attributes are replaced with zero.
	 * @param ratings Preference of attribute values.
	 * @return double the rating of object.
	 */
	//public Double getRating(Double[] ratings);
	
	/**
	 * @param ratings Ratings of attribute values of object.
	 * @param record The object itself.
	 * @return The aggregated rating of the object.
	 */
	public Double getRating(Double[] ratings, AttributeValue[] record);
	

	/**
	 * Configures Rater.
	 * @param config Configuration
	 * @param section Section, in which is the configuration for current rater.
	 */
    public void configClassifier(XMLConfiguration config, String section);

	/**
	 * Initializes Rater.
     * @param attributes Attributes, from which some information may be get.
     */
    public void init(Attribute[] attributes);
    

    /**
     * 
     * Estimates the similarity of the given rater. It is often reasonable to compare only the raters of the same class.
     * @param n Other rater to compare with.
     * @return The degree of similarity between 0 and 1.
     */
    public double compareTo(Rater n);
    
}
