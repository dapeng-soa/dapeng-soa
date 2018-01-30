package com.github.dapeng.monitor.filter;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.monitor.domain.QPSStat;
import com.github.dapeng.util.SoaSystemEnvProperties;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * author with struy.
 * Create by 2018/1/29 20:56
 * email :yq1724555319@gmail.com
 */

public class QpsFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(QpsFilter.class);
    private static final AtomicInteger INDEX = new AtomicInteger(0);

    private int period = 5;
    private final Timer timer = new Timer("QpsFilter-Timer-" + INDEX.incrementAndGet());
    private static List<QPSStat> qpsStats = new ArrayList<>();

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
        TransactionContext context = (TransactionContext) ctx.getAttribute("context");
        SoaHeader soaHeader = context.getHeader();
        QPSStat qpsStat = new QPSStat();

        qpsStat.setServerIP(SoaSystemEnvProperties.SOA_CONTAINER_IP);
        qpsStat.setServerPort(SoaSystemEnvProperties.SOA_CONTAINER_PORT);
        qpsStat.setServiceName(soaHeader.getServiceName());
        qpsStat.setMethodName(soaHeader.getMethodName());
        /*qpsStat.setAnalysisTime(System.currentTimeMillis());*/
        qpsStat.setPeriod(period*1000);
        qpsStat.setCallCount(1);
        qpsStat.setVersionName(soaHeader.getVersionName());

        qpsStats.add(qpsStat);

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
                    uploadQPSStat(millis,ImmutableList.copyOf(qpsStats));

                    qpsStats.clear();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }, calendar.getTime(), period * 1000);
    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {

    }

    // 上送
    public void uploadQPSStat(Long millis,List<QPSStat> qpsStats) throws SoaException {
        LOGGER.info("上送时间:{}ms  上送数据量{} ", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss S").format(millis), qpsStats.size());
        //todo
    }
}
