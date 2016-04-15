package ThesisRelated.ClusterConfigurationManager;

import javax.management.MBeanServerConnection;

public class StatusChecker implements Runnable{
	
	// instance variables
	private String ip_address;
	private String jmx_port_number;
	private JMXConnectionManager cm;
	private MBeanServerConnection connection;
	private boolean running = true;
	private String stop_state;
	
	public StatusChecker(String ip_address, String jmx_port, String stop_state){
		this.ip_address=ip_address;
		this.jmx_port_number=jmx_port;
		this.stop_state=stop_state;
		this.cm = new JMXConnectionManager(this.ip_address, this.jmx_port_number);
		
	}

	@Override
	public void run() {
		String state = "";
		String current_state = "";
		while(running){
			try{
			this.connection = this.cm.connect();
			}
			catch(Exception e){}
			
			if( this.connection==null ){ current_state = "DOWN"; }
			else{
				current_state = cm.getOperationMode(this.connection);
				this.cm.disconnect();
			}
			if( !current_state.equals(state) ){
				state = current_state;
				if(state.equals("ERROR")) { System.out.println("            - state of node @ "+this.ip_address+" : ~DOWN");}
				else{ System.out.println("            - state of node @ "+this.ip_address+" : "+state); }
			}
			
			if( state.equals(this.stop_state) || (state.equals("DOWN") && this.stop_state.equals("DECOMMISSIONED")) ){ 
				this.terminate();
			}
			else{
				try { Thread.sleep(2000); }
				catch (InterruptedException e) {}
			}
			
		}
	}
	
	public void terminate(){
		this.running=false;
	}

}
