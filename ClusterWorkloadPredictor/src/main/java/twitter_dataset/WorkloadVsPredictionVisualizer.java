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


public class WorkloadVsPredictionVisualizer extends Application {

    @Override public void start(Stage stage) {

    	Parameters parameters = getParameters();    
	    List<String> rawArguments = parameters.getRaw();
	   
	    List<String> file_paths =  new LinkedList<String>();
	    
	    int size = rawArguments.size();
	   
	    for(int i = 1; i< size ; i++){
	    	String path_data_file = rawArguments.get(i);
	    	System.out.println(" * data file n."+(i)+" : "+path_data_file);
	    	file_paths.add(path_data_file);
    	}
	    
	    String period = rawArguments.get(0);
    
        stage.setTitle("Twitter Workload vs Prediction [ "+period+" ]");

        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("Time [ hours ]");
        xAxis.setTickUnit(10);
		yAxis.setLabel("Load [ tpm ]");

        final LineChart<Number,Number> lineChart = new LineChart<Number,Number>(xAxis,yAxis);
        
       
        lineChart.setTitle("Twitter Workload vs Prediction [ "+period+" ]");
        lineChart.setCreateSymbols(false);  
      
        
        add_line_to_chart_load(lineChart, file_paths.get(0), "real workload");
        add_line_to_chart_pred(lineChart, file_paths.get(1), "predicted workload");
        
        
      
        Scene scene  = new Scene(lineChart,800,600);       
       
        stage.setScene(scene);
        scene.getStylesheets().add(
                getClass().getResource("chart.css").toExternalForm()
              );
        stage.show();
    }
    

	private void add_line_to_chart_load(LineChart<Number, Number> lineChart, String file_path, String name) {    
		System.out.println(" - inserting line of "+name);
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
				//System.out.println(" (time,val) = ("+i+" , "+value+" )");
			    series.getData().add(new XYChart.Data<Number, Number>(time/60, value));
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
	
	private void add_line_to_chart_pred(LineChart<Number, Number> lineChart, String file_path, String name) {    
		System.out.println(" - inserting line of "+name);
    	XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
    	
    
    	
	    series.setName(name);
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file_path));
			
			// line format : 0.00 0.301
			//     n token :   0    1   
			String line = reader.readLine();
			double i = 0;
			while( line!=null ){
			
				StringTokenizer st = new StringTokenizer(line);
				double time = Double.parseDouble(st.nextToken()); 
				time = i;
				double value = Double.parseDouble(st.nextToken().replace(",", ".")); 
				//System.out.println(" (time,val) = ("+i+" , "+value+" )");
				for(int j=0; j<61;j++){
					series.getData().add(new XYChart.Data<Number, Number>((time+j)/60, value));
				}
				line = reader.readLine();
				i=i+60;
			}
			reader.close();
			lineChart.getData().add(series);
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_path);
			e.printStackTrace();
		}	
	}


    public static void main(String[] args) {
    	int x = 1;
    	args = new String[3];
    	args[0]="Week "+x;
    	
    	args[1]="/home/andrea-muti/git/Thesis_Related_Projects/ClusterWorkloadPredictor/resources/datasets/single_week_workloads/workload_week_"+x+".csv"; // carico giorno vero
    	args[2]="resources/datasets/predictions/prediction_week_"+x+"_time_load.csv"; 
    	
    	if(args.length<1){
    		System.err.println("Error: path to the files to plot are required as argument");
    		System.exit(-1);
    	}
        launch(args);
    }
}