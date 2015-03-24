package com.nflabs.zeppelin.mysql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nflabs.zeppelin.interpreter.Interpreter;
import com.nflabs.zeppelin.interpreter.InterpreterContext;
import com.nflabs.zeppelin.interpreter.InterpreterResult;
import com.nflabs.zeppelin.interpreter.InterpreterResult.Code;
import com.nflabs.zeppelin.scheduler.Scheduler;
import com.nflabs.zeppelin.scheduler.SchedulerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * Mysql interpreter for Zeppelin.
 *
 * @author Hyungu Roh hyungu.roh@navercorp.com
 *
 */
public class MysqlInterpreter extends Interpreter {
  Logger logger = LoggerFactory.getLogger(MysqlInterpreter.class);
  int commandTimeOut = 600000;

  static {
    Interpreter.register("mysql", MysqlInterpreter.class.getName());
  }

  public MysqlInterpreter(Properties property) {
    super(property);
  }

  @Override
  public void open() {}

  @Override
  public void close() {}

  @Override
  public InterpreterResult interpret(String cmd, InterpreterContext contextInterpreter) {
    logger.info("Run mysql command '" + cmd + "'");
    long start = System.currentTimeMillis();

    String host = "";
    String port = "";
    String user = "";
    String password = "";

    // get Properties
    Properties intpProperty = getProperty();
    for (Object k : intpProperty.keySet()) {
      String key = (String) k;
      String value = (String) intpProperty.get(key);

      if ( key.equals("--host") || key.equals("-h") ) {
        host = value;
      } else if ( key.equals("--password") || key.equals("-p") ) {
        password = value;
      } else if ( key.equals("--user") || key.equals("-u") ) {
        user = value;
      } else if ( key.equals("--port") || key.equals("-P") ) {
        port = value;
      } else {
        logger.info("else key : /" +  key + "/");
      }
    }

    String driver = "com.mysql.jdbc.Driver";
    String url = "jdbc:mysql://";
    url += host;
    url += port != "" ? ":" + port : "";
    url += "/?user=" + user;

    // for jdbc connection and result
    Connection conn;
    Statement stmt;
    ResultSet rs;

    logger.info("Connect to " + url);

    try {
      // connect to mysql
      Class.forName(driver);
      conn = DriverManager.getConnection(url, user, password);
      stmt = conn.createStatement();
      rs = stmt.executeQuery(cmd);

      // make result format ( for zeppelin table style )
      String queryResult = "";
      Vector<String> columnNames = new Vector<String>();

      if (rs != null) {
        ResultSetMetaData columns = rs.getMetaData();
        for ( int i = 1; i <= columns.getColumnCount(); ++i ) {
          if ( i != 1 ) {
            queryResult += "\t";
          }
          queryResult += columns.getColumnName(i);
          columnNames.add(columns.getColumnName(i));
        }
        queryResult += "\n";

        logger.info(columnNames.toString());

        while ( rs.next() ) {
          for ( int i = 0; i < columnNames.size(); ++i) {
            if ( i != 0 ) {
              queryResult += "\t";
            }
            queryResult += rs.getString(columnNames.get(i));
          }
          queryResult += "\n";
        }
      }

      String msg = "%table ";
      msg += queryResult;

      // disconnect
      stmt.close();
      conn.close();

      return new InterpreterResult(InterpreterResult.Code.SUCCESS, msg);
    } catch ( ClassNotFoundException | SQLException e ) {
      logger.error("Can not run " + cmd, e);
      return new InterpreterResult(Code.ERROR, e.getMessage());
    }
  }

  @Override
  public void cancel(InterpreterContext context) {}

  @Override
  public FormType getFormType() {
    return FormType.SIMPLE;
  }

  @Override
  public int getProgress(InterpreterContext context) {
    return 0;
  }

  @Override
  public Scheduler getScheduler() {
    return SchedulerFactory.singleton().createOrGetFIFOScheduler(
        MysqlInterpreter.class.getName() + this.hashCode());
  }

  @Override
  public List<String> completion(String buf, int cursor) {
    return null;
  }

}
