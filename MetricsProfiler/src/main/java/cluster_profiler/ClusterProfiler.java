package cluster_profiler;

import java.io.File;

import ThesisRelated.MetricsCollector.MetricsCollector;
import jmeter_automation.FileUtils;
import jmeter_automation.JMRunner;

public class ClusterProfiler {
	
	public static void main(String[] args){
		
		System.out.println("\n -------------------------------------");
		System.out.println(" -         CLUSTER PROFILER          -");
		System.out.println(" -------------------------------------\n");
		
		/** JMRunner **/
		String properties_jmeter = "files/PropertyFiles/jmeter.props";
		JMRunner load_generator = new JMRunner(properties_jmeter);
		
		/** MetricsCollector **/
		String properties_collector = "files/PropertyFiles/collector.properties";
		MetricsCollector collector = new MetricsCollector(properties_collector);
		
		int[] input_rates = {10000, 20000, 30000};
		int n = 0;

		for(int input_rate : input_rates){
			
			System.out.println(" ------------------------------------------");
			
			n++;
			
			System.out.println("\n - start threads for input rate: "+input_rate+" req/sec");
					
			MetricsCollectorRunnable collector_thread = new MetricsCollectorRunnable(collector, input_rate);
			LoadGeneratorThread load_generator_thread = new LoadGeneratorThread(load_generator, input_rate);	
			
			// start del thread che chiamerà il Load Generator
			load_generator_thread.start();
			
			boolean workload_terminated = false;
			
			// dovrei aspettare 10+ secondi per far passare la salita iniziale
			/*
			 * int duration_rampup
			   try {
			 	 Thread.sleep(duration_rampup);
			   } catch (InterruptedException e1) {}
			
			 */
				
			while(!workload_terminated){
				
				// controllo che il Load Generator sia ancora in esecuzione
				if( !load_generator_thread.mythread.isAlive()){
					// se il load generator ha terminato, allora smetto di campionare le metrics uscedo dal while
					workload_terminated = true;
				}
				else{ // se invece il Load Generator NON ha ancora finito -> posso collezionare le metriche
					try {
						// se non c'è attualmente nessun
						if(!collector_thread.running){	
							collector_thread.start();
						} 
						else{ Thread.sleep(1000); } // aspetto che la collection corrente termini 
						// attenzione perchè questo tempo dipende da quanto ci metto a collezionare le metriche
						// da quello che ho visto, a cluster scarico, almeno 13 secs, quindi avrebbe senso aspettare un po' di piu
					} catch (InterruptedException e) {}
					
				}
				
				// tra una invocazione e la successiva del collector aspetto un dato intervallo temporale
				try {
					// intervallo tra invocazioni del metrics collector.  
					int collection_sampling_interval_msec = 3000;
					Thread.sleep(collection_sampling_interval_msec);
				} catch (InterruptedException e) {}
				
			}// fine workload generation corrente 
			
			System.out.println(" ------------------------------------------");
			
			// a meno che non sia l'ultima run, aspetto 10 secs tra una run e l'altra
			if( n < input_rates.length ){
				try {
					System.out.println("\n - waiting 10 seconds until the next test run ... \n");
					Thread.sleep(10000);
				} catch (InterruptedException e) {}
			}
				
		}// End For
		
		System.out.println(" --------------------------");
		
	}// end main
	
}



class MetricsCollectorRunnable implements Runnable{
	
	//Thread mythread ;
	MetricsCollector collector;
	int request_rate;
	public boolean running;
   
	MetricsCollectorRunnable(MetricsCollector c, int req_rate){ 
		//this.mythread = new Thread(this, "MetricsControllerThread");
		this.collector = c;
		this.request_rate = req_rate;
		this.running=false;
	}
   
	public void start(){
		//this.mythread.start();
		this.run();
	}
	
	public void run(){
		this.running=true;
		//this.collector.runCollector(this.request_rate);
		try {
				System.out.println("      ---> executing COLLECTOR for IR:"+this.request_rate);
				Thread.sleep(1000);
	
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.running=false;
   }
}
	
class LoadGeneratorThread implements Runnable{
	Thread mythread ;
	JMRunner load_generator;
	int request_rate;
	   
	LoadGeneratorThread(JMRunner lg, int req_rate){ 
		this.mythread = new Thread(this, "MetricsControllerThread");
		this.load_generator = lg;
		this.request_rate = req_rate;
	}
	
	public void start(){
		this.mythread.start();
	}
	   
	public void run(){
		
		// update del properties file del JMController interno al JMRunner 
		File prop_file = this.load_generator.getPropertiesFile();
		boolean success = false;
		while(!success){
			success = FileUtils.setProperty(prop_file, "rate", ""+request_rate);
		}
		
	   //this.load_generator.runTest();

		try {
			for(int i = 0; i<3; i++){
				System.out.println("     GENERATOR for IR:"+this.request_rate+" is still working");
				Thread.sleep(5000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
   }
}