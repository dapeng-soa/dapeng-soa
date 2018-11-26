package com.github.dapeng.impl.plugins;


import com.github.dapeng.api.AppListener;
import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.api.events.AppEvent;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.ServiceInfo;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.core.timer.ScheduledTask;
import com.github.dapeng.core.timer.ScheduledTaskCron;
import com.github.dapeng.impl.listener.SchedulerJobListener;
import com.github.dapeng.impl.listener.SchedulerTriggerListener;
import com.github.dapeng.impl.listener.TaskMonitorDataReportUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author JackLiang
 */
public class TaskSchedulePlugin implements AppListener, Plugin {

    //private static final Logger LOGGER = LoggerFactory.getLogger(TaskSchedulePlugin.class);
    private static final Logger LOGGER = LoggerFactory.getLogger("container.scheduled.task");

    private final Container container;

    private Scheduler scheduler = null;

    public TaskSchedulePlugin(Container container) {
        this.container = container;
        container.registerAppListener(this);
        if (scheduler == null) {
            try {
                scheduler = StdSchedulerFactory.getDefaultScheduler();

                //添加监听器
                scheduler.getListenerManager().addJobListener(new SchedulerJobListener());
                scheduler.getListenerManager().addTriggerListener(new SchedulerTriggerListener());

            } catch (SchedulerException e) {
                LOGGER.error("TaskSchedulePlugin 初始化出错", e);
            }
        }
    }

    @Override
    public void appRegistered(AppEvent event) {
        LOGGER.warn(getClass().getSimpleName() + "::appRegistered, event[" + event.getSource() + "], do nothing here");
    }

    @Override
    public void appUnRegistered(AppEvent event) {
        LOGGER.warn(getClass().getSimpleName() + "::appUnRegistered, event[" + event.getSource() + "]");
        stop();
    }

    @Override
    public void start() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::start");
        container.getApplications().forEach(application -> {
            List<ServiceInfo> serviceInfos = application.getServiceInfos().stream()
                    .filter(serviceInfo ->
                            serviceInfo.ifaceClass.isAnnotationPresent(ScheduledTask.class))
                    .collect(Collectors.toList());
            serviceInfos.forEach(serviceInfo -> runTask(serviceInfo));
        });

        try {
            scheduler.start();

            //启动监听数据上送线程
            if (SoaSystemEnvProperties.SOA_MONITOR_ENABLE) {
                TaskMonitorDataReportUtils.taskMonitorUploader();
            }

        } catch (SchedulerException e) {
            LOGGER.error("TaskSchedulePlugin::start 定时器启动失败", e);
        }
    }

    @Override
    public void stop() {
        LOGGER.warn("Plugin::TaskSchedulePlugin stop");
        try {
            if (scheduler != null) {
                if (scheduler.isInStandbyMode() || !scheduler.isStarted()) {
                    LOGGER.info(" start to shutdown scheduler: " + scheduler.getSchedulerName());
                    scheduler.shutdown();
                }
            }
        } catch (SchedulerException e) {
            LOGGER.error(" Failed to shutdown scheduler: " + e.getMessage(), e);
        }
    }

    public void runTask(ServiceInfo serviceInfo) {
        Class<?> ifaceClass = serviceInfo.ifaceClass;

        Map<ProcessorKey, SoaServiceDefinition<?>> processorMap = ContainerFactory.getContainer().getServiceProcessors();

        List<Method> taskMethods = Arrays.stream(ifaceClass.getMethods()).filter(method -> method.isAnnotationPresent(ScheduledTaskCron.class))
                .collect(Collectors.toList());

        SoaServiceDefinition soaServiceDefinition = processorMap.get(new ProcessorKey(serviceInfo.serviceName,
                serviceInfo.version));

        if (soaServiceDefinition == null) {
            LOGGER.error(" SoaServiceDefinition Not found....serviceName: {}, version: {} ", serviceInfo.serviceName, serviceInfo.version);
            return;
        }

        taskMethods.forEach(method -> {
            String methodName = method.getName();

            ScheduledTaskCron cron = method.getAnnotation(ScheduledTaskCron.class);
            String cronStr = cron.cron();
            //监控数据是否上报
            boolean isReported = cron.isMonitored();

            //new quartz job
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("function", soaServiceDefinition.functions.get(methodName));
            jobDataMap.put("iface", soaServiceDefinition.iface);
            jobDataMap.put("serviceName", serviceInfo.serviceName);
            jobDataMap.put("versionName", serviceInfo.version);
            jobDataMap.put("methodName", methodName);
            jobDataMap.put("serverIp", SoaSystemEnvProperties.SOA_CONTAINER_IP);
            jobDataMap.putAsString("serverPort", SoaSystemEnvProperties.SOA_CONTAINER_PORT);
            jobDataMap.put("isReported", isReported);

            JobDetail job = JobBuilder.newJob(ScheduledJob.class)
                    .withIdentity(ifaceClass.getName() + ":" + methodName)
                    .setJobData(jobDataMap)
                    .build();

            CronTriggerImpl trigger = new CronTriggerImpl();
            trigger.setName(job.getKey().getName());
            trigger.setJobKey(job.getKey());
            trigger.setJobDataMap(jobDataMap);

            try {
                trigger.setCronExpression(cronStr);
            } catch (ParseException e) {
                LOGGER.error("定时任务({}:{})Cron解析出错", ifaceClass.getName(), methodName);
                LOGGER.error(e.getMessage(), e);
                return;
            }
            try {
                scheduler.scheduleJob(job, trigger);
            } catch (SchedulerException e) {
                LOGGER.error(" Failed to scheduleJob....job: " + job.getKey().getName() + ", reason:" + e.getMessage(), e);
                return;
            }
            LOGGER.info("添加定时任务({}:{})成功", ifaceClass.getName(), methodName);
        });
    }

}
