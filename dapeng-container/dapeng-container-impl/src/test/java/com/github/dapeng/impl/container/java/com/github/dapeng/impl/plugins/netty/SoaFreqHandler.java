package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.core.*;
import com.github.dapeng.impl.filters.ShmManager;
import com.github.dapeng.util.ExceptionUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Optional;

/**
 * 描述: 服务限流 handler
 *
 * @author hz.lei
 * @date 2018年05月08日 下午8:33
 */
@ChannelHandler.Sharable
public class SoaFreqHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final long begin = System.currentTimeMillis();
        final TransactionContext context = TransactionContext.Factory.currentInstance();
        try {

            context.callerIp();
            ShmManager manager = ShmManager.getInstance();

            ShmManager.FreqControlRule rule = new ShmManager.FreqControlRule();
            //todo fetch from zk rule

            boolean access = manager.reportAndCheck(rule, 1);

            if (access) {
                super.channelRead(ctx, msg);
            } else {
                throw new SoaException(SoaBaseCode.FreqControl, "当前服务在一定时间内请求次数过多，被限流");
            }
        } catch (Throwable ex) {
            writeErrorMessage(ctx, context, ExceptionUtil.convertToSoaException(ex));
        }
    }

    private void attachErrorInfo(TransactionContext transactionContext, SoaException e) {
        SoaHeader soaHeader = transactionContext.getHeader();
        soaHeader.setRespCode(e.getCode());
        soaHeader.setRespMessage(e.getMsg());
        transactionContext.soaException(e);
    }

    private void writeErrorMessage(ChannelHandlerContext ctx, TransactionContext transactionContext, SoaException e) {
        attachErrorInfo(transactionContext, e);

        SoaResponseWrapper responseWrapper = new SoaResponseWrapper(transactionContext,
                Optional.ofNullable(null),
                Optional.ofNullable(null));
        ctx.writeAndFlush(responseWrapper);
    }
}
