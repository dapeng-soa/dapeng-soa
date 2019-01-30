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
    }
}
