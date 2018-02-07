package com.github.dapeng.impl.filters;

import com.github.dapeng.api.Container;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.InitializableFilter;
import com.github.dapeng.util.FilterLoaderUtil;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author with struy.
 * Create by 2018/1/29 20:56
 * email :yq1724555319@gmail.com
 */

public class FilterLoader {

    public FilterLoader(Container container, List<ClassLoader> applicationCls) {

        // search container filters
        ServiceLoader<Filter> containerFilters = ServiceLoader.load(Filter.class, FilterLoader.class.getClassLoader());
        for (Filter filter : containerFilters) {
            if (FilterLoaderUtil.included(filter)) {
                container.registerFilter(filter);
                init(filter);
            }
        }

        // search application filters
        applicationCls.forEach(applicationCl -> {
            ServiceLoader<Filter> filters = ServiceLoader.load(Filter.class, applicationCl);
            for (Filter filter : filters) {
                if (FilterLoaderUtil.included(filter)) {
                    container.registerFilter(filter);
                    init(filter);
                }
            }
        });

    }

    /**
     * init filter task，if it exists
     *
     * @param filter
     */
    private void init(Filter filter) {
        if(filter instanceof InitializableFilter){
            ((InitializableFilter) filter).init();
        }
    }
}
