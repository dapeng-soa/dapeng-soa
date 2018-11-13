package com.github.dapeng.registry.zookeeper;


import com.github.dapeng.cookie.CookieRule;
import com.github.dapeng.router.Route;

import java.util.List;

/**
 * @author lihuimin
 * @date 2017/12/24
 */
public interface ClientZkAgent {

    void start();

    void stop();

    void syncService(ZkServiceInfo zkInfo);

    void cancelSyncService(ZkServiceInfo zkInfo);


    List<Route> getRoutes(String service);


    List<CookieRule> getCookieRules(String service);


}