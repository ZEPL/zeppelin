package org.apache.tajo.zeppelin;

import com.nflabs.zeppelin.interpreter.InterpreterResult;
import com.nflabs.zeppelin.interpreter.InterpreterResult.Code;
import org.apache.tajo.TajoConstants;
import org.apache.tajo.catalog.TableDesc;
import org.apache.tajo.catalog.statistics.TableStats;
import org.apache.tajo.cli.tsql.DefaultTajoCliOutputFormatter;
import org.apache.tajo.cli.tsql.TajoCli;
import org.apache.tajo.util.FileUtil;

import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class TajoCliZeppelinOutputFormatter extends DefaultTajoCliOutputFormatter {
  private int maxResult;

  @Override
  public void init(TajoCli.TajoCliContext context) {
    super.init(context);
    maxResult = context.getInt("zeppelin.spark.maxResult", TajoSqlInterpreter.ZEPPELIN_SPARK_MAX_RESULT);
  }

  @Override
  public void printResult(PrintWriter sout, InputStream sin, TableDesc tableDesc, float responseTime, ResultSet res)
      throws Exception {
    if (res == null) {
      sout.println("<font color=red>" + getQuerySuccessMessage(tableDesc, responseTime, 0, "inserted", true) + "</font>");
      sout.flush();
      return;
    }

    sout.print("%table ");

    ResultSetMetaData rsmd = res.getMetaData();
    int numColumns = rsmd.getColumnCount();
    for(int i = 0; i < numColumns; i++) {
      if(i == 0) {
        sout.print(rsmd.getColumnName(i + 1));
      } else {
        sout.print("\t" + rsmd.getColumnName(i + 1));
      }
    }
    sout.println();

    int numRows = 0;
    while (res.next()) {
      for (int i = 0; i < numColumns; i++) {
        if (i > 0) {
          sout.print("\t");
        }
        sout.print(res.getString(i + 1));
      }
      sout.print("\n");
      numRows++;
      if (numRows > maxResult) {
        sout.print("\n<font color=red>Results are limited by " + maxResult + ".</font>");
        break;
      }
    }
    sout.flush();
  }

  private String getQuerySuccessMessage(TableDesc tableDesc, float responseTime, int totalPrintedRows, String postfix,
                                        boolean endOfTuple) {
    TableStats stat = tableDesc.getStats();
    String volume = stat == null ? (endOfTuple ? "0 B" : "unknown bytes") :
        FileUtil.humanReadableByteCount(stat.getNumBytes(), false);
    long resultRows = stat == null ? TajoConstants.UNKNOWN_ROW_NUMBER : stat.getNumRows();

    String displayRowNum;
    if (resultRows == TajoConstants.UNKNOWN_ROW_NUMBER) {

      if (endOfTuple) {
        displayRowNum = totalPrintedRows + " rows";
      } else {
        displayRowNum = "unknown row number";
      }

    } else {
      displayRowNum = resultRows + " rows";
    }
    return "(" + displayRowNum + ", " + getResponseTimeReadable(responseTime) + ", " + volume + " " + postfix + ")";
  }
}
