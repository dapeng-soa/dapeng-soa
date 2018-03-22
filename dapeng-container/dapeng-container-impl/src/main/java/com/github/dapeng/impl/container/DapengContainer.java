package com.github.dapeng.impl.container;

import com.github.dapeng.api.AppListener;
import com.github.dapeng.api.Container;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.api.events.AppEvent;
import com.github.dapeng.api.events.AppEventType;
import com.github.dapeng.core.Application;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.impl.filters.FilterLoader;
import com.github.dapeng.impl.plugins.*;
import com.github.dapeng.impl.plugins.netty.NettyPlugin;
import com.github.dapeng.util.SoaSystemEnvProperties;
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
        LOGGER.info(getClass().getSimpleName() + ":: application[" + app.getClass().getSimpleName() + "] registered");
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
        LOGGER.info(getClass().getSimpleName() + ":: application[" + app.getClass().getSimpleName() + "] unregistered");
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
        LOGGER.info(getClass().getSimpleName() + ":: plugin[" + plugin.getClass().getSimpleName() + "] registered");
        this.plugins.add(plugin);
    }

    @Override
    public void unregisterPlugin(Plugin plugin) {
        LOGGER.info(getClass().getSimpleName() + ":: plugin[" + plugin.getClass().getSimpleName() + "] unregistered");
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

    private static class ExectorFactory {
        private static Executor exector = initExecutor();

        static Executor initExecutor() {
            LOGGER.info(DapengContainer.class.getName()
                    + "业务线程池初始化, 是否使用线程池:"
                    + SoaSystemEnvProperties.SOA_CONTAINER_USETHREADPOOL);

            if (!SoaSystemEnvProperties.SOA_CONTAINER_USETHREADPOOL) {
                return command -> command.run();
            } else {
                ThreadPoolExecutor bizExector = (ThreadPoolExecutor)Executors.newFixedThreadPool(SoaSystemEnvProperties.SOA_CORE_POOL_SIZE,
                        new ThreadFactoryBuilder()
                                .setDaemon(true)
                                .setNameFormat("dapeng-container-biz-pool-%d")
                                .build());
                if ("native".equals(RUN_MODE)) {
                    //容器模式下,预热所有的业务线程
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(DapengContainer.class.getName() + " 预热业务线程池[" + SoaSystemEnvProperties.SOA_CORE_POOL_SIZE + "]");
                    }
                    bizExector.prestartAllCoreThreads();
                }

                return bizExector;
            }
        }
    }

    @Override
    public Executor getDispatcher() {
        return ExectorFactory.exector;
    }


    @Override
    public void registerFilter(Filter filter) {
        LOGGER.info(getClass().getSimpleName() + " plugin[" + filter.getClass().getSimpleName() + "] registered");
        this.filters.add(filter);
    }

    @Override
    public void unregisterFilter(Filter filter) {
        LOGGER.info(getClass().getSimpleName() + " plugin[" + filter.getClass().getSimpleName() + "] registered");
        this.filters.remove(filter);
    }

    @Override
    public List<Filter> getFilters() {
        return ImmutableList.copyOf(this.filters);
    }

    @Override
    public void startup() {
        LOGGER.info(getClass().getSimpleName() + "::startup begin");
        //3. 初始化appLoader,dapengPlugin 应该用serviceLoader的方式去加载
        Plugin springAppLoader = new SpringAppLoader(this, applicationCls);
        Plugin zookeeperPlugin = new ZookeeperRegistryPlugin(this);
        Plugin taskSchedulePlugin = new TaskSchedulePlugin(this);
        Plugin nettyPlugin = new NettyPlugin(this);
        Plugin mbeanAgentPlugin = new MbeanAgentPlugin();
        //add messagePlugin
//        Plugin messagePlugin = new KafkaMessagePlugin();
        // TODO
        if (!"plugin".equals(RUN_MODE)) {
            Plugin logbackPlugin = new LogbackPlugin();
            registerPlugin(logbackPlugin);
        }

        registerPlugin(zookeeperPlugin);
        registerPlugin(springAppLoader);
        registerPlugin(taskSchedulePlugin);
        registerPlugin(nettyPlugin);
        registerPlugin(mbeanAgentPlugin);

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
            getPlugins().forEach(Plugin::stop);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
            SHUTDOWN_SIGNAL.countDown();
            LOGGER.warn("Container graceful shutdown end.");
        }));

        try {
            LOGGER.warn(getClass().getSimpleName() + "::startup end");
            SHUTDOWN_SIGNAL.await();
            LOGGER.warn(getClass().getSimpleName() + "::quit");
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static InputStream loadInputStreamInClassLoader(String path) throws FileNotFoundException {
        if ("sbt".equals(RUN_MODE) || "maven".equals(RUN_MODE))
            return DapengContainer.class.getClassLoader().getResourceAsStream(path);
        return new FileInputStream(new File(System.getProperty("soa.base"), "conf/" + path));
    }


}
