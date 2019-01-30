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
package com.github.dapeng.core.enums;

/**
 * 服务健康度枚举
 *
 * @author Ever
 * @date 2018/07/25
 */
public enum ServiceHealthStatus {
    /**
     * 良好
     */
    Green,
    /**
     * 一般, 业务还能进行, 但要引起注意
     */
    Yellow,
    /**
     * 业务主流程受阻
     */
    Red
}
