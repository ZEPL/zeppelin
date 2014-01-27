package com.nflabs.zeppelin.driver.elasticsearch;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.util.FileUtils;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.nflabs.zeppelin.conf.ZeppelinConfiguration;

public class ElasticsearchDriverTest {
	
	private static Node node;
	private static Client client;
	
	@BeforeClass
	public static void setUpClass(){
		FileUtils.delete(new File("./data"));
		node = NodeBuilder.nodeBuilder().local(true).node();
		client = node.client();
	}
	
	@AfterClass
	public static void tearDownClass(){
		client.admin().indices().prepareDelete().execute().actionGet();
		client.close();
		node.stop();
		node.close();		
	}

	private ElasticsearchDriver driver;
	
	@Before
	public void setUp() throws Exception {
		driver = new ElasticsearchDriver(ZeppelinConfiguration.create(), new URI("es://localhost:9200"), Thread.currentThread().getContextClassLoader());
	}

	@After
	public void tearDown() throws Exception {
		driver.close();
	}

	@Test
	public void testBasicQuery() throws InterruptedException {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("name", "apple");
		data.put("age", 10);
		client.prepareIndex("testIndex", "testType").setSource(data).execute().actionGet();

		driver.query("testIndex.testType {}");
	}
	


}
