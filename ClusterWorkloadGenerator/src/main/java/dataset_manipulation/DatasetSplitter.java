package dataset_manipulation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

public class DatasetSplitter {
	
	
	public static void main(String[] args){
		
		String file_path = "files/datasets/complete_twitter_dataset.csv";
		
		String split_range = "DAY"; // "MINUTE", "HOUR" , "DAY", "WEEK", "MONTH"
		
		int n_minutes = 0;
		
		if(split_range.equals("MINUTE") ){ n_minutes = 1; }
		else if( split_range.equals("HOUR") ){ n_minutes = 60; }
		else if( split_range.equals("DAY") ){  n_minutes = 60*24; }
		else if( split_range.equals("WEEK") ){  n_minutes = 60*24*7; }
		else if( split_range.equals("MONTH") ){  n_minutes = 60*24*7*31; }

		long split_interval = 1000 * 60 * n_minutes; // in  msec
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file_path));
			
			// line format : 0.00 0.301
			//     n token :   0    1   
			String firstline = reader.readLine();
			StringTokenizer st = new StringTokenizer(firstline);
			long start_timestamp = Long.parseLong(st.nextToken()); 
			
			long value = Long.parseLong(st.nextToken()); 
						
			String line = reader.readLine();
		
			int i = 1;
			
			String file_name = "files/datasets/workload_"+split_range.toLowerCase()+"_"+i+".csv";
			
			PrintWriter writer = new PrintWriter(file_name, "UTF-8");
			
			while( line!=null ){
				
				st = new StringTokenizer(line);
				long timestamp = Long.parseLong(st.nextToken()); 
				
				if(timestamp > start_timestamp + split_interval){ 
					System.out.println(" - END "+split_range+" N° "+i);
					writer.close();
					i++;
					file_name = "files/datasets/workload_"+split_range.toLowerCase()+"_"+i+".csv";
					writer = new PrintWriter(file_name, "UTF-8");
					start_timestamp = timestamp;		
				}
				
				value = Long.parseLong(st.nextToken()); 
				
				String to_write = timestamp+" "+value+"\n";
			
				writer.write(to_write);
				
				line = reader.readLine();
			
			}
			writer.close();
			reader.close();
		
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_path);
			e.printStackTrace();
		}
		
		
	}
}
