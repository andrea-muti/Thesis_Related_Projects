package jmeter_automation;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;


public class JMeterController {

	
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
	private int test_duration_sec;
	private int ramp_duration_sec;
	private boolean isRunning;
	private int timeout_jmeter_slaves;
	
	public JMeterController(File jmeterProperties) {
		this(FileUtils.loadProperties(jmeterProperties));
		this.jmeterPropertiesFile=jmeterProperties;
	}
	
	
	public JMeterController(Properties jmeterProperties) {
		super();
		this.jmeterProperties = jmeterProperties;
		this.noGui = Boolean.parseBoolean(jmeterProperties.getProperty("noGui"));
		this.jMeterPath = new File(jmeterProperties.getProperty("jMeterPath"));
		this.jmxFile = new File(jmeterProperties.getProperty("jmxFile"));
		this.java_rmi_server_hostname = jmeterProperties.getProperty("java_rmi_server_hostname");
		this.jmeter_slaves_IPs = jmeterProperties.getProperty("jmeter_slaves_IPs");
		this.slaves_number = count_slaves(this.jmeter_slaves_IPs);
		this.total_request_rate = Integer.parseInt(jmeterProperties.getProperty("rate"));
		this.slave_request_rate = this.total_request_rate / this.slaves_number;
		this.test_duration_sec = Integer.parseInt(jmeterProperties.getProperty("testDuration"));
		this.ramp_duration_sec = Integer.parseInt(jmeterProperties.getProperty("rampDuration"));
		this.timeout_jmeter_slaves = Integer.parseInt(jmeterProperties.getProperty("timeoutJMeterSlaves"));
		this.isRunning = false;
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
		
		//System.out.println("    - total request rate : "+this.total_request_rate+" reqs/sec ");
		//System.out.println("    - jmeter-slaves : "+this.slaves_number);
		//System.out.println("    - each slave will execute "+this.slave_request_rate+" reqs/sec ");
		//System.out.println("    - constant request rate duration : "+this.test_duration_sec+" sec");
		
		// PROVA MIA GENERAZIONE DI PARAMETRI
		String params = generateJMeterParameters_my_version(this.noGui, this.jmxFile, this.java_rmi_server_hostname, 
														    this.jmeter_slaves_IPs, this.slave_request_rate, this.test_duration_sec, this.ramp_duration_sec);

		long test_duration_ms = this.test_duration_sec * 1000;
		long ramp_up_and_down_durations_ms = 2 * this.ramp_duration_sec * 1000;
		long total_duration = 20*1000 + test_duration_ms + ramp_up_and_down_durations_ms; // è la durata del test jmeter
		
		// run JMeter
		try {
			ProgressController progressController = new ProgressUI();

			String exec_command = jMeterPath.toString() + params;
			
			System.out.println(" - start execution of JMeter load generation");
					
			long start = System.currentTimeMillis();
			Process process = Runtime.getRuntime().exec(exec_command);
			progressController.processStarted(process, start + total_duration);
			
			while( (System.currentTimeMillis() - start) <= total_duration ){
				try {
					if(process.isAlive()){
						Thread.sleep( total_duration/100 );
					}
					else{ break;}
				} catch (InterruptedException e) {}	
				progressController.currentProgress( (System.currentTimeMillis() - start) / (double) total_duration );
			}
		
		    try {
		    	// aspetto indefinitamente che i jmeter-slaves terminino --> a volte si blocca!
		    	//process.waitFor();
		    	
		    	
		    	// aspetto al max timeout_jmeter_slaves secondi che i jmeter-slaves terminino
		    	// se non sono terminati entro il timeout, eseguo uno script che li uccide e li rilancia
		    	// in tal modo posso proseguire con l'esecuzione del profiling
		    	boolean terminated_correctly = process.waitFor(this.timeout_jmeter_slaves,TimeUnit.SECONDS);
		    	if(!terminated_correctly){
		    		System.out.println(" - timeout expired : killing and restarting all jmeter-slaves");
		    		// eseguire lo script che uccide i JMeter-server bloccati e li rilancia
		    		Process killer = Runtime.getRuntime().exec("sh files/scripts/jmeter_slaves_restarter.sh");
		    		System.out.println(" - waiting for script that kills and restarts slaves");
		    		killer.waitFor();
		    		System.out.println(" - JMeter-slaves restarted ");
		    	}
		    	
		    } 
		    catch (InterruptedException e) {
		      System.err.println(e);  // "Can'tHappen"
		      return;
		    }
		    progressController.finished();
		    
		    process.destroy();
		  	   
		    System.out.println(" - end execution of JMeter load generation [ exit status : " + process.exitValue()+" ]");	   
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.isRunning = false;
		
		//System.out.println("  ---------------------------------\n");
	}
	
	public boolean isRunning(){return this.isRunning;}
	
	private static String generateJMeterParameters_my_version(boolean noGui, File jmxFile, String rmi_server_hostname, String jmeter_slaves_IPs, int req_rate, int duration, int duration_ramp) {
		
		jmxFile = FileUtils.getAbsolutePath(jmxFile);
		
		String params = "";
		if (noGui) { params += " -n"; }
		
		params += " -t " + jmxFile.toString()
					  + " -Grate="+req_rate
					  + " -Gduration_constant="+duration
					  + " -Gduration_ramp="+duration_ramp
				      + " -R "+jmeter_slaves_IPs
				      + " -Djava.rmi.server.hostname="+rmi_server_hostname;
		
		return params;
	}
	
}