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
package com.github.dapeng.router;

public class RouterTest {
    public static void main(String[] args) {
        ShutDownHookDemo demo = new ShutDownHookDemo();
        System.out.println("shutdown begin...");
        Runtime.getRuntime().addShutdownHook(new Thread(ShutDownHookDemo::shutdown));
        Runtime.getRuntime().addShutdownHook(new Thread(ShutDownHookDemo::shutdown));

        System.out.println("shutdown end..");
    }
}

class ShutDownHookDemo {
    public static void shutdown() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Inside ShutDownHookDemo:shutdown..");
    }
}
