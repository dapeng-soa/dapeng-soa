
package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.FreqControlRule;
import com.github.dapeng.core.helper.IPUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class TestRuleParse {


    private static List<FreqControlRule> doParseRuleData(String ruleData) {

        // todo

        List<FreqControlRule> datasOfRule = new ArrayList<>();

        String[] str = ruleData.split("\n|\r|\r\n");
        String pattern1 = "^\\[.*\\]$";
        String pattern2 = "^[a-zA-Z]+\\[.*\\]$";


        for (int i = 0;i<str.length;)
        {
            if (Pattern.matches(pattern1,str[i])){
                FreqControlRule rule = new FreqControlRule();
                rule.targets = new HashSet<>();

                while (!Pattern.matches(pattern1,str[++i])){
                    if (str[i].trim().equals("")) continue;
                    String[] s = str[i].split("=");
                    switch (s[0].trim()) {
                        case "match_app":
                            rule.app = s[1].trim();
                            break;
                        case "rule_type":
                            if (Pattern.matches(pattern2,s[1].trim())){
                                rule.ruleType = s[1].trim().split("\\[")[0];
                                String[] str1 = s[1].trim().split("\\[")[1].trim().split("\\]")[0].trim().split(",");
                                for (int k = 0; k < str1.length;k++){
                                    if (!str1[k].contains(".")){
                                        rule.targets.add(Integer.parseInt(str1[k].trim()));
                                    }else {
                                        rule.targets.add(IPUtils.transferIp(str1[k].trim()));
                                    }
                                }
                            }else{
                                rule.targets = null;
                                rule.ruleType = s[1].trim();
                            }
                            break;
                        case "min_interval":
                            rule.minInterval = Integer.parseInt(s[1].trim().split(",")[0]);
                            rule.maxReqForMinInterval = Integer.parseInt(s[1].trim().split(",")[1]);
                            break;
                        case "mid_interval":
                            rule.midInterval = Integer.parseInt(s[1].trim().split(",")[0]);
                            rule.maxReqForMidInterval = Integer.parseInt(s[1].trim().split(",")[1]);
                            break;
                        case "max_interval":
                            rule.maxInterval = Integer.parseInt(s[1].trim().split(",")[0]);
                            rule.maxReqForMaxInterval = Integer.parseInt(s[1].trim().split(",")[1]);
                            break;
                    }
                    if ( i == str.length-1)
                    {
                        i++;
                        break;
                    }
                }
                datasOfRule.add(rule);
            }
        }
        return datasOfRule;
    }



    public static void main(String[] args){
        String data = "[customerUserIds]\r\n" +
                      "match_app = com.foo.service1\r\n" +
                      "rule_type = callerId\r\n" +
                      "min_interval = 60,600\r\n" +
                      "mid_interval = 3600,10000\r\n" +
                      "max_interval = 86400,80000\r\n" +
                      "\n" +
                     "[customerUserIds]\r\n" +
                     "match_app = com.foo.service2\r\n" +
                     "rule_type = callerId[1,3,5]\r\n" +
                     "min_interval = 60,600\r\n" +
                     "mid_interval = 3600,10000\r\n" +
                     "max_interval = 86400,80000\r\n" +
                     "\n" +
                     "[customerUserIps]\r\n" +
                     "match_app = com.foo.service3\r\n" +
                     "rule_type = callerIp[192.168.2.1,192.168.35.36,192.162.25.3]\r\n" +
                     "min_interval = 60,600\r\n" +
                     "mid_interval = 3600,10000\r\n" +
                     "max_interval = 86400,80000\r\n";

        List<FreqControlRule> rules = doParseRuleData(data);

        System.out.println(rules.size());
        System.out.println(rules);
    }
}