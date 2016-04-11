package AllCPUReader_Visualizer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;



//legge la cpu anche dei nodi spenti , ovvero 0

public class AllCPUReader {
	
	public static void main(String[] args) throws IOException, InstanceNotFoundException, MalformedObjectNameException, MBeanException, ReflectionException, AttributeNotFoundException, InterruptedException{
				
		//String contact_point_addr = args[0];
    	String contact_point_addr = "192.168.0.169";
		//String contact_point_addr = "127.0.0.1";
    	check_contact_point_address(contact_point_addr);
		

		// String dir_path = args[1] 
		String dir_path = "/home/andrea-muti/Scrivania/metrics_java_CPUReader";
		
    
    	System.out.println("\n **** CLUSTER NODES CPU READER ****\n");   	
       	String port_number = "7199";  
       	
        List<String> all_nodes = new LinkedList<String>();
        all_nodes.add("192.168.0.169");
        all_nodes.add("192.168.1.0");
        all_nodes.add("192.168.1.7");
        all_nodes.add("192.168.1.34");
        all_nodes.add("192.168.1.57");
        all_nodes.add("192.168.1.61");
       
        CpuReader[] readers = new CpuReader[all_nodes.size()]; 
        int i = 0;
        for( String ip : all_nodes ){
			CpuReader reader = new CpuReader(ip, port_number,  dir_path);
			readers[i] = reader;
			i++;
		}
        System.out.println("");
        
        ExecutorService executor = null;
        for(int k = 0; k<1000000000; k++){
        	executor = Executors.newFixedThreadPool(readers.length);
			for (int j = 0; j < readers.length; j++) {
				CpuReader reader = readers[j];
				executor.execute(reader);
			}
			Thread.sleep(1000);
        }
    	executor.shutdown();
		// Wait until all threads are finish
		while (!executor.isTerminated()) {
			
		}
	}
	
	
	
//---------------------------------------------------------------------------------------------	

	private static double getProcessCPULoad(MBeanServerConnection connection) {
		double processCPUload = 0;
		try{
			//get an instance of the OperatingSystem Mbean
			Object osMbean = connection.getAttribute(new ObjectName("java.lang:type=OperatingSystem"),"ProcessCpuLoad");
			double cpuload_value = (double) osMbean;
			
			// usually takes a couple of seconds bfore we get real values
		    if (cpuload_value == -1.0)      return 0;
		    
		    // returns a percentage value with 1 decimal point precision
		    processCPUload = ( (double)(cpuload_value * 1000) / 10.0);
		}
		catch(Exception e){ processCPUload = 0; }
		
		String processCPUload_formatted = String.format( "%.3f", processCPUload ).replace(",", ".");
		return Double.parseDouble(processCPUload_formatted) ;
	}
	
//---------------------------------------------------------------------------------------------
	
	static class CpuReader  implements Runnable{
		   private String ip_address;
		   private String port_number;
		   int samples_count;
		   int sampling_interval_msec;
		   BufferedWriter writer;
		   
		   CpuReader(String ip, String port,  String dir_path){
		       this.ip_address = ip;
		       this.port_number = port;
			   String file_name = dir_path+"/cpu_"+this.ip_address+".txt";
				try {
					this.writer = new BufferedWriter(new FileWriter(file_name, true));		
				} catch (IOException e) {
					System.err.println("Error in opening: "+file_name);
				}	
				System.out.println(" - creater reader for "+this.ip_address);
		   }
		   
		   public void run() {

		    	// create jmx connection with mules jmx agent
			    String serviceURL = "service:jmx:rmi:///jndi/rmi://"+this.ip_address+":"+this.port_number+"/jmxrmi"; // funziona anche questo
		  		//String serviceURL = "service:jmx:rmi://"+ip_address+":"+port_number+"/jndi/rmi://"+ip_address+":"+port_number+"/jmxrmi";
		  		JMXServiceURL url = null;
				try {
					url = new JMXServiceURL(serviceURL);
				} catch (MalformedURLException e2) {
					//e2.printStackTrace();
				}
		  		
		  		JMXConnector jmxc = null;

		  		
		  		MBeanServerConnection connection = null;
		  	
	  			try{
	  				jmxc = JMXConnectorFactory.connect(url, null);
		  			jmxc.connect();
	  				connection = jmxc.getMBeanServerConnection();
	  				
	  				// è vivo
	  			
		  			double cpu_level_sample1 = getProcessCPULoad(connection);
		  			

		  			
		  			double cpu_level = ( cpu_level_sample1 ) ;
		  			
		  			try {
		  				String content = System.currentTimeMillis() + " " + cpu_level;
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
		  				String content = System.currentTimeMillis()+ " " + 0.0;
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
