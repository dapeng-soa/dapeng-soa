package com.github.dapeng.core;

/**
 * 描述: 限流规则
 *
 * @author hz.lei
 * @date 2018年05月09日 下午10:03
 */
public class FreqControlRule {

    String app;
    String ruleType;
    int minInterval;
    int maxReqForMinInterval;
    int midInterval;
    int maxReqForMidInterval;
    int maxInterval;
    int maxReqForMaxInterval;

    @Override
    public String toString() {
        return "app:" + app + ", ruleType:" + ruleType + ", freqRule:["
                + minInterval + "," + maxReqForMinInterval + "/"
                + midInterval + "," + maxReqForMidInterval + "/"
                + maxInterval + "," + maxReqForMaxInterval + ";";
    }
}
