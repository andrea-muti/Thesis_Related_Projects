package charts_joinings;

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

public class CpuDuringJoining extends Application {
	
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
	    
    
        stage.setTitle("Cpu during joining");

        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("time");
        //xAxis.setTickUnit(10);
        
		yAxis.setLabel("CPU %");
		yAxis.setTickUnit(2);

        final LineChart<Number,Number> lineChart = new LineChart<Number,Number>(xAxis,yAxis);
       
        lineChart.setTitle("CPU during Joining and Cleanups");
        lineChart.setCreateSymbols(false);  
            
        //add_line_to_chart(lineChart, file_paths.get(0), "from 3 to 4 nodes", 3, 80000);
        //add_line_to_chart(lineChart, file_paths.get(0), "from 4 to 5 nodes", 4, 95000);
        add_line_to_chart(lineChart, file_paths.get(0), "from 5 to 6 nodes", 5, 112000);
      
        Scene scene  = new Scene(lineChart,800,600);       
       
        stage.setScene(scene);
        stage.show();
    }
    
    
    
	private void add_line_to_chart(LineChart<Number, Number> lineChart, String file_path, String name, int num, int ir) {    	
    	XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
	    series.setName(name);

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file_path));
			System.out.println("\n * parsing input file");
			//                 nodi, IR,    CPU,    THR       mRT  p95RT
			// line format :    3; 80000; 91.701;  79753.125; 5.8; 16.0;
			//     n token :    0     1      2        3       4     5   
			int time = 0;
			String line = reader.readLine();
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line,";");
				st.nextToken();
				int inputrate = Integer.parseInt(st.nextToken());
				if(inputrate==ir){
					double cpu = Double.parseDouble(st.nextToken());
				
					series.getData().add(new XYChart.Data<Number, Number>(time, cpu));
					time=time+15;
				}
				line = reader.readLine();
			}
			reader.close();

			lineChart.getData().add(series);
			System.out.println(" * new line inserted");
			
			
			
		} catch (IOException e) {
			System.err.println("Error in opening|writing|closing the file: "+file_path);
			e.printStackTrace();
		}	
		
		XYChart.Series<Number, Number> seriesnodi = new XYChart.Series<Number, Number>();
	    seriesnodi.setName("number of nodes");

		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file_path));
			System.out.println("\n * parsing input file");
			//                 nodi, IR,    CPU,    THR       mRT  p95RT
			// line format :    3; 80000; 91.701;  79753.125; 5.8; 16.0;
			//     n token :    0     1      2        3       4     5   
			int time = 0;
			String line = reader.readLine();
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line,";");
				int nodi = Integer.parseInt(st.nextToken());
				int inputrate = Integer.parseInt(st.nextToken());
				if(inputrate==ir){
					
					seriesnodi.getData().add(new XYChart.Data<Number, Number>(time, nodi*8));
					time=time+15;
				}
				line = reader.readLine();
			}
			reader.close();

			lineChart.getData().add(seriesnodi);
			System.out.println(" * new line inserted");
			
			
			
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