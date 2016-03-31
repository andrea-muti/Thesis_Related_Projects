package twitter_dataset;


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


public class DatasetVisualizer extends Application {

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
	    
    
        stage.setTitle("Twitter Dataset over Time");

        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("Time [min]");
        xAxis.setTickUnit(10);
		yAxis.setLabel("Tweets [ tpm ]");

        final LineChart<Number,Number> lineChart = new LineChart<Number,Number>(xAxis,yAxis);
       
        lineChart.setTitle("Twitter Dataset over Time");
        lineChart.setCreateSymbols(false);  
        
        add_line_to_chart(lineChart, file_paths.get(0), "dataset");
      
        Scene scene  = new Scene(lineChart,800,600);       
       
        stage.setScene(scene);
        stage.show();
    }
    

	private void add_line_to_chart(LineChart<Number, Number> lineChart, String file_path, String name) {    	
    	XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();

	    series.setName(name);
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file_path));
			
			// line format : 0.00 0.301
			//     n token :   0    1   
			String line = reader.readLine();
			double i = 0;
			while( line!=null ){
				if( i<1){ i++; line = reader.readLine(); continue; }
				StringTokenizer st = new StringTokenizer(line);
				double time = Double.parseDouble(st.nextToken()); 
				time = i;
				double value = Double.parseDouble(st.nextToken()); 
				System.out.println(" (time,val) = ("+i+" , "+value+" )");
			    series.getData().add(new XYChart.Data<Number, Number>(time, value));
				line = reader.readLine();
				i++;
			}
			reader.close();
			lineChart.getData().add(series);
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_path);
			e.printStackTrace();
		}	
	}


    public static void main(String[] args) {
    	args[0]="/home/andrea-muti/Scrivania/dataset_twitter/complete_twitter_dataset.csv";
    	if(args.length<1){
    		System.err.println("Error: path to the files to plot are required as argument");
    		System.exit(-1);
    	}
        launch(args);
    }
}