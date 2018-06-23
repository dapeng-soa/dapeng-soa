package com.github.dapeng.impl.plugins;

import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.google.common.base.Stopwatch;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author tangliu
 * @date 2016/8/17
 * @DisallowConcurrentExecution 的主要作用是quartz单个任务的串行机制
 */
@DisallowConcurrentExecution
public class ScheduledJob implements Job {

    //private static final Logger logger = LoggerFactory.getLogger(TaskSchedulePlugin.class);
    private static final Logger logger = LoggerFactory.getLogger("container.scheduled.task");

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap data = context.getJobDetail().getJobDataMap();
        String serviceName = data.getString("serviceName");
        String versionName = data.getString("versionName");

//        if (!MasterHelper.isMaster(serviceName, versionName)) {
//            logger.info("--定时任务({}:{})不是Master，跳过--", serviceName, versionName);
//            return;
//        }
        Stopwatch stopwatch = Stopwatch.createStarted();
        /**
         * 添加sessionTid
         */
        InvocationContext invocationContext = InvocationContextImpl.Factory.currentInstance();
        invocationContext.sessionTid(DapengUtil.generateTid());
        MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, invocationContext.sessionTid().orElse("0"));


        logger.info("定时任务({})开始执行", context.getJobDetail().getKey().getName());
        Map<ProcessorKey, SoaServiceDefinition<?>> processorMap = ContainerFactory.getContainer().getServiceProcessors();
        SoaServiceDefinition soaServiceDefinition = processorMap.get(new ProcessorKey(serviceName, versionName));
//        SoaProcessFunction<Object, Object, Object, ? extends TCommonBeanSerializer<Object>, ? extends TCommonBeanSerializer<Object>> soaProcessFunction =
//                (SoaProcessFunction<Object, Object, Object, ? extends TCommonBeanSerializer<Object>, ? extends TCommonBeanSerializer<Object>>) data.get("function");
        Object iface = data.get("iface");
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
            logger.error("定时任务({})执行异常,cost({}ms)", context.getJobDetail().getKey().getName(), stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
            logger.error(e.getMessage(), e);
        }finally{
            MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
            InvocationContextImpl.Factory.removeCurrentInstance();
        }
    }
}
