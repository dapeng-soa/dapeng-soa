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
package com.github.dapeng.json;

import com.github.dapeng.org.apache.thrift.TException;

import java.util.Arrays;
import java.util.List;

public class JsonParserTest {
    public static void main(String[] args) throws TException {
        String json = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3], \"d\":10.2," +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";

        String errJson1 = "{ a\": 10, \"b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";

        String errJson2 = "{ \"a\": 10d, \"b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";

        String errJson3 = "{ \"a\": 10, b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";

        String errJson4 = "{ \"a\": 10, \"b\": true, \n\"c: [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";
        String errJson5 = "{ \"a\": 10, \"b\": true, \n\"c\": 1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";
        String errJson6 = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3] " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";
        String errJson7 = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3, " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";
        String errJson8 = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":} }";
        String errJson9 = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{ }";
        String erJson10 = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{}, }";

        List<String> errorJsons = Arrays.asList(errJson1, errJson2, errJson3, errJson4, errJson5, errJson6, errJson7, errJson8, errJson9, erJson10);
        JsonCallback callback = new JsonCallback() {
            @Override
            public void onStartObject() {
                System.out.println("onStartObject");
            }

            @Override
            public void onEndObject() {
                System.out.println("onEndObject");
            }

            @Override
            public void onStartArray() {
                System.out.println("onStartArray");
            }

            @Override
            public void onEndArray() {
                System.out.println("onEndArray");
            }

            @Override
            public void onStartField(String name) {
                System.out.println("onStartField:" + name);
            }

            @Override
            public void onStartField(int index) {
                System.out.println("onStartField:" + index);
            }

            @Override
            public void onEndField() {
                System.out.println("onEndField");
            }

            @Override
            public void onBoolean(boolean value) {
                System.out.println("onBoolean:" + value);
            }

            @Override
            public void onNumber(double value) {
                System.out.println("onNumber:" + value);
            }
            @Override
            public void onNumber(long value) {
                System.out.println("onNumber:" + value);
            }

            @Override
            public void onNull() {
                System.out.println("onNull");
            }

            @Override
            public void onString(String value) {
                System.out.println("onString:" + value);
            }
        };

        JsonParser parser = new JsonParser(json, callback);
        System.out.println(json);
        parser.value();
        System.out.println("finished=====");

        errorJsons.forEach(errorJson -> {
            JsonParser myParser = new JsonParser(errorJson, callback);
            try {
                myParser.value();
                throw new RuntimeException("Should not be here");
            }
            catch(JsonParser.ParsingException ex) {
                System.out.println("find a exception " + ex.getMessage());
            }
        });
        System.out.println("finished=====");
    }
}
