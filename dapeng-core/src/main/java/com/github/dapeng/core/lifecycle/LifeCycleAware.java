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
package com.github.dapeng.core.lifecycle;

/**
 * 提供给业务的lifecycle接口，四种状态
 *
 * @author hui
 * @date 2018/7/26 11:21
 */
public interface LifeCycleAware {

    /**
     * 容器启动时回调方法
     */
    void onStart(LifeCycleEvent event);

    /**
     * 容器暂停时回调方法
     */
    default void onPause(LifeCycleEvent event) {
    }

    /**
     * 容器内某服务master状态改变时回调方法
     * 业务实现方可自行判断具体的服务是否是master, 从而执行相应的逻辑
     */
    default void onMasterChange(LifeCycleEvent event) {
    }

    /**
     * 容器关闭
     */
    void onStop(LifeCycleEvent event);

    /**
     * 配置变化
     */
    default void onConfigChange(LifeCycleEvent event) {
    }
}
