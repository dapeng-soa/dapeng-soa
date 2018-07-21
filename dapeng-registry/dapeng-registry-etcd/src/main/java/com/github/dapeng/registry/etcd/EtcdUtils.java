package com.github.dapeng.registry.etcd;

import com.coreos.jetcd.Watch;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.watch.WatchEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * desc: EtcdUtils
 *
 * @author hz.lei
 * @since 2018年07月19日 下午7:07
 */
public class EtcdUtils {
    private static final Logger logger = LoggerFactory.getLogger(EtcdUtils.class);

    private static final Pattern MODE_PATTERN = Pattern.compile("([0-9]+)n\\+(([0-9]+)..)?([0-9]+)");

    private static Pattern RUNTIME_INSTANCE_PATTERN = Pattern.compile("(/[a-zA-Z0-9]+/[a-zA-Z0-9]+/[a-zA-Z0-9]+)/([a-zA-Z0-9._]+)/([a-zA-Z0-9.:]+)");


    private static ExecutorService executorService = Executors.newCachedThreadPool();


    /**
     * etcd watcher
     *
     * @param watch
     * @param key
     * @param callback
     */
    public static void etcdWatch(Watch watch, String key, Boolean usePrefix, WatchCallback callback) {
        executorService.execute(() -> {
            try {
                Watch.Watcher watcher;
                if (usePrefix) {
                    watcher = watch.watch(ByteSequence.fromString(key), WatchOption.newBuilder().withPrefix(ByteSequence.fromString(key)).build());
                } else {
                    watcher = watch.watch(ByteSequence.fromString(key));
                }
                List<WatchEvent> events = watcher.listen().getEvents();
                callback.callback(events);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    /**
     *
     */
    public static String getInstData(String instPath) {
        Matcher matcher = RUNTIME_INSTANCE_PATTERN.matcher(instPath);
        if (matcher.matches()) {
            String instData = matcher.group(3);
            return instData;
        }
        return null;
    }


    public static void processEtcdConfig(String value) {
        //todo

       /* try {
            List<KeyValue> kvs = data.getKvs();

            kvs.forEach(kv -> {

            });






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
                }
            }
            LOGGER.info("get config from {} with data [{}]", zkInfo.service, configData);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }*/
    }
}
