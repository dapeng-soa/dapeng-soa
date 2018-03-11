package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.basic.api.counter.CounterServiceClient;
import com.github.dapeng.basic.api.counter.domain.DataPoint;
import com.github.dapeng.basic.api.counter.service.CounterService;
import com.github.dapeng.impl.plugins.monitor.config.MonitorFilterProperties;
import com.github.dapeng.util.SoaSystemEnvProperties;
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
 * @author with struy.
 * Create by 2018/3/6 20:42
 * email :yq1724555319@gmail.com
 * 单独的流量统计？
 * 只统计流量的出入，单独的上送任务？
 * 单独的数据存储表？
 */
//@ChannelHandler.Sharable ?
public class SoaMsgFlow extends ChannelDuplexHandler {
    private Logger LOGGER = LoggerFactory.getLogger(getClass());
    private static final int PERIOD = MonitorFilterProperties.SOA_MONITOR_SERVICE_PROCESS_PERIOD;
    private final String DATA_BASE = MonitorFilterProperties.SOA_MONITOR_INFLUXDB_DATABASE;
    private final String NODE_IP = SoaSystemEnvProperties.SOA_CONTAINER_IP;
    private final Integer NODE_PORT = SoaSystemEnvProperties.SOA_CONTAINER_PORT;
    private final CounterService SERVICE_CLIENT = new CounterServiceClient();
    private List<Long> requestFlows = new ArrayList<>();
    private List<Long> responseFlows = new ArrayList<>();
    /**
     * 异常情况存储10小时
     */
    private ArrayBlockingQueue<DataPoint> flowDataQueue = new ArrayBlockingQueue<>(60 * 60 * 10 / PERIOD);

    /**
     * 信号锁, 用于提醒线程跟数据上送线程的同步
     */
    private ReentrantLock signalLock = new ReentrantLock();
    private Condition signalCondition = signalLock.newCondition();
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Long requestFlow = (long) ((ByteBuf) msg).readableBytes();
        requestFlows.add(requestFlow);
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Long responseFlow = (long) ((ByteBuf) msg).readableBytes();
        responseFlows.add(responseFlow);
        ctx.write(msg, promise);
    }

    SoaMsgFlow() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Long initialDelay = calendar.getTime().getTime() - System.currentTimeMillis();

        LOGGER.info("dapeng flow Monitor started, upload interval:" + PERIOD * 1000 + "s");

        ScheduledExecutorService schedulerExecutorService = Executors.newScheduledThreadPool(2,
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("dapeng-monitor-scheduler-%d")
                        .build());
        ExecutorService uploaderExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("dapeng-monitor-uploader-%d")
                .build());

        schedulerExecutorService.scheduleAtFixedRate(() -> {
            try {
                DataPoint point = flowInfo2Point(requestFlows,responseFlows);
                if (flowDataQueue.size() >= (60 * 60 * 10 / PERIOD) * 0.9) {
                    flowDataQueue.take();
                }
                if (null!=point) {
                    flowDataQueue.put(point);
                    requestFlows.clear();
                    responseFlows.clear();
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
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
                    DataPoint point = flowDataQueue.peek();
                    if (null != point) {
                        try {
                            LOGGER.debug("uploading , submitPoint ");
                            System.out.println("====> flow submitPoint : " + point);
                            SERVICE_CLIENT.submitPoint(point);
                            flowDataQueue.remove(point);
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

    private DataPoint flowInfo2Point(List<Long> requestFlows, List<Long> responseFlows) {

        if (requestFlows.size()>0&&responseFlows.size()>0){

            long maxRequestFlow = requestFlows.stream()
                    .sorted()
                    .max(Comparator.naturalOrder()).get();
            long minRequestFlow = requestFlows.stream()
                    .sorted()
                    .min(Comparator.naturalOrder()).get();
            long sumRequestFlow = requestFlows.stream()
                    .reduce((x,y) -> (x+y))
                    .get();
            long avgRequestFlow = sumRequestFlow/ (long) requestFlows.size();

            long minResponseFlow = responseFlows.stream()
                    .sorted()
                    .min(Comparator.naturalOrder()).get();
            long maxResponseFlow = responseFlows.stream()
                    .sorted()
                    .max(Comparator.naturalOrder()).get();
            long sumResponseFlow = responseFlows.stream()
                    .reduce((x,y) -> (x+y))
                    .get();
            long avgResponseFlow = sumResponseFlow/ (long) responseFlows.size();

            DataPoint point = new DataPoint();
            point.setDatabase(DATA_BASE);
            point.setBizTag("dapeng_node_flow");
            Map<String, String> tags = new HashMap<>(16);
            tags.put("period", PERIOD + "");
            tags.put("analysis_time",System.currentTimeMillis()+"");
            tags.put("node_ip",NODE_IP);
            tags.put("node_port",NODE_PORT+"");
            point.setTags(tags);
            Map<String, String> fields = new HashMap<>(16);
            fields.put("max_request_flow",maxRequestFlow+"");
            fields.put("min_request_flow",minRequestFlow+"");
            fields.put("sum_request_flow",sumRequestFlow+"");
            fields.put("avg_request_flow",avgRequestFlow+"");
            fields.put("max_response_flow",minResponseFlow+"");
            fields.put("min_response_flow",maxResponseFlow+"");
            fields.put("sum_response_flow",sumResponseFlow+"");
            fields.put("avg_response_flow",avgResponseFlow+"");
            point.setValues(fields);

            return point;

        }else {
            return null;
        }
    }
}
