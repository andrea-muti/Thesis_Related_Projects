package ThesisRelated.MetricsProfiler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.management.MBeanServerConnection;

/**
 * MetricsProfiler
 * 		goal : collect data to be used in the training phase of the ANN
 *
 * @author andrea-muti
 */

public class MetricsProfiler {
	
    @SuppressWarnings("unused")
	public static void main( String[] args ){
    	System.out.println("\n-------------------------------");
        System.out.println("--      MetricsProfiler      --");
        System.out.println("-------------------------------\n");
        
        int input_rate;				// preso come parametro
        int num_nodes;				// leggo dal java_driver
        double cpu_level; 			// leggo da cassandra jmx
        int throughput_total;		// calcolo leggendo da cassandra jmx
        double rt_mean;				// leggo da java_driver
        double rt_95p;				// leggo da java_driver
        
        List<String> addresses;
        
        // TO DO : LEGGERE DA ARGS
        String contact_point_address = "192.168.0.169";
        check_contact_point_address(contact_point_address);
        
        // TO DO : LEGGERE DA ARGS , check isValidPortNumber
        String jmx_port = "7199";
        
        // TO DO: LEGGERE DA ARGS e check
        int cpu_num_samples = 5;
        
        // TO DO : LEGGERE DA ARGS e check
        int cpu_sampling_interval_msec = 500;
        
        //------------------------------------------------------------------------------
        
        /**   LETTURA ADDRESSES and NUMBER OF NODES IN THE CLUSTER   **/
        
        addresses = getNodesAddresses(contact_point_address, jmx_port);
        
        num_nodes = addresses.size();
        
        System.out.println(" - There are "+num_nodes+" nodes in the cluster");
        
        
        //------------------------------------------------------------------------------
        
        /**    LETTURA AVERAGE CPU LEVEL OF NODES   **/
        cpu_level = getAverageCpuLevel(jmx_port, addresses, cpu_num_samples, cpu_sampling_interval_msec);
        
        System.out.println(" - Average CPU Level : "+cpu_level);
        
    } // END MAIN
    
    //---------------------------------------------------------------------------------------------
    
    private static List<String> getNodesAddresses(String contact_point_addr, String jmx_port){
    	List<String> addresses = null;
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

        addresses = jmxreader.getLiveNodes(remote);
        
        jmxreader.disconnect();
        
        return addresses;
	}
    
    //---------------------------------------------------------------------------------------------
    
    private static double getAverageCpuLevel( String jmx_port_number, List<String> addresses, 
    										  int num_samples, int sampling_interval){
    	
    	System.out.println("\n - Computing Average CPU Level ");
    	double cpu_level_to_return = 0;
    
    	int n_nodes = addresses.size();
    	
    	double[] cpu_levels_array = new double[n_nodes];
    	
    	final ExecutorService service;
        List<Future<Double>>  task_List = new ArrayList<Future<Double>>();
         
        service = Executors.newFixedThreadPool(n_nodes);     
       
     	// per ogni nodo nel cluster, colleziona le statistics
     	for( int i=0; i<n_nodes; i++ ){
     		
     		String IP_address = addresses.get(i);
  		
            task_List.add(i, service.submit(new CPUReader(IP_address,""+jmx_port_number, num_samples, sampling_interval)));
            System.out.println("    - started CPU Reader collector for node ("+IP_address+":"+""+jmx_port_number+")");
     		
     	
     	}// end of for loop
     	
         service.shutdownNow();
         
     	int i = 0;
     	// getting the results of the collector threads
     	for(Future<Double> f : task_List){
     		try {
     			Double returned_cpu_level = f.get();
     			
     			System.out.println("     - cpu of node "+i+" : "+returned_cpu_level);
     			cpu_levels_array[i] = returned_cpu_level;
     			i++;
     		}
     		catch(Exception e){ }
     	}
     	
     	cpu_level_to_return = compute_average_of_double_array(cpu_levels_array);
     	
     	String processCPUload_formatted = String.format( "%.3f", cpu_level_to_return ).replace(",", ".");
     	cpu_level_to_return =  Double.parseDouble(processCPUload_formatted) ;
     	
     	return cpu_level_to_return;
    	
    }	
    
    //---------------------------------------------------------------------------------------------
    
    private static double compute_average_of_double_array(double[] array) {
		double result = 0;
		for(int i = 0; i<array.length; i++){
			result = result + array[i];
		}
		result = result / array.length;
		return result;
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
    
    
}
