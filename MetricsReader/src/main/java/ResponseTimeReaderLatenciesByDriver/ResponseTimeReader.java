package ResponseTimeReaderLatenciesByDriver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;

import javax.management.MBeanServerConnection;

import org.slf4j.LoggerFactory;

import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Metrics;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.utils.UUIDs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;


public class ResponseTimeReader {
	
	public static void main(String[] args){
		
		// Settaggio del loggin level a ERROR
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.ERROR);
		
		
		// funziona! ma non se il processo viene terminato da dentro eclipse
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() { 
		      System.out.println("YOU PRESSED CTR+C , BYE");
		   }
		 });
				
		//String contact_point_addr = args[0];
    	String contact_point_addr = "192.168.0.169";
    	check_contact_point_address(contact_point_addr);
		
    	// int samplesCount = args[1] <- parse int;
		int samplesCount = 8000;
		
		// int op_interval_msec = args[2] <-- parse int , check se è un int
		int op_interval_msec = 500;

		// String dir_path = args[3] ;
		String dir_path = "/home/andrea-muti/Scrivania/metrics_java_ResponseTimeReader";
		
		
		// String operation = args[4] ; // check valori : "Read" , "Write" 
		String operation = "Read";
		
		// String cl = args[5] ; // valori attualmente riconosciuti dal mio parser : "ONE", "QUORUM", "ALL"
		String cl = "ONE";
		ConsistencyLevel consistency_level = consistency_parser(cl);
		
		
		// int update_interval_msec = args[2] <-- parse int, check se è davvero un int
		int update_interval_msec = 2000;
		
    	String jmx_port = "7199";		   // stiamo assumento che la jmx port impostata nei nodi sia la 7199
    	
    	String keyspace = "my_keyspace";   // stiamo assumento che il keyspace verso il quale generare le operazioni
    									   // sia già stato creato e che si chiami my_keyspace
    	
    									   
    	String table = "my_table"; 			// assumiamo inoltre che in questo keyspace ci sia la table my_table solita 
    	
    	System.out.println("\n **** CLUSTER NODES Response Time READER ****\n");   	
                   
        List<String> live_nodes = getNodesAddresses(contact_point_addr, jmx_port);
        
        if( live_nodes == null ){
        	System.err.println(" - ERROR : failed to get Live Nodes");
			System.exit(-1);
        }
        
        int n_nodes = live_nodes.size();
             
        System.out.println(" - there are "+n_nodes+" Live Nodes in the Cluster\n");
		
        // ------------------ OPENING A SESSION WITH THE CLUSTER ------------------
        
        Cluster cluster;
        Session session = null;
             
        cluster = Cluster.builder()
        		.addContactPoint(contact_point_addr)
        		//.withRetryPolicy(DefaultRetryPolicy.INSTANCE)
        		.withLoadBalancingPolicy(new RoundRobinPolicy())
        		.build();
        try{
        	session = cluster.connect();
        }
        catch (NoHostAvailableException e){
        	System.err.println(" - ERROR : no one of the given contact-point nodes is reachable");
        	System.exit(-1);
        }
        catch (Exception e){
        	System.err.println(" - ERROR : there are problems when establishing connection with the cluster - "+e.getMessage());
        	System.exit(-1);
        }
        
        
        // --------------- SELECTING THE KEYSPACE my_keyspace ------------- //
        // NB : assumiamo che il keyspace già esista, altrimeti errore
        
        try{
        	session.execute("USE "+keyspace);
        }
        catch(NoHostAvailableException e){
        	System.err.println(" - ERROR : all nodes in the cluster are down or unreachable");
        	System.exit(-1);
        }
        catch(Exception e){
        	System.err.println(" - ERROR : "+e.getMessage());
        	System.exit(-1);
        }
        
        // -------- START EXECUTION OF THE OPERATION ----- //
        
        int success = 0;
        int failed = 0;
        
        //start counting update_interval_msec sec
		long now = System.currentTimeMillis();			
	    
		FileWriter resultFileWriter = null;
		BufferedWriter resultBufferedWriter;
		try {
			resultFileWriter = new FileWriter(dir_path+"/"+operation+"_response_time_results.csv");
		} catch (IOException e2) {
			System.err.println("error with file writer");
			e2.printStackTrace();
		}
		resultBufferedWriter = new BufferedWriter (resultFileWriter);
		
		String DELIMITER  = ";";
        
        for(int i = 0; i<samplesCount; i++){
        	
        	UUID random_key = UUIDs.random();
               	
        	Statement operation_statement = create_statement_operation(operation,consistency_level,keyspace, table, random_key);

        		
        	try{ 
        		session.execute(operation_statement);
        		success++;
        	}  //---------------------------------------------------------------------------------------------
        	catch(Exception e){
        		System.out.println("error on key: "+random_key+" | "+e.getMessage());
        		failed++;
        	}
        	
        	
        	//  ogni 'update_interval_msec' aggiorno il file
        	if( System.currentTimeMillis() - now >= update_interval_msec ){
        		
				// prende tempo; operazioni fatte; latenza
        		
        		Metrics metrics = session.getCluster().getMetrics();
        		
				Timer requests_timer = metrics.getRequestsTimer();
				Snapshot snap = requests_timer.getSnapshot();
		      	double mean_latency = snap.getMean()/1000;
	      	    double p95 = snap.get95thPercentile()/1000;
	      	    double p99 = snap.get99thPercentile()/1000;
	      	   
				try {
					resultBufferedWriter.write( MyHour.getCurrentMoment()+DELIMITER+
											    mean_latency+DELIMITER+ 
											    p95+DELIMITER+
											    p99+DELIMITER+
											    (i+1)+DELIMITER+ 						// num operations executed
											    success+DELIMITER+						// num successful operations
											    failed+DELIMITER+"\n");				  	// num failed operations
				} catch (IOException e) {
					System.err.println("error with file writer");
					e.printStackTrace();
				}
				
				now = System.currentTimeMillis();
				
				try {	
					resultBufferedWriter.flush();
				} catch (IOException e) {
					System.err.println("error with file writer");
					e.printStackTrace();
				}
				
		      	   
			}
        	
        	// sleep  --> facciamo una operazione ogni op_interval_msec mms
        	try { Thread.sleep(op_interval_msec); } 
        	catch (InterruptedException e) { }
			
        	
        } // end for loop with operations
        
        System.out.println(" - Execution Completed");
        System.out.println("     - total successful : "+success);
        System.out.println("     - total failed : "+failed);
  		
  		try {
			resultBufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}  
        
        
	} // end main
	
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
	
	private static List<String> getNodesAddresses(String contact_point_addr, String jmx_port){
		JMXReader jmxreader = new JMXReader(contact_point_addr, jmx_port);
        MBeanServerConnection remote = null;
		try {
			remote = jmxreader.connect();
		} catch (IOException e) {
			System.err.println(" - ERROR : There are communication problems when establishing the connection with the Cluster"
   		           + " [ "+e.getMessage()+" ]");
			System.exit(-1);
		}
		catch (SecurityException e) {
			System.err.println(" - ERROR : There are security problems when establishing the connection with the Cluster"
	   		           + " [ "+e.getMessage()+" ]");
			System.exit(-1);
		}
		catch (Exception e) {
			System.err.println(" - ERROR : There are unknown problems when establishing the connection with the Cluster"
	   		           + " [ "+e.getMessage()+" ]");
			System.exit(-1);
		}

        List<String> live_nodes = jmxreader.getLiveNodes(remote);
        
        jmxreader.disconnect();
        
        return live_nodes;
	}
	
	//---------------------------------------------------------------------------------------------
}
