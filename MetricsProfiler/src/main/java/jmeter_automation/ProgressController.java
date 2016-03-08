package jmeter_automation;

public interface ProgressController {
	public void processStarted(Process process, long estimatedFinishingTime);
	public void currentProgress(double percentage);
	public void finished();
}