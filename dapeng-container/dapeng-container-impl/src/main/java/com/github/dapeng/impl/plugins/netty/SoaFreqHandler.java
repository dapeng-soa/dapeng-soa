package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.core.*;
import com.github.dapeng.impl.filters.ShmManager;
import com.github.dapeng.registry.RegistryAgent;
import com.github.dapeng.registry.RegistryAgentProxy;
import com.github.dapeng.util.ExceptionUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.List;
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
        final TransactionContext context = TransactionContext.Factory.currentInstance();
        ShmManager manager = ShmManager.getInstance();
        RegistryAgent registryAgent = RegistryAgentProxy.getCurrentInstance(RegistryAgentProxy.Type.Server);
        List<FreqControlRule> freqRules = registryAgent.getFreqControlRule(false, context.getHeader().getServiceName());
        boolean freqResult = processFreqControl(freqRules, manager, context);

        if (freqResult) {
            ctx.fireChannelRead(msg);
        } else {
            throw new SoaException(SoaCode.FreqControl, "当前服务在一定时间内请求次数过多，被限流");
        }
    }


    /**
     * 限流逻辑，判断当前请求是否被限流，返回 true false
     *
     * @param freqRules 限流规则 from zookeeper
     * @param manager   限流 manager
     * @param context   服务端上下文
     * @return result boolean
     */
    private boolean processFreqControl(List<FreqControlRule> freqRules, ShmManager manager, TransactionContext context) {
        if (freqRules.isEmpty()) {
            return true;
        }
        boolean access = false;
        for (FreqControlRule rule : freqRules) {
            int freqKey;
            switch (rule.ruleType) {
                case "all":
                    freqKey = -1;
                    break;
                case "callerIp":
                    String callerIp = context.callerIp().orElse("-1");
                    freqKey = callerIp.hashCode();
                    break;
                case "callerMid":
                    String callerMid = context.callerMid().orElse("-1");
                    freqKey = callerMid.hashCode();
                    break;
                case "userId":
                    Long userId = context.userId().orElse(-1L);
                    freqKey = Integer.valueOf(userId.intValue());
                    break;
                case "userIp":
                    freqKey = context.userIp().orElse(-1);
                    break;
                default:
                    freqKey = -1;
                    break;
            }

            boolean result = manager.reportAndCheck(rule, freqKey);

            if (!result) {
                return result;
            }
            access = result;
        }
        return access;
    }
}
