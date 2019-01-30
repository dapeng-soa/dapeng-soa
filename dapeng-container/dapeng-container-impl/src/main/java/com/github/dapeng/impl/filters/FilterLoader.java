/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.impl.filters;

import com.github.dapeng.api.Container;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.InitializableFilter;
import com.github.dapeng.util.FilterLoaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ServiceLoader;

/**
 * @author with struy.
 * Create by 2018/1/29 20:56
 * email :yq1724555319@gmail.com
 */

public class FilterLoader {
    private static final Logger logger = LoggerFactory.getLogger("container.slowtime.log");

    public FilterLoader(Container container, List<ClassLoader> applicationCls) {

        // search container filters
        ServiceLoader<Filter> containerFilters = ServiceLoader.load(Filter.class, getClass().getClassLoader());
        for (Filter filter : containerFilters) {
            if (FilterLoaderUtil.included(filter)) {
                logger.info("FilterLoader :: container filters :: [{}]", filter.getClass().getSimpleName());
                container.registerFilter(filter);
                init(filter);
            }
        }

        // search application filters
        applicationCls.forEach(applicationCl -> {
            ServiceLoader<Filter> filters = ServiceLoader.load(Filter.class, applicationCl);
            for (Filter filter : filters) {
                if (FilterLoaderUtil.included(filter)) {
                    logger.info("FilterLoader :: application filters :: [{}]", filter.getClass().getSimpleName());
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
        if (filter instanceof InitializableFilter) {
            ((InitializableFilter) filter).init();
        }
    }
}
