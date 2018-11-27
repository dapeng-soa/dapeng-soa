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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author huyj
 * @Created 2018-11-22 11:41
 */
public class TaskMonitorDataReportUtils {
    private static Logger logger = LoggerFactory.getLogger("container.scheduled.task");

    private static final int MAX_SIZE = 32;
    private static final int BATCH_MAX_SIZE = 50;

    public final static String TASK_DATABASE = "dapengTask";
    public final static String TASK_DATABASE_TABLE = "dapeng_task_info";
    //private static CounterService COUNTER_CLIENT = new CounterServiceClient();
    private static CounterServiceAsync COUNTER_CLIENT = new CounterServiceAsyncClient();
    private static final List<DataPoint> dataPointList = new ArrayList<>();
    private static final ArrayBlockingQueue<List<DataPoint>> taskDataQueue = new ArrayBlockingQueue<>(MAX_SIZE);

    //上送线程池
    private static final ExecutorService taskMonitorDataUploaderExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("dapeng-taskMonitorDataUploader-%d")
            .build());

    public static void appendDataPoint(List<DataPoint> uploadList) {
        synchronized (dataPointList) {
            dataPointList.addAll(uploadList);

            if (dataPointList.size() >= BATCH_MAX_SIZE) {
                //taskDataQueue.put(Lists.newArrayList(dataPointList));
                if (!taskDataQueue.offer(Lists.newArrayList(dataPointList))) {
                    logger.info("TaskMonitorDataReportUtils::appendDataPoint put into taskDataQueue failed maxSzie = {}", MAX_SIZE);
                }
                dataPointList.clear();
            }
        }
    }

    public static void taskMonitorUploader() {
        // uploader point thread.
        taskMonitorDataUploaderExecutor.execute(() -> {
            while (true) {
                List<DataPoint> uploaderDataPointList = null;
                try {
                    uploaderDataPointList = taskDataQueue.take();
                    COUNTER_CLIENT.submitPoints(uploaderDataPointList);
                    logger.info("taskMonitorDataUploaderExecutor::upload dataPoint size = {}", uploaderDataPointList.size());
                } catch (SoaException e) {
                    logger.error("TaskMonitorDataReportUtils::taskMonitorUploader dataPoint size = {} upload Exception and re-append to taskDataQueue", uploaderDataPointList.size());
                    logger.error(e.getMsg(), e);
                    appendDataPoint(uploaderDataPointList);
                } catch (InterruptedException e) {
                    logger.error("TaskMonitorDataReportUtils::taskMonitorUploader taskDataQueue take is Interrupted", e);
                    logger.error(e.getMessage(), e);
                }
            }
        });
    }


    public static String setSessionTid(InvocationContext context) {
        InvocationContext invocationContext = context != null ? InvocationContextImpl.Factory.currentInstance(context) : InvocationContextImpl.Factory.createNewInstance();
        if (!invocationContext.sessionTid().isPresent()) {
            long tid = DapengUtil.generateTid();
            invocationContext.sessionTid(tid);
        }
        String sessionTid = DapengUtil.longToHexStr(invocationContext.sessionTid().orElse(0L));
        MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, sessionTid);

        return sessionTid;
    }

    public static void removeSessionTid() {
        MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
        InvocationContextImpl.Factory.removeCurrentInstance();
    }
}
