package prove_metrics;



import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.management.MBeanServerConnection;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class MetReadByCass {
	
	static int n_hosts;
	static String[] addresses;
	static String jmx_port="7199";
	static List<Double> mean_client_read_latencies = new ArrayList<Double>();
	static List<Double> percentile95_client_read_latencies = new ArrayList<Double>();
	static List<Double> client_read_throughputs = new ArrayList<Double>();	
	static String client_read_latency_unit = "";
	static String client_read_throughput_unit = "";
	static String load_label = "1.0"; // diciamo che da cmdline passo 0.5
	static String directory_path = "/home/andrea-muti/Scrivania/Dati_Latenze";
	static String contact_point_addr = "192.168.0.169";
		
	
	public MetReadByCass(){

    	// Settaggio del loggin level a ERROR
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.ERROR);
    
    	System.out.println( "\n ********  Cassandra Metrics Reader ******** \n" );
    	
    	
    	JMXReader reader = new JMXReader(contact_point_addr, jmx_port);
        MBeanServerConnection remote = null;
		try {
			remote = reader.connect();
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

        List<String> live_nodes = reader.getLiveNodes(remote);       
        reader.disconnect();
        
        if(live_nodes==null){
        	System.err.println(" - ERROR : failed to get Live Nodes");
			System.exit(-1);
        }
        
        n_hosts = live_nodes.size();
   
        System.out.println(" - there are "+n_hosts+" Live Nodes in the Cluster\n");      
        addresses = new String[n_hosts];

        int n = 1;
        for(String addr : live_nodes){     	
        	System.out.println(" - Address of Node n°"+n+" : "+addr);
        	addresses[n-1] = addr;
        	n++;
        }
        
        System.out.println("");
	}
	public void read(){

        final ExecutorService service;
        List<Future<MetricsObject>>  task_List = new ArrayList<Future<MetricsObject>>();
        
        service = Executors.newFixedThreadPool(n_hosts);     
      
    	// per ogni nodo nel cluster, colleziona le statistics
    	for( int i=0; i<n_hosts; i++ ){    		
    		String IP_address = addresses[i];
    		int jmx_port_number = 7199;	
            task_List.add(i, service.submit(new MetricsCollector(IP_address,""+jmx_port_number)));
            System.out.println(" - started Collector Thread for metrics of node ("+IP_address+":"+""+jmx_port_number+")");
    	}// end of for loop
    	
        service.shutdownNow();
        
    	System.out.println("\n - Collected Results:");
    	
    	// getting the results of the collector threads
    	for(Future<MetricsObject> f : task_List){
    	  		
    		try {
    			
    			MetricsObject returned_metrics = f.get();
    			System.out.println("\n\n _____________________________________________________________________________");
				System.out.println("\n - Metrics of Node @ "+returned_metrics.getIPAddress()+":"+returned_metrics.getJmxPortNumber());
				
				//----------- Retrieve Client Request Metrics ---------
	            
	            System.out.println("\n - Client Request Latency and Throughput Metrics : ");
				
				// client read latency
				double mean_client_read_latency = returned_metrics.getClientRequestReadMetrics().getMeanLatency();
				double percentile95_client_read_latency = returned_metrics.getClientRequestReadMetrics().getPercentile95Latency();
				String unitCRR = returned_metrics.getClientRequestReadMetrics().getDurationUnit();

				System.out.println("    - Mean Client Request Read Latency : "+mean_client_read_latency+" "+unitCRR);
	            System.out.println("    - 95th percentile Client Request Read Latency : "+percentile95_client_read_latency+" "+unitCRR);
	            
	            mean_client_read_latencies.add(mean_client_read_latency);
	            percentile95_client_read_latencies.add(percentile95_client_read_latency);
	            
	            client_read_latency_unit = unitCRR;
				
	            // client read throughput one minute
	            double client_read_throughput_one_minute = returned_metrics.getClientRequestReadMetrics().getOneMinuteThroughput();
	            String unitCRT = returned_metrics.getClientRequestReadMetrics().getRateUnit();
	            
	            System.out.println("    - Client Read Throughput (one-minute) : "+client_read_throughput_one_minute+" "+unitCRT);
	            client_read_throughputs.add(client_read_throughput_one_minute); 
	            
	            client_read_throughput_unit = unitCRT;

			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
    	}
    	
    	System.out.println("\n\n _____________________________________________________________________________");
    	
    	System.out.println("\n    - Client Read Latency Unit : "+client_read_latency_unit);
    	

    	double mean_client_read_latency = compute_mean_global_latency(mean_client_read_latencies);  
    	double mean_95_percentile_client_read_latency = compute_mean_global_latency(percentile95_client_read_latencies);
    
    	DecimalFormat df = new  DecimalFormat(".000");   	
    	String mcr_latency_formatted = df.format(mean_client_read_latency).replace(",", ".");
    	String m95pcrl_formatted = df.format(mean_95_percentile_client_read_latency).replace(",",".");
    	
    	// update client request latency files
    	update_client_read_latencies_file(n_hosts, mcr_latency_formatted, client_read_latency_unit, directory_path);
    	System.out.println("\n    - Client Mean Read Latencies File successfully updated");
    
    	update_95_percentile_client_read_latencies_file(n_hosts, m95pcrl_formatted, client_read_latency_unit, directory_path);
    	System.out.println("    - Client 95th percentile Read Latencies File successfully updated");
    	
    	// --------------
    	
    	System.out.println("\n    - Client Read Throughput Unit : "+client_read_throughput_unit);
    
    	double mean_client_read_througput = compute_total_throughput(client_read_throughputs);

    	String mcr_throughput_formatted = df.format(mean_client_read_througput).replace(",", ".");
    
   
    	// update client request throughput files
    	update_client_read_throughputs_file(n_hosts, mcr_throughput_formatted , client_read_throughput_unit, directory_path);
    	System.out.println("\n    - Client Mean Read Throughput File successfully updated");
    
    	// print info 
    	System.out.println(" \n    - Cluster with "+n_hosts+" nodes : \n"
    			+ "       - mean_client_read_latency: "+mcr_latency_formatted+" "+client_read_latency_unit+"\n"
    			+ "       - mean_95th_percentile_client_read_latency: "+m95pcrl_formatted+" "+client_read_latency_unit+"\n"
    			+ "       - mean_client_read_throughput: "+mcr_throughput_formatted+" "+client_read_throughput_unit+"\n");

    	// --------------------------------------------------------------------------------------------------
    	
		System.out.println("\n\n _____________________________________________________________________________");
	
		
		
		
	}
	
	
    public static void main(String[] args) throws IOException {
    	MetReadByCass r = new MetReadByCass();
    	
    	for(int i = 0; i<100; i++){
    		r.read();
    		try {
				Thread.sleep(2000);
			} catch (Exception e) {
			
			}
    	}
    
    } // end main

	


	
	//---------------------------------------------------------------------------------------------
	
	// METODI PER L'UPDATE DEI FILES RELATIVI AL CLIENT REQUEST
	
	
	private static void update_client_read_latencies_file(int n_hosts, String mean_client_read_latency, String RLU, String dir_path) {
		String content = mean_client_read_latency+" "+RLU;
		String file_name = dir_path+"/client_read_latencies_"+n_hosts+"_nodes.txt";
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file_name, true));
			writer.append(content+"\n");
			writer.close();
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_name);
			e.printStackTrace();
		}	
	}
	
	private static void update_client_read_throughputs_file(int n_hosts, String mean_client_read_throughput, String RTU, String dir_path) {
		String content = mean_client_read_throughput+" "+RTU;
		String file_name = dir_path+"/client_read_throughputs_"+n_hosts+"_nodes.txt";
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file_name, true));
			writer.append(content+"\n");
			writer.close();
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_name);
			e.printStackTrace();
		}	
	}
	
	private static void update_95_percentile_client_read_latencies_file( int n_hosts, String m95pcrl_formatted, String RLU, String dir_path) {
		String content = m95pcrl_formatted+" "+RLU;
		String file_name = dir_path+"/percentile_95_client_read_latencies_"+n_hosts+"_nodes.txt";
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file_name, true));
			writer.append(content+"\n");
			writer.close();
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_name);
			e.printStackTrace();
		}	
	}
	

	//---------------------------------------------------------------------------------------------
	
	// METODI PER IL CALCOLO DEI VALORI MEDI DI LATENZE E THROUGHPUTS
	
	private static double compute_mean_global_latency(List<Double> mean_node_latencies) {
		double mean = 0;
		for( double v : mean_node_latencies ){ mean += v; }
		return mean/mean_node_latencies.size();
	}
	
	private static double compute_total_throughput(List<Double> mean_node_througputs) {
		double tot = 0;
		for( double v : mean_node_througputs ){ tot += v; }
		return tot;
	}

    //---------------------------------------------------------------------------------------------
    
}




class MetricsCollector implements Callable<MetricsObject> {
	
	private String ip_address;
	private String jmx_port_number;
	
	
	public MetricsCollector(String ip, String jmx_port){
		this.ip_address=ip;
		this.jmx_port_number=jmx_port;
	}
	
    public MetricsObject call() {
        
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
	
        // ---------- Retrieve Client Request Metrics ---------
        
	
       ClientRequestMetricsObject client_request_metrics_read = 
    		   node_reader.getClientRequestLatencyMetrics(node_remote, "Read");
     
        node_reader.disconnect();

        MetricsObject metrics = new MetricsObject(ip_address,jmx_port_number,client_request_metrics_read);
        return (metrics);
    }
}
