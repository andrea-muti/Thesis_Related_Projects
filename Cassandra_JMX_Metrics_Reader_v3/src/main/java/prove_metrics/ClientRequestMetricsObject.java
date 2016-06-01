package prove_metrics;

public class ClientRequestMetricsObject {
	
	private double mean_lat;
	private double percentile_95_lat;
	private String duration_unit;
	
	private double one_min_throughput;
	private String rate_unit ;

	
	public ClientRequestMetricsObject( double mean_latency, double percentile_95_latency, 
									   String durationUnit, double oneMinRate, String rateUnit ){
		this.mean_lat=mean_latency;
		this.percentile_95_lat=percentile_95_latency;
		this.duration_unit=durationUnit;
		this.one_min_throughput=oneMinRate;
		this.rate_unit=rateUnit;
	}

	public double getMeanLatency() {
		return mean_lat;
	}

	public void setMeanLatency(double mean_lat) {
		this.mean_lat = mean_lat;
	}

	public double getPercentile95Latency() {
		return percentile_95_lat;
	}

	public void setPercentile95Latency(double percentile_95_lat) {
		this.percentile_95_lat = percentile_95_lat;
	}

	public String getDurationUnit() {
		return duration_unit;
	}

	public void setDurationUnit(String duration_unit) {
		this.duration_unit = duration_unit;
	}

	public double getOneMinuteThroughput() {
		return one_min_throughput;
	}

	public void setOneMinuteThroughput(double one_min_throughput) {
		this.one_min_throughput = one_min_throughput;
	}

	public String getRateUnit() {
		return rate_unit;
	}

	public void setRateUnit(String rate_unit) {
		this.rate_unit = rate_unit;
	}
}
