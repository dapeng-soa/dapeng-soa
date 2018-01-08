package com.github.dapeng.tools.helpers;


import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.List;

/**
 * Created by Eric on 2016/2/15.
 */
public class ZookeeperSerachHelper {

    private static ZooKeeper zk;

    private final String ZOOKEEPER_HOST = System.getProperty("soa.zookeeper.host", "127.0.0.1:2181");

    private static final String PATH = "/soa/runtime/services";
    StringBuffer infoBuf = new StringBuffer();

    private void connect() {

        try {
            zk = new ZooKeeper(ZOOKEEPER_HOST, 15000, new Watcher() {
                public void process(WatchedEvent e) {
                    if (e.getState() == Watcher.Event.KeeperState.SyncConnected) {
//                        System.out.println("zookeeper connected.");
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> getServiceNames() throws Exception {
        return zk.getChildren(PATH, false);
    }

    private List<String> getServiceInfo(String serviceName) throws Exception {
        return zk.getChildren(PATH + "/" + serviceName, false);
    }

    private String filterAndformatService(String... args) throws Exception {
        String serviceName = null;
        String version = null;
        if (args.length == 3) {
            serviceName = args[1];
            version = args[2];
        } else if (args.length == 2) {
            serviceName = args[1];
        } else {
            throw new Exception("illegal argument");
        }
        List<String> services = getServiceNames();
        for (String service : services) {
            if (serviceName.equals(service)) {
                List<String> serviceInfos = getServiceInfo(serviceName);
                if (serviceInfos == null || serviceInfos.size() == 0) {
                    infoBuf.append("无法找到对应的服务");
                }
                for (String serviceInfo : serviceInfos) {
                    filterServiceInfo(serviceName, version, serviceInfo);
                }
            }
        }
        return infoBuf.toString();
    }

    private void filterServiceInfo(String serviceName, String version, String serviceInfo) {
        if (version == null) {
            format(serviceName, serviceInfo);
        } else if (serviceInfo.endsWith(":" + version)) {
            format(serviceName, serviceInfo);
        }
        infoBuf.append("\n");
    }

    private void format(String serviceName, String serviceInfo) {
        String[] infos = serviceInfo.split(":");
        String host = infos[0];
        String port = infos[1];
        String version = infos[2];
        padString(serviceName, 60);
        padString(version, 20);
        padString(host, 30);
        padString(port, 25);

    }

    private void padString(String info, int length) {
        infoBuf.append(info);
        int infoLength = info.length();
        while (infoLength < length) {
            infoBuf.append(" ");
            infoLength++;
        }
    }

    public static void getInfos(String... args) {
        try {
            ZookeeperSerachHelper staticInfoHelper = new ZookeeperSerachHelper();
            staticInfoHelper.connect();
            System.out.println(staticInfoHelper.filterAndformatService(args));
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
}
