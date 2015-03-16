package com.nflabs.zeppelin.tajo;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.tajo.conf.TajoConf;
import org.apache.tajo.thrift.ThriftServerConstants;
import org.apache.tajo.thrift.client.TajoThriftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ServiceException;
import com.nflabs.zeppelin.interpreter.Interpreter;
import com.nflabs.zeppelin.interpreter.InterpreterContext;
import com.nflabs.zeppelin.interpreter.InterpreterException;
import com.nflabs.zeppelin.interpreter.InterpreterPropertyBuilder;
import com.nflabs.zeppelin.interpreter.InterpreterResult;
import com.nflabs.zeppelin.interpreter.InterpreterResult.Code;

/**
 * Tajo interpreter implementation. with thrift protocol support
 * https://github.com/jerryjung/tajo/tree/thriftserver
 */
public class TajoInterpreter extends Interpreter {
  static {
    Interpreter.register(
        "tsql",
        "tajo",
        TajoInterpreter.class.getName(),
        new InterpreterPropertyBuilder()
          .add("tajo.thrift.server",
               "localhost:" + ThriftServerConstants.DEFAULT_LISTEN_PORT,
               "Tajo thrift server address, host:port")
          .add("tajo.maxResult",
              "1000",
               "Maximum number of result to retreive")
          .build());
  }

  Logger logger = LoggerFactory.getLogger(TajoInterpreter.class);
  private TajoThriftClient tajoClient;


  public TajoInterpreter(Properties property) {
    super(property);
  }

  @Override
  public void open() {
    TajoConf conf = new TajoConf();
    try {
      tajoClient = new TajoThriftClient(conf, getProperty("tajo.thrift.server"), null);
    } catch (IOException e) {
      throw new InterpreterException(e);
    }
  }

  @Override
  public void close() {
    tajoClient.close();
  }

  private int getMaxResult() {
    return Integer.parseInt(getProperty("tajo.maxResult"));
  }

  @Override
  public InterpreterResult interpret(String st, InterpreterContext context) {
    ResultSet result;
    try {
      result = tajoClient.executeQueryAndGetResult(st);
      // empty result
      if (result == null) {
        return new InterpreterResult(Code.SUCCESS, "");
      }

      String m = "";

      // extract column info
      ResultSetMetaData md = result.getMetaData();
      int numColumns = md.getColumnCount();
      for (int i = 1; i <= numColumns; i++) {
        if (i != 1) {
          m += "\t";
        }
        m += md.getColumnName(i);
      }
      m += "\n";

      int maxResult = getMaxResult();
      int currentRow = 0;
      String extraMessage = "";

      while (result.next()) {

        if (currentRow == maxResult) {
          extraMessage = "\n<font color=red>Results are limited by " + maxResult + ".</font>";
          break;
        }
        currentRow++;

        for (int i = 1; i <= numColumns; i++) {
          if (i != 1) {
            m += "\t";
          }

          Object col = result.getObject(i);
          if (col == null) {
            m += "\t";
          } else {
            m += col.toString();
          }
        }

        m += "\n";
      }

      return new InterpreterResult(Code.SUCCESS, "%table " + m + extraMessage);

    } catch (ServiceException | IOException | SQLException e) {
      logger.error("Error", e);
      return new InterpreterResult(Code.ERROR, e.getMessage());
    }


  }

  @Override
  public void cancel(InterpreterContext context) {
  }

  @Override
  public FormType getFormType() {
    return FormType.SIMPLE;
  }

  @Override
  public int getProgress(InterpreterContext context) {
    return 0;
  }

  @Override
  public List<String> completion(String buf, int cursor) {
    return new LinkedList<String>();
  }

}
