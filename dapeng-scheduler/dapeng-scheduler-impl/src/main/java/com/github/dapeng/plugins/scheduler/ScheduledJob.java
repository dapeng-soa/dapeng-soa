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
package com.github.dapeng.plugins.scheduler;

import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.core.Application;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.scheduler.events.TaskEvent;
import com.github.dapeng.util.MdcCtxInfoUtil;
import com.today.api.scheduler.enums.TaskStatusEnum;
import com.today.commons.GenIdUtil;
import com.today.eventbus.CommonEventBus;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.util.Map;

import static com.today.commons.GenIdUtil.TASK_EVENT_ID;

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
        String methodName = data.getString("methodName");

        //事件类型
        TaskEvent taskEvent = new TaskEvent();
        taskEvent.id(GenIdUtil.getId(TASK_EVENT_ID));
        taskEvent.setServiceName(serviceName);
        taskEvent.setMethodName(methodName);
        taskEvent.setVersion(versionName);

        /**
         * 添加sessionTid
         */
        String sessionTid = TaskMonitorDataReportUtils.getInstance().setSessionTid(InvocationContextImpl.Factory.currentInstance());

        StopWatch stopwatch = new StopWatch();
        stopwatch.start();

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
            stopwatch.stop();
            long costTime = stopwatch.getLastTaskTimeMillis();
            taskEvent.setCostTime(costTime);
            taskEvent.setTaskStatus(TaskStatusEnum.SUCCEED);

            logger.info("定时任务({})执行完成,cost({}ms)", context.getJobDetail().getKey().getName(), costTime);
        } catch (Exception e) {
            stopwatch.stop();
            logger.error("定时任务(" + context.getJobDetail().getKey().getName() + ")执行异常,cost(" + "ms)", e);
            taskEvent.setTaskStatus(TaskStatusEnum.FAIL);
            throw new JobExecutionException(e.getMessage(), e);
        } finally {

            //发布消息
            CommonEventBus.fireEvent(taskEvent);

            // sessionTid will be used at SchedulerTriggerListener
            MdcCtxInfoUtil.removeMdcToAppClassLoader(application.getAppClasssLoader(), SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
        }
    }
}
