package prefwork.normalizer;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.Attribute;
import prefwork.AttributeValue;
import prefwork.CommonUtils;

public class ColorNormalizer implements Normalizer {

    //treemap je pouze pole??
    //ukladat barvu jako integer po slozkach oddelenych strednikem: 256;128;128
    TreeMap<String, Double> map = new TreeMap<String, Double>();
    List<String> al = CommonUtils.getList();
    int nnCount = 10;
    int maxColors = 200;

	int index;
	Attribute attr;

    public ColorNormalizer(){
    }
    
    public ColorNormalizer(Attribute attr){
    	init(attr);
    }

	@Override
	public Double normalize(List<Object> record) {
		return normalize(record.get(index));
	}


	
    //normalizer:
    //pokud nalezne objekt se stejnym klicem vrati jeho hodnotu
    //pokud stejny klic v poli neni, spusti prohledavani pole
	/* a. projde vsechny prvky, spocita vzdalenost, 
    vyssledna hodnota je vazeny prumer z 
    1/vzdalenost * hodnota prvku	
    a2. projde pouze pevny (shora omezeny) pocet prvku
     ***podle me nejlepsi
    b. nalezne nejblizsi prvek (obdoba TopK?) a vrati jeho hodnotu
    c. heuristika: projde pouze cast prostoru v okoli prvku
    d. heuristika: vezmu nejblizsiho souseda z kazdeho pole, dopocitam vzdalenost...
    b. c. a d. vyzaduji "chytre" ukladat jednotlive slozky
    -> vytvorit si pole podle kazde slozky, tech muze byt promenny pocet!
    -> musim zjistit podle konfigurace kolik polozek to ma byt (nebo pro RGB pocitat 3)
     */
    /* moznost a. detailneji
     *  - nepotrebuje ukladat jednotlive int hodnoty do specialnich poli jako b. a c.
     *  - v nastaveni tedy neni treba specifikovat pocet atributu (RGB = 3...)
     *  - pro vsechny prvky v map: rozparsuje string podle ";", vypocita vzdalenost 
     *    napr jako sqrt( sqr(a1-b1)+sqr(a2-b2)+sqr(a3-b3)...)
     *  - udrzuje v pole: vzdalenost od objektu, double hodnota
     *  - pote co projde prvky vrati vazeny prumer hodnot:
     *  	sum(vzdalenost*hodnota)/sum(vzdalenost)
     * */
    public Double normalize(Object o) {
        //tento if mozna vyhodit a delat pro vsechny prvky
        if (!map.containsKey(o.toString())) {         
            //rozbaleni stringu do pole integeru
            String[] barvy_objekt = o.toString().split(";");
            int[] barvy_objekt_int = new int[barvy_objekt.length];
            for (int i = 0; i < barvy_objekt.length; i++) {
                barvy_objekt_int[i] = Integer.parseInt(barvy_objekt[i]);

            }
            //inicializace - 0.1 kvuli deleni nulou...
            Double sum_hodnoceni = 0.1;
            Double sum_distance = 0.1;
            int i = 0;
            //pro vsechny/limitovane mnozstvi prvku v mapu porovnam s objektem
            for (i = 0; i < map.size(); i++) {
                String key = (String) map.keySet().toArray()[i];
                Double val = (Double) map.values().toArray()[i];
                
                 //rozbaleni stringu do pole integeru
                String[] barvy = key.split(";");
                int[] barvy_int = new int[barvy_objekt.length];
                for (int j = 0; j < barvy.length; j++) {
                    barvy_int[j] = Integer.parseInt(barvy[j]);
                }
                
                //zjistim euklidovskou vzdalenost mezi objektem o a obj. z mapy
                int dist_sum = 0;
                for (int k = 0; k < barvy_objekt.length; k++) {
                    dist_sum += (barvy_objekt_int[k] - barvy_int[k])*(barvy_objekt_int[k] - barvy_int[k]);
                }
                Double eukl_vzdalenost = Math.sqrt(dist_sum);
                
                //priprava na vazeny prumer z 1/vzdalenost * val
                sum_hodnoceni += (1/eukl_vzdalenost)*val;
                sum_distance  += (1/eukl_vzdalenost);
                
                //pokud jsem dosahnul maximalniho poctu barev, prerusim vypocet
                if(i>=maxColors){ 
                    break;
                }
            }
            //vratim vysledek vazeneho prumeru
            return (sum_hodnoceni/sum_distance);

            /*
             * String nn[]=new String[nnCount]; int nnWeight[]=new int[nnCount];
             * for(Map.Entry<String, Double> entry :map.entrySet()) { String s =
             * entry.getKey(); String s2 = o.toString(); int i=0;
             * while(s.length()<i&&s2.length()<i&&s.charAt(i)==s2.charAt(i))
             * i++; if(i>0&&i>nnWeight[nnCount-1]){ for(int k=0;k<nnCount;k++){
             * if(i>nnWeight[k]){ int j = i; i = nnWeight[k]; nnWeight[k] = j;
             * String js = s; s = nn[k]; nn[k] = js; } } } } Double res= 0.0;
             * Double div= 0.0; for(int i=0;i<nnCount;i++){ res +=
             * nnWeight[i]*(map.get(nn[i])==null?0.0:map.get(nn[i])); div +=
             * nnWeight[i]; } if(div == 0.0) return 0.0; return res/div;
             */
            //double d1 = 0.0, d2 = 0.0;
           // int index = -al.indexOf(o.toString());
            // Object directly above the place where o would be.
           // if (index >= 0 && index < map.size() /*&& map.get(al.get(index)) != null*/) {
           //     d1 = map.get(al.get(index));
//
           // } else if (index == map.size()) {
           //     d1 = 0.0;
            //}

            // Object directly bellow the place where o would be.
           // if (index > 0 && index <= map.size() /*&& map.get(al.get(index - 1)) != null*/) {
            //    d2 = map.get(al.get(index - 1));

           // } else if (index == map.size()) {
            //    d2 = 0.0;
            //}
           // double div = 2.0;
           // if (d1 == 0.0) {
           //     div--;
           // }
           // if (d2 == 0.0) {
           //     div--;
           // }
           // if (div == 0.0) {
           //     return 0.0;
           // }
           // return ((d1 == 0.0 ? 0.0 : d1) + (d2 == 0.0 ? 0.0 : d2)) / div;
        } else {
            return map.get(o.toString());
        }
    }

    //pouze vraci ktery prvek je "vetsi", neni treba menit
    /*public int compare(Object arg0, Object arg1) {
        double a = Normalize(arg0);
        double b = Normalize(arg1);
        if (a < b) {
            return -1;
        }
        if (a > b) {
            return 1;
        }
        return 0;
    }*/

    //zpracovani hodnoty atributu
    //aribut se nejprve ulozi do mapy (stejne jak uz je)
	/*potom se rozparsuje podle ";" na jednotlive slozky
     *  !!!pozor, musim vedet kolik slozek ma obsahovat - nastaveni!!!
     *  slozky se ulozi do jednotlivych atributeLists spolu s odkazy na objekt
     *  	- nutn� pouze pro mo�nosti B, C a D u normalizeru 
     */
    public void init(Attribute attribute) {
    	index = attr.getIndex();
    	this.attr = attribute;
        for (AttributeValue attrVal : attribute.getValues()) {

            map.put(attrVal.getValue().toString(), attrVal.getRepresentant());
        //    al.add(attrVal.getValue().toString());
        /*
         * for(Double r : attrVal.getRatings())
         * if(!r.isInfinite()&&!r.isNaN()) {
         * map.put(attrVal.getValue().toString(), r);
         * al.add(attrVal.getValue().toString()); Collections.sort(al); }
         */
        }
        //Collections.sort(al, new AlphaComparator());
    }

    public Normalizer clone() {
        Normalizer n = new ColorNormalizer();
        return n;
    }

    public void configClassifier(XMLConfiguration config, String section) {
    // TODO Auto-generated method stub
    //konfigurace maxColors - pocet prohledavanych prvku pole
    //konfigurace normalizeru - jaka data se maji pouzit  
    }
    

	public int compare(List<Object> arg0, List<Object> arg1) {
		if (normalize(arg0) > normalize(arg1))
			return 1;
		else if (normalize(arg0) < normalize(arg1))
			return -1;
		else
			return 0;
	}

	@Override
	public double compareTo(Normalizer n) {
		// TODO Auto-generated method stub
		return 0;
	}

}

//pokud budeme uvazovat u normalizeru moznost a. pak nebude treba
//u moznosti b-d by stacil bud comparator integeru (pro kazde pole zvlast)
//nebo vymyslet comparator pro vsechna pole najednou... coz asi nepujde
//idea: jak slozite je udelat collections.sort pro kazdy prvek?
//      - comparator podle vzdalenosti od predaneho objektu?
class ColorComparator implements Comparator<Object> {
    String barvy_objekt;
    int[] barvy_objekt_int = new int[3];
    //konstruktoru dam string-barvy objektu podle ktereho delam comparaci
    public ColorComparator(String object_string) {
        barvy_objekt = object_string;
        String[] array_barvy_objekt = barvy_objekt.split(";");
        for (int i = 0; i < array_barvy_objekt.length; i++) {
            barvy_objekt_int[i] = Integer.parseInt(array_barvy_objekt[i]);
        }
    }

    public int compare(Object arg0, Object arg1) {
        if (arg0 == null && arg1 == null) {
            return 0;
        }
        if (arg0 == null) {
            return 1;
        }
        if (arg1 == null) {
            return -1;
        }
        //pokud mam oba objekty, rozbalim je a zjistim jejich vzdalenost od "porovnavaneho" objektu
       
        //zjistim int hodnoty barev pro oba argumenty
        String[] array_string_arg0 = arg0.toString().split(";");
        String[] array_string_arg1 = arg1.toString().split(";");
        int[] array_int_arg0 = new int[array_string_arg0.length];
        int[] array_int_arg1 = new int[array_string_arg1.length];
        for (int j = 0; j < array_string_arg0.length; j++) {            
            array_int_arg0[j] = Integer.parseInt(array_string_arg0[j]);
            array_int_arg1[j] = Integer.parseInt(array_string_arg0[j]);
        }  
        //vypocitam eukleidovskou vzdalenost pro oba argumenty od objektu
        int dist_sum0 = 0;
        int dist_sum1 = 0;
        for (int k = 0; k < barvy_objekt_int.length; k++) {
            dist_sum0 += (array_int_arg0[k] - barvy_objekt_int[k])*(array_int_arg0[k] - barvy_objekt_int[k]);
            dist_sum1 += (array_int_arg1[k] - barvy_objekt_int[k])*(array_int_arg1[k] - barvy_objekt_int[k]);
        }
        //vratim negativni pokud je prvni argument bliz k objektu, pozitivni pokud je druhy argument bliz, nulu pokud jsou stejne
        return dist_sum0 - dist_sum1;
    }
}
