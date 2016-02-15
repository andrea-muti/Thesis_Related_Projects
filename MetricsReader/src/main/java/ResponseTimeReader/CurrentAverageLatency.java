package ResponseTimeReader;

public class CurrentAverageLatency {
	
	String current_time;
	int number_of_executed_operations;
	long average_latency;
	
	CurrentAverageLatency(int nmbr, long avLat, String time){
		this.number_of_executed_operations=nmbr;
		this.average_latency=avLat;
		this.current_time=time;
	}
	
	public String getTime(){
		return this.current_time;
	}
	
	public  int getOpsNumber(){
		return this.number_of_executed_operations;
	}
	
	public long getLatency(){
		return this.average_latency;
	}	
		
}