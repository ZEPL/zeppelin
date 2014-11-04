package com.nflabs.zeppelin.dfs;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.nflabs.zeppelin.interpreter.InterpreterResult;

public class DfsInterpreterTest {
  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  @Test
  public void test_Hadoop_Dfs_with_ls_agrument() {
    DfsInterpreter dfs = new DfsInterpreter(new Properties());
    dfs.open();
    InterpreterResult result = dfs.interpret("ls ./");
    assertNotNull(result.message());
  }
}
