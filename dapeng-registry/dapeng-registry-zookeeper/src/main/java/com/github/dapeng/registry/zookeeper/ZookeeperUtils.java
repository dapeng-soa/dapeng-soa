package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.FreqControlRule;
import com.github.dapeng.core.enums.LoadBalanceStrategy;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.ServiceInfo;
import com.github.dapeng.router.Route;
import com.github.dapeng.router.RoutesExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author tangliu
 * @date 2016/8/8
 */
public class ZookeeperUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperUtils.class);

    /**
     * 解析 zk /soa/config/service 下的节点的内容
     * <p>
     * timeout/800ms,createSupplier:100ms,modifySupplier:200ms;
     * loadbalance/LeastActive,createSupplier:Random,modifySupplier:RoundRobin;
     *
     * @param data
     * @param zkInfo
     */
    public static void processZkConfig(byte[] data, ZkServiceInfo zkInfo, boolean isGlobal) {
        try {

            String configData = new String(data, "utf-8");

            String[] properties = configData.split(";");

            for (String property : properties) {
                String typeValue = property.split("/")[0];
                if (typeValue.equals(ConfigKey.TimeOut.getValue())) {
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

                } else if (typeValue.equals(ConfigKey.LoadBalance.getValue())) {

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
                } else if (typeValue.equals(ConfigKey.SlowServiceTime.getValue())) {
                    if (isGlobal) {
                        String value = property.split("/")[1];
                        zkInfo.slowServiceTimeConfig.globalConfig = timeHelper(value);
                    } else {
                        String[] keyValues = property.split(",");
                        for (String keyValue : keyValues) {
                            String[] props;
                            if (keyValue.contains("/")) {
                                props = keyValue.split("/");
                            } else {
                                props = keyValue.split(":");
                            }
                            zkInfo.slowServiceTimeConfig.serviceConfigs.put(props[0], timeHelper(props[1]));
                        }
                    }
                }
            }
            LOGGER.info("get config from {} with data [{}]", zkInfo.service, configData);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }
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
     * process zk data 解析route 信息
     */
    public static List<Route> processRouteData(String service, byte[] data, Map<String, List<Route>> routesMap) {
        List<Route> zkRoutes;
        try {
            String routeData = new String(data, "utf-8");
            zkRoutes = RoutesExecutor.parseAll(routeData);
            routesMap.put(service, zkRoutes);
        } catch (Exception e) {
            zkRoutes = new ArrayList<>(16);
            LOGGER.error("parser routes 信息 失败，请检查路由规则写法是否正确!");
        }
        return zkRoutes;
    }


    /**
     * process zk data freqControl 限流规则信息
     *
     * @param service
     * @param data
     * @return
     */
    public static List<FreqControlRule> processFreqRuleData(String service, byte[] data, Map<String, List<FreqControlRule>> freqControlMap) {
        List<FreqControlRule> freqControlRules = null;
        try {
            String ruleData = new String(data, "utf-8");
            freqControlRules = doParseRuleData(ruleData);

            freqControlMap.put(service, freqControlRules);
        } catch (Exception e) {
            LOGGER.error("parser freq rule 信息 失败，请检查 rule data 写法是否正确!");
        }
        return freqControlRules;

    }

    /**
     * 解析 zookeeper 上 配置的 ruleData数据 为FreqControlRule对象
     *
     * @param ruleData data from zk node
     * @return
     */
    private static List<FreqControlRule> doParseRuleData(String ruleData) {

            // todo






        return null;
    }


    /**
     * 将配置信息中的时间单位ms 字母替换掉  100ms -> 100
     *
     * @param number
     * @return
     */
    private static Long timeHelper(String number) {
        number = number.replaceAll("[^(0-9)]", "");
        return Long.valueOf(number);
    }
}
