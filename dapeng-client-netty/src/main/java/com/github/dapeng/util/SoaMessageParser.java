package com.github.dapeng.util;

import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.SoaHeaderSerializer;
import com.github.dapeng.core.enums.CodecProtocol;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TBinaryProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TCompactProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TJSONProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;
import io.netty.buffer.ByteBuf;

import static com.github.dapeng.core.SoaProtocolConstants.ETX;
import static com.github.dapeng.core.SoaProtocolConstants.STX;
import static com.github.dapeng.core.SoaProtocolConstants.VERSION;

/**
 *
 * @author lihuimin
 * @date 2017/12/22
 */
public class SoaMessageParser<RESP> {
    private SoaHeader header;
    private RESP body;
    private BeanSerializer<RESP> bodySerializer;
    private CodecProtocol protocol = CodecProtocol.CompressedBinary;
    private int seqid;
    private TProtocol bodyProtocol;
    private TProtocol headerProtocol;

    private ByteBuf buffer;

    public SoaMessageParser(ByteBuf buffer, BeanSerializer<RESP> bodySerializer) {
        this.buffer = buffer;
        this.bodySerializer = bodySerializer;
    }

    public SoaHeader getHeader() {
        return header;
    }

    public RESP getBody() {
        return body;
    }

    public SoaMessageParser<RESP> parseHeader() throws TException {
        TSoaTransport transport = new TSoaTransport(buffer);
        TBinaryProtocol headerProtocol = new TBinaryProtocol(transport, buffer.readableBytes(),
                buffer.readableBytes(), false, true);
        this.headerProtocol = headerProtocol;
        // length(int32) stx(int8) version(int8) protocol(int8) seqid(i32) header(struct) body(struct) etx(int8)

        byte stx = headerProtocol.readByte();
        if (stx != STX) {// 通讯协议不正确
            throw new TException("通讯协议不正确(起始符)");
        }
        byte version = headerProtocol.readByte();
        if (version != VERSION) {
            throw new TException("通讯协议不正确(协议版本号)");
        }

        CodecProtocol protocol = CodecProtocol.toCodecProtocol(headerProtocol.readByte());
        switch (protocol) {
            case Binary:
                bodyProtocol = new TBinaryProtocol(transport, buffer.readableBytes(), buffer.readableBytes(),
                        false, true);
                break;
            case CompressedBinary:
                bodyProtocol = new TCompactProtocol(transport, buffer.readableBytes(), buffer.readableBytes());
                break;
            case Json:
                bodyProtocol = new TJSONProtocol(transport);
                break;
            default:
                throw new TException("通讯协议不正确(包体协议)");
        }

        this.protocol = protocol;
        this.seqid = headerProtocol.readI32();
        SoaHeader soaHeader = new SoaHeaderSerializer().read(headerProtocol);
        this.header = soaHeader;

        return this;
    }

    public SoaMessageParser<RESP> parseBody() throws TException {
        if (bodySerializer != null) {
            this.body = bodySerializer.read(bodyProtocol);
        }
        byte etx = this.headerProtocol.readByte();
        if (etx != ETX) {
            throw new TException("通讯协议不正确(结束符)");
        }
        return this;
    }

}
