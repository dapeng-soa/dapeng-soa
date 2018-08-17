package com.github.dapeng.core;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 描述: 服务限流规则
 *
 * @author Ever
 * @date 2018年08月13日 下午10:28
 */
public class ServiceFreqControl {

    public final String serviceName;
    public final List<FreqControlRule> globalRules;
    public final Map<String, List<FreqControlRule>> rules4methods;

    public ServiceFreqControl(String serviceName, List<FreqControlRule> gobalRule, Map<String, List<FreqControlRule>> rules4methods) {
        this.serviceName = serviceName;
        this.globalRules = gobalRule;
        this.rules4methods = rules4methods;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("service:" + serviceName + ", ");
        sb.append(" globalRules:[").append(globalRules.stream().map(FreqControlRule::toString).collect(Collectors.joining(","))).append("], ");
        sb.append(" rules4methods:[");
        rules4methods.forEach((method, rules) -> {
            sb.append(method).append(":[").append(rules.stream().map(FreqControlRule::toString).collect(Collectors.joining(","))).append("], ");
        });
        sb.append("]");
        return  sb.toString();
    }
}
