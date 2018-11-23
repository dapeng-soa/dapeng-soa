package com.github.dapeng.impl.listener;

import com.github.dapeng.basic.api.counter.domain.DataPoint;
import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
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
import java.util.concurrent.TimeUnit;

/**
 * 定时任务监听器
 *
 * @author huyj
 * @Created 2018-11-14 11:20
 */
public class SchedulerJobListener implements JobListener {
    private Logger logger = LoggerFactory.getLogger("container.scheduled.task");

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");
    // 线程池
    private static ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("dapeng-SchedulerJobListener-%d")
            .build());

    @Override
    public String getName() {
        return "SchedulerJobListener";
    }

    /**
     * (1)
     * 任务执行之前执行
     * Called by the Scheduler when a JobDetail is about to be executed (an associated Trigger has occurred).
     */
    @Override
    public void jobToBeExecuted(final JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String serviceName = jobDataMap.getString("serviceName");
        String versionName = jobDataMap.getString("versionName");
        String methodName = jobDataMap.getString("methodName");

        context.getJobDetail().getJobDataMap().put("startTime", LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        String message = String.format("SchedulerJobListener::jobToBeExecuted;Task[%s:%s:%s] 即将被执行", serviceName, versionName, methodName);
        //sendMessage(serviceName, versionName, methodName, message, false,"normal");
    }

    /**
     * (2)
     * 这个方法正常情况下不执行,但是如果当TriggerListener中的vetoJobExecution方法返回true时,那么执行这个方法.
     * 需要注意的是 如果方法(2)执行 那么(1),(3)这个俩个方法不会执行,因为任务被终止了嘛.
     * Called by the Scheduler when a JobDetail was about to be executed (an associated Trigger has occurred),
     * but a TriggerListener vetoed it's execution.
     */
    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String serviceName = jobDataMap.getString("serviceName");
        String versionName = jobDataMap.getString("versionName");
        String methodName = jobDataMap.getString("methodName");
        String message = String.format("SchedulerJobListener::jobExecutionVetoed;Task[%s:%s:%s] 触发失败", serviceName, versionName, methodName);
        sendMessage(serviceName, versionName, methodName, message, true, jobDataMap, "failed");
    }

    /**
     * (3)
     * 任务执行完成后执行,jobException如果它不为空则说明任务在执行过程中出现了异常
     * Called by the Scheduler after a JobDetail has been executed, and be for the associated Trigger's triggered(xx) method has been called.
     */
    @Override
    public void jobWasExecuted(final JobExecutionContext context, JobExecutionException exp) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String serviceName = jobDataMap.getString("serviceName");
        String versionName = jobDataMap.getString("versionName");
        String methodName = jobDataMap.getString("methodName");

        int execute_count = context.getRefireCount();
        if (exp != null) {//任务执行出现异常
            if (execute_count <= 5) {//任务执行出错(出异常)  最多重试 5次  ,防止出现死循环
                String message = String.format("SchedulerJobListener::jobWasExecuted;Task[%s:%s:%s] 执行出现异常:%s", serviceName, versionName, methodName, exp.getMessage());
                sendMessage(serviceName, versionName, methodName, message, true, jobDataMap, "failed");
                //错过挤压重试
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
                exp.setRefireImmediately(true);
            }
        } else {//任务执行成功
            LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
            LocalDateTime startTime = (LocalDateTime) jobDataMap.get("startTime");
            long taskCost = Duration.between(startTime, currentTime).toMillis();
            String message = String.format("SchedulerJobListener::jobWasExecuted;Task[%s:%s:%s] 执行完成[%s],cost:%sms", serviceName, versionName, methodName, currentTime.format(DATE_TIME), taskCost);
            sendMessage(serviceName, versionName, methodName, message, false, jobDataMap, "succeed");
        }
    }

    private void sendMessage(String serviceName, String versionName, String methodName, final String message, boolean isError, JobDataMap jobDataMap, String executeState) {
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

                //是否上报监听数据(错误必须上报)
                boolean isReported = isError || jobDataMap.getBoolean("isReported");
                if (SoaSystemEnvProperties.SOA_MONITOR_ENABLE && isReported) {
                    taskInfoReport(jobDataMap, executeState);
                }
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            } finally {
                TaskMonitorDataReportUtils.removeSessionTid();
            }
            //System.out.println(message);
        });
    }

    private void taskInfoReport(JobDataMap jobDataMap, String executeState) {
        DataPoint influxdbDataPoint = new DataPoint();
        influxdbDataPoint.setDatabase(TaskMonitorDataReportUtils.TASK_DATABASE);
        influxdbDataPoint.setBizTag(TaskMonitorDataReportUtils.TASK_DATABASE_TABLE);


        if (jobDataMap.getString("methodName").equalsIgnoreCase("taskDemo2")) {
            logger.error("*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*--*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-");
        }else{
            logger.error("*****taskDemo1*****");
        }
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
