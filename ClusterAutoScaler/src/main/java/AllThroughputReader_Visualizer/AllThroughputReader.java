package AllThroughputReader_Visualizer;

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



//legge il throughput anche dei nodi spenti , ovvero 0

public class AllThroughputReader {
	
	public static void main(String[] args) throws IOException, InstanceNotFoundException, MalformedObjectNameException, MBeanException, ReflectionException, AttributeNotFoundException, InterruptedException{
				
		//String contact_point_addr = args[0];
    	String contact_point_addr = "192.168.0.169";
		//String contact_point_addr = "127.0.0.1";
    	check_contact_point_address(contact_point_addr);
		

		// String dir_path = args[1] 
		String dir_path = "/home/andrea-muti/Scrivania/metrics_java_ThroughputReader";
		
    
    	System.out.println("\n **** CLUSTER NODES Instant Throughput READER ****\n");   	
       	String port_number = "7199";  
       	
        List<String> all_nodes = new LinkedList<String>();
        all_nodes.add("192.168.0.169");
        all_nodes.add("192.168.1.0");
        all_nodes.add("192.168.1.7");
        all_nodes.add("192.168.1.34");
        all_nodes.add("192.168.1.57");
        all_nodes.add("192.168.1.61");
       
        ThroughputReader[] readers = new ThroughputReader[all_nodes.size()]; 
        int i = 0;
        for( String ip : all_nodes ){
			ThroughputReader reader = new ThroughputReader(ip, port_number,  dir_path);
			readers[i] = reader;
			i++;
		}
        System.out.println("");
        
        ExecutorService executor = null;
        executor = Executors.newFixedThreadPool(readers.length);
        for(int k = 0; k<1000000000; k++){
        	
			for (int j = 0; j < readers.length; j++) {
				ThroughputReader reader = readers[j];
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

	private static long getReadCount(MBeanServerConnection connection) {
		long countRead = 0;
		try{
			//get an instance of the OperatingSystem Mbean
			Object osMbean = connection.getAttribute(new ObjectName("org.apache.cassandra.metrics:type=ClientRequest,scope=Read,name=Latency"),"Count");
			long count = (long) osMbean;
			
			// usually takes a couple of seconds before we get real values
		    if (count == -1.0)      return 0;
		    
		    // returns a percentage value with 1 decimal point precision
		    countRead = count;
		}
		catch(Exception e){ countRead = 0; }
		if(countRead<0){countRead=0;}
		return countRead;
	}
	
//---------------------------------------------------------------------------------------------	

	private static long getWriteCount(MBeanServerConnection connection) {
		long countRead = 0;
		try{
			//get an instance of the OperatingSystem Mbean
			Object osMbean = connection.getAttribute(new ObjectName("org.apache.cassandra.metrics:type=ClientRequest,scope=Write,name=Latency"),"Count");
			long count = (long) osMbean;
			
			// usually takes a couple of seconds before we get real values
		    if (count == -1.0)      return 0;
		    
		    // returns a percentage value with 1 decimal point precision
		    countRead = count;
		}
		catch(Exception e){ countRead = 0; }
		if(countRead<0){countRead=0;}
		return countRead;
	}

//---------------------------------------------------------------------------------------------
	
	static class ThroughputReader  implements Runnable{
		   private String ip_address;
		   private String port_number;
		   int samples_count;
		   int sampling_interval_msec;
		   BufferedWriter writer;
		   
		   ThroughputReader(String ip, String port,  String dir_path){
		       this.ip_address = ip;
		       this.port_number = port;
			   String file_name = dir_path+"/throughput_"+this.ip_address+".txt";
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
					e2.printStackTrace();
				}
		  		
		  		JMXConnector jmxc = null;

		  		double countRD = 0;
		  		double countWR = 0;
		  		MBeanServerConnection connection = null;
		  	
	  			try{
	  				jmxc = JMXConnectorFactory.connect(url, null);
		  			jmxc.connect();
	  				connection = jmxc.getMBeanServerConnection();
	  				
	  				// è vivo
	  				
	  				long startcountrd = System.currentTimeMillis();
		  			double readCountstart = getReadCount(connection);
		  			long startcountwr = System.currentTimeMillis();
		  			double writeCountstart = getWriteCount(connection);
		  			Thread.sleep(1000);
		  			double readCountend = getReadCount(connection);
		  			long endcountrd = System.currentTimeMillis();
		  			double writeCountend = getWriteCount(connection);
		  			long endcountwr = System.currentTimeMillis();
		  			 
		  			countRD = (readCountend - readCountstart) / ((endcountrd - startcountrd)/1000);
		  			countWR = (writeCountend - writeCountstart) / ((endcountwr - startcountwr)/1000); 
		  		  	
		  			if(countRD<0){countRD=0;}
		  			if(countWR<0){countWR=0;}
		  			
		  			double throughput_total = countRD + countWR;
		  			
		  			try {
		  				String content = System.currentTimeMillis() + " " + throughput_total;
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
