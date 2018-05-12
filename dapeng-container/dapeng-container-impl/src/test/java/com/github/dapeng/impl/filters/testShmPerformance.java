package com.github.dapeng.impl.filters;


import com.github.dapeng.core.FreqControlRule;

public class testShmPerformance {

    public static void main(String[] args) {
        ShmManager manager = ShmManager.getInstance();
        FreqControlRule rule = new FreqControlRule();
        rule.app = "com.today.servers0";
        rule.ruleType = "callIp";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 20;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 80;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 200;

        System.out.println();
        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:2147483647"+ ", freqRule:["
                + rule.minInterval + "," + rule.maxReqForMinInterval + "/"
                + rule.midInterval + "," + rule.maxReqForMidInterval + "/"
                + rule.maxInterval + "," + rule.maxReqForMaxInterval + "];");
        System.out.println();

        long t1 = System.nanoTime();
        for (int i = 0; i < 1000000; i++) {
            manager.reportAndCheck(rule, 2147483647);
        }
        System.out.println("cost1:" + (System.nanoTime() - t1));
        System.out.println();


    }
}
