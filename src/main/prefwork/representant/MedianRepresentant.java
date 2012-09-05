package prefwork.representant;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

public class MedianRepresentant implements Representant {

	private double precision;
	public Double getRepresentant(List<Double> array) {		
		int indexOfMedian=(int)(array.size()/2);

		if(indexOfMedian>=array.size())
			return array.get(indexOfMedian);
		return array.get(indexOfMedian);	
	}
		
	public String toString(){
		return "Median";
	}
	public Double getRepresentant(Double[] array) {		
		int indexOfMedian=(int)(array.length/2);
		/*
		indexOfMedian=(int)(array.length*(1-precision));
		double denominator=0;
		double nominator=0;
		
		Arrays.sort ( array);
		for(int j=0;j<array.length;j++){
			if(indexOfMedian==0){
				nominator+=array[j];
				denominator+=1.0;
			}
			else{
				nominator+=array[j]*(1.0-Math.abs(indexOfMedian-j)/(double)Math.max(array.length-indexOfMedian,indexOfMedian));
				denominator+=(1.0-Math.abs(indexOfMedian-j)/(double)Math.max(array.length-indexOfMedian,indexOfMedian));
			}
		}
		return nominator/denominator;*/
		if(indexOfMedian>=array.length)
			return array[array.length-1];
		return array[indexOfMedian];	
	}
	public double getPrecision() {
		return precision;
	}
	public void setPrecision(double precision) {
		this.precision = precision;
	}


	public void configClassifier(XMLConfiguration config, String section) {
		// TODO Auto-generated method stub
		
	}

}
