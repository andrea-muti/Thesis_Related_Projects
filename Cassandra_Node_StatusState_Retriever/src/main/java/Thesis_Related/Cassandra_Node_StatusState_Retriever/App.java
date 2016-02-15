package Thesis_Related.Cassandra_Node_StatusState_Retriever;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.management.MBeanServerConnection;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;


/**
 *  16/12/2015
 *  RETRIEVER OF THE CASSANDRA NODE STATE
 *  @author andrea-muti
 */
public class App {
    public static void main( String[] args ){
    	
    	// Settaggio del loggin level a ERROR
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.ERROR);
   	    	
    	// Check numero dei parametri in input
        if(args.length<3){
     	   System.err.println("ERROR : arg1 : IP address of the node to check");
     	   System.err.println("        arg2 : JMX Port Number of the node to check");
     	   System.err.println("        arg3 : Y or N depending on wheter the test is LOCAL");
     	   System.err.println("          - Y : Local (i.e. addresses 127.0.0.1 127.0.0.2 127.0.0.3 ...)");
     	   System.err.println("          - N : Not Local (i.e. node addresses are not 127.0.0.X");
     	   System.exit(-1);
        }    	
    	
    	// se eseguito in locale con i vari nodi che hanno indirizzi 127.0.0.1 127.0.0.2 127.0.0.3 ...
    	// bisogna passare sempre l'address 127.0.0.1
    	String node_addr = args[0];
    	//String node_addr = "127.0.0.1";
    	check_node_address(node_addr);
    	
    	String node_jmx_port = args[1];
    	//String node_jmx_port = "7201";
    	check_node_jmx_port(node_jmx_port);
    	
    	String local = args[2];
    	//String local = "Y";
    	check_is_local(local);
    	
    	boolean local_flag = false;
    	if(local.equalsIgnoreCase("Y")){ local_flag=true; }
    	
    	String status = getNodeStatus(node_addr, node_jmx_port, local_flag);
    	String state = getNodeState(node_addr, node_jmx_port, local_flag);
	
		System.out.println(status+"_"+state);
    	
    }
    
    /** CHECK WHETER THE "local" flag passed as argument is valid i.e. Y or N
     * @param local flag : String
     */
    private static void check_is_local(String local) {
		if(!local.equalsIgnoreCase("Y") && !local.equalsIgnoreCase("n")){
			System.err.println(" - ERROR : Malformed Local Flag Argument - Must be 'Y' or 'N' ");
    		System.exit(-1);
		}
	}


	/** CHECK WHETER THE JMX PORT NUMBER IS VALID
     * @param jmx_port_number
     */
    private static void check_node_jmx_port(String node_jmx_port) {
		try{ Integer.parseInt(node_jmx_port);}
		catch(NumberFormatException e){
			System.err.println(" - ERROR : Malformed JMX Port Number of Node - Must be an Integer number");
    		System.exit(-1);
		}
	}

	/** CHECK WHETER THE NODE ADDRESS IS VALID
     * @param node_addr
     */
	private static void check_node_address(String node_addr) {
    	try{
    		InetAddress.getByName(node_addr);
    	}
    	catch(UnknownHostException e){
    		System.err.println(" - ERROR : Malformed IP Address of Node - Must be in the form x.x.x.x");
    		System.exit(-1);
    	}
	}
	
	
	/**
	 * RETRIEVES THE STATUS OF THE NODE
	 * 
	 * @param address String : IP Address of the Node 
	 * @param jmxport String : JMX Port Number of the Node
	 * @param local_flag boolean : true if the execution is in a local environment where all nodes
	 * 	 							have addresses 127.0.0.X
	 * 							   false otherwise
	 * @return status String : Status of the Node [ "LIVE", "DEAD", "UNKNOWN", "ERROR" ]
	 */
	public static String getNodeStatus(String address, String jmxport, boolean local_flag){
		   
		 String status = "UNKNOWN";
		 String address_invocation = address;
		 if(local_flag){ address_invocation = "127.0.0.1"; }
		 try {
	    	 
	    	 JMXReader reader = new JMXReader(address_invocation, jmxport);
	         MBeanServerConnection remote = reader.connect();
	         List<String> live_nodes = reader.getLiveNodes(remote);
	         
	         if( live_nodes.contains(address) ){ status="LIVE"; }
	         else{ status="DEAD";  }
	         
	         reader.disconnect();
	     }
	     catch(Exception e){ status = "ERROR"; }
	     
	     return status;
	}
	
	/**
	 * RETRIEVES THE STATE OF THE NODE
	 * 
	 * @param address String : IP Address of the Node 
	 * @param jmxport String : JMX Port Number of the Node
	 * @param local_flag boolean : - true if the execution is in a local environment where all nodes
	 * 	 					         have addresses 127.0.0.X
	 * 							   - false otherwise
	 * @return state String : Status of the Node - values in [ "STARTING" , "NORMAL", "JOINING", 
	 *                "LEAVING", "DECOMMISSIONED", "MOVING", "DRAINING", "DRAINED", "UNKNOWN", "ERROR" ]
	 */
	public static String getNodeState(String address, String jmxport, boolean local_flag){
		 
		 String state = "UNKNWON";
		 
		 String jmx_port_number=jmxport;
		 String address_invocation = address;
		 if(local_flag){ address_invocation = "127.0.0.1"; }
   
	     try {
	    	 
	    	 JMXReader reader = new JMXReader(address_invocation, jmx_port_number);
	         MBeanServerConnection remote = reader.connect();
	         
	         String op_mode = reader.getOperationMode(remote);
	         
	         boolean isStarting = reader.isStarting(remote);
	         boolean hasJoined = reader.hasJoined(remote);

	         if(op_mode.equalsIgnoreCase("STARTING") || isStarting ){
	        	 state="STARTING";
	         }
	         else if(op_mode.equalsIgnoreCase("JOINING") || !hasJoined ){
	        	 state = "JOINING";
	         }
	         else{
	        	 state = op_mode;
	         }
	         
	         reader.disconnect();
	         
	     }
	     catch(Exception e){ state="ERROR"; }
	     
	     return state;
	}	
    
}
