package jmeter_plan_creation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/** JMeterPlanCreator
 *  
 *  a partire dal file col twitter dataset . le cui righe sono nel formato : 
 *  	< tstamp  	  tweet_per_min >
 *  	< tstamp+1min tweet_per_min >
 *  
 *  genera il file file jmeter test_plan nella cui porzione relativa al ThroughputShapingTimer
 *  saranno inserite delle entries tali che la curva generata sia quella indicata dal dataset
 * 
 * @author andrea-muti
 *
 */

public class JMeterPlanCreator {
	
	// instance variables
	private String separator ;
	private boolean has_headers ;
	private String filepath ;
	private int single_duration_sec;
	private int number_of_jmeter_slaves;
	private int scaling_factor;
	private int n_hours_initial_shift;
	
	/** public constructor
	 * 		- initial shift = 0 by default, therefore the generated plan corresponds to the WHOLE workload
	**/
	public JMeterPlanCreator( String workload_csv_file_path, String separator, boolean has_headers, 
							  int single_duration_sec, int num_jmeter_slaves, int scaling_factor ){
		this(workload_csv_file_path, separator, has_headers, single_duration_sec, num_jmeter_slaves, scaling_factor, 0);
	}
	
	/** public constructor
	 * 
	 * @param workload_csv_file_path : workload file path
	 * @param separator	: separators for the fields in the workload file path
	 * @param has_headers : whether or not the csv workload file has headers on the first line
	 * @param single_duration_sec : real time duration in secs of each workload line (that corresponds to 1 minute)
	 * @param num_jmeter_slaves : number of jmeter slaves that will execute the workload
	 * @param scaling_factor : scaling factor applied to the workload intensity
	 * @param n_hours_initial_shift : number of hours the generated plan will be shifted forward wrt the workload file
	 */
	public JMeterPlanCreator( String workload_csv_file_path, String separator, boolean has_headers, 
								  int single_duration_sec, int num_jmeter_slaves, int scaling_factor, int n_hours_initial_shift ){
			
		this.filepath 	   = workload_csv_file_path; 
		this.separator	   = separator;
		this.has_headers   = has_headers;

		this.single_duration_sec = single_duration_sec;
		this.number_of_jmeter_slaves = num_jmeter_slaves;
		this.scaling_factor = scaling_factor;
		this.n_hours_initial_shift=n_hours_initial_shift;
			
		}
	
	
	
	public String create_initial_section(){
		String initial_section = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n"
			+ "<jmeterTestPlan version=\"1.2\" properties=\"2.8\" jmeter=\"2.13.20160210\">" + "\n"
            + "<hashTree>" + "\n"
            + "<TestPlan guiclass=\"TestPlanGui\" testclass=\"TestPlan\" testname=\"Workload Plan ( All-Reads ) \" enabled=\"true\">" + "\n"
            + "<stringProp name=\"TestPlan.comments\"></stringProp>" + "\n"
            + "<boolProp name=\"TestPlan.functional_mode\">false</boolProp>" + "\n"
            + "<boolProp name=\"TestPlan.serialize_threadgroups\">false</boolProp>" + "\n"
            + "<elementProp name=\"TestPlan.user_defined_variables\" elementType=\"Arguments\" guiclass=\"ArgumentsPanel\" testclass=\"Arguments\" testname=\"User Defined Variables\" enabled=\"true\">" + "\n"
            + "<collectionProp name=\"Arguments.arguments\"/>" + "\n"
            + "</elementProp>" + "\n"
            + "<stringProp name=\"Tes tPlan.user_define_classpath\"></stringProp>" + "\n"
            + "</TestPlan>" + "\n"
            + "<hashTree>" + "\n"
            + "<com.github.cqljmeter.config.CassandraClusterConfig guiclass=\"TestBeanGUI\" testclass=\"com.github.cqljmeter.config.CassandraClusterConfig\" testname=\"C* Cluster Settings\" enabled=\"true\">" + "\n"
            + "<stringProp name=\"clusterId\">Test Cluster</stringProp>" + "\n"
            + "<stringProp name=\"contactPoint\">192.168.0.169</stringProp>" + "\n"
            + "<stringProp name=\"user\"></stringProp>" + "\n" 
            + "<stringProp name=\"password\"></stringProp>" + "\n" 
            + "<stringProp name=\"TestPlan.comments\"></stringProp>" + "\n"
            + "<stringProp name=\"consistency\">ONE</stringProp>" + "\n"
            + "</com.github.cqljmeter.config.CassandraClusterConfig>" + "\n"
            + "<hashTree/>" + "\n"
            + "<kg.apc.jmeter.timers.VariableThroughputTimer guiclass=\"kg.apc.jmeter.timers.VariableThroughputTimerGui\" testclass=\"kg.apc.jmeter.timers.VariableThroughputTimer\" testname=\"jp@gc - Throughput Shaping Timer\" enabled=\"true\">" + "\n"
            + "<collectionProp name=\"load_profile\">";
		
		return initial_section;
	}
	
	public String create_ending_section(){
		String ending_section = "</collectionProp>" + "\n"
	      + "</kg.apc.jmeter.timers.VariableThroughputTimer>" + "\n"
	      + "<hashTree/>" + "\n"
	      + "<ThreadGroup guiclass=\"ThreadGroupGui\" testclass=\"ThreadGroup\" testname=\"Reader Thread Group\" enabled=\"true\">" + "\n"
	      + "<stringProp name=\"ThreadGroup.on_sample_error\">continue</stringProp>" + "\n"
	      + "<elementProp name=\"ThreadGroup.main_controller\" elementType=\"LoopController\" guiclass=\"LoopControlPanel\" testclass=\"LoopController\" testname=\"Loop Controller\" enabled=\"true\">" + "\n"
	      + "<boolProp name=\"LoopController.continue_forever\">false</boolProp>" + "\n"
	      + "<intProp name=\"LoopController.loops\">-1</intProp>" + "\n"
	      + "</elementProp>" + "\n"
	      + "<stringProp name=\"ThreadGroup.num_threads\">100</stringProp>" + "\n"
	      + "<stringProp name=\"ThreadGroup.ramp_time\">4</stringProp>" + "\n"
	      + "<longProp name=\"ThreadGroup.start_time\">1453471397000</longProp>" + "\n"
	      + "<longProp name=\"ThreadGroup.end_time\">1453471397000</longProp>" + "\n"
	      + "<boolProp name=\"ThreadGroup.scheduler\">false</boolProp>" + "\n"
	      + "<stringProp name=\"ThreadGroup.duration\"></stringProp>" + "\n"
	      + "<stringProp name=\"ThreadGroup.delay\"></stringProp>" + "\n"
	      + "</ThreadGroup>" + "\n"
	      + "<hashTree>" + "\n"
	      + "<com.github.cqljmeter.sampler.CqlSampler guiclass=\"TestBeanGUI\" testclass=\"com.github.cqljmeter.sampler.CqlSampler\" testname=\"Random Read [CL=ONE]\" enabled=\"true\">" + "\n"
	      + "<stringProp name=\"clusterId\">Test Cluster</stringProp>" + "\n"
	      + "<stringProp name=\"keySpace\">my_keyspace</stringProp>" + "\n"
	      + "<stringProp name=\"query\">select * from my_table where key = ${__UUID()}</stringProp>" + "\n"
	      + "<stringProp name=\"consistency\">ONE</stringProp>" + "\n"
	      + "</com.github.cqljmeter.sampler.CqlSampler>" + "\n"
	      + "<hashTree/>" + "\n"
	      + "</hashTree>" + "\n"
	      + "</hashTree>" + "\n"
	      + "</hashTree>" + "\n"
	      + "</jmeterTestPlan>" + "\n";
		
		return ending_section; 
	}
	
	public String create_workload_section(){
		String workload_section = "";
		
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(this.filepath));
			
			// se c'è l'header lo salto
			if( this.has_headers ){ reader.readLine(); }
			
			// devo saltare un numbero di righe pari al numbero di minuti corrispondenti al n_hours_initial_shift
			for(int i = 0; i<60*this.n_hours_initial_shift; i++){
				reader.readLine();
			}
			
			String line = reader.readLine();
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line,this.separator);
				st.nextToken(); 						// primo token = tstamp
				String stringtoken = st.nextToken(); 	// secondo token = load value
				int value = Integer.parseInt(stringtoken);
				
				value = (value * this.scaling_factor);
				value = (value / this.number_of_jmeter_slaves) ;
				
				// if value == 0 jmeter stops the execution of the test plan, therefore we force the value to be not zero
				if(value == 0){ value=1*this.scaling_factor/this.number_of_jmeter_slaves; }
				
				String entry =   "<collectionProp name=\"-139272128\">\n"
							   + "\t<stringProp name=\"1537214\">"+value+"</stringProp>\n"
							   + "\t<stringProp name=\"1537214\">"+value+"</stringProp>\n"
							   + "\t<stringProp name=\"1722\">"+single_duration_sec+"</stringProp>\n"
							   +"</collectionProp>";
				
				workload_section = workload_section+entry;
				line = reader.readLine();
			}
			
			reader.close();
		}catch( Exception e ) {}
		
		return workload_section;
	}
	
	public boolean create_plan_and_save_on_file(String output_file_path){
		boolean result = true;
		String initial_section = this.create_initial_section();
		String workload_section = this.create_workload_section();
		if(workload_section.equals("")){return false;}
		String ending_section = this.create_ending_section();
		
		String complete_plan = initial_section+"\n"+workload_section+"\n"+ending_section;
		try{
			PrintWriter writer = new PrintWriter(output_file_path, "UTF-8") ;	
			writer.write(complete_plan);
			writer.close();
		}
		catch(Exception e){ result = false; }
		return result;
	}
	
	public static void main(String[] args){
		
		System.out.println(" - JMeter Workload Plan Creator");
		
		String output_file_path = "files/jmeter_plans/workload_plan.jmx";
		
		String workload_csv_file_path = "";
		String separator = " ";
		boolean has_headers = false; 
		int single_duration_sec = 5;
		int num_jmeter_slaves = 5;
		int scaling_factor = 300;

		try{
			
			JMeterPlanCreator plan_creator = new JMeterPlanCreator(workload_csv_file_path, separator, 
					has_headers, single_duration_sec, num_jmeter_slaves, scaling_factor);
			
			if(plan_creator.create_plan_and_save_on_file(output_file_path)){
				System.out.println(" - jmeter workload plan created successfully");
			}
			else{ throw new Exception(); }
			
			
		}
		catch(Exception e){
			System.err.println(" - ERROR creating jmeter workload plan");
		}
		
	}
}
