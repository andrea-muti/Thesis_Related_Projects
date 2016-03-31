package ThesisRelated.ClusterWorkloadPredictor;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;

import org.encog.ml.data.MLData;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.networks.BasicNetwork;
import org.encog.persist.EncogDirectoryPersistence;

public class ClusterWorkloadPredictor {
	
	private final String properties_file_path = "resources/properties_files/predictor.properties";
	
	// instance variables
	private String networkFile;
	private BasicNetwork predictonNetwork;
	private double min_workload_value;
	private double max_workload_value;
	
	
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
     	this.min_workload_value = Double.parseDouble(properties.getProperty("min_workload_value"));
     	this.max_workload_value = Double.parseDouble(properties.getProperty("max_workload_value"));
	}
	
	
	
	public double predict_load_in_next_x_minutes(int n_minutes){
		
		Calendar cal = new GregorianCalendar();
	
		long N_MINUTES   = 60 * 1000 * n_minutes;
		
		//long next_n_minutes = cal.getTime().getTime() + N_MINUTES;
		long next_n_minutes = 1393714800000L + N_MINUTES;
		
		cal.setTimeInMillis(next_n_minutes);
		
		double future_wday = cal.get(Calendar.DAY_OF_WEEK);
		double future_month = cal.get(Calendar.MONTH);
		double future_day_number = cal.get(Calendar.DAY_OF_MONTH);
		double future_hour = cal.get(Calendar.HOUR_OF_DAY);
		double future_minute = cal.get(Calendar.MINUTE);
		//System.out.println(" used ts : " +future_wday+" "+future_month+" "+future_day_number+" "+future_hour+" "+future_minute);
		
		double future_wday_normalized = normalize_value(future_wday, 1, 7);
		double future_month_normalized = normalize_value(future_month, 2, 4 );
		double future_day_number_normalized = normalize_value(future_day_number, 1, 31);
		double future_hour_normalized = normalize_value(future_hour, 0, 23);
		double future_minute_normalized = normalize_value(future_minute, 0, 59);
		//System.out.println("normalized month: "+future_month_normalized);
		double wday   = 0.833333;
		double month  = 1.0;
		double day_number   = 0.266667;
		double hour   = 0.434783;
		double minute = 0.559322;
		
		double[] input_values = { future_wday_normalized, future_month_normalized, 
								  future_day_number_normalized, future_hour_normalized, 
								  future_minute_normalized };
		
		//double[] input_values = {wday, month, day_number, hour, minute };
		
		
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

	
    public static void main( String[] args ) {
    	
    	System.out.println("********************************************");
        System.out.println("*** CLUSTER WORKLOAD PREDICTOR TEST MAIN ***");
        System.out.println("********************************************");
        
       try {
		ClusterWorkloadPredictor predictor = new ClusterWorkloadPredictor();
       
		
		double predicted_current_input_rate     = predictor.predict_load_in_next_x_minutes(-1);
		//System.out.println(" - predicted current input rate : "+predicted_current_input_rate);
      
		double predicted_next_10_min_input_rate  = predictor.predict_load_in_next_x_minutes(10);
		//System.out.println(" - predicted next 10 min input rate : "+predicted_next_10_min_input_rate);
		
		PrintWriter writer = new PrintWriter("/home/andrea-muti/Scrivania/pred_first_week_time_load.txt", "UTF-8") ;	
		
		System.out.println("writing data on file...");
		Calendar cal = new GregorianCalendar();
		int whole = 43200;
		int first_week = 10080;
		for(int i = 0 ; i<first_week; i=i+5){
			double  predicted_next_x_min_input_rate  = predictor.predict_load_in_next_x_minutes(i);
			long N_MINUTES   = 60 * 1000 * i;
			long next_n_minutes = 1393714800000L + N_MINUTES;
			
			
			cal.setTimeInMillis(next_n_minutes);
					
			int day_number = cal.get(Calendar.DAY_OF_MONTH);
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			int minute = cal.get(Calendar.MINUTE);
			
			writer.write(hour+":"+minute+":00 "+ String.format("%.8f", predicted_next_x_min_input_rate)+"\n");
		}
		System.out.println("done");
		writer.close();
		

       } 
       catch (Exception e) {
    	   System.err.println(" - ERROR : something wrong occurred");
    	   if(e.getMessage()!=null){System.out.println("   "+e.getMessage());}
       } 
    }
}
