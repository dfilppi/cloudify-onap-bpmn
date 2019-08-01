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

	public static void main(String[] args) throws Exception {
		CloudifyUninstallBlueprintDelegate a = new CloudifyUninstallBlueprintDelegate();
		a.execute(null);
	}

	public void execute(DelegateExecution execution) throws Exception {
		APIV31Impl client = getCloudifyClient(execution);

		String did = getDeploymentId(execution);
		
		// Run install workflow
		try {
			runWorkflow(UNINSTALL_WF, execution, client, did);
		} catch (Exception e) {
			log.error("Cloudify install workflow failed: " + e.getMessage());
			throw e;
		}
	}


	private APIV31Impl getCloudifyClient(DelegateExecution execution) {
		Map<String, String> creds = getCredentials(execution);
		APIV31Impl client = APIV31Impl.create(creds.get("tenant"), creds.get("username"), creds.get("password"),
				creds.get("url"));
		return client;
	}



}
