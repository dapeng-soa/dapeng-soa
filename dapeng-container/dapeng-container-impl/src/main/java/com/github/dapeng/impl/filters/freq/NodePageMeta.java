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
package com.github.dapeng.impl.filters.freq;

/**
 * 描述: nodePage元数据
 *
 * @author hz.lei
 * @date 2018年05月14日 上午10:51
 */
public class NodePageMeta {
    public final int hash;
    public short nodes;

    public NodePageMeta(int hash, short nodes) {
        this.hash = hash;
        this.nodes = nodes;
    }

    public void increaseNode() {
        this.nodes++;
    }

    @Override
    public String toString() {
        return "hash:" + hash + ", nodes:" + nodes;
    }
}
