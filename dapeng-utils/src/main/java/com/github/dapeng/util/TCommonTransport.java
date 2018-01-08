package com.github.dapeng.util;

import com.github.dapeng.org.apache.thrift.transport.TTransport;
import com.github.dapeng.org.apache.thrift.transport.TTransportException;

import java.util.Arrays;

/**
 * @author craneding
 * @date 16/1/12
 */
public class TCommonTransport extends TTransport {

    enum Type {
        Read, Write
    }

    private Type type;
    private byte[] byteBuf;
    private int pos;    // the next read/write position

    public TCommonTransport(byte[] byteBuf, Type type) {
        this.byteBuf = byteBuf;
        this.type = type;
        this.pos = 0;
    }

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

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

        if (type == Type.Write)
            throw new TTransportException("try to read from write-only transport");

        int remain = byteBuf.length-pos;
        int amtToRead = (len > remain ? remain : len);
        if(amtToRead>0){
            System.arraycopy(byteBuf, pos, buf, off, amtToRead);
            pos+=amtToRead;
        }

        return amtToRead;
    }

    @Override
    public void write(byte[] buf, int off, int len) throws TTransportException {
        if (type == Type.Read)
            throw new TTransportException("try to write from read-only transport");
        if ((off < 0) || (off > buf.length) || (len < 0) ||
                ((off + len) - buf.length > 0)) {
            throw new IndexOutOfBoundsException();
        }
        if(byteBuf.length-pos < len){
            grow(pos + 1 + len);
        }
        System.arraycopy(buf, off, byteBuf, pos, len);
        pos += len;

    }

    public byte[] getByteBuf(){
        if(type == Type.Read) return byteBuf;
        else if(type == Type.Write) {
            byte[] array = new byte[pos];
            System.arraycopy(byteBuf, 0, array, 0, pos);
            return array;
        }
        else throw new IllegalStateException();
    }

    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = byteBuf.length;
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        byteBuf = Arrays.copyOf(byteBuf, newCapacity);
    }


    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
                Integer.MAX_VALUE :
                MAX_ARRAY_SIZE;
    }
}
