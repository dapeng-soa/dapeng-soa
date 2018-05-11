package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Optional;

/**
 * 描述: netty handlers 入站 统一异常处理
 *
 * @author hz.lei
 * @date 2018年05月11日 下午4:37
 */
public class SoaExceptionHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
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
