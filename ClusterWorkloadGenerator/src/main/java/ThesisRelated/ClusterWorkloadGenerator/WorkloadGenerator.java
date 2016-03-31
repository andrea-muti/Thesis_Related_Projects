package ThesisRelated.ClusterWorkloadGenerator;

import jmeter_automation.JMRunner;

/**
 * ClusterWorkloadGenerator
 * @author andrea-muti
 * @since 30/03/2016
 */

public class WorkloadGenerator {
    public static void main( String[] args ){
        System.out.println("\n ****** WORKLOAD GENERATOR *******\n");
        
        String jmeter_properties = "files/PropertyFiles/jmeter.props";
        JMRunner jrunner = new JMRunner(jmeter_properties);
        
        jrunner.runWorkload();
        
        // non dovrei mai entrare in questo while, ma ce lo lasciamo per sicurezza
        while(jrunner.isRunning()){
        	try { Thread.sleep(1000); } 
        	catch (InterruptedException e) {}
        }
        
        System.exit(0);
    }
}
