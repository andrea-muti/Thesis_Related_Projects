package ThesisRelated.ClusterAutoScaler;

public class ScalingAction {
	
	private String action;
	private int node_number;	
	
	public ScalingAction(String action, int number){
		check_params(action,number);
		this.action=action;
		this.node_number=number;
	}

	public String getAction(){
		return this.action;
	}
	public int getNumber(){
		return this.node_number;
	}
	
	private void check_params(String action, int number) {
		if( !action.equals(AutoScaleConstants.SCALE_OUT) && 
			!action.equals(AutoScaleConstants.SCALE_IN) && !action.equals(AutoScaleConstants.KEEP_CURRENT) ){
			System.err.println(" ERROR autoscaling action "+action+" not correct");
			System.exit(0);
		}
		if(number<0){
			System.err.println(" ERROR cannot add/remove a negative number of nodes");
			System.exit(0);
		}
		if( ( (number!=0) && (action.equals(AutoScaleConstants.KEEP_CURRENT))  ) ||
			  (number==0) && (!action.equals(AutoScaleConstants.KEEP_CURRENT)) ) {
			System.err.println(" ERROR incompatible action and node number");
			System.exit(0);
		}		
	}

	public String format_as_printable_message( int current){
		String to_print = "";
		String act = this.getAction();
		int num = this.getNumber();
		if(act.equals(AutoScaleConstants.SCALE_OUT)){
			if(num==1){ to_print = "SCALE "+act + " add 1 node"; }
			else{ to_print = "SCALE "+act + " --> add "+num+" nodes"; }
		}
		else if(act.equals(AutoScaleConstants.SCALE_IN)){
			if(num==1){ to_print = "SCALE "+act + " --> remove 1 node"; }
			else{ to_print = "SCALE "+act + " --> remove "+num+" nodes"; }
		}
		else {
			if(num==1){ to_print = act+" --> "+current+" node is OK"; }
			else{ to_print = act+" --> "+current+" nodes are OK"; }
		}
		return to_print;
	}
	
}
