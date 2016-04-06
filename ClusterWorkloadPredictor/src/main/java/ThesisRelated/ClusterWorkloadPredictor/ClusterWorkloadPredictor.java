package ThesisRelated.ClusterWorkloadPredictor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.StringTokenizer;

import org.encog.ml.data.MLData;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.networks.BasicNetwork;
import org.encog.persist.EncogDirectoryPersistence;

public class ClusterWorkloadPredictor {
	
	private final String properties_file_path = "resources/properties_files/predictor.properties";
	
	// instance variables
	private String networkFile;
	private BasicNetwork predictonNetwork;
	private String workloadFilePath;
	private double min_workload_value;
	private double max_workload_value;
	private long initial_timestamp;
	
	
	/** ClusterWorkloadPredictor
	 * 	public Constructor
	 * @throws Exception 
	 */
	public ClusterWorkloadPredictor() throws Exception{
		
		// reading properties file
		File propFile = new File(properties_file_path);
		Properties properties = new Properties();
		properties.load(new FileReader(propFile));		
		
		// setting variables from properties
		this.networkFile = properties.getProperty("networkFile");
		this.predictonNetwork = (BasicNetwork) EncogDirectoryPersistence.loadObject(new File(this.networkFile));
     	this.workloadFilePath=properties.getProperty("workload_file_path");
		this.min_workload_value = Double.parseDouble(properties.getProperty("min_workload_value"));
     	this.max_workload_value = Double.parseDouble(properties.getProperty("max_workload_value"));
     	this.initial_timestamp = getInitialWorkloadTimestamp();
     	System.out.println(" - initial timestamp taken from workload file: "+this.workloadFilePath);
	}
	
	public String getWorkloadFilePath() {
		return this.workloadFilePath;
	}
	
	public long getInitialWorkloadTimestamp(){
		long result = 0;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(this.workloadFilePath));
			String first_line = br.readLine();
			
			StringTokenizer strtok = new StringTokenizer(first_line);
			result = Long.parseLong(strtok.nextToken());
			br.close();
			
		} catch (Exception e) {
			System.err.println(" - ERROR opening/reading/closing the workload file\n - EXITING");
			System.exit(0);
		}
		
		return result;
	}
	
	public double predict_load_in_next_x_hours(int n_hours){

		Calendar cal = new GregorianCalendar();
		
		// how many milliseconds in n_hours hours
		long N_HOURS   = 60 * 60 * 1000 * n_hours;
		
		// point in time (msec from epoc) to predict wrt the initial workload timestamp
		long next_n_hours = this.initial_timestamp + N_HOURS;  
		
		cal.setTimeInMillis(next_n_hours);
		
		double future_wday = cal.get(Calendar.DAY_OF_WEEK);
		double future_hour = cal.get(Calendar.HOUR_OF_DAY);
	
		//System.out.println(" used ts : " +future_wday+" "+future_hour+" "+future_minute);
		
		double future_wday_normalized = normalize_value(future_wday, 1, 7);
		double future_hour_normalized = normalize_value(future_hour, 0, 23);
	
		double[] input_values = { future_wday_normalized,  future_hour_normalized };
	
		BasicMLData data = new BasicMLData(input_values);

		// compute the forecast and get the output values
		MLData computation = this.predictonNetwork.compute(data);
		double[] output_data = computation.getData();		
		return denormalize_value(output_data[0], this.min_workload_value, this.max_workload_value);
	}
	
	private double normalize_value(double value, double min, double max){
		double normalized = (value - min) / (max - min);
		return normalized;
	}
	
	private double denormalize_value(double normalized, double min, double max) {
		double denormalized = ((normalized * (max - min)) + min);
		return denormalized;
	}
	
	/** getPredictionNetwork()
	 * @return the BasicNetwork predictionNetwork used by this ClusterWorkloadPredictor
	 */
	public BasicNetwork getPredictionNetwork(){
		return this.predictonNetwork;
	}

	
    @SuppressWarnings("unused")
	public static void main( String[] args ) {
    	
    	System.out.println("********************************************");
        System.out.println("*** CLUSTER WORKLOAD PREDICTOR TEST MAIN ***");
        System.out.println("********************************************");
        
       try {
    	   ClusterWorkloadPredictor predictor = new ClusterWorkloadPredictor();
       
    	   String outfile_path = "resources/datasets/predictions/prediction_day_16_time_load.csv"; 
		 
    	   PrintWriter writer = new PrintWriter(outfile_path, "UTF-8") ;	
		
    	   System.out.println(" - writing data on file "+outfile_path);
    	   Calendar cal = new GregorianCalendar();
    	   int day_length = 24; 							// how many hours in a day
    	   int week_length = day_length * 7;				// how many hours in a week
    	   //  int complete_workload_length = week_length * 6;	// how many hours in the complete dataset (actually, the complete dataset is 41 days, not 42)
		
    	   for(int i = 0 ; i<day_length; i++){
    		   double  predicted_next_x_hours_input_rate  = predictor.predict_load_in_next_x_hours(i);
    		   long N_HOURS   = 60 * 60 * 1000 * i;
    		   long next_n_hours = predictor.getInitialWorkloadTimestamp() + N_HOURS;
			
    		   cal.setTimeInMillis(next_n_hours);
    		   int hour = cal.get(Calendar.HOUR_OF_DAY);
    		   writer.write(hour +" "+ String.format("%.8f", predicted_next_x_hours_input_rate)+"\n");
    	   }
    	   System.out.println(" - done");
    	   writer.close();
		
       } 
       catch (Exception e) {
    	   System.err.println(" - ERROR : something wrong occurred");
    	   e.printStackTrace();
    	   if(e.getMessage()!=null){System.out.println("   "+e.getMessage());}
       } 
       
    
    }

   

	
}
