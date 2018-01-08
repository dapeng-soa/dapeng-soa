package com.github.dapeng.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Created by tangliu on 2016/6/23.
 */
public class RouteConfigHelper {

    private final Logger LOGGER = LoggerFactory.getLogger(RouteConfigHelper.class);

    private ZooKeeper zk;

    private final String ZOOKEEPER_HOST = System.getProperty("soa.zookeeper.host", "127.0.0.1:2181");

    /**
     * connect to zookeeper
     */
    private void connect() {

        try {
            zk = new ZooKeeper(ZOOKEEPER_HOST, 15000, new Watcher() {
                @Override
                public void process(WatchedEvent e) {
                    if (e.getState() == Event.KeeperState.SyncConnected) {
                        LOGGER.info("Route Config Helper connected to zookeeper {}.", ZOOKEEPER_HOST);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() {
        createConfigNodeWithData("/soa", "");
        createConfigNodeWithData("/soa/config", "");
    }


    private void writeRouteConfig(String configContent) {
        createConfigNodeWithData("/soa/config/route", configContent);
    }

    private void createConfigNodeWithData(String path, String data) {
        zk.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, configNodeCreateCallBack, data);
    }

    /**
     * back call of createConfigNodeWithData
     */
    private AsyncCallback.StringCallback configNodeCreateCallBack = new AsyncCallback.StringCallback() {
        @Override
        public void processResult(int rc, String path, Object ctx, String name) {

            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    LOGGER.info("connection loss while creating node {} with data [{}], try again.", path, (String) ctx);
                    createConfigNodeWithData(path, (String) ctx);
                    break;
                case OK:
                    LOGGER.info("creating node {} succeed with data [{}]", path, (String) ctx);
                    break;
                case NODEEXISTS:
                    LOGGER.info("node {} already exist while creating, update with data [{}]", path, (String) ctx);
                    updateConfigNodeData(path, (String) ctx);
                    break;
                default:
                    LOGGER.info("Something went wrong when creating server info");
            }
        }
    };


    public void updateConfigNodeData(String path, String data) {
        zk.setData(path, data.getBytes(), -1, configNodeDataUpdateCB, data);
    }


    /**
     * call back of updateConfigNodeData
     */
    private AsyncCallback.StatCallback configNodeDataUpdateCB = new AsyncCallback.StatCallback() {
        @Override
        public void processResult(int i, String s, Object o, Stat stat) {
            switch (KeeperException.Code.get(i)) {
                case CONNECTIONLOSS:
                    updateConfigNodeData(s, (String) o);
                    return;
            }
        }
    };


    public static void main(String[] args) throws InterruptedException {

        if (args.length != 1) {
            System.out.println("参数错误，请指定路由配置文件地址");
            return;
        }
        File file = new File(args[0]);
        if (!file.exists() || !file.isFile()) {
            System.out.println("找不到配置文件:" + args[0]);
        }

        String configContent = "";
        try {
            InputStream is = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = "";
            while ((line = br.readLine()) != null) {

                configContent += line;
                configContent += "\n";
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        RouteConfigHelper routeConfigHelper = new RouteConfigHelper();
        routeConfigHelper.connect();
        routeConfigHelper.init();

        routeConfigHelper.writeRouteConfig(configContent);


        Thread.sleep(Long.MAX_VALUE);
    }
}
