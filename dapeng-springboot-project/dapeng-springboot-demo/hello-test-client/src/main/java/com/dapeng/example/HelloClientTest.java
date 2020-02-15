package com.dapeng.example;

import com.dapeng.example.hello.HelloServiceClient;
import com.github.dapeng.core.SoaException;

/**
 * @author with struy.
 * Create by 2018/7/9 00:24
 * email :yq1724555319@gmail.com
 */

public class HelloClientTest {
    public static void main(String[] args) throws SoaException {
        System.setProperty("soa.zookeeper.host", "127.0.0.1:2181");
        HelloServiceClient client = new HelloServiceClient();
        String res = client.sayHello("Dapeng");
        System.out.println("result-->" + res);
    }
}
