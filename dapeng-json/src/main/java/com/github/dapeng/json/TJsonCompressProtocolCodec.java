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
import com.github.dapeng.org.apache.thrift.protocol.TCompactProtocol;
import io.netty.buffer.ByteBuf;

/**
 * dapeng-json定制的Thrift二进制压缩协议工具类,主要更改了集合类的序列化方法
 *
 * @author ever
 */
public class TJsonCompressProtocolCodec {

    /**
     * Invoked before we get the actually collection size.
     * Always assume that the collection size should take 3 bytes.
     *
     * @param elemType type of the collection Element
     * @throws TException
     */
    void writeCollectionBegin(byte elemType, ByteBuf byteBuf) throws TException {
        writeByteDirect((byte) (0xf0 | TCompactProtocol.ttypeToCompactType[elemType]), byteBuf);
        //write 3 byte with 0x0 to hold the collectionSize
        writeFixedLengthVarint32(3, byteBuf);
    }

    /**
     * Invoked after we get the actually collection size.
     *
     * @param size    collection size
     * @param byteBuf
     * @throws TException
     */
    void reWriteCollectionBegin(int size, ByteBuf byteBuf) throws TException {
        //Actually we should only change the collection length.
        byteBuf.writerIndex(byteBuf.writerIndex() + 1);
        reWriteVarint32(size, byteBuf);
    }

    /**
     * Invoked before we get the actually collection size.
     * Always assume that the collection size should take 3 bytes.
     *
     * @throws TException
     */
    void writeMapBegin(byte keyType, byte valueType, ByteBuf byteBuf) throws TException {
        /**
         * origin implementation:
         *  if (map.size == 0) {
         *     writeByteDirect(0);
         *  } else {
         *   writeVarint32(map.size);
         *   writeByteDirect(getCompactType(map.keyType) << 4 | getCompactType(map.valueType));
         *  }
         */
        writeFixedLengthVarint32(3, byteBuf);
        writeByteDirect((byte) (TCompactProtocol.ttypeToCompactType[keyType] << 4
                | TCompactProtocol.ttypeToCompactType[valueType]), byteBuf);
    }

    /**
     * Invoked after we get the actually collection size.
     * Please note that writerIndex should be updated:
     * 1. size > 0, byteBuf.writerIndex(currentWriterIndex);
     * 2. size = 0, byteBuf.writerIndex(byteBuf.writerIndex() + 1);
     *
     * @param size    collection size
     * @param byteBuf byteBuf which has reset the writerIndex to before collection
     * @throws TException
     */
    void reWriteMapBegin(int size, ByteBuf byteBuf) throws TException {
        if (size > 0) {
            reWriteVarint32(size, byteBuf);
        } else {
            // just need to update the byteBuf writerIndex by caller.
        }
    }

    private byte[] byteDirectBuffer = new byte[1];

    private void writeByteDirect(byte b, ByteBuf byteBuf) throws TException {
        byteDirectBuffer[0] = b;
        byteBuf.writeBytes(byteDirectBuffer);
    }

    /**
     * write count bytes as placeholder
     *
     * @param count   count of bytes
     * @param byteBuf
     * @throws TException
     */
    private void writeFixedLengthVarint32(int count, ByteBuf byteBuf) throws TException {
        for (int i = 0; i < count; i++) {
            writeByteDirect((byte) 0, byteBuf);
        }
    }

    /**
     * 低位在前, 高位在后,
     * n = 3, result = 0x83 0x80 0x00
     * n = 129(1000 0001), result = 0x81 0x81 0x00
     * n = 130(1000 0010), result = 0x82 0x81 0x00
     * n = 65537(1 0000 0000 0000 0001) result = 0x81 0x80 0x04
     * Write an i32 as a varint. Always results in 3 bytes on the wire.
     */
    private byte[] i32buf = new byte[3];

    private void reWriteVarint32(int n, ByteBuf byteBuf) throws TException {
        int idx = 0;
        while (true) {
            if (idx >= i32buf.length) {
                throw new TException("Too long:" + n);
            }

            if ((n & ~0x7F) == 0) {
                i32buf[idx++] = (byte) n;
                break;
            } else {
                i32buf[idx++] = (byte) ((n & 0x7F) | 0x80);
                n >>>= 7;
            }
        }

        // 如果不够位数, 那么最后一个首位需要置1,说明后续还有数字.
        if (idx < i32buf.length) {
            i32buf[idx - 1] |= 0x80;
        }

        byteBuf.writeBytes(i32buf, 0, idx);

        for (int i = idx; i < i32buf.length - 1; i++) {
            byteBuf.writeByte((byte) 0x80);
        }

        if (idx < i32buf.length) {
            byteBuf.writeByte((byte) 0x00);
        }
    }
}
