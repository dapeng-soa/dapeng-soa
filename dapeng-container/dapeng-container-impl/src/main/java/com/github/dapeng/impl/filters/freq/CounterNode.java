package com.github.dapeng.impl.filters.freq;

/**
 * 描述:  计数节点
 *
 * @author hz.lei
 * @date 2018年05月14日 上午10:49
 */
public class CounterNode {
    /**
     * app 被映射为 16bit id
     */
    short appId;
    /**
     * rule_type_id 被映射为 16bit id
     */
    short ruleTypeId;
    /**
     * ip, userId, callerMid etc.
     */
    int key;
    /**
     * last updated unix epoch, seconds since 1970.
     */
    int timestamp;
    /**
     * min interval counter
     */
    int minCount;
    /**
     * mid interval counter
     */
    int midCount;
    /**
     * max interval counter
     */
    int maxCount;

    @Override
    public String toString() {
        return "appId:" + appId + ", ruleTypeId:" + ruleTypeId + ", key:" + key
                + ", timestamp:" + timestamp + ", counters:" + minCount + "/" + midCount + "/" + maxCount;
    }
}
