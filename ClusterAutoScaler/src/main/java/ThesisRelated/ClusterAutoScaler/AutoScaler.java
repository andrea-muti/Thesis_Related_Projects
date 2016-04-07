package ThesisRelated.ClusterAutoScaler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import ThesisRelated.ClusterConfigurationManager.ConfigurationManager;
import ThesisRelated.ClusterWorkloadPredictor.ClusterWorkloadPredictor;

/**
 * Cluster AutoScaler
 * 
 * @author andrea-muti
 * @since 07-04-2016
 */

public class AutoScaler{
	
	private static final long ONE_HOUR    = 60 * 60 * 1000;
	private static final long HALF_HOUR   = 30 * 60 * 1000;
	private static final long FIFTEEN_MIN = 15 * 60 * 1000;
	
	private ClusterWorkloadPredictor predictor;
	private ConfigurationManager config_manager;
	private WorkloadTimeTracker time_tracker;
	private int single_duration_sec;
	private int scaling_factor;
	private long initial_wl_tstamp;
	private int min_num_nodes;
	private int max_num_nodes;
	private double max_throughput_percentage;
	private HashMap<Integer,Double> max_throughput_levels;
	
	public AutoScaler( String autoscaler_properties_path, String predictor_properties_path, 
					   String config_man_properties_path, int single_duration_sec, int scaling_factor ){
		this.single_duration_sec = single_duration_sec;
		this.scaling_factor = scaling_factor;
		
		// reading properties file
		File propFile = new File(autoscaler_properties_path);
		Properties properties = new Properties();
		try {
			properties.load(new FileReader(propFile));
		} catch (IOException e1) {
			System.err.println(" - [AutoScaler] ERROR reading autoscaler properties file "+autoscaler_properties_path);
			e1.printStackTrace();
			System.exit(0);
		}
		this.min_num_nodes = Integer.parseInt(properties.getProperty("min_num_nodes").trim());
		this.max_num_nodes = Integer.parseInt(properties.getProperty("max_num_nodes").trim());
		this.max_throughput_percentage = Integer.parseInt(properties.getProperty("max_throughput_percentage").trim()) / 100.0;
		max_throughput_levels = new HashMap<Integer,Double>();
		
		for(int i=this.min_num_nodes; i<=this.max_num_nodes; i++){
			int max_th = Integer.parseInt(properties.getProperty("max_throughput_"+i+"_nodes").trim());
			double to_parse =  max_th * this.max_throughput_percentage;
			double value = Double.parseDouble( String.format("%.3f", to_parse ).replace(",", ".") );
			this.max_throughput_levels.put(i, value);
		}

		try {
			this.predictor = new ClusterWorkloadPredictor(predictor_properties_path);
		} catch (Exception e) {
			System.err.println(" - [AutoScaler] ERROR : failed initialization of ClusterWorkloadPredictor");
			e.printStackTrace();
			System.exit(0);
		}
		this.config_manager = new ConfigurationManager(config_man_properties_path);
		this.initial_wl_tstamp = this.predictor.getInitialWorkloadTimestamp();
		this.time_tracker = new WorkloadTimeTracker(this.single_duration_sec);
		
		this.max_throughput_levels.entrySet();
		for(Entry<Integer, Double> e : this.max_throughput_levels.entrySet()){
			int n_nodes = e.getKey();
			double max = e.getValue();
			System.out.println(" - in a "+n_nodes+" nodes cluster the max input rate admitted is "+max+" req/sec");
		}
		
	}
	
	private String convert_ts_to_string_date(long ts){
		Calendar c = new GregorianCalendar();
		c.setTimeInMillis(ts);
		int day = c.get(Calendar.DAY_OF_MONTH);
		int month = c.get(Calendar.MONTH)+1;
		int year = c.get(Calendar.YEAR);
		int hour = c.get(Calendar.HOUR_OF_DAY);
		int min = c.get(Calendar.MINUTE);
		int sec = c.get(Calendar.SECOND);
		String date = year+"-"+month+"-"+day+"  "+hour+":"+min+":"+sec;
		return date;
	}
	
	public void start(){
		System.out.println(" - [AutoScaler] autoscaler is executing");

		String start_date_time = this.convert_ts_to_string_date(this.initial_wl_tstamp);
		System.out.println(" - [AutoScaler] simulation time starts @ "+start_date_time);
		
		this.time_tracker.start();
		System.out.println(" - [AutoScaler] time_tracker started [using single_duration_sec:"+this.single_duration_sec+"]");
		
		
		int n_sec_between_sampling = 60;
		
		// TODO : invece che un for dovrebbe essere un while, 
		// che si ferma quando qualcuno chiede di stoppare l'autoscaler
		for(int i = 0; i<50;i++){	
			long elapsed_sec = (long) time_tracker.getElapsed();
			double elapsed_min = elapsed_sec/60.0;
			double real_elapsed_min = (i*n_sec_between_sampling)/60.0;
			System.out.print("\n - [AutoScaler] real time elapsed: "+real_elapsed_min+" min --> WL time elapsed: "+elapsed_min+" min");
			
			long wl_now_ts = this.initial_wl_tstamp + (1000*elapsed_sec);
			long wl_next_15min_ts = wl_now_ts + FIFTEEN_MIN;
			long wl_next_half_hour_ts = wl_now_ts + HALF_HOUR;
			long wl_next_hour_ts = wl_now_ts + ONE_HOUR;
			
			
			double current_predicted_load = this.predictor.predict_load_at_time(wl_now_ts) * (this.scaling_factor);
			double next_15min_predicted_load = this.predictor.predict_load_at_time(wl_next_15min_ts) * (this.scaling_factor);
			double next_half_hour_predicted_load  = this.predictor.predict_load_at_time(wl_next_half_hour_ts) * (this.scaling_factor);
			double next_hour_predicted_load  = this.predictor.predict_load_at_time(wl_next_hour_ts) * (this.scaling_factor);
			
			current_predicted_load = Double.parseDouble(String.format("%.3f", current_predicted_load).replace(",", "."));
			next_15min_predicted_load = Double.parseDouble(String.format("%.3f", next_15min_predicted_load).replace(",", "."));
			next_half_hour_predicted_load = Double.parseDouble(String.format("%.3f", next_half_hour_predicted_load).replace(",", "."));
			next_hour_predicted_load = Double.parseDouble(String.format("%.3f", next_hour_predicted_load).replace(",", "."));
			
			String wl_now_date = convert_ts_to_string_date(wl_now_ts);
			System.out.println("\n - [AutoScaler] workload time "+wl_now_date+"");
			System.out.println("       - predicted current load    : "+current_predicted_load+" req/sec | num nodes required : "+this.compute_required_node_number(current_predicted_load));
			System.out.println("       - predicted next 15min load : "+next_15min_predicted_load+" req/sec | num nodes required : "+this.compute_required_node_number(next_15min_predicted_load));
			System.out.println("       - predicted next 30min load : "+next_half_hour_predicted_load+" req/sec | num nodes required : "+this.compute_required_node_number(next_half_hour_predicted_load));
			System.out.println("       - predicted next 60min load : "+next_hour_predicted_load+" req/sec | num nodes required : "+this.compute_required_node_number(next_hour_predicted_load));
			try{ Thread.sleep(1000*n_sec_between_sampling); }
		    catch(Exception e){}
		}
		
	}
	
	public int compute_required_node_number(double load){
		int n = this.max_num_nodes;
		for(int i = this.min_num_nodes; i<=this.max_num_nodes; i++){
			double level_at_i_nodes = this.max_throughput_levels.get(i);
			if( load < level_at_i_nodes){
				n = i;
				break;
			}
		}
		return n;
	}
	
    public static void main( String[] args ){
        System.out.println(" **** AUTOSCALER [main di prova] ****\n");
        String autoscaler_properties_path = "resources/properties_files/autoscaler.properties";
        String predictor_properties_path = "resources/properties_files/predictor.properties";
        String conf_man_prop_path = "resources/properties_files/propertiesCM.properties";
        int single_duration_sec = 2; // 1 minuto vero = 12 minuti simulati
        int scaling_factor = 810; 
        AutoScaler scaler = new AutoScaler( autoscaler_properties_path, predictor_properties_path, 
        							        conf_man_prop_path, single_duration_sec, scaling_factor );
        scaler.start();
    }
}
