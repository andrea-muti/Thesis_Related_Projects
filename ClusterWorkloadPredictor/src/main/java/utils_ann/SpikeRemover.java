package utils_ann;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;

public class SpikeRemover {
	
	public static void main(String[] args){
		String input_file = "normalized_formatted_out-romasicura-count-per-min.csv";
		String input_path = "resources/"+input_file;
		String resultpath="resources/noSpikes_to_renormalize_datafile.csv";
		boolean has_headers = false;
		int num_colonna_con_load = 5; // partendo da 0
		
		double soglia_spike = 0.5;
		
		PrintWriter writer;
		
		
		try{			
			
			writer = new PrintWriter(resultpath, "UTF-8") ;		
			
			BufferedReader reader = new BufferedReader(new FileReader(input_path));
			if(has_headers){
				reader.readLine();
			}
			String line = reader.readLine();
			while(line!=null){
				StringTokenizer st = new StringTokenizer(line,",");
				
				if(st.countTokens()==0){System.out.println("errore zero tokens");}
				
				String to_write = "";
				for(int i = 0; i<num_colonna_con_load; i++){to_write=to_write+st.nextToken()+",";} // salto i tokens precedenti alla colonna che mi interessa
				double value = 0;
			
				String token = st.nextToken();
				try{ value = Double.parseDouble(token);}
				catch(NumberFormatException e){
					System.out.println("error");
				}
				if( value > soglia_spike ){ value = soglia_spike; } 
				to_write=to_write+value;
				writer.write(to_write+"\n");
				line = reader.readLine();
				
			}
			reader.close();
			writer.close();

		}
		catch(Exception e){}
		
		System.out.println("finished");
	}
}
