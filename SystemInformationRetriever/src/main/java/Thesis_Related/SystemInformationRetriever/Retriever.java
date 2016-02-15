package Thesis_Related.SystemInformationRetriever;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.DirUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

/**
 * System Information Retriever using SIGAR APIs
 * 
 * 19/01/2016
 */

public class Retriever {
	
	private static Long BYTES_TO_MB = 1024*1024L;
	public static Long min_disk_space_used = 60L;
	public static Long max_disk_space_used = 20000L;
	
	private Sigar sigar;
	
	public Retriever(){
		this.sigar = new Sigar();
	}
	
	/**
	 * Retrieve current memory usage (percentage)
	 * 
	 * @return double : percentage of memory usage
	 */
	public Double getMemoryUsage() {
		Double memUsed = 0.0;
		Mem mem;
		try {
			mem = this.sigar.getMem();
			memUsed = doubleFormatted(Double.parseDouble(String.valueOf(mem.getUsedPercent()).replace(",", ".")));
		} catch (Exception e) {
			System.err.println("Failed to get memory usage - "+e.getMessage());
		}
		System.out.println("Memory usage: "+memUsed+"%");
		return memUsed;
	}
	
	/**
	 * Retrieve Cpu-usage
	 * 
	 * @return double : percentage of cpu usage
	 */
	public Double getCPUUsage() {
		Double cpuUsed = 0.00;
		try {
			CpuPerc[] cpu_perc_array = sigar.getCpuPercList();
			for(int i = 0; i<cpu_perc_array.length;i++){
				cpuUsed += 100*doubleFormatted(cpu_perc_array[i].getCombined());
			}
			cpuUsed = cpuUsed / cpu_perc_array.length;
		} catch (SigarException e) {
			System.err.println("Failed to get CPU-usage - "+e.getMessage());
		}
		System.out.println("Average CPU used: "+cpuUsed+"%");
		return cpuUsed;
	}
	
	
	
	/**
	 * Retrieve disk-usage in percentage
	 * 
	 * @return double : percentage of disk used with respect to the defined max_disk_space_used
	 */
	public Double getDiskUsage() {
		Long spaceInBytes = 0L;

		try {
			DirUsage dirUsage;
			// path to data directory
			String dir = "/home/andrea-muti/0.5GB_3nodi/cassandra-node-1/data";
			dirUsage = sigar.getDirUsage(dir);
			spaceInBytes = dirUsage.getDiskUsage();

		} catch (SigarException e) {
			System.err.println("Failed to retrieve disk space used in megabytes - "+e.getMessage());
		}
		
		Double diskUsed = ((spaceInBytes / BYTES_TO_MB.doubleValue()) / max_disk_space_used) * 100;
		diskUsed = doubleFormatted(diskUsed);
		System.out.println("Disk space used: "+diskUsed+"%");
		return diskUsed;
	}
	
	/**
	 * Retrieve disk usage in size (Megabytes)
	 * 
	 * @return long : disk space used
	 */
	public Long getDiskSpaceUsed() {
		Long space = 0L;
		Double diskSpace = 0.0;
		try {
			DirUsage dirUsage;
			// path to data directory
			String dir = "/home/andrea-muti/0.5GB_3nodi/cassandra-node-1/data";
			dirUsage = sigar.getDirUsage(dir);
			space = (dirUsage.getDiskUsage() / BYTES_TO_MB);
			
			// For debug/logging purpose
			diskSpace = dirUsage.getDiskUsage() / BYTES_TO_MB.doubleValue();
			diskSpace = doubleFormatted(diskSpace);
			
		} catch (SigarException e) {
			System.err.println("Failed to retrieve disk space used in megabytes - "+e.getMessage());
		}

		System.out.println("Disk usage "+diskSpace+" MB");
		return space;
	}
	
	
	
	private Double doubleFormatted(Double val) {
		NumberFormat formatter = new DecimalFormat("#000.00");    
		String valstr = formatter.format(val).replace(",", ".");
		return Double.valueOf(valstr);
	}
	
	public static void init_sigar(){
    	String s= "/home/andrea-muti/workspace/SystemInformationRetriever/lib";
        System.setProperty("java.library.path", System.getProperty("java.library.path") + File.pathSeparator + s);
    }
	
    public static void main( String[] args ){
    	
    	init_sigar();
        
    	System.out.println( "\n ---- SYSTEM INFORMATION RETRIEVER ----\n\n" );
        Retriever r = new Retriever();
        r.getMemoryUsage();
        r.getCPUUsage();
        r.getDiskUsage();
        r.getDiskSpaceUsed();
    }
}
