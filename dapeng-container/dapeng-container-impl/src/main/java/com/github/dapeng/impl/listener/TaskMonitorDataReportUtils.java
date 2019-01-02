package com.github.dapeng.impl.listener;

import com.github.dapeng.basic.api.counter.CounterServiceAsyncClient;
import com.github.dapeng.basic.api.counter.domain.DataPoint;
import com.github.dapeng.basic.api.counter.service.CounterServiceAsync;
import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.quartz.JobDataMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author huyj
 * @Created 2018-11-22 11:41
 */
public class TaskMonitorDataReportUtils {
    private static Logger logger = LoggerFactory.getLogger("container.scheduled.task");

    private static final int MAX_SIZE = 32;
    private static final int BATCH_MAX_SIZE = 50;
    //定时上报  单位:秒
    private static final int UPLOAD_PERIOD = 20;

    public final static String TASK_DATABASE = "dapengTask";
    public final static String TASK_DATABASE_TABLE = "dapeng_task_info";
    private static CounterServiceAsync COUNTER_CLIENT = null;
    private static final List<DataPoint> dataPointList = new ArrayList<>();
    private static final ArrayBlockingQueue<List<DataPoint>> taskDataQueue = new ArrayBlockingQueue<>(MAX_SIZE);

    private final static TaskMonitorDataReportUtils instance = new TaskMonitorDataReportUtils();
    private static ExecutorService taskMonitorDataUploaderExecutor = null;
    //定时上报定时器
    private static ScheduledExecutorService schedulerExecutorService = null;

    public static TaskMonitorDataReportUtils getInstance() {
        return instance;
    }

    public TaskMonitorDataReportUtils() {
        if (SoaSystemEnvProperties.SOA_MONITOR_ENABLE) {
            COUNTER_CLIENT = new CounterServiceAsyncClient();
            //启动监听数据上送线程
            taskMonitorDataUploaderExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("dapeng-taskMonitorDataUploader-%d")
                    .build());
            taskMonitorUploader();

            //定时上送线程
            schedulerExecutorService = Executors.newScheduledThreadPool(1,
                    new ThreadFactoryBuilder()
                            .setDaemon(true)
                            .setNameFormat("dapeng-tracePoint-Upload-scheduler-%d")
                            .build());
            // 定时上报数据  延迟10秒后，每10秒执行一次
            schedulerExecutorService.scheduleAtFixedRate(this::flushDataPoint, UPLOAD_PERIOD, UPLOAD_PERIOD, TimeUnit.SECONDS);
        }
    }


    public void appendDataPoint(List<DataPoint> uploadList) {
        synchronized (dataPointList) {
            dataPointList.addAll(uploadList);

            if (dataPointList.size() >= BATCH_MAX_SIZE) {
                if (!taskDataQueue.offer(Lists.newArrayList(dataPointList))) {
                    logger.info("TaskMonitorDataReportUtils::appendDataPoint put into taskDataQueue failed Szie = " + dataPointList.size());
                }
                dataPointList.clear();
            }
        }
    }

    /*刷新缓存*/
    public void flushDataPoint() {
        synchronized (dataPointList) {
            if (!dataPointList.isEmpty()) {
                if (!taskDataQueue.offer(Lists.newArrayList(dataPointList))) {
                    logger.info("TaskMonitorDataReportUtils::appendDataPoint put into taskDataQueue failed Szie = " + dataPointList.size());
                }
                dataPointList.clear();
            }
        }
    }

    public void taskMonitorUploader() {
        // uploader point thread.
        taskMonitorDataUploaderExecutor.execute(() -> {
            while (true) {
                List<DataPoint> uploaderDataPointList = null;
                try {
                    uploaderDataPointList = taskDataQueue.take();
                    COUNTER_CLIENT.submitPoints(uploaderDataPointList);
                    logger.info("taskMonitorDataUploaderExecutor::upload dataPoint size = " + uploaderDataPointList.size());
                } catch (SoaException e) {
                    logger.error("TaskMonitorDataReportUtils::taskMonitorUploader dataPoint size = " + uploaderDataPointList.size() + ", upload Exception and re-append to taskDataQueue");
                    logger.error(e.getMsg(), e);
                    appendDataPoint(uploaderDataPointList);
                } catch (InterruptedException e) {
                    logger.error("TaskMonitorDataReportUtils::taskMonitorUploader taskDataQueue take is Interrupted", e);
                    logger.error(e.getMessage(), e);
                }
            }
        });
    }

    public void sendMessage(String serviceName, String versionName, String methodName, ExecutorService executorService, final String message, boolean isError, JobDataMap jobDataMap, String executeState) {
        InvocationContext invocationContext = InvocationContextImpl.Factory.currentInstance();
        executorService.submit(() -> {
            try {
                setSessionTid(invocationContext);
                logger.info(message);

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
                removeSessionTid();
            }
        });
    }


    private void taskInfoReport(JobDataMap jobDataMap, String executeState) {
        DataPoint influxdbDataPoint = new DataPoint();
        influxdbDataPoint.setDatabase(TaskMonitorDataReportUtils.TASK_DATABASE);
        influxdbDataPoint.setBizTag(TaskMonitorDataReportUtils.TASK_DATABASE_TABLE);
        influxdbDataPoint.setTimestamp(System.currentTimeMillis());
        Map<String, String> tags = new HashMap<>(8);
        tags.put("serviceName", jobDataMap.getString("serviceName"));
        tags.put("methodName", jobDataMap.getString("methodName"));
        tags.put("versionName", jobDataMap.getString("versionName"));
        tags.put("serverIp", jobDataMap.getString("serverIp"));
        tags.put("serverPort", jobDataMap.getString("serverPort"));
        tags.put("executeState", executeState);
        //
        tags.put("cronStr", jobDataMap.getString("cronStr"));

        influxdbDataPoint.setTags(tags);

        Map<String, Long> fields = new HashMap<>(8);

        LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        LocalDateTime startTime = (LocalDateTime) jobDataMap.get("startTime");
        long taskCost = Duration.between(startTime, currentTime).toMillis();
        fields.put("costTime", taskCost);
        fields.put("executeCount", 1L);
        fields.put("expectedCount", (Long) jobDataMap.get("expectedCount"));

        switch (executeState) {
            case "triggerTimeOut":
                fields.put("succeedCount", 0L);
                fields.put("failedCount", 0L);
                fields.put("timeoutCount", 1L);
                break;
            case "failed":
                fields.put("succeedCount", 0L);
                fields.put("failedCount", 1L);
                fields.put("timeoutCount", 0L);
                break;
            case "succeed":
                fields.put("succeedCount", 1L);
                fields.put("failedCount", 0L);
                fields.put("timeoutCount", 0L);
                break;
            default:
                break;
        }

        influxdbDataPoint.setValues(fields);
        influxdbDataPoint.setTimestamp(System.currentTimeMillis());

        if (SoaSystemEnvProperties.SOA_MONITOR_ENABLE) {
            //放入上送列表
            appendDataPoint(Lists.newArrayList(influxdbDataPoint));
        }
    }

    public String setSessionTid(InvocationContext context) {
        InvocationContext invocationContext = context != null ? InvocationContextImpl.Factory.currentInstance(context) : InvocationContextImpl.Factory.createNewInstance();
        if (!invocationContext.sessionTid().isPresent()) {
            long tid = DapengUtil.generateTid();
            invocationContext.sessionTid(tid);
        }
        String sessionTid = DapengUtil.longToHexStr(invocationContext.sessionTid().orElse(0L));
        MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, sessionTid);

        return sessionTid;
    }

    public void removeSessionTid() {
        MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
        InvocationContextImpl.Factory.removeCurrentInstance();
    }
}
