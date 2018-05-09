package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.basic.api.counter.CounterServiceClient;
import com.github.dapeng.basic.api.counter.domain.DataPoint;
import com.github.dapeng.basic.api.counter.service.CounterService;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.impl.plugins.monitor.config.MonitorFilterProperties;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 流量计数器,包括请求跟响应的流量
 *
 * @author with struy.
 * Create by 2018/3/6 20:42
 * email :yq1724555319@gmail.com
 */
@ChannelHandler.Sharable
public class SoaFlowCounter extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaFlowCounter.class);
    private static final int PERIOD = MonitorFilterProperties.SOA_MONITOR_SERVICE_PROCESS_PERIOD;
    private static final String DATA_BASE = MonitorFilterProperties.SOA_MONITOR_INFLUXDB_DATABASE;
    private static final String NODE_IP = SoaSystemEnvProperties.SOA_CONTAINER_IP;
    private static final Integer NODE_PORT = SoaSystemEnvProperties.SOA_CONTAINER_PORT;
    private Collection<Long> requestFlows = new ConcurrentLinkedQueue<>();
    private Collection<Long> responseFlows = new ConcurrentLinkedQueue<>();
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

    private ArrayBlockingQueue<DataPoint> flowDataQueue = new ArrayBlockingQueue<>(MAX_SIZE);

    /**
     * 上送数据缓存队列,用于jmx数据监控
     */
    private static ArrayBlockingQueue<DataPoint> flowCacheQueue = new ArrayBlockingQueue<>(30);

    /**
     * 信号锁, 用于提醒线程跟数据上送线程的同步
     * 避免因为上送失败引起的线程异常
     */
    private ReentrantLock signalLock = new ReentrantLock();
    private Condition signalCondition = signalLock.newCondition();


    private ScheduledExecutorService schedulerExecutorService = Executors.newScheduledThreadPool(1,
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("dapeng-" + getClass().getSimpleName() + "-scheduler-%d")
                    .build());
    private ExecutorService uploaderExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("dapeng-" + getClass().getSimpleName() + "-uploader-%d")
            .build());

    private static class CounterClientFactory {
        private static CounterService COUNTER_CLIENT = new CounterServiceClient();
    }

    public static ArrayBlockingQueue<DataPoint> getFlowCacheQueue() {
        return flowCacheQueue;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(getClass().getSimpleName() + "::read");
        }
        Long requestFlow = (long) ((ByteBuf) msg).readableBytes();
        requestFlows.add(requestFlow);
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(getClass().getSimpleName() + "::write");
        }
        Long responseFlow = (long) ((ByteBuf) msg).readableBytes();
        responseFlows.add(responseFlow);
        ctx.write(msg, promise);
    }

    SoaFlowCounter() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Long initialDelay = calendar.getTime().getTime() - System.currentTimeMillis();

        LOGGER.info("dapeng flow Monitor started, upload interval:" + PERIOD + "s");


        // 定时统计时间段内的流量值并加入到上送队列
        schedulerExecutorService.scheduleAtFixedRate(() -> {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(Thread.currentThread().getName() + ", deamon[" + Thread.currentThread().isDaemon() + "]::statistics");
                }
                List<Long> requests = new ArrayList<>(requestFlows.size());
                requests.addAll(requestFlows);
                requestFlows.clear();
                List<Long> responses = new ArrayList<>(responseFlows.size());
                responses.addAll(responseFlows);
                responseFlows.clear();

                DataPoint point = flowInfo2Point(requests, responses);

                // 当容量达到最大容量的90%时
                if (flowDataQueue.size() >= ALERT_SIZE) {
                    LOGGER.warn(Thread.currentThread().getName() + "流量监控本地容量超过" + ALERT_SIZE);
                    while (flowDataQueue.size() >= NORMAL_SIZE)
                        flowDataQueue.remove();
                }
                if (null != point) {
                    flowDataQueue.put(point);

                    // 默认保留最新30条,缓存流量统计数据,offer,poll防止阻塞
                    if (!flowCacheQueue.offer(point)) {
                        flowCacheQueue.poll();
                        flowCacheQueue.offer(point);
                    }
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }, initialDelay, PERIOD * 1000, TimeUnit.MILLISECONDS);

        // wake up the uploader thread every PERIOD.
        schedulerExecutorService.scheduleAtFixedRate(() -> {
            try {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug(Thread.currentThread().getName() + "::reminder is working.");
                signalLock.lock();
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug(Thread.currentThread().getName() + "::reminder has woke up the uploader");
                signalCondition.signal();
            } finally {
                signalLock.unlock();
            }

        }, initialDelay + 50, PERIOD * 1000, TimeUnit.MILLISECONDS);

        // uploader point thread.
        uploaderExecutor.execute(() -> {
            //单次循环上送的数据记录大小
            int uploads = 0;
            while (true) {
                try {
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug(Thread.currentThread().getName() + "::uploader is working.");
                    signalLock.lock();
                    DataPoint point = flowDataQueue.peek();
                    if (null != point) {
                        try {
                            LOGGER.debug(Thread.currentThread().getName() + "::uploading submitPoint ");

                            CounterClientFactory.COUNTER_CLIENT.submitPoint(point);
                            flowDataQueue.remove(point);
                            uploads++;
                        } catch (Throwable e) {
                            // 上送出错
                            LOGGER.error(e.getMessage(), e);
                            if (LOGGER.isDebugEnabled())
                                LOGGER.debug(Thread.currentThread().getName() + " has upload " + uploads + " points, now release the lock.");
                            uploads = 0;
                            signalCondition.await();
                        }
                    } else {
                        if (LOGGER.isDebugEnabled())
                            LOGGER.debug(Thread.currentThread().getName() + " has upload " + uploads + " points, now release the lock.");
                        uploads = 0;
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

    /**
     * 将流量数据转换为Point
     *
     * @param requestFlows
     * @param responseFlows
     * @return
     */
    private DataPoint flowInfo2Point(List<Long> requestFlows, List<Long> responseFlows) {

        if (requestFlows.size() > 0 && responseFlows.size() > 0) {

            long maxRequestFlow = requestFlows.stream()
                    .sorted()
                    .max(Comparator.naturalOrder()).get();
            long minRequestFlow = requestFlows.stream()
                    .sorted()
                    .min(Comparator.naturalOrder()).get();
            long sumRequestFlow = requestFlows.stream()
                    .reduce((x, y) -> (x + y))
                    .get();
            long avgRequestFlow = sumRequestFlow / (long) requestFlows.size();

            long minResponseFlow = responseFlows.stream()
                    .sorted()
                    .min(Comparator.naturalOrder()).get();
            long maxResponseFlow = responseFlows.stream()
                    .sorted()
                    .max(Comparator.naturalOrder()).get();
            long sumResponseFlow = responseFlows.stream()
                    .reduce((x, y) -> (x + y))
                    .get();
            long avgResponseFlow = sumResponseFlow / (long) responseFlows.size();

            DataPoint point = new DataPoint();
            point.setDatabase(DATA_BASE);
            point.setBizTag("dapeng_node_flow");
            Map<String, String> tags = new HashMap<>(16);
            tags.put("period", String.valueOf(PERIOD));
            tags.put("analysis_time", String.valueOf(System.currentTimeMillis()));
            tags.put("node_ip", NODE_IP);
            tags.put("node_port", String.valueOf(NODE_PORT));
            point.setTags(tags);
            Map<String, Long> fields = new HashMap<>(16);
            fields.put("max_request_flow", maxRequestFlow);
            fields.put("min_request_flow", minRequestFlow);
            fields.put("sum_request_flow", sumRequestFlow);
            fields.put("avg_request_flow", avgRequestFlow);
            fields.put("max_response_flow", minResponseFlow);
            fields.put("min_response_flow", maxResponseFlow);
            fields.put("sum_response_flow", sumResponseFlow);
            fields.put("avg_response_flow", avgResponseFlow);
            point.setValues(fields);

            return point;

        } else {
            return null;
        }
    }

    /**
     * 停止上送线程
     *
     * @return
     */
    public void destory() {
        LOGGER.info(" stop flowCounter upload !");
        schedulerExecutorService.shutdown();
        uploaderExecutor.shutdown();
        LOGGER.info(" flowCounter is shutdown");
    }
}
