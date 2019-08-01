package co.cloudify.bpmn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.onap.so.cloudify.client.APIV31;
import org.onap.so.cloudify.client.APIV31Impl;
import org.onap.so.cloudify.client.DeploymentV31;
import org.onap.so.cloudify.client.ExecutionV31;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudifyInstallBlueprintDelegate extends AbstractJavaDelegate {
	private static Logger log = LoggerFactory.getLogger(CloudifyInstallBlueprintDelegate.class);

	// limitation: only archives with blueprint.yaml will work
	private final static String MAIN_YAML = "blueprint.yaml";
	private final static String INSTALL_WF = "install";

	public static void main(String[] args) throws Exception {
		CloudifyInstallBlueprintDelegate a = new CloudifyInstallBlueprintDelegate();
		a.execute(null);
	}

	public void execute(DelegateExecution execution) throws Exception {
		APIV31Impl client = getCloudifyClient(execution);

		// Upload blueprint
		String bid = null;
		try {
			uploadBlueprint(execution, client);
		} catch (Exception e) {
			log.error("Cloudify blueprint upload failed: " + e.getMessage());
			throw e;
		}

		// Create deployment
		String did = null;
		try {
			did = createDeployment(execution, client, bid);
		} catch (Exception e) {
			log.error("Cloudify deployment creation failed: " + e.getMessage());
			throw e;
		}

		// Run install workflow
		try {
			runWorkflow(INSTALL_WF, execution, client, did);
		} catch (Exception e) {
			log.error("Cloudify install workflow failed: " + e.getMessage());
			throw e;
		}
	}


	// TODO: deployment and blueprint names must be derived from request
	private String createDeployment(DelegateExecution execution, APIV31Impl client, String bid) {
		DeploymentV31 deployment = client.createDeployment("test", "test", new HashMap<String, String>(), false, false,
				APIV31.Visibility.TENANT);
		return deployment.getDeployment_id();
	}

	private APIV31Impl getCloudifyClient(DelegateExecution execution) {
		Map<String, String> creds = getCredentials(execution);
		APIV31Impl client = APIV31Impl.create(creds.get("tenant"), creds.get("username"), creds.get("password"),
				creds.get("url"));
		return client;
	}

	/**
	 * Suck in an archive and push it to Cloudify
	 * 
	 * @param execution
	 * @param client
	 * @throws Exception
	 */
	private void uploadBlueprint(DelegateExecution execution, APIV31Impl client) throws Exception {
		// client.uploadBlueprint(blueprint_id, main_yaml_filename, visibility,
		// archive);

		// Get blueprint from database
		String blueprint = getBlueprint(execution, client);
		// Create archive
		File archive = this.createBlueprintArchive(blueprint);

		// Upload
		FileInputStream fis = null;
		byte[] data = new byte[(int) archive.length()];
		try {
			fis = new FileInputStream(archive);
			fis.read(data);
		} finally {
			fis.close();
		}
		// TODO blueprint name should be derived from request
		client.uploadBlueprint("test", MAIN_YAML, APIV31.Visibility.TENANT, data);
	}

	/**
	 * Get blueprint from database. Single file. TODO THIS IS A STUB, FINISH
	 * 
	 * @param execution
	 * @param client
	 * @return
	 */
	private String getBlueprint(DelegateExecution execution, APIV31Impl client) {
		// There no generic artifact storage in the database, so we
		// use position held for HEAT templates
		// TODO

		String blueprint = "tosca_definitions_version: cloudify_dsl_1_3\n" + "imports:\n"
				+ "  - http://www.getcloudify.org/spec/cloudify/4.5/types.yaml\n" + "node_templates:\n" + "  node:\n"
				+ "    type: cloudify.nodes.Root\n";

		return blueprint;
	}

	/**
	 * Create a valid blueprint archive from the supplied blueprint file contents
	 * 
	 * @param blueprint the blueprint
	 * @return a File object pointing to the archive
	 */
	private File createBlueprintArchive(String blueprint) {
		String dirname = makeTempFileName("sobpmn", null);
		File dir = new File(dirname);
		if (!dir.mkdir()) {
			log.error("archive directory creation failed");
			return null;
		}
		try {
			FileOutputStream fos = new FileOutputStream(dirname + File.separator + "blueprint.yaml");
			fos.write(blueprint.getBytes());
			fos.close();
		} catch (Exception e) {
			log.error("error writing blueprint file:" + e.getMessage());
			return null;
		}

		File zip = null;
		try {
			zip = createSimpleZipFile(dirname);
		} catch (Exception e) {
			log.error("error creating zip file: " + e.getMessage());
			return null;
		}

		return zip;
	}

	/**
	 * Creates a zip file that includes the supplied directory. File placed in same
	 * directory as supplied directory. NOT a general zip function.
	 * 
	 * @param dirname directory to zip
	 * @return A File object pointing to the zip
	 * @throws Exception
	 */
	private File createSimpleZipFile(String dirname) throws Exception {
		final Path sourceDir = Paths.get(dirname);
		String zipFileName = dirname.concat(".zip");
		final ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(zipFileName));
		Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
				try {
					Path targetFile = file.subpath(1, file.getNameCount());
					outputStream.putNextEntry(new ZipEntry(targetFile.toString()));
					byte[] bytes = Files.readAllBytes(file);
					outputStream.write(bytes, 0, bytes.length);
					outputStream.closeEntry();
					return FileVisitResult.CONTINUE;
				} catch (IOException e) {
					log.error(e.getMessage());
					throw new UncheckedIOException(e);
				}
			}
		});
		outputStream.close();
		return new File(zipFileName);
	}

	/**
	 * Create a temporary file name/path based in the system temp dir
	 * 
	 * @param prefix added to generated name at beginning followed by '-'
	 * @param suffix added to end (if not null), following .'.'
	 * @return the name
	 */
	private String makeTempFileName(String prefix, String suffix) {
		String extension = suffix;
		String start = prefix + "-";
		if (prefix == null) {
			start = "";
		}
		if (suffix == null) {
			extension = "";
		}
		String path = System.getProperty("java.io.tmpdir") + File.separator + start + UUID.randomUUID().toString()
				+ extension;
		log.debug("temp path=" + path);
		return path;
	}
}
