package cassandra_examples.getting_started_datastax;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.datastax.driver.core.querybuilder.QueryBuilder;

/**
 * Getting Started with Apache Cassandra and Java [ by DataStax ]
 * 
 * reference: 
 * 		https://academy.datastax.com/demos/getting-started-apache-cassandra-and-java-part-i
 * 		https://www.planetcassandra.org/getting-started-with-apache-cassandra-and-java-part-2/ 
 *
 *
 *  NOTA : 
 *  	si presuppone l'esistenza di un keyspace 'getting_started_datastax'
 *  	contente una table users(lastname, age, city, email, firstname)
 * 
 */

public class App {
    public static void main( String[] args ){
    	
        System.out.println( "\n****  Gettin Started with Cassandra by Datastax ****\n" );
        
        if(args.length<1){
     	   System.err.println("ERROR : IP address of the contact point node must be passed as argument" );
     	   System.exit(-1);
        }
        
        String contact_point_addr = args[0];
       
        // First we need to create cluster and session instance fields to hold the references. 
        // A session will manage the connections to our cluster.  
        Cluster cluster;
        Session session;
      
        // Connect to your instance using the Cluster.builder method. 
        // It will add a contact point and build a cluster instance. 
        // Get a session from your cluster, connecting to the "getting_started_datastax” keyspace.
        /*   
         *   NOTARE L'USO DI withRetryPolicy: 
         *     immaginiamo di eseguire questo codice su un cluster invece che su una single instance,
         *     vogliamo avere delle garanzie in caso di failover dei nodi. Possiamo avere tali
         *     garanzie usando una RetryPolicy. La Retry Policy determina il comportamento di 
         *     default che deve essere adottato quando una request va o in timeout o va verso un
         *     nodo unavailable. In questo esempio, usiamo una DefaultRetryPolicy, la quale farà
         *     in modo che le queries vengano ri-eseguite se si verifica :
         *     		- un "read timeout", ovvero quando enough replicas hanno risposto ma il dato
         *     		  non è stato ricevuto
         *     		- un "write timeout", ovvero quando il timeout si verifica durante la scrittura
         *     		  del log da parte di batch statements. 
         * 
         *   NOTARE L'USO di withLoadBalancingPolicy :
         *   	Una Load Balancing Policy determinerà in quale nodo eseguirà la query.
         *   	Poichè un client può leggere o scrivere da/su ogni nodo, a volte ciò
         *   	può essere inefficiente. Se un nodo riceve una read o una write che si riferiscono
         *      ad un dato owned da un altro nodo, esso coordinerà la request per il client.
         *      Possiamo usare una LoadBalancing Policy per controllare tale processo.
         *      La TokenAwarePolicy assicura che le requests andranno al nodo o alla replica
         *      responsabile per il dato indicato dalla primary key. Essa è wrapped in una
         *      DCAwareRoundRobinPolicy per assicurarsi che le requests rimangano nel datacenter
         *      locale. 
         * 
         */
        
        cluster = Cluster.builder().addContactPoint(contact_point_addr)
        		.withRetryPolicy(DefaultRetryPolicy.INSTANCE)
        		.withLoadBalancingPolicy(
        				new TokenAwarePolicy(new DCAwareRoundRobinPolicy())
                 )
        		.build();
        session = cluster.connect("getting_started_datastax");
        
        System.out.println(" * established connection with the cluster @ "+contact_point_addr
        		           +"\n * using keyspace 'getting_started_datastax'");
        
        // Now that you are connected to the “getting_started_datastax” keyspace, 
        // let’s insert a user into the “users” table
        session.execute("INSERT INTO users (lastname, age, city, email, firstname) "
        		      + "VALUES ('Jones', 35, 'Austin', 'bob@example.com', 'Bob')");
        
        System.out.println(" * inserted ('Jones', 35, 'Austin', 'bob@example.com', 'Bob') into users");
        
        // Use select to get the user we just entered
        ResultSet results = session.execute("SELECT * FROM users WHERE lastname='Jones'");
        System.out.println(" * Results of 'SELECT * FROM users WHERE lastname='Jones'");
        for (Row row : results) {
        	System.out.format("   - %s %d\n", row.getString("firstname"), row.getInt("age"));
        }
        
        // Update the same user with a new age
        session.execute("update users set age = 36 where lastname = 'Jones'");
        
        System.out.println(" * updated age to 36 of users with lastname 'Jones'");
        
        // Select and show the change
        results = session.execute("select * from users where lastname='Jones'");
        
        System.out.println(" * results of query : select * from users where lastname='Jones'");
        for (Row row : results) {
        	System.out.format("   - %s %d\n", row.getString("firstname"), row.getInt("age"));
        }
        
        // Delete the user from the users table
        session.execute("DELETE FROM users WHERE lastname = 'Jones'");
        System.out.println(" * deleted from user tuples with lastname = 'Jones'");
        
        // Show that the user is gone
        results = session.execute("SELECT * FROM users");
        
        System.out.println(" * Results of query: SELECT * FROM users");
        for (Row row : results) {
        	System.out.format("    - %s %d %s %s %s\n", row.getString("lastname"), row.getInt("age"),  row.getString("city"), row.getString("email"), row.getString("firstname"));
        }
        
        // un modo alternativo per fare delle insert, invece che usare delle strighe da passare
        // al metodo execute della session, consiste nell'utilizzo dei PREPARED STATEMENTS.
        // I P.S. sono piu sicuri e piu performanti poichè devo essere parsed dal cluster
        // solo una volta.
        
        PreparedStatement statement_insert = session.prepare("INSERT INTO users" 
        		+ "(lastname, age, city, email, firstname)"
        		+ "VALUES (?,?,?,?,?);");
        
        BoundStatement boundStatement = new BoundStatement(statement_insert);
        
        session.execute(boundStatement.bind("Muti", 26, "Aprilia",
        "muti.andrea@gmail.com", "Andrea"));
        
        System.out.println(" * inserted ('Muti', 26, 'Aprilia', 'muti.andrea@gmail.com', 'Andrea') into users");
        
        
        // allo stesso modo della insert, possiamo creare delle queries nel cluster usando
        // un QUERY BUILDER, che è piu sicuro e ci salva da potenziali CQL injection attacks
        
        // Use select to get the user we just entered
        Statement select = QueryBuilder.select().all().from("getting_started_datastax", "users")
        		.where(QueryBuilder.eq("lastname", "Muti"));
        
        results = session.execute(select);
        
        System.out.println(" * Results of selection of tuples with lastname = 'Muti'");
        for (Row row : results) {
        	System.out.format("    - %s %d \n", row.getString("firstname"), row.getInt("age"));
        }
        
      // Update the same user with a new age
        Statement update = QueryBuilder.update("getting_started_datastax", "users")
        		.with(QueryBuilder.set("age", 27))
        	.where((QueryBuilder.eq("lastname", "Muti")));
        
        session.execute(update);
        System.out.println(" * updated age 27 of tuples with lastname = 'Muti'");
        
        // Select and show the change
        select = QueryBuilder.select().all().from("getting_started_datastax", "users")
        		.where(QueryBuilder.eq("lastname", "Muti"));
        
        results = session.execute(select);
        
        System.out.println(" * Results of selection of tuples with lastname = 'Muti'");
        for (Row row : results) {
        	System.out.format("    - %s %d \n", row.getString("firstname"), row.getInt("age"));
        }
        
        // Delete the user from the users table
        Statement delete = QueryBuilder.delete().from("users")
        		.where(QueryBuilder.eq("lastname", "Muti"));

        results = session.execute(delete);
        //Show that the user is gone
        System.out.println(" * deletion from users of tuples with lastname = 'Muti'");
        
        select = QueryBuilder.select().all().from("getting_started_datastax", "users");
        
        System.out.println(" * Results of selection all tuples from users table");
        results = session.execute(select);
        for (Row row : results) {
        	System.out.format("    - %s %d %s %s %s\n", row.getString("lastname"), row.getInt("age"), row.getString("city"),
        	row.getString("email"), row.getString("firstname"));
        }

        
        // Clean up the connection by closing it
        cluster.close();
            
    }
}
