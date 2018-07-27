package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.core.FreqControlRule;
import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.core.lifecycyle.LifecycleEvent;
import com.github.dapeng.core.lifecycyle.LifecycleProcessor;
import com.github.dapeng.registry.RegistryAgent;
import com.github.dapeng.core.helper.MasterHelper;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


/**
 * 服务端  zk 服务注册
 *
 * @author hz.lei
 * @date 2018-03-20
 */
public class ServerZk extends CommonZk {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerZk.class);

    private RegistryAgent registryAgent;

    /**
     * 路由配置信息
     */
    private final Map<String, List<FreqControlRule>> freqControlMap = new ConcurrentHashMap<>(16);

    /**
     * zk 配置 缓存 ，根据 serivceName + versionName 作为 key
     */
    public final ConcurrentMap<String, ZkServiceInfo> zkConfigMap = new ConcurrentHashMap();

    public ServerZk(RegistryAgent registryAgent) {
        this.registryAgent = registryAgent;
    }

    /**
     * zk 客户端实例化
     * 使用 CountDownLatch 门闩 锁，保证zk连接成功后才返回
     */
    public void connect() {
        try {
            CountDownLatch semaphore = new CountDownLatch(1);

            zk = new ZooKeeper(zkHost, 30000, watchedEvent -> {

                switch (watchedEvent.getState()) {

                    case Expired:
                        LOGGER.info("ServerZk session timeout to  {} [Zookeeper]", zkHost);
                        destroy();
                        connect();
                        break;

                    case SyncConnected:
                        semaphore.countDown();
                        //创建根节点
                        create(RUNTIME_PATH, null, false);
                        create(CONFIG_PATH, null, false);
                        create(ROUTES_PATH, null, false);
                        zkConfigMap.clear();
                        LOGGER.info("ServerZk connected to  {} [Zookeeper]", zkHost);
                        if (registryAgent != null) {
                            registryAgent.registerAllServices();//重新注册服务
                        }
                        break;

                    case Disconnected:
                        //zookeeper重启或zookeeper实例重新创建
                        LOGGER.error("[Disconnected]: ServerZookeeper Registry zk 连接断开，可能是zookeeper重启或重建");

                        isMaster.clear(); //断开连接后，认为，master应该失效，避免某个孤岛一直以为自己是master

                        destroy();
                        connect();
                        break;
                    case AuthFailed:
                        LOGGER.info("Zookeeper connection auth failed ...");
                        destroy();
                        break;
                    default:
                        break;
                }
            });
            //hold 10 s
            semaphore.await(10000, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * 关闭 zk 连接
     */
    public void destroy() {
        if (zk != null) {
            try {
                LOGGER.info("ServerZk closing connection to zookeeper {}", zkHost);
                zk.close();
                zk = null;
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

    }

    //～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～
    //                           that's begin                                      ～
    //                                                                             ～
    //～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～


    /**
     * 递归节点创建
     */
    public void create(String path, RegisterContext context, boolean ephemeral) {

        int i = path.lastIndexOf("/");
        if (i > 0) {
            String parentPath = path.substring(0, i);
            //判断父节点是否存在...
            if (!checkExists(parentPath)) {
                create(parentPath, null, false);
            }
        }
        if (ephemeral) {
            createEphemeral(path + ":", context);

            //添加 watch ，监听子节点变化
//            watchInstanceChange(context);
        } else {
            createPersistent(path, "");

        }
    }

    /**
     * 检查节点是否存在
     */
    public boolean checkExists(String path) {
        try {
            Stat exists = zk.exists(path, false);
            if (exists != null) {
                return true;
            }
            return false;
        } catch (Throwable t) {
        }
        return false;
    }


    /**
     * 监听服务节点下面的子节点（临时节点，实例信息）变化
     */
    public void watchInstanceChange(RegisterContext context) {
        String watchPath = context.getServicePath();
        try {
            List<String> children = zk.getChildren(watchPath, event -> {
                //Children发生变化，则重新获取最新的services列表
                if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    LOGGER.info("容器状态:{}, {}子节点发生变化，重新获取子节点...", ContainerFactory.getContainer().status(), event.getPath());
                    if (ContainerFactory.getContainer().status() == Container.STATUS_SHUTTING
                            || ContainerFactory.getContainer().status() == Container.STATUS_DOWN) {
                        LOGGER.warn("Container is shutting down");
                        return;
                    }
                    watchInstanceChange(context);
                }
            });
            if (children.size() > 0) {
                checkIsMaster(children, MasterHelper.generateKey(context.getService(), context.getVersion()), context.getInstanceInfo());
            }
            //masterChange响应
          LifecycleProcessor.getInstance().onLifecycleEvent(LifecycleEvent.MASTER_CHANGE);
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            create(context.getServicePath() + "/" + context.getInstanceInfo(), context, true);
        }

    }


    /**
     * 异步添加持久化的节点
     *
     * @param path
     * @param data
     */
    private void createPersistent(String path, String data) {
        Stat stat = exists(path);

        if (stat == null) {
            zk.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, persistNodeCreateCallback, data);
        }
    }

    private Stat exists(String path) {
        Stat stat = null;
        try {
            stat = zk.exists(path, false);
        } catch (KeeperException | InterruptedException e) {
        }
        return stat;
    }

    /**
     * 异步添加持久化节点回调方法
     */
    private AsyncCallback.StringCallback persistNodeCreateCallback = (rc, path, ctx, name) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOGGER.info("创建节点:{},连接断开，重新创建", path);
                createPersistent(path, (String) ctx);
                break;
            case OK:
                LOGGER.info("创建节点:{},成功", path);
                break;
            case NODEEXISTS:
                LOGGER.info("创建节点:{},已存在", path);
                updateServerInfo(path, (String) ctx);
                break;
            default:
                LOGGER.info("创建节点:{},失败", path);
        }
    };


    /**
     * 异步添加serverInfo,为临时有序节点，如果server挂了就木有了
     */
    public void createEphemeral(String path, RegisterContext context) {
        zk.create(path, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL, serverInfoCreateCallback, context);
    }

    /**
     * 异步添加serverInfo 临时节点 的回调处理
     */
    private AsyncCallback.StringCallback serverInfoCreateCallback = (rc, path, ctx, name) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOGGER.info("添加serviceInfo:{},连接断开，重新添加", path);
                //重新调用
                create(path, (RegisterContext) ctx, true);
                break;
            case OK:
                /**
                 * callback 时 注册监听
                 */
                watchInstanceChange((RegisterContext) ctx);

                LOGGER.info("添加serviceInfo:{},成功,注册实例监听watch watchInstanceChange", path);
                break;
            case NODEEXISTS:
                LOGGER.info("添加serviceInfo:{},已存在，删掉后重新添加", path);
                try {
                    //只删除了当前serviceInfo的节点
                    zk.delete(path, -1);
                } catch (Exception e) {
                    LOGGER.error("删除serviceInfo:{} 失败:{}", path, e.getMessage());
                }
                create(path, (RegisterContext) ctx, true);
                break;
            default:
                LOGGER.info("添加serviceInfo:{}，出错", path);
        }
    };

    /**
     * 异步更新节点信息
     */
    private void updateServerInfo(String path, String data) {
        zk.setData(path, data.getBytes(), -1, serverInfoUpdateCallback, data);
    }

    /**
     * 异步更新节点信息的回调方法
     */
    private AsyncCallback.StatCallback serverInfoUpdateCallback = (rc, path1, ctx, stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                updateServerInfo(path1, (String) ctx);
                return;
            default:
                //just skip
        }
    };

    public void setZookeeperHost(String zkHost) {
        this.zkHost = zkHost;
    }

    //-----竞选master---
    private static Map<String, Boolean> isMaster = MasterHelper.isMaster;

    /**
     * @param children     当前方法下的实例列表，        eg 127.0.0.1:9081:1.0.0,192.168.1.12:9081:1.0.0
     * @param serviceKey   当前服务信息                eg com.github.user.UserService:1.0.0
     * @param instanceInfo 当前服务节点实例信息         eg  192.168.10.17:9081:1.0.0
     */
    public void checkIsMaster(List<String> children, String serviceKey, String instanceInfo) {
        if (children.size() <= 0) {
            return;
        }


        /**
         * 排序规则
         * a: 192.168.100.1:9081:1.0.0:0000000022
         * b: 192.168.100.1:9081:1.0.0:0000000014
         * 根据 lastIndexOf :  之后的数字进行排序，由小到大，每次取zk临时有序节点中的序列最小的节点作为master
         */
        try {
            Collections.sort(children, (o1, o2) -> {
                Integer int1 = Integer.valueOf(o1.substring(o1.lastIndexOf(":") + 1));
                Integer int2 = Integer.valueOf(o2.substring(o2.lastIndexOf(":") + 1));
                return int1 - int2;
            });

            String firstNode = children.get(0);
            LOGGER.info("serviceInfo firstNode {}", firstNode);

            String firstInfo = firstNode.replace(firstNode.substring(firstNode.lastIndexOf(":")), "");

            if (firstInfo.equals(instanceInfo)) {
                isMaster.put(serviceKey, true);
                LOGGER.info("({})竞选master成功, master({})", serviceKey, CURRENT_CONTAINER_ADDR);
            } else {
                isMaster.put(serviceKey, false);
                LOGGER.info("({})竞选master失败，当前节点为({})", serviceKey);
            }
        } catch (NumberFormatException e) {
            LOGGER.error("临时节点格式不正确,请使用新版，正确格式为 etc. 192.168.100.1:9081:1.0.0:0000000022");
        }
    }


    private static final String CURRENT_CONTAINER_ADDR = SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" +
            String.valueOf(SoaSystemEnvProperties.SOA_CONTAINER_PORT);


    /**
     * 获取zk 配置信息，封装到 ZkConfigInfo
     *
     * @param serviceName
     * @return
     */
    protected ZkServiceInfo getConfigData(String serviceName) {
        ZkServiceInfo info = zkConfigMap.get(serviceName);
        if (info != null) {
            return info;
        }
        info = new ZkServiceInfo(serviceName);
        syncZkConfigInfo(info);
        zkConfigMap.put(serviceName, info);
        return info;
    }

    /**
     * 获取 zookeeper 上的 限流规则 freqRule
     *
     * @return
     */
    public List<FreqControlRule> getFreqControl(String service) {
        if (freqControlMap.get(service) == null) {
            try {
                byte[] data = zk.getData(FREQ_PATH + "/" + service, event -> {
                    if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {
                        LOGGER.info("freq 节点 data 发生变更，重新获取信息");
                        freqControlMap.remove(service);
                        getFreqControl(service);
                    }
                }, null);
                List<FreqControlRule> freqControlRules = processFreqRuleData(service, data, freqControlMap);
                return freqControlRules;
            } catch (KeeperException | InterruptedException e) {
                LOGGER.error("获取route service 节点: {} 出现异常", service);
            }
        } else {
            LOGGER.debug("获取route信息, service: {} , route size {}", service, freqControlMap.get(service).size());
            return this.freqControlMap.get(service);
        }
        return new ArrayList<>();
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
    private static List<FreqControlRule> doParseRuleData(String ruleData) throws Exception {
        LOGGER.debug("doParseRuleData,限流规则解析前：{}", ruleData);
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
                    LOGGER.error("doParseRuleData, 限流规则解析失败。rule:{}", rule);
                    throw new Exception();
                }
                datasOfRule.add(rule);
            } else {
                i++;
            }
        }
        LOGGER.debug("doParseRuleData,限流规则解析后：{}", datasOfRule);
        return datasOfRule;
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
