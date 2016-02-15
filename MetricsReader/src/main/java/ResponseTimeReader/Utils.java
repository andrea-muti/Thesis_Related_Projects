package ResponseTimeReader;

import java.util.List;

public class Utils {

/***************************************************************************************
 ***************************************************************************************
 * calculate the latency of the current operation and places it in a list 
 * @author Rubia
 * @param startTime - start time of the operation
 * @param lantenciesList - list of the measured latencies within the current second
 */
	
	public static void collectLatencyStatistics(long startTime, List<Long> lantenciesList){
		
		long currentLatency=System.nanoTime()-startTime;
		lantenciesList.add(currentLatency);
	}
	

	
/*********************************************************************************************
 * stores the averages for the various seconds
 * @author Rubia
 * @param latencies - list of calculated latencies within the second
 * @param statisticsResults - list of average latencies among second with the current hour in hh:mm:ss:ms
 */
	
	public static CurrentAverageLatency perSecondLatencyAverage(List <Long> latencies){
	
		int size= latencies.size();
		long averageLatency=0;
		
		for(Long latency: latencies)				
			averageLatency+=latency;			//somma latenze
			
		averageLatency=averageLatency/size;				//media latenze

		for(int i=(latencies.size()-1); i>=0 ;i--)
			latencies.remove(i);	//contemporary clean the list
			
		//current hour ---> hh:mm:ss:ms
		MyHour hour=new MyHour();
		hour.setCurrentMoment();	
		
		return new CurrentAverageLatency(size,averageLatency,hour.toString());
		
	}
}
