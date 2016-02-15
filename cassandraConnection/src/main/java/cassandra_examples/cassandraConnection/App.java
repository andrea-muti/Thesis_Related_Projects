package cassandra_examples.cassandraConnection;

import java.io.IOException;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Metrics;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

/**
 * Try to write a simple application able to connect to a Cassandra Cluster
 * 
 * -->  è importante anche il pom.xml per generare un jar eseguibile
 * 
 * -->  mettendo il jar generato : cassandraConnection-0.0.1-SNAPSHOT-jar-with-dependencies.jar
 *      in una macchina in cui è installato e running un cassandra server
 *      posso eseguirlo con il comando :
 *         java -jar cassandraConnection-0.0.1-SNAPSHOT-jar-with-dependencies.jar localhost
 *       
 *      attenzione: controllare se ci sono già i keyspaces che vado a generare nel codice.
 *      in tal caso, prima dropparli, poi lanciare l'eseguibile.
 *      altrimenti : impossibile creare keyspace, already existing error
 * 
 * @author andrea-muti
 */

public class App {
    public static void main( String[] args ){
    	
       System.out.println( " *** TESTING CASSANDRA CONNECTION *** " );
       
       if(args.length<1){
    	   System.err.println("ERROR : IP address of the contact point node must be passed as argument" );
    	   System.exit(-1);
       }
       
       String contact_point_addr = args[0];
       
       
      /** STEP 1 : CREATING A CLUSTER OBJECT **/ 
        
      //Creating Cluster.Builder object
      Cluster.Builder builder1 = Cluster.builder();
      
      // Add a contact point (IP address of the node) using addContactPoint() method 
      // of Cluster.Builder object. This method returns Cluster.Builder
      Cluster.Builder builder2 = builder1.addContactPoint(contact_point_addr);
        
      // Using the new builder object, create a cluster object. 
      // To do so, you have a method called build() in the Cluster.Builder class. 
      Cluster cluster = builder2.build(); 
      
      // Oppure le tre istruzioni possono essere sostituite dall'unica istruzione
      // Cluster cluster = Cluster.builder().addContactPoint(contact_point_addr).build();
      
      System.out.println(" * created cluster object");
      
      
      /** STEP 2 : CREATING A SESSION OBJECT **/ 
      
      // Create an instance of Session object using the connect() method of Cluster class
      Session session = cluster.connect( );
      
      printMetrics(session, "iniziali");
      
      // Il metodo precedente crea una nuova session e la inizializza.
      // Se si dispone già di un keyspace, si può settare la session a tale keyspace
      // passando il nome del keyspace in string format al metodo connect()

      // Session session = cluster.connect(" Your keyspace name " );
      
      System.out.println(" * created session object");
      
      
      /** STEP 3 : CREATE A KEYSPACE  **/
      
      // E' possibile eseguire Queries CQL usando il metodo execute() della classe Session
      // passando a tale metodo la query da eseguire o in String format o come un object
      // della classe Statement.
      
      // Esempio:  creazione di un KeySpace di nome 'prova_keyspace'. Viene usata la
      // 'first replica placement' strategy (i.e. SimpleStrategy), e viene settato il
      // replication factor a 1      
      
      String query = "CREATE KEYSPACE IF NOT EXISTS  prova_keyspace WITH replication "
    		   		 + "= {'class':'SimpleStrategy', 'replication_factor':1}; ";
      
      session.execute(query);
      
      System.out.println(" * created keyspace prova_keyspace");
      
      
      /** STEP 4: USE THE KEYSPACE **/
      
      // è ora possibile usare il KeySpace creato usando il metodo execute()
      session.execute("USE prova_keyspace");
      
      System.out.println(" * now using keyspace prova_keyspace");
      
      /** STEP 5 : ALTER A KEYSPACE **/
      
      // Example of ALTERING a keyspace by means of a query executed on the session :
      // 	- vogliamo cambiare la replication option da Simple Strategy a Network Topology Strategy
      //    - vogliamo settare durable_writes a false

      //Query
      String query_alter =    "ALTER KEYSPACE prova_keyspace WITH replication " 
    		  				+ "=   {'class':'NetworkTopologyStrategy', 'datacenter1':3}" 
    		  				+ " AND DURABLE_WRITES = false;";
      
      session.execute(query_alter);
      
      System.out.println(" * Keyspace prova_keyspace altered:");
      System.out.println("      - replication strategy setted to: NetworkTopologyStrategy");
      System.out.println("      - durable_writes setted to false");
      
      
      System.out.println("\n * next step will drop the prova_keyspace - press any key to execute");
      try { System.in.read(); } 
      catch (IOException e) { e.printStackTrace(); }
      
      /** STEP 6 : DROPPING A KEYSPACE **/
      
      //Query drop
      String query_drop= "Drop KEYSPACE prova_keyspace";

      //Executing the query
      session.execute(query_drop);
      System.out.println(" * Keyspace prova_keyspace dropped");
      
      /** STEP 7 : CREATING A TABLE **/
      
      // first create a keyspace prova_ks where the table will be inserted
      String query_create = "CREATE KEYSPACE IF NOT EXISTS prova_ks WITH replication "
		   		 		    + "= {'class':'SimpleStrategy', 'replication_factor':1}; ";
      session.execute(query_create);     
      System.out.println(" * created keyspace prova_ks");

	  // use the prova_ks keyspace
	  session.execute("USE prova_ks");
	  System.out.println(" * now using keyspace prova_ks");
      
      //Query for create a table
      String query_ct = "CREATE TABLE IF NOT EXISTS  employees(emp_id int PRIMARY KEY, "
    		  		  + "emp_name text, "
    		   	      + "emp_city text, "
    		          + "emp_sal varint, "
                      + "emp_phone varint );";
      session.execute(query_ct);
      System.out.println(" * created table employees in keyspace prova_ks");
      
      System.out.println("\n * next step will ADD a column to employees table - press any key to execute");
      try { System.in.read(); } 
      catch (IOException e) { e.printStackTrace(); }
      
      /** STEP 8 : ALTER A TABLE - ADD ATTRIBUTE **/
      
      // Query_alter_table
      String query_alter_table = "ALTER TABLE employees ADD emp_email text";
      session.execute(query_alter_table);
      
      System.out.println(" * altered table employees : added column emp_email");
      
      System.out.println("\n * next step will DELETE a column to employees table - press any key to execute");
      try { System.in.read(); } 
      catch (IOException e) { e.printStackTrace(); }
      
      /** STEP 9 : ALTER A TABLE - DELETE ATTRIBUTE **/
      
      // Query_alter_table
      String query_alter_table_2 = "ALTER TABLE employees DROP emp_email;";
      session.execute(query_alter_table_2);
      
      System.out.println(" * altered table employees : deleted column emp_email");
      
      
      /** STEP 10 : CREATING AN INDEX **/
      // Query
      String query_create_index = "CREATE INDEX name ON employees (emp_name);";
      // execution
      session.execute(query_create_index);
      System.out.println(" * created an index 'name' on the 'emp_name' attribute of table 'employees'");
      
      /** STEP 11 : DROPPING AN INDEX **/
      // Query
      String query_drop_index = "DROP INDEX name;";
      session.execute(query_drop_index);
      System.out.println(" * dropped the index 'name' on the 'emp_name' attribute of table 'employees'");
      
      /** STEP 12 : INSERTING DATA INTO A TABLE **/
      
      String query_ins_1 = "INSERT INTO employees (emp_id, emp_name, emp_city, emp_phone, emp_sal)"
    		  			   +"VALUES(1,'ram', 'Hyderabad', 9848022338, 50000);" ;
    		 
      String query_ins_2 = "INSERT INTO employees (emp_id, emp_name, emp_city, emp_phone, emp_sal)"
    		              +"VALUES(2,'robin', 'Hyderabad', 9848022339, 40000);" ;
    		 
      String query_ins_3 = "INSERT INTO employees (emp_id, emp_name, emp_city, emp_phone, emp_sal)"
    		              +"VALUES(3,'rahman', 'Chennai', 9848022330, 45000);" ;
    		 
      session.execute(query_ins_1);
      session.execute(query_ins_2);
      session.execute(query_ins_3); 
      
      System.out.println(" * 3 tuples were been inserted into the employees table of the prova_ks keyspace");
      
      
      /** STEP 13 : READING DATA FROM A TABLE **/
      
      // query selection
      String query_select = "SELECT * FROM employees";
      // Getting the ResultSet
      ResultSet result_select = session.execute(query_select);
      // printing the results of the selections
      System.out.println("\n * Results of the query: "+query_select+" : ");
      System.out.println("   "+result_select.all());
      
      /** STEP 14 : DELETIN G DATA FROM A TABLE **/
      
      // query
      String query_del = "DELETE FROM employees WHERE emp_id=3;";
      // Executing the query
      session.execute(query_del); 
      System.out.println(" * deleted from employees tables the tuple with emp_id=3");

      // Getting the ResultSet
      ResultSet result_select_after_del = session.execute(query_select);
      // printing the results of the selections
      System.out.println("\n * Results of the query after deletion : "+query_select+" : ");
      System.out.println("   "+result_select_after_del.all());
      
      
      System.out.println("\n * next step will DELETE the whole employees table - press any key to execute");
      try { System.in.read(); } 
      catch (IOException e) { e.printStackTrace(); }
      
      /** STEP 15 : DROP A TABLE **/
      
      // Query_drop_table
      String query_drop_table = "DROP TABLE employees;";
     
      //Executing the query
      session.execute(query_drop_table);
    
      System.out.println(" * Table employees dropped");
      
      
      printMetrics(session, "finali");
         
      
      // CLOSING THE SESSION      
      session.close();
      System.out.println(" * Session closed\n");
      
      System.exit(0);
      
    }
    
    public static void printMetrics(Session session, String info) {
    	   System.out.println("\n\n ********** Metrics ["+info+"] ****************\n\n");
    	   
    	   Metrics metrics = session.getCluster().getMetrics();
    	   
    	   Gauge<Integer> gauge_known = metrics.getKnownHosts();
    	   Integer numberOfKnownHosts = gauge_known.getValue();
    	   System.out.printf(" - Number of known hosts: %d\n", numberOfKnownHosts);
    	   
    	   Gauge<Integer> gauge_connected = metrics.getConnectedToHosts();
    	   Integer numberOfConnectedHosts = gauge_connected.getValue();
    	   System.out.printf(" - Number of connected hosts: %d\n", numberOfConnectedHosts);
    	   
    	   Gauge<Integer> gauge_open_conn = metrics.getOpenConnections();
    	   Integer number_open_connections = gauge_open_conn.getValue();
    	   System.out.printf(" - Number of open_connections: %d\n\n", number_open_connections);
    	   
    	   
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
        	   double request_rate_five_minutes = requests_timer.getFiveMinuteRate();
        	   double request_rate_fifteen_minutes = requests_timer.getFifteenMinuteRate();
        	   double mean_rate = requests_timer.getMeanRate();
        	   
        	   System.out.println("\n - Requests Metrics : ");
        	   System.out.println("  -- Number of user requests: "+ numberUserRequests);
        	   System.out.println("  -- Request Rate in last 1 minute : " +request_rate_one_minute);
        	   System.out.println("  -- Request Rate in last 5 minutes : " + request_rate_five_minutes);
        	   System.out.println("  -- Request Rate in last 15 minutes : " + request_rate_fifteen_minutes);
        	   System.out.println("  -- Mean Request Rate : "+ mean_rate);
        	   
        	   System.out.println("\n  * taken snapshot of requests metrics");
        	   Snapshot snap = requests_timer.getSnapshot();
        	   long max_req = snap.getMax();
        	   long min_req = snap.getMin();
        	   double mean_req = snap.getMean();
        	   double median_req = snap.getMedian();
        	   double std_dev = snap.getStdDev();
        	   
        	   double p75 = snap.get75thPercentile();
        	   double p95 = snap.get95thPercentile();
        	   double p98 = snap.get98thPercentile();
        	   double p99 = snap.get99thPercentile();
        	   double p999 = snap.get999thPercentile();
        	   System.out.println("   - max req: "+max_req);
        	   System.out.println("   - min req: "+min_req);
        	   System.out.println("   - mean req: "+mean_req);
        	   System.out.println("   - median req: "+median_req);
        	   System.out.println("   - standard deviation: "+std_dev);
        	   System.out.println("    - 75 percentile: "+p75);
        	   System.out.println("    - 95 percentile: "+p95);
        	   System.out.println("    - 98 percentile: "+p98);
        	   System.out.println("    - 99 percentile: "+p99);
        	   System.out.println("    - 999 percentile: "+p999); 
        	   
    	   } finally {
    	       context.stop();
    	   }
 
    	   System.out.println("\n********************************************\n\n");
    	}
    
}
