package com.github.dapeng.client.netty;

import com.github.dapeng.org.apache.thrift.transport.TTransport;
import com.github.dapeng.org.apache.thrift.transport.TTransportException;
import io.netty.buffer.ByteBuf;

/**
 * @author craneding
 * @date 16/1/12
 */
public class TSoaTransport extends TTransport {

    enum Type {
        Init, Read, Write
    }

    private int beginIndex;
    private Type type = Type.Init;
    private final ByteBuf byteBuf;

    public TSoaTransport(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;

        beginIndex = byteBuf.readerIndex();
    }

    @Override
    public boolean isOpen() {
        return byteBuf != null;
    }

    @Override
    public void open() throws TTransportException {
    }

    @Override
    public void close() {
    }

    @Override
    public int read(byte[] buf, int off, int len) throws TTransportException {
        if (len < 0) throw new IllegalArgumentException();

        if (type == Type.Init) {
            int length = byteBuf.readInt();

            if (byteBuf.readableBytes() < length)
                throw new TTransportException("ByteBuf's readable bytes is less than frame length");

            type = Type.Read;
        } else if (type == Type.Write)
            throw new TTransportException("try to read from write-only transport");

        int realLen = Math.min(byteBuf.readableBytes(), len);

        if (realLen <= 0) return realLen;

        byteBuf.readBytes(buf, off, realLen);

        return realLen;
    }

    @Override
    public void write(byte[] buf, int off, int len) throws TTransportException {
        if (type == Type.Init) {
            // placeholder for msg length
            byteBuf.writeInt(0);

            type = Type.Write;
        } else if (type == Type.Read)
            throw new TTransportException("try to write from read-only transport");

        byteBuf.writeBytes(buf, off, len);
    }

    @Override
    public void flush() throws TTransportException {
        int endIndex = byteBuf.writerIndex();

        int length = endIndex - beginIndex - Integer.BYTES;
        byteBuf.writerIndex(beginIndex).writeInt(length);
        byteBuf.writerIndex(endIndex);
    }
}
