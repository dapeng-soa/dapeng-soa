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

import com.github.dapeng.api.Plugin;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.transaction.api.GlobalTransactionFactory;
import com.github.dapeng.transaction.api.service.GlobalTransactionProcessService;
import com.github.dapeng.transaction.api.service.GlobalTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

/**
 * TransactionContainer
 *
 * @author craneding
 * @date 16/4/11
 */
public class TransactionPlugin implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionPlugin.class);

    public static final String SPRING_CONFIG = "soa.spring.config";
    public static final String DEFAULT_SPRING_CONFIG = "META-INF/spring/services.xml";

    private ClassPathXmlApplicationContext context;

    @Override
    public void start() {
        if (SoaSystemEnvProperties.SOA_TRANSACTIONAL_ENABLE) {
            LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::start");
            String configPath = System.getProperty(SPRING_CONFIG);
            if (configPath == null || configPath.length() <= 0) {
                configPath = DEFAULT_SPRING_CONFIG;
            }

            try {
                List<String> xmlPaths = new ArrayList<>();

                Enumeration<URL> resources = TransactionPlugin.class.getClassLoader().getResources(configPath);

                while (resources.hasMoreElements()) {
                    URL nextElement = resources.nextElement();

                    if (nextElement.toString().matches(".*dapeng-transaction.*")) {
                        xmlPaths.add(nextElement.toString());
                    }
                }

                context = new ClassPathXmlApplicationContext(xmlPaths.toArray(new String[0]));
                context.start();

                Collection<GlobalTransactionService> services = context.getBeansOfType(GlobalTransactionService.class).values();
                Collection<GlobalTransactionProcessService> processServices = context.getBeansOfType(GlobalTransactionProcessService.class).values();
                if (services.iterator().hasNext()) {
                    GlobalTransactionFactory.setGlobalTransactionService(services.iterator().next());
                } else {
                    LOGGER.warn("----------- No GlobalTransactionService Found..-------");
                }
                if (processServices.iterator().hasNext()) {
                    GlobalTransactionFactory.setGlobalTransactionProcessService(processServices.iterator().next());
                } else {
                    LOGGER.warn("----------- No GlobalTransactionProcessService Found --------");
                }

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void stop() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::stop");
        if (context != null) {
            context.close();
        }
    }

}
