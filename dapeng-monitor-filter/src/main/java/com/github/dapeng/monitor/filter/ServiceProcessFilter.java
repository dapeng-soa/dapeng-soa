package com.github.dapeng.monitor.filter;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * author with struy.
 * Create by 2018/1/29 20:56
 * email :yq1724555319@gmail.com
 */

public class ServiceProcessFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(QpsFilter.class);

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
        try {
            next.onEntry(ctx);
        } catch (TException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {

    }
}
