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
	
	// public constructor
	public JMeterPlanCreator(){
		// TODO : far leggere tutto ciò da file properties
		this.separator	   = " ";
		this.has_headers   = false;
		this.filepath 	   = "files/datasets/workload_day_16.csv"; 
		this.single_duration_sec = 60;
		this.number_of_jmeter_slaves = 1;
		this.scaling_factor = 1;
		
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
			
			String line = reader.readLine();
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line,this.separator);
				st.nextToken(); 						// primo token = tstamp
				String stringtoken = st.nextToken(); 	// secondo token = load value
				int value = Integer.parseInt(stringtoken);
				
				value = (value * this.scaling_factor);
				value = (value / this.number_of_jmeter_slaves) ;
				
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
	
	public static void main(String[] args){
		
		System.out.println(" - JMeter Workload Plan Creator");
		
		String output_path = "files/jmeter_plans/workload_plan.jmx";

		try{
			
			JMeterPlanCreator plan_creator = new JMeterPlanCreator();
			String initial_section = plan_creator.create_initial_section();
			String workload_section = plan_creator.create_workload_section();
			String ending_section = plan_creator.create_ending_section();
			
			String complete_plan = initial_section+"\n"+workload_section+"\n"+ending_section;
			
			PrintWriter writer = new PrintWriter(output_path, "UTF-8") ;	
			writer.write(complete_plan);
			writer.close();
			
			System.out.println(" - jmeter workload plan created successfully");
		}
		catch(Exception e){
			System.out.println(" - ERROR creating jmeter workload plan");
		}
		
	}
}
