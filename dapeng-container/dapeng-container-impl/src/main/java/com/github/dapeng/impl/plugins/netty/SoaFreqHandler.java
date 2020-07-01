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

import com.github.dapeng.core.*;
import com.github.dapeng.impl.filters.freq.ShmManager;
import com.github.dapeng.registry.RegistryAgent;
import com.github.dapeng.registry.zookeeper.ServerZkAgentImpl;
import com.github.dapeng.registry.zookeeper.ZkServiceInfo;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述: 服务限流 handler
 *
 * @author hz.lei
 * @date 2018年05月08日 下午8:33
 */
@ChannelHandler.Sharable
public class SoaFreqHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaFreqHandler.class.getName());
    private static ShmManager manager = ShmManager.getInstance();
    private static RegistryAgent serverZkAgent = ServerZkAgentImpl.getInstance();


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        boolean freqResult = true;

        try {
            freqResult = processServiceFreqControl();
        } catch (Throwable e) {
            LOGGER.error(SoaCode.FreqControlError.toString(), e);
        } finally {
            if (freqResult) {
                ctx.fireChannelRead(msg);
            } else {
                throw new SoaException(SoaCode.FreqLimited, "当前服务在一定时间内请求次数过多，被限流");
            }
        }
    }


    /**
     * 限流逻辑，判断当前请求是否被限流，返回 true false
     *
     * @return result boolean
     */
    private boolean processServiceFreqControl() {
        final TransactionContext context = TransactionContext.Factory.currentInstance();
        final ZkServiceInfo serviceInfo = serverZkAgent.getZkServiceInfo(false, context.getHeader().getServiceName());

        if (serviceInfo == null || serviceInfo.freqControl() == null) {
            return true;
        }

        final ServiceFreqControl freqControl = serviceInfo.freqControl();
        String method = context.getHeader().getMethodName();
        if (freqControl.globalRules.isEmpty() && !freqControl.rules4methods.containsKey(method)) {
            return true;
        }
        if (freqControl.rules4methods.containsKey(method)) {
            for (FreqControlRule rule : freqControl.rules4methods.get(method)) {
                if (!processFreqControl(rule, context)) {
                    return false;
                }
            }
        } else {
            for (FreqControlRule rule : freqControl.globalRules) {
                if (!processFreqControl(rule, context)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean processFreqControl(FreqControlRule rule, TransactionContext context) {
        // 是否需要执行限流规则(对于指定ip,id等的规则来说)
        boolean shouldProcess = true;
        int freqKey;
        switch (rule.ruleType) {
            case "all":
                freqKey = 0;
                break;
            case "callerIp":
                int callerIp = context.callerIp().orElse(0);
                freqKey = callerIp;
                break;
            case "callerMid":
                String callerMid = context.callerMid().orElse("0");
                freqKey = callerMid.hashCode();
                break;
            case "customerId":
                freqKey = context.customerId().orElse(0);
                if (rule.targets != null && !rule.targets.contains(freqKey)) {
                    // 当前请求不属于限流目标范围内, 不需要执行限流规则
                    shouldProcess = false;
                }
                break;
            case "userIp":
                freqKey = context.userIp().orElse(0);
                if (rule.targets != null && !rule.targets.contains(freqKey)) {
                    // 当前请求不属于限流目标范围内, 不需要执行限流规则
                    shouldProcess = false;
                }
                break;
            default:
                freqKey = 0;
        }

        // 限流结果
        boolean result = true;
        if (shouldProcess) {
            result = manager.reportAndCheck(rule, freqKey);
        }
        if (!result) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("processFreqControl,[app/ruleType/key:mincount/midcount/maxcount]:{}", manager.getCounterInfo(rule.app, rule.ruleType, freqKey));
            }
        }

        return result;
    }
}
