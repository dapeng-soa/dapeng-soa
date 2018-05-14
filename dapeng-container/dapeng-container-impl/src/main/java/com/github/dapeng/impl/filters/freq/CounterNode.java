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
    public short appId;
    /**
     * rule_type_id 被映射为 16bit id
     */
    public short ruleTypeId;
    /**
     * ip, userId, callerMid etc.
     */
    public int key;
    /**
     * last updated unix epoch, seconds since 1970.
     */
    public int timestamp;
    /**
     * min interval counter
     */
    public int minCount;
    /**
     * mid interval counter
     */
    public int midCount;
    /**
     * max interval counter
     */
    public int maxCount;

    @Override
    public String toString() {
        return "appId:" + appId + ", ruleTypeId:" + ruleTypeId + ", key:" + key
                + ", timestamp:" + timestamp + ", counters:" + minCount + "/" + midCount + "/" + maxCount;
    }
}
