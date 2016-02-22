package ThesisRelated.MetricsReader.CPUReader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

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


public class CPUReader {
	
	public static void main(String[] args) throws IOException, InstanceNotFoundException, MalformedObjectNameException, MBeanException, ReflectionException, AttributeNotFoundException, InterruptedException{
				
		//String contact_point_addr = args[0];
    	String contact_point_addr = "192.168.0.169";
    	check_contact_point_address(contact_point_addr);
		
    	// int samplesCount = args[1] <- parse int;
		int samplesCount = 8000;
		
		// int sampling_interval_msec = args[2] <-- parse int
		int sampling_interval_msec = 1000;

		// String dir_path = args[3] 
		String dir_path = "/home/andrea-muti/Scrivania/metrics_java_CPUReader";
		
    	String jmx_port = "7199";
    
    	
    	System.out.println("\n **** CLUSTER NODES CPU LOAD READER ****\n");
    	
                   
        List<String> live_nodes = getNodesAddresses(contact_point_addr, jmx_port);
        
        if( live_nodes == null ){
        	System.err.println(" - ERROR : failed to get Live Nodes");
			System.exit(-1);
        }
        
        int n_nodes = live_nodes.size();
             
        System.out.println(" - there are "+n_nodes+" Live Nodes in the Cluster\n");
		
		for( String ip : live_nodes ){
			CPUReaderThread reader = new CPUReaderThread(ip, jmx_port, samplesCount, sampling_interval_msec, dir_path);
			reader.start();
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
		    if (cpuload_value == -1.0)      return Double.NaN;
		    
		    // returns a percentage value with 1 decimal point precision
		    processCPUload = ( (double)(cpuload_value * 1000) / 10.0);
		}
		catch(Exception e){ processCPUload = -1; }
		
		String processCPUload_formatted = String.format( "%.3f", processCPUload ).replace(",", ".");
		return Double.parseDouble(processCPUload_formatted) ;
	}

//---------------------------------------------------------------------------------------------
	
	static class CPUReaderThread implements Runnable {
		   private Thread t;
		   private String ip_address;
		   private String port_number;
		   int samples_count;
		   int sampling_interval_msec;
		   BufferedWriter writer;
		   
		   CPUReaderThread(String ip, String port, int samplesCount, int sampling_interval, String dir_path){
		       this.ip_address = ip;
		       this.port_number = port;
		       this.samples_count = samplesCount;
		       this.sampling_interval_msec = sampling_interval;
		      
			   String file_name = dir_path+"/cpu_utilization_"+this.ip_address+".txt";
				
				try {
					this.writer = new BufferedWriter(new FileWriter(file_name, true));		
				} catch (IOException e) {
					System.err.println("Error in opening: "+file_name);
				}	
		   }
		   
		   public void run() {
			  JMXConnector jmxc = null;
		      try {
		    	  
		    	// create jmx connection with mules jmx agent
		  		String serviceURL = "service:jmx:rmi:///jndi/rmi://"+this.ip_address+":"+this.port_number+"/jmxrmi"; // funziona anche questo
		  		//String serviceURL = "service:jmx:rmi://"+ip_address+":"+port_number+"/jndi/rmi://"+ip_address+":"+port_number+"/jmxrmi";
		  		JMXServiceURL url = new JMXServiceURL(serviceURL);

		  		try{
		  			jmxc = JMXConnectorFactory.connect(url, null);
		  			jmxc.connect();
		  		}
		  		catch(Exception e){
		  			System.err.println("ERROR : There are connection errors in establishing the connnection with the node \n[ "+e.getMessage()+" ]");
		  		}

		  		double tempCPU = 0;
		  		
		  		MBeanServerConnection connection = jmxc.getMBeanServerConnection();
		  	
		  		// initial time stamp
		  		final double start = System.nanoTime();
		  		
		  		//create a loop to get values every second (optional)
		  		for (int i = 0; i < this.samples_count ; i++) {
		  			
		  			final double end = System.nanoTime();
		  			double elapsed = (double)( (end - start) / 1000000000 );
		  			String elapsed_seconds = String.format( "%.2f", elapsed ).replace(",", ".");
		  		
		  			double cpuload = getProcessCPULoad(connection);
		  						
		  			try {
		  				String content = elapsed_seconds + " " + cpuload;
		  				writer.append(content+"\n");	
		  				writer.flush();
		  			}
		  			catch (IOException e) {	
		  				System.err.println("Error in writing the file");
		  			}	
		  				  			
		  			tempCPU = tempCPU + cpuload;
		  		
		  			Thread.sleep(sampling_interval_msec); //delay for the sampling_interval_msec
		  			
		  		}// end for

		  		String average_cpu_load = String.format( "%.3f", tempCPU / this.samples_count  ).replace(",", ".");
		  		
		  		System.out.println("\n - [ " +this.ip_address + " ] average CPU usage during the sampling period : "   + average_cpu_load +" %");
		    
		     } catch (InterruptedException | IOException e) {
		    	 System.out.println("Reader Thread for" +  this.ip_address + " interrupted.");
		         try {
					jmxc.close();
		         } catch (IOException e1) {
					System.err.println("error closing the jmx connection");
		         }
		         
		     }
		      
		     // closing the buffer writer 
		     try { writer.close(); } 
		     catch (IOException e) {
		    	 System.err.println("Error in closing the file");
		  	 } 
		      
		     System.out.println(" - Reader Thread for " +  this.ip_address + " exiting.");
		   }
		   
		   public void start () {
		      System.out.println(" - Starting Reader Thread for " +  this.ip_address );
		      if (t == null){
		         t = new Thread (this);
		         t.start ();
		      }
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
	
//---------------------------------------------------------------------------------------------
	
	private static List<String> getNodesAddresses(String contact_point_addr, String jmx_port){
		JMXReader jmxreader = new JMXReader(contact_point_addr, jmx_port);
        MBeanServerConnection remote = null;
		try {
			remote = jmxreader.connect();
		} catch (IOException e) {
			System.err.println(" - ERROR : There are communication problems when establishing the connection with the Cluster \n"
   		           + "           [ "+e.getMessage()+" ]");
			System.exit(-1);
		}
		catch (SecurityException e) {
			System.err.println(" - ERROR : There are security problems when establishing the connection with the Cluster \n"
	   		           + "           [ "+e.getMessage()+" ]");
			System.exit(-1);
		}
		catch (Exception e) {
			System.err.println(" - ERROR : There are unknown problems when establishing the connection with the Cluster \n"
	   		           + "           [ "+e.getMessage()+" ]");
			System.exit(-1);
		}

        List<String> live_nodes = jmxreader.getLiveNodes(remote);
        
        jmxreader.disconnect();
        
        return live_nodes;
	}
	
}
