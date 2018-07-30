package com.github.dapeng.impl.plugins;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.core.lifecycyle.Lifecycle;
import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.impl.container.DapengApplication;
import com.github.dapeng.core.lifecycyle.LifecycleProcessor;
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
                LifecycleProcessor.getInstance().addLifecycles(((Map<String, Lifecycle>)
                        method.invoke(springCtx, appClassLoader.loadClass(Lifecycle.class.getName()))).values());

                //TODO: 需要构造Application对象
                Map<String, ServiceInfo> appInfos = toServiceInfos(processorMap);
                Application application = new DapengApplication(appInfos.values().stream().collect(Collectors.toList()),
                        appClassLoader);

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
                    container.registerApplication(application); //fire a zk event
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
            } catch (IllegalAccessException|InvocationTargetException e) {
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

            ServiceInfo serviceInfo = new ServiceInfo(service.name(), service.version(),
                    "service", ifaceClass, processor.getConfigInfo(), methodsConfigMap);

            serviceInfoMap.put(processorKey, serviceInfo);
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
        Object context = constructor.newInstance(new Object[]{xmlPaths.toArray(new String[0])});
        return context;
    }

}
