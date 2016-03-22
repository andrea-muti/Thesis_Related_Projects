package twitter_dataset;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

/** converts the twitter dataset from a format :  timestamp  n_tweets
 *  in a format  day_of_week, day, month, hour, n_load
 * 
 */

public class ConvertWDMHLoad {
	
	public static void main(String[] args){
		
		String file_path = "resources/out-romasicura-count-per-min.csv";
		String output_file_path = "resources/formatted_out-romasicura-count-per-min.csv";
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file_path));
			
			// line format : 1393714860000	1
			//     n token :   0            1   
			String line = reader.readLine();
			Calendar cal = new GregorianCalendar();
		    
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line);
				long time = Long.parseLong(st.nextToken()); 
				cal.setTimeInMillis(time);
				int month = cal.get(Calendar.MONTH);
				int year = cal.get(Calendar.YEAR);
				int day = cal.get(Calendar.DAY_OF_MONTH);
				
				int hours = cal.get(Calendar.HOUR_OF_DAY);
				int minutes = cal.get(Calendar.MINUTE);
				int seconds = cal.get(Calendar.SECOND); // sono sempre zero potrei toglierli
				
				int value = Integer.parseInt(st.nextToken()); 
				System.out.println(year+" "+month+" "+day+" "+hours+" "+minutes+" "+seconds+" "+value);
				
				line = reader.readLine();
			}
			reader.close();
			
		} catch (IOException e) {
			System.err.println("Error in opening|reading|closing the file: "+file_path);
			e.printStackTrace();
		}
		
		
	}
}
