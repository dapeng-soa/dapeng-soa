package com.github.dapeng.impl.listener;

import com.github.dapeng.core.helper.MasterHelper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 定时任务触发器监听器
 *
 * @author huyj
 * @Created 2018-11-14 10:36
 */
public class SchedulerTriggerListener implements TriggerListener {
    private Logger logger = LoggerFactory.getLogger("container.scheduled.task");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");
    // 线程池
    private static ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("dapeng-SchedulerTriggerListener-%d")
            .build());

    @Override
    public String getName() {
        return "SchedulerTriggerListener";
    }

    /**
     * (1)
     * Trigger被激发 它关联的job即将被运行
     * Called by the Scheduler when a Trigger has fired, and it's associated JobDetail is about to be executed.
     */
    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {
        TaskMonitorDataReportUtils.getInstance().setSessionTid(null);
    }

    /**
     * (2)
     * Trigger被激发 它关联的job即将被运行,先执行(1)，在执行(2) 如果返回TRUE 那么任务job会被终止
     * Called by the Scheduler when a Trigger has fired, and it's associated JobDetail is about to be executed
     */
    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String serviceName = jobDataMap.getString("serviceName");
        String versionName = jobDataMap.getString("versionName");
        String methodName = jobDataMap.getString("methodName");
        if(!MasterHelper.isMaster(serviceName,versionName)){
            return true;
        }

        context.getJobDetail().getJobDataMap().put("startTime", LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        return false;
    }

    /**
     * (3) 当Trigger错过被激发时执行,比如当前时间有很多触发器都需要执行，但是线程池中的有效线程都在工作，
     * 那么有的触发器就有可能超时，错过这一轮的触发。
     * Called by the Scheduler when a Trigger has misfired.
     */
    @Override
    public void triggerMisfired(Trigger trigger) {
        JobDataMap jobDataMap = trigger.getJobDataMap();
        String serviceName = jobDataMap.getString("serviceName");
        String versionName = jobDataMap.getString("versionName");
        String methodName = jobDataMap.getString("methodName");

        LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        trigger.getJobDataMap().put("startTime", currentTime);

        String currentTimeAsString = currentTime.format(DATE_TIME);
        String message = String.format("SchedulerTriggerListener::triggerMisfired;Task[%s:%s:%s] 触发超时,错过[%s]这一轮触发", serviceName, versionName, methodName, currentTimeAsString);
        TaskMonitorDataReportUtils.getInstance().sendMessage(serviceName, versionName, methodName, executorService, message, true, jobDataMap, "triggerTimeOut");
    }

    /**
     * (4) 任务完成时触发
     * Called by the Scheduler when a Trigger has fired, it's associated JobDetail has been executed
     * and it's triggered(xx) method has been called.
     */
    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context, Trigger.CompletedExecutionInstruction triggerInstructionCode) {
        TaskMonitorDataReportUtils.getInstance().removeSessionTid();
    }
}