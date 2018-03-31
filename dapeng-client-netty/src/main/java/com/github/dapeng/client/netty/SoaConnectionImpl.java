package com.github.dapeng.client.netty;

import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.util.SoaMessageBuilder;
import com.github.dapeng.util.SoaSystemEnvProperties;
import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoaConnectionImpl extends SoaBaseConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaConnectionImpl.class);

    public SoaConnectionImpl(String host, int port) {
        super(host, port);
    }

    @Override
    protected  <REQ> ByteBuf buildRequestBuf(String service, String version, String method, int seqid, REQ request, BeanSerializer<REQ> requestSerializer) throws SoaException {
        AbstractByteBufAllocator allocator =
                SoaSystemEnvProperties.SOA_POOLED_BYTEBUF ?
                        PooledByteBufAllocator.DEFAULT : UnpooledByteBufAllocator.DEFAULT;
        final ByteBuf requestBuf = allocator.buffer(8192);

        SoaMessageBuilder<REQ> builder = new SoaMessageBuilder<>();
//        if(requestSerializer instanceof SoaMessageBuilderAware) {
//            requestSerializer.setMessageBuilder(builder);
//        }

        SoaHeader header = buildHeader(service, version, method);
        try {
            ByteBuf buf = builder.buffer(requestBuf)
                    .header(header)
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
