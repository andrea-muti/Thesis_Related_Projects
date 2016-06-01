package dataset_manipulation;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;


public class PredictionVisualizer extends Application {

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
	    
    
        stage.setTitle("Predicted Twitter Load over Time");

        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("Time [min]");
        xAxis.setTickUnit(10);
		yAxis.setLabel("Tweets [ tpm ]");

        final LineChart<Number,Number> lineChart = new LineChart<Number,Number>(xAxis,yAxis);
       
        lineChart.setTitle("Twitter Dataset over Time");
        lineChart.setCreateSymbols(false);  
        
        System.out.println(" - start generation of the dataset chart");

        add_line_to_chart(lineChart, file_paths.get(0), "dataset");
 
        System.out.println(" - end generation of the dataset chart");
      
        Scene scene  = new Scene(lineChart,800,600);       
       
        stage.setScene(scene);
        scene.getStylesheets().add( getClass().getResource("chart.css").toExternalForm() );
        
        stage.show();
    }
    

	private void add_line_to_chart(LineChart<Number, Number> lineChart, String file_path, String name) {    	
    	XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();

	    series.setName(name);
	    double time = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file_path));
			
			// line format : 0.00 0.301
			//     n token :   0    1   
			String line = reader.readLine();
			double i = 0;
			
			while( line!=null ){
				
				StringTokenizer st = new StringTokenizer(line);
				
				st.nextToken(); // primotoken
				time = i;
				Double value = Double.parseDouble(st.nextToken().replace(",", ".")); 
			    series.getData().add(new XYChart.Data<Number, Number>(time, value));
				line = reader.readLine();
				i++;
			}
			reader.close();
			lineChart.getData().add(series);
			
		} catch (Exception e) {
			System.err.println("\nError in opening|writing|closing the file: "+file_path);
			System.err.println("[  "+e.getMessage()+"  ]");
			System.err.println("\nExiting Program");
			System.exit(0);
		}	
	}


    public static void main(String[] args) {
    	args = new String[1];
    	// args[0] = "files/datasets/complete_twitter_dataset.csv";  // COMPLETE DATASET FILE
    	// args[0] = "files/datasets/workload_week_6.csv";  // WEEK X DATASET FILE
    	// args[0] = "files/datasets/workload_day_1.csv";  // DAY X DATASET FILE
    	 args[0] = "/home/andrea-muti/Scrivania/pred_week_6_time_load.csv";
    	
    	if(args.length<1){
    		System.err.println("Error: path to the files to plot are required as argument");
    		System.exit(-1);
    	}
        launch(args);
    }
}