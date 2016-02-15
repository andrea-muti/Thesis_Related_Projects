package response_time;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

/**
 *  rispetto a RTChart, questo è il grado di fare il grafico leggendo dal file generato dal ResponseTimeReader che 
 *  prende le latenze dal Driver Datastax (senza quindi calcolarle esplicitamente)
 *  
 *  le linee del file sono nel formato : 
 *  	     11:52:37:831;  5633.678835630229; 10812.307; 10812.307;    5;          5;       0;
 *      	 [timestamp]    [mean latency]     [perc95]   [perc99]   [executed] [success] [failed]
 *  tok_n :      0               1                2         3            4         5         6
 * 
 * @author andrea-muti
 *
 */

public class ResTimeDateChart extends Application {

    @Override public void start(Stage stage) {

    	Parameters parameters = getParameters();    
	    List<String> rawArguments = parameters.getRaw();
	   
	    List<String> file_paths =  new LinkedList<String>();
	    
	    int size = rawArguments.size(); // un solo file / argomento in realtà
	   
	    for(int i = 0; i< size ; i++){
	    	String path_data_file = rawArguments.get(i);
	    	System.out.println(" * data file n."+(i+1)+" : "+path_data_file);
	    	file_paths.add(path_data_file);
    	}
	    
    
        stage.setTitle("Response Time");

        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
     
        xAxis.setLabel("Time [seconds]");
        xAxis.setTickUnit(10);
		yAxis.setLabel("Response Time [ milliseconds ]");
		yAxis.setTickUnit(2);

        final LineChart<Number,Number> lineChart = new LineChart<Number,Number>(xAxis,yAxis);
       
        lineChart.setTitle("Response Time");
        lineChart.setCreateSymbols(false);  
        
        add_line_to_chart(lineChart, file_paths.get(0), "Response Time");
        //add_line_to_chart(lineChart, file_paths.get(1), "Response Time_6");
       
        Scene scene  = new Scene(lineChart,800,600);       
       
        stage.setScene(scene);
        stage.show();
    }
    
    
	private void add_line_to_chart(LineChart<Number, Number> lineChart, String file_path, String name) {    	
    	XYChart.Series<Number, Number> series_mean = new XYChart.Series<Number, Number>();
    	XYChart.Series<Number, Number> series_p95 = new XYChart.Series<Number, Number>();
    	XYChart.Series<Number, Number> series_p99 = new XYChart.Series<Number, Number>();

	    series_mean.setName(name+" [Mean]");
	    series_p95.setName(name+" [95Percentile]");
	    series_p99.setName(name+" [99Percentile]");

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file_path));
			
			// scarto la prima riga : contiene i fields names del csv 
			String line = reader.readLine();
			
			
			line = reader.readLine();
			
			boolean first = true;
			long start_time_value = 0;
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
	        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line,";");
				String timestamp_string = st.nextToken();  // token 0
				String mean_latency_value_string = st.nextToken(); // token 1
				String p95_latency_value_string = st.nextToken(); // token 2
				String p99_latency_value_string = st.nextToken(); // token 3
				
		        Date date = sdf.parse("1970-01-01 " + timestamp_string);

		        long time_value = date.getTime()/1000;
				if(first){
					start_time_value=time_value;
					first=false;
				}				
		        
		        double time = (double) (time_value-start_time_value);
				
		      
		        
				double value = Double.parseDouble(mean_latency_value_string )/1000;  // <-- millisec 
				double value95 = Double.parseDouble(p95_latency_value_string )/1000; // <-- millisec 
				double value99 = Double.parseDouble(p99_latency_value_string )/1000; // <-- millisec 
				
			    series_mean.getData().add(new XYChart.Data<Number, Number>(time, value));
			    series_p95.getData().add(new XYChart.Data<Number, Number>(time, value95));
			    series_p99.getData().add(new XYChart.Data<Number, Number>(time, value99));
				
				line = reader.readLine();
				//line=reader.readLine();
				//line=reader.readLine();
				
				
			}
			reader.close();
			lineChart.getData().add(series_mean);
			//lineChart.getData().add(series_p95);
			
			//lineChart.getData().add(series_p99);
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_path);
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}	
	}
	
	// la media non è implementata
	@SuppressWarnings("unused")
	private void add_line_to_chart_media3val(LineChart<Number, Number> lineChart, String file_path, String name) {    	
    	XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();

	    series.setName(name);

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file_path));
			
			// scarto la prima riga : contiene i fields names del csv 
			String line = reader.readLine();
			// line format : 13:45:53:793;36;6033147;4;149;0;
			//     n token :      0        1     2 
			line = reader.readLine();
			
			boolean first = true;
			long start_time_value = 0;
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
	        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line,";");
				String timestamp_string = st.nextToken();
				st.nextToken();
				
				String time_value_string = st.nextToken();
				
		        Date date = sdf.parse("1970-01-01 " + timestamp_string);

		        long time_value = date.getTime()/1000;
				if(first){
					start_time_value=time_value;
					first=false;
				}				
		        
		        double time = (double) (time_value-start_time_value);
				
				double value = Double.parseDouble(time_value_string)/1000000; // <-- millisec 
				
			    series.getData().add(new XYChart.Data<Number, Number>(time, value));
				
				line = reader.readLine();
				line=reader.readLine();
				//line=reader.readLine();

				
			}
			reader.close();
			lineChart.getData().add(series);
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_path);
			e.printStackTrace();
		} catch (ParseException e) {
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