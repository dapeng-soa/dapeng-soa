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
package com.github.dapeng.router.condition;

import com.github.dapeng.router.pattern.Pattern;

import java.util.List;

/**
 * 描述: 路由表达式的每一个匹配规则，即为一个 Matcher
 * etc. method match 'setFoo' ; version match '1.0.0' =>
 * <p>
 * 这里就会有两个matcher
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:39
 */
public class Matcher {

    private String id;
    private List<Pattern> patterns;

    public Matcher(String id, List<Pattern> patterns) {
        this.id = id;
        this.patterns = patterns;
    }

    public String getId() {
        return id;
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    @Override
    public String toString() {
        return "Matcher{" + "id='" + id + '\'' + ", patterns=" + patterns + '}';
    }
}
