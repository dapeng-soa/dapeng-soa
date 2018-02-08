package com.github.dapeng.impl.filters;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by lihuimin on 2017/12/8.
 */
public class FlowControlFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowControlFilter.class);

    public void controlFlow(){}

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
        next.onEntry(ctx);
    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev)  throws SoaException {
        prev.onExit(ctx);
    }
}
