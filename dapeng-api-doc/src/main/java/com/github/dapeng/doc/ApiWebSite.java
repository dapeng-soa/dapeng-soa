package com.github.dapeng.doc;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by tangliu on 2016/3/3.
 */
public class ApiWebSite {

    //private static final int port = 8080;

    private static final String CONTEXT = "/";

    public static Server createServer(int port) throws MalformedURLException, URISyntaxException {
        Server server = new Server();
        server.setStopAtShutdown(true);

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        connector.setReuseAddress(false);

        server.setConnectors(new Connector[]{connector});

        WebAppContext webContext = new WebAppContext("webapp", CONTEXT);
        webContext.setBaseResource(Resource.newResource(new URL(ApiWebSite.class.getResource("/webapp/WEB-INF"), ".")));
        webContext.setClassLoader(ApiWebSite.class.getClassLoader());

        server.setHandler(webContext);

        return server;
    }

    /*
    public static void main(String[] args) throws Exception {
        Server server = ApiWebSite.createServer(ApiWebSite.port);

        try {
            server.stop();
            server.start();
            server.join();

        } catch (Exception e) {

            e.printStackTrace();
            System.out.println("API站点启动失败...");
        }
    }
    */
}
