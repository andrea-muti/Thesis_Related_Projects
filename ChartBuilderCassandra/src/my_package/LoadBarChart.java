package my_package;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.stage.Stage;
import javafx.util.Pair;
 
public class LoadBarChart extends Application {

	private static void setMaxBarWidth(double maxBarWidth, double minCategoryGap, CategoryAxis xAxis, BarChart<String,Number> bc){
	    double barWidth=0;
	    do{
	        double catSpace = xAxis.getCategorySpacing();
	        double avilableBarSpace = catSpace - (bc.getCategoryGap() + bc.getBarGap());
	        barWidth = (avilableBarSpace / bc.getData().size()) - bc.getBarGap();
	        if (barWidth >maxBarWidth){
	            avilableBarSpace=(maxBarWidth + bc.getBarGap())* bc.getData().size();
	            bc.setCategoryGap(catSpace-avilableBarSpace-bc.getBarGap());
	        }
	    } while(barWidth>maxBarWidth);

	    do{
	        double catSpace = xAxis.getCategorySpacing();
	        double avilableBarSpace = catSpace - (minCategoryGap + bc.getBarGap());
	        barWidth = Math.min(maxBarWidth, (avilableBarSpace / bc.getData().size()) - bc.getBarGap());
	        avilableBarSpace=(barWidth + bc.getBarGap())* bc.getData().size();
	        bc.setCategoryGap(catSpace-avilableBarSpace-bc.getBarGap());
	    } while(barWidth < maxBarWidth && bc.getCategoryGap()>minCategoryGap);
	}
	
	
    @SuppressWarnings("unchecked")
	@Override public void start(Stage stage) {
    	
        stage.setTitle("Load Bar Chart");
        
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        
        final BarChart<String,Number> bc =  new BarChart<String,Number>(xAxis,yAxis);
        bc.setLegendVisible(false);
        
        bc.setTitle("Load among different Cluster Nodes");
        xAxis.setLabel("Node IP Address");       
        //yAxis.setLabel("Load (MB)");
        String label_y_axis = "Load ";
        String load_unit = "";
        
        Series<String, Number> series = new XYChart.Series<String,Number>();
  
        Parameters parameters = getParameters();    
	    List<String> rawArguments = parameters.getRaw();
	      
	    // input param #0 : path to output file of nodetool
	    String path_output_nodetool = rawArguments.get(0);
        
       // read file line by line in Java using Scanner
        FileInputStream fis = null;
		try { fis = new FileInputStream(path_output_nodetool); } 
		catch (FileNotFoundException e) {
			System.err.println("ERROR : file "+path_output_nodetool+" NOT FOUND!");
			e.printStackTrace();
		}
        Scanner scanner = new Scanner(fis);
        
        List<Pair<String,Double>> pairs = new LinkedList<Pair<String,Double>>();
        
        Set<String> distinct_ips = new HashSet<String>();
        
        // saltare le prime 5 righe del file
        for(int i =0; i<5; i++){ scanner.nextLine(); }
        
        while(scanner.hasNextLine()){
        	String line = scanner.nextLine();  	
        	if( line.equals("") ){ break; }
        	StringTokenizer st = new StringTokenizer(line);
        	String ip = st.nextToken();
        	if(!distinct_ips.contains(ip)){
        		distinct_ips.add(ip);
            	st.nextToken(); st.nextToken(); st.nextToken();
            	Double load = Double.parseDouble(st.nextToken().replace(",", "."));
            	Pair<String,Double> p = new Pair<String,Double>(ip,load);
            	pairs.add(p);
            	
            	load_unit = st.nextToken(); // KB o MB o GB  - mi serve per l'etichetta dell'asse y
            	
        	}
        }   
        scanner.close();
        
        yAxis.setLabel(label_y_axis+" ("+load_unit+ ")");
        
        MyComparator c = new MyComparator();      
        pairs.sort(c);
           	
    	for(Pair<String, Double> p : pairs){
    		String ip = p.getKey();
    		double load = p.getValue();
    		System.out.println("adding pair : ( "+ip+" ; "+load+" )");
    		series.getData().add(new XYChart.Data<String,Number>(ip, load));      
    		
    	}
          
        Scene scene  = new Scene(bc,800,600);
        bc.getData().add(series);
        stage.setScene(scene);
        stage.show();
        
        setMaxBarWidth(40, 10, xAxis, bc);
        bc.widthProperty().addListener((obs,b,b1)->{
            Platform.runLater(()->setMaxBarWidth(60, 10, xAxis, bc));
        });
    }
    

    public static void main(String[] args) {
    	if(args.length<1){
    		System.err.println("Error: path to the output_file of nodetool is required as argument");
    		System.exit(-1);
    	}
        launch(args);
    }
}