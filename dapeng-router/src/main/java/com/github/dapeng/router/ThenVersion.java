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

/**
 * * 路由匹配成功后，导向的具体 服务ip 实体类
 * * etc.  method match 'setFoo' => v'1.0.0'
 * * 后面的v'1.0.0' 包装为 ThenVersion实体
 *
 * @author huyj
 * @Created 2019-01-17 10:18
 */
public class ThenVersion extends CommonThen {
    public final String version;

    public ThenVersion(int routeType, boolean not, String version) {
        super(routeType, not);
        this.version = version;
    }
}
