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
package com.github.dapeng.core.helper;


import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.apache.zookeeper.Watcher.Event.KeeperState.Disconnected;
import static org.apache.zookeeper.Watcher.Event.KeeperState.SyncConnected;

public class DapengUtilTest {
    public static void main(String[] args) throws InterruptedException {
//        Long tid = DapengUtil.generateTid();
//        System.out.println("finally: " + DapengUtil.longToHexStr(tid));
//        long begin = System.currentTimeMillis();
//        long counter = 0;
//        while (true) {
//            long newTid = DapengUtil.generateTid();
//            counter++;
//            if (newTid == tid) {
//                System.out.println("counter:" + counter + " end:" + (System.currentTimeMillis() - begin));
//                System.out.println(DapengUtil.longToHexStr(newTid));
//                begin = System.currentTimeMillis();
//            }
//        }
        System.out.println(IPUtils.transferIp("192.168.10.23"));

        CountDownLatch semaphore = new CountDownLatch(1);

        // default watch
        Tmp tmp = new Tmp(e -> {
            System.out.println("ClientZk::connect zkEvent:" + e);
            switch (e.getState()) {
                case Expired:
                    break;
                case SyncConnected:
                    semaphore.countDown();
                    break;
                case Disconnected:
                    break;
                case AuthFailed:
                    break;
                default:
                    break;
            }
        });
        tmp.callBackWithinAnotherThread();
        System.out.println("main thread1..");
        tmp.eventThread.echo();
        semaphore.await(10000, TimeUnit.MILLISECONDS);
        System.out.println("main thread2..");
    }
}

class Tmp {
    final EventThread eventThread;
    public Tmp(Watcher e) {
        eventThread = new EventThread(e);
    }

    public void callBackWithinAnotherThread() {
        eventThread.start();
    }
}

class EventThread extends Thread {
    final Watcher e;
    public EventThread(Watcher e) {
        this.e = e;
    }

    public void run() {
        System.out.println(Thread.currentThread().getName() + ":: start..");
        try {
            Thread.currentThread().sleep(5000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        System.out.println(Thread.currentThread().getName() + ":: before callback..");
        e.process(new WatchedEvent(Watcher.Event.EventType.None, SyncConnected, ""));
        System.out.println(Thread.currentThread().getName() + ":: after callback..");
    }

    public void echo() {
        System.out.println("echo");
    }
}
