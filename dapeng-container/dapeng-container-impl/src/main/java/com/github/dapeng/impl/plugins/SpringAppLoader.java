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

import com.github.dapeng.api.Container;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.api.lifecycle.LifecycleProcessorFactory;
import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.core.lifecycle.LifeCycleAware;
import com.github.dapeng.core.lifecycle.LifeCycleEvent;
import com.github.dapeng.impl.container.DapengApplication;
import com.github.dapeng.spring.SpringExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class SpringAppLoader implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringAppLoader.class);

    private final Container container;
    private final List<ClassLoader> appClassLoaders;
    private List<ApplicationContext> springCtxs = new ArrayList<>();

    public SpringAppLoader(Container container, List<ClassLoader> appClassLoaders) {
        this.container = container;
        this.appClassLoaders = appClassLoaders;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void start() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::start");

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (ClassLoader appClassLoader : appClassLoaders) {

            Thread.currentThread().setContextClassLoader(appClassLoader);
            try {
                System.out.println("====1===");
                ApplicationContext applicationContext = tryApplicationContext(appClassLoader);
                if (applicationContext == null) {
                    applicationContext = trySpringXML(appClassLoader);
                }
                if (applicationContext == null) {
                    LOGGER.error("Not an dapeng application");
                } else {
                    launch(applicationContext, appClassLoader);
                    LOGGER.info("application started");
                }
            } finally {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
        }
    }

    private void launch(ApplicationContext appContext, ClassLoader appClassLoader) {
        SpringExtensionContext.setAppClassLoader(appClassLoader);
        appContext.onStart(new LifeCycleEvent(LifeCycleEvent.LifeCycleEventEnum.START));

        org.springframework.context.ApplicationContext applicationContext = SpringExtensionContext.getApplicationContext();

        Map<String, SoaServiceDefinition> definitions =
                applicationContext.getBeansOfType(SoaServiceDefinition.class);

        springCtxs.add(appContext);

        Map<String, ServiceInfo> serviceInfoMap = toServiceInfos(definitions);

        // LifeCycle Support, only service support Lifecyle now.
        List<LifeCycleAware> lifeCycleAwares = definitions.values().stream()
                .filter(definition -> LifeCycleAware.class.isInstance(definition.iface))
                .map(definition -> (LifeCycleAware) (definition.iface))
                .collect(Collectors.toList());

        LifecycleProcessorFactory.getLifecycleProcessor().addLifecycles(lifeCycleAwares);

        // Build an Application
        Application application = new DapengApplication(new ArrayList<>(serviceInfoMap.values()), appClassLoader);

        LOGGER.info("start to boot app");

        if (!application.getServiceInfos().isEmpty()) {
            // fixme only registerApplication
            Map<ProcessorKey, SoaServiceDefinition<?>> serviceDefinitionMap = toSoaServiceDefinitionMap(serviceInfoMap, definitions);
            container.registerAppProcessors(serviceDefinitionMap);

            container.registerAppMap(toApplicationMap(serviceDefinitionMap, application));
            //fire a zk event
            container.registerApplication(application);
        }
    }

    private ApplicationContext trySpringXML(ClassLoader appClassLoader) {
        System.out.println("trySpringXML here ...");
        try {
            Class<?> appCtxClass = appClassLoader.loadClass("org.springframework.context.support.ClassPathXmlApplicationContext");
            Class<?>[] parameterTypes = new Class[]{String[].class};
            Constructor<?> constructor = appCtxClass.getConstructor(parameterTypes);

            String configPath = "META-INF/spring/services.xml";

            if (null == appClassLoader.getResource(configPath)) {
                throw new RuntimeException("cant find " + configPath);
            } else {
                LOGGER.debug("found spring xml:" + appClassLoader.getResource(configPath));
            }

            Object springCtx = getSpringContext(configPath, appClassLoader, constructor);
            // start the context
            appCtxClass.getMethod("start").invoke(springCtx);
            SpringExtensionContext.setApplicationContext((org.springframework.context.ApplicationContext) springCtx);

//            Method method = appCtxClass.getMethod("getBeansOfType", Class.class);

            return new ApplicationContext() {
                @Override
                public void onStop(LifeCycleEvent event) {
                    try {
                        springCtx.getClass().getMethod("close")
                                .invoke(springCtx);
                    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
                        LOGGER.error("stop spring context failed", ex);
                    }
                }
            };
        } catch (ClassNotFoundException ex) { // No Spring
            System.out.println("can't found class" + ex);
            return null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IOException | InstantiationException ex) {
            System.out.println("can't found class" + ex);
            LOGGER.error("Initialize Spring failed", ex);
            throw new RuntimeException("Initialize Spring failed", ex);
        }
    }

    private ApplicationContext tryApplicationContext(ClassLoader appClassLoader) {
        try {
            ServiceLoader<ApplicationContext> loader = ServiceLoader.load(ApplicationContext.class, appClassLoader);
            Iterator<ApplicationContext> iterator = loader.iterator();
            while (iterator.hasNext()) {
                ApplicationContext context = iterator.next();
                //invoke main method
                Method mainMethod = context.getClass().getMethod("main", String[].class);
                //Invoke main
                mainMethod.invoke(null, (Object) new String[]{});

                LOGGER.debug("found an ApplicationContext:" + context.getClass());
                return context;
            }
        } catch (Exception e) {
            LOGGER.warn("TryApplicationContext got error: " + e.getMessage());
        }
        return null;
    }

    private Map<ProcessorKey, Application> toApplicationMap(Map<ProcessorKey,
            SoaServiceDefinition<?>> serviceDefinitionMap, Application application) {

        Map<ProcessorKey, Application> map = serviceDefinitionMap.keySet().stream().
                collect(Collectors.toMap(Function.identity(), processorKey -> application));
        return map;
    }

    @Override
    public void stop() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::stop");
        springCtxs.forEach(context -> {
            context.onStop(new LifeCycleEvent(LifeCycleEvent.LifeCycleEventEnum.STOP));
        });
        LOGGER.warn("Plugin:SpringAppLoader stoped..");
    }

    private Map<String, ServiceInfo> toServiceInfos(Map<String, SoaServiceDefinition> processorMap) {

        try {
            Map<String, ServiceInfo> serviceInfoMap = new HashMap<>(processorMap.size());
            for (Map.Entry<String, SoaServiceDefinition> processorEntry : processorMap.entrySet()) {
                String processorKey = processorEntry.getKey();
                SoaServiceDefinition<?> processor = processorEntry.getValue();

                long count = new ArrayList<>(Arrays.asList(processor.iface.getClass().getInterfaces()))
                        .stream()
                        .filter(m -> "org.springframework.aop.framework.Advised".equals(m.getName()))
                        .count();

                Class<?> ifaceClass = (Class) (count > 0 ?
                        processor.iface.getClass().getMethod("getTargetClass").invoke(processor.iface) :
                        processor.iface.getClass());

                Service service = processor.ifaceClass.getAnnotation(Service.class);
                //
                assert (service != null);

                /**
                 * customConfig 封装到 ServiceInfo 中
                 */
                Map<String, Optional<CustomConfigInfo>> methodsConfigMap = new HashMap<>();

                processor.functions.forEach((key, function) -> {
                    methodsConfigMap.put(key, function.getCustomConfigInfo());
                });

                //判断有没有 接口实现的版本号   默认为IDL定义的版本号
                ServiceVersion serviceVersionAnnotation = ifaceClass.isAnnotationPresent(ServiceVersion.class) ? ifaceClass.getAnnotationsByType(ServiceVersion.class)[0] : null;
                String version = serviceVersionAnnotation != null ? serviceVersionAnnotation.version() : service.version();

                //封装方法的慢服务时间
                HashMap<String, Long> methodsMaxProcessTimeMap = new HashMap<>(16);
                Arrays.asList(ifaceClass.getMethods()).forEach(item -> {
                    if (processor.functions.keySet().contains(item.getName())) {
                        long maxProcessTime = SoaSystemEnvProperties.SOA_MAX_PROCESS_TIME;
                        maxProcessTime = item.isAnnotationPresent(MaxProcessTime.class) ? item.getAnnotation(MaxProcessTime.class).maxTime() : maxProcessTime;
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("{}:{}:{} ; maxProcessTime:{} ", service.name(), version, item.getName(), maxProcessTime);
                        }
                        methodsMaxProcessTimeMap.put(item.getName(), maxProcessTime);
                    }
                });

                if (serviceVersionAnnotation == null || serviceVersionAnnotation.isRegister()) {
                    ServiceInfo serviceInfo = new ServiceInfo(service.name(), version, "service", ifaceClass, processor.getConfigInfo(), methodsConfigMap, methodsMaxProcessTimeMap);
                    serviceInfoMap.put(processorKey, serviceInfo);
                }

            }
            return serviceInfoMap;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException("Reflect service failed", ex);
        }
    }


    private Map<ProcessorKey, SoaServiceDefinition<?>> toSoaServiceDefinitionMap(
            Map<String, ServiceInfo> serviceInfoMap,
            Map<String, SoaServiceDefinition> processorMap) {

        Map<ProcessorKey, SoaServiceDefinition<?>> serviceDefinitions = serviceInfoMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> new ProcessorKey(entry.getValue().serviceName, entry.getValue().version),
                        entry -> processorMap.get(entry.getKey())));
        return serviceDefinitions;
    }


    private Object getSpringContext(String configPath, ClassLoader appClassLoader, Constructor<?> constructor) throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException {
        List<String> xmlPaths = new ArrayList<>();

        Enumeration<URL> resources = appClassLoader.getResources(configPath);

        while (resources.hasMoreElements()) {
            URL nextElement = resources.nextElement();
            // not load dapeng-soa-transaction-impl
            if (!nextElement.getFile().matches(".*dapeng-transaction-impl.*")) {
                xmlPaths.add(nextElement.toString());
            }
        }
        return constructor.newInstance(new Object[]{xmlPaths.toArray(new String[0])});
    }

}
