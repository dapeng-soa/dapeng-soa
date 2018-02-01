package com.github.dapeng.client.filter;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.org.apache.thrift.TException;

/**
 * Created by lihuimin on 2017/12/23.
 */
public class LoadBalanceFilter implements Filter {
    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
        next.onEntry(ctx);

    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {

        prev.onExit(ctx);

    }
}
