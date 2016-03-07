package RTReaderMovingAvg;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class MovingAverageWindow {
 
	private final Queue<Double> window = new LinkedList<Double>();
    private final int period;
    private double sum;

    public MovingAverageWindow(int period) {
        assert period > 0 : "Period must be a positive integer";
        this.period = period;
    }

    public void newNum(double num) {
        sum += num;
        window.add(num);
        if (window.size() > period) {
            sum -= window.remove();
        }
    }

    public double getAvg() {
        if (window.isEmpty()) return 0; // technically the average is undefined
        return sum / window.size();
    }
    
    public double get95percentile() {
    	if(window.isEmpty()) return 0;
    	double index95 = ((window.size() / 100.0 ) * 95.0) - 1;
    	Object[] array =  window.toArray();
    	Arrays.sort(array);
    	double p95 = (double) array[(int)index95];
    	return p95;
    }

    public static void main(String[] args) {
    
        double[] testData = new double[100];
        for(int i = 0; i<100;i++){
        	testData[i] = (i+1);
        }
        int[] windowSizes = {50};
        for (int windSize : windowSizes) {
            MovingAverageWindow ma = new MovingAverageWindow(windSize);
            for (double x : testData) {
                ma.newNum(x);
                System.out.println("Next number = " + x + ", SMA = " + ma.getAvg());
                System.out.println(" AVG = "+ma.getAvg());
                System.out.println(" 95p = "+ma.get95percentile());
            }
            System.out.println(" AVG = "+ma.getAvg());
            System.out.println(" 95p = "+ma.get95percentile());
            System.out.println();
        }
    }
}