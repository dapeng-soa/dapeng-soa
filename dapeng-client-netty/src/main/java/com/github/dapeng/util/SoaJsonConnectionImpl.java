package com.github.dapeng.util;

import com.github.dapeng.client.netty.SoaBaseConnection;
import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.json.JsonSerializer;
import com.github.dapeng.org.apache.thrift.TException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoaJsonConnectionImpl extends SoaBaseConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaJsonConnectionImpl.class);

    public SoaJsonConnectionImpl(String host, int port) {
        super(host, port);
    }

    @Override
    protected  <REQ> ByteBuf buildRequestBuf(String service, String version, String method, int seqid, REQ request, BeanSerializer<REQ> requestSerializer) throws SoaException {
        final ByteBuf requestBuf = PooledByteBufAllocator.DEFAULT.buffer(8192);

        SoaJsonMessageBuilder<REQ> builder = new SoaJsonMessageBuilder();

        ((JsonSerializer)requestSerializer).setRequestByteBuf(requestBuf);

        try {
            ByteBuf buf = builder.buffer(requestBuf)
                    .body(request, requestSerializer)
                    .seqid(seqid)
                    .build();
            return buf;
        } catch (TException e) {
            LOGGER.error(e.getMessage(), e);
            throw new SoaException(e);
        }
    }
}
