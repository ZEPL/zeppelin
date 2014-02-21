package com.nflabs.zeppelin.driver.hive11;

import java.io.PrintStream;
import java.net.URI;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hive.cli.CliDriver;
import org.apache.hadoop.hive.cli.CliSessionState;
import org.apache.hadoop.hive.cli.OptionsProcessor;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.exec.Utilities;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.hadoop.hive.service.HiveClient;
import org.apache.hadoop.hive.service.HiveServerException;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;

import com.nflabs.zeppelin.driver.ZeppelinConnection;
import com.nflabs.zeppelin.driver.ZeppelinDriverException;
import com.nflabs.zeppelin.result.Result;

public class HiveZeppelinCliConnection implements ZeppelinConnection {
	Logger logger = Logger.getLogger(HiveZeppelinCliConnection.class);
	public static final int LINES_TO_FETCH = 40;
	
	private CliSessionState ss;

	public HiveZeppelinCliConnection(HiveConf hiveConf) throws Exception{
		String [] args = new String[0];
		
		OptionsProcessor oproc = new OptionsProcessor();
		if(!oproc.process_stage1(args)){
			throw new ZeppelinDriverException("Can't process option stage1");
		}
		
		ss = new CliSessionState(hiveConf);
		
		if(!oproc.process_stage2(ss)){
			throw new ZeppelinDriverException("Can't process option stage2");
		}
		/*
		ss.out = new PrintStream(System.out, true, "UTF-8");
	    ss.info = new PrintStream(System.err, true, "UTF-8");
	    ss.err = new CachingPrintStream(System.err, true, "UTF-8");
	    */
		
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

	private Result executeQuery(String query){
		if (ss.isRemoteMode()) {
			HiveClient client = ss.getClient();
			client.
		    PrintStream out = ss.out;
		    PrintStream err = ss.err;
		    
		    try {
				client.execute(query);
				List<String> results;
				results = client.fetch
				for (String line : results) {
		            out.println(line);
		        }
			} catch (HiveServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else { // local mode
			
		}
	    
	}
	
	@Override
	public Result query(String query) throws ZeppelinDriverException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result addResource(URI resourceLocation)
			throws ZeppelinDriverException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result select(String tableName, int limit)
			throws ZeppelinDriverException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result createViewFromQuery(String viewName, String query)
			throws ZeppelinDriverException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result createTableFromQuery(String tableName, String query)
			throws ZeppelinDriverException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result dropView(String viewName) throws ZeppelinDriverException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result dropTable(String tableName) throws ZeppelinDriverException {
		// TODO Auto-generated method stub
		return null;
	}

}
