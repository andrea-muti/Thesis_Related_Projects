package Thesis_Retriever.AutoScalingAgent;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import Thesis_Related.SystemInformationRetriever.Config;

public class PerformanceRetrieverServer implements Runnable {

	private static ScheduledExecutorService executor;

	private static PerformanceRetrieverDaemon daemon = null;

	public PerformanceRetrieverServer()throws IOException {

		System.out.println("Listen-server started");
		initAgent();
	}


	public void run() {
		
	}

	
	/**
	 * Initialize / Re-initialize daemon
	 */
	private void initAgent() {
		System.out.println("Initialize agent");
		if (null != daemon) {
			executor.shutdownNow();
		}
		daemon = new PerformanceRetrieverDaemon();
		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(daemon, 0, Config.intervall_timer, TimeUnit.SECONDS);
	}

	/**
	 * Shutdown currently running daemon (NOT the server)
	 */
	@SuppressWarnings("unused")
	private void stopAgent() {
		System.out.println("Stop agent");
		executor.shutdown();
	}
}