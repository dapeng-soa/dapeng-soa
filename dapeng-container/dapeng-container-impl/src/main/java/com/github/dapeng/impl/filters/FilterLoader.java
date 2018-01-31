package com.github.dapeng.impl.filters;

import com.github.dapeng.api.Container;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.util.SoaSystemEnvProperties;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * author with struy.
 * Create by 2018/1/29 20:56
 * email :yq1724555319@gmail.com
 */

public class FilterLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterLoader.class);

    public List<Filter> load(Container container, List<ClassLoader> applicationCls){
        List<Filter> result = new ArrayList<>(16);

        // search container filters
        ServiceLoader<Filter> containerFilters = ServiceLoader.load(Filter.class, FilterLoader.class.getClassLoader());
        for (Filter filter:containerFilters) {
            result.add(filter);
            LOGGER.info(filter.getClass().getSimpleName() +" filter loading ");
        }

        // search application filters
        applicationCls.forEach(applicationCl->{
            ServiceLoader<Filter> filters = ServiceLoader.load(Filter.class, applicationCl);
            for (Filter filter:filters) {
                result.add(filter);
                LOGGER.info(filter.getClass().getSimpleName() +" filter loading ");
            }
        });

        String FilterExcludes = SoaSystemEnvProperties.SOA_FILTER_EXCLUDES;
        String FilterIncludes = SoaSystemEnvProperties.SOA_FILTER_INCLUDES;

        List<Filter> removeFilters = new ArrayList<>();
        for(Filter filter : result){
            if(!"".equals(FilterExcludes)){
                for (String pkg:FilterExcludes.trim().split(",")) {
                    if(pkg.equals(filter.getClass().getName())){
                        removeFilters.add(filter);
                    }
                }
            }
        }

        result.removeAll(removeFilters);

        return result;
    }
}
