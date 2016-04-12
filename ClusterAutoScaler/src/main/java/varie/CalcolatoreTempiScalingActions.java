package varie;

import ThesisRelated.ClusterAutoScaler.AutoScaleConstants;
import ThesisRelated.ClusterAutoScaler.ScalingAction;

public class CalcolatoreTempiScalingActions {
	
	
	// ciò che viene fatto nel main in realtà deve essere fatto dal decider
	public static void main(String[] args){
		
		int t_add = 12;
		
		int t_remove = 25;
		
		int max = 6;
		int min = 3;
		
		int current = 5;
		System.out.println(" - current node number : "+current+" nodes\n\n");
		
		
		ScalingAction decided_scaling_action = null;
		
		if( current < max ){
			System.out.println(" - calcolo previsions per lo scale OUT");
			// potrei scalare out --> facciamo le previsioni
			for( int i = 0; i <(max-current); i++){
				System.out.println("    - previsione @ t = now + T_add*"+(max-current-i));
				System.out.println("        - calcolo scaling_action... [secondo il modo attuale coi max throughput levels]");
				ScalingAction computed_sa = new ScalingAction(AutoScaleConstants.KEEP_CURRENT, 0);
				if(computed_sa.getAction().equals(AutoScaleConstants.SCALE_OUT) && computed_sa.getNumber()>=(max-current-i)){
					System.out.println("    - trovata scaling action da eseguire : ("+computed_sa.getAction()+";"+computed_sa.getNumber()+")");
					decided_scaling_action = computed_sa;
					if(current + computed_sa.getNumber() > max){
						System.out.println("    - [only "+(max-current)+" out of "+computed_sa.getNumber()+" nodes can be added since "+max+" is the maximum number of nodes]");
						decided_scaling_action = new ScalingAction(computed_sa.getAction(), (max-current));
					}
					System.out.println(" -----> returned scaling action : ("+decided_scaling_action.getAction()+";"+decided_scaling_action.getNumber()+")");
					break;
				}
				else{
					System.out.println("    - adding "+(max-current-i)+" nodes is NOT required\n");
				}
			}
			
		}
		
		if( (decided_scaling_action==null) && (current > min) ){
			System.out.println("\n    - calcolo previsions per lo scale IN");
			// potrei scalare in --> facciamo le previsioni
			for( int i = 0; i<(current-min); i++){
				System.out.println("       - previsione @ t = now + T_remove*"+(current-min-i));
				System.out.println("        - calcolo scaling_action...");
				ScalingAction computed_sa = new ScalingAction(AutoScaleConstants.SCALE_IN, 5);
				if(computed_sa.getAction().equals(AutoScaleConstants.SCALE_IN) && computed_sa.getNumber()>=(current-min-i)){
					System.out.println("    - trovata scaling action da eseguire : ("+computed_sa.getAction()+";"+computed_sa.getNumber()+")");
					decided_scaling_action = computed_sa;
					if(current - computed_sa.getNumber() < min){
						System.out.println("    - [only "+(current-min)+" out of "+computed_sa.getNumber()+" nodes can be removed since "+min+" is the minimun number of nodes]");
						decided_scaling_action = new ScalingAction(computed_sa.getAction(), (current-min));
					}
					System.out.println(" -----> returned scaling action : ("+decided_scaling_action.getAction()+";"+decided_scaling_action.getNumber()+")");

					break;
				}
				else{
					System.out.println("    - removing "+(current-min-i)+" nodes is NOT required\n");
				}
			}
			
		}
		
		
		
	}

}
