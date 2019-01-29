package com.github.dapeng.core;

import java.util.Set;

/**
 * 描述: 限流规则
 *
 * @author hz.lei
 * @date 2018年05月09日 下午10:03
 */
public class FreqControlRule {

    public String app;
    public String ruleType;
    public Set<Integer> targets;
    public int minInterval;
    public int maxReqForMinInterval;
    public int midInterval;
    public int maxReqForMidInterval;
    public int maxInterval;
    public int maxReqForMaxInterval;

    @Override
    public String toString() {
        return "app:" + app + ", ruleType:" + ruleType + ", targets:" + targets +", freqRule:["
                + minInterval + "," + maxReqForMinInterval + "/"
                + midInterval + "," + maxReqForMidInterval + "/"
                + maxInterval + "," + maxReqForMaxInterval + ";";
    }
}
