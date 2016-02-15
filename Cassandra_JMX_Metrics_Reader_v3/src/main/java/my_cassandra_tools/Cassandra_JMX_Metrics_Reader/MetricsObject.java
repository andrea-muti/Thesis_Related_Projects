package my_cassandra_tools.Cassandra_JMX_Metrics_Reader;

public class MetricsObject {
	
	private String ip_address;
	private String jmx_port_number;
	
	
	private ClientRequestMetricsObject client_request_read_metrics;
	private ClientRequestMetricsObject client_request_write_metrics;
	
	private NodeMetricsObject node_read_metrics;
	private NodeMetricsObject node_write_metrics;
	
	public MetricsObject(String ip, String jmx,
			ClientRequestMetricsObject crr_metrics, ClientRequestMetricsObject crw_metrics,
			NodeMetricsObject nr_metrics, NodeMetricsObject nw_metrics){
		
		this.setIPAddress(ip);
		this.setJmxPortNumber(jmx);
		this.setClientRequestReadMetrics(crr_metrics);
		this.setClientRequestWriteMetrics(crw_metrics);
		this.setNodeReadMetrics(nr_metrics);
		this.setNodeWriteMetrics(nw_metrics);
		
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

	public ClientRequestMetricsObject getClientRequestWriteMetrics() {
		return client_request_write_metrics;
	}

	public void setClientRequestWriteMetrics(ClientRequestMetricsObject client_request_write_metrics) {
		this.client_request_write_metrics = client_request_write_metrics;
	}

	public NodeMetricsObject getNodeReadMetrics() {
		return node_read_metrics;
	}

	public void setNodeReadMetrics(NodeMetricsObject node_read_metrics) {
		this.node_read_metrics = node_read_metrics;
	}

	public NodeMetricsObject getNodeWriteMetrics() {
		return node_write_metrics;
	}

	public void setNodeWriteMetrics(NodeMetricsObject node_write_metrics) {
		this.node_write_metrics = node_write_metrics;
	}
	
}
