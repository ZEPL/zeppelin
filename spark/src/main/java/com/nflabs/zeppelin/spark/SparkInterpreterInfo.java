package com.nflabs.zeppelin.spark;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import com.nflabs.zeppelin.interpreter.InterpreterInfo;
import com.nflabs.zeppelin.spark.mon.SparkClusterResourceMonitor;

/**
 *
 */
public class SparkInterpreterInfo implements InterpreterInfo {
  private SparkClusterResourceMonitor rm;

  public SparkInterpreterInfo(SparkClusterResourceMonitor rm) {
    this.rm = rm;
  }

  @Override
  public String get(String name) {
    if (rm == null) {
      return null;
    }

    if (name.equals("cpu")) {
      return monitorCPU();
    }

    return null;
  }

  private String monitorCPU() {
    Map<String, Integer> usages = rm.cpuUsage();
    StringBuilder output = new StringBuilder();
    output.append("<h3>Cluster CPU Usage</h3>");
    output.append("<table class=\"table\">");
    output.append("  <tr>");
    output.append("    <th>Host</th>");
    output.append("    <th>CPU usage</th>");
    output.append("  </tr>");
    for (String hostname : usages.keySet()) {
      output.append("  <tr>");
      output.append("    <td>" + hostname + "</td>");
      output.append("    <td>" + usages.get(hostname) + "</td>");
      output.append("  </tr>");
    }
    output.append("</table>");


    return output.toString();
  }


  @Override
  public Collection<String> list() {
    LinkedList<String> listOfInfo = new LinkedList<String>();
    if (rm != null) {
      listOfInfo.add("cpu");
    }
    return listOfInfo;
  }

}
