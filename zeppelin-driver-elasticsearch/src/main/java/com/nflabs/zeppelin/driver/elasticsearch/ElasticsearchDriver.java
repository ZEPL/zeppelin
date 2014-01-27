package com.nflabs.zeppelin.driver.elasticsearch;

import java.net.URI;

import com.nflabs.zeppelin.conf.ZeppelinConfiguration;
import com.nflabs.zeppelin.driver.ZeppelinConnection;
import com.nflabs.zeppelin.driver.ZeppelinDriver;
import com.nflabs.zeppelin.driver.ZeppelinDriverException;

public class ElasticsearchDriver extends ZeppelinDriver {

	/**
	 * 
	 * @param conf
	 * @param uri es://clusteraddr:port/clusterName
	 * @param classLoader
	 */
	public ElasticsearchDriver(ZeppelinConfiguration conf, URI uri,
			ClassLoader classLoader) {
		super(conf, uri, classLoader);		
	}

	@Override
	protected ZeppelinConnection getConnection() throws ZeppelinDriverException {
		URI connectionUri = getUri();

		return new ElasticsearchConnection();
	}
}
