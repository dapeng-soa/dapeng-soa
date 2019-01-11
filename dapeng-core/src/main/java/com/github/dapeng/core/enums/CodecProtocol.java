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
package com.github.dapeng.core.enums;

/**
 * Created by lihuimin on 2017/12/21.
 */
public enum CodecProtocol {

    Binary((byte) 0), CompressedBinary((byte) 1), Json((byte) 2), Xml((byte) 3);

    private byte code;

    private CodecProtocol(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static CodecProtocol toCodecProtocol(byte code) {
        CodecProtocol[] values = CodecProtocol.values();
        for (CodecProtocol protocol : values) {
            if (protocol.getCode() == code)
                return protocol;
        }

        return null;
    }

}
