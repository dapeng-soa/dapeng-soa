package com.github.dapeng.impl.plugins;



import com.github.dapeng.api.AppListener;
import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.api.events.AppEvent;
import com.github.dapeng.core.Application;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.ServiceInfo;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.timer.ScheduledTask;
import com.github.dapeng.core.timer.ScheduledTaskCron;
import com.github.dapeng.api.AppListener;
import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.api.events.AppEvent;
import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.timer.ScheduledTask;
import com.github.dapeng.core.timer.ScheduledTaskCron;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

public class TaskSchedulePlugin implements AppListener,Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskSchedulePlugin.class);

    private final Container container;

    private Scheduler scheduler = null;

    public TaskSchedulePlugin(Container container) {
        this.container = container;
        container.registerAppListener(this);
    }


    @Override
    public void appRegistered(AppEvent event) {
        Application application = (Application) event.getSource();

        List<ServiceInfo> serviceInfos = application.getServiceInfos();
        //TODO: 可以使用Adaptor 或者 Filter 来过滤监听的事件？
        serviceInfos.forEach(i -> runTask(i));
    }

    @Override
    public void appUnRegistered(AppEvent event) {
        Application application = (Application) event.getSource();

        List<ServiceInfo> serviceInfos = application.getServiceInfos();
        //TODO: 可以使用Adaptor 或者 Filter 来过滤监听的事件？
        serviceInfos.forEach(i -> stopTask(i));
    }

    @Override
    public void start() {
        container.getApplications().forEach(i -> {
            List<ServiceInfo> serviceInfos = i.getServiceInfos();
            serviceInfos.forEach(s -> runTask(s));
        });
    }

    @Override
    public void stop() {

    }

    public void runTask(ServiceInfo serviceInfo) {
        Class<?> ifaceClass = serviceInfo.ifaceClass;

        Map<ProcessorKey, SoaServiceDefinition<?>> processorMap = ContainerFactory.getContainer().getServiceProcessors();


        if (ifaceClass.isAnnotationPresent(ScheduledTask.class)) {

            for (Method method : ifaceClass.getMethods()) {
                if (method.isAnnotationPresent(ScheduledTaskCron.class)) {

                    String methodName = method.getName();

                    SoaServiceDefinition soaServiceDefinition = processorMap.get(new ProcessorKey(serviceInfo.serviceName, serviceInfo.version));

                    if (soaServiceDefinition == null) {
                        LOGGER.error(" SoaServiceDefinition Not found....serviceName: {}, version: {} ", serviceInfo.serviceName, serviceInfo.version);
                    }

                    ScheduledTaskCron cron = method.getAnnotation(ScheduledTaskCron.class);
                    String cronStr = cron.cron();

                    //new quartz job
                    JobDataMap jobDataMap = new JobDataMap();
                    jobDataMap.put("function", soaServiceDefinition.functions.get(methodName));
                    jobDataMap.put("iface", soaServiceDefinition.iface);
                    jobDataMap.put("serviceName", serviceInfo.serviceName);
                    jobDataMap.put("versionName", serviceInfo.version);
                    JobDetail job = JobBuilder.newJob(ScheduledJob.class).withIdentity(ifaceClass.getName() + ":" + methodName).setJobData(jobDataMap).build();

                    CronTriggerImpl trigger = new CronTriggerImpl();
                    trigger.setName(job.getKey().getName());
                    trigger.setJobKey(job.getKey());
                    try {
                        trigger.setCronExpression(cronStr);
                    } catch (ParseException e) {
                        LOGGER.error("定时任务({}:{})Cron解析出错", ifaceClass.getName(), methodName);
                        LOGGER.error(e.getMessage(), e);
                        continue;
                    }

                    if (scheduler == null) {
                        try {
                            scheduler = StdSchedulerFactory.getDefaultScheduler();
                            scheduler.start();
                        } catch (SchedulerException e) {
                            LOGGER.error("ScheduledTaskContainer启动失败");
                            LOGGER.error(e.getMessage(), e);
                            return;
                        }
                    }
                    try {
                        scheduler.scheduleJob(job, trigger);
                    } catch (SchedulerException e) {
                        LOGGER.error(" Failed to scheduleJob....job: " + job.getKey().getName(), e);
                    }
                    LOGGER.info("添加定时任务({}:{})成功", ifaceClass.getName(), methodName);
                }
            }
        }
    }

    public void stopTask(ServiceInfo appInfo) {
        //Some logic
    }
}
