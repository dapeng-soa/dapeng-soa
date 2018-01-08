package com.github.dapeng.core.filter;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.SoaException;

/**
 * Created by lihuimin on 2017/12/11.
 */
public interface FilterChain {

    // execute current filter's onEntry
    void onEntry(FilterContext ctx) throws SoaException;

    // execute current filter's onExit
    void onExit(FilterContext ctx)throws SoaException;


}
