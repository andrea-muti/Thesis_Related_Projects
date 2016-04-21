package node_number_monitor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import javafx.util.StringConverter;


public class NodeNumberChart extends Application {
	
	/** !!! ATTENZIONE A QUESTO PARAMETRO !!! **/
	double single_duration_sec = 12.0;

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
	    
    
        stage.setTitle("Number of Nodes in the Cluster Over Time");

        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("Time [ hours ]");
        xAxis.setAutoRanging(false);
        xAxis.setTickUnit(1);
        xAxis.setMinorTickCount(0);
        xAxis.setUpperBound(24);
        
		yAxis.setLabel("Number Of Nodes");
		yAxis.setTickUnit(1);
		yAxis.setAutoRanging(false);
		yAxis.setUpperBound(7);
		yAxis.setMinorTickCount(0);
		yAxis.setTickLabelFormatter(new StringConverter<Number>() {
			
			@Override
			public String toString(Number object) {
				return String.format("%.0f", object);
			}
			
			@Override
			public Number fromString(String string) {
				// TODO Auto-generated method stub
				return null;
			}
		});
		

        final LineChart<Number,Number> lineChart = new LineChart<Number,Number>(xAxis,yAxis);
       
        lineChart.setTitle("Number of Nodes in the Cluster Over Time");
        lineChart.setCreateSymbols(false);  
        
        long first_ts = get_first_ts(file_paths);
        System.out.println("\n * first min ts : "+first_ts);
        
        add_line_to_chart(lineChart, file_paths.get(0), "number of nodes", first_ts);
        
        Scene scene  = new Scene(lineChart,800,600);       
       
        stage.setScene(scene);
        scene.getStylesheets().add( getClass().getResource("chart.css").toExternalForm() );
        stage.show();
    }
    
    private long get_first_ts(List<String> file_paths) {
		long[] primi_ts = new long[file_paths.size()];
		int i=0;
		for(String path : file_paths){	    
		    //carico il primo file
		    try{
	    		BufferedReader reader = new BufferedReader(new FileReader(path));
				
				// line format : 0.00 0.301
				//     n token :   0    1   
				String line = reader.readLine();
				
				if(line!=null){
					StringTokenizer st = new StringTokenizer(line);
					long ts = Long.parseLong(st.nextToken()); 
					primi_ts[i]=ts;
				}
				reader.close();
		    }catch(Exception e){}
		    i++;
		}
		
		Arrays.sort(primi_ts);
		return primi_ts[0];
	}

	private void add_line_to_chart(LineChart<Number, Number> lineChart, String file_path, String name, long first_ts) {    	
    	XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();

	    series.setName(name);

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file_path));
			
			// line format : 0.00 0.301
			//     n token :   0    1   
			String line = reader.readLine();
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line);
				double time = (double) (((Long.parseLong(st.nextToken()) - first_ts) / 1000) * (60.0/single_duration_sec))/60; 
				time = time/60.0;
				int value = Integer.parseInt(st.nextToken()); 
			    series.getData().add(new XYChart.Data<Number, Number>(time, value));			
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
    	args = new String[1];
    	args[0] = "/home/andrea-muti/Scrivania/autoscaling_experiments_results/sim_24_h_completa/cluster_node_number.txt"; 

    	if(args.length<1){
    		System.err.println("Error: path to the files to plot are required as argument");
    		System.exit(-1);
    	}
        launch(args);
    }
}