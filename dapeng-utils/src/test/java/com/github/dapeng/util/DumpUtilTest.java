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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

import static com.github.dapeng.util.DumpUtil.hexStr2bytes;

public class DumpUtilTest {
    public static void main(String[] args) {
        String hexStr = "000000d4020101000496730b00010000002c636f6d2e746f6461792e6170692e676f6f64732e736572766963652e4f70656e476f6f6473536572766963650b0002000000156c697374536b7544657461696c4279536b754e6f730b000300000005312e302e300b0004000000252f6170692f6531626664373632333231653430396365653461633062366538343139363363080005ac1200020a0007ac1e00020000c30e08000879292c580a0009ac1e00020000c30d0d00170b0b00000000001c19f882800008323035343338333929f581800004000003";
        byte[] bytes = hexStr2bytes(hexStr);
        final ByteBuf requestBuf = PooledByteBufAllocator.DEFAULT.buffer(8192);
        requestBuf.setBytes(0, bytes);


    }
}
