package com.nflabs.zeppelin.driver.hive11;

import java.io.File;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Pattern;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.hadoop.hive.service.HiveInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nflabs.zeppelin.conf.ZeppelinConfiguration;
import com.nflabs.zeppelin.conf.ZeppelinConfiguration.ConfVars;
import com.nflabs.zeppelin.driver.ZeppelinConnection;
import com.nflabs.zeppelin.driver.ZeppelinDriverException;
import com.nflabs.zeppelin.driver.ZeppelinDriver;

public class HiveZeppelinDriver extends ZeppelinDriver {
	Logger logger = LoggerFactory.getLogger(HiveZeppelinDriver.class);
    private static String HIVE_SERVER = "org.apache.hadoop.hive.jdbc.HiveDriver";
    private static String HIVE_SERVER_2 = "org.apache.hive.jdbc.HiveDriver";

	private HiveInterface client;	
	
	public void setClient(HiveInterface client){
		this.client = client;
	}

	private String getLocalMetastore(){
		return new File(getConf().getString(ConfVars.ZEPPELIN_HOME)+"/metastore_db").getAbsolutePath();
	}
	
	private String getLocalWarehouse(){
		return new File(getConf().getString(ConfVars.ZEPPELIN_HOME)+"/warehouse").getAbsolutePath();
	}
	
	@Override
	public ZeppelinConnection createConnection(String uri) throws ZeppelinDriverException {
		try {
			Connection con = null;
			logger.info("Create connection "+uri);

			if (client!=null){
				// create connection with given client instance. mainly for unit test
				logger.debug("Create connection from provided client instance");
				con = new HiveConnection(client);
			} else if(isEmpty(uri) || uri.equals("hive0.11://")){
				// local mode. Using hive client 
				return new HiveZeppelinCliConnection(localHiveConf());
			} else if(uri.startsWith("hive0.11://")) {
				// remote mode. Using hive client
				URI u = new URI(uri);				
				return new HiveZeppelinCliConnection(hiveConf(), u.getHost(), u.getPort());
			} else if(uri.equals("hive://") || uri.equals("hive2://")) { 
				// local mode using jdbc driver
				logger.debug("Create connection from local mode");
				con = new HiveConnection(localHiveConf());
			} else if(uri.startsWith("hive://") || uri.startsWith("hive2://")){
				// remote connection using jdbc uri
				logger.debug("Create connection from given jdbc uri");
			    con = DriverManager.getConnection("jdbc:"+uri);
			} else {
				throw new ZeppelinDriverException("Can't create connection");
			}

			return new HiveZeppelinConnection(getConf(), con);
		} catch (SQLException e) {
			throw new ZeppelinDriverException(e);
		} catch (Exception e) {
			throw new ZeppelinDriverException(e);
		}
	}

	
	private boolean isEmpty(String string) {
        return string==null || string.trim().length()==0;
    }

    private HiveConf hiveConf(){
		HiveConf hiveConf = null;
		hiveConf = new HiveConf(SessionState.class);
		return hiveConf;		
	}
	
	private HiveConf localHiveConf(){
		HiveConf hiveConf = null;
		hiveConf = new HiveConf(SessionState.class);
		logger.info("Local Hive Conf. warehouse="+getLocalWarehouse()+", metastore="+getLocalMetastore());
		// set some default configuration if no hive-site.xml provided
		hiveConf.set("javax.jdo.option.ConnectionURL", "jdbc:derby:;databaseName="+getLocalMetastore()+";create=true");
		hiveConf.set(HiveConf.ConfVars.METASTOREWAREHOUSE.varname, getLocalWarehouse());
		new File(getLocalWarehouse()).mkdirs();
		hiveConf.set(HiveConf.ConfVars.HADOOPJT.varname, "local");
		return hiveConf;
	}	

	@Override
	public boolean acceptsURL(String url) {
		return ( Pattern.matches("hive://.*", url) || Pattern.matches("hive2://.*", url) || Pattern.matches("hive0.11://.*", url));
	}

	@Override
	protected void init() {
		try {
			// loading hive driver class in proper order.
			Class.forName(HIVE_SERVER);
			Class.forName(HIVE_SERVER_2);
		} catch (ClassNotFoundException e) {
			throw new ZeppelinDriverException(e);
		}
	}
}
