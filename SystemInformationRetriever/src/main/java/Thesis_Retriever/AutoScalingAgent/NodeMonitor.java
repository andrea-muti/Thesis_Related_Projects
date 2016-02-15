package Thesis_Retriever.AutoScalingAgent;

import Thesis_Related.SystemInformationRetriever.Config;
import Thesis_Related.SystemInformationRetriever.Retriever;

public class NodeMonitor {
	
	private static Retriever nodeStatus = new Retriever();
	
	/* Timers to keep track of duration of breach */
	private static int memMaxBreachTimer = 0;
	private static int memMinBreachTimer = 0;
	private static int diskMaxBreachTimer = 0;
	private static int diskMinBreachTimer = 0;
	@SuppressWarnings("unused")
	private static int cpuMaxBreachTimer = 0;
	@SuppressWarnings("unused")
	private static int cpuMinBreachTimer = 0;
	
	/**
	 * Perfom 1 monitor-cycle
	 */
	public void monitor() {
		monitorMemory();
		monitorDisk();
		monitorCPU();
	}
	
	/**
	 * Monitor current CPU Usage
	 */
	@SuppressWarnings("unused")
	private void monitorCPU(){
		double cpuUsage = nodeStatus.getCPUUsage();
		
		// controllo del cpuUsage e se rompe qualche threshold
		
	}
	
	/**
	 * Monitor current disk-usage in bytes
	 * @return
	 */
	private void monitorDisk() {
		Long diskUsed = nodeStatus.getDiskSpaceUsed();
		
		// Minimums breach
		if(diskUsed < Config.min_disk_space_used) {
			diskMaxBreachTimer = 0;
			diskMinBreachTimer += Config.intervall_timer;
			
			// Scale down
			if(diskMinBreachTimer > Config.threshold_breach_limit) {
				System.out.println("     ---> diskMinBreachTimer ("+diskMinBreachTimer+") > threshold_breach_limit ("+Config.threshold_breach_limit+")--> SCALE DOWN");
				diskMinBreachTimer = 0;
			}
		}
		// Maximum breach
		else if(diskUsed > Config.max_disk_space_used) {
			diskMinBreachTimer = 0;
			diskMaxBreachTimer += Config.intervall_timer;
			
			//Scale up
			if(diskMaxBreachTimer > Config.threshold_breach_limit) {
				System.out.println("     ---> diskUsed > Config.max_disk_space_used && diskMaxBreachTimer > Config.threshold_breach_limit --> SCALE DOWN");
				diskMaxBreachTimer = 0;
			}
		}
		else {
			diskMinBreachTimer = 0;
			diskMaxBreachTimer = 0;
		}
	}
	/**
	 * Monitor current memory status
	 * @return
	 */
	private void monitorMemory() {
		Double memUsed = nodeStatus.getMemoryUsage();
		
		// Min breach
		if(memUsed < Config.min_memory_usage) {
			memMaxBreachTimer = 0;
			memMinBreachTimer += Config.intervall_timer;
			
			if(memMinBreachTimer > Config.threshold_breach_limit) {
				System.out.println("   ----> memMinBreachTimer ("+memMinBreachTimer+")> threshold_breach_limit ("+Config.threshold_breach_limit+"): SCALE DOWN ");
				memMinBreachTimer = 0;
			}
		} 
		
		// Max breach
		else if(memUsed > Config.max_memory_usage) {
			memMinBreachTimer = 0;
			memMaxBreachTimer += Config.intervall_timer;
			
			if(memMaxBreachTimer > Config.threshold_breach_limit) {
				System.out.println("   ----> memUsed ("+memUsed+")> Config.max_memory_usage ("+Config.max_memory_usage+") && memMaxBreachTimer ("+memMaxBreachTimer+") > Config.threshold_breach_limit ("+Config.threshold_breach_limit +"): SCALE UP ");
				memMinBreachTimer = 0;
			}
		}
		else {
			memMaxBreachTimer = 0;
			memMinBreachTimer = 0;
		}
	}
}