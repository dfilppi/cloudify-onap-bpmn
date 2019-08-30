package co.cloudify.bpmn;

import java.util.Map;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.onap.so.cloudify.client.APIV31Impl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudifyUninstallBlueprintDelegate extends AbstractJavaDelegate {
	private static Logger log = LoggerFactory.getLogger(CloudifyUninstallBlueprintDelegate.class);

	// limitation: only archives with blueprint.yaml will work
	private final static String UNINSTALL_WF = "uninstall";

	public void execute(DelegateExecution execution) throws Exception {
		checkInputs(execution);
		Map<String,String> credentials = (Map<String,String>)execution.getVariable(INP_CREDENTIALS_KEY);		
		APIV31Impl client = getCloudifyClient(credentials);

		String did = getDeploymentId(execution);
		
		// Run install workflow
		try {
			runWorkflow(UNINSTALL_WF, execution, client, did);
		} catch (Exception e) {
			log.error("Cloudify install workflow failed: " + e.getMessage());
			throw e;
		}
	}

	/******************************************************************
	 * PRIVATE METHODS
	 ******************************************************************/

	private void checkInputs(DelegateExecution execution) throws Exception{
		StringBuilder sb = new StringBuilder();
		
		if(!execution.hasVariable(INP_CREDENTIALS_KEY)) {
			sb.append("required input not supplied: "+INP_CREDENTIALS_KEY);
		}
		else {
			Map<String,String> creds = (Map<String,String>)execution.getVariable(INP_CREDENTIALS_KEY);
			if(!creds.containsKey("url")) {
				sb.append("required credentials entry not supplied: url");
			}
			if(!creds.containsKey("username")) {
				sb.append("required credentials entry not supplied: username");
			}
			if(!creds.containsKey("password")) {
				sb.append("required credentials entry not supplied: password");
			}
			if(!creds.containsKey("tenant")) {
				sb.append("required credentials entry not supplied: tenant");
			}
		}
		if(!execution.hasVariable(INP_CREDENTIALS_KEY)) {
			sb.append("required input not supplied: "+INP_CREDENTIALS_KEY);
		}
		if(!execution.hasVariable(INP_BLUEPRINT_NAME_KEY)) {
			sb.append("required input not supplied: "+INP_BLUEPRINT_NAME_KEY);
		}

		if(sb.length()>0) {
			throw new Exception(sb.toString());
		}
	}
}
