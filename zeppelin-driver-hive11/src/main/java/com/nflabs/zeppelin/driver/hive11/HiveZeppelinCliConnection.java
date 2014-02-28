package com.nflabs.zeppelin.driver.hive11;

import java.io.PrintStream;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hive.cli.CliDriver;
import org.apache.hadoop.hive.cli.CliSessionState;
import org.apache.hadoop.hive.cli.OptionsProcessor;
import org.apache.hadoop.hive.common.io.CachingPrintStream;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.jdbc.HiveStatement;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.ql.exec.Utilities;
import org.apache.hadoop.hive.ql.processors.CommandProcessor;
import org.apache.hadoop.hive.ql.processors.CommandProcessorFactory;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.hadoop.hive.service.HiveClient;
import org.apache.hadoop.hive.service.HiveInterface;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import com.nflabs.zeppelin.driver.ZeppelinConnection;
import com.nflabs.zeppelin.driver.ZeppelinDriverException;
import com.nflabs.zeppelin.driver.hive11.HiveServer.HiveServerHandler;
import com.nflabs.zeppelin.result.Result;
import com.nflabs.zeppelin.result.ResultDataException;

public class HiveZeppelinCliConnection implements ZeppelinConnection {
	Logger logger = Logger.getLogger(HiveZeppelinCliConnection.class);
	public static final int LINES_TO_FETCH = 40;
	
	private CliSessionState ss;
	private HiveConf hiveConf;
	private PrintStream out;
	private PrintStream err;
	private PrintStream info;

	public HiveZeppelinCliConnection(HiveConf hiveConf) throws Exception{
		this.hiveConf = hiveConf;
		String [] args = new String[]{};
		init(hiveConf, args);
	}

	public HiveZeppelinCliConnection(HiveConf hiveConf, String host, int port) throws Exception{
		this.hiveConf = hiveConf;
		String [] args = new String[]{"-h", host, "-p", String.valueOf(port)};
		init(hiveConf, args);
	}

	private void init(HiveConf hiveConf, String [] args) throws Exception{
		
		OptionsProcessor oproc = new OptionsProcessor();
		if(!oproc.process_stage1(args)){
			throw new ZeppelinDriverException("Can't process option stage1");
		}
		
		ss = new CliSessionState(hiveConf);
		
		if(!oproc.process_stage2(ss)){
			throw new ZeppelinDriverException("Can't process option stage2");
		}

		out = new PrintStream(System.out, true, "UTF-8");
		err = new CachingPrintStream(System.err, true, "UTF-8");
		info = new CachingPrintStream(System.err, true, "UTF-8");
		ss.out = out;
	    ss.info = info;
	    ss.err = err;

		SessionState.start(ss);

		if (ss.getHost()!=null ){
			ss.connect();
			if (ss.isRemoteMode()) {
				logger.info("Connecting "+ss.getHost()+":"+ss.getPort());
			}
		}
		
		if (!ss.isRemoteMode()) {
		      ClassLoader loader = hiveConf.getClassLoader();
		      String auxJars = HiveConf.getVar(hiveConf, HiveConf.ConfVars.HIVEAUXJARS);
		      if (StringUtils.isNotBlank(auxJars)) {
		        loader = Utilities.addToClassPath(loader, StringUtils.split(auxJars, ","));
		      }
		      hiveConf.setClassLoader(loader);
		      Thread.currentThread().setContextClassLoader(loader);			
		}
		
		CliDriver cli = new CliDriver();
	    cli.setHiveVariables(oproc.getHiveVariables());

	    // use the specified database if specified
	    cli.processSelectDatabase(ss);
	}
	
	@Override
	public boolean isConnected() throws ZeppelinDriverException {
		return false;
	}

	@Override
	public void close() throws ZeppelinDriverException {
		ss.close();		
	}

	@Override
	public void abort() throws ZeppelinDriverException {
	}

	private Result execute(String query){
		HiveInterface client;
		HiveStatement stmt;
		
		if (ss.isRemoteMode()) {
			client = ss.getClient();
			stmt = new HiveStatement(client);
		} else {
			try {
				client = new HiveServer.HiveServerHandler(hiveConf, out, err);
				stmt = new HiveStatement(client);
			} catch (MetaException e) {
				throw new ZeppelinDriverException(e);
			}
		}
	
	    ResultSet res = null;
	    
	    try {
	    	res = stmt.executeQuery(query);
	    	Result r = new Result(res);
	    	r.load();
	    	return r;
		} catch (SQLException e) {
			if (e.getMessage().startsWith("The query did not generate a result set")) {
				try {
					return new Result(0, new String[]{});
				} catch (ResultDataException e1) {
					throw new ZeppelinDriverException(e1);
				}
			} else {
				throw new ZeppelinDriverException(e);
			}
		} catch (ResultDataException e) {
			throw new ZeppelinDriverException(e);
		} finally {
			if ( res != null ) {
				try {
					res.close();
					client.clean();
				} catch (SQLException e) {
					throw new ZeppelinDriverException(e);
				} catch (TException e) {
					throw new ZeppelinDriverException(e);
				}
			}
		}	
	}
	
	@Override
	public Result query(String query) throws ZeppelinDriverException {
		return execute(query);
	}

	@Override
	public Result addResource(URI resourceLocation)
			throws ZeppelinDriverException {
		if(resourceLocation.getPath().endsWith(".jar")){
			return execute("ADD JAR "+resourceLocation.toString());			
		} else {
			return execute("ADD FILE "+resourceLocation.toString());			
		}
	}

	@Override
	public Result select(String tableName, int limit)
			throws ZeppelinDriverException {
		if (limit >=0 ){
			return execute("SELECT * FROM "+tableName+" LIMIT "+limit);
		} else {
			return execute("SELECT * FROM "+tableName);
		}
	}

	@Override
	public Result createViewFromQuery(String viewName, String query)
			throws ZeppelinDriverException {
		return execute("CREATE VIEW "+viewName+" AS "+query);
	}

	@Override
	public Result createTableFromQuery(String tableName, String query)
			throws ZeppelinDriverException {
		return execute("CREATE TABLE "+tableName+" AS "+query);
	}

	@Override
	public Result dropView(String viewName) throws ZeppelinDriverException {
		return execute("DROP VIEW "+viewName);		
	}

	@Override
	public Result dropTable(String tableName) throws ZeppelinDriverException {
		return execute("DROP TABLE "+tableName);		
	}

}
