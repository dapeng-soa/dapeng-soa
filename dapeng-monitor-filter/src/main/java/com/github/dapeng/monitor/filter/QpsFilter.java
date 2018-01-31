package com.github.dapeng.monitor.filter;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.monitor.domain.ServiceInfo;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * author with struy.
 * Create by 2018/1/29 20:56
 * email :yq1724555319@gmail.com
 */

public class QpsFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(QpsFilter.class);
    private static final int period = 5;
    private static Map<ServiceInfo,AtomicInteger> qpsStats = new ConcurrentHashMap<>();

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
        TransactionContext context = (TransactionContext) ctx.getAttribute("context");
        SoaHeader soaHeader = context.getHeader();
        ServiceInfo serviceInfo = new ServiceInfo(soaHeader.getServiceName(),soaHeader.getMethodName(),soaHeader.getVersionName());

        if (qpsStats.containsKey(serviceInfo)){
            Integer count = qpsStats.get(serviceInfo).incrementAndGet();
            LOGGER.info(serviceInfo.toString()+" count :{}", count);
        }else {
            qpsStats.put(serviceInfo,new AtomicInteger(1));
        }
    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {

    }

    // 定时上送任务
    static {
        final AtomicInteger INDEX = new AtomicInteger(0);
        final Timer timer = new Timer("QpsFilter-Timer-" + INDEX.incrementAndGet());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        LOGGER.info("QpsMonitorFilter 定时上送时间:{} 上送间隔:{}ms", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss S").format(calendar.getTime()), period * 1000);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {

                    final Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    final long millis = calendar.getTimeInMillis();
                    uploadQPSStat(millis, ImmutableMap.copyOf(qpsStats));
                    qpsStats.clear();

                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }, calendar.getTime(), period * 1000);
    }

    // 上送
    public static void uploadQPSStat(Long millis,Map<ServiceInfo,AtomicInteger> qpsStats) throws SoaException {
        LOGGER.info("上送时间:{}ms  上送数据量{} ", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss S").format(millis), qpsStats.size());
        //todo
    }
}
