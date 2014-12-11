package org.apache.tajo.zeppelin;

import com.nflabs.zeppelin.conf.ZeppelinConfiguration;
import com.nflabs.zeppelin.interpreter.Interpreter;
import com.nflabs.zeppelin.interpreter.InterpreterResult;
import com.nflabs.zeppelin.interpreter.InterpreterResult.Code;
import com.nflabs.zeppelin.scheduler.Scheduler;
import com.nflabs.zeppelin.scheduler.SchedulerFactory;
import org.apache.tajo.SessionVars;
import org.apache.tajo.cli.tsql.TajoCli;
import org.apache.tajo.client.TajoClient;
import org.apache.tajo.client.TajoClientImpl;
import org.apache.tajo.conf.TajoConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

public class TajoSqlInterpreter extends Interpreter {
  public static final int ZEPPELIN_SPARK_MAX_RESULT = 1000;
  Logger logger = LoggerFactory.getLogger(TajoSqlInterpreter.class);

  static {
    Interpreter.register("tsql", TajoSqlInterpreter.class.getName());
  }

  private TajoCli tajoCli;
  private int progress;
  private TajoConf tajoConf;

  private ByteArrayInputStream cliIn;
  private ByteArrayOutputStream cliOut;

  public TajoSqlInterpreter(Properties property) {
    super(property);
    tajoConf = new TajoConf();

    if (property != null) {
      for (Entry<Object, Object> entry: property.entrySet()) {
        tajoConf.set(entry.getKey().toString(), entry.getValue().toString());
      }
    }

    try {
      cliIn = new ByteArrayInputStream(new byte[]{});
      cliOut = new ByteArrayOutputStream();

      tajoCli = new TajoCli(tajoConf, new String[]{}, cliIn, cliOut);
      tajoCli.executeMetaCommand("\\set " + SessionVars.CLI_FORMATTER_CLASS +" " +
          TajoCliZeppelinOutputFormatter.class.getName());
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  @Override
  public void open() {
    ZeppelinConfiguration conf = ZeppelinConfiguration.create();
    int maxResult = conf.getInt("ZEPPELIN_SPARK_MAX_RESULT", "zeppelin.spark.maxResult", ZEPPELIN_SPARK_MAX_RESULT);
    tajoCli.getContext().setInt("zeppelin.spark.maxResult", maxResult);
  }

  @Override
  public void close() {
    tajoCli.close();
  }

  @Override
  public Object getValue(String name) {
    return null;
  }

  @Override
  public InterpreterResult interpret(String st) {
    progress = 0;
    try {
      tajoCli.executeScript(st);
      String consoleResult = new String(cliOut.toByteArray());
      cliOut.reset();
      return new InterpreterResult(Code.SUCCESS, consoleResult);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return new InterpreterResult(Code.ERROR, e.getMessage());
    } finally {
      progress = 100;
    }
  }

  @Override
  public void cancel() {

  }

  @Override
  public void bindValue(String name, Object o) {

  }

  @Override
  public FormType getFormType() {
    return FormType.SIMPLE;
  }

  @Override
  public Scheduler getScheduler() {
    return SchedulerFactory.singleton().createOrGetFIFOScheduler(TajoSqlInterpreter.class.getName()+this.hashCode());
  }

  @Override
  public int getProgress() {
    return progress;
  }

  @Override
  public List<String> completion(String buf, int cursor) {
    return null;
  }
}
