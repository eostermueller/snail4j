package com.github.eostermueller.snail4j;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.github.eostermueller.snail4j.launcher.CannotFindSnail4jFactoryClass;
import com.github.eostermueller.snail4j.launcher.ConfigVariableNotFoundException;
import com.github.eostermueller.snail4j.launcher.Configuration;
import com.github.eostermueller.snail4j.launcher.Event;
import com.github.eostermueller.snail4j.launcher.Suite;
import com.github.eostermueller.snail4j.processmodel.ProcessModelBuilder;
import com.github.eostermueller.snail4j.processmodel.ProcessModelSingleton;

/**
 * Placeholder class to handle progress meter for browser UI, because this could take a few minutes.
 * This event is executed as late as conceivably possible to indicate that 
 * the application is ready to service requests.
 * 
 * @author erikostermueller
 *
 */
@Component
public class SpringBootSnail4J implements ApplicationListener<ApplicationReadyEvent> {
	private static final String WIREMOCK_PORT_PROPERTY = "wiremockPort";

	private static final String H2_PORT_PROPERTY = "h2Port";

	private static final String SUT_PORT_PROPERTY = "sutAppPort";

	private static final String GLOWROOT_PORT_PROPERTY = "glowrootPort";

	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	private static ProcessModelSingleton PROCESS_MODEL_SINGLETON = null;
	
	public static ProcessModelSingleton getProcessModelSingleton() throws ConfigVariableNotFoundException, Snail4jException {
		return PROCESS_MODEL_SINGLETON;
	}
	public static void setProcessModelSingleton(ProcessModelSingleton val){
		PROCESS_MODEL_SINGLETON = val;
	}
	
	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {

		try {
			Configuration cfg = DefaultFactory.getFactory().getConfiguration();
			install(cfg);
			DefaultFactory.getFactory().getEventHistory().getEvents().add(
					Event.create("snail4j install complete.") );
			initProcessModel();
			initPorts(cfg);
			DefaultFactory.getFactory().getEventHistory().getEvents().add(
					Event.create("snail4j startup complete.") );
		} catch (Snail4jException e) {
			try {
				ProcessModelSingleton.getInstance().setCauseOfSystemFailure(e);
				DefaultFactory.getFactory().getEventHistory().addException("Exception during snail4j startup.", e);
			} catch (Snail4jException e1) {
			}
		}
	}

	static class TcpTarget {
		static TcpTarget create(String hostname, int port, String name, String snail4jPropertyName) {
			TcpTarget t = new TcpTarget();
			t.hostname = hostname;
			t.port = port;
			t.name = name;
			t.snail4jPropertyName = snail4jPropertyName;
			return t;
		}
		boolean active = false;
		String hostname = null;
		int port = -1;
		String name = null;
		String snail4jPropertyName = null;
		public String toString() {
			return String.format("Name=%s, Hostname=%s,Property=%s,port=%d\n", 
					this.name,
					this.hostname,
					this.snail4jPropertyName,
					this.port);
		}
	}
	private void initPorts(Configuration cfg) throws Snail4jException {
	
		List<String> errors = new ArrayList<String>();
		int availableUdpPort = OsUtils.getAvailableUdpPort( (int)cfg.getStartJMeterNonGuiPort(), (int)cfg.getMaxJMeterNonGuiPort() );
		if (availableUdpPort >= 0) {
			cfg.setJMeterNonGuiPort(availableUdpPort);
			LOGGER.debug(String.format("Cfg instance [%d] JMeterNonGuiPort availableUdpPort [%d]", cfg.hashCode(), availableUdpPort) );
		} else {
			errors.add( DefaultFactory.getFactory().getMessages().getNoUdpPortsAvailableBetween((int)cfg.getStartJMeterNonGuiPort(), (int)cfg.getMaxJMeterNonGuiPort()));
		}
		LOGGER.info( String.format("JMeter non-gui port is [%d].  Start [%d] and max port [%d].", cfg.getJMeterNonGuiPort(), cfg.getStartJMeterNonGuiPort(), cfg.getMaxJMeterNonGuiPort() ) );

		
		int timeoutMs = 1000;
		List<TcpTarget> targets = new ArrayList<TcpTarget>();
		targets.add( TcpTarget.create(cfg.getWiremockHostname(), cfg.getWiremockPort(), "Wiremock", WIREMOCK_PORT_PROPERTY) );
		targets.add( TcpTarget.create(cfg.getH2Hostname(),cfg.getH2Port(),"H2",H2_PORT_PROPERTY) );
		targets.add( TcpTarget.create(cfg.getSutAppHostname(), cfg.getSutAppPort(), "SUT",SUT_PORT_PROPERTY) );
		targets.add( TcpTarget.create(cfg.getSutAppHostname(), cfg.getGlowrootPort(), "Glowroot",GLOWROOT_PORT_PROPERTY) );
		
		for( TcpTarget t : targets ) {
			if ( OsUtils.isTcpPortActive(t.hostname, t.port, timeoutMs) ) {
				t.active = true;
				String error = DefaultFactory.getFactory().getMessages()
						.tcpPortConflict(t.name,t.hostname,t.port,t.snail4jPropertyName);
				errors.add( error );
			}
			LOGGER.info( "Snail4j Port status: " + t.toString() );
		}
		String portInitStatus = DefaultFactory.getFactory().getMessages()
				.portInitStatus(errors);
		
		DefaultFactory.getFactory().getEventHistory().getEvents().add(
				Event.create(portInitStatus) );

		if (errors.size() > 0) {
			throw new Snail4jMultiException(errors);
		}
		
	}
	private void initProcessModel() throws ConfigVariableNotFoundException, Snail4jException {
		ProcessModelSingleton.getInstance();
	}
	private void install(Configuration cfg) {
		String path = new PathUtil().getBaseClassspath();
		if (path.contains(PathUtil.JAR_SUFFIX)) { //only install if launched using "java -jar".  Elsewise, installs happen with every "backend" build, because Spring Boot is launched during integration testing. 
			dispInstallBanner();
			try {
				Snail4jInstaller snail4jInstaller = DefaultFactory.getFactory().createNewInstaller();
				
				LOGGER.info("online: " + cfg.isMavenOnline() );
				LOGGER.info("snail4j maven repo: " + cfg.isSnail4jMavenRepo() );
				snail4jInstaller.install();
				
		    	LOGGER.info("Install finished.  Ready to load test!");

			} catch (Snail4jException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			this.LOGGER.info("Install has been skipped!   It only runs when WindTunnel is launched with 'java -jar'. Startup: [" + path + "]" );
		}
	}

	private void dispInstallBanner() {
		this.LOGGER.info("##########################" );
		this.LOGGER.info("##########################" );
		this.LOGGER.info("####                 #####" );
		this.LOGGER.info("####  I N S T A L L  #####" );
		this.LOGGER.info("####                 #####" );
		this.LOGGER.info("##########################" );
		this.LOGGER.info("##########################" );
	}

}
