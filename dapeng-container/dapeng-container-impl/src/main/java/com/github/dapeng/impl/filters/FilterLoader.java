package com.github.dapeng.impl.filters;

import com.github.dapeng.core.filter.Filter;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ServiceLoader;

/**
 * author with struy.
 * Create by 2018/1/29 20:56
 * email :yq1724555319@gmail.com
 */

public class FilterLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterLoader.class);

    public List<Filter> load(){
        ServiceLoader<Filter> filters = ServiceLoader.load(Filter.class, FilterLoader.class.getClassLoader());
        for (Filter filter:filters) {
            LOGGER.info(filter.getClass().getSimpleName() +" filter loading ");
        }
        return Lists.newArrayList(filters);
    }
}
