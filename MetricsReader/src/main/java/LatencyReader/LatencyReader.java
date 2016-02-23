package LatencyReader;

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

import ThesisRelated.MetricsReader.CPUReader.JMXReader;

public class LatencyReader {
	
	public static void main(String[] args) throws IOException, InstanceNotFoundException, MalformedObjectNameException, MBeanException, ReflectionException, AttributeNotFoundException, InterruptedException{
				
		//String contact_point_addr = args[0];
    	String contact_point_addr = "192.168.0.169";
		//String contact_point_addr = "127.0.0.1";
    	check_contact_point_address(contact_point_addr);
		
    	// int samplesCount = args[1] <- parse int;
		int samplesCount = 3000;
		
		// int sampling_interval_msec = args[2] <-- parse int
		int sampling_interval_msec = 1000;

		// String dir_path = args[3] 
		String dir_path = "/home/andrea-muti/Scrivania/latency_metrics_java_charts";
		
		
    	String jmx_port = "7199";
    	//String jmx_port = "7201";
    	
    	// String operation = "args[4]
    	String operation = "Read"; // oppure "Write"
    	
    	System.out.println("\n **** CLUSTER NODES Latency READER ****\n");
    	
                   
        List<String> live_nodes = getNodesAddresses(contact_point_addr, jmx_port);
        
        if( live_nodes == null ){
        	System.err.println(" - ERROR : failed to get Live Nodes");
			System.exit(-1);
        }
        
        int n_nodes = live_nodes.size();
             
        System.out.println(" - there are "+n_nodes+" Live Nodes in the Cluster\n");
		
		String port_number = "7199";  // se non gestisco una possibile esecuzione in locale, questo funziona solo distributed		
		
		for( String ip : live_nodes ){
			LatencyReaderThread reader = new LatencyReaderThread(ip, port_number, samplesCount, sampling_interval_msec, dir_path, operation);
			reader.start();
		}

	}
	
//---------------------------------------------------------------------------------------------	

	private static long getOperationCount(MBeanServerConnection connection, String operation) {
		long countOp = 0;
		try{
			//get an instance of the OperatingSystem Mbean
			Object osMbean = connection.getAttribute(new ObjectName("org.apache.cassandra.metrics:type=ClientRequest,scope="+operation+",name=Latency"),"Count");
			long count = (long) osMbean;
			
			// usually takes a couple of seconds bfore we get real values
		    if (count == -1.0)      return -1;
		    
		    // returns a percentage value with 1 decimal point precision
		    countOp = count;
		}
		catch(Exception e){ countOp = -1; }
		return countOp;
	}

//---------------------------------------------------------------------------------------------	

		private static long getTotalOperationLatency(MBeanServerConnection connection, String operation) {
			long totLatency = 0;
			try{
				//get an instance of the OperatingSystem Mbean
				Object osMbean = connection.getAttribute(new ObjectName("org.apache.cassandra.metrics:type=ClientRequest,scope="+operation+",name=TotalLatency"),"Count");
				long lat = (long) osMbean;
				
				// usually takes a couple of seconds before we get real values
			    if (lat == -1.0)      return -1;
			    
			    // returns a percentage value with 1 decimal point precision
			    totLatency = lat;
			}
			catch(Exception e){ totLatency = -1; }
			return totLatency;
		}
	
	
//---------------------------------------------------------------------------------------------
	
	static class LatencyReaderThread implements Runnable {
		   private Thread t;
		   private String ip_address;
		   private String port_number;
		   int samples_count;
		   int sampling_interval_msec;
		   private String operation;
		   BufferedWriter writer;
		   
		   LatencyReaderThread(String ip, String port, int samplesCount, int sampling_interval, String dir_path, String operation){
		       this.ip_address = ip;
		       this.port_number = port;
		       this.samples_count = samplesCount;
		       this.sampling_interval_msec = sampling_interval;
		       this.operation = operation;
		      
			   String file_name = dir_path+"/latency_"+operation+"_"+this.ip_address+".txt";
				
				try {
					this.writer = new BufferedWriter(new FileWriter(file_name, true));		
				} catch (IOException e) {
					System.err.println("Error in opening: "+file_name);
				}	
		   }
		   
		   public void run() {
		      try {
		    	  
		    	// create jmx connection with mules jmx agent
		  		String serviceURL = "service:jmx:rmi:///jndi/rmi://"+this.ip_address+":"+this.port_number+"/jmxrmi"; // funziona anche questo
		  		//String serviceURL = "service:jmx:rmi://"+ip_address+":"+port_number+"/jndi/rmi://"+ip_address+":"+port_number+"/jmxrmi";
		  		JMXServiceURL url = new JMXServiceURL(serviceURL);
		  		
		  		JMXConnector jmxc = null;
		  		
		  		try{
		  			jmxc = JMXConnectorFactory.connect(url, null);
		  			jmxc.connect();
		  		}
		  		catch(Exception e){
		  			System.err.println("ERROR : There are connection errors in establishing the connnection with the node \n[ "+e.getMessage()+" ]");
		  		}

		  		double average_latency = 0;
		  		
		  		MBeanServerConnection connection = jmxc.getMBeanServerConnection();
		  	
		  		// initial time stamp
		  		final double start = System.nanoTime();
		  		
		  		//create a loop to get values every second (optional)
		  		for (int i = 0; i < this.samples_count ; i++) {

		  			double opCount = getOperationCount(connection, operation);
		  			
		  			double totalOpLatency = getTotalOperationLatency(connection, operation);
		  			
		  			final double end = System.nanoTime();
		  			
		  			average_latency = totalOpLatency / opCount ;
		  			
		  			double elapsed = (double)( (end - start) / 1000000000 );
		  			String elapsed_seconds = String.format( "%.2f", elapsed ).replace(",", ".");
				
		  	
		  			try {
		  				String content = elapsed_seconds + " " + average_latency;
		  				System.out.println(content);
		  				writer.append(content+"\n");	
		  				writer.flush();
		  			}
		  			catch (IOException e) {	
		  				System.err.println("Error in writing the file");
		  			}	
		  			Thread.sleep(sampling_interval_msec); //delay for the sampling_interval_msec
		  			
		  		}// end for

		  		
		     } catch (InterruptedException | IOException e) {
		         System.out.println("Reader Thread for" +  this.ip_address + " interrupted.");
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
