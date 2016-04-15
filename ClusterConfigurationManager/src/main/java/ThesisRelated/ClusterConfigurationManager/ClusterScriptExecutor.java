package ThesisRelated.ClusterConfigurationManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * ClusterScriptExecutor
 * class that provides methods that execute various bash scripts implementing some cluster functionalities
 * @author andrea-muti
 *
 */

public class ClusterScriptExecutor {
	
	/** start_cassandra
	 *      executes a bash script that connects via ssh to the node 'node_ip' and triggers the 
	 *      start up of the cassandra process
	 *      
	 * @param node_ip : IP address of the node where cassandra process has to be started
	 * @param remote_user : username required in order to establish an ssh session with node_ip
	 * @param remote_pass : password require in order to establish an ssh session with node_ip
	 * @param cass_path : the remote path where cassandra is installed 
	 * 					  [ it should be such that the nodetool utility is located in
	 *                      cass_path/bin/cassandra ]
	 * @return boolean true if the startup was executed successfully, false otherwise.
	 */
	public static boolean start_cassandra(String node_ip, String remote_user, String remote_pass, String cass_path){
		boolean success = true;
		String exec_command = "sh resources/scripts/startup_trigger.sh "+node_ip+" "
							  +remote_user+" "+remote_pass+" "+cass_path;
	
		try{
			//System.out.println(" - sto per invocare exec startup script exec comm : "+exec_command);
			Process starter_process = Runtime.getRuntime().exec(exec_command);
			//System.out.println(" -  exec startup script finita");
		}
		catch(Exception e){
			System.err.println(" - ERROR in the startup of cassandra on node "+node_ip);
			success=false;
		}
		return success;
	}
	
	/** cleanup
	 *  	executes a bash script that connects via ssh to the node 'node_ip' and triggers the 
	 *      execution of a cleanup operation on that node
	 *      
	 * @param node_ip : IP address of the node where the cleanup has to be executed
	 * @param remote_user : username required in order to establish an ssh session with node_ip
	 * @param remote_pass : password require in order to establish an ssh session with node_ip
	 * @param cass_path : the remote path where cassandra is installed 
	 * 					  [ it should be such that the nodetool utility is located in
	 *                      cass_path/bin/nodetool ]
	 * @return boolean true if the cleanup was executed successfully, false otherwise.
	 */
	public static boolean cleanup(String node_ip, String remote_user, String remote_pass, String cass_path) {
	
		boolean success = true;
		String exec_command = "sh resources/scripts/cleanup_trigger.sh "+node_ip+" "
							  +remote_user+" "+remote_pass+" "+cass_path;
	
		try{
			Process cleaner_process = Runtime.getRuntime().exec(exec_command);
			String line = "";
			
	       BufferedReader en = new BufferedReader(new InputStreamReader(cleaner_process.getErrorStream()));
	       while ((line = en.readLine()) != null) {
	         if(line.equals("Permission denied, please try again.")){
	        	 System.out.print(" PERMISSION DENIED -->");
	        	 success = false;
	         }
	         else if(line.contains("File") && line.contains("directory")){
	        	 System.out.print(" ERROR Non existing dir -->");
	        	 success = false;
	         }      
	       }
	       en.close();
	       cleaner_process.waitFor();
		}
		catch(Exception e){
			System.err.println(" - ERROR in the cleanup on node "+node_ip);
			success=false;
		}
		return success;
	}
	
	
	/** cassandra_process_killer
	 *  	executes a bash script that connects via ssh to the node 'node_ip' and kills the
	 *      currently running cassandra process on that node
	 *      
	 * @param node_ip : IP address of the node where the cassandra process has to be killed
	 * @param remote_user : username required in order to establish an ssh session with node_ip
	 * @param remote_pass : password require in order to establish an ssh session with node_ip
	 *
	 * @return boolean true if the killing was executed successfully, false otherwise.
	 */
	public static boolean cassandra_process_killer(String node_ip, String remote_user, String remote_pass) {
		
		boolean success = true;
		String exec_command = "sh resources/scripts/cassandra_killer.sh "+node_ip+" "
							  +remote_user+" "+remote_pass;
	
		try{
			Process killer_process = Runtime.getRuntime().exec(exec_command);
			String line = "";
			
	       BufferedReader en = new BufferedReader(new InputStreamReader(killer_process.getErrorStream()));
	       while ((line = en.readLine()) != null) {
	         if(line.equals("Permission denied, please try again.")){
	        	 System.out.print(" PERMISSION DENIED -->");
	        	 success = false;
	         }
	         else if(line.contains("File") && line.contains("directory")){
	        	 System.out.print(" ERROR Non existing dir -->");
	        	 success = false;
	         }      
	       }
	       en.close();
	       killer_process.waitFor();
		}
		catch(Exception e){
			System.err.println(" - ERROR during kill of cassandra process on node "+node_ip);
			success=false;
		}
		return success;
	}
	
	/** old_data_removal
	 *  executes a bash script that connects via ssh to the node 'node_ip' and triggers the 
	 *      removal of the content of the cassandra_dir/data directory containing old data.
	 *      These data have to be removed otherwise the node will not be able to startup again
	 *      
	 * @param node_ip : IP address of the node where the data removal has to be executed
	 * @param remote_user : username required in order to establish an ssh session with node_ip
	 * @param remote_pass : password require in order to establish an ssh session with node_ip
	 * @param cass_path : the remote path where cassandra is installed 
	 * 					  [ it should be such that the nodetool utility is located in
	 *                      cass_path/data ]
	 * @return boolean true if the cleanup was executed successfully, false otherwise.
	 */
	public static boolean old_data_removal(String node_ip, String remote_user, String remote_pass, String cass_path){

		boolean success = true;
		String exec_command = "sh resources/scripts/old_data_remover.sh "+node_ip+" "
				  +remote_user+" "+remote_pass+" "+cass_path;
	
		try{
			Process remover_process = Runtime.getRuntime().exec(exec_command);
			String line = "";
			
	       BufferedReader en = new BufferedReader(new InputStreamReader(remover_process.getErrorStream()));
	       while ((line = en.readLine()) != null) {
	         if(line.equals("Permission denied, please try again.")){
	        	 System.out.print(" PERMISSION DENIED -->");
	        	 success = false;
	         }
	         else if(line.contains("File") && line.contains("directory")){
	        	 System.out.print(" ERROR Non existing dir -->");
	        	 success = false;
	         }      
	       }
	       en.close();
	       remover_process.waitFor();
		}
		catch(Exception e){
			System.err.println(" - ERROR during the removal of old data on node "+node_ip);
			success=false;
		}
		return success;
	}
	
}
