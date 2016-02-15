package my_package;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import my_package.VirtualNode.Range;

public class RingChart extends Application {

	int n_machines;
	Set<String> different_ips;
	
    public void start(Stage stage) {
    	
    	Parameters parameters = getParameters();    
	    List<String> rawArguments = parameters.getRaw();
	      
	    // input param #0 : path to output file of nodetool
	    String path_output_nodetool = rawArguments.get(0);
    	
        ObservableList<PieChart.Data> pieChartData = createData(path_output_nodetool);
        
        stage.setTitle("Cassandra Ring Status");
        stage.setWidth(900);
        stage.setHeight(700);
        
        @SuppressWarnings("unused")
		String in_chart_title = "This is the chart Title";
        
        final DoughnutChart chart = new DoughnutChart(pieChartData);
        //chart.setTitle(in_chart_title);
        
        chart.setLabelsVisible(true);
        chart.setLegendVisible(false);
        chart.setLabelLineLength(15);

        Scene scene = new Scene(new StackPane(chart));
        stage.setScene(scene);
        stage.show();
        
        String[] colors = generate_colors(n_machines);
        
        applyCustomColorSequence( pieChartData, colors );
        
    }
    
    // Generates an array of n Strings representing hex values for colors
    private String[] generate_colors(int n) {
    	
    	String[] colors = new String[n];
    	
    	String [] preferiti = {"#FF3300", // rosso
    						   "#FFFF00", // giallo
    						   "#0000FF", // blue
							   "#00FF00", // verde
							   "#FF9900", // arancio
							   "#FF33CC", // viola
							   "#b33b00", // marrone
							   "#00FFFF"  // azzurro
    						   };
    	int size_preferiti = preferiti.length;
    	
    	if (n<=size_preferiti){
    		// mi bastano i preferiti	
    		for(int i = 0; i<n; i++){
    			colors[i] = preferiti[i];
    		}
    	}
    	else{
    		// oltre i preferiti mi servono altri
    		int n_mancanti = n-size_preferiti;
    		
    		for(int i = 0; i<preferiti.length; i++){
    			colors[i] = preferiti[i];
    		}
    	
        	String[] letters = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        	for(int i = 0; i<n_mancanti; i++){
        		boolean already_used = false;
        		String color = "#";
        		do{
        			for (int j = 0; j < 6; j++ ) {
    	     			color += letters[(int) Math.round(Math.random() * 15)];
    	     		}
        			for(int j=0; j<preferiti.length; j++){
        				if(preferiti[j].equals(color)){
        					already_used = true;
        					break;
        				}
        			}
    	     	}while(already_used);    
        		colors[i+preferiti.length] = color;
        	}
    		
    		
    	}
    	
    	
    	
    	return colors;
	}

	private ObservableList<PieChart.Data> createData(String path_string) {

    	// read file line by line in Java using Scanner
        FileInputStream fis = null;
		try { fis = new FileInputStream(path_string); } 
		catch (FileNotFoundException e) {
			System.err.println("ERROR : file "+path_string+" NOT FOUND!");
			e.printStackTrace();
		}
        Scanner scanner = new Scanner(fis);
        
        List<VirtualNode> vnodes = new LinkedList<VirtualNode>();
        
        boolean first = true;
        int index = 0;
        
        // saltare le prime 5 righe del file
        for(int i =0; i<5; i++){ scanner.nextLine(); }
        
        while(scanner.hasNextLine()){
        	
        	String line = scanner.nextLine();
        	
        	if( line.equals("") ){ break; }
        	
        	VirtualNode new_vn = VirtualNode.build_virtual_node(line, first);
        	if(!first){
        		long end_prev = vnodes.get(index-1).getRanges().get(0).getEnd();
        		Range oldRange = new_vn.getRanges().get(0);
        		Range newRange = new_vn.new Range(end_prev+1,oldRange.getEnd());
        		List<Range> newList = new LinkedList<Range>();
        		newList.add(newRange);
        		new_vn.setRanges(newList);
        	}
        	else{
        		Range oldRange = new_vn.getRanges().get(0);
        		Range newRange = new_vn.new Range(((-1)*((long)Math.pow(2, 63))),oldRange.getEnd());
        		List<Range> newList = new LinkedList<Range>();
        		newList.add(newRange);
        		new_vn.setRanges(newList);
        	}
        	vnodes.add(new_vn);
        	first = false;
            index++;
        }
     
        scanner.close();
        
        // update rangeS del primo virtual node
        VirtualNode first_vn = vnodes.get(0);
        Range second_half_range = first_vn.getRanges().get(0);
        
        long start_fist_half = vnodes.get(vnodes.size()-1).getRanges().get(0).getEnd()+1;
        Range first_half_range= first_vn.new Range(start_fist_half, ((long)Math.pow(2, 63))   );
        
        List<Range> newList = new LinkedList<Range>();
        newList.add(first_half_range);
		newList.add(second_half_range);
		vnodes.get(0).setRanges(newList);
        
		
        System.out.println(" - Collection of Virtual Nodes has been initialized");
        System.out.println(" - N° of Virtual Nodes in the Ring: "+vnodes.size());
        System.out.println(" - N° of Distinct Machines in the Cluster : "+count_distinct_machines(vnodes));
        System.out.println(" - N° Virtual Nodes per each Machine : "+count_vnodes_per_machine(vnodes));
        
        n_machines = count_distinct_machines(vnodes);
        
        //print_token_ranges(vnodes);
    	
    	ObservableList<PieChart.Data> data_list = FXCollections.observableArrayList();
    	 	
    	for(VirtualNode v : vnodes){
    		long size_range = compute_size_range(v);
    		String label = v.get_IP_address();
    		System.out.println("label: "+label+ " - range_size: "+size_range);
    		data_list.add(new PieChart.Data(label, size_range));
    	}
    	
    	return data_list;
        
    }
    
	//private void applyCustomColorSequence( ObservableList<PieChart.Data> pieChartData,  String... pieColors) {
    
    @SuppressWarnings("unused")
	private void print_token_ranges(List<VirtualNode> vnodes) {
    	System.out.println("\n *************** LIST OF TOKEN RANGES ***************** \n");
    	
		int n = 1;
		for(VirtualNode v : vnodes){
			System.out.println(" - Ranges of VirtNode n°"+n+" with IP "+v.get_IP_address());
			for(Range r : v.getRanges()){
				System.out.println("    - [ "+r.getStart()+" ; "+r.getEnd()+" ]");
			}
			System.out.println("");
			n++;
		}        		
		System.out.println(" ******************************************************\n");
    }
    
    
    private int count_distinct_machines(List<VirtualNode> vnodes){   	
    	different_ips = new HashSet<String>();
    	for(VirtualNode v : vnodes){ different_ips.add(v.get_IP_address()); }
		return different_ips.size();	
    }
    
    private int count_vnodes_per_machine(List<VirtualNode> vnodes){
    	int count = 0;
    	String first_IP = vnodes.get(0).get_IP_address();
    	for (VirtualNode v : vnodes){
    		if(v.get_IP_address().equals(first_IP)){ count++; }
    	}
		return count;	
    }
    
    private long compute_size_range(VirtualNode v){
    	long size = 0;
    	long len = 0;
    	for(Range r : v.getRanges()){
    		// NOTA : le dimensioni dei ranges sono scalate 1:100000 per evitare che andando il overflow si cambi il segno
    		long end = r.getEnd() / 100000;
    		long start = r.getStart()  / 100000 ;
    		
    		System.out.println("      start: "+start+ " ; end: "+end);
    		
    		
    		if( end > start ){  len = (end-start) ; }
    		else{ len = (start-end); }
    	
    		System.out.println("len: "+len);
    		
    		size = size + len;
    		
    	}
    	
    	return size;
    }
    
    private void applyCustomColorSequence(ObservableList<Data> pieChartData, String... pieColors) {
        List<String> list = new ArrayList<String>();
        Iterator<String> iter = different_ips.iterator();
        while(iter.hasNext()){
        	list.add(iter.next());
        }
        System.out.println(list.size());
        
        for (Data data : pieChartData) {
          String ip = data.getName();
          int ind = list.indexOf(ip);
          data.getNode().setStyle("-fx-pie-color: " + pieColors[ind] + ";");
        }
      }

    
    public static void main(String[] args) {
    	
    	if(args.length<1){
    		System.err.println("Error: path to the output_file of nodetool is required as argument");
    		System.exit(-1);
    	}
    	
    	Application.launch(args);
    }

}