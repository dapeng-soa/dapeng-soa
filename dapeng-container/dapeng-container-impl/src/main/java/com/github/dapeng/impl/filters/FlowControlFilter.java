package com.github.dapeng.impl.filters;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lihuimin
 * @date 2017/12/8
 */
public class FlowControlFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowControlFilter.class);

    public void controlFlow() {
    }

    @Override
    public void onEntry(FilterContext filterContext, FilterChain next) throws SoaException {
        if (LOGGER.isDebugEnabled()) {
            TransactionContext transactionContext = (TransactionContext) filterContext.getAttribute("context");
            LOGGER.debug(getClass().getSimpleName() + "::onEntry[seqId:" + transactionContext.getSeqid() + "]");
        }
        next.onEntry(filterContext);
    }

    @Override
    public void onExit(FilterContext filterContext, FilterChain prev) throws SoaException {
        if (LOGGER.isDebugEnabled()) {
            TransactionContext transactionContext = (TransactionContext) filterContext.getAttribute("context");
            LOGGER.debug(getClass().getSimpleName() + "::onExit[seqId:" + transactionContext.getSeqid() + "]");
        }
        prev.onExit(filterContext);
    }
}
