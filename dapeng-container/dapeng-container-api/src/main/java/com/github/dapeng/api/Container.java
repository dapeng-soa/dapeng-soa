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
 *
 * 所有的组件的注册，卸载动作都应该由Container来负责，
 */
public interface Container {
    /**
     * 容器状态
     */
    int STATUS_UNKNOWN = 0;
    int STATUS_CREATING = 1;
    int STATUS_RUNNING = 2;
    int STATUS_SHUTTING = 3;
    int STATUS_DOWN = 4;

    /**
     * 注册应用程序监听器，
     * @param listener
     */
    public void registerAppListener(AppListener listener);

    /**
     * 卸载用用程序监听器
     * @param listener
     */
    public void unregisterAppListener(AppListener listener);

    /**
     * 注册应用程序（保存容器具体的应用信息）
     * @param app
     */
    public void registerApplication(Application app);

    /**
     * 卸载应用程序
     * @param app
     */
    public void unregisterApplication(Application app);

    /**
     * 注册插件(like: Zookeeper,netty..etc.)
     * @param plugin
     */
    public void registerPlugin(Plugin plugin);

    /**
     * 卸载插件
     * @param plugin
     */
    public void unregisterPlugin(Plugin plugin);

    /**
     * 注册Filter(like: monitor)
     */
    public void registerFilter(Filter filter);

    /**
     * 卸载Filter
     * @param filter
     */
    public void unregisterFilter(Filter filter);

    /**
     * 获取应用程序的相关信息
     * @return
     */
    public List<Application> getApplications();

    public List<Plugin> getPlugins();

    Map<ProcessorKey, SoaServiceDefinition<?>> getServiceProcessors();

    // fixme @Deprecated
    void registerAppProcessors(Map<ProcessorKey, SoaServiceDefinition<?>> processors);

    public Application getApplication(ProcessorKey key);

    // fixme @Deprecated
    public void registerAppMap(Map<ProcessorKey,Application> applicationMap);

    public Executor getDispatcher();

    public List<Filter> getFilters();

    void startup();

    /**
     * 0:unknow;
     * 1:creating;
     * 2:running;
     * 3:shutting
     * 4:down
     *
     * @return status of container
     */
    int status();
}
