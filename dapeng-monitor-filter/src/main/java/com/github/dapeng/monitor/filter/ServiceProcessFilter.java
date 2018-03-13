package com.github.dapeng.monitor.filter;

import com.github.dapeng.basic.api.counter.CounterServiceClient;
import com.github.dapeng.basic.api.counter.domain.DataPoint;
import com.github.dapeng.basic.api.counter.service.CounterService;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.filter.InitializableFilter;
import com.github.dapeng.monitor.domain.ServiceProcessData;
import com.github.dapeng.monitor.domain.ServiceSimpleInfo;
import com.github.dapeng.monitor.util.MonitorFilterProperties;
import com.github.dapeng.util.SoaSystemEnvProperties;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author with struy.
 * Create by 2018/1/29 20:56
 * email :yq1724555319@gmail.com
 */

public class ServiceProcessFilter implements InitializableFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProcessFilter.class);

    private static final int PERIOD = MonitorFilterProperties.SOA_MONITOR_SERVICE_PROCESS_PERIOD;
    private final String DATA_BASE = MonitorFilterProperties.SOA_MONITOR_INFLUXDB_DATABASE;
    private final String SERVER_IP = SoaSystemEnvProperties.SOA_CONTAINER_IP;
    private final Integer SERVER_PORT = SoaSystemEnvProperties.SOA_CONTAINER_PORT;
    private final String SUCCESS_CODE = SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE;
    private final CounterService SERVICE_CLIENT = new CounterServiceClient();
    private Map<ServiceSimpleInfo, ServiceProcessData> serviceProcessCallDatas = new ConcurrentHashMap<>(16);
    private List<Map<ServiceSimpleInfo, Long>> serviceElapses = new ArrayList<>();

    /**
     * 共享资源锁, 用于保护serviceProcessCallDatas的同步锁
     */
    private ReentrantLock shareLock = new ReentrantLock();
    /**
     * 信号锁, 用于提醒线程跟数据上送线程的同步
     */
    private ReentrantLock signalLock = new ReentrantLock();
    private Condition signalCondition = signalLock.newCondition();
    /**
     * 异常情况下，可以保留10小时的统计数据
     */
    private ArrayBlockingQueue<List<DataPoint>> serviceDataQueue = new ArrayBlockingQueue<>(60 * 60 * 10 / PERIOD);

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
        // start time
        ctx.setAttribute("invokeBeginTimeInMs", System.currentTimeMillis());
        next.onEntry(ctx);
    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {
        try {
            shareLock.lock();
            SoaHeader soaHeader = ((TransactionContext) ctx.getAttribute("context")).getHeader();

            final Long invokeBeginTimeInMs = (Long) ctx.getAttribute("invokeBeginTimeInMs");
            final Long cost = System.currentTimeMillis() - invokeBeginTimeInMs;
            ServiceSimpleInfo simpleInfo = new ServiceSimpleInfo(soaHeader.getServiceName(), soaHeader.getMethodName(), soaHeader.getVersionName());
            Map<ServiceSimpleInfo, Long> map = new ConcurrentHashMap<>(16);
            map.put(simpleInfo, cost);
            serviceElapses.add(map);

            LOGGER.debug("ServiceProcessFilter - " + simpleInfo.getServiceName()
                    + ":" + simpleInfo.getMethodName() + ":[" + simpleInfo.getVersionName() + "] 耗时 ==>"
                    + cost + " ms");

            ServiceProcessData processData = serviceProcessCallDatas.get(simpleInfo);
            if (processData != null) {
                processData.getTotalCalls().incrementAndGet();
                processData.setAnalysisTime(Calendar.getInstance().getTimeInMillis());

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
                    newProcessData.getSucceedCalls().incrementAndGet();
                }

                serviceProcessCallDatas.put(simpleInfo, newProcessData);
            }
        } catch (Throwable e) {
            // Just swallow it
            LOGGER.error(e.getMessage(), e);
        } finally {
            shareLock.unlock();
        }

        prev.onExit(ctx);
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

    /**
     * init timer task and while uploading
     */
    @Override
    public void init() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Long initialDelay = calendar.getTime().getTime() - System.currentTimeMillis();

        LOGGER.info("ServiceProcessFilter started, upload interval:" + PERIOD * 1000 + "s");

        ScheduledExecutorService schedulerExecutorService = Executors.newScheduledThreadPool(2,
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("dapeng-monitor-filter-scheduler-%d")
                        .build());
        ExecutorService uploaderExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("dapeng-monitor-filter-uploader")
                .build());

        schedulerExecutorService.scheduleAtFixedRate(() -> {
            shareLock.lock();
            try {
                //todo check wether the queue size has reached 90%
                List<DataPoint> dataList = serviceData2Points(System.currentTimeMillis(),
                        serviceProcessCallDatas, serviceElapses);

                if (!dataList.isEmpty()) {
                    serviceDataQueue.put(dataList);
                    serviceProcessCallDatas.clear();
                    serviceElapses.clear();
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
                LOGGER.debug("remainder is working.");
                signalLock.lock();
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
                    LOGGER.debug("uploader is working.");
                    signalLock.lock();
                    List<DataPoint> points = serviceDataQueue.peek();
                    if (points != null) {
                        try {
                            if (!points.isEmpty()) {
                                SERVICE_CLIENT.submitPoints(points);
                            }
                            serviceDataQueue.remove(points);
                        } catch (Throwable e) {
                            LOGGER.error(e.getMessage(), e);
                            signalCondition.await();
                        }
                    } else { // no task, just release the lock
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

    @Override
    public void destroy() {
        //all threads in pools are daemons, no need to shutdown
    }


    /**
     * upload
     *
     * @param millis
     * @param
     */
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
            Map<String, String> tag = new HashMap<>(16);
            tag.put("period", PERIOD + "");
            tag.put("analysis_time", millis.toString());
            tag.put("service_name", serviceSimpleInfo.getServiceName());
            tag.put("method_name", serviceProcessData.getMethodName());
            tag.put("version_name", serviceProcessData.getVersionName());
            tag.put("server_ip", SERVER_IP);
            tag.put("server_port", SERVER_PORT.toString());
            point.setTags(tag);
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
