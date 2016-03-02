package instant_throughput;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;


public class InstantThroughputChart extends Application {

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
	    
    
        stage.setTitle("Throughput Over Time");

        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("Time [seconds]");
        xAxis.setTickUnit(10);
		yAxis.setLabel("Throughput [ req/sec ]");

        final LineChart<Number,Number> lineChart = new LineChart<Number,Number>(xAxis,yAxis);
       
        lineChart.setTitle("Throughput over Time");
        lineChart.setCreateSymbols(false);  
                
        add_line_to_chart(lineChart, file_paths.get(0), "vm0");
        add_line_to_chart(lineChart, file_paths.get(1), "vm1");
        add_line_to_chart(lineChart, file_paths.get(2), "vm2");
        add_line_to_chart(lineChart, file_paths.get(3), "vm3");
        add_line_to_chart(lineChart, file_paths.get(4), "vm4");
        add_line_to_chart(lineChart, file_paths.get(5), "vm5");
      
        add_line_to_chart_cumulative(lineChart, file_paths, "total");
        
        Scene scene  = new Scene(lineChart,800,600);       
       
        stage.setScene(scene);
        stage.show();
    }
    
    private void add_line_to_chart_cumulative(LineChart<Number, Number> lineChart, List<String> file_paths, String string) {
    	XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();

	    series.setName(string);

	    List<Double> times = new ArrayList<Double>();
	    List<Double> values = new ArrayList<Double>();
	    
	    
	    //carico il primo file
	    try{
    		BufferedReader reader = new BufferedReader(new FileReader(file_paths.get(0)));
			
			// line format : 0.00 0.301
			//     n token :   0    1   
			String line = reader.readLine();
			
			while ( line != null ){
				StringTokenizer st = new StringTokenizer(line);
				double time = Double.parseDouble(st.nextToken()); 
				double value = Double.parseDouble(st.nextToken()); 
				
				values.add(value);
				times.add(time);
				
				line = reader.readLine();
			}
			reader.close();
			
	    }
	    catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file");
			e.printStackTrace();
		}
	   
	    // carico i restanti files
	    for(int i=1; i<file_paths.size(); i++){
	    	int j = 0;
	    	try{
	    		BufferedReader reader = new BufferedReader(new FileReader(file_paths.get(i)));
				
				// line format : 0.00 0.301
				//     n token :   0    1   
				String line = reader.readLine();
				
				while ( line != null ){
					if(j>=values.size()){break;}
					StringTokenizer st = new StringTokenizer(line);
					double time = Double.parseDouble(st.nextToken()); 
					double value = Double.parseDouble(st.nextToken()); 
					
					values.set(j, value+values.get(j));
					times.set(j, time+times.get(j));
				
					j++;
					line = reader.readLine();
				}
				reader.close();
				
		    }
		    catch (IOException e) {
				System.err.println("Error in opening|writing|closing the file");
				e.printStackTrace();
			}
	    }
	    
	    for(int i=0; i<values.size();i++){
	    	double value = values.get(i);
	    	double time = times.get(i)/file_paths.size();
	    	series.getData().add(new XYChart.Data<Number, Number>(time, value));
	    }
	    lineChart.getData().add(series);
	    	
	}

	private void add_line_to_chart(LineChart<Number, Number> lineChart, String file_path, String name) {    	
    	XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();

	    series.setName(name);

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file_path));
			
			// line format : 0.00 0.301
			//     n token :   0    1   
			String line = reader.readLine();
			
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line);
				double time = Double.parseDouble(st.nextToken()); 
				double value = Double.parseDouble(st.nextToken()); 
				
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
    	if(args.length<1){
    		System.err.println("Error: path to the files to plot are required as argument");
    		System.exit(-1);
    	}
        launch(args);
    }
}