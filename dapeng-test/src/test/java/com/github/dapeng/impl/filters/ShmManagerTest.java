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
//package com.github.dapeng.impl.filters;
//
//import com.github.dapeng.core.FreqControlRule;
//import com.github.dapeng.impl.filters.freq.ShmManager;
//
///**
// * Desc: TODO
// *
// * @author hz.lei
// * @date 2018年05月14日 上午11:36
// */
//public class ShmManagerTest {
//
//    public static void main(String[] args) {
//        final ShmManager manager = ShmManager.getInstance();
//
//        final FreqControlRule rule = new FreqControlRule();
//        rule.app = "com.today.hello";
//        rule.ruleType = "callId";
//        rule.minInterval = 60;
//        rule.maxReqForMinInterval = 20;
//        rule.midInterval = 3600;
//        rule.maxReqForMidInterval = 100;
//        rule.maxInterval = 86400;
//        rule.maxReqForMaxInterval = 500;
//
//        long t1 = System.currentTimeMillis();
//        for (int i = 0; i < 10000; i++) {
//            for (int j = 0; j < 100; j++) {
//                manager.reportAndCheck(rule, j);
//
//            }
//        }
//        long t2 = System.currentTimeMillis();
//
//        System.out.println((t2 - t1));
//
//
//
//
//       /* new Thread( ()-> { ttt(manager, rule, 100); }).start();
//        new Thread( ()-> { ttt(manager, rule, 101); }).start();
//        new Thread( ()-> { ttt(manager, rule, 102); }).start();
//        new Thread( ()-> { ttt(manager, rule, 103); }).start();*/
//
//
////        System.out.println("cost1:" + (System.currentTimeMillis() - t1));
////
////
////        for (int j = 0; j < 10; j++) {
////            t1 = System.currentTimeMillis();
////            for (int i = 0; i < 1000000; i++) {
////                manager.reportAndCheck(rule, 100);
////            }
////            System.out.println("cost2:" + (System.currentTimeMillis() - t1));
////        }
//    }
//
//    static void ttt(ShmManager manager, FreqControlRule rule, int key) {
//        long t1 = System.currentTimeMillis();
//        for (int i = 0; i < 1000000; i++) {
//            manager.reportAndCheck(rule, key);
//        }
//        long t2 = System.currentTimeMillis();
//        System.out.println(Thread.currentThread() + " cost:" + (t2 - t1) + "ms");
//    }
//}
