package com.github.dapeng.monitor.filter;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import com.github.dapeng.counter.service.CounterServiceImpl;

/**
 * author with struy.
 * Create by 2018/1/29 20:56
 * email :yq1724555319@gmail.com
 */

public class ServiceProcessFilter implements InitializableFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProcessFilter.class);

    private static final int PERIOD = MonitorFilterProperties.SOA_MONITOR_SERVICE_PROCESS_PERIOD;
    private final String DATA_BASE = MonitorFilterProperties.SOA_MONITOR_INFLUXDB_DATABASE;
    private final String SERVER_IP = SoaSystemEnvProperties.SOA_CONTAINER_IP;
    private final Integer SERVER_PORT = SoaSystemEnvProperties.SOA_CONTAINER_PORT;
    private final String SUCCESS_CODE = "0000";
    private final CounterService SERVICE_CLIENT = new CounterServiceImpl();
    private Map<ServiceSimpleInfo, ServiceProcessData> serviceProcessCallDatas = new ConcurrentHashMap<>(16);
    private final ThreadLocal<Long> SERVICE_LOCAL = new ThreadLocal<>();
    private List<Map<ServiceSimpleInfo, Long>> serviceElapses = new ArrayList<>();
    private ReentrantLock serviceLock = new ReentrantLock();
    /**
     * 异常情况下，可以保留10小时的统计数据
     */
    private ArrayBlockingQueue<List<DataPoint>> serviceDataQueue = new ArrayBlockingQueue<>(60 * 60 * 10/PERIOD);


    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
        // start time Todo ThreadLocal？
        SERVICE_LOCAL.set(System.currentTimeMillis());
        next.onEntry(ctx);
    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {

        SoaHeader soaHeader = ((TransactionContext) ctx.getAttribute("context")).getHeader();
        final Long start = SERVICE_LOCAL.get();
        SERVICE_LOCAL.remove();
        final Long end = System.currentTimeMillis();
        ServiceSimpleInfo simpleInfo = new ServiceSimpleInfo(soaHeader.getServiceName(), soaHeader.getMethodName(), soaHeader.getVersionName());
        Map<ServiceSimpleInfo, Long> map = new ConcurrentHashMap<>(16);
        map.put(simpleInfo, end - start);
        serviceElapses.add(map);

        LOGGER.info("ServiceProcessFilter -" + SERVER_IP + SERVER_PORT + ":[" + simpleInfo.getMethodName() + "]" + " 耗时 ==>" + (end - start) + "ms");
        serviceLock.lock();
        try {
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
                ServiceProcessData newProcessData = new ServiceProcessData();
                newProcessData.setServerIP(SERVER_IP);
                newProcessData.setServerPort(SERVER_PORT);
                newProcessData.setServiceName(simpleInfo.getServiceName());
                newProcessData.setMethodName(simpleInfo.getMethodName());
                newProcessData.setVersionName(simpleInfo.getVersionName());
                newProcessData.setPeriod(PERIOD);
                newProcessData.setAnalysisTime(Calendar.getInstance().getTimeInMillis());
                newProcessData.setTotalCalls(new AtomicInteger(1));

                if (soaHeader.getRespCode().isPresent() && SUCCESS_CODE.equals(soaHeader.getRespCode().get())) {
                    newProcessData.getSucceedCalls().incrementAndGet();
                } else {
                    newProcessData.getSucceedCalls().incrementAndGet();
                }

                serviceProcessCallDatas.put(simpleInfo, newProcessData);
            }
        }finally {
            serviceLock.unlock();
        }
        prev.onExit(ctx);
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

        LOGGER.info("ServiceProcessFilter 定时上送时间:{} 上送间隔:{}ms", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss S").format(calendar.getTime()), PERIOD * 1000);

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(()->{
            serviceLock.lock();
            try {
                serviceDataQueue.put(serviceData2Points(System.currentTimeMillis(), serviceProcessCallDatas, serviceElapses));
                serviceProcessCallDatas.clear();
                serviceElapses.clear();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }finally {
                serviceLock.unlock();
            }
        }, initialDelay, PERIOD * 1000, TimeUnit.MILLISECONDS);

        Thread workerThread = new Thread(() -> {
            while (true) {
                List<DataPoint> points = null;
                try {
                    points =  serviceDataQueue.take();
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                try {
                    if (points != null && points.size() !=0 ){
                        LOGGER.debug("ServiceProcessFilter 上送时间:{}ms ", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss S").format(System.currentTimeMillis()));
                        SERVICE_CLIENT.submitPoints(points);
                    }
                } catch (Exception e) {
                    try {
                        serviceDataQueue.put(points);
                    } catch (InterruptedException e1) {
                        LOGGER.error(e.getMessage(), e);
                    }
                    LOGGER.error(e.getMessage(), e);
                }
            }
        });
        workerThread.setDaemon(true);

        workerThread.start();
    }


    /**
     * upload
     *
     * @param millis
     * @param
     */
    private List<DataPoint> serviceData2Points(Long millis, Map<ServiceSimpleInfo, ServiceProcessData> spd, List<Map<ServiceSimpleInfo, Long>> elapses) {

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
            Map<String, String> tag = new ConcurrentHashMap<>(16);
            tag.put("period", PERIOD + "");
            tag.put("analysis_time", millis.toString());
            tag.put("service_name", serviceSimpleInfo.getServiceName());
            tag.put("method_name", serviceProcessData.getMethodName());
            tag.put("version_name", serviceProcessData.getVersionName());
            tag.put("server_ip", SERVER_IP);
            tag.put("server_port", SERVER_PORT.toString());
            point.setTags(tag);
            Map<String, String> fields = new ConcurrentHashMap<>(16);
            fields.put("i_min_time",iMinTime.toString());
            fields.put("i_max_time",iMaxTime.toString());
            fields.put("i_average_time",iAverageTime.toString());
            fields.put("i_total_time",iTotalTime.toString());
            fields.put("total_calls",serviceProcessData.getTotalCalls().toString());
            fields.put("succeed_calls",serviceProcessData.getSucceedCalls().get()+"");
            fields.put("fail_calls",serviceProcessData.getFailCalls().get()+"");
            point.setValues(fields);
            points.add(point);
        });

        return points;
    }


}
