package com.dapeng.soa;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.soa.PrintServiceAsyncClient;
import com.github.dapeng.soa.PrintServiceClient;

import java.util.concurrent.CompletableFuture;


/**
 * Created by admin on 2017/8/16.
 */
public class TestClient {

    public static void main(String[] args) throws InterruptedException, SoaException {

        System.setProperty("soa.zookeeper.host", "192.168.99.100:2181");

        //测试java客户端同步
        PrintServiceClient client = new PrintServiceClient();
        String result = client.printInfo2("test");
        System.out.println("result:"+result);

        //测试java客户端异步
        PrintServiceAsyncClient asyncClient = new PrintServiceAsyncClient();
        CompletableFuture<String> asyncResult = asyncClient.printInfo2("test",20000);
        asyncResult.whenComplete((res,ex) ->{
            System.out.println(res);
        });


    }
}
