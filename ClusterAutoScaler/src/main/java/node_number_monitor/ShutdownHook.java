package node_number_monitor;

import java.io.BufferedWriter;
import java.io.IOException;

public class ShutdownHook extends Thread {
	BufferedWriter writer;
	String file_path;
	public ShutdownHook(BufferedWriter w, String path) {
		this.writer=w;
		this.file_path=path;
	}
	
	@Override
    public void run(){
        try {
			writer.close();
			System.out.println(" - result file "+this.file_path+" successfully closed");
		} catch (IOException e) {
			System.err.println(" - ERROR closing the result file "+this.file_path);
		}
         
    }
}
