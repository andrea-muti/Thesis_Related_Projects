package ThesisRelated.MetricsCollector;

import java.io.IOException;
import java.util.concurrent.Callable;

import javax.management.MBeanServerConnection;


public class ThroughputReader implements Callable<Double> {
	
	private String ip_address;
	private String jmx_port_number;
	private int num_samples;
	private int sampling_interval;
	
	public ThroughputReader(String ip, String jmx_port, int num_samp, int sampling_inter){
		this.ip_address=ip;
		this.jmx_port_number=jmx_port;
		this.num_samples = num_samp;
		this.sampling_interval = sampling_inter;
	}
	
    public Double call() {
        
    	double throughput_level = 0;
    	
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
	
        // ---------- Retrieve Throughput  ---------
        
		 // prendo num_samples campioni ad un rate di 1sample ogni sampling_interval msec e ne faccio la media
		for(int i = 0; i<this.num_samples; i++){
			try {
				Thread.sleep(sampling_interval);
			} catch (InterruptedException e) {}
			double sample = node_reader.getThroughputLevel(node_remote);
			throughput_level = throughput_level + sample ;
			
		}
		
		throughput_level = throughput_level / this.num_samples ;
		
       
        // ----------------------------------------------------
		
        node_reader.disconnect();

    
        return throughput_level;
    }
    
    
    
}