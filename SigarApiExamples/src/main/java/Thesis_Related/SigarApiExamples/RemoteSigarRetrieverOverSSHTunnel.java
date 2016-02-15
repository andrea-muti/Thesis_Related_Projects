package Thesis_Related.SigarApiExamples;

import java.io.IOException;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import Thesis_Related.SSHManager.SSHTunnelManager;

public class RemoteSigarRetrieverOverSSHTunnel {
	public static void main(String[] args) throws IOException, InstanceNotFoundException, MalformedObjectNameException, MBeanException, ReflectionException, AttributeNotFoundException, InterruptedException{
		
		int samplesCount = 5;
		
		/**
		// local test
		 
	    String ip_address = "127.0.0.1";
		String port_number = "7201";
		 
		String remote_user = "andrea-muti";
    	String remote_pass = "pianoforte";
    	String remote_addr = "localhost";
    	String remote_jmx  = "7199";
		
		**/
		
		// remote test
		
		String ip_address = "192.168.0.169";
		String port_number = "7199";
		
		String remote_user = "muti";
    	String remote_pass = "mUt1";
    	String remote_addr = "vm0";
    	String remote_jmx  = "7199";
    	
    	System.out.println(" - Opening the SSH Tunnel towards the remote host : "+remote_user+"@"+remote_addr+":"+remote_jmx );
       
        SSHTunnelManager sshmanager = new SSHTunnelManager(remote_user, remote_pass, remote_addr, remote_jmx);
        boolean opened = sshmanager.open_tunnel();
        
        System.out.println(" - SSH Tunnel opened? "+opened);
        
        if(!opened){
        	System.err.println(" - ERROR : failed to open the SSL Tunnel");
        	System.exit(-1);
        }
		
        try{ 
        
			System.out.println("\n - Setting up the JMX Connection");
	        
			String serviceURL = "service:jmx:rmi:///jndi/rmi://"+ip_address+":"+port_number+"/jmxrmi"; // funziona anche questo
			//String serviceURL = "service:jmx:rmi://"+ip_address+":"+port_number+"/jndi/rmi://"+ip_address+":"+port_number+"/jmxrmi";
			JMXServiceURL url = new JMXServiceURL(serviceURL);
			JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
			jmxc.connect();
		
			
			//create object instances that will be used to get memory and operating system Mbean objects exposed by JMX; 
			Object memoryMbean = null;
			Object osMbean = null;
			long tempCPU = 0;
			long tempMemory = 0;
			CompositeData cdmem = null;
			double cpuload = 0.0;
			
			MBeanServerConnection connection = jmxc.getMBeanServerConnection();
		
			// call the garbage collector before the test using the Memory Mbean
			connection.invoke(new ObjectName("java.lang:type=Memory"), "gc", null, null);
			System.out.println(" - Garbage Collector called on the remote machine");
				
			//create a loop to get values every second (optional)
			for (int i = 0; i < samplesCount; i++) {
	
				//get an instance of the HeapMemoryUsage Mbean
				memoryMbean = connection.getAttribute(new ObjectName("java.lang:type=Memory"), "HeapMemoryUsage");
				cdmem = (CompositeData) memoryMbean;
						
				//get an instance of the OperatingSystem Mbean
				osMbean = connection.getAttribute(new ObjectName("java.lang:type=OperatingSystem"),"ProcessCpuLoad");
				cpuload = (double) osMbean;
				
				System.out.println("Heap Used memory: " + " " + cdmem.get("used") + " Used cpu: " + cpuload+"\n"); //print memory usage
				tempMemory = tempMemory + Long.parseLong(cdmem.get("used").toString());
				tempCPU = tempCPU + Double.valueOf(cpuload).longValue();
				
				Thread.sleep(1000); //delay for one second
			}
	
			
			System.out.println("average cpu usage is : "   + tempCPU / Long.valueOf(""+samplesCount));
			System.out.println("average heap memory usage is: " + (tempMemory/(1024*1024) ) / samplesCount+" MB");//print average memory usage
			
			System.out.println("\n - closing the JMX connection");
			jmxc.close();
        }
        catch(Exception e){
        	System.err.println(" - ERROR : an error occurred. Closing SSH Tunnel and exit\n   [error message: "+e.getMessage()+"]");
        	sshmanager.close_tunnel();
        	System.exit(-1);
        }
		
		System.out.println(" - closing the SSH Tunnel");
		sshmanager.close_tunnel();
	
	}
}
