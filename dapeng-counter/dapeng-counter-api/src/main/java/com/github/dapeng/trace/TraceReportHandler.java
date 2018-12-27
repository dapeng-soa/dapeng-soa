package com.github.dapeng.trace;

import com.github.dapeng.basic.api.counter.CounterServiceAsyncClient;
import com.github.dapeng.basic.api.counter.domain.DataPoint;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.TransactionContextImpl;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据埋点上报处理类
 *
 * @author huyj
 * @Created 2018-12-24 15:51
 */
public class TraceReportHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraceReportHandler.class);
    //Trace 数据库
    private final String TRACE_DB = "dapengTrace";
    //失败缓存的最大的SIZE
    private final int CACHE_SIZE = 200;
    //批量上报 最大SIZE
    private final int MAX_ALARM_SIZE = 8000;
    //定时上报  单位:秒
    private final int PERIOD = 10;
    private final AtomicInteger queueLock = new AtomicInteger(0);
    private final static TraceReportHandler instance = new TraceReportHandler();
    //定时上报定时器
    private ScheduledExecutorService schedulerExecutorService = null;
    private CounterServiceAsyncClient COUNTER_CLIENT = null;
    //local cache for Trace data
    private LinkedList<DataPoint> traceDataQueue = null;
    private ArrayBlockingQueue<LinkedList<DataPoint>> failedTraceQueue = null;

    public static TraceReportHandler getInstance() {
        return instance;
    }

    public TraceReportHandler() {
        this.COUNTER_CLIENT = new CounterServiceAsyncClient();
        this.schedulerExecutorService = Executors.newScheduledThreadPool(1,
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("dapeng-tracePoint-Upload-scheduler-%d")
                        .build());
        this.traceDataQueue = new LinkedList<>();
        this.failedTraceQueue = new ArrayBlockingQueue<>(CACHE_SIZE);
        //启动定时任务
        initUploadScheduler();
    }

    public void appendPoints(List<DataPoint> points) {
        if (points != null && !points.isEmpty()) {
            for (DataPoint point : points) {
                setPointBasicData(point);
            }

            spinLock();
            this.traceDataQueue.addAll(points);
            resetSpinLock();
        }

        //上报数据
        if (this.traceDataQueue.size() >= MAX_ALARM_SIZE) {
            flush();
        }
    }

    public void appendPoint(DataPoint point) {
        LinkedList<DataPoint> traceData = new LinkedList<DataPoint>();
        setPointBasicData(point);
        traceData.add(point);
        appendPoints(traceData);
    }


    /*初始化上送定时任务*/
    private void initUploadScheduler() {
        LOGGER.info("dapeng trace point task started, upload interval:" + PERIOD + "s");

        // 定时上报数据  延迟10秒后，每10秒执行一次
        schedulerExecutorService.scheduleAtFixedRate(this::flush, 10, PERIOD, TimeUnit.SECONDS);
    }


    public void flush() {
        LinkedList<DataPoint> uploaderDataPoints;
        spinLock();
        uploaderDataPoints = Lists.newLinkedList(this.traceDataQueue);
        this.traceDataQueue = new LinkedList<>();
        resetSpinLock();
        try {
            if (SoaSystemEnvProperties.SOA_MONITOR_ENABLE) {
                this.COUNTER_CLIENT.submitPoints(uploaderDataPoints);
            }
            LOGGER.info("trace::flush succeed dataPoint size =" + uploaderDataPoints.size());
        } catch (Exception e) {
            LOGGER.error("trace::flush error,dataPoint size=" + uploaderDataPoints.size(), e);
            // appendPoints(uploaderDataPoints);
            //缓存到失败的队列
            failedTraceQueue.offer(uploaderDataPoints);
        }


        //上传失败的
        LinkedList<DataPoint> failedDataPoints;
        while ((failedDataPoints = failedTraceQueue.poll()) != null) {
            try {
                if (SoaSystemEnvProperties.SOA_MONITOR_ENABLE) {
                    this.COUNTER_CLIENT.submitPoints(failedDataPoints);
                }
            } catch (SoaException e) {
                LOGGER.error("trace::faild Queue  upload error,dataPoint size=" + failedDataPoints.size(), e);
                //缓存到失败的队列
                //failedTraceQueue.offer(failedDataPoints); //(可能会死循环)
            }
        }
    }


    /**
     * 停止上送线程
     *
     * @return
     */
    public void destory() {
        LOGGER.info(" stop trace  upload !");
        schedulerExecutorService.shutdown();
        LOGGER.info(" trace upload is shutdown");
    }


    //自旋锁
    private void spinLock() {
        while (!queueLock.compareAndSet(0, 1)) ;
    }

    // 恢复自旋锁
    private void resetSpinLock() {
        queueLock.set(0);
    }

    private void setPointBasicData(DataPoint point) {
        Map<String, String> tags = point.getTags();
        if (tags == null) {
            tags = new HashMap<>();
        }
        TransactionContext transactionContext = TransactionContextImpl.Factory.currentInstance();
        SoaHeader soaHeader = TransactionContextImpl.Factory.currentInstance().getHeader();

        tags.put("serviceName", soaHeader.getServiceName());
        tags.put("methodName", soaHeader.getMethodName());
        tags.put("version", soaHeader.getVersionName());
        tags.put("hostIp", SoaSystemEnvProperties.HOST_IP);
        tags.put("hostPort", String.valueOf(SoaSystemEnvProperties.SOA_CONTAINER_PORT));

        point.tags(tags);
        point.setDatabase(TRACE_DB);
        point.setTimestamp(System.currentTimeMillis());
    }
}
