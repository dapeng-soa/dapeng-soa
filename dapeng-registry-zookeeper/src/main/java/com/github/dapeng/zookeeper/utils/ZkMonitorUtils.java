package com.github.dapeng.zookeeper.utils;

import com.github.dapeng.core.FreqControlRule;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.core.helper.MasterHelper;
import com.github.dapeng.router.Route;
import com.github.dapeng.router.RoutesExecutor;
import com.github.dapeng.zookeeper.agent.impl.ServerZkAgentImpl;
import com.github.dapeng.zookeeper.common.BaseZKClient;
import com.github.dapeng.zookeeper.common.ConfigKey;
import com.github.dapeng.zookeeper.common.ZkDataContext;
import com.github.dapeng.zookeeper.common.ZkServiceInfo;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.github.dapeng.zookeeper.common.BaseConfig.*;


/**
 * zk 数据监听工具
 *
 * @author huyj
 * @Created 2018/5/25 9:48
 */
public class ZkMonitorUtils {
    private static final Logger logger = LoggerFactory.getLogger(ServerZkAgentImpl.class);

    public static void MonitorZkData(String path, CuratorFramework curator, BaseZKClient.ZK_TYPE zk_type, BaseZKClient baseZKClient) throws Exception {
        TreeCache treeCache = new TreeCache(curator, path);
        treeCache.start();
        treeCache.getListenable().addListener((curatorFramework, treeCacheEvent) -> {
            if (Objects.isNull(treeCacheEvent.getData())) return;

            String changePath = treeCacheEvent.getData().getPath();
            switch (getChangePath(changePath)) {
                //运行实例
                case MONITOR_RUNTIME_PATH:
                    baseZKClient.lockZkDataContext();
                    System.out.println("----" + zk_type + "------ ZK [" + changePath + "] 开始同步 -------");
                    runtimeInstanceChanged(treeCacheEvent, baseZKClient.zkDataContext(), zk_type, baseZKClient);
                    System.out.println("----" + zk_type + "------ ZK [" + changePath + "] 同步结束 ------- ");
                    baseZKClient.releaseZkDataContext();
                    break;

                //服务配置
                case CONFIG_PATH:
                    //  baseZKClient.lockZkDataContext();
                    System.out.println("----" + zk_type + "------ ZK [" + changePath + "] 开始同步 -------");
                    configsDataChanged(treeCacheEvent, baseZKClient.zkDataContext());
                    System.out.println("----" + zk_type + "------ ZK [" + changePath + "] 同步结束 ------- ");
                    // baseZKClient.releaseZkDataContext();
                    break;

                //路由配置
                case MONITOR_ROUTES_PATH:
                    //baseZKClient.lockZkDataContext();
                    System.out.println("----" + zk_type + "------ ZK [" + changePath + "] 开始同步 -------");
                    routesDataChanged(treeCacheEvent, baseZKClient.zkDataContext());
                    System.out.println("----" + zk_type + "------ ZK [" + changePath + "] 同步结束 ------- ");
                    // baseZKClient.releaseZkDataContext();
                    break;

                //限流规则
                case MONITOR_FREQ_PATH:
                    //baseZKClient.lockZkDataContext();
                    System.out.println("----" + zk_type + "------ ZK [" + changePath + "] 开始同步 -------");
                    freqsDataChanged(treeCacheEvent, baseZKClient.zkDataContext());
                    System.out.println("----" + zk_type + "------ ZK [" + changePath + "] 同步结束 ------- ");
                    //baseZKClient.releaseZkDataContext();
                    break;

                //白名单
                case MONITOR_WHITELIST_PATH:
                    //baseZKClient.lockZkDataContext();
                    System.out.println("----" + zk_type + "------ ZK [" + changePath + "] 开始同步 -------");
                    whiteListChanged(treeCacheEvent, baseZKClient.zkDataContext());
                    System.out.println("----" + zk_type + "------ ZK [" + changePath + "] 同步结束 ------- ");
                    //baseZKClient.releaseZkDataContext();
                    break;

                default:
                    logger.info("The current path[{}] is not monitored....", getChangePath(changePath));
                    break;
            }
        });
    }


    private static String getChangePath(String path) {
        if (path.contains(MONITOR_RUNTIME_PATH)) {
            return MONITOR_RUNTIME_PATH;
        }
        if (path.contains(CONFIG_PATH)) {
            return CONFIG_PATH;
        }
        if (path.contains(MONITOR_ROUTES_PATH)) {
            return MONITOR_ROUTES_PATH;
        }
        if (path.contains(MONITOR_FREQ_PATH)) {
            return MONITOR_FREQ_PATH;
        }
        if (path.contains(MONITOR_WHITELIST_PATH)) {
            return MONITOR_WHITELIST_PATH;
        }
        return path;
    }


    //zk 运行实例监听
    private static void runtimeInstanceChanged(TreeCacheEvent treeCacheEvent, ZkDataContext zkDataContext, BaseZKClient.ZK_TYPE zk_type, BaseZKClient baseZKClient) throws Exception {
        logger.info("the zk path[{}] has changed, then start sync zkDataContext[servicesMap,runtimeInstancesMap]..", RUNTIME_PATH);
        String path = treeCacheEvent.getData().getPath();
        List list = Splitter.on("/").trimResults().omitEmptyStrings().splitToList(path);
        if (list.size() < 5) { //  /soa/runtime/services/XXXXService
            return;
        }

        baseZKClient.lockZkDataContext();
        String[] arr = new String[list.size()];
        list.toArray(arr);
        String serviceName = arr[3];
        String[] innstanceInfo = arr[4].split("[:]");

        String host = innstanceInfo[0];
        String port = innstanceInfo[1];
        String versionName = innstanceInfo[2];
        String temp_seqid = innstanceInfo[3];

        ZkServiceInfo zkServiceInfo = new ZkServiceInfo(serviceName, host, Integer.parseInt(port), versionName);
        RuntimeInstance runtimeInstance = new RuntimeInstance(serviceName, host, Integer.parseInt(port), versionName, temp_seqid);
        switch (treeCacheEvent.getType()) {
            //添加数据
            case NODE_ADDED:
                List<RuntimeInstance> runtimeInstanceList = zkDataContext.getRuntimeInstancesMap().get(serviceName);
                if (runtimeInstanceList == null) {
                    runtimeInstanceList = new ArrayList<RuntimeInstance>();
                }
                runtimeInstanceList.add(runtimeInstance);

                List<ZkServiceInfo> zkServiceInfoList = zkDataContext.getServicesMap().get(serviceName);
                if (zkServiceInfoList == null) {
                    zkServiceInfoList = new ArrayList<ZkServiceInfo>();
                }
                zkServiceInfoList.add(zkServiceInfo);

                zkDataContext.getRuntimeInstancesMap().put(serviceName, runtimeInstanceList);
                zkDataContext.getServicesMap().put(serviceName, zkServiceInfoList);
                logger.info("NODE_ADDED：路径：[{}]，数据：[{}]，状态：[{}]", treeCacheEvent.getData().getPath(), getNodeData(treeCacheEvent), treeCacheEvent.getData().getStat());
                break;

            //数据更新
            case NODE_UPDATED:
                /*zkDataContext.getRuntimeInstancesMap().get(serviceName).add(runtimeInstance);
                zkDataContext.getServicesMap().get(serviceName).add(zkServiceInfo);*/
                logger.info("the path [{}] is protected.. it can not be update.", RUNTIME_PATH);
                break;

            //数据删除
            case NODE_REMOVED:
                zkDataContext.getRuntimeInstancesMap().get(serviceName).removeIf(item -> item.getEqualStr().equalsIgnoreCase(runtimeInstance.getEqualStr()));
                zkDataContext.getServicesMap().get(serviceName).removeIf(item -> item.getZkServiceInfo().equalsIgnoreCase(zkServiceInfo.getZkServiceInfo()));
                //zkDataContext.getServicesMap().get(serviceName).remove(zkServiceInfo);
                logger.info("NODE_REMOVED：路径：[{}]，数据：[{}]，状态：[{}]", treeCacheEvent.getData().getPath(), getNodeData(treeCacheEvent), treeCacheEvent.getData().getStat());
                break;

            default:
                break;
        }
        logger.info("***********the zk path[{}] has changed, sync zkDataContext[servicesMap,runtimeInstancesMap] succeed..", RUNTIME_PATH);
       /* if (zk_type == BaseZKClient.ZK_TYPE.CLIENT && zkDataContext.getRuntimeInstancesMap().get(serviceName).size() > 0) {
            System.out.println("客户端 RuntimeInstances 同步完成。。。" + zkDataContext.getRuntimeInstancesMap().get(serviceName));
        }*/

        //服务器端 要选举
        if (zk_type == BaseZKClient.ZK_TYPE.SERVER) {
            logger.info("-------- zk runtimeinstance changed, To carry out the election.. ----------------");
            String serviceKey = serviceName + ":" + versionName;
            String instanceInfo = host + ":" + port + ":" + versionName;
            checkIsMaster(zkDataContext.getRuntimeInstancesMap().get(serviceName), serviceKey, instanceInfo);
        }
        baseZKClient.releaseZkDataContext();
    }

    //zk config 监听
    private static void configsDataChanged(TreeCacheEvent treeCacheEvent, ZkDataContext zkDataContext) throws UnsupportedEncodingException {
        logger.info("the zk path[{}] has changed, then start sync zkDataContext[configsMap]..", CONFIG_PATH);
        String path = treeCacheEvent.getData().getPath();
        String data = getNodeData(treeCacheEvent);

        //全局配置，格式  timeout/800ms;loadBalance/random
        if (path.equalsIgnoreCase(CONFIG_PATH)) {
            String[] confArr = data.split("[;]");
            for (String itemConfig : confArr) {
                String[] itemArr = itemConfig.split("[/]");
                ConfigKey itemKey = ConfigKey.getConfigKeyByValue(itemArr[0]);
                if (Objects.isNull(itemKey)) {
                    logger.info("the config[{}] is not found in the ConfigKey,Please check whether the input is incorrect...", itemArr[0]);
                } else {
                    zkDataContext.setConfigData(ZkDataContext.ConfigLevel.GLOBAL, null, null, itemKey, itemArr[1]);
                }
            }
        } else {
            //服务级别和方法级别   timeout/800ms,register:4001ms,modifySupplier:200ms;loadBalance/leastActive,createSupplier:random,modifySupplier:roundRobin;
            List list = Splitter.on("/").trimResults().omitEmptyStrings().splitToList(treeCacheEvent.getData().getPath());
            String[] arr = new String[list.size()];
            list.toArray(arr);
            String serviceName = arr[3];

            ConfigKey configKey = null;
            //1.按';'分割不同功能配置
            String[] confArr = data.split("[;]");
            for (String itemConfig : confArr) {//格式: timeout/800ms,register:4001ms,modifySupplier:200ms
                //2.按','区分 服务级别和方法级别
                String[] itemConfigArr = itemConfig.split("[,]");
                for (String item : itemConfigArr) {//格式: timeout/800ms register:4001ms  modifySupplier:200ms
                    if (item.contains("/")) {//服务级别
                        String[] itemArr = item.split("[/]");
                        configKey = ConfigKey.getConfigKeyByValue(itemArr[0]);
                        if (Objects.isNull(configKey)) {
                            logger.info("the config[{}] is not found in the ConfigKey,Please check whether the input is incorrect...", itemArr[0]);
                        } else {
                            zkDataContext.setConfigData(ZkDataContext.ConfigLevel.SERVICE, serviceName, null, configKey, itemArr[1]);
                        }
                    } else {//方法级别
                        String[] itemArr = item.split("[:]");
                        if (itemArr.length >= 2) {
                            zkDataContext.setConfigData(ZkDataContext.ConfigLevel.METHOD, serviceName, itemArr[0], configKey, itemArr[1]);
                        } else {
                            logger.info("the config[{}] is incorrect...", item);
                        }
                    }
                }
            }
        }
        logger.info("*********** the zk path[{}] has changed,sync zkDataContext[configsMap] succeed.. ", CONFIG_PATH);
    }

    //zk 路由配置监听
    private static void routesDataChanged(TreeCacheEvent treeCacheEvent, ZkDataContext zkDataContext) throws
            UnsupportedEncodingException {
        logger.info("the zk path[{}] has changed, then start sync zkDataContext[routesMap]..", ROUTES_PATH);
        String path = treeCacheEvent.getData().getPath();
        List list = Splitter.on("/").trimResults().omitEmptyStrings().splitToList(path);
        String[] arr = new String[list.size()];
        list.toArray(arr);
        String routeData = getNodeData(treeCacheEvent);
        String serviceName = arr[3];

        List<Route> zkRoutes = null;
        if (StringUtils.isNotBlank(routeData)) {
            try {
                zkRoutes = RoutesExecutor.parseAll(routeData);
            } catch (Exception e) {
                zkRoutes = new ArrayList<>(16);
                logger.error("parser routes 信息 失败，请检查路由规则写法是否正确!");
            }
        }

        /*if (zkRoutes != null && !zkRoutes.isEmpty()) {
            zkDataContext.getRoutesMap().get(serviceName).clear();
            zkDataContext.getRoutesMap().get(serviceName).addAll(zkRoutes);
        } else {
            zkDataContext.getRoutesMap().get(serviceName).clear();
        }*/
        zkDataContext.getRoutesMap().put(serviceName, zkRoutes == null ? new ArrayList<>() : zkRoutes);
        logger.info("***********the zk path[{}] has changed,sync zkDataContext[routesMap] succeed..", ROUTES_PATH);
    }

    //zk 限流配置监听
    private static void freqsDataChanged(TreeCacheEvent treeCacheEvent, ZkDataContext zkDataContext) throws
            UnsupportedEncodingException {
        logger.info("the zk path[{}] has changed, then start sync zkDataContext[freqRulesMap]..", FREQ_PATH);
        String path = treeCacheEvent.getData().getPath();
        List list = Splitter.on("/").trimResults().omitEmptyStrings().splitToList(path);
        String[] arr = new String[list.size()];
        list.toArray(arr);
        String freqData = getNodeData(treeCacheEvent);
        String serviceName = arr[3];

        List<FreqControlRule> freqControlRules = null;
        try {
            freqControlRules = doParseRuleData(freqData);
        } catch (Exception e) {
            logger.error("parser freq rule 信息 失败，请检查 rule data 写法是否正确!");
        }

       /* if (freqControlRules != null && !freqControlRules.isEmpty()) {
            zkDataContext.getFreqRulesMap().get(serviceName).clear();
            zkDataContext.getFreqRulesMap().get(serviceName).addAll(freqControlRules);
        } else {
            zkDataContext.getFreqRulesMap().put(serviceName).clear();
        }*/
        zkDataContext.getFreqRulesMap().put(serviceName, freqControlRules == null ? new ArrayList<>() : freqControlRules);
        logger.info(" ***********the zk path[{}] has changed, sync zkDataContext[freqRulesMap] succeed..", FREQ_PATH);
    }

    //zk 白名单监听
    private static void whiteListChanged(TreeCacheEvent treeCacheEvent, ZkDataContext zkDataContext) throws
            UnsupportedEncodingException {
        logger.info("the zk path[{}] has changed, then start sync zkDataContext[whiteList]..", WHITELIST_PATH);
        String path = treeCacheEvent.getData().getPath();
        List list = Splitter.on("/").trimResults().omitEmptyStrings().splitToList(path);
        String[] arr = new String[list.size()];
        list.toArray(arr);
        String serviceName = arr[3];
        switch (treeCacheEvent.getType()) {
            //添加数据
            case NODE_ADDED:
                zkDataContext.getWhiteList().add(serviceName);
                logger.info("NODE_ADDED：路径：[{}]，数据：[{}]，状态：[{}]", treeCacheEvent.getData().getPath(), getNodeData(treeCacheEvent), treeCacheEvent.getData().getStat());
                break;

            //数据更新
            case NODE_UPDATED:
                logger.info("the path [{}] is protected.. it can not be update.", WHITELIST_PATH);
                break;

            //数据删除
            case NODE_REMOVED:
                zkDataContext.getWhiteList().remove(serviceName);
                logger.info("NODE_REMOVED：路径：[{}]，数据：[{}]，状态：[{}]", treeCacheEvent.getData().getPath(), getNodeData(treeCacheEvent), treeCacheEvent.getData().getStat());
                break;

            default:
                break;
        }
        logger.info("***********the zk path[{}] has changed, sync zkDataContext[whiteList] succeed ..", WHITELIST_PATH);
    }


    /**
     * 解析 zookeeper 上 配置的 ruleData数据 为FreqControlRule对象
     *
     * @param ruleData data from zk node
     * @return
     */
    private static List<FreqControlRule> doParseRuleData(String ruleData) throws Exception {
        logger.debug("doParseRuleData,限流规则解析前：{}", ruleData);
        List<FreqControlRule> datasOfRule = new ArrayList<>();
        String[] str = ruleData.split("\n|\r|\r\n");
        String pattern1 = "^\\[.*\\]$";
        String pattern2 = "^[a-zA-Z]+\\[.*\\]$";

        for (int i = 0; i < str.length; ) {
            if (Pattern.matches(pattern1, str[i])) {
                FreqControlRule rule = new FreqControlRule();
                rule.targets = new HashSet<>();

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
                                for (int k = 0; k < str1.length; k++) {
                                    if (!str1[k].contains(".")) {
                                        rule.targets.add(Integer.parseInt(str1[k].trim()));
                                    } else {
                                        rule.targets.add(IPUtils.transferIp(str1[k].trim()));
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
                    logger.error("doParseRuleData, 限流规则解析失败。rule:{}", rule);
                    throw new Exception();
                }
                datasOfRule.add(rule);
            } else {
                i++;
            }
        }
        logger.debug("doParseRuleData,限流规则解析后：{}", datasOfRule);
        return datasOfRule;
    }


    /**
     * @param children     当前方法下的实例列表，        eg 127.0.0.1:9081:1.0.0,192.168.1.12:9081:1.0.0
     * @param serviceKey   当前服务信息                eg com.github.user.UserService:1.0.0
     * @param instanceInfo 当前服务节点实例信息         eg  192.168.10.17:9081:1.0.0
     */
    // TODO 判断是否Master 需要重写
    private static void checkIsMaster(List<RuntimeInstance> runtimeInstances, String serviceKey, String instanceInfo) {
        if (runtimeInstances.size() <= 0) {
            return;
        }
        /**
         * 排序规则
         * a: 192.168.100.1:9081:1.0.0:0000000022
         * b: 192.168.100.1:9081:1.0.0:0000000014
         * 根据 lastIndexOf :  之后的数字进行排序，由小到大，每次取zk临时有序节点中的序列最小的节点作为master
         */
        try {
            runtimeInstances.sort((o1, o2) -> {
                Integer int1 = Integer.valueOf(o1.getTemp_seqid());
                Integer int2 = Integer.valueOf(o2.getTemp_seqid());
                return int1 - int2;
            });

            RuntimeInstance firstInstance = runtimeInstances.get(0);
            logger.info("serviceInfo firstNode {}", firstInstance.getInstanceInfo());
            if (firstInstance.getInstanceInfo().equals(instanceInfo)) {
                MasterHelper.isMaster.put(serviceKey, true);
                logger.info("({})竞选master成功, master({})", serviceKey, instanceInfo);
            } else {
                MasterHelper.isMaster.put(serviceKey, false);
                logger.info("({})竞选master失败，当前节点为({})", serviceKey);
            }
        } catch (NumberFormatException e) {
            logger.error("临时节点格式不正确,请使用新版，正确格式为 etc. 192.168.100.1:9081:1.0.0:0000000022");
        }
    }


    private static String getNodeData(TreeCacheEvent treeCacheEvent) throws UnsupportedEncodingException {
        if (Objects.isNull(treeCacheEvent.getData().getData())) {
            return "null";
        } else {
            return new String(treeCacheEvent.getData().getData(), "utf-8");
        }
    }
}
