package prove_metrics;

public class MetricsObject {
	
	private String ip_address;
	private String jmx_port_number;
	
	
	private ClientRequestMetricsObject client_request_read_metrics;

	
	public MetricsObject(String ip, String jmx, ClientRequestMetricsObject crr_metrics){
		
		this.setIPAddress(ip);
		this.setJmxPortNumber(jmx);
		this.setClientRequestReadMetrics(crr_metrics);
		
	}

	public String getIPAddress() {
		return ip_address;
	}

	public void setIPAddress(String ip_address) {
		this.ip_address = ip_address;
	}

	public String getJmxPortNumber() {
		return jmx_port_number;
	}

	public void setJmxPortNumber(String jmx_port_number) {
		this.jmx_port_number = jmx_port_number;
	}

	public ClientRequestMetricsObject getClientRequestReadMetrics() {
		return client_request_read_metrics;
	}

	public void setClientRequestReadMetrics(ClientRequestMetricsObject client_request_read_metrics) {
		this.client_request_read_metrics = client_request_read_metrics;
	}

	
}
