
package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.FreqControlRule;
import java.util.ArrayList;
import java.util.List;

public class TestRuleParse {


    private static List<FreqControlRule> doParseRuleData(String ruleData) {

        // todo

        List<FreqControlRule> datasOfRule = new ArrayList<>();

        String[] str = ruleData.split("\n|\r|\r\n");

        for (int i = 0;i<str.length;i++)
        {
            if (str[i].indexOf("rule")!= -1){
                FreqControlRule rule = new FreqControlRule();
                rule.app = str[++i].split("=")[1].trim();
                rule.ruleType = str[++i].split("=")[1].trim();
                rule.minInterval = Integer.parseInt( str[++i].split("=")[1].trim().split(",")[0]);
                rule.maxReqForMinInterval = Integer.parseInt( str[i].split("=")[1].trim().split(",")[1]);
                rule.midInterval = Integer.parseInt(str[++i].split("=")[1].trim().split(",")[0]);
                rule.maxReqForMidInterval = Integer.parseInt(str[i].split("=")[1].trim().split(",")[1]);
                rule.maxInterval = Integer.parseInt(str[++i].split("=")[1].trim().split(",")[0]);
                rule.maxReqForMaxInterval = Integer.parseInt(str[i].split("=")[1].trim().split(",")[1]);
                datasOfRule.add(rule);
            }
        }
        return datasOfRule;
    }



    public static void main(String[] args){
        String data = "[rule1]\n" +
                      "match_app = com.foo.service1\n" +
                      "rule_type = callerId\n" +
                      "min_interval = 60,600\n" +
                      "mid_interval = 3600,10000\n" +
                      "max_interval = 86400,80000\n" +
                      "\n" +
                     "[rule2]\n" +
                     "match_app = com.foo.service2\n" +
                     "rule_type = callerIp\n" +
                     "min_interval = 60,600\n" +
                     "mid_interval = 3600,10000\n" +
                     "max_interval = 86400,80000\n" +
                     "\n" +
                     "[rule3]\n" +
                     "match_app = com.foo.service3\n" +
                     "rule_type = callerIp\n" +
                     "min_interval = 60,600\n" +
                     "mid_interval = 3600,10000\n" +
                     "max_interval = 86400,80000\n";

        List<FreqControlRule> rules = doParseRuleData(data);

        System.out.println(rules.size());
        System.out.println(rules);
    }
}