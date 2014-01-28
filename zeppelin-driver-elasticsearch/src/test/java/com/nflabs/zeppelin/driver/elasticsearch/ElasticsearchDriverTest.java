package com.nflabs.zeppelin.driver.elasticsearch;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
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
	public static void setUpClass() throws IOException{
		FileUtils.deleteDirectory(new File("./data"));
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
		Map<String, Object> data1 = new HashMap<String, Object>();
		data1.put("name", "apple");
		data1.put("age", 10);
		
		Map<String, Object> data2 = new HashMap<String, Object>();
		data2.put("name", "oragne");
		data2.put("age", 20);
		client.prepareIndex("index1", "type1").setSource(data1).setRefresh(true).execute().actionGet();
		client.prepareIndex("index1", "type1").setSource(data2).setRefresh(true).execute().actionGet();
		
		driver.query("POST /index1/type1/_search {\"query\":{\"query_string\":{\"query\":\"*\"}}}");
		
		
	}
	
	@Test
	public void testStatisticalFacet() throws InterruptedException {
		Map<String, Object> data1 = new HashMap<String, Object>();
		data1.put("name", "apple");
		data1.put("age", 10);
		
		Map<String, Object> data2 = new HashMap<String, Object>();
		data2.put("name", "oragne");
		data2.put("age", 20);
		client.prepareIndex("index1", "type1").setSource(data1).setRefresh(true).execute().actionGet();
		client.prepareIndex("index1", "type1").setSource(data2).setRefresh(true).execute().actionGet();
		
		driver.query("POST /index1/type1/_search {\"query\":{\"query_string\":{\"query\":\"*\"}},\"facets\":{\"stat1\":{\"statistical\":{\"field\":\"age\"}}}}");
		
		
	}

}
