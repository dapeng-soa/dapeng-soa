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
package com.github.dapeng.router.token;

/**
 * 描述: id 词法解析单元，作为一个被匹配的类型的名称
 * etc.
 * <p>
 * method match
 * version match
 * customerId match
 * method version customerId 等诸如此类的都称为ID
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:03
 */
public class IdToken extends SimpleToken {
    public final String name;

    public IdToken(String name) {
        super(ID);
        this.name = name;
    }

    @Override
    public String toString() {
        return "IdToken[type:" + type + ", name:" + name + "]";
    }
}
