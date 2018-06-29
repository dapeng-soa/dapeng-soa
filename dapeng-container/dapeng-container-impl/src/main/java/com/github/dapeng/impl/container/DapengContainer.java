package com.github.dapeng.impl.container;

import com.github.dapeng.api.AppListener;
import com.github.dapeng.api.Container;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.api.events.AppEvent;
import com.github.dapeng.api.events.AppEventType;
import com.github.dapeng.core.Application;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.ServiceInfo;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.impl.filters.FilterLoader;
import com.github.dapeng.impl.plugins.*;
import com.github.dapeng.impl.plugins.netty.NettyPlugin;
import com.github.dapeng.registry.zookeeper.ServerZkAgent;
import com.github.dapeng.registry.zookeeper.ServerZkAgentImpl;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class DapengContainer implements Container {

    private static final Logger LOGGER = LoggerFactory.getLogger(DapengContainer.class);
    private static final String RUN_MODE = System.getProperty("soa.run.mode", "plugin");
    private List<AppListener> appListeners = new Vector<>();
    private List<Application> applications = new Vector<>();
    private List<Plugin> plugins = new ArrayList<>();
    private List<Filter> filters = new ArrayList<>();
    private Map<ProcessorKey, SoaServiceDefinition<?>> processors = new ConcurrentHashMap<>();
    private Map<ProcessorKey, Application> applicationMap = new ConcurrentHashMap<>();
    private final List<ClassLoader> applicationCls;
    /**
     * 容器状态, 初始状态为STATUS_UNKNOWN
     */
    private static int status = STATUS_UNKNOWN;

    private final static CountDownLatch SHUTDOWN_SIGNAL = new CountDownLatch(1);

    public DapengContainer(List<ClassLoader> applicationCls) {
        this.applicationCls = applicationCls;
    }

    @Override
    public void registerAppListener(AppListener listener) {
        this.appListeners.add(listener);
    }

    @Override
    public void unregisterAppListener(AppListener listener) {
        this.appListeners.remove(listener);
    }

    @Override
    public void registerApplication(Application app) {
        LOGGER.info(getClass().getSimpleName() + "::registerApplication application[" + app.getClass().getSimpleName() + "]");
        this.applications.add(app);
        this.appListeners.forEach(i -> {
            try {
                i.appRegistered(new AppEvent(app, AppEventType.REGISTER));
            } catch (Exception e) {
                LOGGER.error(" Faild to handler appEvent. listener: {}, eventType: {}", i, AppEventType.REGISTER, e.getStackTrace());
            }
        });
    }

    @Override
    public void unregisterApplication(Application app) {
        LOGGER.info(getClass().getSimpleName() + "::unregisterApplication application[" + app.getClass().getSimpleName() + "]");
        this.applications.remove(app);
        this.appListeners.forEach(i -> {
            try {
                i.appUnRegistered(new AppEvent(app, AppEventType.UNREGISTER));
            } catch (Exception e) {
                LOGGER.error(" Faild to handler appEvent. listener: {}, eventType: {}", i, AppEventType.UNREGISTER, e.getStackTrace());
            }
        });
    }

    @Override
    public void registerPlugin(Plugin plugin) {
        LOGGER.info(getClass().getSimpleName() + "::registerPlugin plugin[" + plugin.getClass().getSimpleName() + "]");
        this.plugins.add(plugin);
    }

    @Override
    public void unregisterPlugin(Plugin plugin) {
        LOGGER.info(getClass().getSimpleName() + "::unregisterPlugin plugin[" + plugin.getClass().getSimpleName() + "]");
        this.plugins.remove(plugin);
    }

    @Override
    public List<Application> getApplications() {
        return this.applications;
    }


    @Override
    public List<Plugin> getPlugins() {
        return Collections.unmodifiableList(this.plugins);
    }

    @Override
    public Map<ProcessorKey, SoaServiceDefinition<?>> getServiceProcessors() {
        return this.processors;
    }

    @Override
    public void registerAppProcessors(Map<ProcessorKey, SoaServiceDefinition<?>> processors) {
        this.processors.putAll(processors);
    }

    @Override
    public Application getApplication(ProcessorKey key) {
        return applicationMap.get(key);
    }

    @Override
    public void registerAppMap(Map<ProcessorKey, Application> applicationMap) {
        this.applicationMap.putAll(applicationMap);
    }

    private Executor exector = initExecutor();

    private Executor initExecutor() {
        LOGGER.info(DapengContainer.class.getName()
                + "业务线程池初始化, 是否使用线程池[coreSize:" + SoaSystemEnvProperties.SOA_CORE_POOL_SIZE + "]:"
                + SoaSystemEnvProperties.SOA_CONTAINER_USETHREADPOOL);

        if (!SoaSystemEnvProperties.SOA_CONTAINER_USETHREADPOOL) {
            return command -> command.run();
        } else {
            ThreadPoolExecutor bizExector = (ThreadPoolExecutor) Executors.newFixedThreadPool(SoaSystemEnvProperties.SOA_CORE_POOL_SIZE,
                    new ThreadFactoryBuilder()
                            .setDaemon(true)
                            .setNameFormat("dapeng-container-biz-pool-%d")
                            .build());
            //预热所有的业务线程
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(DapengContainer.class.getName() + " 预热业务线程池[" + SoaSystemEnvProperties.SOA_CORE_POOL_SIZE + "]");
                bizExector.prestartAllCoreThreads();
            }

            return bizExector;
        }
    }

    @Override
    public Executor getDispatcher() {
        return exector;
    }


    @Override
    public void registerFilter(Filter filter) {
        LOGGER.info(getClass().getSimpleName() + "::registerFilter filter[" + filter.getClass().getSimpleName() + "]");
        this.filters.add(filter);
    }

    @Override
    public void unregisterFilter(Filter filter) {
        LOGGER.info(getClass().getSimpleName() + "::unregisterFilter filter[" + filter.getClass().getSimpleName() + "]");
        this.filters.remove(filter);
    }

    @Override
    public List<Filter> getFilters() {
        return ImmutableList.copyOf(this.filters);
    }

    @Override
    public void startup() {
        LOGGER.info(getClass().getSimpleName() + "::startup begin");
        status = STATUS_CREATING;
        //3. 初始化appLoader,dapengPlugin 应该用serviceLoader的方式去加载
        Plugin springAppLoader = new SpringAppLoader(this, applicationCls);
        Plugin zookeeperPlugin = new ZookeeperRegistryPlugin(this);
        Plugin taskSchedulePlugin = new TaskSchedulePlugin(this);
        Plugin nettyPlugin = new NettyPlugin(this);
        Plugin mbeanAgentPlugin = new MbeanAgentPlugin(this);
        Plugin gcMonitorPlugin = new GcMonitorPlugin(this);
        //add messagePlugin
//        Plugin messagePlugin = new KafkaMessagePlugin();
        // TODO
        if (!"plugin".equals(RUN_MODE)) {
            Plugin logbackPlugin = new LogbackPlugin();
            registerPlugin(logbackPlugin);
        }
        registerPlugin(nettyPlugin);
        registerPlugin(zookeeperPlugin);
        registerPlugin(springAppLoader);
        registerPlugin(taskSchedulePlugin);
        registerPlugin(mbeanAgentPlugin);
        registerPlugin(gcMonitorPlugin);

        //add messagePlugin
//        registerPlugin(messagePlugin);

        if ("plugin".equals(RUN_MODE)) {
            Plugin apiDocPlugin = new ApiDocPlugin(this);
            registerPlugin(apiDocPlugin);
        }

        //4.启动Apploader， plugins
        getPlugins().forEach(Plugin::start);

        // register Filters
        new FilterLoader(this, applicationCls);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.warn("Container graceful shutdown begin.");
            status = STATUS_SHUTTING;
            // fixme not so graceful
            getPlugins().stream().filter(plugin -> (plugin instanceof ZookeeperRegistryPlugin)
                    || (plugin instanceof TaskSchedulePlugin)).forEach(Plugin::stop);
            getPlugins().stream().filter(plugin -> !(plugin instanceof ZookeeperRegistryPlugin)
                    && !(plugin instanceof TaskSchedulePlugin)).forEach(Plugin::stop);
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
            SHUTDOWN_SIGNAL.countDown();
            LOGGER.warn("Container graceful shutdown end.");
        }));

        try {
            LOGGER.warn(getClass().getSimpleName() + "::startup end");
            status = STATUS_RUNNING;
            SHUTDOWN_SIGNAL.await();
            LOGGER.warn(getClass().getSimpleName() + "::startup quit");
            status = STATUS_DOWN;
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void resume() {
        if (status == STATUS_PAUSE) {
            ServerZkAgent serverZkAgent = ServerZkAgentImpl.getInstance();
            for (Application application : applicationMap.values()) {
                for (ServiceInfo serviceInfo : application.getServiceInfos()) {
                    serverZkAgent.resume(serviceInfo.serviceName, serviceInfo.version);
                }
            }
            status = STATUS_RUNNING;
        }
    }

    /**
     * 服务集群符合以下条件的实例存在的话,将会执行暂停操作:
     * 1. 扣除本节点实例
     * 2. 扣除不兼容的版本
     */
    @Override
    public void pause() {
        ServerZkAgent serverZkAgent = ServerZkAgentImpl.getInstance();
        // 是否存在单节点实例
        boolean singleNodeExists = false;
        if (!applicationMap.isEmpty()) {
            for (Application application : applicationMap.values()) {
                if (!application.getServiceInfos().isEmpty()) {
                    ServiceInfo serviceInfo = application.getServiceInfos().get(0);
                    List<RuntimeInstance> runtimeInstances = serverZkAgent.getRuntimeInstances(serviceInfo.serviceName);
                    if (runtimeInstances.stream().filter(runtimeInstance ->
                                    (!(runtimeInstance.ip.equals(SoaSystemEnvProperties.SOA_CONTAINER_IP))
                                            && (runtimeInstance.port != SoaSystemEnvProperties.SOA_CONTAINER_PORT))
                                            || DapengUtil.checkVersionCompatibility(serviceInfo.version, runtimeInstance.version))
                            .collect(Collectors.toList()).isEmpty()) {
                        LOGGER.warn("No more other nodes, should not pause current node:" + serviceInfo.toString());
                        singleNodeExists = true;
                        break;
                    }
                }
            }
        }
        if (!singleNodeExists) {
            status =STATUS_PAUSE;
            for (Application application : applicationMap.values()) {
                for (ServiceInfo serviceInfo : application.getServiceInfos()) {
                    serverZkAgent.pause(serviceInfo.serviceName, serviceInfo.version);
                }
            }
        }
}

    @Override
    public int status() {
        return status;
    }

    public static InputStream loadInputStreamInClassLoader(String path) throws FileNotFoundException {
        if ("sbt".equals(RUN_MODE) || "maven".equals(RUN_MODE))
            return DapengContainer.class.getClassLoader().getResourceAsStream(path);
        return new FileInputStream(new File(System.getProperty("soa.base"), "conf/" + path));
    }


}
