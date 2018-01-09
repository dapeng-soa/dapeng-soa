package com.github.dapeng.impl.container;

import com.github.dapeng.api.*;
import com.github.dapeng.api.events.AppEvent;
import com.github.dapeng.api.events.AppEventType;
import com.github.dapeng.core.Application;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.impl.plugins.ApiDocPlugin;
import com.github.dapeng.impl.plugins.SpringAppLoader;
import com.github.dapeng.impl.plugins.TaskSchedulePlugin;
import com.github.dapeng.impl.plugins.ZookeeperRegistryPlugin;
import com.github.dapeng.impl.plugins.netty.NettyPlugin;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DapengContainer implements Container {

    private static final Logger logger = LoggerFactory.getLogger(DapengContainer.class);
    private List<AppListener> appListeners = new Vector<>();
    private List<Application> applications = new Vector<>();
    private List<Plugin> plugins = new ArrayList<>();
    private Map<ProcessorKey, SoaServiceDefinition<?>> processors = new ConcurrentHashMap<>();
    private Map<ProcessorKey,Application>  applicationMap = new ConcurrentHashMap<>();
    private final List<ClassLoader> applicationCls;

    private final static CountDownLatch shutdownSignal = new CountDownLatch(1);

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
        this.applications.add(app);
        this.appListeners.forEach(i -> {
            try {
                i.appRegistered(new AppEvent(app, AppEventType.REGISTER));
            } catch (Exception e) {
                logger.error(" Faild to handler appEvent. listener: {}, eventType: {}",i, AppEventType.REGISTER , e.getStackTrace());
            }
        });
    }

    @Override
    public void unregisterApplication(Application app) {
        this.applications.remove(app);
        this.appListeners.forEach(i -> {
            try {
                i.appUnRegistered(new AppEvent(app, AppEventType.UNREGISTER));
            } catch (Exception e) {
                logger.error(" Faild to handler appEvent. listener: {}, eventType: {}",i, AppEventType.UNREGISTER , e.getStackTrace());
            }
        });
    }

    @Override
    public void registerPlugin(Plugin plugin) {
        this.plugins.add(plugin);
    }

    @Override
    public void unregisterPlugin(Plugin plugin) {
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

    @Override
    public Executor getDispatcher() {
        if(!SoaSystemEnvProperties.SOA_CONTAINER_USETHREADPOOL){
            return command -> command.run();
        }
        else {
            return Executors.newFixedThreadPool(SoaSystemEnvProperties.SOA_CORE_POOL_SIZE);
        }
    }

    @Override
    public List<Filter> getFilters() {
        return new ArrayList<>(); //TODO
    }

    @Override
    public void startup() {
        //3. 初始化appLoader,dapengPlugin 应该用serviceLoader的方式去加载
        Plugin springAppLoader = new SpringAppLoader(this,applicationCls);
        Plugin apiDocPlugin = new ApiDocPlugin(this);
        Plugin zookeeperPlugin = new ZookeeperRegistryPlugin(this);
        Plugin taskSchedulePlugin = new TaskSchedulePlugin(this);
        Plugin nettyPlugin = new NettyPlugin(this);

        //ApiDocPlugin优先启动(为了Spring触发注册事件时，ServiceCache已经实例化，能收到消息)
        registerPlugin(springAppLoader);
        registerPlugin(zookeeperPlugin);
        registerPlugin(taskSchedulePlugin);
        registerPlugin(nettyPlugin);
        registerPlugin(apiDocPlugin);


        //4.启动Apploader， plugins
        getPlugins().forEach(Plugin::start);

        Runtime.getRuntime().addShutdownHook( new Thread( ()->{
            getPlugins().forEach(Plugin::stop);
            shutdownSignal.countDown();
        } ) );

        try {
            shutdownSignal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
