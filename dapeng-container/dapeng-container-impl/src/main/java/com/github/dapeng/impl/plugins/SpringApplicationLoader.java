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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class SpringApplicationLoader implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringAppLoader.class);

    private static final String CLASSPATH_XML_CONTEXT_NAME = "org.springframework.context.support.ClassPathXmlApplicationContext";
    private static final String CONFIG_PATH = "META-INF/spring/services.xml";

    private static final String APP_SPRING_EXTENSION_CTX_NAME = "com.github.dapeng.spring.SpringExtensionContext";
    private String SPRINGBOOT_ENTRY_CLASS_NAME = System.getProperty("springboot.main.class");

    private final Container container;
    private final List<ClassLoader> appClassLoaders;
    private List<Object> springContextList = new ArrayList<>();

    public SpringApplicationLoader(Container container, List<ClassLoader> appClassLoaders) {
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
                trySpring(appClassLoader);
                LOGGER.info("==> Load as Spring OK <====");
            } catch (Throwable ex) {
                try {
                    trySpringBoot(appClassLoader);
                    LOGGER.info("====> Load as SpringBoot OK <====");
                } catch (Throwable ex2) {
                    throw new RuntimeException("not a spring nor spingboot application ", ex2);
                }
            }
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    private boolean trySpring(ClassLoader appClassLoader) throws Exception {
        if (null == appClassLoader.getResource(CONFIG_PATH)) {
            LOGGER.warn("Loading classic classpath spring xml error, cause: Can't find " + CONFIG_PATH);
            throw new RuntimeException("Cant find " + CONFIG_PATH);
        }
        LOGGER.info("Trying to load classic Spring application.");
        Class<?> appCtxClass = appClassLoader.loadClass(CLASSPATH_XML_CONTEXT_NAME);
        Class<?>[] parameterTypes = new Class[]{String[].class};
        Constructor<?> constructor = appCtxClass.getConstructor(parameterTypes);
        Object applicationContext = getSpringContext(appClassLoader, constructor);
        springContextList.add(applicationContext);

        return afterSpringApplicationStart(appCtxClass, applicationContext, appClassLoader);
    }

    private boolean trySpringBoot(ClassLoader appClassLoader) throws Exception {
        Class<?> appBootEntryClass = appClassLoader.loadClass(SPRINGBOOT_ENTRY_CLASS_NAME);
        Method mainMethod = appBootEntryClass.getMethod("main", String[].class);
        //Invoke main
        mainMethod.invoke(null, (Object) new String[]{});

        Class<?> springExtensionClass = appClassLoader.loadClass(APP_SPRING_EXTENSION_CTX_NAME);
        Object applicationContext =
                springExtensionClass.getMethod("getApplicationContext").invoke(null);

        springContextList.add(applicationContext);
        LOGGER.info("Got SpringBoot app: {}", applicationContext);
        //setAppClassLoader
        Method setAppClassLoader = springExtensionClass.getMethod("setAppClassLoader", ClassLoader.class);
        setAppClassLoader.invoke(null, appClassLoader);

        return afterSpringApplicationStart(applicationContext.getClass(), applicationContext, appClassLoader);
    }

    /**
     * Spring 应用启动之后
     * TODO: 需要构造Application对象
     */
    private boolean afterSpringApplicationStart(Class<?> appCtxClass,
                                                Object applicationContext,
                                                ClassLoader appClassLoader) throws Exception {
        Method method = applicationContext.getClass().getMethod("getBeansOfType", Class.class);

        Map<String, SoaServiceDefinition<?>> serviceDefBeanMap = (Map<String, SoaServiceDefinition<?>>)
                getBeanOfType(SoaServiceDefinition.class, method, applicationContext, appClassLoader);

        Map<String, LifeCycleAware> lifeCycleBeanMap = (Map<String, LifeCycleAware>)
                getBeanOfType(LifeCycleAware.class, method, applicationContext, appClassLoader);
        //将所有实现了lifecycle的 bean 加入到 LifecycleProcessorFactory 中
        LifecycleProcessorFactory.getLifecycleProcessor().addLifecycles(lifeCycleBeanMap.values());

        Map<String, ServiceInfo> appInfos = toServiceInfos(serviceDefBeanMap);
        Application application = new DapengApplication(new ArrayList<>(appInfos.values()), appClassLoader);
        //Start ClassPathXmlApplicationContext spring context
        if (judgeClasspathXmlContext(appCtxClass)) {
            LOGGER.info("Invoking ClassPathXmlApplicationContext.start()");
            Method startMethod = appCtxClass.getMethod("start");
            startMethod.invoke(applicationContext);
        }

        if (!application.getServiceInfos().isEmpty()) {
            // fixme only registerApplication
            Map<ProcessorKey, SoaServiceDefinition<?>> serviceDefinitionMap = toSoaServiceDefinitionMap(appInfos, serviceDefBeanMap);
            container.registerAppProcessors(serviceDefinitionMap);

            container.registerAppMap(toApplicationMap(serviceDefinitionMap, application));
            //fire a zk event
            container.registerApplication(application);
        }
        LOGGER.info("SpringApplicationLoader: " + ContainerFactory.getContainer().getApplications());
        return true;
    }

    private Map<ProcessorKey, Application> toApplicationMap(Map<ProcessorKey,
            SoaServiceDefinition<?>> serviceDefinitionMap, Application application) {

        return serviceDefinitionMap.keySet().stream().
                collect(Collectors.toMap(Function.identity(), processorKey -> application));
    }

    @Override
    public void stop() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::stop");
        springContextList.forEach(context -> {
            try {
                LOGGER.info(" start to close SpringApplication.....");
                // TODO suppoer SpingBoot
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

            //customConfig 封装到 ServiceInfo 中
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
                if (processor.functions.containsKey(item.getName())) {
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


    private Object getSpringContext(ClassLoader appClassLoader, Constructor<?> constructor) throws Exception {
        List<String> xmlPaths = new ArrayList<>();

        Enumeration<URL> resources = appClassLoader.getResources(CONFIG_PATH);

        while (resources.hasMoreElements()) {
            URL nextElement = resources.nextElement();
            // not load dapeng-soa-transaction-impl
            if (!nextElement.getFile().matches(".*dapeng-transaction-impl.*")) {
                xmlPaths.add(nextElement.toString());
            }
        }
        return constructor.newInstance(new Object[]{xmlPaths.toArray(new String[0])});
    }

    private Object getBeanOfType(Class<?> targetBeanClass, Method method,
                                 Object applicationContext, ClassLoader appClassLoader) throws Exception {
        return method.invoke(applicationContext, appClassLoader.loadClass(targetBeanClass.getName()));
    }

    private boolean judgeClasspathXmlContext(Class<?> appContextClass) {
        return CLASSPATH_XML_CONTEXT_NAME.equals(appContextClass.getName());
    }
}
