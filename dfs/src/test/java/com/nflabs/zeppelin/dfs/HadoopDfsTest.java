package java.com.nflabs.zeppelin.dfs;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.nflabs.zeppelin.dfs.HadoopDfs;
import com.nflabs.zeppelin.interpreter.InterpreterResult;

public class HadoopDfsTest {
  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  @Test
  public void test_Hadoop_Dfs_with_ls_agrument() {
    HadoopDfs dfs = new HadoopDfs(new Properties());
    dfs.open();
    InterpreterResult result = dfs.interpret("ls ./");
    assertNotNull(result.message());
  }
}
