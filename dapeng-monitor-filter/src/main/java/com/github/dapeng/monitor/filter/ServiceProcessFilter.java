package com.github.dapeng.monitor.filter;

import com.github.dapeng.basic.api.counter.CounterServiceClient;
import com.github.dapeng.basic.api.counter.domain.DataPoint;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.SoaHeaderSerializer;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.monitor.domain.MonitorProperties;
import com.github.dapeng.monitor.domain.ServiceProcessData;
import com.github.dapeng.monitor.domain.ServiceSimpleInfo;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.util.SoaSystemEnvProperties;
import com.google.common.collect.ImmutableMap;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * author with struy.
 * Create by 2018/1/29 20:56
 * email :yq1724555319@gmail.com
 */

public class ServiceProcessFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProcessFilter.class);

    private static final int PERIOD = MonitorProperties.MONITOR_SERVICE_PROCESS_PERIOD;
    private static final String DATA_BASE = MonitorProperties.MONITOR_INFLUXDB_DATABASE;
    private static final String SERVER_IP = SoaSystemEnvProperties.SOA_CONTAINER_IP;
    private static final Integer SERVER_PORT = SoaSystemEnvProperties.SOA_CONTAINER_PORT;
    private Map<ServiceSimpleInfo, ServiceProcessData> serviceProcessCallDatas = new ConcurrentHashMap<>(16);
    private final CounterServiceClient SERVICE_CLIENT = new CounterServiceClient();


    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {

        // start time
        ctx.setAttach(this,"currentServiceStart",System.currentTimeMillis());
        try {
            next.onEntry(ctx);
        } catch (TException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {

        SoaHeader soaHeader = ((TransactionContext)ctx.getAttribute("context")).getHeader();
        try {
            int responseFlow = ctx.getAttribute("respSerializer").toString().getBytes("utf-8").length;
            int requestFlow = ctx.getAttribute("reqSerializer").toString().getBytes("utf-8").length;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        String result = ctx.getAttribute("result").toString();
        Long start  = Long.valueOf(ctx.getAttach(this,"currentServiceStart").toString());
        final Long end = System.currentTimeMillis();

        ServiceSimpleInfo simpleInfo = new ServiceSimpleInfo(soaHeader.getServiceName(), soaHeader.getMethodName(), soaHeader.getVersionName());

        if (serviceProcessCallDatas.containsKey(simpleInfo)) {
            serviceProcessCallDatas.get(simpleInfo).getTotalCalls().incrementAndGet();
        } else {
            ServiceProcessData  processData = new ServiceProcessData();
            processData.setServerIP(SERVER_IP);
            processData.setServerPort(SERVER_PORT);
            processData.setServiceName(simpleInfo.getServiceName());
            processData.setMethodName(simpleInfo.getMethodName());
            processData.setVersionName(simpleInfo.getVersionName());
            processData.setPeriod(PERIOD);
            processData.setAnalysisTime();
            serviceProcessCallDatas.put(simpleInfo, new AtomicInteger(1));
        }



        LOGGER.debug("start ==>" +start+ " end with ==>" +end + serviceProcessCallDatas);

    }


    public void init() {
        final AtomicInteger INDEX = new AtomicInteger(0);
        final Timer timer = new Timer("ServiceProcessFilter-Timer-" + INDEX.incrementAndGet());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        LOGGER.debug("ServiceProcessFilter 定时上送时间:{} 上送间隔:{}ms", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss S").format(calendar.getTime()), PERIOD * 1000);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    final Calendar calendar = Calendar.getInstance();
                    final long millis = calendar.getTimeInMillis();
                    uploadServiceProcessData(millis, ImmutableMap.copyOf(serviceProcessCallDatas));
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }, calendar.getTime(), PERIOD * 1000);
    }


    /**
     * upload
     * @param millis
     * @param qps
     */
    private void uploadServiceProcessData(Long millis, Map<ServiceSimpleInfo, AtomicInteger> qps) {
        LOGGER.debug("上送时间:{}ms  上送数据{} ", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss S").format(millis), qps);

        List<DataPoint> points = new ArrayList<>(16);

        qps.forEach(((serviceSimpleInfo, atomicInteger) -> {
            DataPoint point = new DataPoint();
            Map<String,String> tag =  new ConcurrentHashMap<>(16);
            tag.put("period",PERIOD+"");
                tag.put("analysisTime",millis.toString());
            tag.put("serviceName","");
            tag.put("methodName","");
            tag.put("versionName","");
            tag.put("serverIP","");
            tag.put("serverPort","");
                tag.put("pMinTime","");
                tag.put("pMaxTime","");
                tag.put("pAverageTime","");
                tag.put("pTotalTime","");
                tag.put("iMinTime","");
                tag.put("iMaxTime","");
                tag.put("iAverageTime","");
                tag.put("iTotalTime","");
                tag.put("totalCalls","");
                tag.put("succeedCalls","");
                tag.put("failCalls","");
                tag.put("requestFlow","");
                tag.put("responseFlow","");
            point.setBizTag("dapeng_service_process");
            point.setDatabase(DATA_BASE);
            point.setTags(tag);
            point.setValue("");
            points.add(point);
        }));

        try {
            LOGGER.debug("上送");
            //SERVICE_CLIENT.submitPoints(points);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }finally {
            serviceProcessCallData.clear();
        }
    }


}
