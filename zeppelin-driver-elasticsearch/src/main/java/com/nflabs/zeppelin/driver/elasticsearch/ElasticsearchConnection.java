package com.nflabs.zeppelin.driver.elasticsearch;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nflabs.zeppelin.driver.ZeppelinConnection;
import com.nflabs.zeppelin.driver.ZeppelinDriverException;
import com.nflabs.zeppelin.result.ColumnDef;
import com.nflabs.zeppelin.result.Result;

public class ElasticsearchConnection implements ZeppelinConnection {

	private URI connectionUri;

	public ElasticsearchConnection(URI connectionUri) {
		this.connectionUri = connectionUri;
	}

	@Override
	public boolean isConnected() throws ZeppelinDriverException {
		return true;
	}

	@Override
	public void close() throws ZeppelinDriverException {
	}

	@Override
	public void abort() throws ZeppelinDriverException {
	}

	@Override
	public Result query(String query) throws ZeppelinDriverException {
		Pattern pattern = Pattern.compile("([^ ]*)\\s([^ ]*)(\\s(.*))?");
		Matcher matcher = pattern.matcher(query);
		if (matcher.find()==false) {
			throw new ZeppelinDriverException("Syntax error");
		}
		String method = matcher.group(1);
		String path = matcher.group(2);
		String payload = null;
		if (matcher.groupCount()>=4) {
			payload = matcher.group(4);
		}
		
		if (path.startsWith("/")==false) {
			path = "/"+path;
		}
		
		
		CloseableHttpClient client = HttpClients.createDefault();
		String url = "http://"+connectionUri.getHost()+":"+connectionUri.getPort()+path;

		HttpUriRequest request = null;
		if ("GET".compareToIgnoreCase(method)==0) {
			HttpGet get = new HttpGet(url);
			request = get;
		} else if ("POST".compareToIgnoreCase(method)==0) {
			HttpPost post = new HttpPost(url);
			if (payload!=null) {
				try {
					post.setEntity(new StringEntity(payload));
				} catch (UnsupportedEncodingException e) {
					throw new ZeppelinDriverException(e);
				}
			}
			request = post;
		} else {
			throw new ZeppelinDriverException("Unsupported method "+method);
		}
		
		CloseableHttpResponse response;
		InputStream ins;
		try {
			response = client.execute(request);
			ins = response.getEntity().getContent();
		} catch (ClientProtocolException e) {
			throw new ZeppelinDriverException(e);
		} catch (IOException e) {
			throw new ZeppelinDriverException(e);
		}
		
		if (response.getStatusLine().getStatusCode()!=200) {
			throw new ZeppelinDriverException("Status "+response.getStatusLine()+" "+response.getStatusLine().getReasonPhrase());
		}
		
		Gson gson = new Gson();
		HashMap<String, Object> responseJson = gson.fromJson(new InputStreamReader(ins), new HashMap<String, Object>().getClass());
		Result result = new Result();
		result.setRaw(responseJson);
		dumpJson(responseJson);
		
		// { hists : "hits" : [ { "_source" : { OBJ } } ] }
		
		
		
		try {
			response.close();
		} catch (IOException e) {			
		}
		
		return result;
	}
	
	private void dumpJson(HashMap<String, Object> json){
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting();
		Gson gson = builder.create();
		System.out.println(gson.toJson(json));
	}

	@Override
	public Result addResource(URI resourceLocation)
			throws ZeppelinDriverException {
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
