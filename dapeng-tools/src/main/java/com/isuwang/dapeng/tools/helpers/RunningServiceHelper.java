package com.github.dapeng.tools.helpers;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tangliu on 16/11/18.
 */
public class RunningServiceHelper {

    private ZooKeeper zk;

    private final String ZOOKEEPER_HOST = System.getProperty("soa.zookeeper.host", "127.0.0.1:2181");

    private static final String PATH = "/soa/runtime/services";

    private void connect() {

        try {
            zk = new ZooKeeper(ZOOKEEPER_HOST, 15000, new Watcher() {
                public void process(WatchedEvent e) {
                    if (e.getState() == Watcher.Event.KeeperState.SyncConnected) {

                        System.out.println("zookeeper connected...");
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showServices() {

        final Map<String, List<String>> serviceInfos = new HashMap<>();

        try {
            List<String> services = zk.getChildren(PATH, false);

            for (String service : services) {

                List<String> details = zk.getChildren(PATH + "/" + service, false);

                serviceInfos.put(service, details);
            }

        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("----------------------Running Service Info-----------------------");
        serviceInfos.keySet().stream().forEach(service -> {

            if (serviceInfos.get(service).size() > 0) {
                System.out.println(service);

                for (String detail : serviceInfos.get(service)) {
                    String[] infos = detail.split(":");
                    System.out.println("    ip:" + infos[0] + "   port:" + infos[1] + "   version:" + infos[2]);
                }
            }

        });
        System.out.println("---------------------------end-----------------------------------");

        serviceInfos.clear();
    }

    public static void showRunningService() {

        RunningServiceHelper rsh = new RunningServiceHelper();
        rsh.connect();

        rsh.showServices();
    }
}
