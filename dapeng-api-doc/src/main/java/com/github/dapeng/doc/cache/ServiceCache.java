package com.github.dapeng.doc.cache;


import com.google.common.collect.TreeMultimap;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.core.Application;
import com.github.dapeng.core.ServiceInfo;
import com.github.dapeng.core.metadata.Field;
import com.github.dapeng.core.metadata.Method;
import com.github.dapeng.core.metadata.Struct;
import com.github.dapeng.core.metadata.TEnum;
import com.github.dapeng.metadata.MetadataClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXB;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Service Cache
 *
 * @author craneding
 * @date 15/4/26
 */
public class ServiceCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceCache.class);

    private static Map<String, com.github.dapeng.core.metadata.Service> services = new TreeMap<>();

    private static Map<String, com.github.dapeng.core.metadata.Service> fullNameService = new TreeMap<>();

    public static TreeMultimap<String, String> urlMappings = TreeMultimap.create();

    public void init() {
        System.out.println("------------------------- Initialize serviceCache......");

        System.out.println("--------------------Container: " + ContainerFactory.getContainer());
        System.out.println("--------------------Applications: " + ContainerFactory.getContainer().getApplications());

        List<Application> applications = ContainerFactory.getContainer().getApplications();
        applications.forEach(i -> loadServices(i));
    }

    private void unloadServices(Application application) {
        //Some specific logic here
    }


    private void loadServices(Application application) {

        urlMappings.clear();

        List<ServiceInfo> serviceInfos = application.getServiceInfos();
        serviceInfos.forEach(s -> {
            String metadata = "";
            try {
                metadata = new MetadataClient(s.serviceName, s.version).getServiceMetadata();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            if (metadata != null) {
                try (StringReader reader = new StringReader(metadata)) {
                    com.github.dapeng.core.metadata.Service serviceData = JAXB.unmarshal(reader, com.github.dapeng.core.metadata.Service.class);
                    Map<String, com.github.dapeng.core.metadata.Service> serviceInfo = loadResource(serviceData);
                    ServiceCache.services.putAll(serviceInfo);
                } catch (Exception e) {
                    LOGGER.error("生成SERVICE出错", e);
                }
            }
        });

        LOGGER.info("size of urlMapping: " + urlMappings.size());
    }

    public void destory() {
        services.clear();
    }

    public Map<String, com.github.dapeng.core.metadata.Service> loadResource(com.github.dapeng.core.metadata.Service service) {

        final Map<String, com.github.dapeng.core.metadata.Service> services = new TreeMap<>();

        String key = getKey(service);
        services.put(key, service);

        String fullNameKey = getFullNameKey(service);
        fullNameService.put(fullNameKey, service);

        //将service和service中的方法、结构体、枚举和字段名分别设置对应的url，以方便搜索
        urlMappings.put(service.getName(), "api/service/" + service.name + "/" + service.meta.version + ".htm");
        List<Method> methods = service.getMethods();
        for (int i = 0; i < methods.size(); i++) {
            Method method = methods.get(i);
            urlMappings.put(method.name, "api/method/" + service.name + "/" + service.meta.version + "/" + method.name + ".htm");
        }

        List<Struct> structs = service.getStructDefinitions();
        for (int i = 0; i < structs.size(); i++) {
            Struct struct = structs.get(i);
            urlMappings.put(struct.name, "api/struct/" + service.name + "/" + service.meta.version + "/" + struct.namespace + "." + struct.name + ".htm");

            List<Field> fields = struct.getFields();
            for (int j = 0; j < fields.size(); j++) {
                Field field = fields.get(j);
                urlMappings.put(field.name, "api/struct/" + service.name + "/" + service.meta.version + "/" + struct.namespace + "." + struct.name + ".htm");
            }
        }

        List<TEnum> tEnums = service.getEnumDefinitions();
        for (int i = 0; i < tEnums.size(); i++) {
            TEnum tEnum = tEnums.get(i);
            urlMappings.put(tEnum.name, "api/enum/" + service.name + "/" + service.meta.version + "/" + tEnum.namespace + "." + tEnum.name + ".htm");
        }

        return services;
    }

    public com.github.dapeng.core.metadata.Service getService(String name, String version) {

        if (name.contains(".")) {
            return fullNameService.get(getKey(name, version));
        } else {
            return services.get(getKey(name, version));
        }
    }

    private String getKey(com.github.dapeng.core.metadata.Service service) {
        return getKey(service.getName(), service.getMeta().version);
    }

    private String getFullNameKey(com.github.dapeng.core.metadata.Service service) {
        return getKey(service.getNamespace() + "." + service.getName(), service.getMeta().version);
    }

    private String getKey(String name, String version) {
        return name + ":" + version;
    }

    public Map<String, com.github.dapeng.core.metadata.Service> getServices() {
        return services;
    }

}
