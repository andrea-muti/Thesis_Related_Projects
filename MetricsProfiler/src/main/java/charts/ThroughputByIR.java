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

public class ThroughputByIR extends Application {
	
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
	    
    
        stage.setTitle("Throughput VS Input Rate");

        final NumberAxis xAxis = new NumberAxis(0000,210000,10000);
        final NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("Input Rate [ requests/second ]");
        //xAxis.setTickUnit(10);
        
		yAxis.setLabel("Throughput [ transactions/second ]");
		yAxis.setTickUnit(2);

        final LineChart<Number,Number> lineChart = new LineChart<Number,Number>(xAxis,yAxis);
       
        lineChart.setTitle("Throughput VS Input Rate");
        lineChart.setCreateSymbols(false);  
            
        add_line_to_chart(lineChart, file_paths.get(0), "3 nodes", 3);
        add_line_to_chart(lineChart, file_paths.get(0), "4 nodes", 4);
        add_line_to_chart(lineChart, file_paths.get(0), "5 nodes", 5);
        add_line_to_chart(lineChart, file_paths.get(0), "6 nodes", 6);
        
        
  
        Scene scene  = new Scene(lineChart,800,600);       
       
        stage.setScene(scene);
        stage.show();
    }
    
    
    
	private void add_line_to_chart(LineChart<Number, Number> lineChart, String file_path, String name, int num) {    	
    	XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
	    series.setName(name);

	    Map<Integer, LinkedList<Double>> throughputs_by_IR = new Hashtable<Integer,LinkedList<Double>>();
	    
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file_path));
			System.out.println("\n * parsing input file");
			//                 nodi, IR,    CPU,    THR      mRT  p95RT
			// line format :    3;  2000; 24.415; 1979.999; 1.34;  2.0;
			//     n token :    0     1      2        3       4     5   
			String line = reader.readLine();
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line,";");
				int nodi = Integer.parseInt(st.nextToken());
				if(nodi==num){
					int IR = Integer.parseInt(st.nextToken());
					st.nextToken(); // cpu, non mi serve;
					double TH = Double.parseDouble(st.nextToken());
					HashMapUtils.insert(throughputs_by_IR, IR, TH);
				}
				line = reader.readLine();
			}
			reader.close();
			
			System.out.println(" * computing averages");
			Map<Integer, Double> avg_throughput_by_IR = HashMapUtils.compute_averages(throughputs_by_IR);
			
			double avg_max_th = 0;
			int n = 0;
			
			double IR = 0, TH;
			
			Iterator<Entry<Integer,Double>> iter2 = avg_throughput_by_IR.entrySet().iterator();
			while(iter2.hasNext()){
				Entry<Integer,Double> entry = iter2.next();
				IR = entry.getKey();
				TH = entry.getValue();
				series.getData().add(new XYChart.Data<Number, Number>(IR, TH));
				if(num==3){
					if( IR > 90000 ){
						avg_max_th += TH;
						n++;
					}
				}
				else if(num==4){
					if( IR > 102000 ){
						avg_max_th += TH;
						n++;
					}
				}
				else if(num==5){
					if( IR > 116000 ){
						avg_max_th += TH;
						n++;
					}
				}
				else if(num==6){
					if( IR > 128000 ){
						avg_max_th += TH;
						n++;
					}
				}
				
			
			}
			
			if(avg_max_th!=0){
				avg_max_th = avg_max_th / n;
				System.out.println(" * avg max throughput for "+name+" : "+avg_max_th+" tps");
			}
			
			lineChart.getData().add(series);
			System.out.println(" * new line inserted");
			
			/*
			// linea avg max th
			XYChart.Series<Number, Number> series_max = new XYChart.Series<Number, Number>();
		    series_max.setName("avg max");
		    
		    iter2 = avg_throughput_by_IR.entrySet().iterator();
		    double IR_start = iter2.next().getKey();
		    System.out.println("IRstart "+IR_start);
		    System.out.println("IR end" +IR);
		    series_max.getData().add(new XYChart.Data<Number, Number>(1000, avg_max_th));
		    series_max.getData().add(new XYChart.Data<Number, Number>(200000,avg_max_th));
			lineChart.getData().add(series_max);
		    */
			
			
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