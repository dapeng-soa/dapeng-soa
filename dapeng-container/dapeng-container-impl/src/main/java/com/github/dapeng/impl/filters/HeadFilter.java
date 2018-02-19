package com.github.dapeng.impl.filters;


import com.github.dapeng.core.SoaCode;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.org.apache.thrift.TException;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.github.dapeng.util.SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE;

public class HeadFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeadFilter.class);

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) {
        try {
            next.onEntry(ctx);
        } catch (TException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void onExit(FilterContext filterContext, FilterChain prev) {
        // 第一个filter不需要调onExit(这里不能通过TransactionContext.getCurrentInstance()的方式.因为已经给remove掉了.
        TransactionContext transactionContext = (TransactionContext) filterContext.getAttribute("context");
        ChannelHandlerContext channelHandlerContext = (ChannelHandlerContext) filterContext.getAttribute("channelHandlerContext");

        channelHandlerContext.writeAndFlush(filterContext);
    }
}
