package Thesis_Related.SigarApiExamples;

import java.io.File;

import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

/**
 * Examples of utilization of the SIGAR API
 *
 */
public class SigarExamples {
	
	private static Sigar sigar;
	
    public static void main( String[] args ) throws Exception{
    	   	
    	init_sigar();
    	
        System.out.println("\n**** SIGAR API EXAMPLE ****\n");
        
        System.out.println("\n * CUP USED: "+getCPUUsage());
        System.out.println("\n * MEMORY USED [%] : "+getMemoryUsage());
        
        getInformationsAboutMemory();
        
    }
    
    public static void init_sigar(){
    	String s= "/home/andrea-muti/workspace/SigarApiExamples/lib";
        System.setProperty("java.library.path", System.getProperty("java.library.path") + File.pathSeparator + s);
    	sigar = new Sigar();
    }
    
    public static Double getCPUUsage() {
		Double cpuUsed = 0.0;

		try { cpuUsed = sigar.getCpuPerc().getCombined();
		} catch (SigarException e) { System.err.println(e.getMessage());}
	
		return cpuUsed;
	}
    
    public static Double getMemoryUsage() {
		Double memUsed = 0.0;
		Mem mem;
		try {
			mem = sigar.getMem();
			memUsed = mem.getUsedPercent();
			
		} catch (Exception e) {
			System.err.println("Failed to get memory usage - "+ e.getMessage());
		}
	
		return memUsed;
	}
    
    public static void getInformationsAboutMemory() {

        System.out.println("\n ******* Informations about the Memory: *******\n");

        Mem mem = null;
        try { mem = sigar.getMem(); } 
        catch (SigarException se) { se.printStackTrace(); }

        System.out.println("  - Actual total free system memory: " + mem.getActualFree() / 1024 / 1024+ " MB");
        System.out.println("  - Actual total used system memory: " + mem.getActualUsed() / 1024 / 1024 + " MB");
        System.out.println("  - Total free system memory ......: " + mem.getFree() / 1024 / 1024+ " MB");
        System.out.println("  - System Random Access Memory....: " + mem.getRam()+ " MB");
        System.out.println("  - Total system memory............: " + mem.getTotal() / 1024 / 1024+ " MB");
        System.out.println("  - Total used system memory.......: " + mem.getUsed() / 1024 / 1024+ " MB");

        System.out.println("\n **********************************************\n");
     
    }
    
}
