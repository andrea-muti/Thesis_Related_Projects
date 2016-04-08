package ThesisRelated.ClusterAutoScaler;

import java.io.IOException;
import java.util.List;

import javax.management.MBeanServerConnection;

public class ClusterUtils {

	public static List<String> getNodesAddresses(String contact_point_addr, String jmx_port){
    	List<String> addresses = null;
		JMXReader jmxreader = new JMXReader(contact_point_addr, jmx_port);
        MBeanServerConnection remote = null;
		try {
			remote = jmxreader.connect();
		} catch (IOException e) {
			System.err.println(" - [ClusterUtils] ERROR : There are communication problems when establishing the connection with the Cluster");
		}
		catch (SecurityException e) {
			System.err.println(" - [ClusterUtils] ERROR : There are security problems when establishing the connection with the Cluster");
		}
		catch (Exception e) {
			System.err.println(" - [ClusterUtils] ERROR : There are unknown problems when establishing the connection with the Cluster");
		}
		
		if(remote!=null){
			addresses = jmxreader.getLiveNodes(remote);
	        jmxreader.disconnect();
		}

        return addresses;
	}
	
	public static int countNodes(String contact_point_addr, String jmx_port){
		int result = 0;
		List<String> addresses = getNodesAddresses(contact_point_addr, jmx_port);
		if(addresses==null){result = -1;}
		else{ result = addresses.size(); }
		return result;
	}
}
