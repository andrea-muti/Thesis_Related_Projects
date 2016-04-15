package ThesisRelated.ClusterAutoScaler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
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

@SuppressWarnings("unused")
public class AutoScaler{

	private ClusterWorkloadPredictor predictor;
	private ConfigurationManager config_manager;
	private WorkloadTimeTracker time_tracker;
	private int single_duration_sec;
	private int scaling_factor;
	private int number_hours_initial_shift;
	private long initial_wl_tstamp;
	private int min_num_nodes;
	private int max_num_nodes;
	private double max_throughput_percentage;
	private HashMap<Integer,Double> max_throughput_levels;
	private HashMap<Integer,String> node_addresses_map;
	private int avg_adding_time_sec_real_time;
	private int avg_cleanup_time_sec_real_time;
	private HashMap<Integer,Integer> avg_remove_time_sec_real_time_map;
	private int avg_adding_time_sec_wl_time;
	private int avg_cleanup_time_sec_wl_time;
	private HashMap<Integer,Integer> avg_remove_times_sec_wl_time_map;
	
	String contact_point_address;
	String jmx_port_number;
	
	private int current_node_number;
	private boolean someone_is_joining;
	private boolean cleanups_are_running;
	private boolean someone_is_leaving;
	
	private boolean execute = false;
	
	public AutoScaler( String autoscaler_properties_path, String predictor_properties_path, 
			   String config_man_properties_path, int single_duration_sec, int scaling_factor){
		this(autoscaler_properties_path, predictor_properties_path, config_man_properties_path, single_duration_sec, scaling_factor, 0);	
	}
		
	public AutoScaler( String autoscaler_properties_path, String predictor_properties_path, 
					   String config_man_properties_path, int single_duration_sec, int scaling_factor, int number_hours_initial_shift ){
		
		this.number_hours_initial_shift = number_hours_initial_shift;
		
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
		
		this.avg_adding_time_sec_real_time = Integer.parseInt(properties.getProperty("avg_adding_time_sec").trim());
		this.avg_cleanup_time_sec_real_time = Integer.parseInt(properties.getProperty("avg_cleanup_time_sec").trim());
	
		this.avg_remove_time_sec_real_time_map = new HashMap<Integer,Integer>();
		for(int i=this.max_num_nodes; i>this.min_num_nodes; i--){
			this.avg_remove_time_sec_real_time_map.put(i, Integer.parseInt(properties.getProperty("avg_removing_time_sec_"+i+"_to_"+(i-1)).trim()));
		}
		
		this.avg_remove_times_sec_wl_time_map = new HashMap<Integer,Integer>();
		for(int i=this.max_num_nodes; i>this.min_num_nodes; i--){
			int value_to_conv = this.avg_remove_time_sec_real_time_map.get(i);
			this.avg_remove_times_sec_wl_time_map.put(i, (int) ((60.0/this.single_duration_sec)*value_to_conv));
		}
		
		this.avg_adding_time_sec_wl_time = (int) ((60.0/this.single_duration_sec)*this.avg_adding_time_sec_real_time);
		this.avg_cleanup_time_sec_wl_time = (int) ((60.0/this.single_duration_sec)*this.avg_cleanup_time_sec_real_time);
		//this.avg_remove_time_sec_wl_time = (int) ((60.0/this.single_duration_sec)*this.avg_remove_time_sec_real_time);
		
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
		
		// commentare riga sopra e 
		// scommentare riga sotto per fare finta di avere x nodi attivi [per fare qualche piccolo test con cassandra spenta]
		//this.current_node_number = 3;
		check_min_node_number_for_the_experiment(this.current_node_number);
		System.out.println(" - [AutoScaler] there are "+this.current_node_number+" nodes in the cluster");
		
		System.out.println(" - [AutoScaler] 1 minute of real time corresponds to "+(60/this.single_duration_sec)+" minutes of workload time");
        System.out.println(" - [AutoScaler] avg adding time (wl time) : "+String.format("%.3f", this.avg_adding_time_sec_wl_time/60.0).replace(",", ".")+" min");
        for(int i = this.max_num_nodes; i>this.min_num_nodes; i--){
        	System.out.println(" - [AutoScaler] avg remove time when there are "+i+" nodes (wl time) : "+String.format("%.3f", this.avg_remove_times_sec_wl_time_map.get(i) /60.0).replace(",", ".")+" min");
        }
        System.out.println(" - [AutoScaler] avg cleanup time (wl time) : "+String.format("%.3f", this.avg_cleanup_time_sec_wl_time/60.0).replace(",", ".")+" min");
	}

	public void start(){
	
		String start_date_time = this.convert_ts_to_string_date(this.initial_wl_tstamp + (this.number_hours_initial_shift*60*60*1000));
		System.out.print(" - [AutoScaler] simulation time starts @ "+start_date_time);
		if(this.number_hours_initial_shift==0){System.out.println();}
		else{System.out.println(" (shifted by "+this.number_hours_initial_shift+" hours wrt the real workload starting time)");}
		System.out.println(" - [AutoScaler] autoscaler started");
		this.time_tracker.start(); 
		System.out.println(" - [AutoScaler] time_tracker started [ using single_duration_sec:"+this.single_duration_sec+" ]");
		
		// secondi di real time --> che corrispondono a (60/single_duration_sec)/2=2.5 min di WL time
		int n_sec_between_sampling = 30; 
										 
		long start_ts = System.currentTimeMillis();
		
		execute = true;
		while( execute ){	
			long elapsed_sec = (long) time_tracker.getElapsed();
			double elapsed_min = Double.parseDouble(String.format("%.3f", elapsed_sec/60.0).replace(",", "."));
			int minutes = (int) ((((System.currentTimeMillis() - start_ts)/1000.0)) / 60); // convert seconds (saved in "time") to minutes
			int seconds = (int) (((System.currentTimeMillis() - start_ts)/1000.0)) - minutes*60; // get the rest
			String disMinu = (minutes < 10 ? "0" : "") + minutes; // get minutes and add "0" before if lower than 10
			String disSec = (seconds < 10 ? "0" : "") + seconds; // get seconds and add "0" before if lower than 10
			String formattedTime = disMinu + ":" + disSec; //get the whole time
			double real_elapsed_min ;
			System.out.println("\n - [AutoScaler] real time elapsed: "+formattedTime +" min --> WL time elapsed: "+elapsed_min+" min");
			
			long wl_now_ts = this.initial_wl_tstamp + (this.number_hours_initial_shift*60*60*1000) + (1000*elapsed_sec);
			
			// DECIDE the most adequate scaling action to perform
			ScalingAction action = decide_scaling_action(wl_now_ts);
			
			// EXECUTE the decided scaling action
			double start_exec_time_sec = this.time_tracker.getElapsed();
			execute_scaling_action(action, wl_now_ts);
			double exec_duration_time_sec = ( this.time_tracker.getElapsed() - start_exec_time_sec );
				
			// if the execution of the scaling operation finished before the time that was assessed,
			// then we should wait the remaining time
			long minimum_admitted_duration_sec = compute_minimum_admitted_duration_sec_in_wl_time(action);
			if(exec_duration_time_sec < minimum_admitted_duration_sec){		
				System.out.println(" - [AutoScaler] wait some seconds since the scaling execution finished before what was expected");
				long n_sec_to_wait_in_wl_time = minimum_admitted_duration_sec - (long)exec_duration_time_sec ;
				long n_sec_to_wait_in_real_time = n_sec_to_wait_in_wl_time * ((long)(this.single_duration_sec/60.0));
				try { Thread.sleep(n_sec_to_wait_in_real_time * 1000); } 
				catch (InterruptedException e) {}
			}
			try{ Thread.sleep(1000*n_sec_between_sampling); }
		    catch(Exception e){}
		}
	}
	
	public void stop(){
		this.execute=false;
		System.out.println(" - [AutoScaler] stopping the execution as requested");
	}
	
	private long compute_minimum_admitted_duration_sec_in_wl_time(ScalingAction action){
		long result = 0;
		if(action.getAction().equals(AutoScaleConstants.SCALE_OUT)){
			result = action.getNumber()*this.avg_adding_time_sec_wl_time;
		}
		else if (action.getAction().equals(AutoScaleConstants.SCALE_IN)){
			long t_rem_tot = 0;
			for ( int j=(this.current_node_number+action.getNumber()); j>(this.current_node_number); j--){
				t_rem_tot  = t_rem_tot + this.avg_remove_times_sec_wl_time_map.get(j);
			}
			result = t_rem_tot;
		}
		return result;
	}
	
	private void execute_scaling_action(ScalingAction action, long now_ts_wl_time){
		System.out.println(" - [AutoScaler] decided scaling decision : "+action.format_as_printable_message(this.current_node_number));
		
		if(!action.getAction().equals(AutoScaleConstants.KEEP_CURRENT)){
			String s="";
			String res;
			if(action.getNumber()>1){ s = s+"s"; }
			if( action.getAction().equals(AutoScaleConstants.SCALE_OUT) ){ // WE HAVE TO SCALE OUT
				System.out.println("      [AutoScaler] calling the Configuration Manager to ADD "+action.getNumber()+" node"+s+" from the cluster");
				this.someone_is_joining=true;
				for(int i=0; i<action.getNumber(); i++){
					int index_node_to_add = this.current_node_number + i;
					String address_node_to_add = this.node_addresses_map.get(index_node_to_add);
					boolean add_result = this.config_manager.addNode(address_node_to_add,this.jmx_port_number);
					if(!add_result){ System.err.println(" - [AutoScaler] ATTENTION! the execution of the addNode("+address_node_to_add+") has FAILED !"); }
				}
				this.someone_is_joining=false;
				long wl_now_ts = (long) (this.initial_wl_tstamp + (this.number_hours_initial_shift*60*60*1000) + (1000*this.time_tracker.getElapsed()));
				// update the current number of nodes in the cluster
				this.current_node_number = this.current_node_number + action.getNumber() ;
				System.out.println("      [AutoScaler] ["+convert_ts_to_string_date(wl_now_ts)+"] scale out completed - now there are "+this.current_node_number+" nodes in the cluster");
				
				// dopo aver completato lo scale out aggiungendo tutti i nodi che dovevamo
				// eseguiamo la cleanup su tutti i nodi del cluster
				System.out.println("      [AutoScaler] calling the Configuration Manager to execute the cleanup on all nodes of the cluster");
				cleanups_are_running=true;
				boolean result_cleanup = this.config_manager.cleanup_all_nodes_parallel();
				cleanups_are_running=false;
				
				if(result_cleanup){ res="SUCCESS"; }
				else{res="FAILED";}
				System.out.println("      [AutoScaler] execution of the cleanup completed ("+res+")");
			}
			else{ // WE HAVE TO SCALE IN
				System.out.println("      [AutoScaler] calling the Configuration Manager to REMOVE "+action.getNumber()+" node"+s+" from the cluster");
				this.someone_is_leaving=true;
				for(int i=0; i<action.getNumber(); i++){
					int index_node_to_remove = this.current_node_number - i -1;
					String address_node_to_remove = this.node_addresses_map.get(index_node_to_remove);
					boolean remove_result = this.config_manager.removeNode(address_node_to_remove, this.jmx_port_number);
					if(!remove_result){ System.err.println(" - [AutoScaler] ATTENTION! the execution of the addNode("+address_node_to_remove+") has FAILED !"); }
				}
				this.someone_is_leaving=false;
				 
				// update the current number of nodes in the cluster
				this.current_node_number = this.current_node_number - action.getNumber();
				 
				long wl_now_ts = (long) (this.initial_wl_tstamp + (this.number_hours_initial_shift*60*60*1000) + (1000*this.time_tracker.getElapsed()));
				System.out.println("      [AutoScaler] ["+convert_ts_to_string_date(wl_now_ts)+"] scale in completed - now there are "+this.current_node_number+" nodes in the cluster");
			}
		}
	}
	
	private ScalingAction compute_scaling_action(int required_nodes){
		ScalingAction computed_sa;
		if(this.current_node_number > required_nodes){ // SCALE IN
			int how_many_to_remove = this.current_node_number - required_nodes;
			computed_sa = new ScalingAction(AutoScaleConstants.SCALE_IN, how_many_to_remove);
		}
		else if(this.current_node_number < required_nodes){  // SCALE OUT
			int how_many_to_add = required_nodes - this.current_node_number;
			computed_sa = new ScalingAction(AutoScaleConstants.SCALE_OUT, how_many_to_add);
		}
		else{ // KEEP CURRENT
			computed_sa = new ScalingAction(AutoScaleConstants.KEEP_CURRENT, 0);
		}
		return computed_sa;
	}
	
	private ScalingAction decide_scaling_action(long now_ts_wl_time){
		
		int current = this.current_node_number;
		int max = this.max_num_nodes;
		int min = this.min_num_nodes;
		int t_add = this.avg_adding_time_sec_wl_time*1000;
		Map<Integer,Integer> t_remove = new HashMap<Integer,Integer>();
		for(int i=this.max_num_nodes; i>this.min_num_nodes; i--){
			t_remove.put(i, this.avg_remove_times_sec_wl_time_map.get(i)*1000);
		}
		
		ScalingAction decided_scaling_action = null;
		
		// prediction carico corrente
		String wl_now_date = convert_ts_to_string_date(now_ts_wl_time);
		System.out.println(" - [AutoScaler] workload time : " + wl_now_date);
		double predicted_load = this.predictor.predict_load_at_time(now_ts_wl_time) * (this.scaling_factor);
		String formatted_load = String.format("%.3f", predicted_load).replace(",", ".");
		int required_nodes = this.compute_required_node_number(predicted_load);
		System.out.println(" - [AutoScaler] predicted current load    : "+formatted_load+" req/sec | num nodes required : "+required_nodes+" (current: "+this.current_node_number+")");
		
		// mi è permesso fare un eventuale scale out?
		if( current < max ){ // se si
			System.out.println(" - [AutoScaler] compute future load predictions in order to decide whether to scale OUT or not");
			// calcoliamo il carico al tempo necessario per aggiungere (x) , (x-1) , (x-2) ... nodi,
			// calcoliamo il numero di nodi necessari a reggere tale carico e se sono superiori dei nodi 
			// attualmente presenti, ne aggiungo altri
			for( int i = 0; i <(max-current); i++){

				// predizione del carico e del numero di nodi necessari a servire tale carico
				long now_plus_time_to_add_x_nodes = now_ts_wl_time + t_add*(max-current-i);
				predicted_load = this.predictor.predict_load_at_time(now_plus_time_to_add_x_nodes) * (this.scaling_factor);
				formatted_load = String.format("%.3f", predicted_load).replace(",", ".");
				required_nodes = this.compute_required_node_number(predicted_load);
				System.out.println("    - should we add "+(max-current-i)+" nodes within time "+convert_ts_to_string_date(now_plus_time_to_add_x_nodes)+" (time to add "+((max-current-i))+" nodes) ?");
				System.out.println("    - predicted load @ t = "+convert_ts_to_string_date(now_plus_time_to_add_x_nodes)+" : "+formatted_load+" | required nodes: "+required_nodes);
				ScalingAction computed_sa = compute_scaling_action(required_nodes);
				
				if(computed_sa.getAction().equals(AutoScaleConstants.SCALE_OUT) && computed_sa.getNumber()>=(max-current-i)){
					System.out.println("    - required scaling action to execute is : ("+computed_sa.getAction()+";"+computed_sa.getNumber()+")");
					decided_scaling_action = computed_sa;
					if(current + computed_sa.getNumber() > max){
						System.out.println("    - [ only "+(max-current)+" out of "+computed_sa.getNumber()+" nodes can be added since "+max+" is the maximum number of nodes ]");
						decided_scaling_action = new ScalingAction(computed_sa.getAction(), (max-current));
					}
					break;
				}
				else{ System.out.println("    - adding "+(max-current-i)+" nodes is NOT required\n"); }
			}
			
		}
		
		// mi è permesso fare un eventuale scale IN ?
		if( (decided_scaling_action==null) && (current > min) ){ // se si
			System.out.println(" - [AutoScaler] compute future load predictions in order to decide whether to scale IN or not");
			// calcoliamo il carico al tempo necessario per rimuovere (x) , (x-1) , (x-2) ... nodi,		
			// calcoliamo il numero di nodi necessari a reggere tale carico e se sono inferiori dei nodi 
			// attualmente presenti, ne rimuovo qualcuno
			for( int i = 0; i<(current-min); i++){
				
				// predizione del carico e del numero di nodi necessari a servire tale carico
				long t_rem_tot = 0;
				for ( int j=this.current_node_number; j>(this.min_num_nodes+i); j--){
					t_rem_tot  = t_rem_tot + t_remove.get(j);
				}
				long now_plus_time_to_remove_x_nodes = now_ts_wl_time + t_rem_tot;
				
				predicted_load = this.predictor.predict_load_at_time(now_plus_time_to_remove_x_nodes) * (this.scaling_factor);
				formatted_load = String.format("%.3f", predicted_load).replace(",", ".");
				required_nodes = this.compute_required_node_number(predicted_load);
				
				System.out.println("    - should we remove "+(current-min-i)+" nodes within time "+convert_ts_to_string_date(now_plus_time_to_remove_x_nodes)+" (time to remove "+((current-min-i))+" nodes) ?");
				System.out.println("    - predicted load @ t = "+convert_ts_to_string_date(now_plus_time_to_remove_x_nodes)+" : "+formatted_load+" | required nodes: "+required_nodes);

				ScalingAction computed_sa = compute_scaling_action(required_nodes);
				
				if( computed_sa.getAction().equals(AutoScaleConstants.SCALE_IN) && computed_sa.getNumber()>=(current-min-i) ){
					System.out.println("    - required scaling action to execute is : ("+computed_sa.getAction()+";"+computed_sa.getNumber()+")");
					decided_scaling_action = computed_sa;
					if( current - computed_sa.getNumber() < min ){
						System.out.println("    - [ only "+(current-min)+" out of "+computed_sa.getNumber()+" nodes can be removed since "+min+" is the minimun number of nodes ]");
						decided_scaling_action = new ScalingAction(computed_sa.getAction(), (current-min));
					}
					break;
				}
				else{ System.out.println("    - removing "+(current-min-i)+" nodes is NOT required\n"); }
			}	
		}
		
		if(decided_scaling_action==null){
			decided_scaling_action=new ScalingAction(AutoScaleConstants.KEEP_CURRENT, 0);
		}
		
		return decided_scaling_action;
		
	}
	
	/*
	private ScalingAction decide_scaling_action_old(long wl_now_ts){
		
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
		
		int required_now = this.compute_required_node_number(current_predicted_load);
		int required_15min = this.compute_required_node_number(next_15min_predicted_load);
		int required_half_hour = this.compute_required_node_number(next_half_hour_predicted_load);
		int required_one_hour = this.compute_required_node_number(next_hour_predicted_load);
		
		String wl_now_date = convert_ts_to_string_date(wl_now_ts);
		System.out.println("\n - [AutoScaler] workload time : " + wl_now_date);
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
	*/
	
	private int compute_required_node_number(double load){
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
        int single_duration_sec = 12; // 1 minuto vero = 6 minuti simulati
        int scaling_factor = 810; 
        int initial_shift_num_hours =6;
        AutoScaler scaler = new AutoScaler( autoscaler_properties_path, predictor_properties_path, 
        		conf_man_prop_path, single_duration_sec, scaling_factor, initial_shift_num_hours );
        scaler.start();
    }

    
}