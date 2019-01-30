package com.github.dapeng.impl.plugins;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
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

public class SpringAppLoader implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringAppLoader.class);
    private final Container container;
    private final List<ClassLoader> appClassLoaders;
    private List<Object> springCtxs = new ArrayList<>();

    public SpringAppLoader(Container container, List<ClassLoader> appClassLoaders) {
        this.container = container;
        this.appClassLoaders = appClassLoaders;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void start() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::start");
        String configPath = "META-INF/spring/services.xml";

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (ClassLoader appClassLoader : appClassLoaders) {
            try {

                // ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new Object[]{xmlPaths.toArray(new String[0])});
                // context.start();
                Class<?> appCtxClass = appClassLoader.loadClass("org.springframework.context.support.ClassPathXmlApplicationContext");
                Class<?>[] parameterTypes = new Class[]{String[].class};
                Constructor<?> constructor = appCtxClass.getConstructor(parameterTypes);

                Thread.currentThread().setContextClassLoader(appClassLoader);
                Object springCtx = getSpringContext(configPath, appClassLoader, constructor);

                springCtxs.add(springCtx);

                Method method = appCtxClass.getMethod("getBeansOfType", Class.class);

                Map<String, SoaServiceDefinition<?>> processorMap = (Map<String, SoaServiceDefinition<?>>)
                        method.invoke(springCtx, appClassLoader.loadClass(SoaServiceDefinition.class.getName()));

                //获取所有实现了lifecycle的bean
                LifecycleProcessorFactory.getLifecycleProcessor().addLifecycles(((Map<String, LifeCycleAware>)
                        method.invoke(springCtx, appClassLoader.loadClass(LifeCycleAware.class.getName()))).values());


                //TODO: 需要构造Application对象
                Map<String, ServiceInfo> appInfos = toServiceInfos(processorMap);
                Application application = new DapengApplication(new ArrayList<>(appInfos.values()), appClassLoader);

                //Start spring context
                LOGGER.info(" start to boot app");
                Method startMethod = appCtxClass.getMethod("start");
                startMethod.invoke(springCtx);

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


    private Object getSpringContext(String configPath, ClassLoader appClassLoader, Constructor<?> constructor) throws Exception {
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
