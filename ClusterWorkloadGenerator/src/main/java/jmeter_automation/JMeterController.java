package jmeter_automation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.StringTokenizer;


public class JMeterController {

	/**    instance variables    **/
	Properties jmeterProperties;
	File jmeterPropertiesFile;
	private boolean noGui;
	private File 	jMeterPath;
	private File 	jmxFile; 
	private String java_rmi_server_hostname;
	private String jmeter_slaves_IPs;
	@SuppressWarnings("unused")
	private int slaves_number;
	private boolean isRunning;
	
	
	/** public constructor **/
	public JMeterController(File jmeterProperties) {
		this(FileUtils.loadProperties(jmeterProperties));
		this.jmeterPropertiesFile=jmeterProperties;
	}
	
	/** private constructor **/
	private JMeterController(Properties jmeterProperties) {
		super();
		this.jmeterProperties = jmeterProperties;
		this.noGui = Boolean.parseBoolean(jmeterProperties.getProperty("noGui","true")); // OK
		this.jMeterPath = new File(jmeterProperties.getProperty("jMeterPath")); // OK
		this.jmxFile = new File(jmeterProperties.getProperty("jmxFile"));	// OK
		this.java_rmi_server_hostname = jmeterProperties.getProperty("java_rmi_server_hostname"); // OK
		this.jmeter_slaves_IPs = jmeterProperties.getProperty("jmeter_slaves_IPs");	// OK
		this.slaves_number = count_slaves(this.jmeter_slaves_IPs);
		this.isRunning = false;
		kill_and_restart_jmeter_slaves_by_script();
	}
	
	private void kill_and_restart_jmeter_slaves_by_script(){
		System.out.print(" - [JMeterController] initial killing & restarting of jmeter-slaves: ");
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
		
		// Parameters Generation
		String params = generateJMeterParameters( this.noGui, this.jmxFile, this.java_rmi_server_hostname, 
												  this.jmeter_slaves_IPs);

		long test_duration = 1441 * 8 * 1000; // test duration è calcolabile con n_righe file * durata ogni riga
		long total_duration = 20*1000 + test_duration; // è la durata del test jmeter
		
		// invokes the execution of JMeter Master ( that will call the slaves )
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
												   String jmeter_slaves_IPs ) {
		
		jmxFile = FileUtils.getAbsolutePath(jmxFile);
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		
		
		String jmxFilePath = s+"/"+jmxFile.toString();
		
		String params = "";
		if( noGui ){ params += " -n"; }
		
		params  += " -t " + jmxFilePath
				+  " -R "+jmeter_slaves_IPs
				+  " -Djava.rmi.server.hostname="+rmi_server_hostname;
		
		return params;
	}
	
}