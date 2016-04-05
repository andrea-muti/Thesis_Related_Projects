package twitter_dataset;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

/** converts the twitter dataset from a format :  timestamp  n_tweets
 *  in a format  w-day, hour,  carico,
 * 
 */

public class ConvertWHLoad {
	
	public static void main(String[] args){
		
		String file_path = "resources/datasets/overprovision_95p_halfhour_complete_twitter_dataset.csv";
		String output_file_path = "resources/datasets/formatted_overprovision_95p_halfhour_complete_twitter_dataset.csv";
		
		System.out.print(" - start conversion: ");
		
		try {
			FileReader fr = new FileReader(file_path);
			BufferedReader reader = new BufferedReader(fr);
			reader.mark(0);
		
			PrintWriter writer = new PrintWriter(output_file_path, "UTF-8") ;			

			// line format : 1393714860000	1
			//     n token :   0            1   
			String line = reader.readLine();
			Calendar cal = new GregorianCalendar();
		    
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line);
				long time = Long.parseLong(st.nextToken()); 
				cal.setTimeInMillis(time);
				
				// mi serve :  w-day, hour, load,
				int wday = cal.get(Calendar.DAY_OF_WEEK);
				//int daynum = cal.get(Calendar.DAY_OF_MONTH);
				
				int hours = cal.get(Calendar.HOUR_OF_DAY);
				int  load = (int) Double.parseDouble(st.nextToken()); 
				
				String output_line = wday+","+hours+","+load+",\n";
			
				writer.write(output_line);
				line = reader.readLine();
				
			}
			reader.close();
			fr.close();
			writer.close();
			
		} catch (IOException e) {
			System.err.println(" Error in opening|reading|closing the file: "+file_path);
			e.printStackTrace();
		}
		System.out.println(" DONE");
		
		
	}
}
