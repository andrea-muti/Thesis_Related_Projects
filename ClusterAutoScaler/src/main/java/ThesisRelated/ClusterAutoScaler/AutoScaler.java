package ThesisRelated.ClusterAutoScaler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;

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
	private HashMap<Integer,String> node_addresses_map;
	private int avg_adding_time_sec_real_time;
	private int avg_cleanup_time_sec_real_time;
	private int avg_remove_time_sec_real_time;
	private int avg_adding_time_sec_wl_time;
	private int avg_cleanup_time_sec_wl_time;
	private int avg_remove_time_sec_wl_time;
	
	String contact_point_address;
	String jmx_port_number;
	
	private int current_node_number;
	private boolean someone_is_joining;
	private boolean cleanups_are_running;
	private boolean someone_is_leaving;
	
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
		
		this.avg_adding_time_sec_real_time = Integer.parseInt(properties.getProperty("avg_adding_time_sec").trim());
		this.avg_cleanup_time_sec_real_time = Integer.parseInt(properties.getProperty("avg_cleanup_time_sec").trim());
		this.avg_remove_time_sec_real_time = Integer.parseInt(properties.getProperty("avg_removing_time_sec").trim());
		
		this.avg_adding_time_sec_wl_time = (int) ((60.0/this.single_duration_sec)*this.avg_adding_time_sec_real_time);
		this.avg_cleanup_time_sec_wl_time = (int) ((60.0/this.single_duration_sec)*this.avg_cleanup_time_sec_real_time);
		this.avg_remove_time_sec_wl_time = (int) ((60.0/this.single_duration_sec)*this.avg_remove_time_sec_real_time);
		
		this.min_num_nodes = Integer.parseInt(properties.getProperty("min_num_nodes").trim());
		this.max_num_nodes = Integer.parseInt(properties.getProperty("max_num_nodes").trim());
		this.max_throughput_percentage = Integer.parseInt(properties.getProperty("max_throughput_percentage").trim()) / 100.0;
	
		// read max throughput level
		max_throughput_levels = new HashMap<Integer,Double>();
		for(int i=this.min_num_nodes; i<=this.max_num_nodes; i++){
			int max_th = Integer.parseInt(properties.getProperty("max_throughput_"+i+"_nodes").trim());
			double to_parse =  max_th * this.max_throughput_percentage;
			double value = Double.parseDouble( String.format("%.3f", to_parse ).replace(",", ".") );
			this.max_throughput_levels.put(i, value);
		}
		
		// populating the node addresses map
		this.node_addresses_map = new HashMap<Integer, String>();
		for(int i=0; i<this.max_num_nodes;i++){
			this.node_addresses_map.put(i, properties.getProperty("address_node_"+i).trim());
		}
		
		// instantiating the predictors
		try {
			this.predictor = new ClusterWorkloadPredictor(predictor_properties_path);
		} catch (Exception e) {
			System.err.println(" - [AutoScaler] ERROR : failed initialization of ClusterWorkloadPredictor");
			e.printStackTrace();
			System.exit(0);
		}
		
		// instantiating the configuration manager
		this.config_manager = new ConfigurationManager(config_man_properties_path);
		
		// instantiating the time tracker 
		this.initial_wl_tstamp = this.predictor.getInitialWorkloadTimestamp();
		this.time_tracker = new WorkloadTimeTracker(this.single_duration_sec);
		
		// print max input rate levels
		this.max_throughput_levels.entrySet();
		for(Entry<Integer, Double> e : this.max_throughput_levels.entrySet()){
			int n_nodes = e.getKey();
			double max = e.getValue();
			System.out.println(" - [AutoScaler] in a "+n_nodes+" nodes cluster the max input rate admitted is "+max+" req/sec");
		}
		
		// Check node number
		this.contact_point_address = properties.getProperty("contact_point_address").trim();
		this.jmx_port_number = properties.getProperty("jmx_port_number").trim();
		this.current_node_number = count_nodes(contact_point_address, jmx_port_number);
		// facciamo finta di avere 3 nodi attivi [per fare qualche piccolo test con cassandra spenta]
		//this.current_node_number = 3;
		check_min_node_number_for_the_experiment(this.current_node_number);
		System.out.println(" - [AutoScaler] there are "+this.current_node_number+" nodes in the cluster");
		
	}
	

	public void start(){
		System.out.println(" - [AutoScaler] autoscaler started");
		
        System.out.println(" - [AutoScaler] 1 minute of real time corresponds to "+(60/this.single_duration_sec)+" minutes of workload time");
        
        System.out.println(" - [AutoScaler] avg adding time (wl time) : "+String.format("%.3f", this.avg_adding_time_sec_wl_time/60.0).replace(",", ".")+" min");
        System.out.println(" - [AutoScaler] avg remove time (wl time) : "+String.format("%.3f", this.avg_remove_time_sec_wl_time/60.0).replace(",", ".")+" min");
        System.out.println(" - [AutoScaler] avg cleanup time (wl time) : "+String.format("%.3f", this.avg_cleanup_time_sec_wl_time/60.0).replace(",", ".")+" min");
		
		String start_date_time = this.convert_ts_to_string_date(this.initial_wl_tstamp);
		System.out.println(" - [AutoScaler] simulation time starts @ "+start_date_time);
		
		this.time_tracker.start();
		System.out.println(" - [AutoScaler] time_tracker started [using single_duration_sec:"+this.single_duration_sec+"]");
			
		double[] future_loads = new double[4];
		
		int n_sec_between_sampling = 30; // secondi di real time --> che corrispondono a 30/2=15 min di WL time
										 	
		// SHIFT IN AVANTI DELLA SIMULAZIONE [ ATTENZIONE!!! ]
		//this.initial_wl_tstamp = this.initial_wl_tstamp + 1000*60*60*8; 
		
		// TODO : invece che un for dovrebbe essere un while, 
		// che si ferma quando qualcuno chiede di stoppare l'autoscaler
		for(int i = 0; i<20000;i++){	
			long elapsed_sec = (long) time_tracker.getElapsed();
			double elapsed_min = Double.parseDouble(String.format("%.3f", elapsed_sec/60.0).replace(",", "."));
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
			
			future_loads[0] = current_predicted_load;
			future_loads[1] = next_15min_predicted_load;
			future_loads[2] = next_half_hour_predicted_load;
			future_loads[3] = next_hour_predicted_load;
			
			// DECIDE the most adequate scaling action to perform
			ScalingAction action = decide_scaling_action(wl_now_ts, future_loads);
			
			// EXECUTE the decided scaling action
			execute_scaling_action(action);
					
			try{ Thread.sleep(1000*n_sec_between_sampling); }
		    catch(Exception e){}
		}
		
	}
	
	private void execute_scaling_action(ScalingAction action){
		System.out.println(" - [AutoScaler] scaling decision [for the next hour]: "+action.format_as_printable_message(this.current_node_number));
		
		String ip_address="the_ip_of_the_node_to_remove";
		
		if(!action.getAction().equals(AutoScaleConstants.KEEP_CURRENT)){
			String s="";
			if(action.getNumber()>1){ s = s+"s"; }
			int min_plus;
			if( action.getAction().equals(AutoScaleConstants.SCALE_OUT) ){ // WE HAVE TO SCALE OUT
				min_plus=1;
				System.out.println("      [AutoScaler] calling the Configuration Manager to ADD "+action.getNumber()+" node"+s+" from the cluster");
				this.someone_is_joining=true;
				for(int i=0; i<action.getNumber(); i++){
					int index_node_to_add = this.current_node_number + i;
					String address_node_to_add = this.node_addresses_map.get(index_node_to_add);
					this.config_manager.fake_addNode(address_node_to_add,this.jmx_port_number);
				}
				this.someone_is_joining=false;
			}
			else{ // WE HAVE TO SCALE IN
				 min_plus=-1;
				 System.out.println("      [AutoScaler] calling the Configuration Manager to REMOVE "+action.getNumber()+" node"+s+" from the cluster");
				 this.someone_is_joining=true;
				 for(int i=0; i<action.getNumber(); i++){
					int index_node_to_remove = this.current_node_number - i -1;
					String address_node_to_remove = this.node_addresses_map.get(index_node_to_remove);
					this.config_manager.fake_removeNode(address_node_to_remove, this.jmx_port_number);
				 }
				 this.someone_is_leaving=false;
			}

			
			
			// update the current number of nodes in the cluster
			this.current_node_number = this.current_node_number + (min_plus*action.getNumber()) ;
			System.out.println("      [AutoScaler] Configuration Manager execution completed - now there are "+this.current_node_number+" nodes in the cluster");
		}

	}
	
	
	private ScalingAction decide_scaling_action(long wl_now_ts, double[] future_loads){
		
		double current_predicted_load = future_loads[0];
		double next_15min_predicted_load = future_loads[1];
		double next_half_hour_predicted_load  = future_loads[2];
		double next_hour_predicted_load = future_loads[3];
		
		int required_now = this.compute_required_node_number(current_predicted_load);
		int required_15min = this.compute_required_node_number(next_15min_predicted_load);
		int required_half_hour = this.compute_required_node_number(next_half_hour_predicted_load);
		int required_one_hour = this.compute_required_node_number(next_hour_predicted_load);
		
		String wl_now_date = convert_ts_to_string_date(wl_now_ts);
		System.out.println("\n - [AutoScaler] workload time "+wl_now_date+"");
		System.out.println("       - predicted current load    : "+current_predicted_load+" req/sec | num nodes required : "+required_now);
		System.out.println("       - predicted next 15min load : "+next_15min_predicted_load+" req/sec | num nodes required : "+required_15min);
		System.out.println("       - predicted next 30min load : "+next_half_hour_predicted_load+" req/sec | num nodes required : "+required_half_hour);
		System.out.println("       - predicted next 60min load : "+next_hour_predicted_load+" req/sec | num nodes required : "+required_one_hour);
		
		// DOBBIAMO DECIDERE COSA FARE PER ESSERE OK TRA 1 ORA
		ScalingAction action;
		if(this.current_node_number > required_one_hour){ // SCALE IN
			int how_many_to_remove = this.current_node_number - required_one_hour;
			action = new ScalingAction(AutoScaleConstants.SCALE_IN, how_many_to_remove);
		}
		else if(this.current_node_number < required_one_hour){  // SCALE OUT
			int how_many_to_add = required_one_hour - this.current_node_number;
			action = new ScalingAction(AutoScaleConstants.SCALE_OUT, how_many_to_add);
		}
		else{ // KEEP CURRENT
			action = new ScalingAction(AutoScaleConstants.KEEP_CURRENT, 0);
		}
		
	
		return action;
		
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
	
	private void check_min_node_number_for_the_experiment(int number){
		if(this.current_node_number==-1){
			System.err.println(" - [AutoScaler] ERROR : impossible to communicate with the cluster");
			System.exit(0);
		}
		if( this.current_node_number < this.min_num_nodes ){
			System.err.println(" - [AutoScaler] ERROR : there are "+this.current_node_number+" nodes in the cluster but the minimum number of nodes in order to run the experiment is "+this.min_num_nodes);
			System.exit(0);
		}
	}
	
	private int count_nodes(String contact_point_addr, String jmx_port) {
		int number = 0;
		number = ClusterUtils.countNodes(contact_point_addr, jmx_port);
		return number;
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
	
	
    public static void main( String[] args ){
        System.out.println(" **** AUTOSCALER [main di prova] ****\n");
        String autoscaler_properties_path = "resources/properties_files/autoscaler.properties";
        String predictor_properties_path = "resources/properties_files/predictor.properties";
        String conf_man_prop_path = "resources/properties_files/propertiesCM.properties";
        int single_duration_sec = 12; // 1 minuto vero = 5 minuti simulati
        int scaling_factor = 810; 
        AutoScaler scaler = new AutoScaler( autoscaler_properties_path, predictor_properties_path, 
        							        conf_man_prop_path, single_duration_sec, scaling_factor );
        scaler.start();
    }
}
