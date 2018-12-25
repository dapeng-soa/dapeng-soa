package com.github.dapeng.impl.listener;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.dapeng.impl.listener.TaskMonitorDataReportUtils.sendMessage;


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
    private static ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
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
        context.getJobDetail().getJobDataMap().put("startTime", LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
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
        if (exp != null) {
            String message = String.format("SchedulerJobListener::jobWasExecuted;Task[%s:%s:%s] 执行出现异常:%s", serviceName, versionName, methodName, exp.getMessage());
            sendMessage(serviceName, versionName, methodName, executorService, message, true, jobDataMap, "failed");

        } else {//任务执行成功
            LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
            LocalDateTime startTime = (LocalDateTime) jobDataMap.get("startTime");
            long taskCost = Duration.between(startTime, currentTime).toMillis();
            String message = String.format("SchedulerJobListener::jobWasExecuted;Task[%s:%s:%s] 执行完成[%s],cost:%sms", serviceName, versionName, methodName, currentTime.format(DATE_TIME), taskCost);
            sendMessage(serviceName, versionName, methodName, executorService, message, false, jobDataMap, "succeed");
        }
    }

}
