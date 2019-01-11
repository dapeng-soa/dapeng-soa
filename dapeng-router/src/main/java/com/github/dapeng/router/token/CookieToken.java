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
 * @author <a href=mailto:leihuazhe@gmail.com>maple</a>
 * @since 2018-10-24 4:06 PM
 */
public class CookieToken extends SimpleToken {


    private final String cookieKey;

    private final String cookieValue;

    public CookieToken(String cookieKey, String cookieValue) {
        super(COOKIE);
        this.cookieKey = cookieKey;
        this.cookieValue = cookieValue;
    }

    public String getCookieKey() {
        return cookieKey;
    }

    public String getCookieValue() {
        return cookieValue;
    }

    @Override
    public String toString() {
        return "CookieToken{ type=" + type + "cookieKey='" + cookieKey + '\'' + ", cookieValue='" + cookieValue + '\'' + '}';
    }
}