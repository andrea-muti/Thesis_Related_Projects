package utils_ann;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Normalizer {
	
	String filepath;
	boolean has_headers;
	String separator;
	static String resultpath="resources/datasets/normalized_scaled12_formatted_overprovision_1h_complete_twitter_dataset.csv";
	//static String resultpath="resources/datasets/normalized_formatted_overprovision_95p_halfhour_complete_twitter_dataset.csv";
	//static String resultpath="resources/noSpike_normalized_dataset.csv";
	int num_columns;
	List<MinMaxHolder> min_max_values ;
	
	public Normalizer(String path, boolean has_headers, String separator){
		this.filepath=path;
		this.has_headers=has_headers;
		this.separator=separator;
		this.num_columns = count_columns();
		this.min_max_values = new ArrayList<MinMaxHolder>();
		create_file_result();
		compute_min_max_values();
	}
	
	public List<MinMaxHolder> get_MinMaxList(){
		return this.min_max_values;
	}

	
	private void compute_min_max_values(){
		try{
			for(int col_index = 0; col_index<this.num_columns; col_index++){
				double min = Double.MAX_VALUE;
				double max = Double.MIN_VALUE;
				BufferedReader reader = new BufferedReader(new FileReader(filepath));
				if(this.has_headers){
					reader.readLine();
				}
				String line = reader.readLine();
				while(line!=null){
					StringTokenizer st = new StringTokenizer(line,",");
					
					if(st.countTokens()==0){System.out.println("errore zero tokens");}
					
					for(int i = 0; i<col_index; i++){st.nextToken();} // salto i tokens precedenti alla colonna che mi interessa
					double value = 0;
				
					String token = st.nextToken();
					try{ value = Double.parseDouble(token);}
					catch(NumberFormatException e){
						System.out.println("error");
					}
				
					if( value<min ){ min=value; }
					if( value>max ){ max=value; }
		
					line = reader.readLine();
				}
				reader.close();
				//System.out.println("column #"+col_index+"    max: "+max+" | min: "+min);
				
				// FEDERICO DICE : raddoppiare il max value per far prevere meglio i picchi, dove le ANN hanno difficoltà
				double scaling_factor = 1.0;
				this.min_max_values.add( col_index, new MinMaxHolder(min, scaling_factor*max) );
			}
		}
		catch(Exception e){}
	}
	
	private int count_columns(){
		int result = 0;
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(this.filepath));			
			String line = reader.readLine();
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line,this.separator);
				result = st.countTokens();
				if(result==0){System.out.println("errore zero tokens");}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {} 
		return result;
	}
	
	private void create_file_result(){
		PrintWriter writer;
		try {
			writer = new PrintWriter(resultpath, "UTF-8");
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {}

	}
	
	
	public void normalize() throws Exception {
		
		PrintWriter writer;
		try {
			writer = new PrintWriter(resultpath, "UTF-8") ;			
			
			BufferedReader reader = new BufferedReader(new FileReader(filepath));
			
			// se c'è l'header lo ricopio
			if(this.has_headers){
				String header = reader.readLine();
				writer.write(header+"\n");
			}
			
			String line = reader.readLine();
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line,this.separator);
				String resultline = "";
				int i = 0;
				while(st.hasMoreTokens()){
					String stringtoken = st.nextToken();
					Double value = Double.parseDouble(stringtoken);
					double value_normalized;
					
					boolean cutter = false;
					double cut_threshold = 140.0;
					
					if(cutter){
						if(!st.hasMoreElements()){ // sono al token con il carico (l'ultimo)
							if( value >= cut_threshold ){ value=cut_threshold; }
							value_normalized = normalize_value(value, this.min_max_values.get(i).getMin(), cut_threshold);
						}
						else{ // non sono all'ultimo token, procedo normalmente
							value_normalized = normalize_value(value, this.min_max_values.get(i).getMin(), this.min_max_values.get(i).getMax());
						}
					}else{
						value_normalized = normalize_value(value, this.min_max_values.get(i).getMin(), this.min_max_values.get(i).getMax());
					}
					
				    String value_formatted = String.format("%.16f", value_normalized).replace(",", ".");
					resultline = resultline+value_formatted+separator;
				    i++;
				}
				
				writer.write(resultline+"\n");
				line = reader.readLine();
			}
			
			reader.close();
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {}
		
	}
	
	private double normalize_value(double value, double min, double max){
			double normalized = (value - min) / (max - min);
			return normalized;
	}
	
	@SuppressWarnings("unused")
	private double denormalize_value(double normalized, double min, double max) {
		double denormalized = (normalized * (max - min) + min);
		return denormalized;
	}
	
	public static void main(String[] args){
		
		String inpath = "resources/datasets/formatted_overprovision_1h_complete_twitter_dataset.csv";
		//String inpath = "resources/datasets/formatted_overprovision_95p_halfhour_complete_twitter_dataset.csv";
		//String path = "resources/noSpikes_to_renormalize_datafile.csv";
		Normalizer norm = new Normalizer(inpath, false, ",");
		List<MinMaxHolder> lista = norm.get_MinMaxList();
	
		for(int i = 0; i<lista.size(); i ++ ){
			System.out.println("column #"+i+" | min: "+lista.get(i).getMin()+" , max: "+lista.get(i).getMax());
		}
		System.out.println("\n * start normalization");
		try {
			norm.normalize();
		} catch (Exception e) {System.out.println("eccezione "+e.getMessage());}
		System.out.println(" * normalization of "+inpath+" finished");
		System.out.println(" * output file : "+resultpath);
	}

}


class MinMaxHolder{
	double[] values = new double[2];
 	
	MinMaxHolder(double min,double max){
		values[0]=min;
		values[1]=max;
	}
	double getMin(){return this.values[0];}
	double getMax(){return this.values[1];}
}
