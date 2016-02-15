package my_package;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Class to represent Virtual Nodes objects
 * 
 * @author andrea-muti
 */



public class VirtualNode {
	
	private List<Range> ranges =  new LinkedList<Range>();		
	private String ip_address = null;
	
	public VirtualNode(String ip, long start, long end){
		this.set_IP_address(ip);
		this.ranges.add(new Range(start,end));
		
	}

	public String get_IP_address() {
		return ip_address;
	}

	public void set_IP_address(String ip_address) {
		this.ip_address = ip_address;
	}
	
	public List<Range> getRanges() {
		return ranges;
	}

	public void setRanges(List<Range> ranges) {
		this.ranges = ranges;
	}
	
	
	public static VirtualNode build_virtual_node(String line, boolean first){
		
		StringTokenizer st = new StringTokenizer(line);
		List<String> tokens = new ArrayList<String>();
		while(st.hasMoreTokens()){
			tokens.add(st.nextToken());
		}
		Object[] tokens_array = tokens.toArray();

		String IP = (String) tokens_array[0];
		int start = 0;
		long end = Long.parseLong((String)tokens_array[7]);
		VirtualNode vn = new VirtualNode(IP, start, end);

		
		return vn;
	}
	class Range {

		private long start;
		private long end;
		Range(long s, long e){
			this.setStart(s);
			this.setEnd(e);
		}
		
		public long getStart() {
			return start;
		}
		public void setStart(long start) {
			this.start = start;
		}
		public long getEnd() {
			return end;
		}
		public void setEnd(long end) {
			this.end = end;
		}
	}
	
}
