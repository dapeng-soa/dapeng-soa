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
package com.github.dapeng.impl.plugins;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.impl.container.DapengContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Logback Container
 *
 * @author craneding
 * @date 16/1/18
 */
public class LogbackPlugin implements Plugin {
    private static Logger LOGGER = LoggerFactory.getLogger(LogbackPlugin.class);

    @Override
    public void start() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::start");

        try (InputStream logbackCnfgStream = new BufferedInputStream(DapengContainer.loadInputStreamInClassLoader("logback.xml"))) {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(lc);
            lc.reset();
            configurator.doConfigure(logbackCnfgStream);

            StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
        } catch (Exception e) {
            LOGGER.error("LogbackContainer failed, ignoring ..." + e.getMessage(), e);
            // throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::stop");
    }

}
