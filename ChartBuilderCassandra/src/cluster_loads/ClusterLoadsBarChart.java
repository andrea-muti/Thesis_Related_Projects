package cluster_loads;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
 
public class ClusterLoadsBarChart extends Application {
	
    final static String nodes_3 = "3 Nodes";
    final static String nodes_4 = "4 Nodes";
    final static String nodes_5 = "5 Nodes";
    final static String nodes_6 = "6 Nodes";
 
    @Override public void start(Stage stage) {
    	
    	Parameters parameters = getParameters();    
	    List<String> rawArguments = parameters.getRaw();
	   
	    List<String> file_paths =  new LinkedList<String>();
	    
	    int size = rawArguments.size();
	   
	    for(int i = 0; i< size ; i++){
	    	String path_data_file = rawArguments.get(i);
	    	System.out.println(" * data file n."+(i+1)+" : "+path_data_file);
	    	file_paths.add(path_data_file);
    	}
	    
	    String a_file_path = file_paths.get(0);
	    StringTokenizer st = new StringTokenizer(a_file_path, "_");
	    String load_label_long = st.nextToken();
	    String load_label = load_label_long.substring(load_label_long.lastIndexOf("/")+1);
	    
	    int n_nodes = 0;
	    for(String p : file_paths){
	    	int count = count_nodes(p);
	    	if(n_nodes<count){ n_nodes=count; }
	    }
    	
    	Map<String, LinkedList<Double>> map = new HashMap<String, LinkedList<Double>>();
    	// initialization
    	for(int i = 1; i<=n_nodes;i++){
    		map.put(""+i, new LinkedList<Double>());
    	}
    	
    	populate_map(map, file_paths);
    	
    	
    	
        stage.setTitle("Cluster Load Chart");
        
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        
        final BarChart<String,Number> bc = new BarChart<String,Number>(xAxis,yAxis);
        
        bc.setTitle("Cluster Load Chart\nInitial Cluster Load: "+load_label);
        
        xAxis.setLabel("Node");       
        yAxis.setLabel("Load ["+load_label.substring(load_label.length()-2, load_label.length())+"]");
         
        Scene scene  = new Scene(bc,800,600);
        
        for(int i=1; i<=n_nodes;i++){
    		LinkedList<Double> list = map.get(""+i);
    		XYChart.Series<String,Number> series = new XYChart.Series<String,Number>();
            series.setName("Nodo "+i);       
            
            if(list.size()==4){
            	series.getData().add(new XYChart.Data<String,Number>(nodes_3, list.get(0)));
                series.getData().add(new XYChart.Data<String,Number>(nodes_4, list.get(1)));
                series.getData().add(new XYChart.Data<String,Number>(nodes_5, list.get(2)));
                series.getData().add(new XYChart.Data<String,Number>(nodes_6, list.get(3)));
            }
            else if(list.size()==3){
                series.getData().add(new XYChart.Data<String,Number>(nodes_4, list.get(0)));
                series.getData().add(new XYChart.Data<String,Number>(nodes_5, list.get(1)));
                series.getData().add(new XYChart.Data<String,Number>(nodes_6, list.get(2)));
            }
            else if(list.size()==2){
                series.getData().add(new XYChart.Data<String,Number>(nodes_5, list.get(0)));
                series.getData().add(new XYChart.Data<String,Number>(nodes_6, list.get(1)));
            }
            else if(list.size()==1){
                series.getData().add(new XYChart.Data<String,Number>(nodes_6,list.get(0)));
            }
            bc.getData().add(series);
    	}
        
        
        
        stage.setScene(scene);
        stage.show();
    }
    
    private void populate_map(Map<String, LinkedList<Double>> map, List<String> file_paths) {
    	for(String file_name : file_paths){
	    	try {
				BufferedReader reader = new BufferedReader(new FileReader(file_name));
				
				String line = reader.readLine();
				int index_node = 0;
				while(line!=null ){
					StringTokenizer st = new StringTokenizer(line);
					if(st.hasMoreTokens()){
						String first = st.nextToken();
						if(first.equals("UN") || first.equals("DN")){
							index_node +=1;
							st.nextToken(); // IP addr non mi serve
							Double load = Double.parseDouble(st.nextToken().replace(",", "."));
							String load_label = st.nextToken();
							if(load_label.equals("MB")){
								load = load/1024;
							}
							else if(load_label.equals("KB")){
								load = load/1024/1024;
							}
							map.get(""+index_node).add(load);
						}
					}
					
					line = reader.readLine();
					
				}
				reader.close();
				
			} catch (IOException e) {
				System.err.println("Error in opening|writing|closing the file: "+file_name);
				e.printStackTrace();
			}
    	}
	}

	public int count_nodes(String file_name){
    	int n = 0;
    	try {
			BufferedReader reader = new BufferedReader(new FileReader(file_name));
			
			String line = reader.readLine();
			while(line!=null ){
				StringTokenizer st = new StringTokenizer(line);
				if(st.hasMoreTokens()){
					String first = st.nextToken();
					if(first.equals("UN") || first.equals("DN")){n++;}
				}
				
				line = reader.readLine();
				
			}
			reader.close();
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_name);
			e.printStackTrace();
		}
    	return n;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}