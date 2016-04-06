package ThesisRelated.ClusterWorkloadGenerator;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/** WorkloadTimeTracker
 * 	- given the number of seconds that correspond to 60 seconds of simulation, 
 *    it keeps track of the passage of time in the simulation of workload execution
 *   
 * @author andrea-muti
 * @since 06-04-2016
 */

public class WorkloadTimeTracker {
	
	/** instance variables **/
	private double elapsed;
	private int time_unit;
	private ScheduledExecutorService scheduler = null;
	
	/** WorkloadTimeTracker()
	 * 		public constructor
	 * 
	 * @param real_seconds : number of real seconds that correspond to 60 seconds of 
	 *             workload simulation execution
	 */
	public WorkloadTimeTracker(int real_seconds){
	    if( (!((real_seconds%2)==0)) && (!((real_seconds%3)==0)) && (!((real_seconds%5)==0)) ){
	    	System.err.println(" - ERROR : the number of seconds that correspond to 1 min "
	    			+ "of workload execution must be a divisor of 60");
	    }
	    else{
	    	this.time_unit=real_seconds;
	    	this.elapsed=0.0;
	    	this.scheduler = Executors.newScheduledThreadPool(1);
	    }
	    
	}
	
	/** reset()
	 * 	resets to 0 the counter of elapsed simulated seconds;
	 */
	public void reset(){
		this.elapsed=0;
	}
	
	/** start()
	 *  starts the simulation of workload passage of time
	 */
	public boolean start() {
		
		if(this.scheduler==null){ return false; }
		
		final Runnable timeIncrementor = new Runnable() {
			public void run() {
				if(time_unit%2==0){
					elapsed = elapsed + (60/2.0) ; 
				}
				else if(time_unit%3==0){
					elapsed = elapsed + (60/3.0);
				}
				else if(time_unit%5==0){
					elapsed = elapsed + (60/5.0);
				}
				else{ elapsed = elapsed + 60; }
			}
		};
		
		double unit = 0;
		if(time_unit%2==0){
			unit = this.time_unit/2.0 ; 
		}
		else if(time_unit%3==0){
			unit = this.time_unit/3.0;
		}
		if(time_unit%5==0){
			unit = this.time_unit/5.0;
		}
		
		scheduler.scheduleAtFixedRate(timeIncrementor, (long) unit, (long) unit, SECONDS);
		return true;
	}
	
	/** stop()
	 *  stops the simulation of workload time passage
	 */
	public void stop(){
		scheduler.shutdownNow();
	}
	
	/** getElapsed()
	 *    returns the number of simulated seconds from the start of the time tracker
	 * @return (int) number of simulated seconds from the start of the time tracker
	 */
	public double getElapsed(){ 
		return this.elapsed; 
	}

	
	/** main di prova **/
	public static void main(String[] args){
		
		// un minuto di workload viene simulato da jmeter in 5 secondi
		int single_duration_sec = 5;
		
		WorkloadTimeTracker timeTracker = new WorkloadTimeTracker(single_duration_sec);
		timeTracker.start();
		System.out.println(" - started");
		
		int n_iterations = 5;
		int n_sec_between_sampling = 5;
		
		for(int i = 0; i<n_iterations; i++){
			System.out.println("[ after "+(i*n_sec_between_sampling)+" real sec ] WL time : " + timeTracker.getElapsed());
			
			try{ Thread.sleep(1000*n_sec_between_sampling); }
		    catch(Exception e){}
		   
		}
		System.out.println("[ after "+(n_iterations*n_sec_between_sampling)+" real sec ] WL time : " + timeTracker.getElapsed());

		timeTracker.stop();
		System.out.println(" - stopped");
		System.out.println(" - wait 10");
		try{ Thread.sleep(1000*10); }
	    catch(Exception e){}
		System.out.println("[ after 10 secs from the stop ] WL time : " + timeTracker.getElapsed());

		
	}

}