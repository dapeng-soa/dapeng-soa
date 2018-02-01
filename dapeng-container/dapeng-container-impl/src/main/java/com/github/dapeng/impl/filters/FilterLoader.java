package com.github.dapeng.impl.filters;

import com.github.dapeng.api.Container;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

/**
 * author with struy.
 * Create by 2018/1/29 20:56
 * email :yq1724555319@gmail.com
 */

public class FilterLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterLoader.class);
    private static final String FILTER_EXCLUDES = SoaSystemEnvProperties.SOA_FILTER_EXCLUDES.trim();
    private static final String FILTER_INCLUDES = SoaSystemEnvProperties.SOA_FILTER_INCLUDES.trim();

    public FilterLoader(Container container, List<ClassLoader> applicationCls) {

        // search container filters
        ServiceLoader<Filter> containerFilters = ServiceLoader.load(Filter.class, FilterLoader.class.getClassLoader());
        for (Filter filter : containerFilters) {
            if (included(filter)) {
                container.registerFilter(filter);
                init(filter);
            }
        }

        // search application filters
        applicationCls.forEach(applicationCl -> {
            ServiceLoader<Filter> filters = ServiceLoader.load(Filter.class, applicationCl);
            for (Filter filter : filters) {
                if (included(filter)) {
                    container.registerFilter(filter);
                    init(filter);
                }
            }
        });

    }

    /**
     * need include ？
     *
     * @param filter
     * @return
     */
    private Boolean included(Filter filter) {
        List<String> excludes = new ArrayList<>();
        List<String> includes = new ArrayList<>();

        if ("".equals(FILTER_EXCLUDES) && "".equals(FILTER_INCLUDES)) {
            return true;
        }
        if (!"".equals(FILTER_EXCLUDES)) {
            excludes.addAll(Arrays.asList(FILTER_EXCLUDES.split(",")));
        }

        if (!"".equals(FILTER_INCLUDES)) {
            includes.addAll(Arrays.asList(FILTER_INCLUDES.split(",")));
        }
        // default
        if (includes.size() == 0 && excludes.size() == 0) {
            return true;
        } else if (includes.size() > 0) {
            return includes.contains(filter.getClass().getName());
        } else {
            return !excludes.contains(filter.getClass().getName());
        }
    }

    /**
     * init filter task，if it exists
     *
     * @param filter
     */
    private void init(Filter filter) {
        for (Method method : filter.getClass().getMethods()) {
            if ("init".equals(method.getName())) {
                try {
                    method.invoke(filter.getClass().newInstance());
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }
}
