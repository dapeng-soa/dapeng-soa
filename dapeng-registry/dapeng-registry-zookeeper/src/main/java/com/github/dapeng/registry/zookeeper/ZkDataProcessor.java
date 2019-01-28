/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.*;
import com.github.dapeng.core.enums.LoadBalanceStrategy;
import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Pattern;

import static com.github.dapeng.core.SoaCode.FreqConfigError;

/**
 * @author tangliu
 * @date 2016/8/8
 */
public class ZkDataProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkDataProcessor.class);

    /**
     * new get config data
     * <p>
     * timeout/800ms,createSupplier:100ms,modifySupplier:200ms
     * loadbalance/LeastActive,createSupplier:Random,modifySupplier:RoundRobin
     * weight/192.168.4.107/9095/700  service weight config1
     * weight/192.168.4.107/500       service weight config2
     * weight/600                    global weight config
     *
     * @param data
     * @param zkInfo
     */
    public static void processZkConfig(byte[] data, ZkServiceInfo zkInfo, boolean isGlobal) {
        try {

            String configData = new String(data, "utf-8").trim();

            String[] properties = configData.split("\n|\r|\r\n");

            if (!isGlobal && !zkInfo.weightServiceConfigs.isEmpty()) {
                zkInfo.weightServiceConfigs.clear();
            }
            for (String property : properties) {
                if (!"".equals(property)) {
                    String typeValue = property.split("/")[0];
                    if (typeValue.equals(ConfigKey.TimeOut.getValue())) {//服务超时 TimeOut
                        if (isGlobal) {
                            String value = property.split("/")[1];
                            zkInfo.timeConfig.globalConfig = timeHelper(value);
                        } else {
                            String[] keyValues = property.split(",");
                            for (String keyValue : keyValues) {
                                String[] props;
                                if (keyValue.contains("/")) {
                                    props = keyValue.split("/");
                                } else {
                                    props = keyValue.split(":");
                                }
                                zkInfo.timeConfig.serviceConfigs.put(props[0], timeHelper(props[1]));
                            }
                        }
                    } else if (typeValue.equals(ConfigKey.ProcessTime.getValue())) { //服务慢服务检测 ProcessTime
                        if (isGlobal) {
                            String value = property.split("/")[1];
                            zkInfo.processTimeConfig.globalConfig = timeHelper(value);
                        } else {
                            String[] keyValues = property.split(",");
                            for (String keyValue : keyValues) {
                                String[] props;
                                if (keyValue.contains("/")) {
                                    props = keyValue.split("/");
                                } else {
                                    props = keyValue.split(":");
                                }
                                zkInfo.processTimeConfig.serviceConfigs.put(props[0], timeHelper(props[1]));
                            }
                        }
                    } else if (typeValue.equals(ConfigKey.LoadBalance.getValue())) { //负载均衡 LoadBalance
                        if (isGlobal) {
                            String value = property.split("/")[1];
                            zkInfo.loadbalanceConfig.globalConfig = LoadBalanceStrategy.findByValue(value);
                        } else {

                            String[] keyValues = property.split(",");
                            for (String keyValue : keyValues) {
                                String[] props;
                                if (keyValue.contains("/")) {
                                    props = keyValue.split("/");
                                } else {
                                    props = keyValue.split(":");
                                }
                                zkInfo.loadbalanceConfig.serviceConfigs.put(props[0], LoadBalanceStrategy.findByValue(props[1]));
                            }
                        }
                    } else if (typeValue.equals(ConfigKey.Weight.getValue())) {//权重 Weight
                        if (isGlobal) {
                            zkInfo.weightGlobalConfig = doParseWeightData(property);

                        } else {
                            zkInfo.weightServiceConfigs.add(doParseWeightData(property));
                        }
                    }
                }
            }
            recalculateRuntimeInstanceWeight(zkInfo);
            LOGGER.info("get " + (isGlobal?"global":"") + " config from " + zkInfo.serviceName() + " with data [" + configData + "]");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }


    /**
     * 将zk config 中的权重设置，同步到运行实例中
     *
     * @param zkInfo
     */
    public static void recalculateRuntimeInstanceWeight(ZkServiceInfo zkInfo) {
        if (zkInfo != null) {
            List<RuntimeInstance> runtimeInstances = zkInfo.runtimeInstances();
            if (runtimeInstances != null && runtimeInstances.size() > 0) {
                for (RuntimeInstance runtimeInstance : runtimeInstances) {
                    if (zkInfo.weightGlobalConfig.ip != null) {   //没有全局配置的情况下ip = null，有全局配置ip = ""
                        runtimeInstance.weight = zkInfo.weightGlobalConfig.weight;
                    }
                    if (zkInfo.weightServiceConfigs != null) {
                        List<Weight> weights = zkInfo.weightServiceConfigs;
                        for (Weight weight : weights) {
                            if (weight.ip.equals(runtimeInstance.ip)) {
                                if (weight.port == runtimeInstance.port) {
                                    runtimeInstance.weight = weight.weight;
                                    break;
                                } else if (weight.port == -1) {
                                    runtimeInstance.weight = weight.weight;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * parse zk weight config
     *
     * @param weightData
     * @return
     */
    public static Weight doParseWeightData(String weightData) {
        Weight weight = new Weight();
        String[] strArr = weightData.split("[/]");
        if (strArr.length >= 2) {
            if (strArr.length == 2) {                   //weight/600  global weight config
                weight.ip = "";
                weight.port = -1;
                weight.weight = Integer.parseInt(strArr[1]);
            } else if (strArr.length == 3) {              //weight/192.168.4.107/500       service weight config2
                weight.ip = strArr[1];
                weight.port = -1;
                weight.weight = Integer.parseInt(strArr[2]);
            } else {                                   //   weight/192.168.4.107/9095/700  service weight config1
                weight.ip = strArr[1];
                weight.port = Integer.parseInt(strArr[2]);
                weight.weight = Integer.parseInt(strArr[3]);
            }
        } else {
            weight = null;
        }
        return weight;
    }

    /**
     * serviceName下子节点列表即可用服务地址列表
     * 子节点命名为：host:port:versionName
     *
     * @param serviceName
     * @param path
     * @param infos
     */
    public static void resetServiceInfoByName(String serviceName, String path, List<String> infos, Map<String, List<ServiceInfo>> caches) {
        LOGGER.info(serviceName + "   " + infos);
        List<ServiceInfo> sinfos = new ArrayList<>();

        for (String info : infos) {
            String[] serviceInfo = info.split(":");
            ServiceInfo sinfo = new ServiceInfo(serviceInfo[0], Integer.valueOf(serviceInfo[1]), serviceInfo[2]);
            sinfos.add(sinfo);
        }

        if (caches.containsKey(serviceName)) {
            List<ServiceInfo> currentInfos = caches.get(serviceName);

            for (ServiceInfo sinfo : sinfos) {
                for (ServiceInfo currentSinfo : currentInfos) {
                    if (sinfo.equalTo(currentSinfo)) {
                        sinfo.setActiveCount(currentSinfo.getActiveCount());
                        break;
                    }
                }
            }
        }
        caches.put(serviceName, sinfos);
    }

    /**
     * 将配置信息中的时间单位ms 字母替换掉  100ms -> 100
     *
     * @param number
     * @return
     */
    public static Long timeHelper(String number) {
        number = number.replaceAll("[^(0-9)]", "");
        return Long.valueOf(number);
    }


    /**
     * process zk data freqControl 限流规则信息
     *
     * @param serviceName
     * @param data
     * @return
     */
    static synchronized ServiceFreqControl processFreqRuleData(String serviceName, byte[] data) {
        try {
            String ruleData = new String(data, "utf-8");
            return doParseRuleData(serviceName, ruleData);
        } catch (Exception e) {
            LOGGER.error("parser freq rule 信息 失败，请检查 rule data 写法是否正确!");
        }
        return null;
    }

    /**
     * 解析 zookeeper 上 配置的 ruleData数据 为FreqControlRule对象
     *
     * @param ruleData data from zk node
     * @return
     */
    static ServiceFreqControl doParseRuleData(final String serviceName, final String ruleData) throws Exception {
        LOGGER.info("doParseRuleData,限流规则解析前ruleData:" + ruleData);
        List<FreqControlRule> rules4service = new ArrayList<>(16);
        Map<String, List<FreqControlRule>> rules4method = new HashMap<>(16);

        String[] str = ruleData.split("\n|\r|\r\n");
        String pattern1 = "^\\[.*\\]$";
        String pattern2 = "^[a-zA-Z]+\\[.*\\]$";

        for (int i = 0; i < str.length; ) {
            if (Pattern.matches(pattern1, str[i])) {
                FreqControlRule rule = new FreqControlRule();
                rule.targets = new HashSet<>(16);

                while (!Pattern.matches(pattern1, str[++i])) {
                    if ("".equals(str[i].trim())) continue;

                    String[] s = str[i].split("=");
                    switch (s[0].trim()) {
                        case "match_app":
                            rule.app = s[1].trim();
                            break;
                        case "rule_type":
                            if (Pattern.matches(pattern2, s[1].trim())) {
                                rule.ruleType = s[1].trim().split("\\[")[0];
                                String[] str1 = s[1].trim().split("\\[")[1].trim().split("\\]")[0].trim().split(",");
                                for (String aStr1 : str1) {
                                    if (!aStr1.contains(".")) {
                                        rule.targets.add(Integer.parseInt(aStr1.trim()));
                                    } else {
                                        rule.targets.add(IPUtils.transferIp(aStr1.trim()));
                                    }
                                }
                            } else {
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
                        default:
                            LOGGER.warn("FreqConfig parse error:" + str[i]);
                    }
                    if (i == str.length - 1) {
                        i++;
                        break;
                    }
                }
                if (rule.app == null || rule.ruleType == null ||
                        rule.minInterval == 0 ||
                        rule.midInterval == 0 ||
                        rule.maxInterval == 0) {
                    LOGGER.error("doParseRuleData, 限流规则解析失败。rule:{}", rule);
                    throw new SoaException(FreqConfigError);
                }
                if (rule.app.equals("*")) {
                    rule.app = serviceName;
                    rules4service.add(rule);
                } else {
                    if (rules4method.containsKey(rule.app)) {
                        rules4method.get(rule.app).add(rule);
                    } else {
                        List<FreqControlRule> rules = new ArrayList<>(8);
                        rules.add(rule);
                        rules4method.put(rule.app, rules);
                    }
                }

            } else {
                i++;
            }
        }

        ServiceFreqControl freqControl = new ServiceFreqControl(serviceName, rules4service, rules4method);
        LOGGER.info("doParseRuleData限流规则解析后内容: " + freqControl);
        return freqControl;
    }

}
