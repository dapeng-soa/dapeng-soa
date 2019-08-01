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
package com.github.dapeng.registry.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ZkUtils {
    private final static Logger LOGGER = LoggerFactory.getLogger(ZkUtils.class);

    final static String RUNTIME_PATH = "/soa/runtime/services";
    final static String CONFIG_PATH = "/soa/config/services";
    final static String ROUTES_PATH = "/soa/config/routes";
    final static String COOKIE_RULES_PATH = "/soa/config/cookies";
    final static String FREQ_PATH = "/soa/config/freq";

    public static void syncZkConfigInfo(ZkServiceInfo zkInfo, ZooKeeper zk, Watcher watcher, boolean isGlobal) {
        if (!isZkReady(zk)) return;

        String configPath = CONFIG_PATH;
        if (!isGlobal) {
            configPath += "/" + zkInfo.serviceName();
        }

        try {
            byte[] data = zk.getData(configPath, watcher, null);
            ZkDataProcessor.processZkConfig(data, zkInfo, isGlobal);
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error(ZkUtils.class + "::syncZkConfigInfo failed, service:"
                    + zkInfo.serviceName() + ", zk status:" + zk.getState(), e);
        }
    }

    /**
     * 异步添加serverInfo,为临时有序节点，如果server挂了就木有了
     */
    public static void createEphemeral(String path, String data, ZooKeeper zkClient) throws KeeperException, InterruptedException {
        // 如果存在重复的临时节点， 删除之。
        // 由于目前临时节点采用CreateMode.EPHEMERAL_SEQUENTIAL的方式， 会自动带有一个序号(ip:port:version:seq)，
        // 故判重方式需要比较(ip:port:version)即可
        int i = path.lastIndexOf("/");
        if (i > 0) {
            String parentPath = path.substring(0, i);
            createPersistNodeOnly(parentPath, zkClient);
            try {
                List<String> childNodes = zkClient.getChildren(parentPath, false);
                String _path = path.substring(i + 1);
                for (String nodeName : childNodes) {
                    if (nodeName.startsWith(_path)) {
                        zkClient.delete(parentPath + "/" + nodeName, -1);
                    }
                }
            } catch (KeeperException | InterruptedException e) {
                LOGGER.error("ServerZk::createEphemeral delete exist nodes failed, zk status:" + zkClient.getState(), e);
            }
        }

        zkClient.create(path + ":", data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    /**
     * 异步添加持久化的节点
     *
     * @param path
     * @param data
     */
    public static void createPersistent(String path, String data, ZooKeeper zkClient) throws KeeperException, InterruptedException {
        int i = path.lastIndexOf("/");
        if (i > 0) {
            String parentPath = path.substring(0, i);
            createPersistNodeOnly(parentPath, zkClient);
        }
        if (!exists(path, zkClient)) {
            zkClient.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    /**
     * 递归节点创建, 不监听
     */
    public static void createPersistNodeOnly(String path, ZooKeeper zkClient) {

        int i = path.lastIndexOf("/");
        if (i > 0) {
            String parentPath = path.substring(0, i);
            //判断父节点是否存在...
            if (!exists(parentPath, zkClient)) {
                createPersistNodeOnly(parentPath, zkClient);
            }
        }

        try {
            zkClient.create(path, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException | InterruptedException e) {
            if (e instanceof KeeperException.NodeExistsException) {
                LOGGER.info(ZkUtils.class + "::createPersistNodeOnly failed," + e.getMessage());
            } else {
                LOGGER.error(ZkUtils.class + "::createPersistNodeOnly failed, zk status:" + zkClient.getState(), e);
            }
        }
    }

    /**
     * 检查节点是否存在
     */
    public static boolean exists(String path, ZooKeeper zkClient) {
        try {
            Stat exists = zkClient.exists(path, false);
            return exists != null;
        } catch (Throwable t) {
            LOGGER.error(ZkUtils.class + "::exists check failed, zk status:" + zkClient.getState(), t);
        }
        return false;
    }

    /**
     * 判断zk客户端是否就绪
     *
     * @param zk
     * @return
     */
    public static boolean isZkReady(ZooKeeper zk) {
        boolean isValid = ((zk != null) && zk.getState().isConnected());
        if (!isValid) {
            LOGGER.warn(ZkUtils.class + "::isZkReady zk is not ready, status:"
                    + (zk == null ? null : zk.getState()));
        }
        return isValid;
    }
}
