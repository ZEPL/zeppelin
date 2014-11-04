/**
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 * 
 */
package com.nflabs.zeppelin.dfs;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FsShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nflabs.zeppelin.interpreter.Interpreter;
import com.nflabs.zeppelin.interpreter.InterpreterResult;
import com.nflabs.zeppelin.interpreter.InterpreterResult.Code;
import com.nflabs.zeppelin.scheduler.Scheduler;
import com.nflabs.zeppelin.scheduler.SchedulerFactory;

/**
 * Hadoop Shell interpreter
 * 
 * @author anthonycorbacho
 *
 */
public class HadoopDfs extends Interpreter {

  private static final Logger LOG = LoggerFactory.getLogger(HadoopDfs.class);

  static {
    Interpreter.register("dfs", HadoopDfs.class.getName());
  }

  public HadoopDfs(Properties property) {
    super(property);

  }

  @Override
  public InterpreterResult interpret(String commandLine) {
    LOG.info("run dfs command: {}", commandLine);
    
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(out);
    System.setOut(printStream);
    List<String> argsList = createListOfArgumentsFrom(commandLine);
    
    try {
      execute(argsList, out);
    } catch (Exception e) {
      e.printStackTrace();
      return new InterpreterResult(Code.ERROR, e.getMessage());
    }
    return new InterpreterResult(Code.SUCCESS, out.toString());
  }
  
  private void execute(List<String> argsList, ByteArrayOutputStream out) throws Exception {
    FsShell fsShell = new FsShell(new Configuration());
    fsShell.run(argsList.toArray(new String[] {}));
    out.flush();
    out.close();
  }

  private List<String> createListOfArgumentsFrom(String commandLine) {
    StringTokenizer st = new StringTokenizer(commandLine, " ");
    List<String> argsList = new ArrayList<String>();
    while (st.hasMoreTokens()) {
      argsList.add(st.nextToken());
    }
    if (!argsList.get(0).startsWith("-")) {
      argsList.set(0, "-" + argsList.get(0));
    }
    return argsList;
  }
  
  @Override
  public void open() {}

  @Override
  public void close() {}

  @Override
  public Object getValue(String name) {
    return null;
  }
  
  @Override
  public void cancel() {}

  @Override
  public void bindValue(String name, Object o) {}

  @Override
  public FormType getFormType() {
    return FormType.SIMPLE;
  }
  @Override
  public Scheduler getScheduler() {
      return SchedulerFactory.singleton().createOrGetParallelScheduler(HadoopDfs.class.getName()+this.hashCode(), 5);
  }

  @Override
  public int getProgress() {
    return 0;
  }

  @Override
  public List<String> completion(String buf, int cursor) {
    return null;
  }

}
