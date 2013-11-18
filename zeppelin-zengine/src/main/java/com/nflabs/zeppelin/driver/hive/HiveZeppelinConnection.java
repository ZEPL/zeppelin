package com.nflabs.zeppelin.driver.hive;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.hadoop.hive.conf.HiveConf;

import com.nflabs.zeppelin.driver.TableInfo;
import com.nflabs.zeppelin.driver.ZeppelinConnection;
import com.nflabs.zeppelin.driver.ZeppelinDriverException;
import com.nflabs.zeppelin.result.Result;
import com.nflabs.zeppelin.result.ResultDataException;

public class HiveZeppelinConnection implements ZeppelinConnection {

	private Connection connection;
	
	public HiveZeppelinConnection(Connection connection) {
		this.connection = connection;
	}

	@Override
	public boolean isConnected() throws ZeppelinDriverException {
		try {
			if(connection.isClosed()){
				return false;
			} else {
				return true;
			}
		} catch (SQLException e) {
			throw new ZeppelinDriverException(e);
		}
	}

	@Override
	public void close() throws ZeppelinDriverException {
		try {
			connection.close();
		} catch (SQLException e) {
			throw new ZeppelinDriverException(e);
		}
	}
	
	private Result execute(String query) throws ZeppelinDriverException{

		try{
			ResultSet res = null;
			Statement stmt = connection.createStatement();
			res = stmt.executeQuery(query);
			Result r = new Result(res);
			r.load();
			stmt.close();
			return r;
		} catch (SQLException e) {
			throw new ZeppelinDriverException(e);
		} catch (ResultDataException e) {
			throw new ZeppelinDriverException(e);
		}

	}

	@Override
	public Result query(String query) throws ZeppelinDriverException {
		return execute(query);
	}

	@Override
	public Result addResource(URI resourceLocation) throws ZeppelinDriverException {
		if(resourceLocation.getPath().endsWith(".jar")){
			return execute("ADD JAR "+resourceLocation.toString());			
		} else {
			return execute("ADD FILE "+resourceLocation.toString());			
		}
	}

	@Override
	public Result createViewFromQuery(String viewName, String query) throws ZeppelinDriverException {
		return execute("CREATE VIEW "+viewName+" AS "+query);
	}

	@Override
	public Result createTableFromQuery(String tableName, String query) throws ZeppelinDriverException {
		return execute("CREATE TABLE "+tableName+" AS "+query);
	}

	@Override
	public Result dropView(String viewName) throws ZeppelinDriverException {
		return execute("DROP VIEW "+viewName);		
	}

	@Override
	public Result dropTable(String tableName) throws ZeppelinDriverException{
		return execute("DROP TABLE "+tableName);		
	}

	@Override
	public Result select(String tableName, int limit) throws ZeppelinDriverException {
		if (limit >=0 ){
			return execute("SELECT * FROM "+tableName+" LIMIT "+limit);
		} else {
			return execute("SELECT * FROM "+tableName);
		}
	}

	@Override
	public void abort() throws ZeppelinDriverException {
		throw new ZeppelinDriverException("Abort not supported");
	}

	@Override
	public TableInfo getTableInfo(String tableName)
			throws ZeppelinDriverException {
		if(tableName==null) return null;
		Result result = execute("DESCRIBE FORMATTED "+tableName);
		
		TableInfo tableInfo = resultToTableInfo(result, tableName);

		if(tableInfo.isExists()==false){
			synchronized(this){
				if(warehouseUri==null){
					execute("CREATE TABLE if not exists zp_driver_detect_warehouse(a INT)");
					result = execute("DESCRIBE FORMATTED zp_driver_detect_warehouse");
					tableInfo = resultToTableInfo(result, tableName);
					tableInfo.setExists(false);
					execute("DROP TABLE zp_driver_detect_warehouse");
					warehouseUri = tableInfo.getLocation().replace("/zp_driver_detect_warehouse", "");
				}				
				tableInfo.setLocation(warehouseUri+"/"+tableName);
			}
		}
		return tableInfo;
	}

	static String warehouseUri;
	
	private TableInfo resultToTableInfo(Result result, String tableName){
		TableInfo tableInfo = new TableInfo();
		int piece = -1;
		
		for (Object [] row : result.rows){
			String key = (String) row[0];
			String value = (String) row[1];
			if (key==null) continue;
			if (key.startsWith("Table "+tableName+" does not exist")){
				tableInfo.setExists(false);
				break;
			}
			if (key.startsWith("#")){
				piece++;
			}
			
			if (piece==0){ // # col_name
				tableInfo.setExists(true);
			} else if (piece==1){ // # Detailed Table Information
				if(key.startsWith("Databaase")){
					tableInfo.setDatabase(value); 
				} else if(key.startsWith("Owner")){
					tableInfo.setOwner(value);
				} else if(key.startsWith("Location")){
					tableInfo.setLocation(value);
				}
			} else if (piece==2){ // # Storage Information
				
			}
		}
		
		return tableInfo;
	}
}
