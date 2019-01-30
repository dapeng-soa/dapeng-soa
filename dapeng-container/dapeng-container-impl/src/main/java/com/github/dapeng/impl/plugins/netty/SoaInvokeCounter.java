/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.impl.plugins.monitor.ServerCounterContainer;
import com.github.dapeng.impl.plugins.monitor.ServiceBasicInfo;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 统计服务调用次数和耗时，包括成功失败的次数
 *
 * @author with struy.
 * Create by 2018/3/8 15:37
 * email :yq1724555319@gmail.com
 */
@ChannelHandler.Sharable
public class SoaInvokeCounter extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaInvokeCounter.class);
    private static final ServerCounterContainer counterContainer = ServerCounterContainer.getInstance();
    private static final String SUCCESS_CODE = SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            TransactionContext transactionContext = TransactionContext.Factory.currentInstance();
            int seqId = transactionContext.seqId();
            transactionContext.setAttribute("invokeBeginTime", System.currentTimeMillis());

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(getClass().getSimpleName() + "::read response[seqId=" + seqId + "]");
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        try {
            // 异步返回不能从通过 TransactionContext.Factory.currentInstance() 去拿context
            SoaResponseWrapper wrapper = (SoaResponseWrapper) msg;
            TransactionContext context = wrapper.transactionContext;

            handleInvocationInfo(context);
        } catch (Throwable ex) {
            LOGGER.error(ex.getMessage(), ex);
        } finally {
            ctx.write(msg, promise);
        }
    }

    private void handleInvocationInfo(TransactionContext context) {
        SoaHeader soaHeader = context.getHeader();

        ServiceBasicInfo basicInfo = new ServiceBasicInfo(soaHeader.getServiceName(),
                soaHeader.getMethodName(), soaHeader.getVersionName());

        long invokeBeginTime = (Long) context.getAttribute("invokeBeginTime");

        long cost = System.currentTimeMillis() - invokeBeginTime;

        counterContainer.addServiceElapseInfo(basicInfo, cost);

        if (soaHeader.getRespCode().isPresent() && SUCCESS_CODE.equals(soaHeader.getRespCode().get())) {
            counterContainer.increaseServiceCall(basicInfo, true);
        } else {
            counterContainer.increaseServiceCall(basicInfo, false);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(getClass().getSimpleName() + "::write response[seqId=" + context.seqId() + ", respCode=" + soaHeader.getRespCode().get()
                    + "] cost:" + cost + "ms");
        }
    }
}
