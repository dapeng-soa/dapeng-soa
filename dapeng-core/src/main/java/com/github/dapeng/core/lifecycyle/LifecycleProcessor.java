package com.github.dapeng.core.lifecycyle;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: hui
 * @Date: 2018/7/26 0026 9:36
 * @Description:
 */

public class LifecycleProcessor  {

    private static LifecycleProcessor instance = new LifecycleProcessor();

    public  Map<String,Lifecycle> lifecycles = new HashMap<>();

    private LifecycleProcessor(){}

    public static LifecycleProcessor getInstance(){
        return instance;
    }

    /**
     *对业务不同事件的响应
     */
    public void onLifecycleEvent(LifecycleEvent event){
        switch (event){
            case START:
                for (Map.Entry<String, Lifecycle> entry : lifecycles.entrySet()) {
                    Lifecycle bean = entry.getValue();
                    bean.onStart();
                }
                break;
            case PAUSE:
                for (Map.Entry<String, Lifecycle> entry : lifecycles.entrySet()) {
                    Lifecycle bean = entry.getValue();
                    bean.onPause();
                }
                break;
            case MASTER_CHANGE:
                for (Map.Entry<String, Lifecycle> entry : lifecycles.entrySet()) {
                    Lifecycle bean = entry.getValue();
                    bean.onMasterChange();
                }
                break;
            case STOP:
                for (Map.Entry<String, Lifecycle> entry : lifecycles.entrySet()) {
                    Lifecycle bean = entry.getValue();
                    bean.onStop();
                }
                break;
        }

    }
}

