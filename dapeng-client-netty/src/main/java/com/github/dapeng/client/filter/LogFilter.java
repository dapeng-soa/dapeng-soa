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
package com.github.dapeng.client.filter;


import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.InvocationInfoImpl;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static com.github.dapeng.core.helper.IPUtils.transferIp;

/**
 * @author Ever
 * @date 2018-04-11
 */
public class LogFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogFilter.class);

    @Override
    public void onEntry(FilterContext filterContext, FilterChain next) throws SoaException {
        try {
            InvocationContextImpl invocationContext = (InvocationContextImpl) filterContext.getAttribute("context");
            filterContext.setAttribute("startTime", System.currentTimeMillis());

            InvocationInfoImpl invocationInfo = new InvocationInfoImpl();
            invocationContext.lastInvocationInfo(invocationInfo);

            if (!invocationContext.sessionTid().isPresent()) {
                if (TransactionContext.hasCurrentInstance()
                        && TransactionContext.Factory.currentInstance().sessionTid().isPresent()) {
                    invocationContext.sessionTid(TransactionContext.Factory.currentInstance().sessionTid().get());
                } else {
                    invocationContext.sessionTid(DapengUtil.generateTid());
                }
            }

            String logLevel = invocationContext.cookie(SoaSystemEnvProperties.THREAD_LEVEL_KEY);

            if (logLevel != null) {
                MDC.put(SoaSystemEnvProperties.THREAD_LEVEL_KEY, logLevel);
            }

            MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, invocationContext.sessionTid().map(DapengUtil::longToHexStr).orElse("0"));

            String infoLog = "request[seqId:" + invocationContext.seqId() + ", server:" + filterContext.getAttribute("serverInfo") + "]:"
                    + "service[" + invocationContext.serviceName()
                    + "]:version[" + invocationContext.versionName()
                    + "]:method[" + invocationContext.methodName() + "]";

            LOGGER.info(getClass().getSimpleName() + "::onEntry," + infoLog);
        } finally {
            next.onEntry(filterContext);
        }
    }

    @Override
    public void onExit(FilterContext filterContext, FilterChain prev) {
        try {
            InvocationContextImpl invocationContext = (InvocationContextImpl) filterContext.getAttribute("context");
            Long startTime = (Long) filterContext.getAttribute("startTime");
            InvocationInfoImpl invocationInfo = (InvocationInfoImpl) invocationContext.lastInvocationInfo();
            invocationInfo.serviceTime(System.currentTimeMillis() - startTime);

            String infoLog = "response[seqId:" + invocationContext.seqId() + ", respCode:" + invocationInfo.responseCode() + ", server: " + filterContext.getAttribute("serverInfo") + "]:"
                    + "service[" + invocationContext.serviceName()
                    + "]:version[" + invocationContext.versionName()
                    + "]:method[" + invocationContext.methodName()
                    + "] cost[total:" + invocationInfo.serviceTime()
                    + ", calleeTime1:" + invocationInfo.calleeTime1()
                    + ", calleeTime2:" + invocationInfo.calleeTime2()
                    + ", calleeIp: " + transferIp(invocationInfo.calleeIp());
            if (SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE.equals(invocationInfo.responseCode())) {
                LOGGER.info(getClass().getSimpleName() + "::onExit," + infoLog);
            } else {
                LOGGER.error(getClass().getSimpleName() + "::onExit," + infoLog);
            }
        } finally {
            try {
                prev.onExit(filterContext);
            } catch (TException e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                // 如果在服务里面, 那么不清理MDC
                if (!TransactionContext.hasCurrentInstance()) {
                    MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
                    MDC.remove(SoaSystemEnvProperties.THREAD_LEVEL_KEY);
                }
            }
        }
    }
}
