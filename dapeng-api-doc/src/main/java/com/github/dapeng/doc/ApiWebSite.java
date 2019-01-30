/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        connector.setReuseAddress(true);

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
