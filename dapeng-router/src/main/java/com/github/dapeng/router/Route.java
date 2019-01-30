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

import com.github.dapeng.router.condition.Condition;

import java.util.List;

/**
 * 描述: 代表 每一条路由表达式解析出来的内容
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:45
 */
public class Route {

    private final Condition left;
    private final List<CommonThen> thenRouteDests;

    public Route(Condition left, List<CommonThen> thenRouteDests) {
        this.left = left;
        this.thenRouteDests = thenRouteDests;
    }

    public Condition getLeft() {
        return left;
    }

    public List<CommonThen> getThenRouteDests() {
        return thenRouteDests;
    }

    @Override
    public String toString() {
        return "Route{" +
                "left=" + left +
                ", thenRouteDests=" + thenRouteDests +
                '}';
    }
}
