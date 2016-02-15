package my_package;

import java.util.Comparator;
import java.util.StringTokenizer;

import javafx.util.Pair;



@SuppressWarnings("rawtypes")
public class MyComparator implements Comparator{

	@SuppressWarnings("unchecked")
	@Override
	public int compare(Object o1, Object o2) {
		Pair<String,Number> p1 = (Pair<String,Number>) o1;
		Pair<String,Number> p2 =  (Pair<String,Number>) o2;	
		StringTokenizer st1 = new StringTokenizer(p1.getKey(),".");
		String ipString1 = st1.nextToken() + st1.nextToken() + st1.nextToken() + st1.nextToken();
		int ip_int1 = Integer.parseInt(ipString1);
		
		StringTokenizer st2 = new StringTokenizer(p2.getKey(),".");
		String ipString2 = st2.nextToken() + st2.nextToken() + st2.nextToken() + st2.nextToken();
		int ip_int2 = Integer.parseInt(ipString2);
		
		if(ip_int1 > ip_int2){ return 1;}
		else if( ip_int1 == ip_int2 ){ return 0; }
		else{ return -1; }
	}
}
