package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.impl.plugins.monitor.config.MonitorFilterProperties;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author with struy.
 * Create by 2018/4/8 19:17
 * email :yq1724555319@gmail.com
 */

@ChannelHandler.Sharable
public class NettyConnectCounter extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyConnectCounter.class);
    private static AtomicInteger ACTIVE_CHANNEL = new AtomicInteger(0);
    private static AtomicInteger INACTIVE_CHANNEL = new AtomicInteger(0);
    private static AtomicInteger TOTAL_CHANNEL = new AtomicInteger(0);
    private ChannelHandler flowCounter = null;

    public static Integer getActiveChannel() {
        return ACTIVE_CHANNEL.get();
    }

    public static Integer getInactiveChannel() {
        return INACTIVE_CHANNEL.get();
    }

    public static Integer getTotalChannel() {
        return TOTAL_CHANNEL.get();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ChannelHandler flowCounter1 = ctx.pipeline().get(HandlerConstants.SOA_FLOW_COUNTER_HANDLER);
        ChannelHandler invokeCounter = ctx.pipeline().get(HandlerConstants.SOA_INVOKE_COUNTER_HANDLER);
        if (MonitorFilterProperties.SOA_JMX_SWITCH_MONITOR) {
            // 流量监控
            if (null != flowCounter1) {
                LOGGER.debug("流量监控已在启用状态");
            } else {
                synchronized (this) {
                    if (null == flowCounter) {
                        flowCounter = new SoaFlowCounter();
                    }
                }
                LOGGER.info("未启用流量监控，现在启用");
                ctx.pipeline().addBefore(HandlerConstants.SOA_MSG_ENCODER_HANDLER,
                        HandlerConstants.SOA_FLOW_COUNTER_HANDLER, flowCounter);
            }
            // 调用监控
            if (null != invokeCounter) {
                LOGGER.debug("调用监控已在启用状态");
            } else {
                LOGGER.info("未启用调用监控，现在启用");
                ctx.pipeline().addAfter(HandlerConstants.SOA_MSG_DECODER_HANDLER,
                        HandlerConstants.SOA_INVOKE_COUNTER_HANDLER, new SoaInvokeCounter());
            }

        } else {
            if (null != flowCounter1) {
                LOGGER.info("监控开关关闭，移除流量监控");
                ((SoaFlowCounter) flowCounter1).destory();
                ctx.pipeline().remove(HandlerConstants.SOA_FLOW_COUNTER_HANDLER);
            } else {
                LOGGER.debug("流量监控未启用");
            }

            if (null != invokeCounter) {
                LOGGER.info("监控开关关闭，移除调用监控");
                ((SoaInvokeCounter) invokeCounter).destory();
                ctx.pipeline().remove(HandlerConstants.SOA_INVOKE_COUNTER_HANDLER);
            } else {
                LOGGER.debug("调用监控未启用");
            }
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ACTIVE_CHANNEL.incrementAndGet();
        TOTAL_CHANNEL.incrementAndGet();
        LOGGER.info("==> Channel连接建立 当前连接数:[{}]", ACTIVE_CHANNEL.get());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ACTIVE_CHANNEL.decrementAndGet();
        INACTIVE_CHANNEL.incrementAndGet();
        LOGGER.info("==> Channel连接关闭 当前连接数:[{}]", ACTIVE_CHANNEL.get());
    }


}
