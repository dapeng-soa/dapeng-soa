package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.basic.api.counter.CounterServiceClient;
import com.github.dapeng.basic.api.counter.domain.DataPoint;
import com.github.dapeng.basic.api.counter.service.CounterService;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.impl.plugins.monitor.ServiceProcessData;
import com.github.dapeng.impl.plugins.monitor.ServiceSimpleInfo;
import com.github.dapeng.impl.plugins.monitor.config.MonitorFilterProperties;
import com.github.dapeng.util.SoaSystemEnvProperties;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author with struy.
 * Create by 2018/3/8 15:37
 * email :yq1724555319@gmail.com
 */

public class SoaInvokeMonitorHandler extends ChannelDuplexHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private static final int PERIOD = MonitorFilterProperties.SOA_MONITOR_SERVICE_PROCESS_PERIOD;
    private static final String DATA_BASE = MonitorFilterProperties.SOA_MONITOR_INFLUXDB_DATABASE;
    private static final String SUCCESS_CODE = SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE;
    private static final String SERVER_IP = SoaSystemEnvProperties.SOA_CONTAINER_IP;
    private static final Integer SERVER_PORT = SoaSystemEnvProperties.SOA_CONTAINER_PORT;
    private final CounterService SERVICE_CLIENT = new CounterServiceClient();
    private List<Map<ServiceSimpleInfo, Long>> serviceElapses = new ArrayList<>();
    /**
     * 异常情况存储10小时
     */
    private ArrayBlockingQueue<List<DataPoint>> serviceDataQueue = new ArrayBlockingQueue<>(60 * 60 * 10 / PERIOD);
    /**
     * k => seqId ,v => invokeStartTime
     * if seqId exist,cost = invokeEndTime - invokeStartTime
     */
    private Map<Integer, Long> invokeStartPair = new ConcurrentHashMap<>(16);

    private Map<ServiceSimpleInfo, ServiceProcessData> serviceProcessCallDatas = new ConcurrentHashMap<>(16);

    /**
     * 共享资源锁, 用于保护serviceProcessCallDatas的同步锁
     */
    private ReentrantLock shareLock = new ReentrantLock();
    /**
     * 信号锁, 用于提醒线程跟数据上送线程的同步
     */
    private ReentrantLock signalLock = new ReentrantLock();
    private Condition signalCondition = signalLock.newCondition();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        TransactionContext transactionContext = TransactionContext.Factory.getCurrentInstance();
        Integer seqId = transactionContext.getSeqid();
        Long invokeStartTime = System.currentTimeMillis();
        invokeStartPair.put(seqId, invokeStartTime);
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        Long invokeEndTime = System.currentTimeMillis();
        SoaResponseWrapper wrapper = (SoaResponseWrapper) msg;
        TransactionContext context = wrapper.transactionContext;
        SoaHeader soaHeader = context.getHeader();
        Integer seqId = context.getSeqid();

        ServiceSimpleInfo simpleInfo = new ServiceSimpleInfo(soaHeader.getServiceName(), soaHeader.getMethodName(), soaHeader.getVersionName());
        ServiceProcessData processData = serviceProcessCallDatas.get(simpleInfo);
        Long invokeStartTime = invokeStartPair.get(seqId);
        Long cost = invokeEndTime - invokeStartTime;
        Map<ServiceSimpleInfo, Long> map = new ConcurrentHashMap<>(1);
        map.put(simpleInfo, cost);
        serviceElapses.add(map);

        //synchronized ?
        if (null != processData) {
            processData.getTotalCalls().incrementAndGet();
            if (soaHeader.getRespCode().isPresent() && SUCCESS_CODE.equals(soaHeader.getRespCode().get())) {
                processData.getSucceedCalls().incrementAndGet();
            } else {
                processData.getFailCalls().incrementAndGet();
            }
        } else {
            ServiceProcessData newProcessData = createNewData(simpleInfo);
            newProcessData.setTotalCalls(new AtomicInteger(1));
            if (soaHeader.getRespCode().isPresent() && SUCCESS_CODE.equals(soaHeader.getRespCode().get())) {
                newProcessData.getSucceedCalls().incrementAndGet();
            } else {
                newProcessData.getFailCalls().incrementAndGet();
            }
            serviceProcessCallDatas.put(simpleInfo, newProcessData);
        }
        LOGGER.info("当前seqid{} ==> service:{}:version[]:method:[{}] 耗时 ==>{} ms 响应状态码[{}]", seqId, simpleInfo.getServiceName(), simpleInfo.getMethodName(), simpleInfo.getVersionName(), cost, soaHeader.getRespCode().get());

        ctx.write(msg, promise);
    }

    SoaInvokeMonitorHandler() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Long initialDelay = calendar.getTime().getTime() - System.currentTimeMillis();

        LOGGER.info("dapeng invoke Monitor started, upload interval:" + PERIOD * 1000 + "s");

        ScheduledExecutorService schedulerExecutorService = Executors.newScheduledThreadPool(2,
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("dapeng-monitor-scheduler-%d")
                        .build());
        ExecutorService uploaderExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("dapeng-monitor-uploader")
                .build());

        schedulerExecutorService.scheduleAtFixedRate(() -> {
            shareLock.lock();
            try {
                List<DataPoint> dataList = serviceData2Points(System.currentTimeMillis(),
                        serviceProcessCallDatas, serviceElapses);
                if (serviceDataQueue.size() >= (60 * 60 * 10 / PERIOD) * 0.9) {
                    serviceDataQueue.take();
                }
                if (!dataList.isEmpty()) {
                    serviceDataQueue.put(dataList);
                    serviceProcessCallDatas.clear();
                    serviceElapses.clear();
                    invokeStartPair.clear();
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                shareLock.unlock();
            }
        }, initialDelay, PERIOD * 1000, TimeUnit.MILLISECONDS);

        // wake up the uploader thread every PERIOD.
        schedulerExecutorService.scheduleAtFixedRate(() -> {
            try {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("remainder is working.");
                signalLock.lock();
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("remainder has woke up the uploader");
                signalCondition.signal();
            } finally {
                signalLock.unlock();
            }

        }, initialDelay, PERIOD * 1000, TimeUnit.MILLISECONDS);

        // uploader thread.
        uploaderExecutor.execute(() -> {
            while (true) {
                try {
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("uploader is working.");
                    signalLock.lock();
                    List<DataPoint> points = serviceDataQueue.peek();
                    if (points != null) {
                        try {
                            if (!points.isEmpty()) {
                                LOGGER.debug("uploading , submitPoints ");
                                System.out.println("====> submitPoints : " + points);
                                SERVICE_CLIENT.submitPoints(points);
                            }
                            serviceDataQueue.remove(points);
                        } catch (Throwable e) {
                            LOGGER.error(e.getMessage(), e);
                            signalCondition.await();
                        }
                    } else {
                        if (LOGGER.isDebugEnabled())
                            LOGGER.debug("no more tasks, uploader release the lock.");
                        signalCondition.await();
                    }
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                } finally {
                    signalLock.unlock();
                }
            }
        });

    }

    private ServiceProcessData createNewData(ServiceSimpleInfo simpleInfo) {
        ServiceProcessData newProcessData = new ServiceProcessData();
        newProcessData.setServerIP(SERVER_IP);
        newProcessData.setServerPort(SERVER_PORT);
        newProcessData.setServiceName(simpleInfo.getServiceName());
        newProcessData.setMethodName(simpleInfo.getMethodName());
        newProcessData.setVersionName(simpleInfo.getVersionName());
        newProcessData.setPeriod(PERIOD);
        newProcessData.setAnalysisTime(Calendar.getInstance().getTimeInMillis());

        return newProcessData;
    }

    private List<DataPoint> serviceData2Points(Long millis,
                                               Map<ServiceSimpleInfo, ServiceProcessData> spd,
                                               List<Map<ServiceSimpleInfo, Long>> elapses) {

        List<DataPoint> points = new ArrayList<>(spd.size());

        spd.forEach((serviceSimpleInfo, serviceProcessData) -> {

            final Long iTotalTime = elapses.stream()
                    .filter(x -> x.containsKey(serviceSimpleInfo)).map(y -> y.get(serviceSimpleInfo))
                    .reduce((m, n) -> (m + n)).get();
            final Long iMinTime = elapses.stream()
                    .filter(x -> x.containsKey(serviceSimpleInfo)).map(y -> y.get(serviceSimpleInfo))
                    .sorted()
                    .min(Comparator.naturalOrder()).get();
            final Long iMaxTime = elapses.stream()
                    .filter(x -> x.containsKey(serviceSimpleInfo)).map(y -> y.get(serviceSimpleInfo))
                    .sorted()
                    .max(Comparator.naturalOrder()).get();
            final Long iAverageTime = iTotalTime / elapses.stream()
                    .filter(x -> x.containsKey(serviceSimpleInfo)).map(y -> y.get(serviceSimpleInfo)).count();

            DataPoint point = new DataPoint();
            point.setDatabase(DATA_BASE);
            point.setBizTag("dapeng_service_process");
            Map<String, String> tags = new HashMap<>(16);
            tags.put("period", PERIOD + "");
            tags.put("analysis_time", millis.toString());
            tags.put("service_name", serviceSimpleInfo.getServiceName());
            tags.put("method_name", serviceProcessData.getMethodName());
            tags.put("version_name", serviceProcessData.getVersionName());
            tags.put("server_ip", SERVER_IP);
            tags.put("server_port", SERVER_PORT.toString());
            point.setTags(tags);
            Map<String, String> fields = new HashMap<>(16);
            fields.put("i_min_time", iMinTime.toString());
            fields.put("i_max_time", iMaxTime.toString());
            fields.put("i_average_time", iAverageTime.toString());
            fields.put("i_total_time", iTotalTime.toString());
            fields.put("total_calls", serviceProcessData.getTotalCalls().toString());
            fields.put("succeed_calls", String.valueOf(serviceProcessData.getSucceedCalls().get()));
            fields.put("fail_calls", String.valueOf(serviceProcessData.getFailCalls().get()));
            point.setValues(fields);
            points.add(point);
        });

        return points;
    }
}
