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
 * 描述: 词法解析 最小单元 token
 *
 * @author hz.lei
 * @date 2018年04月13日 下午8:51
 */
public interface Token {
    /**
     * 回车换行符
     */
    int EOL = 1;

    /**
     * =>
     */
    int THEN = 2;

    /**
     * otherwise
     */
    int OTHERWISE = 3;

    /**
     * match 关键字
     */
    int MATCH = 4;

    /**
     * not in    ~
     */
    int NOT = 5;

    /**
     * string类型: eg. A B C D
     */
    int STRING = 6;

    /**
     * regex 正则
     */
    int REGEXP = 7;

    /**
     * range 范围
     */
    int RANGE = 8;

    /**
     * 数字类型
     */
    int NUMBER = 9;

    /**
     * ip "192.168.1.1"
     */
    int IP = 10;

    /**
     * k v 存储
     */
    int KV = 11;

    /**
     * mode  %
     */
    int MODE = 12;
    /**
     * 匹配类型id  like  method ，version ，service
     */
    int ID = 13;

    /**
     * 文件结束符
     */
    int EOF = -1;

    /**
     * 分号 < ；>  区分多个 Matcher
     */
    int SEMI_COLON = 14;

    /**
     * 逗号: <,>  区分多个 pattern
     */
    int COMMA = 15;

    /**
     * cookie 设置 cookie值
     */
    int COOKIE = 16;

    /**
     * token 类型 type
     *
     * @return
     */
    int type();


    /**
     * v "1.0.2"
     */
    int VERSION = 17;
}
