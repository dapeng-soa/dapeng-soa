package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.api.lifecycle.LifecycleProcessorFactory;
import com.github.dapeng.core.*;
import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.core.helper.MasterHelper;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.core.lifecycle.LifeCycleEvent;
import com.github.dapeng.registry.RegistryAgent;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import static com.github.dapeng.core.SoaCode.FreqConfigError;


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
     * zk 配置 缓存 ，根据 serivceName + versionName 作为 key
     */
    public final Map<String, ZkServiceInfo> serviceInfoByName = new ConcurrentHashMap<>(128);

    public ServerZk(RegistryAgent registryAgent) {
        this.registryAgent = registryAgent;
    }

    /**
     * zk 客户端实例化
     * 使用 CountDownLatch 门闩 锁，保证zk连接成功后才返回
     */
    public synchronized void connect() {
        try {
            CountDownLatch semaphore = new CountDownLatch(1);
            // zk 需要为空
            destroy();

            zk = new ZooKeeper(zkHost, 30000, watchedEvent -> {
                LOGGER.warn("ServerZk::connect zkEvent:" + watchedEvent);
                switch (watchedEvent.getState()) {

                    case Expired:
                        LOGGER.info("ServerZk session timeout to  {} [Zookeeper]", zkHost);
                        connect();
                        break;

                    case SyncConnected:
                        semaphore.countDown();
                        //创建根节点
                        create(RUNTIME_PATH, "", null, false);
                        create(CONFIG_PATH, "", null, false);
                        create(ROUTES_PATH, "", null, false);
                        LOGGER.info("ServerZk connected to  {} [Zookeeper]", zkHost);
                        if (registryAgent != null) {
                            registryAgent.registerAllServices();//重新注册服务
                        }
                        resyncZkInfos();
                        break;

                    case Disconnected:
                        //zookeeper重启或zookeeper实例重新创建
                        LOGGER.error("[Disconnected]: ServerZookeeper Registry zk 连接断开，可能是zookeeper重启或重建");

                        isMaster.clear(); //断开连接后，认为，master应该失效，避免某个孤岛一直以为自己是master
                        // 一般来说， zk重启导致的Disconnected事件不需要处理；
                        // 但对于zk实例重建，需要清理当前zk客户端并重连(自动重连的话会一直拿当前sessionId去重连).
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

    private void resyncZkInfos() {
        synchronized (serviceInfoByName) {
            if (!serviceInfoByName.isEmpty()) {
                serviceInfoByName.values().forEach(serviceInfo -> {
                    syncZkConfigInfo(serviceInfo);
                    if (SoaSystemEnvProperties.SOA_FREQ_LIMIT_ENABLE) {
                        syncZkFreqControl(serviceInfo);
                    }
                });
            }
        }
    }

    /**
     * 关闭 zk 连接
     */
    public synchronized void destroy() {
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
     * 监听服务节点下面的子节点（临时节点，实例信息）变化
     */
    public void watchInstanceChange(RegisterContext context) {
        String watchPath = context.getServicePath();
        try {
            List<String> children = zk.getChildren(watchPath, event -> {
                LOGGER.warn("ServerZk::watchInstanceChange zkEvent:" + event);
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
            boolean _isMaster = false;
            if (children.size() > 0) {
                _isMaster = checkIsMaster(children, MasterHelper.generateKey(context.getService(), context.getVersion()), context.getInstanceInfo());
            }
            //masterChange响应
            LifecycleProcessorFactory.getLifecycleProcessor().onLifecycleEvent(
                    new LifeCycleEvent(LifeCycleEvent.LifeCycleEventEnum.MASTER_CHANGE,
                            context.getService(), _isMaster));
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            create(context.getServicePath() + "/" + context.getInstanceInfo(), "", context, true);
        }
    }

    /**
     * 异步添加持久化节点回调方法
     */
    private AsyncCallback.StringCallback persistNodeCreateCallback = (rc, path, ctx, name) -> {
        LOGGER.warn("ServerZk::persistNodeCreateCallback zkEvent: " + rc + ", " + path + ", " + name);
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOGGER.info("创建节点:{},连接断开，重新创建", path);
                create(path, (String) ctx, null, false);
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
     * 异步添加serverInfo 临时节点 的回调处理
     */
    private AsyncCallback.StringCallback serverInfoCreateCallback = (rc, path, ctx, name) -> {
        LOGGER.warn("ServerZk::serverInfoCreateCallback zkEvent: " + rc + ", " + path + ", " + name);
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOGGER.info("添加serviceInfo:{},连接断开，重新添加", path);
                //重新调用
                create(path, "", (RegisterContext) ctx, true);
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
                create(path, "", (RegisterContext) ctx, true);
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
        LOGGER.warn("ServerZk::serverInfoUpdateCallback zkEvent: " + rc + ", " + path1 + ", " + stat);
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
    public boolean checkIsMaster(List<String> children, String serviceKey, String instanceInfo) {
        if (children.size() <= 0) {
            return false;
        }

        boolean _isMaster = false;

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
                _isMaster = true;
                LOGGER.info("({})竞选master成功, master({})", serviceKey, CURRENT_CONTAINER_ADDR);
            } else {
                isMaster.put(serviceKey, false);
                _isMaster = false;
                LOGGER.info("({})竞选master失败，当前节点为({})", serviceKey);
            }
        } catch (NumberFormatException e) {
            LOGGER.error("临时节点格式不正确,请使用新版，正确格式为 etc. 192.168.100.1:9081:1.0.0:0000000022");
        }

        return _isMaster;
    }


    private static final String CURRENT_CONTAINER_ADDR = SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" +
            String.valueOf(SoaSystemEnvProperties.SOA_CONTAINER_PORT);


    /**
     * 获取zk 配置信息，封装到 ZkConfigInfo
     * 加入并发考虑
     *
     * @param serviceName 服务名(服务唯一)
     * @return ZkServiceInfo
     */
    protected ZkServiceInfo getZkServiceInfo(String serviceName) {
        ZkServiceInfo info = serviceInfoByName.get(serviceName);
        if (info == null) {
            synchronized (serviceInfoByName) {
                info = serviceInfoByName.get(serviceName);
                if (info == null) {
                    info = new ZkServiceInfo(serviceName, new CopyOnWriteArrayList<>());
                    try {
                        // when container is shutdown, zk is down and will throw execptions
                        syncZkConfigInfo(info);
                        if (SoaSystemEnvProperties.SOA_FREQ_LIMIT_ENABLE) {
                            syncZkFreqControl(info);
                        }
                        serviceInfoByName.put(serviceName, info);
                    } catch (Throwable e) {
                        LOGGER.error("ServerZk::getConfigData failed." + e.getMessage());
                        info = null;
                    }
                }
            }
        }
        return info;
    }

    /**
     * 获取 zookeeper 上的 限流规则 freqRule
     *
     * @return
     */
    private void syncZkFreqControl(ZkServiceInfo serviceInfo) {
        if (zk == null || !zk.getState().isConnected()) {
            LOGGER.warn(getClass() + "::syncZkFreqControl zk is not ready, status:"
                    + (zk == null ? null : zk.getState()));
            return;
        }
        try {
            byte[] data = zk.getData(FREQ_PATH + "/" + serviceInfo.serviceName(), this, null);
            processFreqRuleData(serviceInfo, data);
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error("获取freq 节点: {} 出现异常", serviceInfo.serviceName());
        }
    }

    /**
     * process zk data freqControl 限流规则信息
     *
     * @param serviceInfo
     * @param data
     * @return
     */
    private synchronized void processFreqRuleData(ZkServiceInfo serviceInfo, byte[] data) {
        try {
            String ruleData = new String(data, "utf-8");
            serviceInfo.freqControl(doParseRuleData(serviceInfo.serviceName(), ruleData));
        } catch (Exception e) {
            LOGGER.error("parser freq rule 信息 失败，请检查 rule data 写法是否正确!");
        }

    }

    /**
     * 解析 zookeeper 上 配置的 ruleData数据 为FreqControlRule对象
     *
     * @param ruleData data from zk node
     * @return
     */
    private ServiceFreqControl doParseRuleData(final String serviceName, final String ruleData) throws Exception {
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

    @Override
    public void process(WatchedEvent event) {
        LOGGER.warn("ServerZk::process, zkEvent: " + event);
        if (event.getPath() == null) {
            // when zk restart, a zkEvent is trigger: WatchedEvent state:SyncConnected type:None path:null
            // we should ignore this.
            LOGGER.warn("ServerZk::process Just ignore this event.");
            return;
        }
        String serviceName = event.getPath().substring(event.getPath().lastIndexOf("/") + 1);
        ZkServiceInfo serviceInfo = serviceInfoByName.get(serviceName);
        if (serviceInfo == null) {
            LOGGER.warn("ServerZk::process, no such service: " + serviceName + " Just ignore this event.");
            return;
        }

        switch (event.getType()) {
            case NodeDataChanged:
                if (event.getPath().startsWith(CONFIG_PATH)) {
                    syncZkConfigInfo(serviceInfo);
                } else if (event.getPath().startsWith(FREQ_PATH)) {
                    syncZkFreqControl(serviceInfo);
                }
                break;
            default:
                LOGGER.warn("ClientZkAgent::process Just ignore this event.");
                break;
        }
    }

    public void create(String path, String data, RegisterContext registerContext, boolean ephemeral) {
        AsyncCallback.StringCallback callback = ephemeral ? serverInfoCreateCallback : persistNodeCreateCallback;
        ZkUtils.create(path, data, registerContext, ephemeral, callback, zk);
    }
}
