package com.github.dapeng.tools.helpers;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.*;


/**
 * @author Eric on 2016-06-27
 */
public class RouteInfoHelper {
    private static ZooKeeper zk;

    private final String ZOOKEEPER_HOST = System.getProperty("soa.zookeeper.host", "127.0.0.1:2181");

    private static final String PATH = "/soa/config/route";
    StringBuffer infoBuf = new StringBuffer();

    private void connect() {

        try {
            zk = new ZooKeeper(ZOOKEEPER_HOST, 15000, new Watcher() {
                public void process(WatchedEvent e) {
                    if (e.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createConfigNodeWithData(String path, String data) {
        zk.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, configNodeCreateCallBack, data);
    }

    /**
     * back call of createConfigNodeWithData
     */
    private static AsyncCallback.StringCallback configNodeCreateCallBack = new AsyncCallback.StringCallback() {
        @Override
        public void processResult(int rc, String path, Object ctx, String name) {

            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    createConfigNodeWithData(path, (String) ctx);
                    break;
                case OK:
                    break;
                case NODEEXISTS:
                    updateConfigNodeData(path, (String) ctx);
                    break;
                default:
                    break;
            }
        }
    };

    public static void updateConfigNodeData(String path, String data) {
        zk.setData(path, data.getBytes(), -1, configNodeDataUpdateCB, data);
    }


    /**
     * call back of updateConfigNodeData
     */
    private static AsyncCallback.StatCallback configNodeDataUpdateCB = new AsyncCallback.StatCallback() {
        @Override
        public void processResult(int i, String s, Object o, Stat stat) {
            switch (KeeperException.Code.get(i)) {
                case CONNECTIONLOSS:
                    updateConfigNodeData(s, (String) o);
                    return;
            }
        }
    };

    public static void routeInfo(String... args) {
        try {
            RouteInfoHelper routeInfoHelper = new RouteInfoHelper();
            routeInfoHelper.connect();

            if (args.length == 2) {
                String routeRules = readRouteFromConfFile(args[1]);
                updateConfigNodeData(PATH, routeRules);
            } else {
                byte[] data = zk.getData(PATH, false, null);
                System.out.println(new String(data));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            try {
                zk.close();
            } catch (Exception e1) {
                System.err.println(e1.getMessage());
            }
        }

    }

    private static String readRouteFromConfFile(String arg) {
        StringBuffer configContent = new StringBuffer();
        try {
            InputStream is = new FileInputStream(arg);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = "";
            while ((line = br.readLine()) != null) {
                configContent.append(line).append("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return configContent.toString();
    }
}
