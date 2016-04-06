package twitter_dataset;

public class ProporzioniThroughput {
	
	public static void main(String[] args){
		/*
		double max = 110;
		
		double sf6 = 1;
		double sf5 = 0.898;
		double sf4 = 0.805;
		double sf3 = 0.703;
		
		System.out.println(" - "+(max*sf6));
		System.out.println(" - "+(max*sf5));
		System.out.println(" - "+(max*sf4));
		System.out.println(" - "+(max*sf3));
		*/
		
		double percentuale = 0.85;
		double max_th_3 = 92.4;
		double max_th_4 = 105.8;
		double max_th_5 = 118.0;
		double max_th_6 = 131.4;
		
		System.out.println(" - "+(max_th_3*percentuale));
		System.out.println(" - "+(max_th_4*percentuale));
		System.out.println(" - "+(max_th_5*percentuale));
		System.out.println(" - "+(max_th_6*percentuale));
		
		
	}
}
