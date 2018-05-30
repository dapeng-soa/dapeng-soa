package com.github.dapeng;

import com.github.dapeng.zoomkeeper.agent.ClientZkAgent;
import com.github.dapeng.zoomkeeper.agent.ServerZkAgent;
import com.github.dapeng.zoomkeeper.agent.impl.ClientZkAgentImpl;
import com.github.dapeng.zoomkeeper.agent.impl.ServerZkAgentImpl;
import org.junit.Test;

/**
 * @author huyj
 * @Created 2018/5/28 11:31
 */
public class ZKTest {
    @Test
    public void CreateNodeTest() throws Exception {


        ClientZkAgent clientZkAgent = ClientZkAgentImpl.getClientZkAgentInstance();
        System.out.println(clientZkAgent.getZkClient().getZkDataContext());

        ServerZkAgent serverZkAgent = ServerZkAgentImpl.getServerZkAgentInstance();
        System.out.println(serverZkAgent.getZkClient().getZkDataContext());
        System.out.println("------------------------------------");


        serverZkAgent.registerService("com.github.dapeng.hello.service.HelloService", "1.0.0");

        /*CuratorFramework curatorFramework1 = ZKConnectFactory.getCuratorClient("127.0.0.1:2181");
        CuratorFramework curatorFramework2 = ZKConnectFactory.getCuratorClient("192.168.4.154:2181");
        CuratorFramework curatorFramework3 = ZKConnectFactory.getCuratorClient("192.168.4.102:2181");
        CuratorFramework curatorFramework4 = ZKConnectFactory.getCuratorClient("10.10.10.45:2181");
        CuratorFramework curatorFramework5 = ZKConnectFactory.getCuratorClient("192.168.4.154:2181");*/

       /* while (true) {
            System.out.println("*******************************");
            Thread.sleep(1000 * 10);
        }*/

        /* System.out.println("********************************");*/
    }
}
