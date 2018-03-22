package com.github.dapeng.impl.filters;


import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.impl.plugins.netty.SoaResponseWrapper;
import com.github.dapeng.org.apache.thrift.TException;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class HeadFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeadFilter.class);

    @Override
    public void onEntry(FilterContext filterContext, FilterChain next) {
        try {
            if (LOGGER.isDebugEnabled()) {
                TransactionContext transactionContext = (TransactionContext) filterContext.getAttribute("context");
                LOGGER.debug(getClass().getSimpleName() + "::onEntry[seqId:" + transactionContext.getSeqid() + "]");
            }
            next.onEntry(filterContext);
        } catch (TException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void onExit(FilterContext filterContext, FilterChain prev) {
        // 第一个filter不需要调onExit
        // (这里不能通过TransactionContext.getCurrentInstance()的方式.因为已经给remove掉了.
        TransactionContext transactionContext = (TransactionContext) filterContext.getAttribute("context");
        ChannelHandlerContext channelHandlerContext = (ChannelHandlerContext) filterContext.getAttribute("channelHandlerContext");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(getClass().getSimpleName()
                    + "::onExit:[seqId:" + transactionContext.getSeqid()
                    + ", execption:" + transactionContext.getSoaException()
                    + ",\n result:" + filterContext.getAttribute("result") + "]\n");
        }

        SoaResponseWrapper responseWrapper = new SoaResponseWrapper(transactionContext,
                Optional.ofNullable(filterContext.getAttribute("result")),
                Optional.ofNullable((BeanSerializer) filterContext.getAttribute("respSerializer")));
        channelHandlerContext.writeAndFlush(responseWrapper);
    }
}
