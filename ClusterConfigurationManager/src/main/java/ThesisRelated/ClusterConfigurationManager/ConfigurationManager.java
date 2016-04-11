package ThesisRelated.ClusterConfigurationManager;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;

import Utils.FileUtils;


/**
 * ClusterConfigurationManager
 * @author andrea-muti
 * @since 17/03/2016
 */

@SuppressWarnings("unused")
public class ConfigurationManager {
	
	// instance variables
	private String contact_point_address;
	private String jmx_port_number;
	private JMXConnectionManager jmx_connection_manager;
	private String remote_username;
	private String remote_password;
	private String remote_cassandra_dir;
	
	// public constructor
	public ConfigurationManager(String propertiesFile){
		this(FileUtils.loadProperties(new File(propertiesFile)));
	}
	
	// private constructor
	private ConfigurationManager(Properties cmProperties){
		super();
		this.contact_point_address = cmProperties.getProperty("contact_point_address"); 
		this.jmx_port_number = cmProperties.getProperty("jmx_port_number");
		this.jmx_connection_manager = new JMXConnectionManager(this.contact_point_address, this.jmx_port_number);
		this.remote_username = cmProperties.getProperty("ssh_remote_username");
		this.remote_password = cmProperties.getProperty("ssh_remote_password");
		this.remote_cassandra_dir = cmProperties.getProperty("remote_cassandra_dir");
	}
	
	//---------------------------------------------------------------------------------
	
	/** ADD NODE
	 * 		adds the node with IP=ip_address to the cluster
	 * @param ip_address : IP Address of the node to add
	 * @param jmx_port : JMX Port number of the node to add
	 * @return true if the new node was addedd successfully, false otherwise
	 */
	public boolean addNode(String ip_address, String jmx_port){
		boolean success = true;
		long start,end;
		double start_time;
		
		// check that the node to insert is not already a node of the cluster
		List<String> live_nodes = getLiveNodes(this.contact_point_address, this.jmx_port_number);
		if( live_nodes.contains((String) ip_address) ){
			System.err.println("\n - ERROR : the node selected for insertion is already a node of the cluster");
			return false;
		}
		
		// 0) in teoria qui andrebbe accesa la VM corrispondente al nodo
		
		// 1) STARTING THE NEW NODE
		System.out.println(" ---- starting cassandra process on the new node ("+ip_address+")");
		start = System.currentTimeMillis();
		
		boolean startup_result = start_cassandra(ip_address);
		end = System.currentTimeMillis();
		
		start_time = (end - start) / 1000;
		if( startup_result ){  
			System.out.println(" ---- startup of cassandra process on new node ("+ip_address+") : DONE [ "+start_time+" sec ]"); 
		}
		else{
			System.out.println(" ---- startup of cassandra process on new node ("+ip_address+") : FAILED [ "+start_time+" sec ]");
			return false;
		}
		
		return success;
	}
	
	// ---------------------------------------------------------------------------------
		
	/** ADD NODE with CLEANUPS
	 * 		adds the node with IP=ip_address to the cluster
	 *      and executes a cleanup operation on the nodes of the cluster
	 * @param ip_address : IP Address of the node to add
	 * @param jmx_port : JMX Port number of the node to add
	 * @return true if the new node was addedd successfully, false otherwise
	 */
	public boolean addNodeCleanups(String ip_address, String jmx_port){
		boolean success = true;
		long start,end;
		double start_time;
		
		// check that the node to insert is not already a node of the cluster
		List<String> live_nodes = getLiveNodes(this.contact_point_address, this.jmx_port_number);
		if( live_nodes.contains((String) ip_address) ){
			System.err.println("\n - ERROR : the node selected for insertion is already a node of the cluster");
			return false;
		}
		
		// 0) in teoria qui andrebbe accesa la VM corrispondente al nodo
		
		// 1) STARTING THE NEW NODE
		System.out.println(" ---- starting cassandra process on the new node ("+ip_address+")");
		start = System.currentTimeMillis();
		
		boolean startup_result = start_cassandra(ip_address);
		end = System.currentTimeMillis();
		
		start_time = (end - start) / 1000;
		if( startup_result ){  
			System.out.println(" ---- startup of cassandra process on new node ("+ip_address+") : DONE [ "+start_time+" sec ]"); 
		}
		else{
			System.out.println(" ---- startup of cassandra process on new node ("+ip_address+") : FAILED [ "+start_time+" sec ]");
			return false;
		}
		
		// 2) CLEANUP EXECUTION ON THE OLD LIVE NODES
		live_nodes = getLiveNodes(this.contact_point_address, jmx_port);
		
		if( live_nodes == null ){
			System.err.println(" - ERROR : No Live Nodes in the Cluster!");
			return false;
		}
		
		boolean cleanup_result;
		
		double cleanup_time;
		for(String node_ip : live_nodes){
			// in teoria l'IP del nodo rimosso non dovrebbe essere incluso tra i live_nodes
			// ma nella pratica succede che a causa di ritardi nell'aggiornamento delle liste
			// dei nodi vivi, il nodo rimosso potrebbe figurare ancora come vivo.
			// facciamo dunque il check a mano
			if(!node_ip.equals(ip_address)){  
				System.out.print(" ---- executing cleanup on node "+node_ip+" : ");
				start = System.currentTimeMillis();
				// exec cleanup on the node
				cleanup_result = ClusterScriptExecutor.cleanup(node_ip, this.remote_username, 
						     this.remote_password, this.remote_cassandra_dir);
				end = System.currentTimeMillis();
				cleanup_time = (end - start) / 1000;
				if(cleanup_result){ System.out.println(" DONE [ "+cleanup_time+" sec ]"); }
				else{ 
					System.out.println(" FAILED [ "+cleanup_time+" s ]"); 
					return false; 
				}
			}
		}
		
		return success;
	}
	
	// ---------------------------------------------------------------------------------

	/** REMOVE NODE 
	 * 		removes the node with IP=ip_address from the cluster.
	 * 		ATTENZIONE : il metodo è BLOCCANTE ( a causa della decommission interna )
	 * @param ip_address : IP Address of the node to remove
	 * @param jmx_port : JMX Port number of the node to remove
	 * @return true if the new node was removed successfully, false otherwise
	 */
	public boolean removeNode(String ip_address, String jmx_port){
		boolean success = true;
		long start,end;
		
		// check that the node to remove is actually a node of the cluster
		List<String> live_nodes = getLiveNodes(this.contact_point_address, this.jmx_port_number);
		if( !live_nodes.contains((String) ip_address) ){
			System.err.println("\n - ERROR : the node selected for removal is actually NOT part of the cluster");
			return false;
		}
		
		// faccio partire il thread parallelo per il check dello status del nodo da rimuovere
		// lo status viene monitorato finchè esso non diventa 'DECOMMISSIONED'
		StatusChecker checker = new StatusChecker(ip_address, this.jmx_port_number, "DECOMMISSIONED");
		ExecutorService executor = Executors.newFixedThreadPool(1);
		executor.execute(checker);
		
		double dec_time;
		start = System.currentTimeMillis();
		// 1) DECOMMISSION
		System.out.println(" ---- decommissioning node ("+ip_address+")");
		boolean decommission_result = decommissionNode(ip_address, jmx_port);
		end = System.currentTimeMillis();
		dec_time = (end-start)/1000;
		// aspetto qualche secondo bonus prima di killare il cassandra process
		try { Thread.sleep(4000); } 
		catch (InterruptedException e) {}
		
		if( decommission_result ){  System.out.println(" ---- decommission of node ("+ip_address+") : DONE [ "+dec_time+" sec ]"); }
		else{
			System.out.println(" ---- decommission of node ("+ip_address+") : FAILED [ "+dec_time+" sec ]");
			return false;
		}
		
		// 2) KILL CASSANDRA PROCESS ON THE NODE TO REMOVE
		System.out.print(" ---- killing cassandra process on node ("+ip_address+") : ");
		boolean kill_result = ClusterScriptExecutor.cassandra_process_killer( ip_address, 
				this.remote_username, this.remote_password);
		if( kill_result ){  System.out.println("DONE"); }
		else{
			System.out.println("FAILED");
			return false;
		}
		
		
		// 3) REMOVE OLD DATA FROM THE NODE TO REMOVE
		System.out.print(" ---- removing old data from the node ("+ip_address+") : ");
		boolean removal_result = ClusterScriptExecutor.old_data_removal( ip_address, 
				this.remote_username, this.remote_password, this.remote_cassandra_dir);
		if( removal_result ){  System.out.println("DONE"); }
		else{	
			System.out.println("FAILED");
			return false;
		}
		
		
		// QUI IN TEORIA ANDREBBE SPENTA LA VM DEL NODO RIMOSSO [sennò la pago inutilmente]
		
		return success;
	}
	
	// ---------------------------------------------------------------------------------
	
	
	/** cleanup_all_nodes()
	 * 		triggers a remote execution of a cleanup operation on all the cassandra nodes of the clustr
	 * 
	 * @return true if all cleanups executes successfully, false otherwise.
	 */
	public boolean cleanup_all_nodes(){
		List<String> live_nodes = getLiveNodes(this.contact_point_address, this.jmx_port_number);
		
		if( live_nodes == null ){
			System.err.println(" - ERROR : No Live Nodes in the Cluster!");
			return false;
		}
		System.out.println(" - Cleaning up all nodes of the cluster");
		boolean cleanup_result;
		long start,end;
		double cleanup_time;
		for(String node_ip : live_nodes){

			System.out.print(" ---- executing cleanup on node "+node_ip+" : ");
			start = System.currentTimeMillis();
			// exec cleanup on the node
			cleanup_result = ClusterScriptExecutor.cleanup(node_ip, this.remote_username, 
					     this.remote_password, this.remote_cassandra_dir);
			end = System.currentTimeMillis();
			cleanup_time = (end - start) / 1000;
			if(cleanup_result){ System.out.println(" DONE [ "+cleanup_time+" sec ]"); }
			else{ 
				System.out.println(" FAILED [ "+cleanup_time+" s ]"); 
				return false; 
			}
		}
		return true;
	}
	
	public boolean cleanup_all_nodes_parallel(){
		List<String> live_nodes = getLiveNodes(this.contact_point_address, this.jmx_port_number);
		
		if( live_nodes == null ){
			System.err.println(" - ERROR : No Live Nodes in the Cluster!");
			return false;
		}
		
		System.out.println(" - Cleaning up all nodes of the cluster IN PARALLEL");
		boolean cleanup_result = true;;
		long start,end;
		double cleanup_time;
		int ended = live_nodes.size();
		CountDownLatch latch = new CountDownLatch(live_nodes.size());
		 ExecutorService executor = Executors.newFixedThreadPool(live_nodes.size());
		for(String node_ip : live_nodes){	
			executor.execute(new Runnable() {
				@Override
				public void run() {
					System.out.println(" ---- executing cleanup on node "+node_ip);
					long start = System.currentTimeMillis();
					// exec cleanup on the node
					boolean cleanup_result = ClusterScriptExecutor.cleanup(node_ip, remote_username, 
							     remote_password, remote_cassandra_dir);
					long end = System.currentTimeMillis();
					double cleanup_time = (end - start) / 1000;
					if(cleanup_result){ 
						System.out.println(" ---- cleanup on node "+node_ip+" : DONE [ "+cleanup_time+" sec ]"); 
					}
					else{ 
						System.out.println(" ---- cleanup on node "+node_ip+" : FAILED [ "+cleanup_time+" s ]"); 
						cleanup_result = false;
					}
					latch.countDown();
				}
			});
		}
		
		try {
			latch.await();
			executor.shutdown();
		} catch (InterruptedException e) {}
		
		return cleanup_result;
	}
	
	
	/** check_is_normal
 	 * 		returns true if the cassandra node @ IP address 'ip_address' is in NORMAL state, false otherwise
	 * @param ip_address : String representing the IP address of the node to check
	 * @return true if the node is in state NORMAL, false otherwise
	 */
	private boolean check_is_normal(String ip_address) {
		boolean result = true;
		// creazione jmx manager
		JMXConnectionManager aux_jmxcm = new JMXConnectionManager(ip_address, this.jmx_port_number);
		
		// connection 
		MBeanServerConnection remote = null;
		try { remote = aux_jmxcm.connect(); } 
		catch (Exception e) { result = false; }
		
		String state  = aux_jmxcm.getOperationMode(remote);
		if(state.equals("NORMAL")){result = true;}
		else{result = false;}
		
		// closing the jmx connection
		aux_jmxcm.disconnect();
		return result;
	}
	
	
	/** kill_cassandra_process
	 * 		kills the cassandra process that is currently running on the node 'ip_address'
	 * 
	 * @param ip_address : IP address of the node where the cassandra process has to be killed
	 * @return true if the killing of cassandra was successfull, false otherwise
	 */
	private boolean kill_cassandra_process(String ip_address){
		boolean kill_result = ClusterScriptExecutor.cassandra_process_killer(ip_address, this.remote_username, 
				     this.remote_password);
		return kill_result;
	}
	
	
	/** old_data_removal
	 * 		removes the content of <cassandra-dir>/data directory form the node 'ip_address'
	 * 
	 * @param ip_address : IP Address of the node where the content of data dir has to be removed
	 * @return true if the removal process was successfull, false otherwise.
	 */
	private boolean old_data_removal(String ip_address){
		boolean success = true;
		boolean removal_result = ClusterScriptExecutor.old_data_removal( ip_address, 
				this.remote_username, this.remote_password, this.remote_cassandra_dir);
		if( !removal_result ){ success = false; }
		return success;
	}

	
	/** cleanup
	 * 		remotely invokes the cleanup on the node with IP:ip_addr
	 *
	 * @param ip_addr  : IP address of the node where the cleanup has to be executed
	 * @param jmx_port : JMX Port number of the node where the cleanup has to be executed
	 * @return true, if the cleanup on the node was successfull, false otherwise
	 */
	private boolean cleanup(String ip_address){
		return ClusterScriptExecutor.cleanup(ip_address, this.remote_username, 
					     this.remote_password, this.remote_cassandra_dir);
	}
	
	
	/** start_cassandra
	 * 		remotely invokes the startup of the cassandra process the node with IP:ip_addr
	 *
	 * @param ip_addr  : IP address of the node where cassandra has to be started
	 * @param jmx_port : JMX Port number of the node where  cassandra has to be started
	 * @return true, if the startup of cassandra on the node was successfull, false otherwise
	 */
	private boolean start_cassandra(String ip_address){
		boolean success = true;
		
		// faccio partire il thread parallelo per il check dello status del nuovo nodo
		StatusChecker checker = new StatusChecker(ip_address, this.jmx_port_number, "NORMAL");
		ExecutorService executor = Executors.newFixedThreadPool(1);
		executor.execute(checker);
		
		// la start_cassandra NON è bloccante
		boolean start = ClusterScriptExecutor.start_cassandra( ip_address, 
				this.remote_username, this.remote_password, this.remote_cassandra_dir);
		
		if( !start ){
			System.err.println(" - ERROR : failed to remotely start the cassandra process");
			success = false;
		}
		else{
			
			// siccome ClusterScriptExecutor.start_cassandra non è bloccante, devo aspettare 
			// finche il nuovo nodo non diventa normal
			// qui aspettare finche stato nuovo nodo non diventa NORMAL
			while( !check_is_normal(ip_address) ){
				try { Thread.sleep(1000); } 
				catch (InterruptedException e) {}
			}
			
			// aspetto qualche secondo bonus prima di ritornare
			try { Thread.sleep(4000); } 
			catch (InterruptedException e) {}
			
		}
		return success;
	}

	
	/** decommissionNode
	 * 		remotely invokes the decommission of the node with IP:ip_addr
	 *
	 * @param ip_addr  : IP address of the node to decommission
	 * @param jmx_port : JMX Port number of the node to decommission
	 * @return true, if the decommission process on the node was successfull, false otherwise
	 */
	private boolean decommissionNode(String ip_addr, String jmx_port){
		boolean success = true;
		
		// creazione jmx manager
		JMXConnectionManager aux_jmxcm = new JMXConnectionManager(ip_addr, jmx_port);
		
		// connection 
		MBeanServerConnection remote = null;
		try { remote = aux_jmxcm.connect(); } 
		catch (Exception e) { success = false; }
		
		// decommission [ QUESTA CHIAMATA E' BLOCCANTE ]
		boolean decomm_result = aux_jmxcm.decommission(remote);
		if(!decomm_result){ success = false; }
		
		// check whether the state of the node is actually DECOMMISSIONED
		String node_state = aux_jmxcm.getOperationMode(remote);
		if( !node_state.equalsIgnoreCase("DECOMMISSIONED") ){ 
			System.err.println(" - decommission returned successfully but the node state is not DECOMMISSIONED ["+node_state+"]");
			success = false; 
		}
		
		// closing the jmx connection
		aux_jmxcm.disconnect();
		
		return success;
	}
	
	
	/** drainNode
	 * 		remotely invokes the drain of the node with IP:ip_addr
	 *
	 * @param ip_addr  : IP address of the node to drain
	 * @param jmx_port : JMX Port number of the node to drain
	 * @return true, if the drain process on the node was successfull, false otherwise
	 */
	private boolean drainNode(String ip_addr, String jmx_port){
		boolean success = true;
		
		// creazione jmx manager
		JMXConnectionManager aux_jmxcm = new JMXConnectionManager(ip_addr, jmx_port);
		
		// connection 
		MBeanServerConnection remote = null;
		try { remote = aux_jmxcm.connect(); } 
		catch (Exception e) { success = false; }
		
		// decommission [ QUESTA CHIAMATA E' BLOCCANTE ]
		boolean decomm_result = aux_jmxcm.drain(remote);
		if(!decomm_result){ success = false; }
		
		// closing the jmx connection
		aux_jmxcm.disconnect();
		
		return success;
	}
	
	
	/** getLiveNodes
	 * 		returns a list of Strings with IP addresses of currently live nodes
	 * 
	 * @param ip_addr  : IP address of the node to decommission
	 * @param jmx_port : JMX Port number of the node to decommission
	 * @return a List of Strings containing IP addresses of currently live nodes; null if it was
	 * not possible to retrieve the list.
	 */
	private List<String> getLiveNodes(String ip_addr, String jmx_port){
		List<String> live_nodes = null;
		
		// creazione jmx manager
		JMXConnectionManager aux_jmxcm = new JMXConnectionManager(ip_addr, jmx_port);
		
		// connection 
		MBeanServerConnection remote = null;
		try { remote = aux_jmxcm.connect(); } 
		catch (Exception e) { return live_nodes; }
		
		// get live nodes
		live_nodes = aux_jmxcm.getLiveNodes(remote);
		
		return live_nodes;
		
	}
	
	
	
	/** STOP GOSSIP 
	 * 		stops the gossip service on the node with IP=ip_address 
	 * @param ip_address : IP Address of the node 
	 * @param jmx_port : JMX Port number of the node 
	 * @return true if the gossip was stopped successfully, false otherwise
	 */
	public boolean stopGossip(String ip_address, String jmx_port){
		boolean success = true;
		JMXConnectionManager aux_jmxcm = new JMXConnectionManager(ip_address, jmx_port);
		MBeanServerConnection remote = null;
		try { remote = aux_jmxcm.connect(); } 
		catch (Exception e) { success = false; }
	
		boolean stopgres = aux_jmxcm.stopGossip(remote);
		if(!stopgres){ success = false; }
		
		aux_jmxcm.disconnect();
		
		return success;
	}
	

	
	/** START GOSSIP 
	 * 		starts the gossip service on the node with IP=ip_address 
	 * @param ip_address : IP Address of the node 
	 * @param jmx_port : JMX Port number of the node 
	 * @return true if the gossip was started successfully, false otherwise
	*/
	public boolean startGossip(String ip_address, String jmx_port){
		boolean success = true;
		JMXConnectionManager aux_jmxcm = new JMXConnectionManager(ip_address, jmx_port);
		MBeanServerConnection remote = null;
		try { remote = aux_jmxcm.connect(); } 
		catch (Exception e) { success = false; }
		
		boolean stopgres = aux_jmxcm.startGossip(remote);
		if(!stopgres){ success = false; }
		
		aux_jmxcm.disconnect();
		return success;
	}
	

    // ---------------------------------------------------------------------------------
	
	
	/** GET CONTACT POINT ADDRESS
	 * @return string, the contact point ip address
	 */
	public String getContactPointAddress(){
		return this.contact_point_address;
	}
	
	
	/** GET JMX CONNECTION MANAGER
	 * @return the JMXConnectionManager of this ConfigurationManager
	 */
	public JMXConnectionManager getJmxConnectionManager() {
		return this.jmx_connection_manager;
	}
	
	// ---------------------------------------------------------------------------------

	// FAKE ADD AND REMOVE JUST TO TEST THE AUTOSCALER
	public boolean fake_addNode(String ip_address, String jmx_port){
		boolean result = true;
		String addr_node_to_add = ip_address+":"+jmx_port;
		System.out.print("         [ConfigurationManager] - FAKE ADD NODE "+addr_node_to_add+" : ");
		try{Thread.sleep(4000);}
		catch(Exception e){}
		System.out.println("DONE");
		return result;
	}
	public boolean fake_removeNode(String ip_address, String jmx_port){
		boolean result = true;
		String addr_node_to_remove = ip_address+":"+jmx_port;
		System.out.print("         [ConfigurationManager] - FAKE REMOVE NODE "+addr_node_to_remove+" : ");
		try{Thread.sleep(2000);}
		catch(Exception e){}
		System.out.println("DONE");
		return result;
	}
	
	// ---------------------------------------------------------------------------------
	
	//  main di prova
    public static void main( String[] args ) throws MalformedObjectNameException, InstanceNotFoundException, MBeanException, ReflectionException, IOException{
    	System.out.println("\n *************************");
    	System.out.println(" *    Cluster Manager    *");
        System.out.println(" *************************\n");
        
        String properties_path = "resources/properties/propertiesCM.properties";
        
        ConfigurationManager confManager = new ConfigurationManager(properties_path);
       
        ZonedDateTime start = ZonedDateTime.now();
        System.out.println(" - START @ "+start.toString()+"\n");       
        
        //  confManager.cleanup_all_nodes();
        
        
        //confManager.cleanup_all_nodes_parallel();
        
        /*
        // AGGIUNTA NODO 
        String ip_new_node = "192.168.1.34";
        System.out.println(" - adding new cassandra node on "+ip_new_node);
        boolean resStopGossip = confManager.addNode(ip_new_node, "7199");
        if(resStopGossip){ System.out.println(" - node insertion : OK"); }
        else{ System.out.println(" - node insertion : FAILED"); }
     
        
       */
        // RIMOZIONE NODO 
      	String ip_to_remove = "192.168.1.34";
		System.out.println(" - invoked removal of node "+ip_to_remove);
		boolean resDecom = confManager.removeNode(ip_to_remove, "7199");
		if(resDecom){ System.out.println(" - removing node : OK"); }
		else{ System.out.println(" - removing node : FAILED"); }
		
        
        ZonedDateTime end = ZonedDateTime.now();
        
        System.out.println("\n - END @ "+end.toString());
        
        Duration duration = Duration.between(start,end);
        System.out.println(" - Elapsed Time : " + duration.toString().substring(2));
        System.exit(0);
    }
}