/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.util;

import com.github.dapeng.client.netty.ClientRefManager;
import com.github.dapeng.cookie.CookieExecutor;
import com.github.dapeng.cookie.CookieRule;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.SoaCode;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.registry.zookeeper.ZkServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class InvocationContextUtils {
    private final static Logger logger = LoggerFactory.getLogger(InvocationContextUtils.class);
    private final static ClientRefManager clientRefManager = ClientRefManager.getInstance();
    /**
     * 封装InvocationContext， 把路由需要用到的东西放到InvocationContext中。
     *
     * @param context
     * @param serviceName
     * @param method
     */
    public static void capsuleContext(InvocationContextImpl context, final String serviceName,
                                      final String version, final String method) throws SoaException {
        context.serviceName(serviceName);
        context.methodName(method);
        context.versionName(version);

        InvocationContextImpl.InvocationContextProxy invocationCtxProxy = InvocationContextImpl.Factory.getInvocationContextProxy();

        if (invocationCtxProxy != null) {
            context.userIp(invocationCtxProxy.userIp().orElse(null));
            context.customerId(invocationCtxProxy.customerId().orElse(null));
            context.operatorId(invocationCtxProxy.operatorId().orElse(null));
            context.callerMid(invocationCtxProxy.callerMid().orElse(null));
            context.cookies(invocationCtxProxy.cookies());
        }

        //set cookies if cookie injection rule matches
        injectCookie(serviceName, context);
    }

    /**
     * cookie injection
     */
    private static void injectCookie(String service, InvocationContextImpl context) throws SoaException {
        ZkServiceInfo zkServiceInfo = clientRefManager.serviceInfo(service);

        if (zkServiceInfo == null) {
            logger.error(InvocationContextUtils.class + "::injectCookie serviceInfo not found: " + service);
            throw new SoaException(SoaCode.NotFoundServer, "服务 [ " + service + " ] 无可用实例");
        }

        List<CookieRule> cookieRules = zkServiceInfo.cookieRules();

        if (cookieRules == null || cookieRules.size() == 0) {
            logger.debug("cookie rules 信息为空或size为0, 跳过 cookie injection");
        } else {
            CookieExecutor.injectCookies(context, cookieRules);
        }
    }
}
