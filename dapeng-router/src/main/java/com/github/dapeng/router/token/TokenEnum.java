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
 * @author maple 2018.09.29 7:43 PM
 */
public enum TokenEnum {
    /**
     * 回车换行符
     */
    EOL(1, "回车换行符"),

    /**
     * =>
     */
    THEN(2, "=>"),

    /**
     * otherwise
     */
    OTHERWISE(3, "otherwise"),

    /**
     * match 关键字
     */
    MATCH(4, "match"),

    /**
     * not in    ~
     */
    NOT(5, "~"),

    /**
     * string类型: eg. A B C D
     */
    STRING(6, "String"),

    /**
     * regex 正则
     */
    REGEXP(7, "REGEXP"),

    /**
     * range 范围
     */
    RANGE(8, "range"),

    /**
     * 数字类型
     */
    NUMBER(9, "数字类型"),

    /**
     * ip "192.168.1.1"
     */
    IP(10, "IP"),

    /**
     * k v 存储
     */
    KV(11, "KV"),

    /**
     * mode  %
     */
    MODE(12, "MODE"),

    /**
     * 匹配类型id  like  method ，version ，service
     */
    ID(13, "ID"),

    /**
     * 文件结束符
     */
    EOF(-1, "文件结束符"),

    /**
     * 分号 < ；>  区分多个 Matcher
     */
    SEMI_COLON(14, "分号"),

    /**
     * 逗号: <,>  区分多个 pattern
     */
    COMMA(15, "逗号"),

    /**
     * cookie <c> 区分多个 cookies
     */
    COOKIE(16, "COOKIES"),

    /**
     * 未知
     */
    UNKNOWN(16, "未知Token");


    private int type;
    private String name;

    TokenEnum(int type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String toString() {
        return "(" + "" + type +
                ",'" + name + '\'' +
                ')';
    }

    public static TokenEnum findById(int type) {
        switch (type) {
            case -1:
                return EOF;
            case 1:
                return EOL;
            case 2:
                return THEN;
            case 3:
                return OTHERWISE;
            case 4:
                return MATCH;
            case 5:
                return NOT;
            case 6:
                return STRING;
            case 7:
                return REGEXP;
            case 8:
                return RANGE;
            case 9:
                return NUMBER;
            case 10:
                return IP;
            case 11:
                return KV;
            case 12:
                return MODE;
            case 13:
                return ID;
            case 14:
                return SEMI_COLON;
            case 15:
                return COMMA;
            case 16:
                return COOKIE;
            default:
                return UNKNOWN;
        }
    }
}
