package com.github.dapeng.monitor.filter;

import com.github.dapeng.basic.api.counter.domain.DataPoint;
import com.github.dapeng.basic.api.counter.service.CounterService;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.filter.InitializableFilter;
import com.github.dapeng.counter.service.CounterServiceImpl;
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

/**
 * @author with struy.
 * Create by 2018/1/29 20:56
 * email :yq1724555319@gmail.com
 */

public class QpsFilter implements InitializableFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(QpsFilter.class);

    private static final int PERIOD = MonitorFilterProperties.SOA_MONITOR_QPS_PERIOD;
    private final String DATA_BASE = MonitorFilterProperties.SOA_MONITOR_INFLUXDB_DATABASE;
    private final String SERVER_IP = SoaSystemEnvProperties.SOA_CONTAINER_IP;
    private final Integer SERVER_PORT = SoaSystemEnvProperties.SOA_CONTAINER_PORT;
    private final CounterService SERVICE_CLIENT = new CounterServiceImpl();
    private Map<ServiceSimpleInfo, AtomicInteger> qpsStats = new ConcurrentHashMap<>(16);
    private ReentrantLock qpsLock = new ReentrantLock();
    /**
     * 异常情况下，可以保留10小时的统计数据， PERIOD单位是秒
     */
    private ArrayBlockingQueue<List<DataPoint>> qpsDataQueue = new ArrayBlockingQueue<>(60 * 60 * 10 / PERIOD);

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {

        SoaHeader soaHeader = TransactionContext.Factory.getCurrentInstance().getHeader();
        ServiceSimpleInfo simpleInfo = new ServiceSimpleInfo(soaHeader.getServiceName(), soaHeader.getMethodName(), soaHeader.getVersionName());

        qpsLock.lock();
        try {
            AtomicInteger qpsStat = qpsStats.get(simpleInfo);
            if (qpsStat != null) {
                qpsStat.incrementAndGet();
            } else {
                qpsStats.put(simpleInfo, new AtomicInteger(1));
            }
        } finally {
            qpsLock.unlock();
        }

        next.onEntry(ctx);

    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {
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

        LOGGER.info("QpsMonitorFilter 定时上送时间:{} 上送间隔:{}ms", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss S").format(calendar.getTime()), PERIOD * 1000);

        ScheduledExecutorService masterExecutorService = Executors.newSingleThreadScheduledExecutor();
        masterExecutorService.scheduleAtFixedRate(() -> {
            qpsLock.lock();
            try {
                qpsDataQueue.put(qpsData2Point(System.currentTimeMillis(), qpsStats));
                qpsStats.clear();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                qpsLock.unlock();
            }
        }, initialDelay, PERIOD * 1000, TimeUnit.MILLISECONDS);


        Thread workerThread = new Thread(() -> {
            while (true) {
                List<DataPoint> points = null;
                try {
                    points = qpsDataQueue.take();
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                try {
                    if (null != points && points.size() != 0) {
                        LOGGER.info("QpsFilter 上送时间:{}ms ", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss S").format(System.currentTimeMillis()));
                        SERVICE_CLIENT.submitPoints(points);
                    }
                } catch (SoaException e) {
                    // 上送失败将失败数据回滚到队列
                    try {
                        qpsDataQueue.put(points);
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
     * @param qps
     */
    private List<DataPoint> qpsData2Point(Long millis, Map<ServiceSimpleInfo, AtomicInteger> qps) {

        List<DataPoint> points = new ArrayList<>(qps.size());
        qps.forEach(((serviceSimpleInfo, count) -> {
            DataPoint point = new DataPoint();
            point.setBizTag("dapeng_qps");
            point.setDatabase(DATA_BASE);
            Map<String, String> tags = new ConcurrentHashMap<>(16);
            tags.put("service_name", serviceSimpleInfo.getServiceName());
            tags.put("method_name", serviceSimpleInfo.getMethodName());
            tags.put("version_name", serviceSimpleInfo.getVersionName());
            tags.put("server_ip", SERVER_IP);
            tags.put("server_port", SERVER_PORT.toString());
            tags.put("period", PERIOD + "");
            tags.put("analysis_time", millis.toString());
            point.setTags(tags);
            Map<String, String> fields = new ConcurrentHashMap<>(16);
            fields.put("call_count", count + "");
            point.setValues(fields);
            points.add(point);
        }));
        return points;
    }
}
