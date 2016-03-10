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
	
	public void runTest(){
		this.isRunning = true;
		this.jMeter.runJMeter();
		while( this.jMeter.isRunning() ){ 
			try { Thread.sleep(1000); /* aspetto */} 
			catch (InterruptedException e) {} 
		}
		this.isRunning=false;
	}
	
	/** main di prova **/
	public static void main(String[] args){
		System.out.println("\n ************** JMETER RUNNER ***************\n");
		
		int[] input_rates = {1000, 2000, 3000};
		
		String properties_file_path = "files/PropertyFiles/jmeter.props";
		JMRunner runner = new JMRunner(properties_file_path);
		
		File prop_file = new File(properties_file_path);
		
		boolean success;
		int n=0;
		
		// per ogni input rate nell'array, eseguo una run
		for(int input_rate : input_rates){
			n++;
			success = false;
			while(!success){ success = FileUtils.setProperty(prop_file, "rate", ""+input_rate); }

			runner.runTest();
			
			// aspetto 10 secondi tra un test e il successivo
			if( n < input_rates.length ){
				try {
					System.out.println(" - waiting 10 seconds until the next test run ... \n");
					Thread.sleep(10000);
				} catch (InterruptedException e) { e.printStackTrace(); }
			}
		}
	
		System.out.println("\n *********************************************\n");	
		System.exit(0);
	}

}
