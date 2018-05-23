package com.github.dapeng.api;

import com.github.dapeng.core.Application;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.filter.Filter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 大鹏容器的主结构，负责管理容器相关的监听器，插件，应用程序。
 * <p>
 * 所有的组件的注册，卸载动作都应该由Container来负责，
 */
public interface Container {

    /**
     * 注册应用程序监听器，
     *
     * @param listener
     */
    void registerAppListener(AppListener listener);

    /**
     * 卸载用用程序监听器
     *
     * @param listener
     */
    void unregisterAppListener(AppListener listener);

    /**
     * 注册应用程序（保存容器具体的应用信息）
     *
     * @param app
     */
    void registerApplication(Application app);

    /**
     * 卸载应用程序
     *
     * @param app
     */
    void unregisterApplication(Application app);

    /**
     * 注册插件(like: Zookeeper,netty..etc.)
     *
     * @param plugin
     */
    void registerPlugin(Plugin plugin);

    /**
     * 卸载插件
     *
     * @param plugin
     */
    void unregisterPlugin(Plugin plugin);

    /**
     * 注册Filter(like: monitor)
     */
    void registerFilter(Filter filter);

    /**
     * 卸载Filter
     *
     * @param filter
     */
    void unregisterFilter(Filter filter);

    /**
     * 获取应用程序的相关信息
     *
     * @return
     */
    List<Application> getApplications();

    List<Plugin> getPlugins();

    Map<ProcessorKey, SoaServiceDefinition<?>> getServiceProcessors();

    void registerAppProcessors(Map<ProcessorKey, SoaServiceDefinition<?>> processors);

    Application getApplication(ProcessorKey key);

    void registerAppMap(Map<ProcessorKey, Application> applicationMap);

    Executor getDispatcher();

    List<Filter> getFilters();

    void startup();
}
