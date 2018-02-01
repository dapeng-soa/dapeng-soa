package com.github.dapeng.zookeeper;

import org.apache.zookeeper.server.quorum.QuorumPeerMain;

import java.io.File;

/**
 * Zookeeper Server
 *
 * @author craneding
 * @date 16/1/13
 */
public class ZookeeperServer {

    public static void main(String[] args) {
        System.setProperty("zookeeper.jmx.log4j.disable", "true");

        if (args != null && args.length > 0)
            QuorumPeerMain.main(args);
        else
            QuorumPeerMain.main(new String[]{new File("dapeng-zookeeper", "src/main/resources/conf/zoo.cfg").getAbsolutePath()});
    }

}
