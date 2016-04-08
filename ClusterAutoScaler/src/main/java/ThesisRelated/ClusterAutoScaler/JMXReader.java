package ThesisRelated.ClusterAutoScaler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class JMXReader {
	
	private String address_invocation;
	private String jmx_port_number;
	private JMXServiceURL target;
	private JMXConnector connector;
	
	public JMXReader(String address, String JMX_port){
		try{
	    	this.address_invocation = address;
	    	this.jmx_port_number = JMX_port;
	    	String serviceURL = "service:jmx:rmi:///jndi/rmi://"+this.address_invocation+":"+this.jmx_port_number+"/jmxrmi";
	        this.target = new JMXServiceURL(serviceURL);
	        
	    }
	    catch(Exception e){
	        System.err.println(" ERROR in the JMXReader");
	        System.exit(0);
	    }
	}
	
	public MBeanServerConnection connect() throws Exception {
		MBeanServerConnection remote=null;
		
		Map<String, String[]> authenticationInfo = new HashMap<String, String[]>();     
	    this.connector  = JMXConnectorFactory.connect(this.target, authenticationInfo);
	    remote = this.connector.getMBeanServerConnection();  
		
		return remote;
	}
	
	public void disconnect(){
		try {
			this.connector.close();
		} catch (IOException e) {
			System.err.println("error closing the jmx connection");
		}
	}
	
	// ----------------------------------------------------------------------------------------------

	
    // dovrebbe ritornare un valore tra
    // - "STARTING" , "NORMAL", "JOINING", "LEAVING", "DECOMMISSIONED", "MOVING", "DRAINING", "DRAINED"
    // - "ERROR"  [se qualcosa non va. definito da me]
	public String getOperationMode(MBeanServerConnection remote){
    	String mode = "ERROR";
    	String objname = "org.apache.cassandra.db:type=StorageService";
        try {
			ObjectName bean = new ObjectName(objname);	
			 mode = (String) remote.getAttribute(bean, "OperationMode");
		}
        catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException |
        	   MBeanException | ReflectionException	| IOException e) {} 
        
    	return mode;
    }
	
	// -----------------------------------------------------------------------------------------
    
    public boolean hasJoined(MBeanServerConnection remote){
    	boolean joined = false;
    	String objname = "org.apache.cassandra.db:type=StorageService";
        try {
			ObjectName bean = new ObjectName(objname);	
			joined = (Boolean) remote.getAttribute(bean, "Joined");
		} 
        catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException |
         	   MBeanException | ReflectionException	| IOException e) {} 
    	
        return joined;
    }
    
    // ---------------------------------------------------------------------------------------
    
    public boolean isStarting(MBeanServerConnection remote){
    	boolean starting = false;
    	String objname = "org.apache.cassandra.db:type=StorageService";
        try {
			ObjectName bean = new ObjectName(objname);	
			starting = (Boolean) remote.getAttribute(bean, "Starting");
		}
        catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException |
         	   MBeanException | ReflectionException	| IOException e) {} 
        
    	return starting;
    }
    
    //-----------------------------------------------------------------------------------------------

    public String getLoad(MBeanServerConnection remote){
    	String load = null;
    	String objname = "org.apache.cassandra.db:type=StorageService";
        try {
			ObjectName bean = new ObjectName(objname);	
			load = (String) remote.getAttribute(bean, "LoadString");
		}
        catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException |
         	   MBeanException | ReflectionException	| IOException e) {} 
        
    	return load;
    }
    
    // -----------------------------------------------------------------------------------------------
    
    @SuppressWarnings("unchecked")
	public List<String> getLiveNodes(MBeanServerConnection remote){
    	List<String> live_nodes = null;
    	String objname = "org.apache.cassandra.db:type=StorageService";
        try {
			ObjectName bean = new ObjectName(objname);	
			live_nodes = (List<String>) remote.getAttribute(bean, "LiveNodes");
		}
        catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException |
         	   MBeanException | ReflectionException	| IOException e) {} 
        
    	return live_nodes;
    }
    
    // ------------------------------------------------------------------------------------------
    
    @SuppressWarnings("unchecked")
	public int getNodesNumber(MBeanServerConnection remote){
    	List<String> live_nodes = null;
    	String objname = "org.apache.cassandra.db:type=StorageService";
        try {
			ObjectName bean = new ObjectName(objname);	
			live_nodes = (List<String>) remote.getAttribute(bean, "LiveNodes");
		}
        catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException |
         	   MBeanException | ReflectionException	| IOException e) {} 
        
    	return live_nodes.size();
    }
    
    // -----------------------------------------------------------------------------

	
	public double getCPULevel(MBeanServerConnection connection) {
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
	
	public double getThroughputLevel(MBeanServerConnection connection){
	
		double throughput_total=0;

	  		double countRD = 0;
	  		double countWR = 0;
  			
  			double readCountstart = getReadCount(connection);
  			long startcountrd = System.currentTimeMillis();
  						
  			double writeCountstart = getWriteCount(connection);
  			long startcountwr = System.currentTimeMillis();
		
			try{ Thread.sleep(1000); }
			catch(Exception e){}
  						
  			double readCountend = getReadCount(connection);
  			long endcountrd = System.currentTimeMillis();
  			
  			double writeCountend = getWriteCount(connection);
  			long endcountwr = System.currentTimeMillis();
  			 
  			double elapsed_rd = ((endcountrd - startcountrd)/1000.0);
  			double elapsed_wr = ((endcountwr - startcountwr)/1000.0);
  			if(elapsed_rd==0){elapsed_rd = 1.0;}
  			if(elapsed_wr==0){elapsed_wr = 1.0;}
  			
  			// diciamo che questo epsilon tiene conto del ping time 
  			double epsilon = 0.000;
  			
  			countRD = (readCountend - readCountstart)   / (elapsed_rd - epsilon);
  			countWR = (writeCountend - writeCountstart) / (elapsed_wr - epsilon); 
	
  			throughput_total = countRD + countWR;

		return throughput_total;
	}
	
	
	//---------------------------------------------------------------------------------------------	

	private static long getReadCount(MBeanServerConnection connection) {
		long countRead = 0;
		try{
			//get an instance of the OperatingSystem Mbean
			Object osMbean = connection.getAttribute(new ObjectName("org.apache.cassandra.metrics:type=ClientRequest,scope=Read,name=Latency"),"Count");
			long count = (long) osMbean;
			
			// usually takes a couple of seconds before we get real values
		    if (count == -1.0)      return -1;
		    
		    // returns a percentage value with 1 decimal point precision
		    countRead = count;
		}
		catch(Exception e){ countRead = -1; }
		
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
		    if (count == -1.0)      return -1;
		    
		    // returns a percentage value with 1 decimal point precision
		    countRead = count;
		}
		catch(Exception e){ countRead = -1; }
		
		return countRead;
	}

	//---------------------------------------------------------------------------------------------

}