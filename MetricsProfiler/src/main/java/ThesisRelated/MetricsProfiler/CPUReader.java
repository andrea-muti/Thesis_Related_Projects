package ThesisRelated.MetricsProfiler;

import java.io.IOException;
import java.util.concurrent.Callable;

import javax.management.MBeanServerConnection;


public class CPUReader implements Callable<Double> {
	
	private String ip_address;
	private String jmx_port_number;
	
	
	public CPUReader(String ip, String jmx_port){
		this.ip_address=ip;
		this.jmx_port_number=jmx_port;
	}
	
    public Double call() {
        
    	double cpu_level = 0;
    	
    	JMXReader node_reader = new JMXReader(this.ip_address, ""+this.jmx_port_number);
        MBeanServerConnection node_remote = null;
		try {
			node_remote = node_reader.connect();
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
	
        // ---------- Retrieve CPU Level ---------
        
		// TO DO : invece di prendere un valore singolo , prendere una serie e fare la media
		
		cpu_level = node_reader.getCPULevel(node_remote);
		
       
        // ----------------------------------------------------
		
        node_reader.disconnect();

    
        return cpu_level;
    }
    
    
    
}