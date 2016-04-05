package twitter_dataset;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.StringTokenizer;

public class ConvertOverProvisionHour {
	
	
	public static void main(String[] args){
		
		String input_file_path = "/home/andrea-muti/Scrivania/dataset_twitter/complete_twitter_dataset.csv";
		String output_file_path = "/home/andrea-muti/Scrivania/dataset_twitter/overprovision_1h_complete_twitter_dataset.csv";
		
		int percentile_overprovision = 95;
		
		try {

			PrintWriter writer = new PrintWriter(output_file_path, "UTF-8") ;	
		
			BufferedReader reader = new BufferedReader(new FileReader(input_file_path));
			
			// line format : 0.00 0.301
			//     n token :   0    1   
			String first_line_of_hour = reader.readLine();
			double i = 1;
			int minute = 1;
			int hour_counter = 1;
			String line = first_line_of_hour;
			long initial_ts_of_current_hour = 0;
			double[] values_current_hour = new double[60];
			
			while( line!=null ){
				
				StringTokenizer st = new StringTokenizer(line, "\t");
				long time = Long.parseLong(st.nextToken()); 
				if( minute == 1 ){ initial_ts_of_current_hour = time; }

				double value = Double.parseDouble(st.nextToken()); 
				//System.out.println("     - value n*"+minute+" : "+value);
				values_current_hour[minute-1] = value;
	
				if(minute==60){
					Arrays.sort(values_current_hour);
					int index_x_percentile = (int) ((values_current_hour.length/100.0)*percentile_overprovision);
					double xPercentile_current_hour = values_current_hour[index_x_percentile];
					System.out.println("["+hour_counter+"] [ "+initial_ts_of_current_hour+" ; "+xPercentile_current_hour+" ]");
					minute = 1;
					hour_counter++;
					String to_write = initial_ts_of_current_hour+" "+xPercentile_current_hour+"\n";
					writer.write(to_write);
				}
				else{
					minute ++;
				}
			
				if( (i % 10000) == 0 ){ 
					String top = "    - "+i+" file lines analyzed";
					System.out.println(top);
				} 
				
				line = reader.readLine();
				
				i++;
			}
			reader.close();
			

			writer.close();
			
			
		} catch (IOException e) {
			System.err.println("\n - Error manipulation in/out files");
			e.printStackTrace();
		}			
	}

}