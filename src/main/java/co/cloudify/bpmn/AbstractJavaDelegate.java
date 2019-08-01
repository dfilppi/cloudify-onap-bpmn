package co.cloudify.bpmn;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.onap.so.cloudify.client.APIV31Impl;
import org.onap.so.cloudify.client.ExecutionV31;

/**
 * Adds a few utilities to the JavaDelegate interface
 * @author dewayne
 *
 */
public abstract class AbstractJavaDelegate implements JavaDelegate {
	protected static final String CFY_CORRELATION_ID = "CFY_CORRELATION_ID";

	@Override
	public abstract void execute(DelegateExecution execution) throws Exception;
	
	protected void runWorkflow(String workflowId, DelegateExecution execution, APIV31Impl client, String did) {
		ExecutionV31 exe = client.runExecution(workflowId, "test", new HashMap<String,String>(), false, false, false, null, 60, false);
		//TODO: check result
	}

	/**
	 * Get a deployment ID.  Just returns the supplied correlation id
	 * 
	 * @param execution
	 * @return
	 */
	protected String getDeploymentId(DelegateExecution execution) {
		return (String)execution.getVariable(CFY_CORRELATION_ID);
	}

	// TODO: Implement with cloud site info
	protected Map<String, String> getCredentials(DelegateExecution execution) {
		Map<String, String> creds = new HashMap<>();

		creds.put("tenant", "default_tenant");
		creds.put("username", "admin");
		creds.put("password", "admin");
		creds.put("url", "http://localhost:80");
		return creds;
	}



}
