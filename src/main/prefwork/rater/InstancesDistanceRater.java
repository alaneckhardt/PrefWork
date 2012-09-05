package prefwork.rater;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.CommonUtils;
import prefwork.normalizer.Normalizer;

public class InstancesDistanceRater extends InstancesRater {




	public void configClassifier(XMLConfiguration config, String section) {

	}

	public Double getRating(Double[] ratings) {
		return null;
	}

	public String toString() {
		return super.toString()+"Distance";
	}
	/*
	protected boolean checkPareto(List<List<Object>> pareto,
			List<Object> newElement, boolean up) {
		for (List<Object> instance : pareto) {
			for (int i = 3; i < attributes.length; i++) {
				Normalizer norm = attributes[i].getNorm();
				// newElement is better, but we want higher pareto (worst
				// objects from better objects)
				if (norm.compare(newElement, instance) > 0
						&& up) {
					return false;
				}
				// newElement is worse, but we want lower pareto (best objects
				// from worse objects)
				if (norm.compare(newElement, instance) > 0
						&& !up) {
					return false;
				}
			}
		}
		return true;
	}

	protected List<List<Object>> findPareto(Double[] ratings,
			List<Object> record, boolean up) {
		List<List<Object>> pareto = CommonUtils.getList();
		for (List<Object> instance : instances) {
			int recordIsBetter = 0, recordIsWorse = 0;
			for (int i = 3; i < attributes.length; i++) {
				Normalizer norm = attributes[i].getNorm();
				if (record.get(i) == null || instance.get(i) == null
						|| instance.get(i) == null)
					continue;
				if (norm.compare(record, instance) > 0) {
					recordIsBetter++;
				}
				if (norm.compare(record, instance) < 0) {
					recordIsWorse++;
				}
			}
			if (recordIsWorse == 0 && !up) {
				if (checkPareto(pareto, instance, up)) {
					pareto.add(instance);
				}
			}

			if (recordIsBetter == 0 && up) {
				if (checkPareto(pareto, instance, up)) {
					pareto.add(instance);
				}
			}
		}
		return pareto;
	}*/

	/**
	 * Finds intersection of a line between the ideal point and the record, with
	 * the borders of cube defined by a pareto point.
	 * 
	 * @param rec
	 * @param paretoPoint
	 * @return
	 */
	protected Double getIntersection(List<Object> rec,
			List<Object> paretoPoint) {
		Double t = null;
		for (int i = 3; i < rec.size(); i++) {
			Normalizer norm = attributes[i].getNorm();
			if (rec.get(i) == null || paretoPoint.get(i) == null) {
				continue;
			}
			Double reci = norm.normalize(rec);
			Double pari = norm.normalize(paretoPoint);
			if(reci == null || pari == null){
				continue;
			}
			if (t == null || t > 1 + pari / (reci - 1)) {
				t = 1 + pari / (reci - 1);
			}
		}
		return t;
	}

	/**
	 * Finds the intersection of a line between the ideal point and the record
	 * and the hyperplanes defined by points belonging to pareto front.
	 * 
	 * @param rec
	 * @param pareto
	 * @return
	 */
	protected int getIntersection(List<Object> rec,
			List<List<Object>> pareto) {
		// We search for the intersection with every point
		int indexMax = -1;
		Double tMax = null;
		for (int i = 0; i < pareto.size(); i++) {
			List<Object> paretoPoint = pareto.get(i);
			Double t = getIntersection(rec, paretoPoint);
			if (tMax == null || tMax < t) {
				tMax = t;
				indexMax = i;
			}
		}
		// We return the most distant intersection.
		return indexMax;
	}

	/**
	 * Procedure for computing the rating of the given point.
	 * 
	 * @param ratings
	 * @param record
	 * @return
	 */
	public Double getRating(List<Object> record) {
		Double[] locPrefRec = CommonUtils.getLocalPref(record, attributes);
		paretodown = CommonUtils.getList();
		paretoup = CommonUtils.getList();
		findPareto(locPrefRec);
		double rating = 0.0D;
		double ratingUp = 0.0D;
		double ratingDown = 0.0D;
		double tUp = 0.0D;
		double tDown = 0.0D;
		try {
			if (paretoup.size() == 0) {
				ratingUp = 6.0 * Math.abs(1 - distance(record, 1.0));
				tUp = Math.abs(1 - distance(record, 1.0));
			} else {
				int index = getIntersection(record, paretoup);
				if (index == -1)
					ratingUp = 0.0;
				else {
					tUp = Math.abs(1 - getIntersection(record, paretoup
							.get(index)));
					ratingUp = (Math.abs(1 - getIntersection(record, paretoup
							.get(index))))
							* CommonUtils.objectToDouble(((List<Object>) paretoup
									.get(index)).get(targetAttribute));
				}
			}
		} catch (java.lang.NullPointerException e) {

			tUp = 0.0;
		}

		if (paretodown.size() == 0) {
			ratingDown = 0.0 * Math.abs(1 - distance(record, 0.0));
			tDown = Math.abs(1 - distance(record, 0.0));
		} else {
			int index = getIntersection(record, paretodown);
			if (index == -1)
				ratingUp = 0.0;
			else {

				tDown = Math.abs(1 - getIntersection(record, paretodown
						.get(index)));
				ratingDown = (Math.abs(1 - getIntersection(record, paretodown
						.get(index))))
						* CommonUtils.objectToDouble(((List<Object>) paretodown
								.get(index)).get(targetAttribute));
			}
		}

		if (ratingUp == 0)
			return ratingDown / tDown;
		if (ratingDown == 0)
			return ratingUp / tUp;
		rating = (ratingUp + ratingDown) / (tUp + tDown);
		return rating;
	}

	protected double distance(List<Object> rec1, Double rating) {
		double distance = 0;
		int denominator = 0;
		for (int i = 3; i < rec1.size(); i++) {
			Normalizer norm = attributes[i].getNorm();
			if (rec1.get(i) == null || norm == null) {
				continue;
			}
			Double pref1 = norm.normalize(rec1);
			Double pref2 = rating;
			if(pref1 == null || pref2 == null){
				continue;
			}
			distance += Math.abs(pref1 - pref2);
			denominator++;
		}
		return distance / rec1.size();
	}

	protected double distance(List<Object> rec1, List<Object> rec2) {
		double distance = 0;
		int denominator = 0;
		for (int i = 3; i < rec1.size(); i++) {
			Normalizer norm = attributes[i].getNorm();
			if (rec1.get(i) == null || rec2.get(i) == null) {
				continue;
			}
			Double pref1 = norm.normalize(rec1);
			Double pref2 = norm.normalize(rec2);
			if(pref1 == null || pref2 == null){
				continue;
			}
			distance += Math.abs(pref1 - pref2);
			denominator++;
		}
		return distance / rec1.size();
	}

}
