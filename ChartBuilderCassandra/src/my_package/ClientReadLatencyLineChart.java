package  my_package;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;


public class ClientReadLatencyLineChart extends Application {

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
	    
    
        stage.setTitle("Client Read Latencies");

        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("LOAD [GB]");
		yAxis.setLabel("Client Read Latency [milliseconds]");

        final LineChart<String,Number> lineChart = new LineChart<String,Number>(xAxis,yAxis);
       
        lineChart.setTitle("Client Read Latency vs Cluster Load");
        lineChart.setCreateSymbols(false);  
        
        for(int i = 0; i<size; i++){
        	
        	add_line_to_chart(lineChart, file_paths.get(i));
        	
        }

        Scene scene  = new Scene(lineChart,800,600);       

        stage.setScene(scene);
        stage.show();
    }
    
    private void add_line_to_chart(LineChart<String, Number> lineChart, String file_path) {    	
    	XYChart.Series<String, Number> series = new XYChart.Series<String, Number>();
    	
    	// si assume che i data file abbiamo name : client_read_latencies_x_nodes.txt
    	int ind = file_path.lastIndexOf("/")+1;
    	
    	
    	String series_name = file_path.substring(ind).replace("client_read_latencies_" , "").replace("_nodes", " nodes")
    								 .replace(".txt", "");
	    series.setName(series_name);
    	
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file_path));
			
			// line format : 3.0 GB : 2934.463 microseconds
			
			String line = reader.readLine();
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line);
				String load_label = st.nextToken();
				st.nextToken(); // GB 
				st.nextElement(); // :
				double value = Double.parseDouble(st.nextToken()) / 1000;
				
			    series.getData().add(new XYChart.Data<String, Number>(load_label, value));
				
				line = reader.readLine();
			}
			reader.close();
			lineChart.getData().add(series);
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_path);
			e.printStackTrace();
		}	
	}


    public static void main(String[] args) {
    	if(args.length<1){
    		System.err.println("Error: at least one path to the read latencies data file is required as argument");
    		System.exit(-1);
    	}
        launch(args);
    }
}