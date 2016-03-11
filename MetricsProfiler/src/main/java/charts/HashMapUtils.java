package charts;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HashMapUtils {
	
	public static void insert(Map<Integer, LinkedList<Double>> map, Integer key, Double value){
		if(!map.containsKey(key)){
			map.put(key, new LinkedList<Double>());
		}
		map.get(key).add(value);
	}
	
	public static Map<Integer,Double> compute_averages(Map<Integer, LinkedList<Double>> map_multiple){
		Map<Integer,Double> result = new Hashtable<Integer,Double>();
		
		Iterator<Entry<Integer, LinkedList<Double>>> iter = map_multiple.entrySet().iterator();
		
		while(iter.hasNext()){
			Entry<Integer,LinkedList<Double>> entry = iter.next();
			Integer key = entry.getKey();
			List<Double> values = entry.getValue();
			double average = 0;
			for(Double value : values){ average = average + value.doubleValue(); }
			average=average/values.size();
			result.put(key, average);
		}
		return result;
	}

}
