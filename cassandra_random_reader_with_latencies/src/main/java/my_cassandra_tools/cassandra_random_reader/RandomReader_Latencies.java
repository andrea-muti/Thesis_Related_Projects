package my_cassandra_tools.cassandra_random_reader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Metrics;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.utils.UUIDs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Cassandra Random Reader with Latency collection 
 *
 */
public class RandomReader_Latencies {
	
    public static void main( String[] args ){
    	
    	// Settaggio del loggin level a ERROR
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.ERROR);
    	
        System.out.println("\n ****** Cassandra Random Reader ******\n");
        
        if(args.length<2){
     	   System.err.println(" ERROR : arg1 : IP address of the contact point node\n"
     			   			 +"         arg2 : number of  random tuples to read\n");
     	   System.exit(-1);
        }
        
        String contact_point_addr = args[0];
        int n_tuple = Integer.parseInt(args[1]);
       
        Cluster cluster;
        Session session;
             
        cluster = Cluster.builder()
        		.addContactPoint(contact_point_addr)
        		.withRetryPolicy(DefaultRetryPolicy.INSTANCE)
        		.withLoadBalancingPolicy(
        				new RoundRobinPolicy()
                )
        		.build();
        session = cluster.connect();
        
     
        
        // ------------------------------------------------------------------------------- //
        
        // creation of the keyspace
        String query_creation_KS = "CREATE KEYSPACE  IF NOT EXISTS my_keyspace WITH replication "
        					      + "= {'class':'SimpleStrategy', 'replication_factor':3}; ";
        session.execute(query_creation_KS);
        
        // Using the created keyspace 
        session.execute("USE my_keyspace");
        
        System.out.println("\n * established connection with the cluster @ "+contact_point_addr
        				   +"\n * created keyspace 'my_keyspace'"
        		           +"\n * using keyspace 'my_keyspace'");
        
        //---------------------------------------------------------
        
      //Query for create a table
        String query_ct = "CREATE TABLE IF NOT EXISTS my_table(key uuid PRIMARY KEY, "
      		  		  + "a text, "
      		   	      + "b text, "
      		   	      + "c text, "
      		   	      + "d text, "
      		   	      + "e text, "
      		   	      + "f text, "
      		   	      + "g text, "
      		   	      + "h text, "
      		   	      + "i text, "
      		          + "j text ); ";
                       
        session.execute(query_ct);
        System.out.println(" * created table my_table in keyspace my_keyspace");
        
        //-----------------------------------------------------
        
      
        
        int percent_10 =((n_tuple/100)*10);
        int percent_20 =((n_tuple/100)*20);
        int percent_30 =((n_tuple/100)*30);
        int percent_40 =((n_tuple/100)*40);
        int percent_50 =((n_tuple/100)*50);
        int percent_60 =((n_tuple/100)*60);
        int percent_70 =((n_tuple/100)*70);
        int percent_80 =((n_tuple/100)*80);
        int percent_90 =((n_tuple/100)*90);
        boolean printed_10 = false;
        boolean printed_20 = false;
        boolean printed_30 = false;
        boolean printed_40 = false;
        boolean printed_50 = false;
        boolean printed_60 = false;
        boolean printed_70 = false;
        boolean printed_80 = false;
        boolean printed_90 = false;
        
        System.out.println("\n\n * starting execution of "+n_tuple+" random reads form table 'my_keyspace.my_table' [CL=ONE]");
        
        final double start = System.nanoTime();
        int failed = 0;
        
        //start counting 1 sec
		long now = System.currentTimeMillis();			
		int time = 0;
		
		List <Long> readLatencies = new ArrayList<Long>();
        
		FileWriter readResultFileWriter = null;
		BufferedWriter readResultBufferedWriter;
		try {
			readResultFileWriter = new FileWriter("read_result.csv");
		} catch (IOException e2) {
			System.err.println("error with file writer");
			e2.printStackTrace();
		}
		readResultBufferedWriter = new BufferedWriter (readResultFileWriter);
		
	
		String DELIMITER=";";
        
        for(int i = 0; i<n_tuple; i++){
        	UUID random_key = UUIDs.random();
        	
        	Statement select = QueryBuilder.select("key","a","b","c","d","e","f","g","h","i","j")
        			.from("my_keyspace", "my_table")
        			.where( QueryBuilder.eq("key", random_key) );
        	
        	select.setConsistencyLevel(ConsistencyLevel.ONE);
        	
        	// TAKE OPs START TIME
			long start_read = System.nanoTime();		
        	try{ 
        		session.execute(select);
        	}
        	catch(Exception e){
        		System.out.println("error on key: "+random_key+" | "+e.getMessage());
        		failed++;
        	}
        	
        	// CALCULATE LATENCY
			Utils.collectLatencyStatistics(start_read, readLatencies);
        	
        	
        	if(i>percent_10 && i<percent_20 && !printed_10){ 
        		System.out.println("    - 10% of random reads executed");
        		printed_10 = true;
        	}
        	else if (i>percent_20 && i<percent_30 && !printed_20){
        		System.out.println("    - 20% of random reads executed");
        		printed_20 = true;
        	}
        	else if (i>percent_30 && i<percent_40 && !printed_30){
        		System.out.println("    - 30% of random reads executed");
        		printed_30 = true;
        	}
        	else if (i>percent_40 && i<percent_50 && !printed_40){
        		System.out.println("    - 40% of random reads executed");
        		printed_40 = true;
        	}
        	else if (i>percent_50 && i<percent_60 && !printed_50){
        		System.out.println("    - 50% of random reads executed");
        		printed_50 = true;
        	}
        	else if (i>percent_60 && i<percent_70 && !printed_60){
        		System.out.println("    - 60% of random reads executed");
        		printed_60 = true;
        	}
        	else if (i>percent_70 && i<percent_80 && !printed_70){
        		System.out.println("    - 70% of random reads executed");
        		printed_70 = true;
        	}
        	else if (i>percent_80 && i<percent_90 && !printed_80){
        		System.out.println("    - 80% of random reads executed");
        		printed_80 = true;
        	}
        	else if (i>percent_90 && !printed_90) {
        		System.out.println("    - 90% of random reads executed");
        		printed_90 = true;
        	} 
        	
        	//  ogni 1 secondo aggiorno il file
        	if(System.currentTimeMillis()-now >=1000){
        		
				// prende tempo; operazioni fatte; latenza
				CurrentAverageLatency rd_cal = Utils.perSecondLatencyAverage(readLatencies);
				
				try {
					readResultBufferedWriter.write(rd_cal.getTimeLatency()+DELIMITER+time++ +DELIMITER+rd_cal.getThisLatency()+DELIMITER+ rd_cal.getOpsNumber()+DELIMITER+ "\n");
				} catch (IOException e) {
					System.err.println("error with file writer");
					e.printStackTrace();
				}
				
				now = System.currentTimeMillis();
				
				try {	
					readResultBufferedWriter.flush();
				} catch (IOException e) {
					System.err.println("error with file writer");
					e.printStackTrace();
				} 
			}
        	
        } // end for loop with operations
        
  		final double end = System.nanoTime();   
  		
  		try {
			readResultBufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}  
        
        System.out.println("    - 100% of random reads executed");
        System.out.println(" * completed execution of "+n_tuple+" random reads form table 'my_keyspace.my_table'\n");
        
        double exec_time = ( end - start )/ 1000000000 ;
        System.out.println(" * insertion completed [ Execution time: "+exec_time +" ]");
      
        System.out.println(" * successful ops : "+(n_tuple-failed));
        System.out.println(" * failed ops : "+failed+"\n");
        
        System.out.println(" * my throughput computation : "+(n_tuple/exec_time) +" ops/sec ");
        
        
        printMetrics(session, n_tuple+" READS");
        
        session.close();
        cluster.close();
    }
    
    public static void printMetrics(Session session, String info) {
  	   System.out.println("\n\n ********** Session Metrics ["+info+"] **************** \n\n");
  	   
  	   Metrics metrics = session.getCluster().getMetrics();
  	   
  	   Gauge<Integer> gauge_known = metrics.getKnownHosts();
  	   Integer numberOfKnownHosts = gauge_known.getValue();
  	   System.out.printf(" - Number of known hosts: %d\n", numberOfKnownHosts);
  	   
  	   
  	   Metrics.Errors errors = metrics.getErrorMetrics();
  	   long connection_errors_count = errors.getConnectionErrors().getCount();
  	   long read_timeouts_count = errors.getReadTimeouts().getCount();
  	   long write_timeouts_count = errors.getWriteTimeouts().getCount();
  	   
  	   System.out.println(" - Error Metrics : ");
  	   System.out.printf("   -- Number of connection errors : %d\n", connection_errors_count);
  	   System.out.printf("   -- Number of read timeouts: %d\n", read_timeouts_count);
  	   System.out.printf("   -- Number of write timeouts: %d\n", write_timeouts_count);
  	   
  	   
  	   Timer requests_timer = metrics.getRequestsTimer();
  	   Timer.Context context = requests_timer.time();
  	   try {
  	       long numberUserRequests = requests_timer.getCount();
  	       double request_rate_one_minute = requests_timer.getOneMinuteRate();
      	   double mean_rate = requests_timer.getMeanRate();
      	   
      	   System.out.println("\n - Request Rate Metrics (events/second) : ");
      	   System.out.println("  -- Number of user requests: "+ numberUserRequests);
      	   System.out.println("  -- Request Rate in last 1 minute : " +request_rate_one_minute);
      	   System.out.println("  -- Mean Request Rate : "+ mean_rate);
      	   
      	   Snapshot snap = requests_timer.getSnapshot();
      	   double mean_req = snap.getMean()/1000;
      	   double p95 = snap.get95thPercentile()/1000;
      	   
      	   System.out.println("\n - Request Latency Metrics (msec) : ");
      	   System.out.println("   - mean req: "+mean_req);
      	   System.out.println("   - 95 percentile: "+p95);
      	   
  	   } 
  	   finally { context.stop(); }

  	   System.out.println("\n********************************************\n\n");
  	}
    
}
