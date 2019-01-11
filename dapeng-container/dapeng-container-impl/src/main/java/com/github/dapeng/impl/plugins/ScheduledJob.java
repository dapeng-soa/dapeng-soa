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
package com.github.dapeng.impl.plugins;

import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.core.Application;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.impl.listener.TaskMonitorDataReportUtils;
import com.github.dapeng.impl.plugins.netty.MdcCtxInfoUtil;
import com.google.common.base.Stopwatch;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author tangliu
 * @date 2016/8/17
 * @DisallowConcurrentExecution 的主要作用是quartz单个任务的串行机制
 */
@DisallowConcurrentExecution
public class ScheduledJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger("container.scheduled.task");

    @SuppressWarnings("unchecked")
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap data = context.getJobDetail().getJobDataMap();
        String serviceName = data.getString("serviceName");
        String versionName = data.getString("versionName");

        Stopwatch stopwatch = Stopwatch.createStarted();
        /**
         * 添加sessionTid
         */
        String sessionTid = TaskMonitorDataReportUtils.setSessionTid(InvocationContextImpl.Factory.currentInstance());

        logger.info("定时任务({})开始执行", context.getJobDetail().getKey().getName());

        ProcessorKey processorKey = new ProcessorKey(serviceName, versionName);
        Map<ProcessorKey, SoaServiceDefinition<?>> processorMap = ContainerFactory.getContainer().getServiceProcessors();
        SoaServiceDefinition soaServiceDefinition = processorMap.get(processorKey);
        Application application = ContainerFactory.getContainer().getApplication(processorKey);
        Object iface = data.get("iface");
        MdcCtxInfoUtil.putMdcToAppClassLoader(application.getAppClasssLoader(), SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, sessionTid);
        try {
            if (soaServiceDefinition.isAsync) {
                SoaFunctionDefinition.Async<Object, Object, Object> functionDefinition = (SoaFunctionDefinition.Async<Object, Object, Object>) data.get("function");
                functionDefinition.apply(iface, new Object());
            } else {
                SoaFunctionDefinition.Sync<Object, Object, Object> functionDefinition = (SoaFunctionDefinition.Sync<Object, Object, Object>) data.get("function");
                functionDefinition.apply(iface, null);
            }
            logger.info("定时任务({})执行完成,cost({}ms)", context.getJobDetail().getKey().getName(), stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            //logger.error("定时任务({})执行异常,cost({}ms)", context.getJobDetail().getKey().getName(), stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
            logger.error(e.getMessage(), e);
            throw new JobExecutionException(e.getMessage(), e);
        } finally {
            // sessionTid will be used at SchedulerTriggerListener
            //MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
            MdcCtxInfoUtil.removeMdcToAppClassLoader(application.getAppClasssLoader(), SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
            // InvocationContextImpl.Factory.removeCurrentInstance();
        }

    }
}
