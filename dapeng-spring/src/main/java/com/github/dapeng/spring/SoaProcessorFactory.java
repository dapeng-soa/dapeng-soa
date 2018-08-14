package com.github.dapeng.spring;

import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Soa Processor Factory
 *
 * @author craneding
 * @date 16/1/19
 */
public class SoaProcessorFactory implements FactoryBean<List<SoaServiceDefinition<?>>> {
    private static final Logger logger = LoggerFactory.getLogger(SoaProcessorFactory.class);
    private Object serviceRef;
    private String refId;

    public SoaProcessorFactory(Object serviceRef, String refId) {
        this.serviceRef = serviceRef;
        this.refId = refId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SoaServiceDefinition<?>> getObject() throws Exception {
        final Class<?> aClass = serviceRef.getClass();
        final List<Class<?>> interfaces = Arrays.asList(aClass.getInterfaces());

        List<Class<?>> filterInterfaces = interfaces.stream()
                .filter(anInterface -> anInterface.isAnnotationPresent(Service.class) && anInterface.isAnnotationPresent(Processor.class))
                .collect(toList());

        if (filterInterfaces.isEmpty()) {
            throw new RuntimeException("not config @Service & @Processor in " + refId);
        }

        Class<?> interfaceClass = filterInterfaces.get(filterInterfaces.size() - 1);
        List<SoaServiceDefinition<?>> soaServiceDefinitionList = new ArrayList<>(16);

        //Set<Class<?>> serviceImplClasses = new Reflections(aClass).getSubTypesOf((Class<Object>) interfaceClass);
        String serviceImplName = serviceRef.toString().substring(0, serviceRef.toString().lastIndexOf("@"));
        Set<Class<?>> serviceImplClasses = new Reflections(Class.forName(serviceImplName)).getSubTypesOf((Class<Object>) interfaceClass);

        boolean flag = false;
        if (serviceImplClasses.size() > 1) {
            flag = true;
            logger.warn("*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*");
            logger.warn("**** interface:{}  存在多个实现版本 ,请检查是否合理 ", interfaceClass.getName());
        }

        for (Class<?> serviceImpl : serviceImplClasses) {
            ServiceVersion serviceVersion = serviceImpl.isAnnotationPresent(ServiceVersion.class) ? serviceImpl.getAnnotationsByType(ServiceVersion.class)[0] : null;
            if (flag) {
                String version = serviceVersion != null ? serviceVersion.version() : "缺失";
                boolean isRegister = serviceVersion != null ? serviceVersion.isRegister() : true;
                logger.warn("---- class {}:version:{}:isRegister:{}", serviceImpl.getName(), version, isRegister);
            }
            if (serviceVersion != null && !serviceVersion.isRegister()) {
                continue;
            }
            soaServiceDefinitionList.add(getServiceProcess(interfaceClass, serviceImpl.getConstructor().newInstance()));
        }

        if (flag) {
            logger.warn("*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*");
        }
        return soaServiceDefinitionList;
    }

    @Override
    public Class<?> getObjectType() {
        return ArrayList.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }


    /**
     * @return
     */
    private SoaServiceDefinition<?> getServiceProcess(Class<?> interfaceClass, Object serviceImplClass) throws Exception {
        Processor processor = interfaceClass.getAnnotation(Processor.class);
        Class<?> processorClass = Class.forName(processor.className(), true, interfaceClass.getClassLoader());
        Constructor<?> constructor = processorClass.getConstructor(interfaceClass, Class.class);
        SoaServiceDefinition tProcessor = (SoaServiceDefinition) constructor.newInstance(serviceImplClass, interfaceClass);
        /**
         * idl service custom config
         */
        if (interfaceClass.isAnnotationPresent(CustomConfig.class)) {
            CustomConfig customConfig = interfaceClass.getAnnotation(CustomConfig.class);
            long timeout = customConfig.timeout();
            tProcessor.setConfigInfo(new CustomConfigInfo(timeout));
        }
        /**
         * 过滤有 @CustomConfig 的方法
         */
        Method[] serviceMethods = interfaceClass.getDeclaredMethods();
        List<Method> configMethod = Arrays.stream(serviceMethods)
                .filter(method -> tProcessor.functions.keySet().contains(method.getName()))
                .filter(method -> method.isAnnotationPresent(CustomConfig.class))
                .collect(Collectors.toList());
        /**
         * 将值设置到 functions 中
         */
        configMethod.forEach(method -> {
            CustomConfig customConfig = method.getAnnotation(CustomConfig.class);
            SoaFunctionDefinition functionDefinition = (SoaFunctionDefinition) tProcessor.functions.get(method.getName());
            functionDefinition.setCustomConfigInfo(new CustomConfigInfo(customConfig.timeout()));
        });
        return tProcessor;
    }

}
