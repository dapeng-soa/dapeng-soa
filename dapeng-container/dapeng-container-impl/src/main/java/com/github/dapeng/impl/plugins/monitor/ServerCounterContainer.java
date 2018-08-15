package com.github.dapeng.impl.plugins.monitor;

import com.github.dapeng.basic.api.counter.CounterServiceClient;
import com.github.dapeng.basic.api.counter.domain.DataPoint;
import com.github.dapeng.basic.api.counter.service.CounterService;
import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.impl.plugins.monitor.config.MonitorFilterProperties;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author ever
 * @date 2017-05-29
 */
public class ServerCounterContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCounterContainer.class);
    private final boolean MONITOR_ENABLE = SoaSystemEnvProperties.SOA_MONITOR_ENABLE;
    private final static ServerCounterContainer instance = new ServerCounterContainer();
    /**
     * channel计数器
     */
    private final AtomicInteger activeChannel = new AtomicInteger(0);
    private final AtomicInteger inactiveChannel = new AtomicInteger(0);
    private final AtomicInteger totalChannel = new AtomicInteger(0);

    /**
     * 流量计数器
     * 无锁设计,
     * key为某小时的第N分钟
     * value为该分钟内的流量列表(每个请求/响应直接放到列表中, 方便统计最大/最小/平均值)
     */
    private final Map<Integer, List<Long>> reqFlows = new HashMap<>(64);
    private final Map<Integer, List<Long>> respFlows = new HashMap<>(64);
    /**
     * 服务耗时计数器
     */
    private Map<Integer, List<ElapseInfo>> serviceElapses = new HashMap<>(64);
    /**
     * 服务调用计数器
     */
    private Map<Integer, Map<ServiceBasicInfo, ServiceProcessData>> serviceInvocationDatas = new HashMap<>(64);


    private final String DATA_BASE = MonitorFilterProperties.SOA_MONITOR_INFLUXDB_DATABASE;
    private final String NODE_IP = SoaSystemEnvProperties.SOA_CONTAINER_IP;
    private final String NODE_PORT = String.valueOf(SoaSystemEnvProperties.SOA_CONTAINER_PORT);

    private final int PERIOD = MonitorFilterProperties.SOA_MONITOR_SERVICE_PROCESS_PERIOD;

    /**
     * 异常情况本地可存储10小时的数据.
     * 当本地容量达到90%时, 触发告警, 将会把部分消息丢弃, 降低到80%的水位
     */
    private final int MAX_SIZE = 60 * 60 * 10 / PERIOD;
    /**
     * 告警水位
     */
    private final int ALERT_SIZE = (int) (MAX_SIZE * 0.9);
    /**
     * 正常水位
     */
    private final int NORMAL_SIZE = (int) (MAX_SIZE * 0.8);

    /**
     * local cache for flow data
     */
    private final ArrayBlockingQueue<DataPoint> flowDataQueue = new ArrayBlockingQueue<>(MAX_SIZE);
    /**
     * local cache for service invoked counts
     */
    private ArrayBlockingQueue<List<DataPoint>> invokeDataQueue = new ArrayBlockingQueue<>(MAX_SIZE);

    /**
     * 信号锁, 用于提醒线程跟数据上送线程的同步
     */
    private ReentrantLock signalLock = new ReentrantLock();
    private Condition flowDataSignalCondition = signalLock.newCondition();
    private Condition invocationDataSignalCondition = signalLock.newCondition();

    private final ScheduledExecutorService schedulerExecutorService = Executors.newScheduledThreadPool(1,
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("dapeng-" + getClass().getSimpleName() + "-scheduler-%d")
                    .build());

    private final ExecutorService flowDataUploaderExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("dapeng-" + getClass().getSimpleName() + "-flowDataUploader-%d")
            .build());
    private final ExecutorService invocationDataUploaderExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("dapeng-" + getClass().getSimpleName() + "-invocationDataUploader-%d")
            .build());

    public static ServerCounterContainer getInstance() {
        return instance;
    }

    private ServerCounterContainer() {
        init();
    }

    public void increaseServiceCall(ServiceBasicInfo basicInfo, boolean isSucceed) {
        Integer currentMinute = currentMinuteOfHour();
        ServiceProcessData serviceProcessData = serviceInvocationDatas.get(currentMinute).get(basicInfo);
        if (serviceProcessData == null) {
            synchronized (serviceInvocationDatas) {
                serviceProcessData = serviceInvocationDatas.get(currentMinute).get(basicInfo);
                if (serviceProcessData == null) {
                    serviceProcessData = createNewData(basicInfo);
                    serviceInvocationDatas.get(currentMinute).put(basicInfo, serviceProcessData);
                }
            }
        }
        serviceProcessData.getTotalCalls().incrementAndGet();
        if (isSucceed) {
            serviceProcessData.getSucceedCalls().incrementAndGet();
        } else {
            serviceProcessData.getFailCalls().incrementAndGet();
        }
    }

    private static class CounterClientFactory {
        private static CounterService COUNTER_CLIENT = new CounterServiceClient();
    }


    /**
     * 停止上送线程
     *
     * @return
     */
    public void destory() {
        LOGGER.info(" stop flowCounter upload !");
        schedulerExecutorService.shutdown();
        flowDataUploaderExecutor.shutdown();
        invocationDataUploaderExecutor.shutdown();
        LOGGER.info(" flowCounter is shutdown");
    }

    public void addServiceElapseInfo(final ServiceBasicInfo serviceBasicInfo, final long cost) {
        serviceElapses.get(currentMinuteOfHour()).add(new ElapseInfo(serviceBasicInfo, cost));
    }

    public void addRequestFlow(long requestSize) {
        reqFlows.get(currentMinuteOfHour()).add(requestSize);
    }

    public void addResponseFlow(long responseSize) {
        respFlows.get(currentMinuteOfHour()).add(responseSize);
    }

    public int increaseActiveChannelAndGet() {
        return activeChannel.incrementAndGet();
    }

    public int decreaseActiveChannelAndGet() {
        return activeChannel.decrementAndGet();
    }

    public int getActiveChannel() {
        return activeChannel.get();
    }

    public int increaseInactiveChannelAndGet() {
        return inactiveChannel.incrementAndGet();
    }

    public int decreaseInactiveChannelAndGet() {
        return inactiveChannel.decrementAndGet();
    }

    public int getInactiveChannel() {
        return inactiveChannel.get();
    }

    public int increaseTotalChannelAndGet() {
        return totalChannel.incrementAndGet();
    }

    public int decreaseTotalChannelAndGet() {
        return totalChannel.decrementAndGet();
    }

    public int getTotalChannel() {
        return totalChannel.get();
    }

    public String getCurrentChannelStatus() {
        return activeChannel.get() + "/" + inactiveChannel.get() + "/" + totalChannel;
    }

    private void init() {
        for (int i = 0; i < 60; i++) {
            reqFlows.put(i, new ArrayList<>(1024));
            respFlows.put(i, new ArrayList<>(1024));
            serviceElapses.put(i, new ArrayList<>(1024));
            serviceInvocationDatas.put(i, new ConcurrentHashMap<>(1024));
        }

        if (MONITOR_ENABLE) {
            initThreads();
        }
    }

    /**
     * 获取当前时间所处的分钟
     *
     * @return
     */
    private int currentMinuteOfHour() {
        return (int) (System.currentTimeMillis() / 60000) % 60;
    }

    /**
     * 将上一分钟的流量数据转换为Point, 并把计数器置0
     *
     * @return
     */
    private DataPoint flowPointOfLastMinute() {
        int currentMinuteOfHour = currentMinuteOfHour();
        int oneMinuteBefore = (currentMinuteOfHour == 0) ? 59 : (currentMinuteOfHour - 1);

        List<Long> currentReqFlows = reqFlows.get(oneMinuteBefore);
        List<Long> currentRespFlows = respFlows.get(oneMinuteBefore);
        if (!currentReqFlows.isEmpty() || !currentRespFlows.isEmpty()) {
            long maxRequestFlow = 0;
            long minRequestFlow = 0;
            long sumRequestFlow = 0;
            long avgRequestFlow = 0;
            if (!currentReqFlows.isEmpty()) {
                maxRequestFlow = currentReqFlows.stream()
                        .sorted()
                        .max(Comparator.naturalOrder()).get();
                minRequestFlow = currentReqFlows.stream()
                        .sorted()
                        .min(Comparator.naturalOrder()).get();
                sumRequestFlow = currentReqFlows.stream()
                        .reduce((x, y) -> (x + y))
                        .get();
                avgRequestFlow = sumRequestFlow / (long) currentReqFlows.size();

                currentReqFlows.clear();
            }

            long minResponseFlow = 0;
            long maxResponseFlow = 0;
            long sumResponseFlow = 0;
            long avgResponseFlow = 0;
            if (!currentRespFlows.isEmpty()) {
                minResponseFlow = currentRespFlows.stream()
                        .sorted()
                        .min(Comparator.naturalOrder()).get();
                maxResponseFlow = currentRespFlows.stream()
                        .sorted()
                        .max(Comparator.naturalOrder()).get();
                sumResponseFlow = currentRespFlows.stream()
                        .reduce((x, y) -> (x + y))
                        .get();
                avgResponseFlow = sumResponseFlow / (long) currentRespFlows.size();

                currentRespFlows.clear();
            }


            DataPoint point = new DataPoint();
            point.setDatabase(DATA_BASE);
            point.setBizTag("dapeng_node_flow");
            long now = System.currentTimeMillis();
            Map<String, String> tags = new HashMap<>(4);
            tags.put("period", String.valueOf(PERIOD));
            tags.put("analysis_time", String.valueOf(now));
            tags.put("node_ip", NODE_IP);
            tags.put("node_port", String.valueOf(NODE_PORT));
            point.setTags(tags);
            Map<String, Long> fields = new HashMap<>(8);
            fields.put("max_request_flow", maxRequestFlow);
            fields.put("min_request_flow", minRequestFlow);
            fields.put("sum_request_flow", sumRequestFlow);
            fields.put("avg_request_flow", avgRequestFlow);
            fields.put("max_response_flow", maxResponseFlow);
            fields.put("min_response_flow", minResponseFlow);
            fields.put("sum_response_flow", sumResponseFlow);
            fields.put("avg_response_flow", avgResponseFlow);
            point.setValues(fields);
            point.setTimestamp(now);
            return point;

        } else {
            return null;
        }
    }

    public List<DataPoint> invokePointsOfLastMinute() {
        int currentMinuteOfHour = currentMinuteOfHour();
        int oneMinuteBefore = (currentMinuteOfHour == 0) ? 59 : (currentMinuteOfHour - 1);

        Map<ServiceBasicInfo, ServiceProcessData> invocationDatas = serviceInvocationDatas.get(oneMinuteBefore);
        List<ElapseInfo> elapses = serviceElapses.get(oneMinuteBefore);

        List<DataPoint> points = new ArrayList<>(invocationDatas.size());

        long now = System.currentTimeMillis();
        AtomicLong increment = new AtomicLong(0);
        invocationDatas.forEach((serviceBasicInfo, serviceProcessData) -> {
            final Optional<Long> iTotalTime = elapses.stream()
                    .filter(x -> x.serviceInfo.equals(serviceBasicInfo)).map(y -> Long.valueOf(y.cost))
                    .reduce((m, n) -> (m + n));
            if (iTotalTime.isPresent()) {
                final Long iMinTime = elapses.stream()
                        .filter(x -> x.serviceInfo.equals(serviceBasicInfo)).map(y -> Long.valueOf(y.cost))
                        .sorted()
                        .min(Comparator.naturalOrder()).get();
                final Long iMaxTime = elapses.stream()
                        .filter(x -> x.serviceInfo.equals(serviceBasicInfo)).map(y -> Long.valueOf(y.cost))
                        .sorted()
                        .max(Comparator.naturalOrder()).get();
                final Long iAverageTime = iTotalTime.get() / elapses.stream()
                        .filter(x -> x.serviceInfo.equals(serviceBasicInfo)).map(y -> Long.valueOf(y.cost)).count();

                DataPoint point = new DataPoint();
                point.setDatabase(DATA_BASE);
                point.setBizTag("dapeng_service_process");
                Map<String, String> tags = new HashMap<>(8);
                tags.put("period", String.valueOf(PERIOD));
                tags.put("analysis_time", String.valueOf(now));
                tags.put("service_name", serviceBasicInfo.getServiceName());
                tags.put("method_name", serviceProcessData.getMethodName());
                tags.put("version_name", serviceProcessData.getVersionName());
                tags.put("server_ip", NODE_IP);
                tags.put("server_port", NODE_PORT);
                point.setTags(tags);
                Map<String, Long> fields = new HashMap<>(8);
                fields.put("i_min_time", iMinTime);
                fields.put("i_max_time", iMaxTime);
                fields.put("i_average_time", iAverageTime);
                fields.put("i_total_time", iTotalTime.get());
                fields.put("total_calls", (long) serviceProcessData.getTotalCalls().get());
                fields.put("succeed_calls", (long) serviceProcessData.getSucceedCalls().get());
                fields.put("fail_calls", (long) serviceProcessData.getFailCalls().get());
                point.setValues(fields);
                point.setTimestamp(now + increment.incrementAndGet());

                points.add(point);
            }
        });

        elapses.clear();
        invocationDatas.clear();

        return points;
    }

    /**
     * Assume that the current second is 20, then initialDelay should be 40 + 5.
     * This could ensure that the task will be triggered exactly at 5 second of some minute.
     *
     * @return
     */
    private long initialDelay() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime().getTime() - System.currentTimeMillis() + 5000;
    }

    private void initThreads() {
        LOGGER.info("dapeng flow Monitor started, upload interval:" + PERIOD + "s");
        long initialDelay = initialDelay();

        // 定时统计时间段内的流量值并加入到上送队列
        schedulerExecutorService.scheduleAtFixedRate(() -> {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(Thread.currentThread().getName() + ", deamon[" + Thread.currentThread().isDaemon() + "]::statistics");
                }
                DataPoint flowPoint = flowPointOfLastMinute();

                if (null != flowPoint) {
                    flowDataQueue.put(flowPoint);
                }

                List<DataPoint> invocationDataList = invokePointsOfLastMinute();
                if (!invocationDataList.isEmpty()) {
                    invokeDataQueue.put(invocationDataList);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }, initialDelay, PERIOD * 1000, TimeUnit.MILLISECONDS);

        // wake up the uploader thread every PERIOD.
        schedulerExecutorService.scheduleAtFixedRate(() -> {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(Thread.currentThread().getName()
                            + "::reminder is working. trying to acquire the signalLock");
                }
                signalLock.lock();

                //检查水位
                checkWater();

                flowDataSignalCondition.signal();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(Thread.currentThread().getName()
                            + "::reminder got the signalLock and has woke up the uploader");
                }
            } finally {
                signalLock.unlock();
            }
        }, initialDelay + 10000, PERIOD * 1000, TimeUnit.MILLISECONDS);

        // uploader point thread.
        flowDataUploaderExecutor.execute(() -> {
            while (true) {
                try {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(Thread.currentThread().getName()
                                + "::uploader is working. trying to acquire the signalLock");
                    }
                    signalLock.lock();
                    flowDataSignalCondition.await();
                    submitFlowPoint();
                    invocationDataSignalCondition.signal();
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                } finally {
                    signalLock.unlock();
                }
            }
        });
        invocationDataUploaderExecutor.execute(() -> {
            while (true) {
                try {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(Thread.currentThread().getName()
                                + "::uploader is working. trying to acquire the signalLock");
                    }
                    signalLock.lock();
                    invocationDataSignalCondition.await();
                    submitInvokePoints();
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                } finally {
                    signalLock.unlock();
                }
            }
        });
    }

    /**
     * 检查水位
     * 当容量达到警戒水位时, 抛弃部分数据, 恢复到正常水位
     */
    private void checkWater() {
        if (flowDataQueue.size() >= ALERT_SIZE) {
            LOGGER.warn(Thread.currentThread().getName() + "流量数据本地容量超过警戒水位" + ALERT_SIZE);
            while (flowDataQueue.size() >= NORMAL_SIZE) {
                flowDataQueue.remove();
            }
        }

        if (invokeDataQueue.size() >= ALERT_SIZE) {
            LOGGER.warn(Thread.currentThread().getName() + "服务调用计数本地容量超过警戒水位" + ALERT_SIZE);
            while (invokeDataQueue.size() >= NORMAL_SIZE) {
                invokeDataQueue.remove();
            }
        }
    }

    private void submitFlowPoint() {
        AtomicInteger uploadCounter = new AtomicInteger(0);
        DataPoint point = flowDataQueue.peek();
        InvocationContext invocationContext = InvocationContextImpl.Factory.currentInstance();
        invocationContext.timeout(5000);
        while (point != null) {
            try {
                CounterClientFactory.COUNTER_CLIENT.submitPoint(point);
                flowDataQueue.remove(point);
                uploadCounter.incrementAndGet();
                point = flowDataQueue.peek();
            } catch (Throwable e) {
                // 上送出错
                LOGGER.error(e.getMessage(), e);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(Thread.currentThread().getName()
                            + " points:" + uploadCounter.get() + " uploaded before error, now release the lock.");
                }
                InvocationContextImpl.Factory.removeCurrentInstance();
                return;
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(Thread.currentThread().getName() + " no more points, total points:"
                    + uploadCounter.get() + "  uploaded");
        }
        InvocationContextImpl.Factory.removeCurrentInstance();
    }

    private void submitInvokePoints() {
        AtomicInteger uploadCounter = new AtomicInteger(0);
        List<DataPoint> points = invokeDataQueue.peek();
        InvocationContext invocationContext = InvocationContextImpl.Factory.currentInstance();
        invocationContext.timeout(5000);
        while (points != null) {
            try {
                if (!points.isEmpty()) {
                    LOGGER.debug(Thread.currentThread().getName() + "::uploading submitPoints ");

                    CounterClientFactory.COUNTER_CLIENT.submitPoints(points);
                    uploadCounter.addAndGet(points.size());
                }
                invokeDataQueue.remove(points);

                points = invokeDataQueue.peek();
            } catch (Throwable e) {
                // 上送出错
                LOGGER.error(e.getMessage(), e);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(Thread.currentThread().getName()
                            + " points:" + uploadCounter.get() + " uploaded before error, now  release the lock.");
                }
                return;
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(Thread.currentThread().getName() + " no more points, total points:" + uploadCounter.get()
                    + " uploaded, now release the lock.");
        }
        InvocationContextImpl.Factory.removeCurrentInstance();
    }

    private ServiceProcessData createNewData(ServiceBasicInfo basicInfo) {
        ServiceProcessData newProcessData = new ServiceProcessData();
        newProcessData.setServerIP(NODE_IP);
        newProcessData.setServerPort(SoaSystemEnvProperties.SOA_CONTAINER_PORT);
        newProcessData.setServiceName(basicInfo.getServiceName());
        newProcessData.setMethodName(basicInfo.getMethodName());
        newProcessData.setVersionName(basicInfo.getVersionName());
        newProcessData.setPeriod(PERIOD);
        newProcessData.setAnalysisTime(Calendar.getInstance().getTimeInMillis());

        return newProcessData;
    }

    class ElapseInfo {
        final ServiceBasicInfo serviceInfo;
        final long cost;

        ElapseInfo(ServiceBasicInfo serviceInfo, long cost) {
            this.serviceInfo = serviceInfo;
            this.cost = cost;
        }
    }
}
