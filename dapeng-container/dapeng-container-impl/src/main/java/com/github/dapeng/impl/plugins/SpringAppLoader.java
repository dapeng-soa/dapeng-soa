package com.github.dapeng.impl.plugins;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.core.Application;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.Service;
import com.github.dapeng.core.ServiceInfo;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.impl.container.DapengApplication;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class SpringAppLoader implements Plugin {

    private final Container container;
    private final List<ClassLoader> appClassLoaders;

    public SpringAppLoader(Container container, List<ClassLoader> appClassLoaders) {
        this.container = container;
        this.appClassLoaders = appClassLoaders;
    }

    //伪代码
    @Override
    public void start() {
        String configPath = "META-INF/spring/services.xml";

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (ClassLoader appClassLoader : appClassLoaders) {
            try {

                // ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new Object[]{xmlPaths.toArray(new String[0])});
                // context.start();
                Class<?> appClass = appClassLoader.loadClass("org.springframework.context.support.ClassPathXmlApplicationContext");
                Class<?>[] parameterTypes = new Class[]{String[].class};
                Constructor<?> constructor = appClass.getConstructor(parameterTypes);

                Thread.currentThread().setContextClassLoader(appClassLoader);
                Object context = getSpringContext(configPath, appClassLoader,constructor);

                Method method = appClass.getMethod("getBeansOfType", Class.class);

                Map<String, SoaServiceDefinition<?>> processorMap = (Map<String, SoaServiceDefinition<?>>)
                        method.invoke(context, appClassLoader.loadClass(SoaServiceDefinition.class.getName()));
                //TODO: 需要构造Application对象
                Map<String,ServiceInfo> appInfos = toServiceInfos(processorMap);
                Application application = new DapengApplication(appInfos.values().stream().collect(Collectors.toList()),appClassLoader);

                Map<ProcessorKey, SoaServiceDefinition<?>> serviceDefinitionMap = toSoaServiceDefinitionMap(appInfos,processorMap);
                container.registerAppProcessors(serviceDefinitionMap);

                // IApplication app = new ...
                if (! application.getServiceInfos().isEmpty()) {
                    container.registerApplication(application);
                    container.registerAppMap(toApplicationMap(serviceDefinitionMap,application));
                }

                System.out.println(" ------------ SpringClassLoader: " + ContainerFactory.getContainer().getApplications());

                //Start spring context
                System.out.println(" start to boot app");
                Method startMethod = appClass.getMethod("start");
                startMethod.invoke(context);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e.getCause() + "  : "+ e.getMessage());
            } finally {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
        }
    }

    private Map<ProcessorKey,Application> toApplicationMap(Map<ProcessorKey, SoaServiceDefinition<?>> serviceDefinitionMap,Application application){
        Map<ProcessorKey,Application> map = new HashMap<>();
        serviceDefinitionMap.forEach((key,serviceDef)->{
            map.put(key,application);
        });
        return map;
    }

    @Override
    public void stop() {

    }

    private Map<String,ServiceInfo> toServiceInfos(Map<String, SoaServiceDefinition<?>> processorMap) throws Exception {

        Map<String,ServiceInfo> serviceInfoMap = new HashMap<>();
        for (Map.Entry<String, SoaServiceDefinition<?>> processorEntry : processorMap.entrySet()) {
            String processorKey = processorEntry.getKey();
            SoaServiceDefinition<?> processor = processorEntry.getValue();

            long count = new ArrayList<>(Arrays.asList(processor.iface.getClass().getInterfaces()))
                    .stream()
                    .filter(m -> m.getName().equals("org.springframework.aop.framework.Advised"))
                    .count();

            Class<?> ifaceClass = (Class) (count > 0 ? processor.iface.getClass().getMethod("getTargetClass").invoke(processor.iface) : processor.iface.getClass());

            Service service = processor.ifaceClass.getAnnotation(Service.class);
            assert(service != null); // TODO

            ServiceInfo serviceInfo = new ServiceInfo(service.name(), service.version(), "service", ifaceClass);
            serviceInfoMap.put(processorKey,serviceInfo);
        }

        return serviceInfoMap;
    }



    private Map<ProcessorKey, SoaServiceDefinition<?>> toSoaServiceDefinitionMap(Map<String,ServiceInfo> serviceInfoMap, Map<String, SoaServiceDefinition<?>> processorMap) {
        Map<ProcessorKey, SoaServiceDefinition<?>>  serviceDefinitions = new HashMap<>();
        serviceInfoMap.entrySet().forEach(i -> {
            serviceDefinitions.put(new ProcessorKey(i.getValue().serviceName,i.getValue().version), processorMap.get(i.getKey()));
        });
        return serviceDefinitions;
    }


    private Object getSpringContext(String configPath, ClassLoader appClassLoader, Constructor<?> constructor) throws Exception{
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
