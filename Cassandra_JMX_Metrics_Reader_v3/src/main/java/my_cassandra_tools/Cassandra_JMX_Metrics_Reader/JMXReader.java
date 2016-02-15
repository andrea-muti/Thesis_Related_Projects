package my_cassandra_tools.Cassandra_JMX_Metrics_Reader;

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
	        System.out.println("[e]"+e.getClass().getName()+" "+e.getMessage());
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
    
    
    public ClientRequestMetricsObject getClientRequestLatencyMetrics(MBeanServerConnection remote, String scope){
    	
    	ClientRequestMetricsObject client_req_metrics =  null;
    	double mean_lat = 0;
    	double percentile_95_lat = 0;
    	String duration_unit = "";
    	double one_min_throughput = 0;
    	String rate_unit = "";
    	
    	String objname = "org.apache.cassandra.metrics:type=ClientRequest,scope="+scope+",name=Latency";
        try {
			ObjectName bean = new ObjectName(objname);	
			
			try{
				
				mean_lat = (double) remote.getAttribute(bean, "Mean");
				if(Double.isNaN(mean_lat)){mean_lat=-1;}
			}
			catch(NumberFormatException e){mean_lat=-1;}
			
			percentile_95_lat = (double) remote.getAttribute(bean, "95thPercentile");
			duration_unit = (String) remote.getAttribute(bean, "DurationUnit");
			
			one_min_throughput = (double) remote.getAttribute(bean, "OneMinuteRate");
			rate_unit = (String) remote.getAttribute(bean, "RateUnit");
		
		}
        catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException |
         	   MBeanException | ReflectionException	| IOException e) {}
        
        client_req_metrics =  new ClientRequestMetricsObject(mean_lat, percentile_95_lat, 
	  			duration_unit, one_min_throughput, rate_unit );

        return client_req_metrics;
    }
    
    public NodeMetricsObject getNodeLatencyMetrics(MBeanServerConnection remote, String name){
    	
    	NodeMetricsObject node_metrics =  null;
    	double mean_lat = 0;
    	double percentile_95_lat = 0;
    	String duration_unit = "";
    	double one_min_throughput = 0;
    	String rate_unit = "";
    	
    	String objname = "org.apache.cassandra.metrics:type=ColumnFamily,name=" + name;
        try {
			ObjectName bean = new ObjectName(objname);	
			
			try{
				
				mean_lat = (double) remote.getAttribute(bean, "Mean");
				if(Double.isNaN(mean_lat)){mean_lat=-1;}
			}
			catch(NumberFormatException e){mean_lat=-1;}
			
			percentile_95_lat = (double) remote.getAttribute(bean, "95thPercentile");
			duration_unit = (String) remote.getAttribute(bean, "DurationUnit");
			
			one_min_throughput = (double) remote.getAttribute(bean, "OneMinuteRate");
			rate_unit = (String) remote.getAttribute(bean, "RateUnit");			
			
			node_metrics =  new NodeMetricsObject(mean_lat, percentile_95_lat, 
							  			duration_unit, one_min_throughput, rate_unit );

			
		}
        catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException |
         	   MBeanException | ReflectionException	| IOException e) {} 
        
        return node_metrics;
    }
    

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
}