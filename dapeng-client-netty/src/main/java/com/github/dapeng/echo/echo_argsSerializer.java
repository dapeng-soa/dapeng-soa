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
package com.github.dapeng.echo;

import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;

public class echo_argsSerializer implements BeanSerializer<echo_args> {

    @Override
    public echo_args read(TProtocol iprot) throws TException {

        echo_args bean =new echo_args();
        com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
        iprot.readStructBegin();

        while (true) {
            schemeField = iprot.readFieldBegin();
            if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
                break;
            }
            switch (schemeField.id) {
                default:
                    com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);

            }
            iprot.readFieldEnd();
        }
        iprot.readStructEnd();

        validate(bean);
        return bean;
    }

    @Override
    public void write(echo_args bean, TProtocol oprot) throws TException {

        validate(bean);
        oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("echo_args"));
        oprot.writeFieldStop();
        oprot.writeStructEnd();
    }

    @Override
    public void validate(echo_args bean) throws TException {}

    @Override
    public String toString(echo_args bean) {
        return bean == null ? "null" : bean.toString();
    }
}