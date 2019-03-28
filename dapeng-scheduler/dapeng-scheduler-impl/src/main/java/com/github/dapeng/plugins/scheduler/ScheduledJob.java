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
import com.github.dapeng.util.MdcCtxInfoUtil;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


/**
 * @author tangliu
 * @date 2016/8/17
 * @DisallowConcurrentExecution 的主要作用是quartz单个任务的串行机制
 */
@DisallowConcurrentExecution
public class ScheduledJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger("container.scheduled.task");
    Object messageBean = null;

    public ScheduledJob() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap data = context.getJobDetail().getJobDataMap();
        String serviceName = data.getString("serviceName");
        String versionName = data.getString("versionName");
        String methodName = data.getString("methodName");

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

        //事件类型
        Map eventMap = new HashMap(16);
        eventMap.put("serviceName", serviceName);
        eventMap.put("methodName", methodName);
        eventMap.put("versionName", versionName);
        eventMap.put("remark", "Dapeng-Scheduler-Running");

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
            eventMap.put("costTime", costTime);
            eventMap.put("taskStatus", "success");

            logger.info("定时任务({})执行完成,cost({}ms)", context.getJobDetail().getKey().getName(), costTime);
        } catch (Exception e) {
            stopwatch.stop();
            logger.error("定时任务(" + context.getJobDetail().getKey().getName() + ")执行异常,cost(" + "ms)", e);
            eventMap.put("costTime", 0L);
            eventMap.put("taskStatus", "fail");
            throw new JobExecutionException(e.getMessage(), e);
        } finally {
            //发布消息
            publishKafkaMessage(application, eventMap);

            // sessionTid will be used at SchedulerTriggerListener
            MdcCtxInfoUtil.removeMdcToAppClassLoader(application.getAppClasssLoader(), SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
        }
    }

    /**
     * 定时任务执行完成发布 消息
     *
     * @param application
     * @param eventMap
     */
    private void publishKafkaMessage(Application application, Map eventMap) {
        try {

            if (messageBean == null) {
                messageBean = application.getSpringBean("taskMsgKafkaProducer");
            }
            if (messageBean != null) {
                Method publishMessageMethod = messageBean.getClass().getMethod("sendTaskMessageDefaultTopic", Map.class);
                publishMessageMethod.invoke(messageBean, eventMap);
            } else {
                logger.info("没有检测到kafka消息生产者配置[taskMsgKafkaProducer]，不会推送消息.");
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.error("定时任务消息推送失败");
            logger.error(e.getMessage(), e);
        }
    }
}