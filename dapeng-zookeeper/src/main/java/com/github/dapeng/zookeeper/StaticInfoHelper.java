package com.github.dapeng.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by tangliu on 2016/2/15.
 */
public class StaticInfoHelper {

    private final Logger LOGGER = LoggerFactory.getLogger(StaticInfoHelper.class);

    private ZooKeeper zk;

    private final String ZOOKEEPER_HOST = "127.0.0.1:2181";

    /**
     * connect to zookeeper
     */
    private void connect() {

        try {
            zk = new ZooKeeper(ZOOKEEPER_HOST, 15000, new Watcher() {
                @Override
                public void process(WatchedEvent e) {
                    if (e.getState() == Watcher.Event.KeeperState.SyncConnected) {
                        LOGGER.info("zookeeper connected.");
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void init() {

        createConfigNodeWithData("/soa", "");
        createConfigNodeWithData("/soa/config", "");
        createConfigNodeWithData("/soa/config/service", "");
    }

    private void setLoadBalance() {

        //clear old properties on zookeeper
        deleteAllChildren("/soa/config/service");

        Properties prop = new Properties();
        try {
            InputStream in = StaticInfoHelper.class.getClassLoader().getResourceAsStream("config.properties");
            prop.load(in);
        } catch (IOException e) {
            LOGGER.error("error reading file config.properties");
            LOGGER.error(e.getMessage(), e);
        }

        Iterator<Map.Entry<Object, Object>> it = prop.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, Object> entry = it.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            String t_value = ((String) value).replaceAll("/", "=");

            createConfigNodeWithData("/soa/config/service/" + (String) key, t_value);
        }

    }

    private void deleteAllChildren(String path) {

        try {
            List<String> children = zk.getChildren(path, null);
            for (String configNodeName : children) {
                zk.delete(path + "/" + configNodeName, -1);
                LOGGER.info("node {} deleted. ", path + "/" + configNodeName);
            }
        } catch (KeeperException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
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
                default:
                    //Just skip
            }
        }
    };


    public static void main(String[] args) throws InterruptedException {

        StaticInfoHelper staticInfoHelper = new StaticInfoHelper();
        staticInfoHelper.connect();

        staticInfoHelper.init();

        staticInfoHelper.setLoadBalance();

        Thread.sleep(Long.MAX_VALUE);
    }
}
