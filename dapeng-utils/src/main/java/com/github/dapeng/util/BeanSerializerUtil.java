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

import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TCompactProtocol;

/**
 *
 * @author lihuimin
 * @date 2017/10/27
 */
public class BeanSerializerUtil {

    public static <T> byte[] serialize(T structBean, BeanSerializer<T> structSerializer) throws TException {

        byte [] byteBuf = new byte[8192];
        final TCommonTransport outputCommonTransport = new TCommonTransport(byteBuf, TCommonTransport.Type.Write);

        TCompactProtocol outputProtocol = new TCompactProtocol(outputCommonTransport);
        structSerializer.write(structBean, outputProtocol);
        return outputCommonTransport.getByteBuf();
    }

    public static <T> T deserialize(byte[] buff, BeanSerializer<T> structSerializer) throws TException {

        final TCommonTransport inputCommonTransport = new TCommonTransport(buff, TCommonTransport.Type.Read);

        TCompactProtocol intputProtocol = new TCompactProtocol(inputCommonTransport);
        T struct = structSerializer.read(intputProtocol);
        return struct;
    }




}
