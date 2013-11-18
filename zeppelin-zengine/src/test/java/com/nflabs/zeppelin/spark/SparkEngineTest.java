package com.nflabs.zeppelin.spark;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.nflabs.zeppelin.zengine.Z;
import com.nflabs.zeppelin.zengine.ZException;

public class SparkEngineTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testInit() throws ZException {
		Z.configure();
		SparkEngine se = new SparkEngine(Z.conf(), "test", null);
		se.interpret("println(\"HELLO world!\");");
		se.stop();
	}

	
	@Test
	public void testBind() throws ZException {
		Z.configure();
		SparkEngine se = new SparkEngine(Z.conf(), "test", null);
		AtomicLong a = new AtomicLong(0);
		assertEquals(0, a.get());
		se.bind("v1", a);
		se.interpret("v1.incrementAndGet()");
		assertEquals(1, a.get());
		se.stop();
	}

}
