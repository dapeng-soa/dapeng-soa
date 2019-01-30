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
package com.github.dapeng.util;

import com.github.dapeng.org.apache.thrift.transport.TTransportException;

/**
 * 描述: 描述: thrift 基于 byte 数组 传输层的实现 for kafka 序列化
 *
 * @author maple.lei
 * @date 2018年02月12日 下午8:54
 */
public class TKafkaTransport extends TCommonTransport {

    public TKafkaTransport(byte[] byteBuf, Type type) {
        super(byteBuf, type);
    }

    /**
     * @return
     */
    public String getEventType() {
        while (pos < byteBuf.length) {
            if (byteBuf[pos++] == (byte) 0) {
                break;
            }
        }
        byte[] subBytes = new byte[pos - 1];
        System.arraycopy(byteBuf, 0, subBytes, 0, pos - 1);
        return new String(subBytes);
    }

    /**
     * @param eventType
     * @throws TTransportException
     */
    public void setEventType(String eventType) throws TTransportException {
        write(eventType.getBytes(), pos, eventType.getBytes().length);
        byte[] bytes = new byte[1];
        bytes[0] = (byte) 0;
        write(bytes, 0, 1);
    }
}
