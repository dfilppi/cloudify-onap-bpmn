package co.cloudify.bpmn;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.camunda.bpm.spring.boot.starter.event.PostDeployEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@EnableProcessApplication
public class Test {
	@Autowired
	private ProcessEngine processEngine;	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());	

	public static void main(String[] args) {
		/*Logger l = (Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		l.setLevel(ch.qos.logback.classic.Level.DEBUG);*/
		
	    SpringApplication.run(Test.class, args);
		
	}

	@EventListener
	  public void onPostDeploy(PostDeployEvent event) {
	    logger.info("postDeploy: {}", event);
	  }

}
