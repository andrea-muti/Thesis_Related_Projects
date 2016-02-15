package my_cassandra_tools.data_injector;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.core.utils.UUIDs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;



/**
 * Inserimento di tuple random nel Cassandra Cluster per vedere come 
 * vengono distribuiti i dati e come viene ripartito il ring in caso 
 * di aggiunta di nodi al cluster
 * 
 * @author andrea-muti
 */

public class App {
    public static void main( String[] args ){
    	
    	// Settaggio del loggin level a ERROR
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.ERROR);
    	
    	System.out.println("\n ****** Cassandra Data Injector ******\n");
        
        if(args.length<2){
     	   System.err.println(" ERROR : arg1 : IP address of the contact point node\n"
     			   			 +"         arg2 : number of  random tuples to insert\n");
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
                 ).build();
        
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
        
        // --------------------------------------------------------------------------------- //
        
        // tupla : (key, a, b, c, d, e, f, g, h, i, j)  -> 1 uuid + 10 strings
        
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
        
        
        // --------------------------------------------------------------------------------- //
        
        // Now that you are connected to the “my_keyspace” keyspace, 
        // let’s insert random tuples inside ” table
        
        //int min = 0;
        //int max = 1100000000;
        //Random rn = new Random();
        
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
        
        PreparedStatement statement_insert = session.prepare("INSERT INTO my_table" 
        		+ "(key, a, b, c, d, e, f, g, h, i, j) VALUES (?,?,?,?,?,?,?,?,?,?,?);");
        BoundStatement boundStatement = new BoundStatement(statement_insert);
        boundStatement.setConsistencyLevel(ConsistencyLevel.ONE);
        
        UUIDs.random();
        
        int len_random_strings = 20;
        System.out.println("\n * start insertion of "+n_tuple+" tuples into table 'my_keyspace.my_table' [CL=ONE]");
        
        Instant start = Instant.now();
       
        for(int i = 0; i<n_tuple; i++){
        	UUID key = UUIDs.random();
        	String a = generate_random_string(len_random_strings);
        	String b = a;
        	String c = a;
        	String d = a;
        	String e = a;
        	String f = a;
        	String g = a;
        	String h = a;
        	String is = a;
        	String j = a;
        	session.execute(boundStatement.bind(key, a, b, c, d, e, f, g, h, is, j));
        	
        	if(i>percent_10 && i<percent_20 && !printed_10){ 
        		System.out.println("    - 10% of tuples inserted");
        		printed_10 = true;
        	}
        	else if (i>percent_20 && i<percent_30 && !printed_20){
        		System.out.println("    - 20% of tuples inserted");
        		printed_20 = true;
        	}
        	else if (i>percent_30 && i<percent_40 && !printed_30){
        		System.out.println("    - 30% of tuples inserted");
        		printed_30 = true;
        	}
        	else if (i>percent_40 && i<percent_50 && !printed_40){
        		System.out.println("    - 40% of tuples inserted");
        		printed_40 = true;
        	}
        	else if (i>percent_50 && i<percent_60 && !printed_50){
        		System.out.println("    - 50% of tuples inserted");
        		printed_50 = true;
        	}
        	else if (i>percent_60 && i<percent_70 && !printed_60){
        		System.out.println("    - 60% of tuples inserted");
        		printed_60 = true;
        	}
        	else if (i>percent_70 && i<percent_80 && !printed_70){
        		System.out.println("    - 70% of tuples inserted");
        		printed_70 = true;
        	}
        	else if (i>percent_80 && i<percent_90 && !printed_80){
        		System.out.println("    - 80% of tuples inserted");
        		printed_80 = true;
        	}
        	else if (i>percent_90 && !printed_90) {
        		System.out.println("    - 90% of tuples inserted");
        		printed_90 = true;
        	}
        	
        }
        System.out.println("    - 100% of tuples inserted");
        Instant end = Instant.now();
        System.out.println(" * insertion completed [ Execution time: "+Duration.between(start, end)+" ]");
      
             
        // ---------------------------------------------------------------------------------- //
        
        session.close();
        cluster.close();
            
    }
    
    private static String generate_random_string(int len){
    	char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < len; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        String output = sb.toString();
        return output;
    }
    
    
}
