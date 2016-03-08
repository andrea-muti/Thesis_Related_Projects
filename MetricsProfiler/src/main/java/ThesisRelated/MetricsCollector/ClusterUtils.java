package ThesisRelated.MetricsCollector;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;


public class ClusterUtils {

	public static final Session createSession(Cluster cluster, int retry_interval_sec) {
		Session session = null;
		do {
			try {
				session = cluster.connect();
			}catch (NoHostAvailableException e){
	        	System.err.println(" - ERROR : no one of the given contact-point nodes is reachable");
	        	try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {}
	        } 
			catch (Exception connErr) {
				System.err.println(" - ERROR : Unable to connect to cluster (retrying in "+retry_interval_sec+"s): "+connErr.getMessage());
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {}
			}
		} while (session == null);
		return session;
	}
	
}



