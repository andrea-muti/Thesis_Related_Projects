package cluster_profiler;

import java.io.File;
import java.util.Properties;

import ThesisRelated.MetricsCollector.MetricsCollector;
import jmeter_automation.FileUtils;
import jmeter_automation.JMRunner;

/**
 * CLUSTER PROFILER 
 * 
 * start development : 08/03/2016
 * @author andrea-muti
 */

public class ClusterProfiler {
	
	public static void main(String[] args){
		
		final int SECOND = 1000; // how many milliseconds are in one second
		
		System.out.println("\n ----------------------------------------------");
		System.out.println(" -             CLUSTER PROFILER               -");
		System.out.println(" ----------------------------------------------\n");
				
		/** JMRunner **/
		String properties_jmeter = "files/PropertyFiles/jmeter.props";
		JMRunner load_generator = new JMRunner(properties_jmeter);
		
		/** MetricsCollector **/
		String properties_collector = "files/PropertyFiles/collector.properties";
		MetricsCollector collector = new MetricsCollector(properties_collector);
		
		
		File f1= new File(properties_jmeter);
		Properties prop_jmeter = FileUtils.loadProperties(f1);
		//Properties prop_collector = FileUtils.loadProperties(new File(properties_collector));	
		
		int rampup_seconds, rampdown_seconds;
		rampup_seconds = rampdown_seconds = Integer.parseInt(prop_jmeter.getProperty("rampDuration"));			
		int constant_duration = Integer.parseInt(prop_jmeter.getProperty("testDuration"));
		
		int intersampling_collection = 2;
		int inter_run_interval = 10;
		int bonus_seconds = 12;
		int bonus_seconds_down = 10;		
		
		//int[] input_rates = { 10000, 15000, 20000, 25000, 30000, 35000, 40000,  45000, 50000, 55000, 60000, 65000, 70000 };
		/*int[] input_rates = { 2000, 4000, 6000, 8000, 10000, 12000, 14000, 16000, 18000, 20000, 22000, 24000, 26000,
							  28000, 30000, 32000, 34000, 36000, 38000, 40000, 42000, 44000, 46000, 48000, 50000,
							  52000, 54000, 56000, 58000, 60000, 62000, 64000, 66000, 68000, 70000};
		*/
		
		int[] input_rates = {  8000, 10000, 12000, 14000, 16000, 18000, 22000, 24000, 26000,
				  		28000, 30000, 38000, 40000, 42000, 44000, 46000, 48000, 50000,
				  		52000, 54000, 56000, 58000, 60000, 62000, 66000, 68000, 70000};
		int n = 0;

		for(int input_rate : input_rates){
			n++;
			
			System.out.println(" ----------------------------------------------");
			System.out.println("\n - starting profiling for Input Rate: "+input_rate+" req/sec\n");
					
			MetricsCollectorRunnable collector_thread = new MetricsCollectorRunnable(collector, input_rate);
			LoadGeneratorThread load_generator_thread = new LoadGeneratorThread(load_generator, input_rate);	
				
			// start del thread che chiamerà il Load Generator
			load_generator_thread.start();
			
			boolean workload_terminated = false;
			
			// prima di collezionare le metrics, aspetto che la ramp-up iniziale sia terminata
			int duration_rampup = (rampup_seconds*SECOND) + (bonus_seconds*SECOND);
			try {
				System.out.println(" -- wait the ending of the initial ramp-up period before start collecting metrics\n");
			 	Thread.sleep( duration_rampup );
			} catch( InterruptedException e1 ){}
			
		
			// per evitare di collezionare durante la rampdown devo smettere di prima dell'effettivo termine
			// del load generator	
			int average_collection_duration = 14;
			int duration_defect = constant_duration - average_collection_duration;
			
			long start = System.currentTimeMillis();
			while( !workload_terminated ){
	
				// controllo se è FINITO il periodo di input rate constante
				// in tal caso NON devo collezionare le metrics, ma aspettare la ramp-down,
				// aspettare che il load_generator termini, e in fine uscire dal wile
				if( (System.currentTimeMillis() - start) > duration_defect*SECOND ){
					
					// se è FINITO il peridio con input rate costante 
					// sto nella ramp-down e non mi interessa collezionare le metrics		
					try {	
						// aspetto che la ramp-down termini [ dura almeno duration_rampdown secondi, 
						// ci aggiungiamo un po' di secondi bonus per sicurezza ]
						int duration_rampdown = rampdown_seconds*SECOND + bonus_seconds_down*SECOND;
						System.out.println("\n -- wait the ending of the ending ramp-down period");
						Thread.sleep( duration_rampdown );
						
						// aspetto che il load generator abbia finito completamente 
						System.out.println(" -- wait termination of current load_generator run");
						while(load_generator.isRunning()){	
							try { Thread.sleep( 1*SECOND ); } 
							catch (InterruptedException e) {}				
						}
					}
					catch( InterruptedException e ){}
					break;
				} 
				else{ // se invece sono ancora nel periodo di input rate COSTANTE colleziono le metrics
					System.out.println("\n - collecting metrics started -");
					collector_thread.start();
				}
						
				// tra una invocazione e la successiva del collector aspetto intersampling_collection secondi
				try {
					Thread.sleep( intersampling_collection*SECOND );
				} catch (InterruptedException e) {}
					

			}// fine workload generation corrente 
			
			System.out.println("\n -------------------------------------------------");
				
			// a meno che non sia l'ultima run, aspetto inter_run_interval secs tra una run e l'altra
			if( n < input_rates.length ){
				try {
					System.out.println("\n - waiting "+inter_run_interval+" seconds until the next test run ... \n");
					Thread.sleep( inter_run_interval*SECOND );
				} catch (InterruptedException e) {}
			}
				
		}// End For
		
		System.out.println(" - cluster profiling process terminated");
		System.out.println(" -------------------------------------------------");
		
		System.exit(0);
		
	}// end main
	
} // end ClusterProfiler class


// sottoclassi utilizzate

/*------------------------------------------------------------------------------------*/

class MetricsCollectorRunnable implements Runnable{
	
	MetricsCollector collector;
	int request_rate;
	public boolean running;
   
	MetricsCollectorRunnable(MetricsCollector c, int req_rate){ 
		this.collector = c;
		this.request_rate = req_rate;
		this.running=false;
	}
   
	public void start(){
		this.run();
	}
	
	public void run(){
		this.running=true;
		this.collector.runCollector(this.request_rate);
		this.running=false;
   }
}

/*------------------------------------------------------------------------------------*/
	
class LoadGeneratorThread implements Runnable{
	Thread mythread ;
	JMRunner load_generator;
	int request_rate;
	boolean isRunning;
	   
	LoadGeneratorThread(JMRunner lg, int req_rate){ 
		this.mythread = new Thread(this, "MetricsControllerThread");
		this.load_generator = lg;
		this.request_rate = req_rate;
		this.isRunning = false;
	}
	
	public boolean isRunning(){
		return this.isRunning;
	}
	
	public void start(){
		this.isRunning = true;
		this.mythread.start();
	}
	   
	public void run(){
		this.isRunning =true;
		
		// update del properties file del JMController interno al JMRunner 
		File prop_file = this.load_generator.getPropertiesFile();
		boolean success = false;
		while(!success){
			success = FileUtils.setProperty(prop_file, "rate", ""+request_rate);
		}
		
		this.load_generator.runTest();
		
		while( this.load_generator.isRunning() ){ 
			try { Thread.sleep(1000); } 
			catch (InterruptedException e) {}
		}
		
		this.isRunning = false;
   }
}