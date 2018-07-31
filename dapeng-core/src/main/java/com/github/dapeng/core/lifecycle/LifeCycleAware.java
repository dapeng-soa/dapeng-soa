package com.github.dapeng.core.lifecycle;

/**
 * 提供给业务的lifecycle接口，四种状态
 * @author hui
 * @date 2018/7/26 11:21
 *
 */
public interface LifeCycleAware {

    /**
     * 容器启动时回调方法
     */
    void onStart(LifeCycleEvent event);

    /**
     * 容器暂停时回调方法
     */
    void onPause(LifeCycleEvent event);

    /**
     * 容器内某服务master状态改变时回调方法
     * 业务实现方可自行判断具体的服务是否是master, 从而执行相应的逻辑
     */
    void onMasterChange(LifeCycleEvent event);

    /**
     * 容器关闭
     */
    void onStop(LifeCycleEvent event);

    /**
     * 配置变化
     */

}
