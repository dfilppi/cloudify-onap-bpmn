package co.cloudify.bpmn;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class Test implements JavaDelegate {

	public void execute(DelegateExecution execution) throws Exception {
		execution.setVariable("test","BLORF");
	}
}
