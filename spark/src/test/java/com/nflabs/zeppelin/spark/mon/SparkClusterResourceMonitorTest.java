package com.nflabs.zeppelin.spark.mon;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.nflabs.zeppelin.spark.SparkInterpreter;

public class SparkClusterResourceMonitorTest {

  private SparkInterpreter repl;
  private SparkContext sc;

  @Before
  public void setUp() throws Exception {
    SparkConf conf =
        new SparkConf()
            .setMaster("local[*]")
            .setAppName("test");

    conf.set("spark.scheduler.mode", "FAIR");

    sc = new SparkContext(conf);
  }

  @After
  public void tearDown() {
    sc.stop();
  }


  @Test
  public void testCpuUsage() {
    SparkClusterResourceMonitor scrm = new SparkClusterResourceMonitor(sc);
    Map<String, Integer> usage = scrm.cpuUsage();
    assertEquals(1, usage.size());
  }

}
