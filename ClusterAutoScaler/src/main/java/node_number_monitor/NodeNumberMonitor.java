package node_number_monitor;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import ThesisRelated.ClusterAutoScaler.JMXReader;

public class NodeNumberMonitor {
	
	
	
	public static void main(String[] args) throws IOException, InstanceNotFoundException, MalformedObjectNameException, MBeanException, ReflectionException, AttributeNotFoundException, InterruptedException{
				
		//String contact_point_addr = args[0];
    	String contact_point_addr = "192.168.0.169";
		//String contact_point_addr = "127.0.0.1";
    	check_contact_point_address(contact_point_addr);
		
		// String dir_path = args[1] 
		String dir_path = "/home/andrea-muti/Scrivania/";		
    
    	System.out.println("\n **** CLUSTER NODE NUMBER MONITOR ****\n");   	
       	String port_number = "7199";      	
       	
       	
       
        
        System.out.println("");
        NumberMonitor monitor = new NumberMonitor(contact_point_addr,port_number,dir_path);
        ExecutorService executor = null;
        executor = Executors.newFixedThreadPool(1);
        System.out.println(" - start monitoring [ results are saved on "+dir_path+"cluster_node_number.txt ]");
        int elapsed_sec = 0;
        int elapsed_min = 0;
        for(int k = 0; k<2000000000; k++){
			executor.execute(monitor);
			Thread.sleep(4000);
			elapsed_sec = elapsed_sec + 4;
			if( elapsed_sec % 60 == 0 ){
				elapsed_min++;
				System.out.println(" - elapsed "+elapsed_min+" minutes [true time]");
			}
        }
    	executor.shutdown();
		
		// Wait until all threads are finish
		while (!executor.isTerminated()) {}
	}
	

//---------------------------------------------------------------------------------------------
	
	static class NumberMonitor  implements Runnable{
		   private String ip_address;
		   private String port_number;
		   private String file_path;
		   public BufferedWriter writer;
		   
		   NumberMonitor(String ip, String port,  String dir_path){
		       this.ip_address = ip;
		       this.port_number = port;
			   this.file_path=dir_path+"cluster_node_number.txt";
				try {
					this.writer = new BufferedWriter(new FileWriter(file_path, true));		
				} catch (IOException e) {
					System.err.println("Error in opening: "+file_path);
				}	
				System.out.println(" - creater number monitor");
				
				Runtime.getRuntime().addShutdownHook(new ShutdownHook(this.writer, this.file_path));
				
		   }
		   
		   public void run() {

		    	// create jmx connection with mules jmx agent
			    String serviceURL = "service:jmx:rmi:///jndi/rmi://"+this.ip_address+":"+this.port_number+"/jmxrmi"; // funziona anche questo
		  		//String serviceURL = "service:jmx:rmi://"+ip_address+":"+port_number+"/jndi/rmi://"+ip_address+":"+port_number+"/jmxrmi";
		  		JMXServiceURL url = null;
				try {
					url = new JMXServiceURL(serviceURL);
				} catch (MalformedURLException e2) {
					e2.printStackTrace();
					System.exit(0);
				}
		  		
		  		JMXConnector jmxc = null;

		  		MBeanServerConnection connection = null;
		  	
	  			try{
	  				jmxc = JMXConnectorFactory.connect(url, null);
		  			jmxc.connect();
	  				connection = jmxc.getMBeanServerConnection();
	  				
	  				JMXReader reader = new JMXReader(serviceURL, port_number);
	  				
	  				
	  				// è vivo		
	  				int number = reader.getNodesNumber(connection);
	  				//System.out.println(" - there are "+number+" nodes");
		  			
		  			try {
		  				String content = System.currentTimeMillis() + " " + number;
		  				writer.append(content+"\n");	
		  				writer.flush();
		  			}
		  			catch (IOException e) {	
		  				System.err.println("Error in writing the file");
		  			}
		  			
	  			}
	  			catch(Exception e){
	  			
	  				// è morto
	  				// se la getMBeanServerConnection ha generato eccezione il nodo e ancora dead
	  				//inserisco fake counter a 0 
	  				try {	
		  				String content = System.currentTimeMillis()+ " " + 0;
		  				writer.append(content+"\n");	
		  				writer.flush();
		  			}
		  			catch (IOException e1) {	
		  				System.err.println("Error in writing the file " );
		  				e1.printStackTrace();
		  			}	
	  			}
	  			Thread.currentThread().interrupt();
		}		
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
		
}
