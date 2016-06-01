package charts;

import java.util.Iterator;
import java.util.LinkedList;

public class Pair {
	
	public double ir;
	public double val;
	
	public Pair(double i, double val){
		this.ir=i;
		this.val=val;
	}
	
	
	public static void addValue(LinkedList<Pair> lista, Pair p) {

        if (lista.size() == 0) {
        	lista.add(p);
        } else if (lista.get(0).ir > p.ir) {
        	lista.add(0, p);
        } else if (lista.get(lista.size() - 1).ir < p.ir) {
        	lista.add(lista.size(), p);
        } else {
            int i = 0;
            while (lista.get(i).ir < p.ir) {
                i++;
            }
            lista.add(i, p);
        }
    }
	
	public static void printList(LinkedList<Pair> lista){
		Iterator<Pair> iter = lista.iterator();
		while(iter.hasNext()){
			Pair p = iter.next();
			System.out.println((p.val+"").replace(".", ","));
		}
	}

}
