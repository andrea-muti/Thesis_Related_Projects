package jmeter_automation;

import java.io.File;

public class JMRunner {
	
	private JMeterController jMeter;
	
	public JMRunner(String properties_file){
		File prop_file = new File(properties_file);
		this.jMeter = new JMeterController(prop_file);
	}
	
	public void runTest(){
		this.jMeter.runJMeter();
	}
	
	public static void main(String[] args){
		System.out.println("\n ************** JMETER RUNNER ***************\n");
		
		int[] input_rates = {1000, 2000};
		
		String properties_file_path = "files/PropertyFiles/jmeter.props";
		JMRunner runner = new JMRunner(properties_file_path);
		
		File prop_file = new File(properties_file_path);
		
		boolean success;
		int n=0;
		for(int input_rate : input_rates){
			n++;
			success = false;
			while(!success){
				success = FileUtils.setProperty(prop_file, "rate", ""+input_rate);
			}

			runner.runTest();
			
			if( n < input_rates.length ){
				try {
					System.out.println(" - waiting 10 seconds until the next test run ... \n");
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		

		System.out.println("\n *********************************************\n");
		
		System.exit(0);
		
	}
	
}
