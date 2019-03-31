package com.github.eostermueller.tjp.launcher.agent;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.eostermueller.tjp.launcher.agent.runner.StdoutProcessRunner;


/**
 * These tests confirm that we can start a headless OS process and assess success/failures
 * by looking at unstructured messages in the stdout.
 * @author erikostermueller
 *
 */
public class BasicProcessManagementTest {
	boolean ynStateChanged = false;
	
	 @Rule
	    public TemporaryFolder testFolder = new TemporaryFolder();
	 File tmpFolder = null;
	 @Before
	 public void setup() throws IOException {
		 this.tmpFolder = testFolder.newFolder();
	 }
	@Test
	public void canRunJavaProgramAndReadStdout() throws Exception {
		ProcessKey key = ProcessKey.create(this.getClass().getCanonicalName(), Level.CHILD, "executingJavaClass");
		
		TestConfiguration t = new TestConfiguration();
		
		MockServerProcess testOne = new MockServerProcess(this.tmpFolder,t.getJavaHome(),key.getTinyId());
		testOne.setSleepMsAfterStartup(0);
		testOne.setSleepMsBeforeStartup(0);
		testOne.compile();
		
		StdoutProcessRunner p = new StdoutProcessRunner(key);
		p.setProcessBuilder( testOne.getProcessBuilder() );
		
		StateChangeListener sscl = new StateChangeListener() {
			@Override
			public void stateHasChanged(ProcessKey processKey, State newState) {
				BasicProcessManagementTest.this.ynStateChanged = true;
			}
		};
		
		StdoutStateChanger ssc = new AbstractStdoutStateChanger() {
			@Override
			public void evaluateStdoutLine(String s) throws TjpException {
				if (s.indexOf("Startup Complete") >=0 ) {
					this.fireStateChange(key, State.STARTED);
				}
			}
		};
		ssc.registerStateChangeListener(sscl);

		p.setStdoutStateChanger(ssc);
		p.start();
		Thread.sleep(1000);
		Assert.assertTrue(this.ynStateChanged);
		
	}

}
