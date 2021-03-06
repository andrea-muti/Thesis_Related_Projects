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

        xAxis.setLabel("Time [ hours ]");
        xAxis.setAutoRanging(false);
        xAxis.setTickUnit(24);
        xAxis.setMinorTickCount(2);
        xAxis.setUpperBound(24*14);
		yAxis.setLabel("Tweets [ tpm ]");
		  yAxis.setAutoRanging(false);
		  yAxis.setUpperBound(250);
		  yAxis.setMinorTickCount(0);
		  yAxis.setTickUnit(25);

        final LineChart<Number,Number> lineChart = new LineChart<Number,Number>(xAxis,yAxis);
       
        //lineChart.setTitle("Twitter Dataset");
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
				//int scaling_factor=710;
				int scaling_factor=1;
				double value = Double.parseDouble(st.nextToken()) * scaling_factor; 
				//System.out.println(" (time,val) = ("+i+" , "+value+" )");
			    series.getData().add(new XYChart.Data<Number, Number>(time/60.0, value));
				line = reader.readLine();
			
				if(i%10000==0){System.out.println("    - analyzed "+i+" lines");}
				i++;
			}
			reader.close();
			lineChart.getData().add(series);
			
		} catch (Exception e) {
			System.err.println("\nError in opening|writing|closing the file: "+file_path+"\nExiting Program");
			System.out.println(e.getMessage());
			System.exit(0);
		}	
	}


    public static void main(String[] args) {
    	args = new String[1];
    	// SCALING FACTOR 710
    	 //args[0] = "files/datasets/complete_twitter_dataset.csv";  // COMPLETE DATASET FILE
    	 //args[0] = "files/datasets/workload_week_2.csv";  // WEEK X DATASET FILE
    	 
    	//args[0] = "files/datasets/workload_day_16.csv";  // DAY X DATASET FILE
    	args[0] = "files/datasets/workload_day_5_to_18.csv";
    	//args[0] = "/home/andrea-muti/Scrivania/clarknet_trace/output_clarknet.txt";  // CLARKNET
    	
    	if(args.length<1){
    		System.err.println("Error: path to the files to plot are required as argument");
    		System.exit(-1);
    	}
        launch(args);
    }
}