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
	 * @param properties_file_path : path to the file containing the required propertiess
	 * @throws Exception 
	 */
	public ClusterWorkloadPredictor(String properties_file_path) throws Exception{
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
     	System.out.println(" - [ClusterWorkloadPredictor] initial timestamp from workload file: "+this.workloadFilePath);	
	}
	
	/** ClusterWorkloadPredictor
	 * 	public Constructor che usa il file di properties resources/properties_files/predictor.properties
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
     	System.out.println(" - initial timestamp from workload file: "+this.workloadFilePath);
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
			System.err.println(" - ERROR opening/reading/closing the workload file\n   ["+e.getMessage()+"]\n - EXITING");
			System.exit(0);
		}
		
		return result;
	}
	
	// predice il carico dopo x hours a partire dall'initial timestamp del workload file 
	public double predict_load_in_next_x_hours_from_start(int n_hours){

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
	
	/** predict_load_at_time()
	 * 		predicts the workload at the instant in time specified by the timestamp given as argumnet
	 * @param timestamp : timestamp (msecs form epoch) of the point in time we want to predict the load
	 * @return the predicted load
	 */
	public double predict_load_at_time(long timestamp){

		Calendar cal = new GregorianCalendar();
		
		cal.setTimeInMillis(timestamp);
		
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
       
    	   String outfile_path = "resources/datasets/predictions/prediction_day_27_time_load.csv"; 
		 
    	   PrintWriter writer = new PrintWriter(outfile_path, "UTF-8") ;	
		
    	   System.out.println(" - writing data on file "+outfile_path);
    	   Calendar cal = new GregorianCalendar();
    	   int hour_length = 60;					// how many minutes in one hour
    	   int day_length = 24 * hour_length; 		// how many minutes in a day
    	   int week_length = day_length * 7;		// how many minutes in a week
    	   //  int complete_workload_length = week_length * 6;	// how many minutes in the complete dataset (actually, the complete dataset is 41 days, not 42)
		
    	   for(int i = 0 ; i<day_length; i++){
    		  
    		   long MINUTES   = 1000 * 60 * i;
    		   long next_n_mins = predictor.getInitialWorkloadTimestamp() + MINUTES;
    		   double  predicted_next_x_hours_input_rate  = predictor.predict_load_at_time(next_n_mins);
			
    		   cal.setTimeInMillis(next_n_mins);
    		   int hour = cal.get(Calendar.HOUR_OF_DAY);
    		   int minute = cal.get(Calendar.MINUTE);
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
