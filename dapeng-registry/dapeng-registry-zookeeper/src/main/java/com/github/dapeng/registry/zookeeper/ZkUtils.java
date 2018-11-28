package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.FreqControlRule;
import com.github.dapeng.core.ServiceFreqControl;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.helper.IPUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

import static com.github.dapeng.core.SoaCode.FreqConfigError;

public class ZkUtils {
    private final static Logger LOGGER = LoggerFactory.getLogger(ZkUtils.class);

    /**
     * 递归节点创建
     */
    public static void create(String path, String data, RegisterContext context, boolean ephemeral, AsyncCallback.StringCallback callback, ZooKeeper zkClient) {

        int i = path.lastIndexOf("/");
        if (i > 0) {
            String parentPath = path.substring(0, i);
            //判断父节点是否存在...
            if (!exists(parentPath, zkClient)) {
                create(parentPath, "", null, false, null, zkClient);
            }
        }
        if (ephemeral) {
            createEphemeral(path + ":", data, context, callback, zkClient);
        } else {
            createPersistent(path, data, callback, zkClient);
        }
    }

    /**
     * 异步添加serverInfo,为临时有序节点，如果server挂了就木有了
     */
    public static void createEphemeral(String path, String data, RegisterContext context, AsyncCallback.StringCallback callback, ZooKeeper zkClient) {
        // 如果存在重复的临时节点， 删除之
        int i = path.lastIndexOf("/");
        if (i > 0) {
            String parentPath = path.substring(0, i);
            try {
                List<String> childNodes = zkClient.getChildren(parentPath, false);
                String _path = path.substring(i + 1);
                for (String nodeName : childNodes) {
                    if (nodeName.startsWith(_path)) {
                        zkClient.delete(parentPath + "/" + nodeName, -1);
                    }
                }
            } catch (KeeperException | InterruptedException e) {
                LOGGER.error("ServerZk::createEphemeral delete exist nodes failed", e);
            }
        }
        //serverInfoCreateCallback
        zkClient.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL, callback, context);
    }

    /**
     * 异步添加持久化的节点
     *
     * @param path
     * @param data
     */
    public static void createPersistent(String path, String data, AsyncCallback.StringCallback callback, ZooKeeper zkClient) {
        if (!exists(path, zkClient)) {
            zkClient.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, callback, data);
        }
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


    /**
     * 检查节点是否存在
     */
    public static boolean exists(String path, ZooKeeper zkClient) {
        try {
            Stat exists = zkClient.exists(path, false);
            return exists != null;
        } catch (Throwable t) {
            LOGGER.error(ZkUtils.class + "::exists: " + t.getMessage(), t);
        }
        return false;
    }
}
