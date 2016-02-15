package my_cassandra_tools.Cassandra_JMX_Metrics_Reader;



import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.management.MBeanServerConnection;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("unused")

public class App {
	
	
    public static void main(String[] args) throws IOException {
    	
    	boolean debug = false;
    	
    	// Settaggio del loggin level a ERROR
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.ERROR);
    	
    	
    	// Check numero dei parametri in input
    	 
        if(args.length<3){
     	   System.err.println("ERROR : arg1 : IP address of the contact point node");
     	   System.err.println("      : arg2 : Cluster Load (in GB) [ 0.5 | 1.0 | 1.5 | 2.0 | 2.5 | 3.0 ]");
     	   System.err.println("        arg3 : <absolute-path-of-the-directory-to-store-metrics-files>\n");
     	   System.exit(-1);
        }
        
        if(args.length==4){
        	if(args[3].equalsIgnoreCase("debug")){
        		debug=true;
        	}
        }
    	
    	String contact_point_addr = args[0];
    	//String contact_point_addr = "127.0.0.1";
    	check_contact_point_address(contact_point_addr);
    	   	
    	
    	String load_label = args[1]; 
    	//String load_label = "0.5"; // diciamo che da cmdline passo 0.5
    	check_cluster_load(load_label);
    	load_label = load_label + " GB";

    	
    	String directory_path = args[2];
    	//String directory_path = "/home/andrea-muti/Scrivania/Dati_Latenze";
    	check_directory_path(directory_path);
    	
    	// variables
    	List<Double> mean_node_read_latencies =  new ArrayList<Double>();
    	List<Double> mean_node_write_latencies = new ArrayList<Double>();
    	List<Double> mean_client_read_latencies = new ArrayList<Double>();
    	List<Double> mean_client_write_latencies = new ArrayList<Double>();
    	List<Double> percentile95_node_read_latencies =  new ArrayList<Double>();
    	List<Double> percentile95_node_write_latencies = new ArrayList<Double>();
    	List<Double> percentile95_client_read_latencies = new ArrayList<Double>();
    	List<Double> percentile95_client_write_latencies = new ArrayList<Double>();
    	List<Double> client_read_throughputs = new ArrayList<Double>();
    	List<Double> client_write_throughputs = new ArrayList<Double>();

    	List<Double> node_read_throughputs = new ArrayList<Double>();
    	List<Double> node_write_throughputs = new ArrayList<Double>();
    	
    	List<Double> loads_MB = new ArrayList<Double>();
    	
    	String read_latency_unit = "";
    	String write_latency_unit = "";
    	String client_read_latency_unit = "";
    	String client_write_latency_unit = "";
    	
    	String node_read_throughput_unit = "";
    	String node_write_throughput_unit = "";
    	String client_read_throughput_unit = "";
    	String client_write_throughput_unit = "";
    	
    	String[] addresses;
    	int n_hosts;
    	
    	System.out.println( "\n ********  Cassandra Metrics Reader ******** \n" );
    	
    	
    	String jmx_port;
    	if(debug){jmx_port="7201";}
    	else{jmx_port="7199";}
        
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
       
        final ExecutorService service;
        List<Future<MetricsObject>>  task_List = new ArrayList<Future<MetricsObject>>();
        
        service = Executors.newFixedThreadPool(n_hosts);     
      
    	// per ogni nodo nel cluster, colleziona le statistics
    	for( int i=0; i<n_hosts; i++ ){
    		
    		String IP_address = addresses[i];

    		int jmx_port_number = 7199;
    		
    		if(debug){
    			IP_address  = "127.0.0.1";
    			jmx_port_number = 7200 + Integer.parseInt(""+addresses[i].charAt(addresses[i].length()-1));
    		}
    	
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
	            
	            // ---
				
				// client write latency
				double mean_client_write_latency = returned_metrics.getClientRequestWriteMetrics().getMeanLatency();
				double percentile95_client_write_latency = returned_metrics.getClientRequestWriteMetrics().getPercentile95Latency();
				String unitCRW = returned_metrics.getClientRequestWriteMetrics().getDurationUnit();
		
				System.out.println("    - Mean Client Request Write Latency : "+mean_client_write_latency+" "+unitCRW);
	            System.out.println("    - 95th percentile Client Request Write Latency : "+percentile95_client_write_latency+" "+unitCRW);
	            
	            mean_client_write_latencies.add(mean_client_write_latency);
	            percentile95_client_write_latencies.add(percentile95_client_write_latency);
	            
	            client_write_latency_unit = unitCRW;
				
	            // client write throughput one minute
	            double client_write_throughput_one_minute = returned_metrics.getClientRequestWriteMetrics().getOneMinuteThroughput();
	            String unitCWT = returned_metrics.getClientRequestWriteMetrics().getRateUnit();
	            
	            System.out.println("    - Client Write Throughput (one-minute) : "+client_write_throughput_one_minute+" "+unitCRT);
	            client_write_throughputs.add(client_write_throughput_one_minute);
	            
	            client_write_throughput_unit = unitCRT;

	            
	            //----------- Retrieve Node Metrics ---------
	            
	            System.out.println("\n - Node Latency and Throughput Metrics : ");
	            
	         // node read latency
				double mean_node_read_latency = returned_metrics.getNodeReadMetrics().getMeanLatency();
				double percentile95_node_read_latency = returned_metrics.getNodeReadMetrics().getPercentile95Latency();
				String unitNRL = returned_metrics.getNodeReadMetrics().getDurationUnit();
			
				
				System.out.println("    - Mean node Request Read Latency : "+mean_node_read_latency+" "+unitNRL);
	            System.out.println("    - 95th percentile node Request Read Latency : "+percentile95_node_read_latency+" "+unitNRL);
	            
	            mean_node_read_latencies.add(mean_node_read_latency);
	            percentile95_node_read_latencies.add(percentile95_node_read_latency);
	            
	            read_latency_unit = unitNRL;
				
	            // node read throughput one minute
	            double node_read_throughput_one_minute = returned_metrics.getNodeReadMetrics().getOneMinuteThroughput();
	            String unitNRT = returned_metrics.getNodeReadMetrics().getRateUnit();
	            
	            System.out.println("    - Node Read Throughput (one-minute) : "+node_read_throughput_one_minute+" "+unitNRT);
	            node_read_throughputs.add(node_read_throughput_one_minute);
	            
	            node_read_throughput_unit = unitNRT;
	            
	           // ---
				
				// node write latency
				double mean_node_write_latency = returned_metrics.getNodeWriteMetrics().getMeanLatency();
				double percentile95_node_write_latency = returned_metrics.getNodeWriteMetrics().getPercentile95Latency();
				String unitNWL = returned_metrics.getNodeWriteMetrics().getDurationUnit();
			
				
				System.out.println("    - Mean node Request Write Latency : "+mean_node_write_latency+" "+unitNWL);
	            System.out.println("    - 95th percentile node Request Write Latency : "+percentile95_node_write_latency+" "+unitNWL);
	            
	            mean_node_write_latencies.add(mean_node_write_latency);
	            percentile95_node_write_latencies.add(percentile95_node_write_latency);
	            
	            write_latency_unit = unitNWL;
				
	            // node write throughput one minute
	            double node_write_throughput_one_minute = returned_metrics.getNodeWriteMetrics().getOneMinuteThroughput();
	            String unitNWT = returned_metrics.getNodeWriteMetrics().getRateUnit();
	            
	            System.out.println("    - node Write Throughput (one-minute) : "+node_write_throughput_one_minute+" "+unitNWT);	            
	            node_write_throughputs.add(node_write_throughput_one_minute);
	            
	            node_write_throughput_unit = unitNRT;
	            
				
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
    	}
    	

        
    	
    	System.out.println("\n\n _____________________________________________________________________________");
    	
    	System.out.println("\n - Load Label : "+load_label);
    	

    	System.out.println("\n\n - CLIENT REQUEST DATA: ");
    	
    	System.out.println("\n    - Client Read Latency Unit : "+client_read_latency_unit);
    	System.out.println("    - Client Write Latency Unit : "+client_write_latency_unit);
    	

    	double mean_client_read_latency = compute_mean_global_latency(mean_client_read_latencies);
    	double mean_client_write_latency = compute_mean_global_latency(mean_client_write_latencies);
    	double mean_95_percentile_client_read_latency = compute_mean_global_latency(percentile95_client_read_latencies);
    	double mean_95_percentile_client_write_latency = compute_mean_global_latency(percentile95_client_write_latencies);
    	
    	DecimalFormat df = new  DecimalFormat(".000");
    	
    	String mcr_latency_formatted = df.format(mean_client_read_latency).replace(",", ".");
    	String mcw_latency_formatted = df.format(mean_client_write_latency).replace(",",".");
    	String m95pcrl_formatted = df.format(mean_95_percentile_client_read_latency).replace(",",".");
    	String m95pcwl_formatted = df.format(mean_95_percentile_client_write_latency).replace(",",".");
    	
    	// update client request latency files
    	update_client_read_latencies_file(load_label, n_hosts, mcr_latency_formatted, client_read_latency_unit, directory_path);
    	System.out.println("\n    - Client Mean Read Latencies File successfully updated");
    	
    	update_client_write_latencies_file(load_label, n_hosts, mcw_latency_formatted, client_write_latency_unit, directory_path);
    	System.out.println("    - Client Mean Write Latencies File successfully updated");
    	
    	update_95_percentile_client_read_latencies_file(load_label, n_hosts, m95pcrl_formatted, client_read_latency_unit, directory_path);
    	System.out.println("    - Client 95th percentile Read Latencies File successfully updated");
    	
    	update_95_percentile_client_write_latencies_file(load_label, n_hosts, m95pcwl_formatted, client_write_latency_unit, directory_path);
    	System.out.println("    - Client 95th percentile Write Latencies File successfully updated");
    
    	// --------------
    	
    	System.out.println("\n    - Client Read Throughput Unit : "+client_read_throughput_unit);
    	System.out.println("    - Client Write Throughput Unit : "+client_write_throughput_unit);
    	
    	double mean_client_read_througput = compute_mean_global_throughput(client_read_throughputs);
    	double mean_client_write_througput = compute_mean_global_throughput(client_write_throughputs);
    	
    	String mcr_throughput_formatted = df.format(mean_client_read_througput).replace(",", ".");
    	String mcw_throughput_formatted = df.format(mean_client_write_througput).replace(",",".");
   
    	// update client request throughput files
    	update_client_read_throughputs_file(load_label, n_hosts, mcr_throughput_formatted , client_read_throughput_unit, directory_path);
    	System.out.println("\n    - Client Mean Read Throughput File successfully updated");
    	
    	update_client_write_throughputs_file(load_label, n_hosts, mcw_throughput_formatted , client_write_throughput_unit, directory_path);
    	System.out.println("    - Client Mean Write Throughput File successfully updated");

    	
    	// print info 
    	System.out.println(" \n    - Cluster "+load_label+" with "+n_hosts+" nodes : \n"
    			+ "       - mean_client_read_latency: "+mcr_latency_formatted+" "+client_read_latency_unit+"\n"
    			+ "       - mean_client_write_latency: "+mcw_latency_formatted+" "+client_write_latency_unit+"\n"
    			+ "       - mean_95th_percentile_client_read_latency: "+m95pcrl_formatted+" "+client_read_latency_unit+"\n"
    			+ "       - mean_95th_percentile_client_write_latency: "+m95pcwl_formatted+" "+client_write_latency_unit+"\n"
    			+ "       - mean_client_read_throughput: "+mcr_throughput_formatted+" "+client_read_throughput_unit+"\n"
    			+ "       - mean_client_write_throughput: "+mcw_throughput_formatted+" "+client_write_throughput_unit+"\n");

    	// --------------------------------------------------------------------------------------------------
    	
		System.out.println("\n\n _____________________________________________________________________________");

    	System.out.println("\n - NODE DATA: ");
    	
    	System.out.println("\n    - Read Latency Unit : "+read_latency_unit);
    	System.out.println("    - Write Latency Unit : "+write_latency_unit);	
    	
    	double mean_global_read_latency = compute_mean_global_latency(mean_node_read_latencies);
    	double mean_global_write_latency = compute_mean_global_latency(mean_node_write_latencies);
    	double mean_95_percentile_node_read_latency = compute_mean_global_latency(percentile95_node_read_latencies);
    	double mean_95_percentile_node_write_latency = compute_mean_global_latency(percentile95_node_write_latencies);
    	
    	String mgr_latency_formatted = df.format(mean_global_read_latency).replace(",", ".");
    	String mgw_latency_formatted = df.format(mean_global_write_latency).replace(",",".");
    	String m95pnrl_formatted = df.format(mean_95_percentile_node_read_latency).replace(",",".");
    	String m95pnwl_formatted = df.format(mean_95_percentile_node_write_latency).replace(",",".");
    
    	update_read_latencies_file(load_label, n_hosts, mgr_latency_formatted, read_latency_unit, directory_path);
    	System.out.println("\n    - Node Mean Read Latencies File successfully updated");
    	
    	update_write_latencies_file(load_label, n_hosts, mgw_latency_formatted, write_latency_unit, directory_path);
    	System.out.println("    - Node Mean Write Latencies File successfully updated");
    	
    	update_95_percentile_read_latencies_file(load_label, n_hosts, m95pnrl_formatted, read_latency_unit, directory_path);
    	System.out.println("    - Node 95th percentile Read Latencies File successfully updated");
    	
    	update_95_percentile_write_latencies_file(load_label, n_hosts, m95pnwl_formatted, write_latency_unit, directory_path);
    	System.out.println("    - Node 95th percentile Write Latencies File successfully updated");
	
    	// throughput
    	
    	System.out.println("\n    - Node Read Throughput Unit : "+node_read_throughput_unit);
    	System.out.println("    - Node Write Throughput Unit : "+node_write_throughput_unit);	
    	
    	double mean_node_read_througput = compute_mean_global_throughput(node_read_throughputs);
    	double mean_node_write_througput = compute_mean_global_throughput(node_write_throughputs);
    	
    	String mnr_throughput_formatted = df.format(mean_node_read_througput).replace(",", ".");
    	String mnw_throughput_formatted = df.format(mean_node_write_througput).replace(",",".");
    	
    	// update node throughput files
    	update_node_read_throughputs_file(load_label, n_hosts, mnr_throughput_formatted , node_read_throughput_unit, directory_path);
    	System.out.println("\n    - Node Mean Read Throughput File successfully updated");
    	
    	update_node_write_throughputs_file(load_label, n_hosts, mnw_throughput_formatted , node_write_throughput_unit, directory_path);
    	System.out.println("    - Node Mean Write Throughput File successfully updated");
    	
    	// print 
    	System.out.println(" \n    - Cluster "+load_label+" with "+n_hosts+" nodes : \n"
    			+ "       - mean_global_read_latency: "+mgr_latency_formatted+" "+read_latency_unit+"\n"
    			+ "       - mean_global_write_latency: "+mgw_latency_formatted+" "+write_latency_unit+"\n"
    			+ "       - mean_95th_percentile_global_read_latency: "+m95pnrl_formatted+" "+client_read_latency_unit+"\n"
    			+ "       - mean_95th_percentile_global_write_latency: "+m95pnwl_formatted+" "+client_write_latency_unit+"\n"
    			+ "       - mean_node_read_throughput: "+mnr_throughput_formatted+" "+node_read_throughput_unit+"\n"
    			+ "       - mean_node_write_throughput: "+mnw_throughput_formatted+" "+node_write_throughput_unit+"\n");
   		
   		
    
    } // end main
    
    


	/**
     * CHECK WHETHER THE LOAD LABEL IS VALID
     * @param load_label : String,  in ( "0.5", "1.0", "1.5", "2.0", "2.5", "3.0" )
     */
    private static void check_cluster_load(String load_label) {
    	String ll = load_label.trim();
		if( !ll.equals("0.5") && !ll.equals("1.0") && !ll.equals("1.5") && 
		    !ll.equals("2.0") && !ll.equals("2.5") && !ll.equals("3.0") ){
			System.err.println(" - ERROR : Load Label must be one of those: [ 0.5, 1.0, 1.5, 2.0, 2.5, 3.0 ]\n"
					         + "           received instead : "+load_label);
			System.exit(-1);
		}
		
	}

	//---------------------------------------------------------------------------------------------
    
    /**
     * CHECK WHETHER THE DIRECTORY PATH IS VALID
     * @param directory_path
     */
    private static void check_directory_path(String directory_path) {
    	try {
    		Path p = Paths.get(directory_path);
    		if(!p.isAbsolute()){
    			System.err.println(" - ERROR : Path to the directory must be ABSOLUTE");
    			System.exit(-1);
    		}
		} catch (InvalidPathException e) {
			System.err.println(" - ERROR : Malformed Directory Path");
    		System.exit(-1);
		}	
	}

    //---------------------------------------------------------------------------------------------
    
    /** CHECK WHETER THE CONTACT POINT ADDRESS IS VALID
     * @param contact_point_addr
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
	
	// METODI PER L'UPDATE DEI FILES RELATIVI AL NODE
	
	private static void update_write_latencies_file(String load_label, int n_hosts, String mean_global_write_latency, String WLU, String dir_path) {
		String content = load_label+" : "+mean_global_write_latency+" "+WLU;
		String file_name = dir_path+"/mean_write_latencies_"+n_hosts+"_nodes.txt";
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file_name, true));
			writer.append(content+"\n");
			writer.close();
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_name);
			e.printStackTrace();
		}	
	}

	
	private static void update_read_latencies_file(String load_label, int n_hosts, String mean_global_read_latency, String RLU, String dir_path) {
		String content = load_label+" : "+mean_global_read_latency+" "+RLU;
		String file_name = dir_path+"/mean_read_latencies_"+n_hosts+"_nodes.txt";
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file_name, true));
			writer.append(content+"\n");
			writer.close();
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_name);
			e.printStackTrace();
		}	
	}

	private static void update_node_read_throughputs_file(String load_label, int n_hosts, String mean_node_read_throughput, String RTU, String dir_path) {
		String content = load_label+" : "+mean_node_read_throughput+" "+RTU;
		String file_name = dir_path+"/node_read_throughputs_"+n_hosts+"_nodes.txt";
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file_name, true));
			writer.append(content+"\n");
			writer.close();
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_name);
			e.printStackTrace();
		}	
	}
	
	private static void update_node_write_throughputs_file(String load_label, int n_hosts, String mean_node_write_throughput, String WTU, String dir_path) {
		String content = load_label+" : "+mean_node_write_throughput+" "+WTU;
		String file_name = dir_path+"/node_write_throughputs_"+n_hosts+"_nodes.txt";
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file_name, true));
			writer.append(content+"\n");
			writer.close();
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_name);
			e.printStackTrace();
		}	
	}
	
	private static void update_95_percentile_write_latencies_file(String load_label, int n_hosts,
			String m95pnwl_formatted, String WLU, String dir_path) {
		String content = load_label+" : "+m95pnwl_formatted+" "+WLU;
		String file_name = dir_path+"/percentile_95_node_write_latencies_"+n_hosts+"_nodes.txt";
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file_name, true));
			writer.append(content+"\n");
			writer.close();
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_name);
			e.printStackTrace();
		}	
	}

	private static void update_95_percentile_read_latencies_file(String load_label, int n_hosts,
			String m95pnrl_formatted, String RLU, String dir_path) {
		String content = load_label+" : "+m95pnrl_formatted+" "+RLU;
		String file_name = dir_path+"/percentile_95_node_read_latencies_"+n_hosts+"_nodes.txt";
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
	
	// METODI PER L'UPDATE DEI FILES RELATIVI AL CLIENT REQUEST
	
	private static void update_client_write_latencies_file(String load_label, int n_hosts, String mean_client_write_latency, String WLU, String dir_path) {
		String content = load_label+" : "+mean_client_write_latency+" "+WLU;
		String file_name = dir_path+"/client_write_latencies_"+n_hosts+"_nodes.txt";
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file_name, true));
			writer.append(content+"\n");
			writer.close();
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_name);
			e.printStackTrace();
		}	
	}
	
	private static void update_client_read_latencies_file(String load_label, int n_hosts, String mean_client_read_latency, String RLU, String dir_path) {
		String content = load_label+" : "+mean_client_read_latency+" "+RLU;
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
	
	private static void update_client_read_throughputs_file(String load_label, int n_hosts, String mean_client_read_throughput, String RTU, String dir_path) {
		String content = load_label+" : "+mean_client_read_throughput+" "+RTU;
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
	
	private static void update_client_write_throughputs_file(String load_label, int n_hosts, String mean_client_write_throughput, String WTU, String dir_path) {
		String content = load_label+" : "+mean_client_write_throughput+" "+WTU;
		String file_name = dir_path+"/client_write_throughputs_"+n_hosts+"_nodes.txt";
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file_name, true));
			writer.append(content+"\n");
			writer.close();
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_name);
			e.printStackTrace();
		}	
	}
	
	
	private static void update_95_percentile_client_write_latencies_file(String load_label, int n_hosts,
			String m95pcwl_formatted, String WLU, String dir_path) {
		String content = load_label+" : "+m95pcwl_formatted+" "+WLU;
		String file_name = dir_path+"/percentile_95_client_write_latencies_"+n_hosts+"_nodes.txt";
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file_name, true));
			writer.append(content+"\n");
			writer.close();
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_name);
			e.printStackTrace();
		}	
	}

	private static void update_95_percentile_client_read_latencies_file(String load_label, int n_hosts,
			String m95pcrl_formatted, String RLU, String dir_path) {
		String content = load_label+" : "+m95pcrl_formatted+" "+RLU;
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
	
	private static double compute_mean_global_throughput(List<Double> mean_node_througputs) {
		double mean = 0;
		for( double v : mean_node_througputs ){ mean += v; }
		return mean/mean_node_througputs.size();
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
       
       ClientRequestMetricsObject client_request_metrics_write = 
    		   node_reader.getClientRequestLatencyMetrics(node_remote, "Write");
        
       
       // ---------- Retrieve Node Metrics ---------
       
   	
       NodeMetricsObject node_metrics_read = 
    		   node_reader.getNodeLatencyMetrics(node_remote, "ReadLatency");
       
       NodeMetricsObject node_metrics_write = 
    		   node_reader.getNodeLatencyMetrics(node_remote, "WriteLatency");
        
        // ----------
        
        node_reader.disconnect();

    
        MetricsObject metrics = new MetricsObject(ip_address,jmx_port_number,
        								client_request_metrics_read, client_request_metrics_write, 
        								node_metrics_read, node_metrics_write );
        return (metrics);
    }
}
