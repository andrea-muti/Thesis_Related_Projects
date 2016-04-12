package ThesisRelated.CoordinatorWLGeneratorAndAutoscaler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import ThesisRelated.ClusterWorkloadGenerator.WorkloadGenerator;
import ThesisRelated.ClusterAutoScaler.AutoScaler;

/**
 * Coordinator between the WorkloadGenerator and the AutoScaler
 * @author andrea-muti
 * @since 06-04-2016
 */

public class Coordinator {
    public static void main( String[] args ){
    	System.out.println(" *----------------------*");
        System.out.println(" *-- TEST COORDINATOR --*");
        System.out.println(" *----------------------*\n");
        
        int number_hour_initial_shift = 8;
        
        final  CountDownLatch latch = new CountDownLatch(1);
        Thread generatorModule = new Thread(new GeneratorExecutorThread(latch, number_hour_initial_shift));
        Thread autoscalerModule = new Thread(new AutoscalerExecutorThread(latch, number_hour_initial_shift));

        try{Thread.sleep(4000);}
        catch(Exception e){}
        System.out.println("");

        generatorModule.start();
        autoscalerModule.start();

        try{Thread.sleep(4000);}
        catch(Exception e){}
        System.out.println("");
        
        latch.countDown();  // solo dopo che il latch è andato a 0 i due thread partiranno davvero
        
       
        
     }
}

//---------------------------------------------------------------------------------------------------------

/**
 * Executor Thread for the WORKLOAD GENERATOR
 */
class GeneratorExecutorThread implements Runnable{
    private final CountDownLatch latch;
    private WorkloadGenerator generator;
    private int number_hours_initial_shift;
 
    // reading various required properties
    String generator_properties_path = "files/properties_files/generator.props";   
    String jmeter_properties_path = "files/properties_files/jmeter.props";
     
    public GeneratorExecutorThread(CountDownLatch latch, int number_hours_initial_shift){
        this.latch = latch;
        this.number_hours_initial_shift=number_hours_initial_shift;
        this.generator = new WorkloadGenerator(generator_properties_path, jmeter_properties_path);
        
        // creazione del JMeter execution plan che verrà usato dal JMRunner (JMeterController)
        System.out.print(" - [WorkloadGenerator Executor] generation JMeter execution plan : ");
    	boolean plan_creation = generator.generate_jmeter_plan_with_initial_shift(this.number_hours_initial_shift);
        if(!plan_creation){
        	System.err.println(" FAILED");
        	System.exit(0);
        }
        else{System.out.println(" DONE"); }
        
        System.out.println(" - [WorkloadGenerator Executor] WorkloadGenerator initialized");
        System.out.println(" - [WorkloadGenerator Executor] start of generated workload will be shifted by "+this.number_hours_initial_shift+" hours with respect to the workload file");
        long approx_total_dur_sec = generator.get_avg_workload_duration_sec() - ((60*this.number_hours_initial_shift)*generator.get_single_duration_sec());
        double approx_dur_min = approx_total_dur_sec/60.0;
        String form_dur_min = String.format("%.0f", approx_dur_min);
        String form_dur_hours = String.format("%.2f", approx_dur_min/60.0).replace(",", ".");
        System.out.println(" - [WorkloadGenerator Executor] 1 minute of workload will be generated in "+generator.get_single_duration_sec()+" sec of real time");
        System.out.println(" - [WorkloadGenerator Executor] 1 minute of real time corresponds to "+(60/generator.get_single_duration_sec())+" minutes of workload time");
        System.out.println(" - [WorkloadGenerator Executor] total approx. simulation duration: "+form_dur_min+" min ("+approx_total_dur_sec+" sec) ("+form_dur_hours+" hours)");
    }
 
    @Override
    public void run() {
        try {
        	System.out.println(" - [WorkloadGenerator Executor] WorkloadGenerator awaiting start");
            latch.await();          //The thread keeps waiting till it is informed
        } catch (InterruptedException e) {
        	System.err.println(" - [WorkloadGenerator Executor] WorkloadGenerator thread awaiting to start has been interrupet");
            e.printStackTrace();
            System.err.println(" - [WorkloadGenerator Executor] ABORTING EXECUTION");
            System.exit(0);
        }
        System.out.println(" - [WorkloadGenerator Executor] start generating workload");
        this.generator.generateWorkload();
        System.out.println(" - [WorkloadGenerator Executor] workload generation completed");
    }
}

// -------------------------------------------------------------------------------------

/**
 * Executor Thread for the AUTOSCALER
 */
class AutoscalerExecutorThread implements Runnable{
	
    private final CountDownLatch latch;
    private AutoScaler autoscaler;
    private int single_duration_sec;
    private int scaling_factor;
    private int number_hours_initial_shift;
    
    String autoscaler_props_file = "files/properties_files/autoscaler.properties";
    String predictor_props_file = "files/properties_files/predictor.properties";
    String generator_pros_file = "files/properties_files/generator.props";
    String configuration_manager_props_file = "files/properties_files/propertiesCM.properties";
    
    public AutoscalerExecutorThread(CountDownLatch latch,  int number_hours_initial_shift){
    	this.number_hours_initial_shift=number_hours_initial_shift;
        this.latch = latch;
    	this.single_duration_sec = get_single_duration_sec();
    	this.scaling_factor = get_scaling_factor();
        this.autoscaler = new AutoScaler( autoscaler_props_file, predictor_props_file, configuration_manager_props_file,
        		this.single_duration_sec,  this.scaling_factor, this.number_hours_initial_shift);
        System.out.println(" - [AutoScaler Executor] AutoScaler initialized");
    }
 
    @Override
    public void run() {
        try {
            System.out.println(" - [AutoScaler Executor] AutoScaler awaiting start");
            latch.await();          //The thread keeps waiting till it is informed
        } catch (InterruptedException e) {
        	System.err.println(" - [AutoScaler Executor] the AutoScaler thread awaiting to start has been interrupet");
            e.printStackTrace();
            System.err.println(" - [AutoScaler Executor] ABORTING EXECUTION");
            System.exit(0);
        }
        System.out.println(" - [AutoScaler Executor] AutoScaler starts execution");
        this.autoscaler.start();
        System.out.println(" - [AutoScaler Executor] AutoScaler execution end");
        
    }
    
    
    private int get_single_duration_sec(){
		File propFile = new File(generator_pros_file);
		Properties properties = new Properties();
		try { properties.load(new FileReader(propFile)); } 
		catch (IOException e) {
			System.err.println(" - [AutoScaler Executor] ERROR reading generator properties file");
			System.exit(0);
		}		
		return Integer.parseInt(properties.getProperty("single_duration_sec"));
    }
    private int get_scaling_factor(){
		File propFile = new File(generator_pros_file);
		Properties properties = new Properties();
		try { properties.load(new FileReader(propFile)); } 
		catch (IOException e) {
			System.err.println(" - [AutoScaler Executor] ERROR reading generator properties file");
			System.exit(0);
		}		
		return Integer.parseInt(properties.getProperty("scaling_factor").trim());
    }
}
