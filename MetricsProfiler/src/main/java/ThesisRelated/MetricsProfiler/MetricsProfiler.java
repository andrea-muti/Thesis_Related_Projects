package ThesisRelated.MetricsProfiler;

/**
 * MetricsProfiler
 * 		goal : collect data to be used in the training phase of the ANN
 *
 * @author andrea-muti
 */

public class MetricsProfiler {
    public static void main( String[] args ){
        System.out.println("This is the MetricsProfiler");
        
        int input_rate;				// preso come parametro
        int num_nodes;				// leggo dal java_driver
        long cpu_level; 			// leggo da cassandra jmx
        int throughput_total;		// calcolo leggendo da cassandra jmx
        double rt_mean;				// leggo da java_driver
        double rt_95p;				// leggo da java_driver
        
        
        
        
        
    }
}
