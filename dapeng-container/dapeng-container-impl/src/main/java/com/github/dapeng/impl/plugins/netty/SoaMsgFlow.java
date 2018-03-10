package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.basic.api.counter.CounterServiceClient;
import com.github.dapeng.basic.api.counter.service.CounterService;
import com.github.dapeng.impl.plugins.monitor.config.MonitorFilterProperties;
import com.github.dapeng.util.SoaSystemEnvProperties;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author with struy.
 * Create by 2018/3/6 20:42
 * email :yq1724555319@gmail.com
 * 单独的流量统计？
 * 只统计流量的出入，单独的上送任务？
 * 单独的数据存储表？
 *
 */
//@ChannelHandler.Sharable
public class SoaMsgFlow extends ChannelDuplexHandler {
    private Logger LOGGER = LoggerFactory.getLogger(getClass());
    private static final int PERIOD = MonitorFilterProperties.SOA_MONITOR_SERVICE_PROCESS_PERIOD;
    private final String DATA_BASE = MonitorFilterProperties.SOA_MONITOR_INFLUXDB_DATABASE;
    private final String SUCCESS_CODE = SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE;
    private final String SERVER_IP = SoaSystemEnvProperties.SOA_CONTAINER_IP;
    private final Integer SERVER_PORT = SoaSystemEnvProperties.SOA_CONTAINER_PORT;
    private final CounterService SERVICE_CLIENT = new CounterServiceClient();


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Long requestFlow = (long) ((ByteBuf) msg).readableBytes();
        System.out.println("requestFlow========> "+requestFlow);
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Long responseFlow = (long) ((ByteBuf) msg).readableBytes();
        System.out.println("responseFlow========> "+responseFlow);
        ctx.write(msg, promise);
    }


    static {

    }

    public void destroy(){

    }

}
