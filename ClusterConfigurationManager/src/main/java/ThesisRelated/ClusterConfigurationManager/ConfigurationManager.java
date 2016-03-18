package ThesisRelated.ClusterConfigurationManager;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

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
public class ConfigurationManager {
	
	// instance variables
	private String contact_point_address;
	private String jmx_port_number;
	private JMXConnectionManager jmx_connection_manager;
	
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
	}
	
	// ---------------------------------------------------------------------------------
		
	/** ADD NODE [ DA IMPLEMENTARE ]
	 * 		adds the node with IP=ip_address to the cluster
	 * @param ip_address : IP Address of the node to add
	 * @param jmx_port : JMX Port number of the node to add
	 * @return true if the new node was addedd successfully, false otherwise
	 */
	public boolean addNode(String ip_address, String jmx_port){
		boolean success = true;
		System.out.println(" --- adding the new node ("+ip_address+") to the Cluster");
		return success;
	}
	
	/** REMOVE NODE [ DA IMPLEMENTARE ]
	 * 		removes the node with IP=ip_address from the cluster.
	 * 		ATTENZIONE : il metodo è BLOCCANTE ( a causa della decommission interna )
	 * @param ip_address : IP Address of the node to remove
	 * @param jmx_port : JMX Port number of the node to remove
	 * @return true if the new node was removed successfully, false otherwise
	 */
	public boolean removeNode(String ip_address, String jmx_port){
		boolean success = true;
		
		System.out.println(" --- removing the node ("+ip_address+") from the Cluster");
		
		// DECOMMISSION
		boolean decommission_result = decommissionNode(ip_address, jmx_port);
		if(!decommission_result){ success = false; }

		return success;
	}
	
	
	/** decommissionNode
	 * 		remotely invokes the decommission of the node with IP:ip_addr
	 *
	 * @param ip_addr : IP address of the node to decommission
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

	//  main di prova
    public static void main( String[] args ) throws MalformedObjectNameException, InstanceNotFoundException, MBeanException, ReflectionException, IOException{
    	System.out.println("\n *************************");
    	System.out.println(" * Cluster Manager Prova *");
        System.out.println(" *************************\n");
        
        String properties_path = "resources/propertiesCM.properties";
        
        ConfigurationManager confManager = new ConfigurationManager(properties_path);
        
        System.out.print(" - stopping gossip on node 192.168.0.169 : ");
        boolean resStopGossip = confManager.stopGossip("192.168.0.169", "7199");
        if(resStopGossip){ System.out.println(" OK"); }
        else{ System.out.println(" FAILED"); }
        
        System.out.print(" - starting gossip on node 192.168.0.169 : ");
        boolean resStartGossip = confManager.startGossip("192.168.0.169", "7199");
        if(resStartGossip){ System.out.println(" OK"); }
        else{ System.out.println(" FAILED"); }
        	
        
        /* ATTENZIONE AD ESEGUIRE QUESTO METODO
		// prova decommission 
		// ATTENZIONE LA DECOMMISSION E' BLOCCANTE ! 
		javax.swing.JOptionPane.showMessageDialog(null, "press Ok to remove the node from the cluster");
		System.out.println(" - invoked removal of node 192.168.0.169");
		boolean resDecom = confManager.removeNode("192.168.0.169", "7199");
		if(resDecom){ System.out.println(" - removing node : OK"); }
		else{ System.out.println(" - removing node : FAILED"); }
		
		 */
     
        
    }
}
