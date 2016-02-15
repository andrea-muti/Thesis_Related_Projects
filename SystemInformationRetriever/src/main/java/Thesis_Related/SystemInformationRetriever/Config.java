package Thesis_Related.SystemInformationRetriever;


import java.util.HashMap;
import java.util.Map;

public class Config {

	public static Integer intervall_timer = 5;
	public static Integer threshold_breach_limit = 10;
	public static Double min_memory_usage = 10.0;
	public static Double max_memory_usage = 90.0;
	public static Long min_disk_space_used = 60L;
	public static Long max_disk_space_used = 20000L;
	public static Double min_cpu_used = 5.0;
	public static Double max_cpu_used = 80.0;
	public static volatile String master_host = "127.0.0.1";
	
	/* Communication-ports */
	public static Integer slave_input_port = 7799;
	public static Integer slave_output_port = 7798;
	
	/* Local configurations - Not modified by master */
	public static String configuration_file = "./conf/autoscale-agent.yaml";

	public static String root = "path-to-root/apache-cassandra-1.1.7";
	public static String startup_command = "bin/cassandra";
	public static String shutdown_command = "kill %d";
	
	public static Map<String, String> clean_directories = new HashMap<String, String>();
	
	public static Process runtime_process = null;
	
	/* Current node- address and port */
	public static String node_address = "127.0.0.1";
	public static Integer node_port = 8001;
}