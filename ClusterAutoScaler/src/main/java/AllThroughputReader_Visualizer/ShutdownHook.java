package AllThroughputReader_Visualizer;

import java.io.IOException;

import AllThroughputReader_Visualizer.AllThroughputReader.ThroughputReader;

public class ShutdownHook extends Thread {
	private ThroughputReader[] readers;
	public ShutdownHook(ThroughputReader[] readers) {
		this.readers=readers;
	}
	
	@Override
    public void run(){
        try {
			for(int i=0; i<this.readers.length; i++){
				this.readers[i].writer.close();
			}
		} catch (IOException e) {
			System.err.println(" - ERROR closing one of the throughput result file");
		}
         
    }
}
