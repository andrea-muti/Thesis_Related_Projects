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

public class RemoteSigarRetriever_v2 {
	public static void main(String[] args) throws IOException, InstanceNotFoundException, MalformedObjectNameException, MBeanException, ReflectionException, AttributeNotFoundException, InterruptedException{
		
		int samplesCount = 5;
		int sampling_interval_msec = 1000;
		
		// prova in locale
		//String ip_address  = "127.0.0.1";
		//String port_number = "7201";
		
		// prova in remoto sulla vm0 del cluster DIAG
		String ip_address  = "192.168.0.169";
		String port_number = "7199";
		
		// create jmx connection with mules jmx agent
		String serviceURL = "service:jmx:rmi:///jndi/rmi://"+ip_address+":"+port_number+"/jmxrmi"; // funziona anche questo
		//String serviceURL = "service:jmx:rmi://"+ip_address+":"+port_number+"/jndi/rmi://"+ip_address+":"+port_number+"/jmxrmi";
		JMXServiceURL url = new JMXServiceURL(serviceURL);
		
		JMXConnector jmxc = null;
		
		try{
			jmxc = JMXConnectorFactory.connect(url, null);
			jmxc.connect();
		
		}
		catch(Exception e){
			System.err.println("ERROR : There are connection errors in establishing the connnection with the node \n[ "+e.getMessage()+" ]");
			System.exit(-1);
		}

		double tempCPU = 0;
		double tempNonHeapMemory = 0;
		double tempHeapMemory = 0;
		
		
		MBeanServerConnection connection = jmxc.getMBeanServerConnection();
	
		// call the garbage collector before the test using the Memory Mbean
		// connection.invoke(new ObjectName("java.lang:type=Memory"), "gc", null, null);

		// initial time stamp
		final double start = System.nanoTime();
		
		//create a loop to get values every second (optional)
		for (int i = 0; i < samplesCount; i++) {
			
			final double end = System.nanoTime();
			double elapsed = (double)((end - start) / 1000000000);
			String elapsed_seconds = String.format( "%.2f", elapsed ).replace(",", ".");
			
			long used_non_heap_mem = getNonHeapMemoryUsage(connection);
			long used_heap_mem = getHeapMemoryUsage(connection);
			
			double cpuload = getProcessCPULoad(connection);
						
			System.out.println("\n[ "+elapsed_seconds+" ] Non-Heap Used memory: " + " " + used_non_heap_mem + " KB\n"
					+ "[ "+elapsed_seconds+" ] Heap Used Memory: " + used_heap_mem + " KB\n"
					+ "[ "+elapsed_seconds+" ] Used cpu: " + cpuload+" %"); //print memory usage
			
			tempNonHeapMemory = tempNonHeapMemory + used_non_heap_mem;
			tempHeapMemory = tempHeapMemory + used_heap_mem;
			tempCPU = tempCPU + cpuload;
		
			Thread.sleep(sampling_interval_msec); //delay for the sampling_interval_msec
			
		}// end while

	
		String average_cpu_load = String.format( "%.3f", tempCPU / samplesCount ).replace(",", ".");
		String average_non_heap_memory_usage_MB = String.format( "%.3f", (tempNonHeapMemory/(1024) ) / samplesCount ).replace(",", ".");
		String average_heap_memory_usage_MB = String.format( "%.3f", (tempHeapMemory/(1024) ) / samplesCount ).replace(",", ".");
		
		System.out.println("\naverage CPU usage is : "   + average_cpu_load +" %");
		System.out.println("average non-heap memory usage is: " + average_non_heap_memory_usage_MB +" MB");
		System.out.println("average heap memory usage is: " + average_heap_memory_usage_MB +" MB");

	}
	
	private static double getProcessCPULoad(MBeanServerConnection connection) {
		double processCPUload = 0;
		try{
			//get an instance of the OperatingSystem Mbean
			Object osMbean = connection.getAttribute(new ObjectName("java.lang:type=OperatingSystem"),"ProcessCpuLoad");
			double cpuload_value = (double) osMbean;
			
			// usually takes a couple of seconds before we get real values
		    if (cpuload_value == -1.0)      return Double.NaN;
		    
		    // returns a percentage value with 1 decimal point precision
		    processCPUload = ( (double)(cpuload_value * 1000) / 10.0);
		}
		catch(Exception e){ processCPUload = -1; }
		
		String processCPUload_formatted = String.format( "%.3f", processCPUload ).replace(",", ".");
		return Double.parseDouble(processCPUload_formatted) ;
	}

	public static long getNonHeapMemoryUsage(MBeanServerConnection connection) {
		long usedmemKB = 0;
		try{
			Object memoryMbean = connection.getAttribute(new ObjectName("java.lang:type=Memory"), "NonHeapMemoryUsage");
			CompositeData cdmem  = (CompositeData) memoryMbean;
			long usedmem = (long) cdmem.get("used");
			usedmemKB = usedmem/(1024);		
		}
		catch(Exception e){ usedmemKB = -1; }
		return usedmemKB;
	}
	
	public static long getHeapMemoryUsage(MBeanServerConnection connection) {
		long usedmemKB = 0;
		try{
			Object memoryMbean = connection.getAttribute(new ObjectName("java.lang:type=Memory"), "HeapMemoryUsage");
			CompositeData cdmem  = (CompositeData) memoryMbean;
			long usedmem = (long) cdmem.get("used");
			usedmemKB = usedmem/(1024);		
		}
		catch(Exception e){ usedmemKB = -1; }
		return usedmemKB;
	}
	
}
