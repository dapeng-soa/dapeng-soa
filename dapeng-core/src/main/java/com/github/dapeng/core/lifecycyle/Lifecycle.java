package com.github.dapeng.core.lifecycyle;

/**
 * @Author: hui
 * @Date: 2018/7/26 0026 11:21
 * @Description: 提供给业务的lifecycle接口，四种状态
 */
public interface Lifecycle {

    void onStart();

    void onPause();

    void onMasterChange();

    void onStop();

}
