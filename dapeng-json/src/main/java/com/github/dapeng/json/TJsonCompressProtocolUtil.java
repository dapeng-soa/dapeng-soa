package com.github.dapeng.json;

import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TCompactProtocol;
import io.netty.buffer.ByteBuf;

/**
 * dapeng-json定制的Thrift二进制压缩协议工具类,主要更改了集合类的序列化方法
 *
 * @author ever
 */
public class TJsonCompressProtocolUtil {

    /**
     * Invoked before we get the actually collection size.
     * Always assume that the collection size should take 3 bytes.
     *
     * @param elemType type of the collection Element
     * @throws TException
     */
    public static void writeCollectionBegin(byte elemType, ByteBuf byteBuf) throws TException {
        writeByteDirect((byte) (0xf0 | TCompactProtocol.ttypeToCompactType[elemType]), byteBuf);
        //write 3 byte with 0x0 to hold the collectionSize
        writeFixedLengthVarint32(3, byteBuf);
    }

    /**
     * Invoked after we get the actually collection size.
     *
     * @param elemType
     * @param size     collection size
     * @param byteBuf
     * @throws TException
     */
    public static void reWriteCollectionBegin(byte elemType, int size, ByteBuf byteBuf) throws TException {
//        writeByteDirect((byte) (0xf0 | TCompactProtocol.ttypeToCompactType[elemType]), byteBuf);
        byteBuf.writerIndex(byteBuf.writerIndex()+1);
        reWriteVarint32(size, byteBuf);
    }

    /**
     * Invoked before we get the actually collection size.
     * Always assume that the collection size should take 3 bytes.
     *
     * @throws TException
     */
    public static void writeMapBegin(byte keyType, byte valueType, ByteBuf byteBuf) throws TException {
//        if (map.size == 0) {
//            writeByteDirect(0);
//        } else {
//            writeVarint32(map.size);
//            writeByteDirect(getCompactType(map.keyType) << 4 | getCompactType(map.valueType));
//        }

        writeFixedLengthVarint32(3, byteBuf);
        writeByteDirect((byte) (TCompactProtocol.ttypeToCompactType[keyType] << 4
                | TCompactProtocol.ttypeToCompactType[valueType]), byteBuf);
    }

    /**
     * Invoked after we get the actually collection size.
     * Please note that writerIndex should be updated:
     * 1. size > 0, byteBuf.writerIndex(currentWriterIndex);
     * 2. size = 0, byteBuf.writerIndex(byteBuf.writerIndex() + 1);
     * @param size               collection size
     * @param byteBuf            byteBuf which has reset the writerIndex to before collection
     * @throws TException
     */
    public static void reWriteMapBegin(int size, ByteBuf byteBuf) throws TException {
        if (size > 0) {
            reWriteVarint32(size, byteBuf);
        }
    }

    private static byte[] byteDirectBuffer = new byte[1];

    private static void writeByteDirect(byte b, ByteBuf byteBuf) throws TException {
        byteDirectBuffer[0] = b;
        byteBuf.writeBytes(byteDirectBuffer);
    }

    /**
     * write count bytes as placeholder
     *
     * @param count count of bytes
     * @param byteBuf
     * @throws TException
     */
    private static void writeFixedLengthVarint32(int count, ByteBuf byteBuf) throws TException {
        for (int i = 0; i < count; i++) writeByteDirect((byte) 0, byteBuf);
    }

    /**
     * Write an i32 as a varint. Always results in 3 bytes on the wire.
     */
    private static byte[] i32buf = new byte[3];

    private static void reWriteVarint32(int n, ByteBuf byteBuf) throws TException {
        int idx = 0;
        while (true) {
            if (idx >= i32buf.length) throw new TException("Too long:" + n);

            if ((n & ~0x7F) == 0) {
                i32buf[idx++] = (byte) n;
                break;
            } else {
                i32buf[idx++] = (byte) ((n & 0x7F) | 0x80);
                n >>>= 7;
            }
        }

        for (int i = idx; i < i32buf.length; i++) {
            byteBuf.writeByte((byte) 0x80);
        }
        byteBuf.writeBytes(i32buf, 0, idx);
    }
}
