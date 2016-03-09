package ThesisRelated.MetricsCollector;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;

import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.utils.UUIDs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import jmeter_automation.FileUtils;

/**
 * MetricsCollector
 * 		goal : collect data to be used in the training phase of the ANN
 *
 * @author andrea-muti
 */

public class MetricsCollector {

	private Properties properties;
	private String contact_point_address;
	private String jmx_port;
	private int cpu_sampling_interval_msec;
	private int cpu_num_samples;
	private int throughput_num_samples;
	private int throughput_sampling_interval_msec;
	private int samples_count_RT;
	private String consistency_level;
	private ConsistencyLevel cl;
	private String operation_RT;
	@SuppressWarnings("unused")
	private int rt_op_interval_msec;
	
	
	public MetricsCollector(String properties_file){
		
		File prop_file = new File(properties_file);
		this.properties = FileUtils.loadProperties(prop_file);
		
		// Settaggio del loggin level a ERROR
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.ERROR);
				        
        this.contact_point_address = this.properties.getProperty("contact_point_address");
        check_contact_point_address(contact_point_address);
        
        // TO DO : check isValidPortNumber
        this.jmx_port = this.properties.getProperty("jmx_port");
        
        // TO DO: check is number
        this.cpu_num_samples = Integer.parseInt(this.properties.getProperty("cpu_num_samples"));
        
        // TO DO : check is number
        this.cpu_sampling_interval_msec = Integer.parseInt(this.properties.getProperty("cpu_sampling_interval_msec"));
        
        // TO DO: check is number
        this.throughput_num_samples = Integer.parseInt(this.properties.getProperty("throughput_num_samples"));
        
        // TO DO : check is number
        this.throughput_sampling_interval_msec = Integer.parseInt(this.properties.getProperty("throughput_sampling_interval_msec"));
          
        // TO DO : check is number
        this.samples_count_RT = Integer.parseInt(this.properties.getProperty("samples_count_RT"));
        
        // TO DO : check is number
        this.rt_op_interval_msec = Integer.parseInt(this.properties.getProperty("rt_op_interval_msec"));
        
        this.consistency_level = this.properties.getProperty("consistency_level");
        this.cl = consistency_parser(consistency_level);
        
        // TO DO : check is number
    	this.operation_RT = this.properties.getProperty("operation_RT");
		
		
	}
	
	public void runCollector(int in_rate){
		
		int input_rate = in_rate;	
		int num_nodes;				// leggo dal java_driver
		double cpu_level; 			// leggo da cassandra jmx
		double throughput_total;	// calcolo leggendo da cassandra jmx
		double rt_mean;				// leggo da java_driver
		double rt_95p;				// leggo da java_driver
		 
		List<String> addresses;

		//------------------------------------------------------------------------------
        
        /**   LETTURA ADDRESSES and NUMBER OF NODES IN THE CLUSTER   **/
        
        addresses = getNodesAddresses(contact_point_address, jmx_port);
        
        num_nodes = addresses.size();
       
        //------------------------------------------------------------------------------
        
        /**    LETTURA AVERAGE CPU LEVEL OF NODES   **/
        cpu_level = getAverageCpuLevel(jmx_port, addresses, cpu_num_samples, cpu_sampling_interval_msec);
        
        //------------------------------------------------------------------------------
        
        /**    LETTURA AVERAGE TOTAL THROUGHPUT OF NODES   **/
        throughput_total = getAverageTotalThroughput(jmx_port, addresses, throughput_num_samples, throughput_sampling_interval_msec);
        
        //------------------------------------------------------------------------------
        
        /**    LETTURA RESPONSE TIME    **/

        double[] response_times = getResponseTimes(jmx_port, contact_point_address, samples_count_RT, 
        		operation_RT, cl);
        
        rt_mean = response_times[0];
        rt_95p = response_times[1];
        
        
        //------------------------------------------------------------------------------
        
        /**    STAMPA DELLE METRICS COLLEZIONATE  **/
        // invece di stamparle su stdout dovrei stamparle su file
        
        System.out.println(" \t\t - Input Rate : "+input_rate +" req/sec");
        System.out.println(" \t\t - There are "+num_nodes+" nodes in the cluster");       
        System.out.println(" \t\t - Average CPU Level : "+cpu_level+" %"); 
        System.out.println(" \t\t - Average Total Throughput : "+throughput_total+" ops/sec");  
        System.out.println(" \t\t - Mean Response Time : "+rt_mean+" msec");
        System.out.println(" \t\t - 95percentile Response Time : "+rt_95p+" msec");
	}
	

	public static void main( String[] args ){
    	
		System.out.println("\n ************** Metrics Collector ***************");
		
		long start = System.currentTimeMillis();
		
		String properties_file = "files/PropertyFiles/collector.properties";
		MetricsCollector collector = new MetricsCollector(properties_file);
		
		collector.runCollector(1000);
    	
		System.out.println("\n - ELAPSED TIME : "+(System.currentTimeMillis()-start)/1000+" seconds");
		
		System.out.println("\n ************************************************");
		
		
        System.exit(0);
       
        
    } // END MAIN
    
    //---------------------------------------------------------------------------------------------
    
    private static List<String> getNodesAddresses(String contact_point_addr, String jmx_port){
    	List<String> addresses = null;
		JMXReader jmxreader = new JMXReader(contact_point_addr, jmx_port);
        MBeanServerConnection remote = null;
		try {
			remote = jmxreader.connect();
		} catch (IOException e) {
			System.err.println(" - ERROR : There are communication problems when establishing the connection with the Cluster \n"
   		           + "           [ "+e.getMessage()+" ]");
			System.exit(-1);
		}
		catch (SecurityException e) {			System.err.println(" - ERROR : There are security problems when establishing the connection with the Cluster \n"
	   		           + "           [ "+e.getMessage()+" ]");
			System.exit(-1);
		}
		catch (Exception e) {
			System.err.println(" - ERROR : There are unknown problems when establishing the connection with the Cluster \n"
	   		           + "           [ "+e.getMessage()+" ]");
			System.exit(-1);
		}

        addresses = jmxreader.getLiveNodes(remote);
        
        jmxreader.disconnect();
        
        return addresses;
	}
    
    //---------------------------------------------------------------------------------------------
    
    private static double getAverageCpuLevel( String jmx_port_number, List<String> addresses, 
    										  int num_samples, int sampling_interval){
    	
    	System.out.println(" \t - Computing Average CPU Level [collecting "+num_samples+" samples @ sampling interval of "+sampling_interval+" msec ]");
    	double cpu_level_to_return = 0;
    
    	int n_nodes = addresses.size();
    	
    	double[] cpu_levels_array = new double[n_nodes];
    	
    	final ExecutorService service;
        List<Future<Double>>  task_List = new ArrayList<Future<Double>>();
         
        service = Executors.newFixedThreadPool(n_nodes);     
       
     	// per ogni nodo nel cluster, colleziona le statistics
     	for( int i=0; i<n_nodes; i++ ){
     		
     		String IP_address = addresses.get(i);
     		
     		/* IN LOCALE
     		if(i==0){jmx_port_number="7201";}
     		else if(i==1){jmx_port_number="7202"; }
     		else if(i==2){jmx_port_number="7203";}
     		else{jmx_port_number="7199";}
     		IP_address="127.0.0.1";
			*/
  		
            task_List.add(i, service.submit(new CPUReader(IP_address,""+jmx_port_number, num_samples, sampling_interval)));
     		
     	
     	}// end of for loop
     	
         service.shutdownNow();
         
     	int i = 0;
     	// getting the results of the collector threads
     	for(Future<Double> f : task_List){
     		try {
     			Double returned_cpu_level = f.get();
     			
     			//System.out.println("     - cpu of node "+i+" : "+returned_cpu_level+" %");
     			cpu_levels_array[i] = returned_cpu_level;
     			i++;
     		}
     		catch(Exception e){ }
     	}
     	
     	cpu_level_to_return = compute_average_of_double_array(cpu_levels_array);
     	
     	String processCPUload_formatted = String.format( "%.3f", cpu_level_to_return ).replace(",", ".");
     	cpu_level_to_return =  Double.parseDouble(processCPUload_formatted) ;
     	
     	return cpu_level_to_return;
    	
    }	
    
    //---------------------------------------------------------------------------------------------
    
    private static double getAverageTotalThroughput( String jmx_port_number, List<String> addresses,
    												 int num_samples, int sampling_interval){
    
    	System.out.println(" \t - Computing Average Total Throughput [collecting "+num_samples+" samples @ sampling interval of "+sampling_interval+" msec ]");
    	double throughtput_to_return = 0;
    
    	int n_nodes = addresses.size();
    		
    	final ExecutorService service;
        List<Future<Double>>  task_List = new ArrayList<Future<Double>>();
        
        service = Executors.newFixedThreadPool(n_nodes);     
        
     	// per ogni nodo nel cluster, colleziona le statistics
     	for( int i=0; i<n_nodes; i++ ){
     		
     		String IP_address = addresses.get(i);
            task_List.add(i, service.submit(new ThroughputReader(IP_address,""+jmx_port_number, num_samples, sampling_interval)));	
     	
     	}// end of for loop
     	
         service.shutdownNow();
     
     	// getting the results of the collector threads

     	for(Future<Double> f : task_List){
     		try {
     			Double returned_throughput = f.get();   		
     			throughtput_to_return = throughtput_to_return + returned_throughput;
     		}
     		catch(Exception e){ }
     	}
     	
     	
     	String throughput_formatted = String.format( "%.3f", throughtput_to_return ).replace(",", ".");
     	throughtput_to_return =  Double.parseDouble(throughput_formatted) ;
     	
     	return throughtput_to_return;
    	
    }
    
    //---------------------------------------------------------------------------------------------
    
    private static double[] getResponseTimes(String jmx_port_number, String cp_address, int samples_count, String operation,ConsistencyLevel cl ){
    	
    	System.out.println(" \t - Computing Response Time [collecting "+samples_count+" samples  ]");
    	
    	double[] res_times = new double[2];
    	
    	int retry_interval_sec = 3;
    	int window_size = 50;
    	
    	int warmup_ops = 100;
    	
    	String keyspace = "my_keyspace";
    	String table = "my_table";
    	
    	Cluster cluster;
        Session session = null;
         	
     	cluster = Cluster.builder()
          		.addContactPoint(cp_address)
          		.withLoadBalancingPolicy(new RoundRobinPolicy()).build();
          		           	
        session = ClusterUtils.createSession(cluster, retry_interval_sec);
        
        // definition of the moving average window to track latencies
        MovingAverageWindow ma = new MovingAverageWindow(window_size);
        
        // -------- some warm-up operations ----- //
        // per far passare i primi secondi in cui la latenza diminuisce rapidamente
       
        for(int j = 0; j<warmup_ops; j++){
        	UUID random_key = UUIDs.random();
        	Statement operation_statement = create_statement_operation(operation,cl,keyspace, table, random_key);
        	try{ session.execute(operation_statement);  }  
        	catch(Exception e){}
        }
        
       // -------- START EXECUTION OF THE OPERATION ----- //
        
        long start = 0, duration;
        for(int i = 0; i<samples_count; i++){
        	
        	UUID random_key = UUIDs.random();
               	
        	Statement operation_statement = create_statement_operation(operation,cl,keyspace, table, random_key);
        	
        	try{ 
        		start = System.nanoTime();
        		session.execute(operation_statement);
        		//session.execute("select now() from system.local;");
        	}  
        	catch(Exception e){
        		System.err.println("error on key: "+random_key+" | "+e.getMessage());
        	}
        	duration = TimeUnit.MILLISECONDS.convert((System.nanoTime()-start), TimeUnit.NANOSECONDS);

        	ma.newNum(Double.parseDouble(""+duration));
        }
        
        // closing current session with the cluster in order to reset the metrics
  		try{
  			session.close();
  		}catch(Exception e){System.err.println("Error during closing the session");}
  		
  		cluster.close();
						
      	double mean_latency = ma.getAvg();
  	    double p95 = ma.get95percentile();
  	    			
  	    res_times[0] = mean_latency;
  	    res_times[1] = p95;
			
    	return res_times;
    }
    
    //---------------------------------------------------------------------------------------------
    
    private static double compute_average_of_double_array(double[] array) {
		double result = 0;
		for(int i = 0; i<array.length; i++){
			result = result + array[i];
		}
		result = result / array.length;
		return result;
	}
    
    //---------------------------------------------------------------------------------------------

	/** CHECK WHETER THE CONTACT POINT ADDRESS IS VALID
     *  @param contact_point_addr
     */
	private static void check_contact_point_address(String contact_point_addr) {
    	try{
    		InetAddress.getByName(contact_point_addr);
    	}
    	catch(UnknownHostException e){
    		System.err.println(" - ERROR : Malformed IP Address of Contact Point Node - Must be in the form x.x.x.x");
    		System.exit(-1);
    	}
	}
	
	//---------------------------------------------------------------------------------------------
	

    private static Statement create_statement_operation(String operation, ConsistencyLevel consistency_level, String keyspace, String table, UUID random_key) {
    	Statement op_statement = null;
    	
    	
    	if( operation.equalsIgnoreCase("READ") ){
    		op_statement = QueryBuilder.select("key","a","b","c","d","e","f","g","h","i","j")
        			.from(keyspace, table)
        			.where( QueryBuilder.eq("key", random_key) );
        	
    		op_statement.setConsistencyLevel(consistency_level);
    	}
    	else if( operation.equalsIgnoreCase("WRITE") ){
    		
        	String rand = org.apache.commons.lang.RandomStringUtils.random(19);
    		op_statement = QueryBuilder.insertInto(keyspace, table)
    		        .value("key", random_key)
    		        .value("a", rand+"a")
    		        .value("b", rand+"b")
    		        .value("c", rand+"c");
    		
    	}
    	// else {}  // se voglio gestire anche altre operazioni+
    	
    	return op_statement;
    	
	}
    
    
  //---------------------------------------------------------------------------------------------

  	private static ConsistencyLevel consistency_parser(String cl) {
      	ConsistencyLevel result = null;
  		if(cl.equalsIgnoreCase("ONE")){
  			result = ConsistencyLevel.ONE;
  		}
  		else if (cl.equalsIgnoreCase("QUORUM")){
  			result = ConsistencyLevel.QUORUM;
  		}
  		else if (cl.equalsIgnoreCase("ALL")){
  			result = ConsistencyLevel.ALL;
  		}
  		// else{} if {} // ci andrebbero anche tutti gli altri livelli, per adesso facciamo solo questi 3 
  		else{
  			System.err.println(" - ERROR : malformed consistency level "+cl+" - Must be one among ONE, QUORUM, ALL");
  			System.exit(-1);
  		}
  		return result;
  	}
      
}
