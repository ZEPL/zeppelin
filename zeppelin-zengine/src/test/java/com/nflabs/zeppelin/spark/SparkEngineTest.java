package com.nflabs.zeppelin.spark;

import static org.junit.Assert.*;

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
	public void test() throws ZException {
		Z.configure();
		SparkEngine se = new SparkEngine(Z.conf(), "test", null);
		se.interpret("println(\"HELLO world!\");");
		se.stop();
	}

}
