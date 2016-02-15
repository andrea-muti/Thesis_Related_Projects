package  metrics_stabilization;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;


public class WriteThroughputLineChart extends Application {

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
	    
    
        stage.setTitle("Write Throughput");

        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("Number of Random Write Requests");
		yAxis.setLabel("Write Throughput [ events/sec ]");

        final LineChart<Number,Number> lineChart = new LineChart<Number,Number>(xAxis,yAxis);
       
        lineChart.setTitle("Write Throughput vs Number of Write Requests");
        lineChart.setCreateSymbols(false);  
                
        add_line_to_chart(lineChart, file_paths.get(0), "Client Write Throughput");
        add_line_to_chart(lineChart, file_paths.get(1), "Node Write Throughput");	

        Scene scene  = new Scene(lineChart,800,600);       
       
        stage.setScene(scene);
        stage.show();
    }
    
    private void add_line_to_chart(LineChart<Number, Number> lineChart, String file_path, String name) {    	
    	XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();

	    series.setName(name);
    	
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file_path));
			
			// line format : 1.0 GB : 1.491 events/second
			//     n token :  0   1 2  3              4  
			String line = reader.readLine();
			int n_reads = 0;
			
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line);
				st.nextToken(); // 1.0
				st.nextToken(); // GB 
				st.nextToken(); // :
				double value = Double.parseDouble(st.nextToken()); // value
				
			    series.getData().add(new XYChart.Data<Number, Number>(n_reads, value));
				
				line = reader.readLine();
				n_reads += 500;
			}
			reader.close();
			lineChart.getData().add(series);
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_path);
			e.printStackTrace();
		}	
	}


    public static void main(String[] args) {
    	if(args.length<2){
    		System.err.println("Error: path to the client write throughput and to node write "
    				+ "throughput data files are required as argument");
    		System.exit(-1);
    	}
        launch(args);
    }
}