package com.nflabs.zeppelin.driver.elasticsearch;

import java.net.URI;

import com.nflabs.zeppelin.driver.ZeppelinConnection;
import com.nflabs.zeppelin.driver.ZeppelinDriverException;
import com.nflabs.zeppelin.result.Result;

public class ElasticsearchConnection implements ZeppelinConnection {

	@Override
	public boolean isConnected() throws ZeppelinDriverException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close() throws ZeppelinDriverException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void abort() throws ZeppelinDriverException {
		// TODO Auto-generated method stub
		
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
