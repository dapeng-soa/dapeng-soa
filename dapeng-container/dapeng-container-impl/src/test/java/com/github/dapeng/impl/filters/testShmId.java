package com.github.dapeng.impl.filters;

import com.github.dapeng.core.FreqControlRule;
import com.github.dapeng.impl.filters.freq.ShmManager;

/**
 * Root Page 4K(version:1, nodePageCount:128K, rootPageLock(i32), nextUtf8offset, nextDictionId, resolved)
 * Dictionary Root：12K
 * Dictionary Data: 128K
 * Node Page: 128M
 * <p>
 *
 * @author ever
 */
public class testShmId {

    public static void main(String[] args) {
        ShmManager manager = ShmManager.getInstance();
        FreqControlRule rule = new FreqControlRule();

        System.out.println();
        System.out.println("insert new item");
        rule.app = "com.today.servers1";
        rule.ruleType = "callId";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 20;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 80;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 100;

        manager.reportAndCheck(rule, 214);
        System.out.println();

        rule.app = "com.today.servers2";
        rule.ruleType = "callId";
        manager.reportAndCheck(rule, 214);
        System.out.println();

        rule.app = "com.today.servers3";
        rule.ruleType = "callId";
        manager.reportAndCheck(rule, 214);
        System.out.println();

        rule.app = "com.today.servers4";
        rule.ruleType = "callId";
        manager.reportAndCheck(rule, 214);


        System.out.println();
        System.out.println("check item");
        System.out.println();
        rule.app = "com.today.servers5";
        rule.ruleType = "callIp";
        manager.reportAndCheck(rule, 214);

        rule.app = "com.today.servers2";
        rule.ruleType = "callId";
        manager.reportAndCheck(rule, 214);
        System.out.println();

        rule.app = "com.today.servers6";
        rule.ruleType = "callIp";
        manager.reportAndCheck(rule, 214);

    }
}
