package com.github.dapeng.util;

import com.github.dapeng.core.*;
import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.SoaHeaderSerializer;
import com.github.dapeng.core.enums.CodecProtocol;
import com.github.dapeng.json.JsonSerializer;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TBinaryProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TCompactProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TJSONProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;
import io.netty.buffer.ByteBuf;

/**
 * Created by lihuimin on 2017/12/22.
 */
public class SoaMessageBuilder<T> {

    public final byte STX = 0x02;
    public final byte ETX = 0x03;
    public final byte VERSION = 1;

    private SoaHeader header;
    protected T body;
    protected BeanSerializer<T> bodySerializer;
    protected CodecProtocol protocol;
    protected int seqid;

    protected ByteBuf buffer;


    public SoaMessageBuilder<T> header(SoaHeader header) {
        this.header = header;
        return this;
    }

    public SoaMessageBuilder<T> buffer(ByteBuf buffer) {
        this.buffer = buffer;
        return this;
    }

    public SoaMessageBuilder<T> body(T body, BeanSerializer<T> serializer) {
        this.body = body;
        this.bodySerializer = serializer;
        return this;
    }

    public SoaMessageBuilder<T> protocol(CodecProtocol protocol) {
        this.protocol = protocol;
        return this;
    }

    public SoaMessageBuilder<T> seqid(int seqid) {
        this.seqid = seqid;
        return this;
    }

    public ByteBuf build() throws TException {
        InvocationContext invocationCtx = InvocationContextImpl.Factory.getCurrentInstance();

        //buildHeader
        protocol = protocol == null ? (invocationCtx.getCodecProtocol() == null ? CodecProtocol.CompressedBinary
                : invocationCtx.getCodecProtocol()) : protocol;
        TSoaTransport transport = new TSoaTransport(buffer);
        TBinaryProtocol headerProtocol = new TBinaryProtocol(transport);
        headerProtocol.writeByte(STX);
        headerProtocol.writeByte(VERSION);
        headerProtocol.writeByte(protocol.getCode());
        headerProtocol.writeI32(seqid);

        boolean isStreamProcessor = bodySerializer instanceof JsonSerializer;

        if (isStreamProcessor) {
            //如果是流式序列化器, 那么延后写入header信息
            ((JsonSerializer) bodySerializer).setRequestByteBuf(buffer);
        } else {
            new SoaHeaderSerializer().write(header, headerProtocol);
        }

        //writer body
        TProtocol bodyProtocol = null;
        switch (protocol) {
            case Binary:
                bodyProtocol = new TBinaryProtocol(transport);
                break;
            case CompressedBinary:
                bodyProtocol = new TCompactProtocol(transport);
                break;
            case Json:
                bodyProtocol = new TJSONProtocol(transport);
                break;
            default:
                throw new TException("通讯协议不正确(包体协议)");
        }
        bodySerializer.write(body, bodyProtocol);

        headerProtocol.writeByte(ETX);
        transport.flush();

        return this.buffer;
    }
}
