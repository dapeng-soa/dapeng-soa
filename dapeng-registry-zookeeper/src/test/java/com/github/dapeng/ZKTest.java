package com.github.dapeng;

import com.github.dapeng.zookeeper.agent.ClientZkAgent;
import com.github.dapeng.zookeeper.agent.ServerZkAgent;
import com.github.dapeng.zookeeper.agent.impl.ClientZkAgentImpl;
import com.github.dapeng.zookeeper.agent.impl.ServerZkAgentImpl;
import org.apache.zookeeper.*;
import org.junit.Test;

/**
 * @author huyj
 * @Created 2018/5/28 11:31
 */
public class ZKTest {
    @Test
    public void CreateNodeTest() throws Exception {


        ServerZkAgent serverZkAgent = ServerZkAgentImpl.getServerZkAgentInstance();
        System.out.println(serverZkAgent.getZkClient().getZkDataContext());
        serverZkAgent.registerService("com.github.dapeng.hello.service.HelloService", "1.0.0");

        ClientZkAgent clientZkAgent = ClientZkAgentImpl.getClientZkAgentInstance();
        System.out.println(clientZkAgent.getZkClient().getZkDataContext());

        System.out.println("------------------------------------");
        /*CuratorFramework curatorFramework1 = ZKConnectFactory.getCuratorClient("127.0.0.1:2181");
        CuratorFramework curatorFramework2 = ZKConnectFactory.getCuratorClient("192.168.4.154:2181");
        CuratorFramework curatorFramework3 = ZKConnectFactory.getCuratorClient("192.168.4.102:2181");
        CuratorFramework curatorFramework4 = ZKConnectFactory.getCuratorClient("10.10.10.45:2181");
        CuratorFramework curatorFramework5 = ZKConnectFactory.getCuratorClient("192.168.4.154:2181");*/

        while (true) {
            System.out.println("*******************************");
            Thread.sleep(1000 * 10);
        }

        /* System.out.println("********************************");*/
    }


    @Test
    public void testNativeZK() throws Exception {
        // Watcher实例
        ZooKeeper zk = new ZooKeeper("127.0.0.1", 500000, null);
        System.out.println("---------------------");
// 创建一个节点root，数据是mydata,不进行ACL权限控制，节点为永久性的(即客户端shutdown了也不会消失)
        zk.register(new Watcher() {
            public void process(WatchedEvent event) {
                //System.out.println("回调watcher实例： 路径" + event.getPath() + " 类型：" + event.getType());
                System.out.println("回调watcher实例*****************");
            }
        });
        //zk.exists("/root", true);
        zk.create("/root", "mydata".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        System.out.println("---------------------");

// 在root下面创建一个childone znode,数据为childone,不进行ACL权限控制，节点为永久性的

        zk.exists("/root/childone", true);

        zk.create("/root/childone", "childone".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        System.out.println("---------------------");

// 删除/root/childone这个节点，第二个参数为版本，－1的话直接删除，无视版本

        zk.exists("/root/childone", true);

        zk.delete("/root/childone", -1);

        System.out.println("---------------------");

        zk.exists("/root", true);

        zk.delete("/root", -1);

        System.out.println("---------------------");


// 关闭session

        zk.close();
    }
}
