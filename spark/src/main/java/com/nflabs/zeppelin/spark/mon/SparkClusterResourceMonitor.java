package com.nflabs.zeppelin.spark.mon;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

/**
 *
 */
public class SparkClusterResourceMonitor {

  private JavaSparkContext sc;
  private int estimatedParallelism = 10;
  Map<String, Integer> lastCpuUsage = new HashMap<String, Integer>();

  public SparkClusterResourceMonitor(SparkContext sc) {
    this.sc = new JavaSparkContext(sc);
  }


  /**
   * Check cpu usage of all spark workers
   * @return
   */
  public Map<String, Integer> cpuUsage() {
    Map<String, Integer> currentCpuUsage = new HashMap<String, Integer>();

    List<Integer> data = new LinkedList<Integer>();
    for (int i = 0; i < estimatedParallelism; i++) {
      data.add(i);
    }

    sc.setLocalProperty("spark.scheduler.pool", "resourceMonitor");

    JavaRDD<Integer> rdd = sc.parallelize(data, estimatedParallelism);
    CPUMonitorFunction cpu = new CPUMonitorFunction(1000);
    List<Map<String, Integer>> usages = rdd.map(cpu).collect();


    int numCollectedInfo = 0;
    for (Map<String, Integer> hostCpu : usages) {
      numCollectedInfo += hostCpu.size();
      currentCpuUsage.putAll(hostCpu);
    }

    // adjust estimatedParallelism
    lastCpuUsage = currentCpuUsage;

    return currentCpuUsage;
  }
}
