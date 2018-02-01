package com.github.dapeng.core.filter;


import com.github.dapeng.core.SoaException;

/**
 * Created by lihuimin on 2017/12/11.
 */
public interface Filter {

    void onEntry(FilterContext ctx, FilterChain next) throws SoaException;

    void onExit(FilterContext ctx, FilterChain prev) throws SoaException;

}
