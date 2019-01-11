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
package com.github.dapeng.impl.filters.slow.service;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author huyj
 * @Created 2018/6/26 14:14
 */
public class SlowServiceCheckFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger("container.slowtime.log");

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
        if (SoaSystemEnvProperties.SOA_SLOW_SERVICE_CHECK_ENABLE) {
            SlowServiceCheckTask task = new SlowServiceCheckTask(ctx);
            ctx.setAttach(this, "slowServiceCheckTask", task);
            SlowServiceCheckTaskManager.addTask(task);
            if (!SlowServiceCheckTaskManager.hasStarted()) {
                //fixme lifecycle
                SlowServiceCheckTaskManager.start();
                logger.info("slow service check started");
            }
        }
        next.onEntry(ctx);
    }


    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {
        if (SoaSystemEnvProperties.SOA_SLOW_SERVICE_CHECK_ENABLE) {
            SlowServiceCheckTask task = (SlowServiceCheckTask) ctx.getAttach(this, "slowServiceCheckTask");
            SlowServiceCheckTaskManager.remove(task);
        }
        prev.onExit(ctx);
    }


}
