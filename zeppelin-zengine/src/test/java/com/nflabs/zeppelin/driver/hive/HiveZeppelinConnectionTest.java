package com.nflabs.zeppelin.driver.hive;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.nflabs.zeppelin.conf.ZeppelinConfiguration.ConfVars;
import com.nflabs.zeppelin.driver.TableInfo;
import com.nflabs.zeppelin.driver.ZeppelinConnection;
import com.nflabs.zeppelin.driver.ZeppelinDriverException;
import com.nflabs.zeppelin.zengine.Z;
import com.nflabs.zeppelin.zengine.ZException;

public class HiveZeppelinConnectionTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testLocation() throws ZException, ZeppelinDriverException {
		Z.configure();
		HiveZeppelinDriver driver = new HiveZeppelinDriver(Z.conf());
		ZeppelinConnection conn = driver.getConnection();
		TableInfo info = conn.getTableInfo("nonexisttable");
		assertFalse(info.isExists());
		assertTrue(info.getLocation().endsWith("warehouse/nonexisttable"));
		conn.close();
		driver.shutdown();
	}

}
