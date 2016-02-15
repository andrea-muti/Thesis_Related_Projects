package Thesis_Retriever.AutoScalingAgent;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



import Thesis_Related.SystemInformationRetriever.Config;

public class Agent {

	private static ScheduledExecutorService executor;
	private static PerformanceRetrieverServer server;

	private static int INTERVALL_TIMER = Config.intervall_timer;

	public static void init_sigar(){
    	String s= "/home/andrea-muti/workspace/SystemInformationRetriever/lib";
        System.setProperty("java.library.path", System.getProperty("java.library.path") + File.pathSeparator + s);
    }
	
	public static void main(String[] args) {
		init_sigar();
		System.out.println("AutoScalingAgent initializing");
		try {
			server = new PerformanceRetrieverServer();
		} catch (IOException e) {
			System.err.println("Failed to initialize agent server");
		}
		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(server, 0, INTERVALL_TIMER, TimeUnit.SECONDS);
		System.out.println("AutoScalingAgent initized");
	}
}