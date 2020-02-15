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
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.api.lifecycle.LifecycleProcessorFactory;
import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.core.lifecycle.LifeCycleAware;
import com.github.dapeng.impl.container.DapengApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SpringBootAppLoader implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringBootAppLoader.class);
    private final Container container;
    private final List<ClassLoader> appClassLoaders;
    private List<Object> springCtxs = new ArrayList<>();

    public SpringBootAppLoader(Container container, List<ClassLoader> appClassLoaders) {
        this.container = container;
        this.appClassLoaders = appClassLoaders;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void start() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::start");
        String entryMainClassName = System.getProperty("spring.boot.main.class");

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (ClassLoader appClassLoader : appClassLoaders) {
            try {
                Class<?> appCtxClass = appClassLoader.loadClass(entryMainClassName);

                Class<?> applicationCtx = appClassLoader.loadClass("org.springframework.boot.SpringApplication");
                Class<?>[] parameterTypes = new Class[]{Class[].class};
                Constructor<?> constructor = applicationCtx.getConstructor(parameterTypes);

                Thread.currentThread().setContextClassLoader(appClassLoader);


                Object bootApplication = constructor.newInstance((Object) new Class[]{appCtxClass});
                //Class<?>... primarySources
                Method runMethod = applicationCtx.getMethod("run", String[].class);

                Object appObject = runMethod.invoke(bootApplication, (Object) new String[]{});

                // SpringApplication(primarySources).run(args)

//                runMethod.invoke(null, appCtxClass, new String[]{});

//                Method mainMethod = appCtxClass.getMethod("main", Class.class, String[].class);
                //启动 SpringBoot
//                mainMethod.invoke(null, (Object) new String[]{});

//                Thread.currentThread().setContextClassLoader(appClassLoader);

                Class<?> springFactoryClass = appClassLoader.loadClass("com.github.dapeng.spring.SpringExtensionFactory");
                Map<String, ApplicationContext> contextMap = (Map<String, ApplicationContext>) springFactoryClass
                        .getMethod("getContexts").invoke(null);

                //setAppClassLoader
                Method setAppClassLoader = springFactoryClass.getMethod("setAppClassLoader", ClassLoader.class);
                setAppClassLoader.invoke(null, appClassLoader);

                Object applicationContext = contextMap.values().iterator().next();
                springCtxs.add(applicationContext);

                LOGGER.info("Got SpringBoot app: {}", applicationContext);

                Method method = applicationContext.getClass().getMethod("getBeansOfType", Class.class);

                Map<String, SoaServiceDefinition<?>> processorMap = (Map<String, SoaServiceDefinition<?>>)
                        method.invoke(applicationContext, appClassLoader.loadClass(SoaServiceDefinition.class.getName()));

                //获取所有实现了lifecycle的bean
                LifecycleProcessorFactory.getLifecycleProcessor().addLifecycles(((Map<String, LifeCycleAware>)
                        method.invoke(applicationContext, appClassLoader.loadClass(LifeCycleAware.class.getName()))).values());


                //TODO: 需要构造Application对象
                Map<String, ServiceInfo> appInfos = toServiceInfos(processorMap);
                Application application = new DapengApplication(new ArrayList<>(appInfos.values()), appClassLoader);

                //Start spring context
                LOGGER.info(" start to boot app");
//                Method startMethod = appCtxClass.getMethod("start");
//                startMethod.invoke(applicationContext);

                // IApplication app = new ...
                if (!application.getServiceInfos().isEmpty()) {
                    // fixme only registerApplication
                    Map<ProcessorKey, SoaServiceDefinition<?>> serviceDefinitionMap = toSoaServiceDefinitionMap(appInfos, processorMap);
                    container.registerAppProcessors(serviceDefinitionMap);

                    container.registerAppMap(toApplicationMap(serviceDefinitionMap, application));
                    //fire a zk event
                    container.registerApplication(application);
                }

                LOGGER.info(" ------------ SpringClassLoader: " + ContainerFactory.getContainer().getApplications());


            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
        }
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
            try {
                LOGGER.info(" start to close SpringApplication.....");
                Method method = context.getClass().getMethod("close");
                method.invoke(context);
            } catch (NoSuchMethodException e) {
                LOGGER.info(" failed to get context close method.....");
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOGGER.info(e.getMessage());
            }
        });
        LOGGER.warn("Plugin:SpringAppLoader stoped..");
    }

    private Map<String, ServiceInfo> toServiceInfos(Map<String, SoaServiceDefinition<?>> processorMap)
            throws Exception {

        Map<String, ServiceInfo> serviceInfoMap = new HashMap<>(processorMap.size());
        for (Map.Entry<String, SoaServiceDefinition<?>> processorEntry : processorMap.entrySet()) {
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
            // TODO
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
    }

    private Map<ProcessorKey, SoaServiceDefinition<?>> toSoaServiceDefinitionMap(
            Map<String, ServiceInfo> serviceInfoMap,
            Map<String, SoaServiceDefinition<?>> processorMap) {

        Map<ProcessorKey, SoaServiceDefinition<?>> serviceDefinitions = serviceInfoMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> new ProcessorKey(entry.getValue().serviceName, entry.getValue().version),
                        entry -> processorMap.get(entry.getKey())));
        return serviceDefinitions;
    }
}
