package com.github.dapeng.impl.filters;

import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.filter.Filter;

/**
 * Created by lihuimin on 2017/12/8.
 */
public class FlowControlFilter implements Filter {

    public void controlFlow(){}

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) {

    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) {

    }
}
