package Thesis_Retriever.AutoScalingAgent;

public class PerformanceRetrieverDaemon implements Runnable {
	
	private static NodeMonitor monitor = new NodeMonitor();
	
	public PerformanceRetrieverDaemon() {
		System.out.println("Performance Retriever Daemon started");
	}
	
	public void run() {
			System.out.println(" - performance monitor daemon is running...");
			monitor.monitor();
	}
}
