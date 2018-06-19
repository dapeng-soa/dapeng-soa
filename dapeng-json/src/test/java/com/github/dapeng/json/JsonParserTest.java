package com.github.dapeng.json;

import com.github.dapeng.org.apache.thrift.TException;

import java.util.Arrays;
import java.util.List;

public class JsonParserTest {
    public static void main(String[] args) throws TException {
        String json = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3], \"d\":10.2," +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";

        String json1 = "{ a\": 10, \"b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";

        String json2 = "{ \"a\": 10d, \"b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";

        String json3 = "{ \"a\": 10, b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";

        String json4 = "{ \"a\": 10, \"b\": true, \n\"c: [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";
        String json5 = "{ \"a\": 10, \"b\": true, \n\"c\": 1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";
        String json6 = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3] " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";
        String json7 = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3, " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";
        String json8 = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":} }";
        String json9 = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{ }";
        String json10 = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{}, }";

        List<String> errorJsons = Arrays.asList(json1, json2, json3, json4, json5, json6, json7, json8, json9, json10);
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
//        errorJsons.forEach(errorJson -> {
//            JsonParser myParser = new JsonParser(errorJson, callback);
//            try {
//                myParser.value();
//            } catch (ParsingException e) {
//                e.printStackTrace();
//            }
//            System.out.println("finished=====");
//        });
    }
}
