package jmeter_automation;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;


public class JMeterController {

	/**    instance variables    **/
	Properties jmeterProperties;
	File jmeterPropertiesFile;
	private boolean noGui;
	private File 	jMeterPath;
	private File 	jmxFile; 
	private String java_rmi_server_hostname;
	private String jmeter_slaves_IPs;
	private int slaves_number;
	private int total_request_rate;
	private int slave_request_rate;
	private int thread_number_per_slave;
	private int test_duration_sec;
	private int ramp_duration_sec;
	private boolean isRunning;
	private int timeout_jmeter_slaves;
	
	
	/** public constructor **/
	public JMeterController(File jmeterProperties) {
		this(FileUtils.loadProperties(jmeterProperties));
		this.jmeterPropertiesFile=jmeterProperties;
	}
	
	/** private constructor **/
	private JMeterController(Properties jmeterProperties) {
		super();
		this.jmeterProperties = jmeterProperties;
		this.noGui = Boolean.parseBoolean(jmeterProperties.getProperty("noGui","true"));
		this.jMeterPath = new File(jmeterProperties.getProperty("jMeterPath"));
		this.jmxFile = new File(jmeterProperties.getProperty("jmxFile"));
		this.java_rmi_server_hostname = jmeterProperties.getProperty("java_rmi_server_hostname");
		this.jmeter_slaves_IPs = jmeterProperties.getProperty("jmeter_slaves_IPs");
		this.slaves_number = count_slaves(this.jmeter_slaves_IPs);
		this.total_request_rate = Integer.parseInt(jmeterProperties.getProperty("rate"));
		this.slave_request_rate = this.total_request_rate / this.slaves_number;
		this.thread_number_per_slave = this.slave_request_rate / 5 ;
		if(this.thread_number_per_slave == 0){ this.thread_number_per_slave = 1; }
		this.test_duration_sec = Integer.parseInt(jmeterProperties.getProperty("testDuration","60"));
		this.ramp_duration_sec = Integer.parseInt(jmeterProperties.getProperty("rampDuration","10"));
		this.timeout_jmeter_slaves = Integer.parseInt(jmeterProperties.getProperty("timeoutJMeterSlaves","60"));
		this.isRunning = false;
		kill_and_restart_jmeter_slaves_by_script();
	}
	
	private void kill_and_restart_jmeter_slaves_by_script(){
		System.out.print(" - initial killing & restarting of jmeter-slaves: ");
		try{
		// allo startup killiamo e facciamo ripartire i jmeter-slaves per sicurezza
		Process killer = Runtime.getRuntime().exec("sh files/scripts/jmeter_slaves_restarter.sh");
		killer.waitFor();
		}
		catch(Exception e){
			System.out.println(" FAILED");
			System.err.println(" - ERROR in the initial killing and restarting of jmeter-slaves");
		}
		System.out.println(" DONE");
	}
	
	private int count_slaves(String jmeter_slaves_IPs){
		StringTokenizer strtok = new StringTokenizer(jmeter_slaves_IPs, ",");
		int count = strtok.countTokens();
		return count;
	}
		
	public void runJMeter() {
		this.isRunning = true;
		//System.out.println("  ---------- runJMeter() ----------");
		
		this.jmeterProperties = FileUtils.loadProperties(jmeterPropertiesFile);
		
		this.total_request_rate = Integer.parseInt(this.jmeterProperties.getProperty("rate"));
		this.slave_request_rate = this.total_request_rate / this.slaves_number;
		
		// Parameters Generation
		String params = generateJMeterParameters( this.noGui, this.jmxFile, this.java_rmi_server_hostname, 
												  this.jmeter_slaves_IPs, this.slave_request_rate, 
												  this.test_duration_sec, this.ramp_duration_sec, 
												  this.thread_number_per_slave );

		long test_duration_ms = this.test_duration_sec * 1000;
		long ramp_up_and_down_durations_ms = 2 * this.ramp_duration_sec * 1000;
		long total_duration = 20*1000 + test_duration_ms + ramp_up_and_down_durations_ms; // è la durata del test jmeter
		
		// invokes the execution of JMeter Master ( that will call the slaves)
		try {
			ProgressController progressController = new ProgressUI();

			String exec_command = jMeterPath.toString() + params;
			
			System.out.println(" - start execution of Load Generation");
					
			long start = System.currentTimeMillis();
			Process process = Runtime.getRuntime().exec(exec_command);
			progressController.processStarted(process, start + total_duration);
			
			// aspetto la fine dell'esecuione dei jmeter-slaves e dunque del master per un tempo 'total_duration'
			while( (System.currentTimeMillis() - start) <= total_duration ){
				try {
					if(process.isAlive()){
						Thread.sleep( total_duration/100 );
					}
					else{ break;}
				} catch (InterruptedException e) {}	
				progressController.currentProgress( (System.currentTimeMillis() - start) / (double) total_duration );
			}
			
			// se dopo 'total_duration' secondi i jmeter-slaves non hanno ancora terminato
			// gli permetto di terminare entro altri 'timeout_jmeter_slaves' secondi, 
			// altrimenti li assumo come bloccati, li uccido e li faccio ripartire
		    try {
		    	// aspetto indefinitamente che i jmeter-slaves terminino --> a volte si blocca!
		    	//process.waitFor();
		    	
		    	// allo scadere del tempo 'total_duration' che ho preventivato per il completamento dell'esecuzione
		    	// del load generation da parte dei jmeter-slaves, se questi non hanno ancora terminato
		    	// aspetto al max altri 'timeout_jmeter_slaves' secondi. Se i jmeter-slaves non sono ancora
		    	// terminati entro il timeout assumo che si siano bloccati, ed eseguo quindi uno script che li 
		    	// uccide e li rilancia, in modo tale da poter proseguire l'esecuzione del profiling
		    	boolean terminated_correctly = process.waitFor(this.timeout_jmeter_slaves,TimeUnit.SECONDS);
		    	if( !terminated_correctly ){
		    		System.out.println(" - timeout expired : killing and restarting all jmeter-slaves");
		    		// eseguire lo script che uccide i JMeter-server bloccati e li rilancia
		    		Process killer = Runtime.getRuntime().exec("sh files/scripts/jmeter_slaves_restarter.sh");
		    		System.out.println(" - waiting for completion of the script that kills and restarts the jmeter-slaves");
		    		killer.waitFor();
		    		System.out.println(" - all JMeter-slaves are restarted ");
		    	}
		    	
		    } 
		    catch( InterruptedException e ){
		      System.err.println(" - ERROR : " +e);  // "Can'tHappen"
		      return;
		    }
		    progressController.finished();		    
		    process.destroy();
		  	   
		    System.out.println(" - end execution of Load Generation ");	   
			
		}
		catch( IOException e ){
			System.err.println(" - ERROR : "+e);
		}
		
		this.isRunning = false;
		
		//System.out.println("  ---------------------------------\n");
	}
	
	public boolean isRunning(){ return this.isRunning; }
	
	private static String generateJMeterParameters(boolean noGui, File jmxFile, String rmi_server_hostname, 
												   String jmeter_slaves_IPs, int req_rate, int duration, 
												   int duration_ramp, int thread_number ) {
		
		jmxFile = FileUtils.getAbsolutePath(jmxFile);
		
		String params = "";
		if( noGui ){ params += " -n"; }
		
		params  += " -t " + jmxFile.toString()
			    +  " -Grate="+req_rate
		   	    +  " -Gduration_constant="+duration
			    +  " -Gduration_ramp="+duration_ramp
			    +  " -Gthread_number="+thread_number
				+  " -R "+jmeter_slaves_IPs
				+  " -Djava.rmi.server.hostname="+rmi_server_hostname;
		
		return params;
	}
	
}