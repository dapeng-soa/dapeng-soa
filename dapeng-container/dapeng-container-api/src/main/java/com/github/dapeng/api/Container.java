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

import com.github.dapeng.core.Application;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.filter.Filter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 大鹏容器的主结构，负责管理容器相关的监听器，插件，应用程序。
 *
 * 所有的组件的注册，卸载动作都应该由Container来负责，
 */
public interface Container {
    /**
     * 容器状态
     */
    int STATUS_UNKNOWN = 0;
    int STATUS_CREATING = 1;
    int STATUS_RUNNING = 2;
    int STATUS_SHUTTING = 3;
    int STATUS_DOWN = 4;

    /**
     * 注册应用程序监听器，
     * @param listener
     */
    void registerAppListener(AppListener listener);

    /**
     * 卸载用用程序监听器
     * @param listener
     */
    void unregisterAppListener(AppListener listener);

    /**
     * 注册应用程序（保存容器具体的应用信息）
     * @param app
     */
    void registerApplication(Application app);

    /**
     * 卸载应用程序
     * @param app
     */
    void unregisterApplication(Application app);

    /**
     * 注册插件(like: Zookeeper,netty..etc.)
     * @param plugin
     */
    void registerPlugin(Plugin plugin);

    /**
     * 卸载插件
     * @param plugin
     */
    void unregisterPlugin(Plugin plugin);

    /**
     * 注册Filter(like: monitor)
     */
    void registerFilter(Filter filter);

    /**
     * 卸载Filter
     * @param filter
     */
    void unregisterFilter(Filter filter);

    /**
     * 获取应用程序的相关信息
     * @return
     */
    List<Application> getApplications();

    List<Plugin> getPlugins();

    Map<ProcessorKey, SoaServiceDefinition<?>> getServiceProcessors();

    // fixme @Deprecated
    void registerAppProcessors(Map<ProcessorKey, SoaServiceDefinition<?>> processors);

    Application getApplication(ProcessorKey key);

    // fixme @Deprecated
    void registerAppMap(Map<ProcessorKey,Application> applicationMap);

    Executor getDispatcher();

    List<Filter> getFilters();

    void startup();

    /**
     * 0:unknow;
     * 1:creating;
     * 2:running;
     * 3:shutting
     * 4:down
     *
     * @return status of container
     */
    int status();

    /**
    * 容器内未完成的请求计数
    */
    AtomicInteger requestCounter();
}
