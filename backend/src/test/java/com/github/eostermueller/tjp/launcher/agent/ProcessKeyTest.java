package com.github.eostermueller.tjp.launcher.agent;

import static org.junit.Assert.*;

import org.junit.Test;

import com.github.eostermueller.havoc.PerfGoatException;
import com.github.eostermueller.havoc.launcher.CannotFindTjpFactoryClass;
import com.github.eostermueller.havoc.launcher.Level;
import com.github.eostermueller.havoc.launcher.ProcessKey;

public class ProcessKeyTest {

	@Test
	public void canUnmarshalProcessKey() throws PerfGoatException {
		String suite = "MySystem";
		
		String processType = "LoadGenerator";
		String myHostName = "foo.com";
		long unique = 235;
		long pid = 678;
		ProcessKey key = ProcessKey.create(suite,Level.PARENT,processType, myHostName, unique, pid);
		
		assertEquals("MySystem!PARENT!LoadGenerator!foo.com!235!678", key.getKey() );
		
	}
	@Test
	public void canCloneProcessKey() throws PerfGoatException {
		String suite = "MySystem";
		String processType = "LoadGenerator";
		String myHostName = "foo.com";
		long unique = 235;
		long pid = 678;
		ProcessKey key = ProcessKey.create(suite,Level.PARENT,processType, myHostName, unique, pid);
		
		ProcessKey key2 = ProcessKey.create(key.getKey() );
				
		assertEquals(key.getKey(), key2.getKey());
	}
	@Test
	public void canParseProcessKeyParts() throws NumberFormatException, CannotFindTjpFactoryClass {

		ProcessKey key = ProcessKey.create("MySystem!PARENT!LoadGenerator!foo.com!353!235");
		
		assertEquals( "MySystem", key.getSuite());
		assertEquals( Level.PARENT, key.getLevel() );
		assertEquals( "LoadGenerator", key.getProcessType() );
		assertEquals( "foo.com", key.getHostName());
		assertEquals( 353, key.getTinyId() );
		assertEquals( 235, key.getPid());
		
		
		
	}

}
