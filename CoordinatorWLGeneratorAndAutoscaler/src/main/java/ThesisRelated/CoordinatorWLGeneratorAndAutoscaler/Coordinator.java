package ThesisRelated.CoordinatorWLGeneratorAndAutoscaler;

import java.util.concurrent.CountDownLatch;

/**
 * Coordinator between the WorkloadGenerator and the AutoScaler
 * @author andrea-muti
 * @since 06-04-2016
 */

public class Coordinator {
    public static void main( String[] args ){
        System.out.println( " *** COORDINATOR ***");
        
        final  CountDownLatch latch = new CountDownLatch(1);
        Thread generatorService = new Thread(new StarterThread("GeneratorService",  latch));
        Thread autoscalerService = new Thread(new StarterThread("AutoScalerService", latch));

        generatorService.start();
        autoscalerService.start();
        
        try{Thread.sleep(5000);}
        catch(Exception e){}
        latch.countDown();  // solo dopo che il latch è andato a 0 i due thread partiranno davvero
      
        
      
     }
}

/**
 * Service class which will be executed by Thread using CountDownLatch synchronizer.
 */
class StarterThread implements Runnable{
    private final String name;
    private final CountDownLatch latch;
 
    public StarterThread(String name, CountDownLatch latch){
        this.name = name;
        this.latch = latch;
    }
 
    @Override
    public void run() {
       
        System.out.println(" - " + name + " is Up");
        try {
            latch.await();          //The thread keeps waiting till it is informed
        } catch (InterruptedException e) {
        	System.err.println(" - "+this.name+" thread awaiting to start has been interrupet");
            e.printStackTrace();
        }
        System.out.println(" - "+this.name+" executing");
    }
    
 
}
