package com.github.dapeng.impl.container;

import com.github.dapeng.api.AppListener;
import com.github.dapeng.api.Container;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.api.events.AppEvent;
import com.github.dapeng.api.events.AppEventType;
import com.github.dapeng.api.healthcheck.DoctorFactory;
import com.github.dapeng.api.lifecycle.LifecycleProcessor;
import com.github.dapeng.api.lifecycle.LifecycleProcessorFactory;
import com.github.dapeng.core.Application;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.core.lifecycle.LifeCycleEvent;
import com.github.dapeng.impl.filters.FilterLoader;
import com.github.dapeng.impl.plugins.*;
import com.github.dapeng.impl.plugins.netty.NettyPlugin;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dapeng.core.helper.SoaSystemEnvProperties.SOA_SHUTDOWN_TIMEOUT;

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

    private static AtomicInteger requestCounter = new AtomicInteger(0);

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
        //TODO: should return the bean copy..not the real one.
        return this.plugins;
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
            return Runnable::run;
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
        DoctorFactory.createDoctor(this.getClass().getClassLoader());
        LifecycleProcessorFactory.createLifecycleProcessor(this.getClass().getClassLoader());
        //3. 初始化appLoader,dapengPlugin 应该用serviceLoader的方式去加载
        Plugin springAppLoader = new SpringAppLoader(this, applicationCls);
        Plugin zookeeperPlugin = new ZookeeperRegistryPlugin(this);
        Plugin taskSchedulePlugin = new TaskSchedulePlugin(this);
        Plugin nettyPlugin = new NettyPlugin(this);
        Plugin mbeanAgentPlugin = new MbeanAgentPlugin(this);
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

        //add messagePlugin
//        registerPlugin(messagePlugin);

        if ("plugin".equals(RUN_MODE)) {
            Plugin apiDocPlugin = new ApiDocPlugin(this);
            registerPlugin(apiDocPlugin);
        }

        //4.启动Apploader， plugins
        getPlugins().forEach(Plugin::start);

        final LifecycleProcessor lifecycleProcessor = LifecycleProcessorFactory.getLifecycleProcessor();
        //启动LifeCycle start
        lifecycleProcessor.onLifecycleEvent(new LifeCycleEvent(LifeCycleEvent.LifeCycleEventEnum.START));

        // register Filters
        new FilterLoader(this, applicationCls);

        Runtime.getRuntime().addShutdownHook(new Thread("container-shutdown-hook-thread") {
            @Override
            public void run() {
                LOGGER.warn("Container graceful shutdown begin.");
                status = STATUS_SHUTTING;
                // fixme not so graceful
                getPlugins().stream().filter(plugin -> plugin instanceof ZookeeperRegistryPlugin).forEach(Plugin::stop);
                lifecycleProcessor.onLifecycleEvent(new LifeCycleEvent(LifeCycleEvent.LifeCycleEventEnum.STOP));

                //重试3次，保证容器内请求已完成
                retryCompareCounter();

                Lists.reverse(getPlugins()).stream().filter(plugin -> !(plugin instanceof ZookeeperRegistryPlugin)).forEach(Plugin::stop);
                SHUTDOWN_SIGNAL.countDown();
                LOGGER.warn("Container graceful shutdown end.");
            }
        });

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
    public int status() {
        return status;
    }

    @Override
    public AtomicInteger requestCounter() {
        return requestCounter;
    }

    public void retryCompareCounter() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Retry to ensure requests processing is complete");
        }

        LOGGER.warn("容器内尚余[" + requestCounter.get() + "]个请求还未处理..."
                + (requestCounter.get() > 0 ? "现在最多等待[" + SOA_SHUTDOWN_TIMEOUT + "ms]" : ""));

        long sleepTime = 2000;
        long retry = SOA_SHUTDOWN_TIMEOUT / sleepTime;

        do {
            if (requestCounter.intValue() <= 0) {
                return;
            } else {
                try {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("requests haven't been processed  completely, sleep " + sleepTime + "ms");
                    }
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        } while (--retry > 0);

        if (requestCounter.intValue() != 0) {
            LOGGER.warn(retry + "次等待共[" + SOA_SHUTDOWN_TIMEOUT + "ms]之后，容器内尚余[" + requestCounter.get() + "]个请求还未处理完，容器即将关闭...");
        }
    }

    public static InputStream loadInputStreamInClassLoader(String path) throws FileNotFoundException {
        if ("sbt".equals(RUN_MODE) || "maven".equals(RUN_MODE))
            return DapengContainer.class.getClassLoader().getResourceAsStream(path);
        return new FileInputStream(new File(System.getProperty("soa.base"), "conf/" + path));
    }


}
