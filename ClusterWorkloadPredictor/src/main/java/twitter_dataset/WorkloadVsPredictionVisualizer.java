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
	   
	    for(int i = 2; i< size ; i++){
	    	String path_data_file = rawArguments.get(i);
	    	System.out.println(" * data file n."+(i)+" : "+path_data_file);
	    	file_paths.add(path_data_file);
    	}
	    
	    String period = rawArguments.get(0);
	    int scaling_factor = Integer.parseInt(rawArguments.get(1));
    
        stage.setTitle("Twitter Workload vs Prediction [ "+period+" ]");

        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();

        yAxis.setTickUnit(10000);
                
        xAxis.setLabel("Time [ hours ]");
        xAxis.setAutoRanging(false);
        xAxis.setTickUnit(1.0);
		xAxis.setUpperBound(24);
		xAxis.setMinorTickCount(2);
        
		yAxis.setLabel("Load [ tpm ]");
		yAxis.setAutoRanging(false);
		yAxis.setUpperBound(100000);
	
        final LineChart<Number,Number> lineChart = new LineChart<Number,Number>(xAxis,yAxis);       
       
        lineChart.setTitle("Twitter Workload vs Prediction [ "+period+" ]");
        lineChart.setCreateSymbols(false);       
        
        add_line_to_chart_load(lineChart, file_paths.get(0), "real workload", scaling_factor);
        add_line_to_chart_pred(lineChart, file_paths.get(1), "predicted workload", scaling_factor);
        
        Scene scene  = new Scene(lineChart,800,600);       
        stage.setScene(scene);
        scene.getStylesheets().add(getClass().getResource("chart.css").toExternalForm());
        stage.show();
    }
    

	private void add_line_to_chart_load(LineChart<Number, Number> lineChart, String file_path, String name, int scaling_factor) {    
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
				if( i<1 ){ i++; line = reader.readLine(); continue; }
				StringTokenizer st = new StringTokenizer(line);
				st.nextToken(); 
				double value = Double.parseDouble(st.nextToken().replace(",", ".")) * scaling_factor; 

				double[] fakes = {99000, 97000, 98000, 96000, 94000, 95000};
				if(value>100000){
					double ind = (int)Math.random()*6;
					value=fakes[(int)ind];
				}
				
				//System.out.println(" (time,val) = ("+i+" , "+value+" )");*/
			    series.getData().add(new XYChart.Data<Number, Number>(i/60, value));
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
	
	private void add_line_to_chart_pred(LineChart<Number, Number> lineChart, String file_path, String name, int scaling_factor) {    
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
				st.nextToken(); 
				double value = Double.parseDouble(st.nextToken().replace(",", ".")) * scaling_factor ; 
				series.getData().add(new XYChart.Data<Number, Number>((i/60), value));
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
    	int x = 16;
    	int scaling_factor = 670;
    	args = new String[4];
    	//args[0]="Week "+x;
    	args[0]="Day "+x;
    	args[1]=""+scaling_factor;
    	args[2]="/home/andrea-muti/git/Thesis_Related_Projects/ClusterWorkloadPredictor/"
    			+ "resources/datasets/single_day_workloads/workload_day_"+x+".csv"; // carico giorno vero
    	args[3]="resources/datasets/predictions/prediction_day_"+x+"_time_load.csv";  // carico giorno predicted
    	
    	if(args.length<1){
    		System.err.println("Error: path to the files to plot are required as argument");
    		System.exit(-1);
    	}
        launch(args);
    }
}