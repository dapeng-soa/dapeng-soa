package com.github.dapeng.client.netty;

import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.helper.SoaHeaderHelper;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.util.DumpUtil;
import com.github.dapeng.util.SoaMessageBuilder;
import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoaConnectionImpl extends SoaBaseConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaConnectionImpl.class);

    public SoaConnectionImpl(String host, int port, SubPool parent) {
        super(host, port, parent);
    }

    @Override
    protected <REQ> ByteBuf buildRequestBuf(String service, String version, String method, int seqid, REQ request, BeanSerializer<REQ> requestSerializer) throws SoaException {
        AbstractByteBufAllocator allocator =
                SoaSystemEnvProperties.SOA_POOLED_BYTEBUF ?
                        PooledByteBufAllocator.DEFAULT : UnpooledByteBufAllocator.DEFAULT;
        final ByteBuf requestBuf = allocator.buffer(8192);

        SoaMessageBuilder<REQ> builder = new SoaMessageBuilder<>();

        try {
            SoaHeader header = SoaHeaderHelper.buildHeader(service, version, method);

            ByteBuf buf = builder.buffer(requestBuf)
                    .header(header)
                    .body(request, requestSerializer)
                    .seqid(seqid)
                    .build();
            return buf;
        } catch (TException e) {
            LOGGER.error(e.getMessage(), e);
            requestBuf.release();
            throw new SoaException(e);
        }
    }
}
