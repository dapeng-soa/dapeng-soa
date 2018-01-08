package com.github.dapeng.client.json;

import com.github.dapeng.org.apache.thrift.TException;

public interface JsonCallback {

    void onStartObject() throws TException;
    void onEndObject() throws TException;

    void onStartArray() throws TException;
    void onEndArray() throws TException;

    void onStartField(String name) throws TException;
    void onEndField() throws TException;

    void onBoolean(boolean value) throws TException;
    void onNumber(double value) throws TException;
    void onNull() throws TException;

    void onString(String value) throws TException;
}
