package utils_ann;

public class SineFunctionTimeStamps {
	
	public static void main(String[] args){
		int anno = 1;
		for(int i = 0; i<360; i++){
		
			double ang = Math.toRadians(i);

			int periodic = 0;
			if(i<30){ periodic = 1;	}
			else if(i<60){periodic = 2;}
			else if(i<90){periodic = 3;}
			else if(i<120){periodic = 4;}
			else if(i<150){periodic = 5;}
			else if(i<180){periodic = 6;}
			else if(i<210){periodic = 7;}
			else if(i<240){periodic = 8;}
			else if(i<270){periodic = 9;}
			else if(i<300){periodic = 10;}
			else if(i<330){periodic = 11;}
			else{periodic = 12;}
			System.out.println(anno+","+periodic+","+String.format("%.3f", 100*Math.sin(ang)).replace(",", ".")
				
					);
		}
		anno = 2;
		for(int i = 0; i<360; i++){
			double ang = Math.toRadians(i);
			
			int periodic = 0;
			if(i<30){ periodic = 1;	}
			else if(i<60){periodic = 2;}
			else if(i<90){periodic = 3;}
			else if(i<120){periodic = 4;}
			else if(i<150){periodic = 5;}
			else if(i<180){periodic = 6;}
			else if(i<210){periodic = 7;}
			else if(i<240){periodic = 8;}
			else if(i<270){periodic = 9;}
			else if(i<300){periodic = 10;}
			else if(i<330){periodic = 11;}
			else{periodic = 12;}
			System.out.println(anno+","+periodic+","+String.format("%.3f", 200*Math.sin(ang)).replace(",", ".")
			
					);
			
		}
		anno = 3;
		for(int i = 0; i<360; i++){
			double ang = Math.toRadians(i);
	
			int periodic = 0;
			if(i<30){ periodic = 1;	}
			else if(i<60){periodic = 2;}
			else if(i<90){periodic = 3;}
			else if(i<120){periodic = 4;}
			else if(i<150){periodic = 5;}
			else if(i<180){periodic = 6;}
			else if(i<210){periodic = 7;}
			else if(i<240){periodic = 8;}
			else if(i<270){periodic = 9;}
			else if(i<300){periodic = 10;}
			else if(i<330){periodic = 11;}
			else{periodic = 12;}
			System.out.println(anno+","+periodic+","+String.format("%.3f", 300*Math.sin(ang)).replace(",", ".")
					
					);
		
		}
		
		anno = 4;
		for(int i = 0; i<360; i++){
			double ang = Math.toRadians(i);
	
			int periodic = 0;
			if(i<30){ periodic = 1;	}
			else if(i<60){periodic = 2;}
			else if(i<90){periodic = 3;}
			else if(i<120){periodic = 4;}
			else if(i<150){periodic = 5;}
			else if(i<180){periodic = 6;}
			else if(i<210){periodic = 7;}
			else if(i<240){periodic = 8;}
			else if(i<270){periodic = 9;}
			else if(i<300){periodic = 10;}
			else if(i<330){periodic = 11;}
			else{periodic = 12;}
			System.out.println(anno+","+periodic+","+String.format("%.3f", 200*Math.sin(ang)).replace(",", ".")
					
					);
		
		}
		
		anno = 5;
		for(int i = 0; i<360; i++){
			double ang = Math.toRadians(i);
	
			int periodic = 0;
			if(i<30){ periodic = 1;	}
			else if(i<60){periodic = 2;}
			else if(i<90){periodic = 3;}
			else if(i<120){periodic = 4;}
			else if(i<150){periodic = 5;}
			else if(i<180){periodic = 6;}
			else if(i<210){periodic = 7;}
			else if(i<240){periodic = 8;}
			else if(i<270){periodic = 9;}
			else if(i<300){periodic = 10;}
			else if(i<330){periodic = 11;}
			else{periodic = 12;}
			System.out.println(anno+","+periodic+","+String.format("%.3f", 400*Math.sin(ang)).replace(",", ".")
					
					);
		
		}
	}

}
