package AllCPUReader_Visualizer;

import java.io.IOException;

import AllCPUReader_Visualizer.AllCPUReader.CpuReader;

public class ShutdownHook extends Thread {
	private CpuReader[] readers;
	public ShutdownHook(CpuReader[] readers) {
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
