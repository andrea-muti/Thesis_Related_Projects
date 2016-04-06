package jmeter_automation;

import java.io.File;

public class JMRunner {
	
	/** instance variables **/
	private JMeterController jMeter;
	private File properties_file;
	private boolean isRunning;
	
	/** public constructor **/
	public JMRunner(String properties_file){
		this.properties_file = new File(properties_file);
		this.jMeter = new JMeterController(this.properties_file);
		this.isRunning=false;
	}
	
	public File getPropertiesFile(){
		return this.properties_file;
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	public void runWorkload(){
		this.isRunning = true;
		this.jMeter.runJMeter();
		while( this.jMeter.isRunning() ){ 
			try { Thread.sleep(1000); /* aspetto */} 
			catch (InterruptedException e) {} 
		}
		this.isRunning=false;
	}
	
	/** main di prova **/
	@SuppressWarnings("unused")
	public static void main(String[] args){
		System.out.println("\n ************** JMETER RUNNER ***************\n");
		
		
		String properties_file_path = "files/PropertyFiles/jmeter.props";
		JMRunner runner = new JMRunner(properties_file_path);
		
		File prop_file = new File(properties_file_path);	
		
		runner.runWorkload();
			
		System.out.println("\n *********************************************\n");	
		System.exit(0);
	}
}