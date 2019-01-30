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

/**
 * @author zxwang
 */
public interface JsonCallback {

    /**
     * Called at start of Json object, typical handle the '{'
     *
     * @throws TException
     */
    void onStartObject() throws TException;

    /**
     * Called at end of Json object, typical handle the '}'
     *
     * @throws TException
     */
    void onEndObject() throws TException;

    /**
     * Called at start of Json array, typical handle the '['
     *
     * @throws TException
     */
    void onStartArray() throws TException;

    /**
     * Called at end of Json array, typical handle the ']'
     *
     * @throws TException
     */
    void onEndArray() throws TException;

    /**
     * Called at start of Json field, such as: "orderId":130
     *
     * @param name name of the filed, as for the example above, that is "orderId"
     * @throws TException
     */
    void onStartField(String name) throws TException;

    /**
     * called begin an array element
     * @param index
     * @throws TException
     */
    void onStartField(int index) throws TException;

    /**
     * Called at end of Json field
     *
     * @throws TException
     */
    void onEndField() throws TException;

    /**
     * Called when a boolean value is met,
     * as to given field: <pre>"expired":false</pre>
     * First onStartField("expired") is called, followed by a call onBoolean(false) and a call onEndField()
     *
     * @param value
     * @throws TException
     */
    void onBoolean(boolean value) throws TException;

    /**
     * Called when a double value is met.
     *
     * @param value
     * @throws TException
     */
    void onNumber(double value) throws TException;

    /**
     * Called when a long/int value is met.
     *
     * @param value
     * @throws TException
     */
    void onNumber(long value) throws TException;

    /**
     * Called when a null value is met.
     * Such as: "subItemId":null
     *
     * @throws TException
     */
    void onNull() throws TException;

    /**
     * Called when a String value is met.
     * Such as: "name": "Walt"
     *
     * @param value
     * @throws TException
     */
    void onString(String value) throws TException;
}
