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
package com.github.dapeng.api;


import java.util.List;
import java.util.ServiceLoader;

public class ContainerFactory {

    private static volatile Container applicationContainer;

    public static void createContainer(List<ClassLoader> applicationCls, ClassLoader containerCl, ClassLoader pluginCl) {
        if (applicationContainer == null) {
            synchronized (ContainerFactory.class) {
                ServiceLoader<ContainerFactorySpi> containerFactorySpis = ServiceLoader.load(ContainerFactorySpi.class, containerCl);
                assert containerFactorySpis.iterator().hasNext();
                applicationContainer = containerFactorySpis.iterator().next().createInstance(applicationCls, pluginCl);
            }
        }
    }

    public static Container getContainer() {
        return applicationContainer;
    }
}
