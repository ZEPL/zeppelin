package com.nflabs.zeppelin.server;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nflabs.zeppelin.conf.ZeppelinConfiguration;
import com.nflabs.zeppelin.conf.ZeppelinConfiguration.ConfVars;
import com.nflabs.zeppelin.interpreter.InterpreterFactory;
import com.nflabs.zeppelin.notebook.Notebook;
import com.nflabs.zeppelin.rest.ZeppelinRestApi;
import com.nflabs.zeppelin.scheduler.SchedulerFactory;
import com.nflabs.zeppelin.socket.NotebookServer;

public class ZeppelinServer extends Application {
  
	private static final Logger LOG = LoggerFactory.getLogger(ZeppelinServer.class);
	public static Notebook notebook;
	
	private SchedulerFactory schedulerFactory;
	private InterpreterFactory replFactory;

	public static void main(String [] args) throws Exception{
		ZeppelinConfiguration conf = ZeppelinConfiguration.create();
        conf.setProperty("args",args);

		int port = conf.getInt(ConfVars.ZEPPELIN_PORT);
        final Server server = setupJettyServer(port);
        final NotebookServer websocket = new NotebookServer(port + 1);

        setupZeppelinServerContextHandlers(server, conf);
        startZeppelinServers(server, websocket);
        addShutdownHook(server, websocket);
		server.join();
	}

    private static Server setupJettyServer(int port) {
        int timeout = 1000*30;
        final Server server = new Server();
        SocketConnector connector = new SocketConnector();

        // Set some timeout options to make debugging easier.
        connector.setMaxIdleTime(timeout);
        connector.setSoLingerTime(-1);
        connector.setPort(port);
        server.addConnector(connector);
        return server;
    }

  private static void startZeppelinServers(final Server server, final NotebookServer websocket)
      throws Exception {
    LOG.info("Start zeppelin servers");
    websocket.start();
    server.start();
    LOG.info("Started");
  }

  private static void addShutdownHook(final Server server, final NotebookServer websocket) {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        LOG.info("Shutting down Zeppelin Servers ... ");
        try {
          server.stop();
          websocket.stop();
        } catch (Exception e) {
          LOG.error("Error while stopping servlet container", e);
        }
        LOG.info("Bye");
      }
    });
  }
  
  private static void setupZeppelinServerContextHandlers(final Server server, 
                                                         final ZeppelinConfiguration conf) {
    // REST api
    final ServletContextHandler restApi = setupRestApiContextHandler();
    // Web UI
    final WebAppContext webApp = setupWebAppContext(conf);

    // add all handlers
    ContextHandlerCollection contexts = new ContextHandlerCollection();
    contexts.setHandlers(new Handler[] {restApi, webApp});
    server.setHandler(contexts);
  }
    
    private static ServletContextHandler setupRestApiContextHandler() {
        final ServletHolder cxfServletHolder = new ServletHolder( new CXFNonSpringJaxrsServlet() );
		cxfServletHolder.setInitParameter("javax.ws.rs.Application", ZeppelinServer.class.getName());
		cxfServletHolder.setName("rest");
		cxfServletHolder.setForcedPath("rest");

		final ServletContextHandler cxfContext = new ServletContextHandler();
		cxfContext.setSessionHandler(new SessionHandler());
		cxfContext.setContextPath("/api");
		cxfContext.addServlet( cxfServletHolder, "/*" );
        return cxfContext;
    }

    private static WebAppContext setupWebAppContext(ZeppelinConfiguration conf) {
        WebAppContext webApp = new WebAppContext();
        File webapp = new File(conf.getString(ConfVars.ZEPPELIN_WAR));
        if(webapp.isDirectory()){ // Development mode, read from FS
            //webApp.setDescriptor(webapp+"/WEB-INF/web.xml");
            webApp.setResourceBase(webapp.getPath());
            webApp.setContextPath("/");
            webApp.setParentLoaderPriority(true);
        } else { //use packaged WAR
            webApp.setWar(webapp.getAbsolutePath());
        }
        // Explicit bind to root
        webApp.addServlet(new ServletHolder(new DefaultServlet()), "/*");
        return webApp;
    }


	public ZeppelinServer() throws Exception {
		ZeppelinConfiguration conf = ZeppelinConfiguration.create();
		
		this.schedulerFactory = new SchedulerFactory();

		this.replFactory = new InterpreterFactory(conf);
		notebook = new Notebook(conf, schedulerFactory, replFactory);
	}

	@Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        return classes;
    }

	@Override
    public java.util.Set<java.lang.Object> getSingletons(){
    	Set<Object> singletons = new HashSet<Object>();

    	/** Rest-api root endpoint */
    	ZeppelinRestApi root = new ZeppelinRestApi();
    	singletons.add(root);
    	
    	return singletons;
    }

}
