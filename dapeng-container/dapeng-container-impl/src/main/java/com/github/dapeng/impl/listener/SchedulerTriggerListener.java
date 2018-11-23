package com.github.dapeng.impl.listener;

import com.github.dapeng.basic.api.counter.domain.DataPoint;
import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
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
    private static ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
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
        TaskMonitorDataReportUtils.setSessionTid(null);

        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String serviceName = jobDataMap.getString("serviceName");
        String versionName = jobDataMap.getString("versionName");
        String methodName = jobDataMap.getString("methodName");

        String message = String.format("SchedulerTriggerListener::triggerFired;Task[%s:%s:%s] 即将被触发", serviceName, versionName, methodName);
        //sendMessage(serviceName, versionName, methodName, message, false,jobDataMap,"normal");
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

        context.getJobDetail().getJobDataMap().put("startTime", LocalDateTime.now(ZoneId.of("Asia/Shanghai")));

        String message = String.format("SchedulerTriggerListener::vetoJobExecution;Task[%s:%s:%s] 即将开始执行", serviceName, versionName, methodName);
        //sendMessage(serviceName, versionName, methodName, message, false,jobDataMap,"normal");
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

        trigger.getJobDataMap().put("startTime", LocalDateTime.now(ZoneId.of("Asia/Shanghai")));

        String currentTime = LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(DATE_TIME);
        String message = String.format("SchedulerTriggerListener::triggerMisfired;Task[%s:%s:%s] 触发超时,错过[%s]这一轮触发", serviceName, versionName, methodName, currentTime);
        sendMessage(serviceName, versionName, methodName, message, true, jobDataMap, "triggerTimeOut");
    }

    /**
     * (4) 任务完成时触发
     * Called by the Scheduler when a Trigger has fired, it's associated JobDetail has been executed
     * and it's triggered(xx) method has been called.
     */
    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context, Trigger.CompletedExecutionInstruction triggerInstructionCode) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String serviceName = jobDataMap.getString("serviceName");
        String versionName = jobDataMap.getString("versionName");
        String methodName = jobDataMap.getString("methodName");

        LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        LocalDateTime startTime = (LocalDateTime) jobDataMap.get("startTime");
        long taskCost = Duration.between(startTime, currentTime).toMillis();

        String message = String.format("SchedulerTriggerListener::triggerComplete;Task[%s:%s:%s] 执行完成[%s] ,cost:%sms", serviceName, versionName, methodName, currentTime.format(DATE_TIME), taskCost);
        //sendMessage(serviceName, versionName, methodName, message, false,jobDataMap,"succeed");
    }


    private void sendMessage(String serviceName, String versionName, String methodName, final String message, boolean isError, JobDataMap jobDataMap, String executeState) {
        try {
            InvocationContext invocationContext = InvocationContextImpl.Factory.currentInstance();
            executorService.submit(() -> {
                try {
                    TaskMonitorDataReportUtils.setSessionTid(invocationContext);
                    if (logger.isInfoEnabled()) {
                        logger.info(message);
                    }
                    if (isError) {
                        logger.error(message);
                    }

                    if (SoaSystemEnvProperties.SOA_MONITOR_ENABLE) {
                        taskInfoReport(jobDataMap, executeState);
                    }
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    TaskMonitorDataReportUtils.removeSessionTid();
                }
                //System.out.println(message);
            });
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        } finally {
            TaskMonitorDataReportUtils.removeSessionTid();
        }
    }


    private void taskInfoReport(JobDataMap jobDataMap, String executeState) {
        DataPoint influxdbDataPoint = new DataPoint();
        influxdbDataPoint.setDatabase(TaskMonitorDataReportUtils.TASK_DATABASE);
        influxdbDataPoint.setBizTag(TaskMonitorDataReportUtils.TASK_DATABASE_TABLE);

        Map<String, String> tags = new HashMap<>(8);
        tags.put("serviceName", jobDataMap.getString("serviceName"));
        tags.put("methodName", jobDataMap.getString("methodName"));
        tags.put("versionName", jobDataMap.getString("versionName"));
        tags.put("serverIp", jobDataMap.getString("serverIp"));
        tags.put("serverPort", jobDataMap.getString("serverPort"));
        tags.put("executeState", executeState);
        influxdbDataPoint.setTags(tags);

        Map<String, Long> fields = new HashMap<>(8);

        LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        LocalDateTime startTime = (LocalDateTime) jobDataMap.get("startTime");
        long taskCost = Duration.between(startTime, currentTime).toMillis();
        fields.put("costTime", taskCost);

        influxdbDataPoint.setValues(fields);
        influxdbDataPoint.setTimestamp(System.currentTimeMillis());

        //放入上送列表
        TaskMonitorDataReportUtils.appendDataPoint(Lists.newArrayList(influxdbDataPoint));
    }
}