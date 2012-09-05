package prefwork.representant;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

public interface Representant {
	public Double getRepresentant(Double[] array);
	public Double getRepresentant(List<Double> array);

	/**
	 * Configures Representant.
	 * @param config Configuration
	 * @param section Section, in which is the configuration for current Representant.
	 */
    public void configClassifier(XMLConfiguration config, String section);
}
