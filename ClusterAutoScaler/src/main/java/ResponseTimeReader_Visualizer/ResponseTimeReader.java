package ResponseTimeReader_Visualizer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

public class ResponseTimeReader {
	
	private String contact_point_address;
	private String jmx_port;
	private String dir_path;
	private RTReader reader;
	private ExecutorService executor;
	private boolean execute;
	
	
	public ResponseTimeReader(String contact_point_addr, String jmx_port, String dir_path){
		// Settaggio del loggin level a ERROR
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.ERROR);
		
		this.contact_point_address=contact_point_addr;
		this.jmx_port=jmx_port;
		this.dir_path=dir_path;
		this.execute=false;
		String consistency = "ONE";
		this.reader = new  RTReader(this.contact_point_address, this.jmx_port, this.dir_path, consistency);
		this.executor = Executors.newFixedThreadPool(1);
		 
		//Runtime.getRuntime().addShutdownHook(new ShutdownHook(this.reader);
	    System.out.println(" - [ResponseTimeReader] ResponseTimeReader initialized");
	}

	public void start_read_RT(){
		System.out.println(" - [ResponseTimeReader] start reading ResponseTime [ results are saved in dir "+dir_path+"  ]");
		this.execute=true;
		while(execute){
			executor.execute(this.reader);
			try { Thread.sleep(4000); }
			catch (InterruptedException e) {e.printStackTrace();}
		}
	}
	public void stop_read_RT(){
		this.execute=false;
		this.executor.shutdown();
		// Wait until all threads are finish
		while (!executor.isTerminated()) {}
	}
	
	public static void main(String[] args){
		String contact_point_addr = "192.168.0.169";
		String jmx_port = "7199";
		String dir_path = "/home/andrea-muti/Scrivania/autoscaling_experiments_results/";
		ResponseTimeReader reader = new ResponseTimeReader(contact_point_addr, jmx_port, dir_path);
		reader.start_read_RT();
	}
	
	static class RTReader  implements Runnable{
		@SuppressWarnings("unused")
		private boolean execute;
		private String ip_address;
		private String port_number;
		int sampling_interval_msec;
		BufferedWriter writer;
		private String consistency_level;
		private ConsistencyLevel cl;
		private int samples_count_RT;
		private String operation_RT;
		private Cluster cluster;
			
		   
		RTReader(String ip, String port,  String dir_path, String consistency){
			this.ip_address = ip;
			this.port_number = port;
			this.operation_RT = "READ";
			this.samples_count_RT=100;
			String file_name = dir_path+"/response_times.txt";
			try {
				this.writer = new BufferedWriter(new FileWriter(file_name, true));		
			} catch (IOException e) {
				System.err.println("Error in opening: "+file_name);
			}
			this.consistency_level = consistency;
			this.cl = consistency_parser(consistency_level);
			this.cluster = Cluster.builder()
					.addContactPoint(this.ip_address)
					.withLoadBalancingPolicy(new RoundRobinPolicy()).build();
			this.execute=false;
	   }
		   
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

		public void run() {
		   /**    LETTURA RESPONSE TIME    **/
	        double[] response_times = getResponseTimes(port_number, ip_address, samples_count_RT, operation_RT, cl);       
	        double rt_mean = response_times[0];
	        double rt_95p = response_times[1];
	        String content = System.currentTimeMillis() + " " + rt_mean + " " + rt_95p;
			try {
				writer.append(content+"\n");
				writer.flush();
			} catch (IOException e) {}	
		}// end run
		 
		
		
		private double[] getResponseTimes( String jmx_port_number, String cp_address, 
											int samples_count,  String operation,ConsistencyLevel cl ){
			double[] res_times = new double[2];
			
			int retry_interval_sec = 3;
			int window_size = 100;
			
			int warmup_ops = 100;
			
			String keyspace = "my_keyspace";
			String table = "my_table";
			
			Session session = null;	
			session = ClusterUtils.createSession(this.cluster, retry_interval_sec);
			
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
			for( int i = 0; i<samples_count; i++ ){
				
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
			try{ session.close(); }
			catch(Exception e){ System.err.println("Error during closing the session"); }
			
			double mean_latency = ma.getAvg();
			double p95 = ma.get95percentile();
			
			res_times[0] = mean_latency;
			res_times[1] = p95;
			
			return res_times;
	}
		   
	   private static Statement create_statement_operation(String operation, ConsistencyLevel consistency_level, 
			   String keyspace, String table, UUID random_key) {
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
		   
	}
}
