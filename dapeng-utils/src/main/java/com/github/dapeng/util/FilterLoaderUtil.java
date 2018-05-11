package com.github.dapeng.util;

import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * author with struy.
 * Create by 2018/2/6 19:04
 * email :yq1724555319@gmail.com
 */

public class FilterLoaderUtil {
    private static final String FILTER_EXCLUDES = SoaSystemEnvProperties.SOA_FILTER_EXCLUDES.trim();
    private static final String FILTER_INCLUDES = SoaSystemEnvProperties.SOA_FILTER_INCLUDES.trim();

    /**
     * need include ？
     *
     * @param filter
     * @return
     */
    public static Boolean included(Filter filter) {
        List<String> excludes = new ArrayList<>();
        List<String> includes = new ArrayList<>();

        if ("".equals(FILTER_EXCLUDES) && "".equals(FILTER_INCLUDES)) {
            return true;
        }

        if (!"".equals(FILTER_INCLUDES)) {
            includes.addAll(Arrays.asList(FILTER_INCLUDES.split(",")));
        }

        if (!"".equals(FILTER_EXCLUDES)) {
            excludes.addAll(Arrays.asList(FILTER_EXCLUDES.split(",")));
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
}
