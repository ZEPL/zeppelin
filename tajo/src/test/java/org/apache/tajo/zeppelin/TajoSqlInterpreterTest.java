package org.apache.tajo.zeppelin;

import com.nflabs.zeppelin.interpreter.InterpreterResult;
import com.nflabs.zeppelin.interpreter.InterpreterResult.Type;
import org.apache.hadoop.fs.Path;
import org.apache.tajo.*;
import org.apache.tajo.client.TajoClient;
import org.apache.tajo.client.TajoClientImpl;
import org.apache.tajo.conf.TajoConf;
import org.apache.tajo.storage.StorageUtil;
import org.apache.tajo.util.CommonTestingUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TajoSqlInterpreterTest {
  private static TajoTestingCluster cluster;
  private static TajoConf conf;
  private static TajoSqlInterpreter tajo;
  private static Path testDir;

  @BeforeClass
  public static void setUp() throws Exception {
    cluster = new TajoTestingCluster();
    cluster.startMiniCluster(1);
    conf = cluster.getConfiguration();

    Properties props = new Properties();
    Iterator<Map.Entry<String, String>> it = conf.iterator();
    while (it.hasNext()) {
      Map.Entry<String, String> entry = it.next();
      props.put(entry.getKey(), entry.getValue());
    }
    tajo = new TajoSqlInterpreter(props);

    testDir = CommonTestingUtil.getTestDir();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    if (tajo != null) {
      tajo.close();
    }
    if (cluster != null) {
      cluster.shutdownMiniCluster();
    }
  }

  @Test
  public void testBasicOperation() throws Exception {
    String tableName = "testsimplequery";
    Path tablePath = writeTmpTable(tableName);
    TajoClient client = new TajoClientImpl(conf);
    String sql =
        "create external table " + tableName + " (deptname text, score int4) "
            + "using csv location '" + tablePath + "'";
    client.executeQueryAndGetResult(sql);
    assertTrue(client.existTable(tableName));

    InterpreterResult result = tajo.interpret("select * from testsimplequery limit 20");

    assertEquals(InterpreterResult.Code.SUCCESS, result.code());
    assertEquals(Type.TABLE, result.type());

    String expected = "deptname\tscore\n" +
        "test0\t1\n" +
        "test1\t2\n" +
        "test2\t3\n" +
        "test3\t4\n" +
        "test4\t5\n" +
        "test5\t6\n" +
        "test6\t7\n" +
        "test7\t8\n" +
        "test8\t9\n" +
        "test9\t10\n" +
        "test10\t11\n" +
        "test11\t12\n" +
        "test12\t13\n" +
        "test13\t14\n" +
        "test14\t15\n" +
        "test15\t16\n" +
        "test16\t17\n" +
        "test17\t18\n" +
        "test18\t19\n" +
        "test19\t20\n";

    assertEquals(expected, result.message());

    // Test listing tables.
    result = tajo.interpret("\\d");
    assertEquals(InterpreterResult.Code.SUCCESS, result.code());
    assertEquals(Type.TEXT, result.type());

    expected = "testsimplequery\n";
    assertEquals(expected, result.message());

    // Test describing the specified table.
    result = tajo.interpret("\\d testsimplequery");
    assertEquals(InterpreterResult.Code.SUCCESS, result.code());
    assertEquals(Type.TEXT, result.type());

    // Result of describing table contains data file path which is different every test.
    // So This test suite compares with result message except data path.
    expected = "store type: CSV\n" +
        "number of rows: unknown\n" +
        "volume: 982 B\n" +
        "Options: \n" +
        "\t'text.delimiter'='|'\n" +
        "\n" +
        "schema: \n" +
        "deptname\tTEXT\n" +
        "score\tINT4";

    assertTrue(result.message().indexOf(expected) >= 0);
  }

  private static Path writeTmpTable(String tableName) throws IOException {
    Path tablePath = StorageUtil.concatPath(testDir, tableName);
    BackendTestingUtil.writeTmpTable(conf, tablePath);
    return tablePath;
  }
}
