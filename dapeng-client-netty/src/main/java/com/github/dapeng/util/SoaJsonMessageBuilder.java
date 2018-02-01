package com.github.dapeng.util;

import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.enums.CodecProtocol;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TBinaryProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TCompactProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TJSONProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;
import io.netty.buffer.ByteBuf;

/**
 * @author ever
 */
public class SoaJsonMessageBuilder<REQ> extends SoaMessageBuilder<REQ> {
    @Override
    public ByteBuf build() throws TException {
        //buildHeader
        InvocationContext invocationCtx = InvocationContextImpl.Factory.getCurrentInstance();

        protocol = protocol == null ? (invocationCtx.getCodecProtocol() == null ? CodecProtocol.CompressedBinary
                : invocationCtx.getCodecProtocol()) : protocol;

        TSoaTransport transport = new TSoaTransport(buffer);
        TBinaryProtocol headerProtocol = new TBinaryProtocol(transport);
        headerProtocol.writeByte(STX);
        headerProtocol.writeByte(VERSION);
        headerProtocol.writeByte(protocol.getCode());
        headerProtocol.writeI32(seqid);

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
