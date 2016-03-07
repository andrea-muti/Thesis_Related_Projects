package RTReaderMovingAvg;

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