package charts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

public class RTp95ByIR extends Application {

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
	    
    
        stage.setTitle("Response Time [95th percentile] VS Input Rate");

        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("Input Rate [ requests/second ]");
        xAxis.setTickUnit(10);
		yAxis.setLabel("95th percentile RT  [ msec ]");
		yAxis.setTickUnit(2);

        final LineChart<Number,Number> lineChart = new LineChart<Number,Number>(xAxis,yAxis);
       
        lineChart.setTitle("95th percentile Response Time VS Input Rate");
        lineChart.setCreateSymbols(false);  
            
        add_line_to_chart(lineChart, file_paths.get(0), "3 nodes");
  
        Scene scene  = new Scene(lineChart,800,600);       
       
        stage.setScene(scene);
        stage.show();
    }
    
	private void add_line_to_chart(LineChart<Number, Number> lineChart, String file_path, String name) {    	
    	XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
	    series.setName(name);

	    Map<Integer, LinkedList<Double>> p95rt_by_IR = new Hashtable<Integer,LinkedList<Double>>();
	    
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file_path));
			System.out.println(" * parsing input file");
			//                 nodi, IR,    CPU,    THR      mRT  p95RT
			// line format :    3;  2000; 24.415; 1979.999; 1.34;  2.0;
			//     n token :    0     1      2        3       4     5   
			String line = reader.readLine();
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line,";");
				st.nextToken(); // nodi , non mi serve
				int IR = Integer.parseInt(st.nextToken());
				st.nextToken(); //cpu
				st.nextToken(); // th
				st.nextToken();
				double mrt = Double.parseDouble(st.nextToken());
				HashMapUtils.insert(p95rt_by_IR, IR, mrt);
				line = reader.readLine();
			}
			reader.close();
			
			System.out.println(" * computing averages");
			Map<Integer, Double> avg_throughput_by_IR = HashMapUtils.compute_averages(p95rt_by_IR);
			
			Iterator<Entry<Integer,Double>> iter2 = avg_throughput_by_IR.entrySet().iterator();
			while(iter2.hasNext()){
				Entry<Integer,Double> entry = iter2.next();
				double IR = entry.getKey();
				double TH = entry.getValue();
				series.getData().add(new XYChart.Data<Number, Number>(IR, TH));
			}
			
			lineChart.getData().add(series);
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