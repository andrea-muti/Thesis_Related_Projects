package ThesisRelated.RandomReader.MultipleConnection;

import java.util.UUID;

import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.utils.UUIDs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Cassandra Multi Random Reader
 * @author andrea-muti
 */

public class MultiRndReader {
	
    public static void main( String[] args ) {
    	
    	// Settaggio del loggin level a ERROR
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.ERROR);
    	
        
    	// int n_threads = Integer.parseInt(args[0])
    	int n_threads = 20;
    	
    	// int n_tuples = Integer.parseInt(args[1])
    	int n_tuples = 1000;
    	
    	// String contact_point = args[2]
    	String contact_point = "192.168.0.169";
    	
    	for(int i=0; i<n_threads; i++){
    		RandomReader_Thread t = new RandomReader_Thread(i, n_tuples, contact_point);
    		t.start();
    	}
    	
    	
    	
    }
    
    static class RandomReader_Thread implements Runnable {

    	private Thread t;
    	private int id;
    	private int n_tuples;
    	private String contact_point_address;
    	
    	RandomReader_Thread(int id, int n_tuples, String contact_point ){
    		this.id = id;
    		this.n_tuples = n_tuples;
    		this.contact_point_address = contact_point;
    	}
    	
    	
    	public void start() {
    		System.out.println(" - Starting Random Reader Thread n* "+ this.id +" for " +  this.contact_point_address );
		      if (t == null){
		         t = new Thread (this);
		         t.start ();
		      }
    	}
    	
		public void run() {

	        Cluster cluster;
	        Session session;
	             
	        cluster = Cluster.builder()
	        		.addContactPoint(this.contact_point_address)
	        		.withRetryPolicy(DefaultRetryPolicy.INSTANCE)
	        		.build();
	        session = cluster.connect();
	        
	        final double start = System.nanoTime();
	        int failed = 0;
	        double exec_time = 0;
	        
	        for(int i = 0; i<n_tuples; i++){
	        	
	        	UUID random_key = UUIDs.random();
	        	Statement select = QueryBuilder.select("key","a","b","c","d","e","f","g","h","i","j").from("my_keyspace", "my_table").where(QueryBuilder.eq("key", random_key));
	        	select.setConsistencyLevel(ConsistencyLevel.ONE);
	        	
	        	try{ session.execute(select); }
	        	catch(Exception e){ failed++; }

		  		final double end = System.nanoTime();         
		        exec_time = ( end - start )/ 1000000000 ;  
	        }
	        
	        session.close();
	        cluster.close();
	        System.out.println(" * [thread "+this.id+"] successful ops : "+(n_tuples-failed)+" | failed ops : "+failed);
	        System.out.println(" * [thread "+this.id+"] my throughput computation : "+(n_tuples/exec_time) +" ops/sec ");
	        
	        System.out.println(" - Random Reader Thread n* "+ this.id +" completed the execution" );
	        
		} // end run()
			
    }
    
    
}
