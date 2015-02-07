package com.nflabs.zeppelin.spark.mon;

import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.spark.api.java.function.Function;

/**
 *
 */
public class CPUMonitorFunction implements Function<Integer, Map<String, Integer>> {
  static long lastMonitor = 0;
  private long minIntervalMsec;
  static int smoothCpu = 0;

  public CPUMonitorFunction(long minIntervalMsec) {
    this.minIntervalMsec = minIntervalMsec;
  }

  @Override
  public Map<String, Integer> call(Integer arg0) throws Exception {
    HashMap<String, Integer> result = new HashMap<String, Integer>();

    if (lastMonitor != 0 && System.currentTimeMillis() - lastMonitor <= minIntervalMsec) {
      return result;
    } else {
      lastMonitor = System.currentTimeMillis();
    }

    // get cpu usage
    MBeanServer mbs = java.lang.management.ManagementFactory.getPlatformMBeanServer();
    ObjectName name = javax.management.ObjectName.getInstance("java.lang:type=OperatingSystem");
    AttributeList list = mbs.getAttributes(name, new String[]{"SystemCpuLoad"});  // ProcessCpuLoad


    int usage = 0;
    int numCpuCore = Runtime.getRuntime().availableProcessors();


    if (!list.isEmpty()) {
      Attribute att = (javax.management.Attribute) list.get(0);
      Double value = (Double) att.getValue();

      if (value >= 0) {
        usage = (int) (value * 100 / numCpuCore);
        String hostname = java.net.InetAddress.getLocalHost().getHostName();
        smoothCpu += (usage - smoothCpu) * 0.3;  // damping factor

        result.put(hostname, usage);
        lastMonitor = System.currentTimeMillis();
      }
    }
    return result;
  }
}
