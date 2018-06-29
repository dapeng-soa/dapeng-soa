package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.core.FreqControlRule;
import com.github.dapeng.core.SoaCode;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.impl.filters.freq.ShmManager;
import com.github.dapeng.registry.RegistryAgent;
import com.github.dapeng.registry.zookeeper.ServerZkAgentImpl;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final TransactionContext context = TransactionContext.Factory.currentInstance();
        RegistryAgent serverZkAgent = ServerZkAgentImpl.getInstance();
        List<FreqControlRule> freqRules = serverZkAgent.getFreqControlRule(false, context.getHeader().getServiceName());
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
            boolean flag = true;
            boolean result = true;
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
                case "userId":
                    Long userId = context.userId().orElse(0L);
                    freqKey = userId.intValue();
                    if (rule.targets != null && !rule.targets.contains(freqKey)) {
                        flag = false;
                    }
                    break;
                case "userIp":
                    freqKey = context.userIp().orElse(0);
                    if (rule.targets != null && !rule.targets.contains(freqKey)) {
                        flag = false;
                    } else {
                        freqKey = Math.abs(freqKey);
                    }
                    break;
                default:
                    freqKey = 0;
            }
            if (flag) {
                result = manager.reportAndCheck(rule, freqKey);
            }
            if (!result) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("processFreqControl,[app/ruleType/key:mincount/midcount/maxcount]:{}", manager.getCounterInfo(rule.app, rule.ruleType, freqKey));
                }
                return result;
            }
            access = result;
        }
        return access;
    }
}
