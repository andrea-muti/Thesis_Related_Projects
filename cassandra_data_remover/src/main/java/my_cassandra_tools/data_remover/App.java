package my_cassandra_tools.data_remover;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.core.querybuilder.QueryBuilder;

/**
 * Rimozione di tuple random nel Cassandra Cluster 

 * @author andrea-muti
 */

public class App {
    public static void main( String[] args ){
    	
        System.out.println( "\n****  Cassandra Data Remover \n" );
        
        if(args.length<2){
     	   System.err.println("ERROR : arg1 : IP address of the contact point node\n"
     			   			 +"        arg2 : number of  random tuples to remove\n");
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
        // let’s remove random tuples inside ” table
        
        
        QueryBuilder qb = new QueryBuilder(cluster);
        
        Statement select = qb.select().from("my_table").limit(n_tuple);
        ResultSet results = session.execute(select);
        List<Row> rows = results.all();
        System.out.println("\n * retrieved "+rows.size()+" tuples from the table");
       
        Iterator<Row> iter = rows.iterator();
        
        System.out.println("\n * start deletion of "+n_tuple+" tuples into table 'my_keyspace.my_table' [CL=ONE]");
        
        Instant start = Instant.now();
       
        while(iter.hasNext()){
        	Row r = iter.next();
        	UUID uuid = r.getUUID(0);
        	Statement delete = qb.delete().from("my_table")
				.where(QueryBuilder.eq("key", uuid));
        	session.execute(delete);
        }
   
        Instant end = Instant.now();
        System.out.println(" * deletion completed [ Execution time: "+Duration.between(start, end)+" ]");
      
             
        // ---------------------------------------------------------------------------------- //
        
        session.close();
        cluster.close();
            
    }
    
}
