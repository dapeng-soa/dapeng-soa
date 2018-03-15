package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.basic.api.counter.CounterServiceClient;
import com.github.dapeng.basic.api.counter.domain.DataPoint;
import com.github.dapeng.basic.api.counter.service.CounterService;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.impl.plugins.monitor.ServiceProcessData;
import com.github.dapeng.impl.plugins.monitor.ServiceBasicInfo;
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
 * 统计服务调用次数和耗时，包括成功失败的次数
 * 不用 @ChannelHandler.Sharable
 * 避免seqid重复
 * @author with struy.
 * Create by 2018/3/8 15:37
 * email :yq1724555319@gmail.com
 */
public class SoaInvokeCounter extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaInvokeCounter.class);
    private static final int PERIOD = MonitorFilterProperties.SOA_MONITOR_SERVICE_PROCESS_PERIOD;
    private static final String DATA_BASE = MonitorFilterProperties.SOA_MONITOR_INFLUXDB_DATABASE;
    private static final String SUCCESS_CODE = SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE;
    private static final String SERVER_IP = SoaSystemEnvProperties.SOA_CONTAINER_IP;
    private static final Integer SERVER_PORT = SoaSystemEnvProperties.SOA_CONTAINER_PORT;
    private final CounterService SERVICE_CLIENT = new CounterServiceClient();
    /**
     * 异常情况本地可存储10小时的数据.
     * 当本地容量达到90%时, 触发告警, 将会把部分消息丢弃, 降低到80%的水位
     */
    private static final int MAX_SIZE = 60 * 60 * 10 / PERIOD;
    /**
     * 告警水位
     */
    private static final int ALERT_SIZE = (int) (MAX_SIZE * 0.9);
    /**
     * 正常水位
     */
    private static final int NORMAL_SIZE = (int) (MAX_SIZE * 0.8);
    private ArrayBlockingQueue<List<DataPoint>> serviceDataQueue = new ArrayBlockingQueue<>(MAX_SIZE);
    /**
     * 请求的seqid信息与调用起时，匹配响应seqid做耗时运算
     */
    private Map<Integer, Long> invokeStartPair = new ConcurrentHashMap<>(16);
    private List<Map<ServiceBasicInfo, Long>> serviceElapses = new ArrayList<>();
    private Map<ServiceBasicInfo, ServiceProcessData> serviceProcessCallDatas = new ConcurrentHashMap<>(16);

    /**
     * 信号锁, 用于提醒线程跟数据上送线程的同步
     * 避免因为上送失败引起的线程异常
     */
    private ReentrantLock signalLock = new ReentrantLock();
    private Condition signalCondition = signalLock.newCondition();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        TransactionContext transactionContext = TransactionContext.Factory.getCurrentInstance();
        Integer seqId = transactionContext.getSeqid();
        invokeStartPair.put(seqId, System.currentTimeMillis());
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Long invokeEndTime = System.currentTimeMillis();
        // 异步返回不能从通过 TransactionContext.Factory.getCurrentInstance() 去拿context
        SoaResponseWrapper wrapper = (SoaResponseWrapper) msg;
        TransactionContext context = wrapper.transactionContext;
        SoaHeader soaHeader = context.getHeader();
        Integer seqId = context.getSeqid();

        ServiceBasicInfo basicInfo = new ServiceBasicInfo(soaHeader.getServiceName(), soaHeader.getMethodName(), soaHeader.getVersionName());
        ServiceProcessData processData = serviceProcessCallDatas.get(basicInfo);
        Long invokeStartTime = invokeStartPair.remove(seqId);
        Long cost = invokeEndTime - invokeStartTime;
        Map<ServiceBasicInfo, Long> map = new ConcurrentHashMap<>(1);
        map.put(basicInfo, cost);
        serviceElapses.add(map);

        // 存在服务信息增加计数，不存在则初始化服务信息
        if (null != processData) {
            processData.getTotalCalls().incrementAndGet();
            if (soaHeader.getRespCode().isPresent() && SUCCESS_CODE.equals(soaHeader.getRespCode().get())) {
                processData.getSucceedCalls().incrementAndGet();
            } else {
                processData.getFailCalls().incrementAndGet();
            }
        } else {
            ServiceProcessData newProcessData = createNewData(basicInfo);
            newProcessData.setTotalCalls(new AtomicInteger(1));
            if (soaHeader.getRespCode().isPresent() && SUCCESS_CODE.equals(soaHeader.getRespCode().get())) {
                newProcessData.getSucceedCalls().incrementAndGet();
            } else {
                newProcessData.getFailCalls().incrementAndGet();
            }
            serviceProcessCallDatas.put(basicInfo, newProcessData);
        }
        LOGGER.info("当前seqid{} ==> service:{}:version[{}]:method:[{}] 耗时 ==>{} ms 响应状态码[{}]", seqId, basicInfo.getServiceName(), basicInfo.getMethodName(), basicInfo.getVersionName(), cost, soaHeader.getRespCode().get());

        ctx.write(msg, promise);
    }

    SoaInvokeCounter() {
        ensureNotSharable();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Long initialDelay = calendar.getTime().getTime() - System.currentTimeMillis();

        LOGGER.info("dapeng invoke Monitor started, upload interval:" + PERIOD + "s");

        ScheduledExecutorService schedulerExecutorService = Executors.newScheduledThreadPool(2,
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("dapeng-monitor-scheduler-%d")
                        .build());
        ExecutorService uploaderExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("dapeng-monitor-uploader")
                .build());

        // 定时统计服务调用数据并加入到上送队列
        schedulerExecutorService.scheduleAtFixedRate(() -> {
            try {
                List<DataPoint> dataList = serviceData2Points(System.currentTimeMillis(),
                        serviceProcessCallDatas, serviceElapses);
                serviceProcessCallDatas.clear();
                serviceElapses.clear();
                invokeStartPair.clear();
                // 当容量达到最大容量的90%时,丢弃头部数据，保留正常容量
                if (serviceDataQueue.size() >= ALERT_SIZE) {
                    LOGGER.warn("服务调用监控本地容量超过" + ALERT_SIZE);
                    while (serviceDataQueue.size() >= NORMAL_SIZE)
                        serviceDataQueue.take();
                }
                if (!dataList.isEmpty()) {
                    serviceDataQueue.put(dataList);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }, initialDelay, PERIOD * 1000, TimeUnit.MILLISECONDS);

        // wake up the uploader thread every PERIOD.
        schedulerExecutorService.scheduleAtFixedRate(() -> {
            try {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("reminder is working.");
                signalLock.lock();
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("reminder has woke up the uploader");
                signalCondition.signal();
            } finally {
                signalLock.unlock();
            }

        }, initialDelay, PERIOD * 1000, TimeUnit.MILLISECONDS);

        // uploader point thread.
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
                                LOGGER.debug("uploading submitPoints ");
                                SERVICE_CLIENT.submitPoints(points);
                            }
                            serviceDataQueue.remove(points);
                        } catch (Throwable e) {
                            // 上送出错
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

    private ServiceProcessData createNewData(ServiceBasicInfo basicInfo) {
        ServiceProcessData newProcessData = new ServiceProcessData();
        newProcessData.setServerIP(SERVER_IP);
        newProcessData.setServerPort(SERVER_PORT);
        newProcessData.setServiceName(basicInfo.getServiceName());
        newProcessData.setMethodName(basicInfo.getMethodName());
        newProcessData.setVersionName(basicInfo.getVersionName());
        newProcessData.setPeriod(PERIOD);
        newProcessData.setAnalysisTime(Calendar.getInstance().getTimeInMillis());

        return newProcessData;
    }

    /**
     * 将服务信息转换为Points
     * @param millis
     * @param spd
     * @param elapses
     * @return
     */
    private List<DataPoint> serviceData2Points(Long millis,
                                               Map<ServiceBasicInfo, ServiceProcessData> spd,
                                               List<Map<ServiceBasicInfo, Long>> elapses) {

        List<DataPoint> points = new ArrayList<>(spd.size());

        spd.forEach((serviceBasicInfo, serviceProcessData) -> {

            final Long iTotalTime = elapses.stream()
                    .filter(x -> x.containsKey(serviceBasicInfo)).map(y -> y.get(serviceBasicInfo))
                    .reduce((m, n) -> (m + n)).get();
            final Long iMinTime = elapses.stream()
                    .filter(x -> x.containsKey(serviceBasicInfo)).map(y -> y.get(serviceBasicInfo))
                    .sorted()
                    .min(Comparator.naturalOrder()).get();
            final Long iMaxTime = elapses.stream()
                    .filter(x -> x.containsKey(serviceBasicInfo)).map(y -> y.get(serviceBasicInfo))
                    .sorted()
                    .max(Comparator.naturalOrder()).get();
            final Long iAverageTime = iTotalTime / elapses.stream()
                    .filter(x -> x.containsKey(serviceBasicInfo)).map(y -> y.get(serviceBasicInfo)).count();

            DataPoint point = new DataPoint();
            point.setDatabase(DATA_BASE);
            point.setBizTag("dapeng_service_process");
            Map<String, String> tags = new HashMap<>(16);
            tags.put("period", String.valueOf(PERIOD));
            tags.put("analysis_time", millis.toString());
            tags.put("service_name", serviceBasicInfo.getServiceName());
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
