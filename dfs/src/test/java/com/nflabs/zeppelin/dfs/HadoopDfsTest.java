package com.nflabs.zeppelin.dfs;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Test;

import com.nflabs.zeppelin.interpreter.InterpreterResult;
import com.nflabs.zeppelin.interpreter.InterpreterResult.Code;

public class HadoopDfsTest {
  @Test
  public void test_Hadoop_Dfs_with_ls_agrument() {
    HadoopDfs dfs = new HadoopDfs(new Properties());
    dfs.open();
    InterpreterResult result = dfs.interpret("ls ./");
    assertNotNull(result.message());
    assertEquals(result.code(), Code.SUCCESS);
    System.out.println(result.message());
  }
  
  @Test
  public void test_Hadoop_Dfs_with_invalid_agrument() {
    HadoopDfs dfs = new HadoopDfs(new Properties());
    dfs.open();
    InterpreterResult result = dfs.interpret("asdasads");
    assertEquals(result.code(), Code.ERROR);
  }

}
