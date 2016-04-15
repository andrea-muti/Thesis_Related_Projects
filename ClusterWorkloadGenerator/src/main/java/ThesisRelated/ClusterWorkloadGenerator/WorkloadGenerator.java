package ThesisRelated.ClusterWorkloadGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.StringTokenizer;

import jmeter_automation.FileUtils;
import jmeter_automation.JMRunner;
import jmeter_plan_creation.JMeterPlanCreator;

/**
 * ClusterWorkloadGenerator
 * @author andrea-muti
 * @since 30/03/2016
 */
@SuppressWarnings("unused")
public class WorkloadGenerator {
	
	static final int ERROR_STATUS = 0;
	static final int SUCCESS_STATUS = 1;
	
	// instance variables
	
	private String generator_props_file;
	private String jmeter_props_file;
	private  String jmeter_plan_path;
	private String workload_file_path;
	private  String csv_separator;
	private boolean has_headers;
	private int single_duration_sec ;
	private int num_jmeter_slaves ;
	private  int workload_scaling_factor ;
	private boolean is_plan_generated;
	private long avg_workload_duration_sec;
	private JMRunner jrunner;
	private boolean execute=false;
	
	/** WorkloadGenerator
	 * 		public constructor
	 * @param generator_properties_path
	 * @param jmeter_properties_path
	 */
	public WorkloadGenerator(String generator_properties_path, String jmeter_properties_path){
		
		this.generator_props_file=generator_properties_path;
		this.jmeter_props_file=jmeter_properties_path;
		
		Properties props_generator = FileUtils.loadProperties(new File(generator_properties_path));
		Properties props_jmeter = FileUtils.loadProperties(new File(jmeter_properties_path));
		
        this.jmeter_plan_path = props_jmeter.getProperty("jmxFile");
        this.workload_file_path = props_generator.getProperty("workload_filepath");
        this.csv_separator = props_generator.getProperty("csv_separator");
        this.has_headers = Boolean.valueOf(props_generator.getProperty("has_headers").trim());
        this.single_duration_sec = Integer.parseInt(props_generator.getProperty("single_duration_sec").trim());
        this.num_jmeter_slaves = count_slaves(props_jmeter.getProperty("jmeter_slaves_IPs").trim());
        this.workload_scaling_factor = Integer.parseInt(props_generator.getProperty("scaling_factor").trim());
        this.is_plan_generated=false;
        System.out.println(" - [WorkloadGenerator] selected workload file: "+workload_file_path);
        this.avg_workload_duration_sec = this.single_duration_sec * count_lines();

		// creazione del JMRunner
        // spostato nel costruttore del workload generator perche il costruttore dei JMRunner
        // invoca a sua volta il costruttore del JMeterController che invoca la
        // kill_and_restart_jmeter_slaves() che richiede del tempo per essere eseguita
        // siccome nel coordinator ho il latch tra il costruttore del generator e la runWorkload()
        // sono sicuro che quando invoco la runWorkload() i jmeter-slaves sono stati restarted
        this.jrunner = new JMRunner(this.jmeter_props_file);
	}
	

	public long get_avg_workload_duration_sec(){
		return this.avg_workload_duration_sec;
	}
	
	private long count_lines(){
		long len = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(this.workload_file_path));	
			while (( br.readLine()) != null) {	len++; }
			br.close();
		} catch (Exception e) {
			System.err.println(" - [WorkloadGenerator] Error reading workload file: " + this.workload_file_path);
		}
		return len;
	}
	
	public boolean generate_jmeter_plan(){
		 JMeterPlanCreator plan_creator = new JMeterPlanCreator(workload_file_path, csv_separator, 
					has_headers, single_duration_sec, num_jmeter_slaves, workload_scaling_factor);
		 boolean result = plan_creator.create_plan_and_save_on_file(jmeter_plan_path);
		 this.is_plan_generated=result;
		 return result;
	}
	
	public boolean generate_jmeter_plan_with_initial_shift(int n_hours){
		 JMeterPlanCreator plan_creator = new JMeterPlanCreator(workload_file_path, csv_separator, 
					has_headers, single_duration_sec, num_jmeter_slaves, workload_scaling_factor, n_hours);
		 boolean result = plan_creator.create_plan_and_save_on_file(jmeter_plan_path);
		 this.is_plan_generated=result;
		 return result;
	}
	
	public void generateWorkload(){
		if(!this.is_plan_generated){
			System.err.println(" - [WorkloadGenerator] ERROR : workload cannot be executed since the jmeter plan has not been generated successfully");
			return ;
		}
		
		
        this.execute=true;
        for(int i=0; i<30000; i++){
			System.out.println("\n     #@#@#@# [print di test] WORKLOAD GENERATOR STA GENERANDO CARICO #@#@#@#@# \n");
			try{ Thread.sleep(30*1000);}
			catch(Exception e){}
		}
        
        
        //this.jrunner.runWorkload();
        
        this.execute=false;
        
        /*
        // wait until termination of workload execution
        // non dovrei mai entrare in questo while, ma ce lo lasciamo per sicurezza
        while(jrunner.isRunning()){
        	try { Thread.sleep(1000); } 
        	catch (InterruptedException e) {}
        }
       */
	}
	
	public int get_single_duration_sec(){
		return this.single_duration_sec;
	}
	
	public long getInitialWorkloadTimestamp(){
		long result = 0;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(this.workload_file_path));
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
	
	
	private int count_slaves(String jmeter_slaves_IPs){
		StringTokenizer strtok = new StringTokenizer(jmeter_slaves_IPs, ",");
		int count = strtok.countTokens();
		return count;
	}
	
	
    public static void main( String[] args ){
    	
        System.out.println("\n ****** WORKLOAD GENERATOR *******\n");
        
        // reading various required properties
        String generator_properties_path = "files/PropertyFiles/generator.props";   
        String jmeter_properties_path = "files/PropertyFiles/jmeter.props";
        
        WorkloadGenerator generator = new WorkloadGenerator(generator_properties_path, jmeter_properties_path);
        
        // creazione del JMeter execution plan che verrà usato dal JMRunner (JMeterController)
        System.out.print(" - generation JMeter execution plan : ");
		boolean plan_creation = generator.generate_jmeter_plan();
        if(!plan_creation){
        	System.err.println(" FAILED");
        	System.exit(ERROR_STATUS);
        }
        else{System.out.println(" DONE"); }
       
        
        int real_seconds = generator.get_single_duration_sec();
        System.out.println(" - real seconds corresponding to 1 min of workload : "+real_seconds);
        
        long initial_wl_ts = generator.getInitialWorkloadTimestamp();
        
        
        CheckTimeThread timer = new CheckTimeThread(real_seconds, initial_wl_ts);
        System.out.println(" - started time checker");
        timer.start();
        
        System.out.println("\n - start running workload generation");
        generator.generateWorkload();
       
        System.out.println(" - workload generation completed");
        
        timer.stop();
        System.out.println(" - stopped time checker");
        
        System.exit(SUCCESS_STATUS);
    }

}



class CheckTimeThread implements Runnable{
	Thread mythread ;
	WorkloadTimeTracker tracker;
	long initial_wl_ts;
	boolean isRunning;
	int counter_iterations;
	long start_ts;
	   
	CheckTimeThread(int real_seconds, long init_wl_ts){ 
		this.mythread = new Thread(this, "CheckTimeThread");
		this.tracker = new WorkloadTimeTracker(real_seconds);
		this.initial_wl_ts=init_wl_ts;
		this.isRunning = false;
		this.counter_iterations=0;
	}
	
	private String convert_ts_to_string(long tstamp_msec){
		String result = "";
		Calendar c = new GregorianCalendar();	
		c.setTimeInMillis(tstamp_msec);
		int day = c.get(Calendar.DAY_OF_MONTH);
		int month = c.get(Calendar.MONTH)+1; // i mesi partono da 0
		int year = c.get(Calendar.YEAR);
		int hour = c.get(Calendar.HOUR);
		int min = c.get(Calendar.MINUTE);
		int sec = c.get(Calendar.SECOND);
		result = result+day+"-"+month+"-"+year+" "+hour+":"+min+":"+sec;
		return result;
	}
	
	public boolean isRunning(){
		return this.isRunning;
	}
	
	public void start(){
		System.out.println(" - Workload trace starts at "+this.convert_ts_to_string(this.initial_wl_ts));
		this.start_ts = System.currentTimeMillis();
		boolean result_tt = tracker.start();
        if(!result_tt){
        	System.err.println(" - ERROR : impossible to start the time tracker");
        	System.exit(0);
        }
		this.isRunning = true;
		this.mythread.start();
		
	}
	   
	public void run(){
		while(isRunning){
			double sim_sec_elapsed = tracker.getElapsed();
			String now_sim = convert_ts_to_string(this.initial_wl_ts+(long)sim_sec_elapsed*1000);
			System.out.println("     - [TimeChecker] real min elapsed: "+(this.counter_iterations)+
					" --> simulation min elapsed: "+ (sim_sec_elapsed/60.0) +
					" ( "+ sim_sec_elapsed+" sec ) simulation date: "+now_sim);
		
		
			
			try{ Thread.sleep(60*1000); }
    		catch(Exception e){}
			this.counter_iterations++;
		}
   }
	
	public void stop(){
		this.tracker.stop();
		double sim_sec_elapsed = tracker.getElapsed();
		double real_elapsed_min = (System.currentTimeMillis() - start_ts)/(1000.0*60);
		String real_elapsed_min_str = String.format("%.2f", real_elapsed_min).replace(",", ".");
		String now_sim = convert_ts_to_string(this.initial_wl_ts+(long)sim_sec_elapsed*1000);
		System.out.println("     - [TimeChecker] real min elapsed: "+(real_elapsed_min_str)+
				" --> simulation min elapsed: "+ (sim_sec_elapsed/60.0) +
				" ( "+ sim_sec_elapsed+" sec ) simulation date: "+now_sim);
		this.isRunning=false;
	}
}
