package com.github.dapeng.impl.plugins.netty;


import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.SoaHeaderSerializer;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.enums.CodecProtocol;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TBinaryProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TCompactProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TJSONProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.dapeng.core.enums.CodecProtocol.*;

/**
 * Created by lihuimin on 2017/12/8.
 */
public class SoaMessageProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaMessageProcessor.class);

    private final byte STX = 0x02;
    private final byte ETX = 0x03;
    private final byte VERSION = 1;

    private TProtocol headerProtocol;
    private TProtocol contentProtocol;


    public TSoaTransport transport;

    public TProtocol getHeaderProtocol() {
        return headerProtocol;
    }

    public void setHeaderProtocol(TProtocol headerProtocol) {
        this.headerProtocol = headerProtocol;
    }

    public TProtocol getContentProtocol() {
        return contentProtocol;
    }

    public void setContentProtocol(TProtocol contentProtocol) {
        this.contentProtocol = contentProtocol;
    }

    public TSoaTransport getTransport() {
        return transport;
    }

    public void setTransport(TSoaTransport transport) {
        this.transport = transport;
    }

    public SoaMessageProcessor(TSoaTransport transport) {
        this.transport = transport;
    }

    public void writeHeader(TransactionContext context) throws TException {

        headerProtocol = new TBinaryProtocol(transport);

        headerProtocol.writeByte(STX);
        headerProtocol.writeByte(VERSION);
        headerProtocol.writeByte(context.getCodecProtocol().getCode());
        headerProtocol.writeI32(context.getSeqid());

        switch (context.getCodecProtocol()) {
            case Binary:
                contentProtocol = new TBinaryProtocol(transport);
                break;
            case CompressedBinary:
                contentProtocol = new TCompactProtocol(transport);
                break;
            case Json:
                contentProtocol = new TJSONProtocol(transport);
                break;
            case Xml:
                contentProtocol = null;
                break;
            default:
                LOGGER.error("Unknow Protocol", new Throwable());
        }

        new SoaHeaderSerializer().write(context.getHeader(), headerProtocol);
    }

    public <RESP>void writeBody(BeanSerializer<RESP> respSerializer, RESP result ) throws TException {
        respSerializer.write(result,contentProtocol);
    }

    public SoaHeader parseSoaMessage(TransactionContext context) throws TException{

        if (headerProtocol == null) {
            headerProtocol = new TBinaryProtocol(getTransport());
        }

        // length(int32) stx(int8) version(int8) protocol(int8) seqid(i32) header(struct) body(struct) etx(int8)

        byte stx = headerProtocol.readByte();
        if (stx != STX) {// 通讯协议不正确
            throw new TException("通讯协议不正确(起始符)");
        }

        // version
        byte version = headerProtocol.readByte();
        if (version!=VERSION) {
            throw new TException("通讯协议不正确(协议版本号)");
        }

        byte protocol = headerProtocol.readByte();
        context.setCodecProtocol(toCodecProtocol(protocol));
        switch (context.getCodecProtocol()) {
            case Binary:
                contentProtocol = new TBinaryProtocol(getTransport());
                break;
            case CompressedBinary:
                contentProtocol = new TCompactProtocol(getTransport());
                break;
            case Json:
                contentProtocol = new TJSONProtocol(getTransport());
                break;
            case Xml:
                //realContentProtocol = null;
                throw new TException("通讯协议不正确(包体协议)");
            default:
                throw new TException("通讯协议不正确(包体协议)");
        }

        context.setSeqid(headerProtocol.readI32());
        return new SoaHeaderSerializer().read( headerProtocol);
    }

    public void writeMessageEnd() throws TException {
        contentProtocol.writeMessageEnd();

        headerProtocol.writeByte(ETX);
    }


}
