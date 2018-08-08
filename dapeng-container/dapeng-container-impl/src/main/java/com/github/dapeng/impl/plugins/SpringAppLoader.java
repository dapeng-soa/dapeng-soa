package com.github.dapeng.impl.plugins;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.lifecycle.LifeCycleAware;
import com.github.dapeng.core.lifecycle.LifeCycleProcessor;
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
                Map<String, List<SoaServiceDefinition<?>>> serviceProcessorListMap = (Map<String, List<SoaServiceDefinition<?>>>) method.invoke(springCtx, appClassLoader.loadClass(ArrayList.class.getName()));

                //获取所有实现了lifecycle的bean
                LifeCycleProcessor.getInstance().addLifecycles(((Map<String, LifeCycleAware>) method.invoke(springCtx, appClassLoader.loadClass(LifeCycleAware.class.getName()))).values());

                List<ServiceInfo> serviceInfoList = new ArrayList<>();
                Map<ProcessorKey, SoaServiceDefinition<?>> serviceDefinitionMap = new HashMap<>(16);
                Map<ProcessorKey, Application> applicationMap = new HashMap<>(16);

                //解析serviceProcessorListMap
                parseSoaServiceDefinitionMap(serviceProcessorListMap, serviceInfoList, serviceDefinitionMap);

                //TODO: 需要构造Application对象
                Application application = new DapengApplication(serviceInfoList, appClassLoader);

                //Start spring context
                LOGGER.info(" start to boot app");
                Method startMethod = appCtxClass.getMethod("start");
                startMethod.invoke(springCtx);

                // IApplication app = new ...
                if (!application.getServiceInfos().isEmpty()) {
                    // fixme only registerApplication
                    container.registerAppProcessors(serviceDefinitionMap);
                    container.registerAppMap(buildApplicationMap(serviceDefinitionMap, application));
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


    private Map<ProcessorKey, Application> buildApplicationMap(Map<ProcessorKey, SoaServiceDefinition<?>> serviceDefinitionMap, Application application) {
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

    private void parseSoaServiceDefinitionMap(Map<String, List<SoaServiceDefinition<?>>> serviceProcessorListMap, List<ServiceInfo> serviceInfoList, Map<ProcessorKey, SoaServiceDefinition<?>> serviceDefinitionMap) throws Exception {
        //合并serviceProcessorListMap 中的List
       /* List<SoaServiceDefinition<?>> serviceDefinitionList = new ArrayList<>();
        serviceProcessorListMap.values().forEach(serviceDefinitionList::addAll);
        List<SoaServiceDefinition<?>> serviceDefinitionList1 = Stream.of(serviceProcessorListMap.values()).flatMap(Collection::stream).flatMap(Collection::stream).collect(Collectors.toList());*/
        List<SoaServiceDefinition<?>> serviceDefinitionList = serviceProcessorListMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());

        for (SoaServiceDefinition<?> soaServiceDefinition : serviceDefinitionList) {
            ServiceInfo serviceInfo = parseSoaServiceDefinition(soaServiceDefinition);
            serviceInfoList.add(serviceInfo);
            serviceDefinitionMap.put(new ProcessorKey(serviceInfo.serviceName, serviceInfo.version), soaServiceDefinition);
        }
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

    /**
     * 解析 SoaServiceDefinition<?>  => ServiceInfo
     *
     * @param soaServiceDefinition
     * @return ServiceInfo
     * @throws Exception
     */
    private ServiceInfo parseSoaServiceDefinition(SoaServiceDefinition<?> soaServiceDefinition) throws Exception {
        long count = new ArrayList<>(Arrays.asList(soaServiceDefinition.iface.getClass().getInterfaces()))
                .stream()
                .filter(m -> "org.springframework.aop.framework.Advised".equals(m.getName()))
                .count();

        Class<?> ifaceClass = (Class) (count > 0 ?
                soaServiceDefinition.iface.getClass().getMethod("getTargetClass").invoke(soaServiceDefinition.iface) :
                soaServiceDefinition.iface.getClass());

        Service service = soaServiceDefinition.ifaceClass.getAnnotation(Service.class);
        // TODO
        assert (service != null);

        /**
         * customConfig 封装到 ServiceInfo 中
         */
        Map<String, Optional<CustomConfigInfo>> methodsConfigMap = new HashMap<>(16);
        soaServiceDefinition.functions.forEach((key, function) -> methodsConfigMap.put(key, function.getCustomConfigInfo()));

        //判断有没有 接口实现的版本号   默认为IDL定义的版本号
        String serviceVersion = service.version();
        if (ifaceClass.isAnnotationPresent(ServiceVersion.class)) {
            serviceVersion = ifaceClass.getAnnotationsByType(ServiceVersion.class)[0].version();
        }
        return new ServiceInfo(service.name(), serviceVersion, "service", ifaceClass, soaServiceDefinition.getConfigInfo(), methodsConfigMap);
    }
}
