package jmeter_plan_creation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;

/** JMeterPlanCreator
 *  
 *  a partire dal file col twitter dataset . le cui righe sono nel formato : 
 *  	< tstamp  	  tweet_per_min >
 *  	< tstamp+1min tweet_per_min >
 *  
 *  genera il file file jmeter test_plan nella cui porzione relativa al ThroughputShapingTimer
 *  saranno inserite delle entries tali che la curva generata sia quella indicata dal dataset
 * 
 * @author andrea-muti
 *
 */

public class JMeterPlanCreator {
	
	public static void main(String[] args){
			
		String separator = ",";
		boolean has_headers = false;
		String filepath = "";
		int single_duration_sec = 60;
		
		
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(filepath));
			// se c'è l'header lo salto
			if( has_headers ){
				String header = reader.readLine();
			}
			
			String line = reader.readLine();
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line,separator);
				String resultline = "";
				
				st.nextToken(); 						// primo token = tstamp
				String stringtoken = st.nextToken(); 	// secondo token = load value
				Double value = Double.parseDouble(stringtoken);
				
				// con il value letto devo creare le righe corrispondenti nel test plan
			
				
				String entry =   "<collectionProp name=\"-139272128\">\n"
							   + "<stringProp name=\"1537214\">"+value+"</stringProp>\n"
							   + "<stringProp name=\"1537214\">"+value+"</stringProp>\n"
							   + "<stringProp name=\"1722\">"+single_duration_sec+"</stringProp>\n"
							   +"</collectionProp>";
				
				line = reader.readLine();
			}
			
			reader.close();
		} catch (Exception e) {
			System.out.println(" EXCEPTION");
		} 
	}
}
