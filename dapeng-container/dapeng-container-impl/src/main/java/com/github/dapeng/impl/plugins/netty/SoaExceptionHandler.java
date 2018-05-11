package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.core.*;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * 描述: netty handlers 入站 统一异常处理
 *
 * @author hz.lei
 * @date 2018年05月11日 下午4:37
 */
@ChannelHandler.Sharable
public class SoaExceptionHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaExceptionHandler.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("[SoaExceptionHandler] soaHandler throw an exception" + cause.getMessage(), cause);
        final TransactionContext transactionContext = TransactionContext.Factory.currentInstance();

        if (transactionContext.getHeader() == null) {
            LOGGER.error("should not come here. soaHeader is null");
            ((TransactionContextImpl) transactionContext).setHeader(new SoaHeader());
        }
        writeErrorMessage(ctx, transactionContext, new SoaException(SoaCode.UnKnown.getCode(), cause.getMessage(), cause));
    }

    private void writeErrorMessage(ChannelHandlerContext ctx, TransactionContext transactionContext, SoaException e) {
        attachErrorInfo(transactionContext, e);
        SoaResponseWrapper responseWrapper = new SoaResponseWrapper(transactionContext,
                Optional.ofNullable(null),
                Optional.ofNullable(null));
        ctx.writeAndFlush(responseWrapper);
    }


    /**
     * attach errorInfo to transactionContext, so that we could handle it with HeadFilter
     *
     * @param transactionContext
     * @param e
     */
    private void attachErrorInfo(TransactionContext transactionContext, SoaException e) {
        SoaHeader soaHeader = transactionContext.getHeader();
        soaHeader.setRespCode(e.getCode());
        soaHeader.setRespMessage(e.getMsg());
        transactionContext.soaException(e);
    }
}
