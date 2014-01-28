package com.nflabs.zeppelin.driver.elasticsearch;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.sql.Types;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		Pattern pattern = Pattern.compile("([^ ]*)\\s([^ ]*)\\s([^ ]*)\\s([^ ]*)(\\s(.*))?");
		Matcher matcher = pattern.matcher(query);
		if (matcher.find()==false) {
			throw new ZeppelinDriverException("Syntax error");
		}
		String method = matcher.group(1);
		String path = matcher.group(2);
		String listPath = matcher.group(3);
		String docBase = matcher.group(4);
		String payload = null;
		if (matcher.groupCount()>=6) {
			payload = matcher.group(6);
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
		dumpJson(responseJson);
		
		// { hists : "hits" : [ { "_source" : { OBJ } } ] }
		ESResult result = null;
		
		Object list = findObjectFromJson(responseJson, listPath);
		if (list instanceof List) {
			Iterator it = ((List)list).iterator();
			while (it.hasNext()) {
				Object row = findObjectFromJson((Map<String, Object>) it.next(), docBase);
				if (row instanceof Map) {
					Map<String, Object> columns = (Map<String, Object>) row;
					
					if (result == null ) {
						result = new ESResult(createColumnDef(columns));
					}
					
					ColumnDef[] columnDef = result.getColumnDef();
					Object[] rowData = new Object[columnDef.length];
					for (int i=0; i<columnDef.length; i++) {
						ColumnDef c = columnDef[i];
						rowData[i] = columns.get(c.getName());
					}
					result.addRow(rowData);
					dumpJson(columns);
				} else {
					throw new ZeppelinDriverException("Can not find object under "+docBase);
				}
			}			
		} else if (list instanceof Map) {
			Map<String, Object> columns = (Map<String, Object>) list;
			result = new ESResult(createColumnDef(columns));
			ColumnDef [] columnDef = result.getColumnDef();
			Object [] rowData = new Object[columnDef.length];
			for (int i=0; i<columnDef.length; i++) {
				ColumnDef c = columnDef[i];
				rowData[i] = columns.get(c.getName());
			}
			result.addRow(rowData);
		} else {
			throw new ZeppelinDriverException("List not found in path "+listPath);
		}
		try {
			response.close();
		} catch (IOException e) {			
		}
		
		if (result!=null) {
			result.rawData = responseJson;
		}
		return result;
	}
	
	public ColumnDef [] createColumnDef(Map<String, Object> json){
		LinkedList<ColumnDef> columns = new LinkedList<ColumnDef>();
		Set<Entry<String, Object>> entries = json.entrySet();
		for(Entry<String, Object> e : entries) {
			String key = e.getKey();
			Object value = e.getValue();
			if (value instanceof Integer) {
				columns.add(new ColumnDef(key, Types.INTEGER, "INT"));
			} else if (value instanceof Long) {
				columns.add(new ColumnDef(key, Types.BIGINT, "LONG"));
			} else if(value instanceof Float) {
				columns.add(new ColumnDef(key, Types.FLOAT, "FLOAT"));
			} else if(value instanceof Double) {
				columns.add(new ColumnDef(key, Types.DOUBLE, "DOUBLE"));
			} else if(value instanceof String) {
				columns.add(new ColumnDef(key, Types.VARCHAR, "STRING"));				
			} else {
				// unsupported conversion
			}
		}
		
		return columns.toArray(new ColumnDef []{});
	}
	
	public Object findObjectFromJson(Map<String, Object> json, String path) {
		if (path==null) return json;
		
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		
		String[] paths = path.split("/");
		if (paths.length==0) return json;

		Object o = json.get(paths[0]);
		if (paths.length==1) {
			return o;
		} else {
			if (o instanceof Map) {
				String childPath = "";
				for (int i=1; i<paths.length; i++){
					childPath=childPath+"/"+paths[i];
				}
				return findObjectFromJson((Map<String, Object>) o, childPath);
			} else {
				throw new ZeppelinDriverException("Can't find path on the document");
			}
		}
		
		
	}
	
	
	private void dumpJson(Map<String, Object> json){
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
