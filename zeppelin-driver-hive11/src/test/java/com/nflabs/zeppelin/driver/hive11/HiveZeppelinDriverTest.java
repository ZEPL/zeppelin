package com.nflabs.zeppelin.driver.hive11;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

import junit.framework.TestCase;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.nflabs.zeppelin.conf.ZeppelinConfiguration;
import com.nflabs.zeppelin.conf.ZeppelinConfiguration.ConfVars;
import com.nflabs.zeppelin.result.Result;

public class HiveZeppelinDriverTest extends TestCase {

	private File tmpDir;

	public HiveZeppelinDriverTest() throws IOException {
		super();
	}
	  
	@Before
	public void setUp() throws Exception {
		super.setUp();
        tmpDir = new File(System.getProperty("java.io.tmpdir")+"/ZeppelinLTest_"+System.currentTimeMillis());                
        tmpDir.mkdir();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testQuery() throws URISyntaxException, IOException {
		System.setProperty(ConfVars.ZEPPELIN_HOME.getVarName(), new File("./").getAbsolutePath());
		HiveZeppelinDriver driver = new HiveZeppelinDriver(ZeppelinConfiguration.create(), new URI("jdbc:hive2://"), new URLClassLoader(new URL[]{}, Thread.currentThread().getContextClassLoader()));

		// create table
		Result res = driver.query("create table if not exists test(a INT)");

		// show table
		res = driver.query("show tables");		
		assertEquals("test", res.getRows().get(0)[0]);

		// add some data
		FileOutputStream out = new FileOutputStream(new File("./warehouse/test/data"));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
        bw.write("1\n");
	    bw.write("2\n");
	    bw.close();

	    // count
	    res = driver.query("select count(*) from test");
	    assertEquals(new Long(2), res.getRows().get(0)[0]);
	}

}
